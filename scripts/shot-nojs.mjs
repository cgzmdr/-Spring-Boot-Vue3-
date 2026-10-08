/**
 * 生成「禁用 JS」下的截图，用于人工核对预渲染效果。
 *
 * 注意：不要用 Chrome 命令行的 `--disable-javascript` 截图 ——
 * 该开关会连带影响渲染管线，截出来正文区域是空白，
 * 与 CDP 的 `Emulation.setScriptExecutionDisabled` 行为不一致
 * （后者只停 JS 执行，CSS 与绘制完全正常，才等价于「JS 还没到达」）。
 *
 * 用法：node scripts/shot-nojs.mjs <baseUrl> <输出目录>
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync, mkdirSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://127.0.0.1:4500";
const OUT = process.argv[3] || join(tmpdir(), "nojs-shots");

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
}

const PAGES = ["/", "/ethnic", "/about", "/unity", "/sports", "/heritage"];

async function main() {
	mkdirSync(OUT, { recursive: true });
	const chrome = findChrome();
	const profile = mkdtempSync(join(tmpdir(), "cdp-shot-nojs-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9356",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const l = await (await fetch("http://127.0.0.1:9356/json/list")).json();
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
	await cdp.send("Emulation.setDeviceMetricsOverride", {
		width: 1440,
		height: 1100,
		deviceScaleFactor: 1,
		mobile: false,
	});
	// 只停 JS，不动 CSS/绘制
	await cdp.send("Emulation.setScriptExecutionDisabled", { value: true });

	for (const path of PAGES) {
		await cdp.send("Page.navigate", { url: BASE + path });
		await sleep(1600);
		const shot = await cdp.send("Page.captureScreenshot", { format: "png" });
		const name = (path === "/" ? "home" : path.replace(/^\//, "").replace(/\//g, "-")) + ".png";
		writeFileSync(join(OUT, name), Buffer.from(shot.data, "base64"));
		console.log(`  ✓ ${name}`);
	}

	ws.close();
	proc.kill();
	console.log(`\n截图输出到：${OUT}`);
}

main().catch((e) => {
	console.error(e.message);
	process.exit(1);
});
