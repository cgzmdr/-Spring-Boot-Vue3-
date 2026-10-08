/**
 * 开发环境图片加载自检。
 *
 * 背景：`CoverImage` 里曾把**整个** `/images/` 前缀当成「前端自带资源」，
 * 导致后端提供的真实内容图（`/images/ethnic/...`、`/images/topic/...`）
 * 被请求到 dev server 上。Vite 对未知路径会回退成 index.html
 * （HTTP 200 + Content-Type: text/html），浏览器解码失败 → 图片全部裂开。
 * 这个脚本把「图片是否真的解码成功」变成可回归的断言。
 *
 * 判定标准用 `naturalWidth > 0`，而不是「请求是否 200」——
 * 回退成 HTML 时请求同样是 200，看状态码根本发现不了。
 *
 * 用法：node scripts/check-dev-images.mjs [baseUrl]
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://localhost:5173";

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
		ws.addEventListener("message", (ev) => {
			const m = JSON.parse(ev.data);
			if (m.id && this.pending.has(m.id)) {
				const { resolve, reject } = this.pending.get(m.id);
				this.pending.delete(m.id);
				m.error ? reject(new Error(JSON.stringify(m.error))) : resolve(m.result);
			}
		});
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

/** 检查页面：路径 + 该页应有的图片特征 */
const PAGES = [
	{ path: "/", label: "首页" },
	{ path: "/ethnic", label: "民族列表" },
];

async function main() {
	let pass = 0;
	let fail = 0;
	const check = (name, ok, detail = "") => {
		if (ok) {
			pass++;
			console.log(`  PASS  ${name}`);
		} else {
			fail++;
			console.log(`  FAIL  ${name}${detail ? "  — " + detail : ""}`);
		}
	};

	const chrome = findChrome();
	const profile = mkdtempSync(join(tmpdir(), "cdp-devimg-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9380",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const l = await (await fetch("http://127.0.0.1:9380/json/list")).json();
			target = l.find((t) => t.type === "page");
			if (target) break;
		} catch {
			/* 等待 */
		}
	}
	const ws = new WebSocket(target.webSocketDebuggerUrl);
	await new Promise((r) => ws.addEventListener("open", r));
	const cdp = new CDP(ws);
	await cdp.send("Page.enable");
	await cdp.send("Runtime.enable");
	await cdp.send("Emulation.setDeviceMetricsOverride", {
		width: 1440,
		height: 2400,
		deviceScaleFactor: 1,
		mobile: false,
	});

	console.log(`=== 开发环境图片加载检查 @ ${BASE} ===\n`);

	for (const page of PAGES) {
		await cdp.send("Page.navigate", { url: BASE + page.path });
		await sleep(8000);

		// 滚到底触发懒加载，再回到顶部
		await cdp.eval(`window.scrollTo(0, document.body.scrollHeight); true`);
		await sleep(3000);
		await cdp.eval(`window.scrollTo(0, 0); true`);
		await sleep(1500);

		const imgs = JSON.parse(
			await cdp.eval(`
				JSON.stringify([...document.querySelectorAll('img')].map(i => ({
					src: i.getAttribute('src') || '',
					nw: i.naturalWidth,
				})))
			`),
		);

		// 只统计「应该加载」的图：src 非空
		const real = imgs.filter((i) => i.src && !i.src.startsWith("data:"));
		const broken = real.filter((i) => i.nw === 0);

		check(
			`${page.label.padEnd(10)} 存在图片`,
			real.length > 0,
			`共 ${real.length} 张`,
		);
		check(
			`${page.label.padEnd(10)} 图片全部解码成功`,
			broken.length === 0,
			broken.length
				? `${broken.length} 张失败，例：${broken[0].src}`
				: `${real.length} 张全部 naturalWidth>0`,
		);

		// 后端图片必须指向 STATIC_BASE，而不是 dev server 自身
		const localhostBackend = real.filter((i) =>
			/^https?:\/\/localhost:20256\//.test(i.src),
		);
		const wronglyLocal = real.filter(
			(i) =>
				i.src.startsWith("/images/") && !i.src.startsWith("/images/placeholders/"),
		);
		check(
			`${page.label.padEnd(10)} 内容图指向后端（非同源 /images/）`,
			wronglyLocal.length === 0,
			wronglyLocal.length
				? `${wronglyLocal.length} 张被误判为本地资源，例：${wronglyLocal[0].src}`
				: localhostBackend.length
					? `其中 ${localhostBackend.length} 张走 ${"http://localhost:20256"}`
					: "",
		);
	}

	ws.close();
	proc.kill();
	console.log(`\n${pass} 项通过，${fail} 项失败`);
	process.exit(fail ? 1 : 0);
}

main().catch((e) => {
	console.error("检查失败：", e.message);
	process.exit(1);
});
