/**
 * 验证「省级地图数据不进首屏、只在切到地图视图时才请求」。
 *
 * 检查点：
 *   1. 打包产物中不存在省级 GeoJSON 数据（只有 URL 字符串）；
 *   2. 打开 /ethnic 卡片墙时，**没有**发起对 china-provinces.json 的请求；
 *   3. 切换到地图视图后才发起该请求；
 *   4. Nginx 未开启 br 时也能正常加载（这里只验证响应可用）。
 *
 * 用法：node scripts/check-map-lazy-fetch.mjs <baseUrl>
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync, readFileSync, readdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://127.0.0.1:4360";
const MAP_FILE = "china-provinces.json";

const CHROME_CANDIDATES = [
	"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
	"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
	"/usr/bin/google-chrome",
	"/usr/bin/chromium",
];
const findChrome = () => {
	for (const p of CHROME_CANDIDATES) if (existsSync(p)) return p;
	throw new Error("找不到 Chrome");
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

class CDP {
	constructor(ws) {
		this.ws = ws;
		this.id = 0;
		this.pending = new Map();
		this.handlers = [];
		ws.addEventListener("message", (ev) => {
			const msg = JSON.parse(ev.data);
			if (msg.id && this.pending.has(msg.id)) {
				const { resolve, reject } = this.pending.get(msg.id);
				this.pending.delete(msg.id);
				msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result);
			} else if (msg.method) {
				for (const h of this.handlers) h(msg);
			}
		});
	}
	on(fn) {
		this.handlers.push(fn);
	}
	send(method, params = {}) {
		const id = ++this.id;
		return new Promise((resolve, reject) => {
			this.pending.set(id, { resolve, reject });
			this.ws.send(JSON.stringify({ id, method, params }));
		});
	}
	async eval(expression) {
		const r = await this.send("Runtime.evaluate", {
			expression,
			awaitPromise: true,
			returnByValue: true,
		});
		if (r.exceptionDetails) throw new Error(r.exceptionDetails.text);
		return r.result.value;
	}
}

/** 静态检查：源码与产物里是否混入了 GeoJSON 数据 */
function staticCheck() {
	const out = [];

	// 产物目录里的 js chunk
	const assetDir = "dist/assets";
	if (existsSync(assetDir)) {
		for (const f of readdirSync(assetDir).filter((x) => x.endsWith(".js"))) {
			const t = readFileSync(join(assetDir, f), "utf8");
			const hasData =
				t.includes("内蒙古自治区") || t.includes('"features":[{');
			if (hasData) out.push(`${f} 内联了省级 GeoJSON 数据`);
		}
	}
	return out;
}

async function main() {
	const results = [];
	const check = (name, pass, detail = "") => {
		results.push({ name, pass });
		console.log(`  ${pass ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
	};

	// ---- 静态检查 ----
	const staticProblems = staticCheck();
	check(
		"打包产物中未内联省级 GeoJSON 数据",
		staticProblems.length === 0,
		staticProblems.join("; "),
	);

	// ---- 运行时检查 ----
	const chrome = findChrome();
	const profile = mkdtempSync(join(tmpdir(), "cdp-map-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9334",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const list = await (await fetch("http://127.0.0.1:9334/json/list")).json();
			target = list.find((t) => t.type === "page");
			if (target) break;
		} catch {
			/* 等待 */
		}
	}
	if (!target) throw new Error("DevTools 未就绪");

	const ws = new WebSocket(target.webSocketDebuggerUrl);
	await new Promise((res, rej) => {
		ws.addEventListener("open", res);
		ws.addEventListener("error", rej);
	});
	const cdp = new CDP(ws);
	await cdp.send("Page.enable");
	await cdp.send("Runtime.enable");
	await cdp.send("Network.enable");

	/**
	 * 记录所有地图数据请求。
	 *
	 * 说明：本检查跑在 `vite preview`（纯静态），没有后端。
	 * 地图组件要等 `/ethnic-groups/map` 接口返回点位后才渲染，
	 * 因此这里先用 CDP 把该接口打桩，让「有聚居地点位」这一前提成立，
	 * 否则页面会停在错误态，测不到 GeoJSON 的请求时机。
	 */
	const mapRequests = [];
	/** 网络层失败（CORS / 连接被拒等），用于区分「没请求」与「请求失败」 */
	const networkFailures = [];
	/** 页面控制台输出与异常 */
	const pageLogs = [];
	cdp.on((msg) => {
		if (msg.method === "Network.requestWillBeSent") {
			const url = msg.params?.request?.url || "";
			if (url.includes(MAP_FILE)) mapRequests.push(url);
		}
	});

	/**
	 * 打桩后端接口。
	 *
	 * 本检查跑在 `vite preview`（纯静态），没有后端。地图组件要等
	 * `/ethnic-groups/map` 返回点位后才渲染，所以需要让这一前提成立。
	 *
	 * 用 CDP 的 Fetch 域在**网络层**拦截并伪造响应，而不是在页面里替换
	 * window.fetch / XMLHttpRequest。原因是项目用 axios，
	 * 它会直接读 XHR 实例上的 readyState/status/response 并依赖内部
	 * 事件时序，用 JS 伪造的属性很容易被覆盖，导致「看起来打桩了但不生效」。
	 */
	await cdp.send("Fetch.enable", {
		patterns: [{ urlPattern: "*", requestStage: "Request" }],
	});

	/**
	 * 地图接口的假点位。
	 *
	 * 字段名必须与 src/api/types.ts 的 EthnicMapPoint 一致——
	 * 注意是 longitude / latitude，不是 lng / lat
	 * （早先这里写错，导致打桩"成功"但地图上什么都没有，白排查了一轮）。
	 */
	const MAP_STUB = {
		code: 0,
		data: [
			{
				id: "p1",
				ethnicGroupId: "e1",
				ethnicGroupName: "汉族",
				ethnicGroupSlug: "han",
				themeColor: "#B6402E",
				province: "北京市",
				city: "北京市",
				longitude: 116.4,
				latitude: 39.9,
				description: "",
			},
			{
				id: "p2",
				ethnicGroupId: "e2",
				ethnicGroupName: "彝族",
				ethnicGroupSlug: "yi",
				themeColor: "#2E6FB6",
				province: "云南省",
				city: "昆明市",
				longitude: 102.7,
				latitude: 25.0,
				description: "",
			},
		],
	};
	const LIST_STUB = { code: 0, data: { data: [], total: 0 } };

	cdp.on(async (msg) => {
		if (msg.method === "Network.requestWillBeSent") {
			const url = msg.params?.request?.url || "";
			const method = msg.params?.request?.method || "GET";
			if (url.includes(MAP_FILE)) mapRequests.push(`${method} ${url}`);
			return;
		}
		if (msg.method === "Network.loadingFailed") {
			networkFailures.push(
				`${msg.params?.type} ${msg.params?.errorText} blocked=${msg.params?.blockedReason || "-"}`,
			);
			return;
		}
		if (msg.method === "Runtime.consoleAPICalled") {
			const text = (msg.params?.args || [])
				.map((a) => a.value ?? a.description ?? "")
				.join(" ");
			if (text && !text.includes("Console Ninja")) {
				pageLogs.push(`[${msg.params.type}] ${text}`.slice(0, 300));
			}
			return;
		}
		if (msg.method === "Runtime.exceptionThrown") {
			pageLogs.push(
				`[exception] ${(msg.params?.exceptionDetails?.exception?.description || msg.params?.exceptionDetails?.text || "").slice(0, 300)}`,
			);
			return;
		}
		if (msg.method !== "Fetch.requestPaused") return;
		const { requestId, request } = msg.params;
		const url = request.url || "";
		// CORS 预检：直接放行，否则跨域 GET 会被浏览器拦掉
		if (request.method === "OPTIONS") {
			try {
				await cdp.send("Fetch.fulfillRequest", {
					requestId,
					responseCode: 204,
					responseHeaders: [
						{ name: "Access-Control-Allow-Origin", value: "*" },
						{ name: "Access-Control-Allow-Methods", value: "GET,POST,OPTIONS" },
						{ name: "Access-Control-Allow-Headers", value: "*" },
						{ name: "Access-Control-Max-Age", value: "600" },
					],
				});
			} catch {
				/* 已取消 */
			}
			return;
		}
		const payload = url.includes("/ethnic-groups/map")
			? MAP_STUB
			: url.includes("/ethnic-groups")
				? LIST_STUB
				: null;
		try {
			if (payload) {
				await cdp.send("Fetch.fulfillRequest", {
					requestId,
					responseCode: 200,
					responseHeaders: [
						{ name: "Content-Type", value: "application/json" },
						{ name: "Access-Control-Allow-Origin", value: "*" },
					],
					body: Buffer.from(JSON.stringify(payload)).toString("base64"),
				});
			} else {
				await cdp.send("Fetch.continueRequest", { requestId });
			}
		} catch {
			/* 请求可能已取消 */
		}
	});

	// 切换视图按钮的文案（源码里为「卡片墙 / 分布地图」）
	const MAP_BTN = "分布地图";

	// 打开民族列表（默认卡片墙视图）
	// 注意用 History 路径，不是旧的 hash 形式（/#/ethnic）
	await cdp.send("Page.navigate", { url: BASE + "/ethnic" });
	// 等待路由过渡 + 列表接口返回
	for (let i = 0; i < 30; i++) {
		await sleep(500);
		const ready = await cdp.eval(`!!document.querySelector('.filter')`);
		if (ready) break;
	}
	await sleep(1500);

	check(
		"打开民族列表（卡片墙）时未请求地图数据",
		mapRequests.length === 0,
		mapRequests.length ? `${mapRequests.length} 次请求` : "",
	);

	// 切换到地图视图
	const switched = await cdp.eval(`
		(() => {
			const btn = [...document.querySelectorAll('button')]
				.find(b => b.textContent.trim() === ${JSON.stringify(MAP_BTN)});
			if (!btn) return false;
			btn.click();
			return true;
		})()
	`);
	check("找到并点击「分布地图」切换按钮", switched === true);
	await sleep(5000);

	check(
		"切到地图视图后才请求地图数据",
		mapRequests.length > 0,
		mapRequests.length ? `${mapRequests.length} 次` : "未发起请求",
	);

	// 地图 SVG 是否真的渲染出来
	const mapRendered = await cdp.eval(`
		(() => {
			const svg = document.querySelector('.map-svg');
			const canvas = document.querySelector('.map-canvas');
			const stats = document.querySelector('.map-stats');
			const note = document.querySelector('.map-note');
			return JSON.stringify({
				svg: !!svg,
				canvas: !!canvas,
				provinces: document.querySelectorAll('.map-svg .province').length,
				bubbles: document.querySelectorAll('.map-svg .bubble').length,
				circles: document.querySelectorAll('.map-svg circle').length,
				svgLen: svg ? svg.innerHTML.length : -1,
				statsText: stats ? stats.innerText.replace(/\\s+/g, " ").slice(0, 200) : "",
				noteText: note ? note.innerText.replace(/\\s+/g, " ").slice(0, 200) : "",
				mainText: document.querySelector('main').innerText.replace(/\\s+/g, " ").slice(-260),
			});
		})()
	`);
	const mr = JSON.parse(mapRendered);
	if (networkFailures.length) {
		console.log(`    （网络层事件：${networkFailures.slice(0, 5).join("; ")}）`);
	}
	if (pageLogs.length) {
		console.log("    （页面日志）");
		for (const l of pageLogs.slice(0, 8)) console.log(`      ${l}`);
	}
	// 省级轮廓 + 我们打桩的两个聚居地气泡
	check(
		"省级轮廓已渲染",
		mr.svg && mr.provinces > 20,
		`province paths = ${mr.provinces}`,
	);
	check(
		"聚居地气泡已渲染",
		mr.bubbles > 0 && mr.circles > 0,
		`bubbles = ${mr.bubbles}, circles = ${mr.circles}`,
	);

	ws.close();
	proc.kill();

	const failed = results.filter((r) => !r.pass);
	console.log(
		`\n${results.length - failed.length}/${results.length} 项通过` +
			(failed.length ? `，失败：${failed.map((f) => f.name).join("、")}` : ""),
	);
	process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
	console.error("检查失败：", e.message);
	process.exit(1);
});
