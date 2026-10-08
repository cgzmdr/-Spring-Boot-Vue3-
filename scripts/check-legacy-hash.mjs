/**
 * 验证「旧 hash 链接」的兼容跳转。
 *
 * 本站从 hash 路由换到 History 路由后，站外可能还留着 `/#/ethnic`
 * 这类旧链接。它们必须被平滑改写到 `/ethnic`，而不是停留在一个
 * 匹配不到路由、甚至会让 scrollBehavior 抛错的状态。
 *
 * 用法：node scripts/check-legacy-hash.mjs <baseUrl>
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://127.0.0.1:4500";

/** 旧链接 → 期望最终落在的路径 */
const CASES = [
	{ from: "/#/ethnic", expect: "/ethnic", text: "五十六个民族" },
	{ from: "/#/about", expect: "/about", text: "关于我们" },
	{ from: "/#/unity", expect: "/unity", text: "民族团结" },
	{ from: "/#/", expect: "/", text: "COVER STORY" },
];

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
		this.events = [];
		ws.addEventListener("message", (ev) => {
			const m = JSON.parse(ev.data);
			if (m.id && this.pending.has(m.id)) {
				const { resolve, reject } = this.pending.get(m.id);
				this.pending.delete(m.id);
				m.error ? reject(new Error(JSON.stringify(m.error))) : resolve(m.result);
			} else if (m.method) {
				this.events.push(m);
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
	const profile = mkdtempSync(join(tmpdir(), "cdp-legacy-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9360",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const l = await (await fetch("http://127.0.0.1:9360/json/list")).json();
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

	console.log("=== 旧 hash 链接兼容性 ===\n");

	for (const c of CASES) {
		// 每轮重新加载，避免历史记录叠加造成干扰
		await cdp.send("Page.navigate", { url: BASE + c.from });
		await sleep(2500);

		const path = await cdp.eval(`location.pathname + location.search`);
		const text = await cdp.eval(`(document.body.innerText || '')`);
		const errors = cdp.events.filter(
			(m) =>
				m.method === "Runtime.exceptionThrown" &&
				/exquerySelector|not a valid selector|SyntaxError/i.test(
					m.params?.exceptionDetails?.exception?.description ||
						m.params?.exceptionDetails?.text ||
						"",
				),
		);
		cdp.events = [];

		check(
			`${c.from.padEnd(14)} → ${c.expect}`,
			path === c.expect,
			`实际落在 ${path}`,
		);
		check(
			`${c.from.padEnd(14)} 渲染出目标页面`,
			text.includes(c.text),
			text.includes(c.text) ? "" : `未见「${c.text}」`,
		);
		check(
			`${c.from.padEnd(14)} 无选择器报错`,
			errors.length === 0,
			errors.length ? "出现 querySelector 非法选择器" : "",
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
