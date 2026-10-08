/**
 * 端到端验证：用「模拟 nginx 行为」的静态服务器，检查每个预渲染路由
 * 都能被正确送达，并且**不执行 JS** 就能看到内容。
 *
 * 为什么不用 `vite preview`：它不认识「目录下的 index.html」，
 * 对 `/about` 一律回退到根 index.html。于是预渲染好的子页面在本地
 * 怎么测都是错的（排查过程中确实被这一点误导过）。
 *
 * 用法：
 *   终端 A: node scripts/serve-dist.mjs 4500
 *   终端 B: node scripts/check-prerender-served.mjs http://127.0.0.1:4500
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://127.0.0.1:4500";

/** 路径 → 该页面必须出现的文字（不执行 JS 也应可见） */
const PAGES = [
	{ path: "/", must: ["走进多彩 56 民族世界", "COVER STORY", "闽ICP备"] },
	{ path: "/ethnic", must: ["五十六个民族", "ETHNIC GROUPS", "闽ICP备"] },
	{ path: "/about", must: ["关于我们", "闽ICP备"] },
	{ path: "/unity", must: ["民族团结", "闽ICP备"] },
	{ path: "/sports", must: ["传统体育", "闽ICP备"] },
	{ path: "/heritage", must: ["非遗", "闽ICP备"] },
	{ path: "/persons", must: ["人物", "闽ICP备"] },
	{ path: "/autonomous", must: ["自治", "闽ICP备"] },
	{ path: "/festival", must: ["节日", "闽ICP备"] },
	{ path: "/art", must: ["艺术", "闽ICP备"] },
	{ path: "/discussion", must: ["讨论", "闽ICP备"] },
	{ path: "/festival/calendar", must: ["日历", "闽ICP备"] },
];

/** 动态路由：没有预渲染产物，应由 SPA 空壳兜底 */
const SPA_FALLBACK = ["/ethnic/00000000-0000-0000-0000-000000000000", "/no-such-page"];

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
			const msg = JSON.parse(ev.data);
			if (msg.id && this.pending.has(msg.id)) {
				const { resolve, reject } = this.pending.get(msg.id);
				this.pending.delete(msg.id);
				msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result);
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
	const profile = mkdtempSync(join(tmpdir(), "cdp-served-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9355",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const l = await (await fetch("http://127.0.0.1:9355/json/list")).json();
			target = l.find((t) => t.type === "page");
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

	// 关键：禁用 JS 执行，但 CSS 照常加载 —— 模拟「JS 还没到达」的慢网首屏
	await cdp.send("Emulation.setScriptExecutionDisabled", { value: true });

	console.log("=== 预渲染页面送达检查（JS 已禁用）===\n");

	for (const page of PAGES) {
		await cdp.send("Page.navigate", { url: BASE + page.path });
		await sleep(900);

		// innerText 只返回**可见**文本，能同时证明「内容在」且「没有被隐藏」
		const text = await cdp.eval(`(document.body.innerText || '')`);
		const missing = page.must.filter((k) => !text.includes(k));
		const skelDisplay = await cdp.eval(`
			(() => {
				const e = document.getElementById('boot-skeleton');
				return e ? getComputedStyle(e).display : 'missing';
			})()
		`);

		check(
			`${page.path.padEnd(20)} 无 JS 即可见正文`,
			missing.length === 0,
			missing.length ? `缺少「${missing.join("」「")}」` : `${text.trim().length} 字符可见`,
		);
		check(
			`${page.path.padEnd(20)} 骨架未遮挡内容`,
			skelDisplay === "none",
			`display=${skelDisplay}`,
		);
	}

	console.log("\n=== SPA 兜底检查（动态路由 / 不存在的路径）===\n");

	for (const path of SPA_FALLBACK) {
		await cdp.send("Page.navigate", { url: BASE + path });
		await sleep(900);
		const skel = await cdp.eval(`
			(() => {
				const e = document.getElementById('boot-skeleton');
				return e ? getComputedStyle(e).display : 'missing';
			})()
		`);
		const appEmpty = await cdp.eval(`
			(() => {
				const e = document.getElementById('app');
				return !e || e.children.length === 0;
			})()
		`);
		check(
			`${path.padEnd(46)} 回退到 SPA 空壳`,
			skel === "flex" && appEmpty === true,
			`骨架=${skel} #app为空=${appEmpty}`,
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
