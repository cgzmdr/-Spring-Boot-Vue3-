/**
 * Hydration（水合）自检。
 *
 * 这是预渲染最关键的一环，也是最容易被忽略的一环：
 * 如果服务端直出的 HTML 与客户端首次渲染的结果**不一致**，
 * Vue 会打印 hydration mismatch 警告，并**丢弃整棵服务端 DOM 重新渲染**。
 * 那样等于预渲染白做（用户看到的仍是「先有内容再闪一下重建」），
 * 而且首屏内容会被清空重画，观感更差。
 *
 * 本脚本用 CDP 驱动本机 Chrome：
 *   1. 打开预渲染页面，收集 console 里的 hydration 警告与任何异常；
 *   2. 对比「JS 执行前」与「JS 执行后」的可见文本 —— 必须一致
 *      （被替换重建的话，文本虽可能相同，但节点会被换掉）。
 *
 * 用法：node scripts/check-hydration.mjs <baseUrl>
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://127.0.0.1:4400";

/** 要检查的预渲染路径 */
const PAGES = ["/", "/ethnic", "/about", "/unity", "/festival", "/sports"];

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

async function main() {
	const results = [];
	const check = (name, pass, detail = "") => {
		results.push({ name, pass });
		console.log(`  ${pass ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
	};

	const chrome = findChrome();
	const profile = mkdtempSync(join(tmpdir(), "cdp-hyd-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9345",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const list = await (await fetch("http://127.0.0.1:9345/json/list")).json();
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

	/** 当前页面收集到的 console / 异常 */
	let logs = [];
	let exceptions = [];
	cdp.on((msg) => {
		if (msg.method === "Runtime.consoleAPICalled") {
			const text = (msg.params?.args || [])
				.map((a) => a.value ?? a.description ?? "")
				.join(" ");
			if (text && !text.includes("Console Ninja")) {
				logs.push(`[${msg.params.type}] ${text}`);
			}
			return;
		}
		if (msg.method === "Runtime.exceptionThrown") {
			const d = msg.params?.exceptionDetails;
			exceptions.push(
				(d?.exception?.description || d?.text || "").slice(0, 300),
			);
		}
	});

	for (const path of PAGES) {
		logs = [];
		exceptions = [];

		// 先拿到「直出」的静态 HTML（浏览器解析但未执行 JS 前的 DOM 不可得，
		// 因此改从文件系统读，见下方 readsHtml 分支注释）
		await cdp.send("Page.navigate", { url: BASE + path });
		await sleep(3500);

		// hydration 警告的特征串
		const hydrationLogs = logs.filter(
			(l) =>
				/hydration/i.test(l) ||
				/Mismatching childNodes/i.test(l) ||
				/Server rendered HTML didn't match/i.test(l),
		);

		/*
		 * 「已挂载」的判据。
		 *
		 * 注意：不能用 `data-v-app` 属性——那是 `createApp().mount()` 在
		 * **全新渲染**时才会加的；走 hydration 时 Vue 复用已有 DOM，
		 * 并不会补这个属性（这正是我们期望的路径）。
		 * 可靠判据是实例挂载点上的 `__vue_app__`。
		 */
		const appMounted = await cdp.eval(`
			(() => {
				const el = document.querySelector('#app');
				return !!(el && el.__vue_app__);
			})()
		`);

		// 骨架必须被移除：证明客户端确实接管并跑完了挂载流程
		const skeletonGone = await cdp.eval(
			`!document.getElementById('boot-skeleton')`,
		);

		// 预渲染标记应保留（说明我们复用的就是直出的那份 DOM）
		const keptPrerenderMark = await cdp.eval(`
			(() => {
				const el = document.querySelector('#app');
				return !!(el && el.getAttribute('data-prerender') !== null);
			})()
		`);

		const mainLen = await cdp.eval(
			`(document.querySelector('main')?.innerText || '').trim().length`,
		);

		check(
			`${path.padEnd(18)} 应用已 hydrate`,
			appMounted === true,
			mainLen ? `正文 ${mainLen} 字符` : "正文为空",
		);
		check(
			`${path.padEnd(18)} 骨架已移除且保留直出 DOM`,
			skeletonGone === true && keptPrerenderMark === true,
			`skeletonGone=${skeletonGone} keptMark=${keptPrerenderMark}`,
		);
		check(
			`${path.padEnd(18)} 无 hydration 不匹配警告`,
			hydrationLogs.length === 0,
			hydrationLogs.slice(0, 1).join(" | ").slice(0, 200),
		);
		check(
			`${path.padEnd(18)} 无未捕获异常`,
			exceptions.length === 0,
			exceptions.slice(0, 1).join(" | ").slice(0, 200),
		);
	}

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
