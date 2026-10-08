/**
 * 交互回归检查：确认「按需加载」的弹窗仍然能正常打开。
 *
 * 背景：为了让 Element Plus 不进入首屏，登录弹窗、移动端菜单抽屉、
 * 首页反馈弹窗都改成了 defineAsyncComponent + v-if 的懒加载形式。
 * 这类改动最容易出的问题是「点了没反应」（chunk 加载完但没触发 open，
 * 或事件监听随组件一起被删掉）。
 *
 * 用法：node scripts/check-lazy-dialogs.mjs <baseUrl>
 * 依赖：本机 Chrome（通过 CDP 直接驱动，不引入 puppeteer）
 */
import { spawn } from "node:child_process";
import { mkdtempSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = process.argv[2] || "http://127.0.0.1:4350";

const CHROME_CANDIDATES = [
	"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
	"C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
	"/usr/bin/google-chrome",
	"/usr/bin/chromium",
];

function findChrome() {
	for (const p of CHROME_CANDIDATES) if (existsSync(p)) return p;
	throw new Error("找不到 Chrome，可传入 CHROME_PATH 环境变量");
}

/** 极简 CDP 客户端 */
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
	/** 在页面里执行表达式并返回 JSON 结果 */
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

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function main() {
	const chrome = findChrome();
	const profile = mkdtempSync(join(tmpdir(), "cdp-"));
	const proc = spawn(
		chrome,
		[
			"--headless=new",
			"--disable-gpu",
			"--no-first-run",
			"--no-default-browser-check",
			`--user-data-dir=${profile}`,
			"--remote-debugging-port=9333",
			"about:blank",
		],
		{ stdio: "ignore" },
	);

	// 等待 DevTools 端点就绪
	let target = null;
	for (let i = 0; i < 40; i++) {
		await sleep(250);
		try {
			const res = await fetch("http://127.0.0.1:9333/json/list");
			const list = await res.json();
			target = list.find((t) => t.type === "page");
			if (target) break;
		} catch {
			/* 还没起来 */
		}
	}
	if (!target) throw new Error("Chrome DevTools 端点未就绪");

	const ws = new WebSocket(target.webSocketDebuggerUrl);
	await new Promise((res, rej) => {
		ws.addEventListener("open", res);
		ws.addEventListener("error", rej);
	});
	const cdp = new CDP(ws);
	await cdp.send("Page.enable");
	await cdp.send("Runtime.enable");

	const results = [];
	const check = (name, pass, detail = "") => {
		results.push({ name, pass, detail });
		console.log(`  ${pass ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
	};

	// ---------------------------------------------------------- 首页
	await cdp.send("Page.navigate", { url: BASE + "/" });
	await sleep(4000);

	const heroOk = await cdp.eval(`!!document.querySelector('.hero-duo')`);
	check("首页 Hero 渲染", heroOk === true);

	// 反馈弹窗：点开后应出现 el-dialog
	await cdp.eval(`
		(() => {
			const btn = document.querySelector('.aside-btn .feedback');
			if (btn) btn.click();
			return !!btn;
		})()
	`);
	await sleep(2500);
	const feedbackOpen = await cdp.eval(
		`!!document.querySelector('.el-dialog, .el-overlay')`,
	);
	check("首页反馈弹窗可按需打开", feedbackOpen === true);

	// 关掉它，避免影响后续判断
	await cdp.eval(`
		(() => {
			const b = [...document.querySelectorAll('.el-dialog__headerbtn, .el-button')]
				.find(el => el.textContent.includes('关闭'));
			if (b) b.click();
			return true;
		})()
	`);
	await sleep(800);

	// ---------------------------------------------------------- 移动端抽屉
	// 用 CDP 覆盖视口为窄屏，让「菜单」按钮显示
	await cdp.send("Emulation.setDeviceMetricsOverride", {
		width: 390,
		height: 844,
		deviceScaleFactor: 2,
		mobile: true,
	});
	await cdp.send("Page.navigate", { url: BASE + "/" });
	await sleep(4000);

	const burgerVisible = await cdp.eval(`
		(() => {
			const b = document.querySelector('.mast-burger');
			if (!b) return 'missing';
			const r = b.getBoundingClientRect();
			return r.width > 0 && r.height > 0 ? 'visible' : 'hidden';
		})()
	`);
	check("窄视口下菜单按钮可见", burgerVisible === "visible", burgerVisible);

	await cdp.eval(`
		(() => { const b = document.querySelector('.mast-burger'); if (b) b.click(); return true; })()
	`);
	await sleep(2500);
	const drawerOpen = await cdp.eval(`
		(() => {
			const d = document.querySelector('.el-drawer');
			const nav = document.querySelector('.mobile-nav');
			return JSON.stringify({ drawer: !!d, nav: !!nav, links: nav ? nav.querySelectorAll('a').length : 0 });
		})()
	`);
	const d = JSON.parse(drawerOpen);
	check("移动端菜单抽屉可按需打开", d.drawer && d.nav && d.links > 0, drawerOpen);

	// ---------------------------------------------------------- 登录弹窗
	await cdp.send("Emulation.clearDeviceMetricsOverride");
	await cdp.send("Page.navigate", { url: BASE + "/" });
	await sleep(3500);

	await cdp.eval(`
		(() => {
			const link = [...document.querySelectorAll('.mast-top .links a')]
				.find(a => a.textContent.trim() === '登录');
			if (link) link.click();
			return !!link;
		})()
	`);
	await sleep(2500);
	const loginOpen = await cdp.eval(`
		(() => {
			const dlg = document.querySelector('.el-dialog');
			const inputs = document.querySelectorAll('.el-dialog input');
			return JSON.stringify({ dialog: !!dlg, inputs: inputs.length });
		})()
	`);
	const l = JSON.parse(loginOpen);
	check("登录弹窗可按需打开", l.dialog && l.inputs > 0, loginOpen);

	// ---------------------------------------------------------- auth:required 事件
	await cdp.send("Page.navigate", { url: BASE + "/" });
	await sleep(3500);
	await cdp.eval(`window.dispatchEvent(new CustomEvent('auth:required')); true`);
	await sleep(2500);
	const viaEvent = await cdp.eval(`!!document.querySelector('.el-dialog')`);
	check("auth:required 事件仍能唤起登录弹窗", viaEvent === true);

	ws.close();
	proc.kill();

	const failed = results.filter((r) => !r.pass);
	console.log(
		`\n${results.length - failed.length}/${results.length} 项通过` +
			(failed.length ? `，失败：${failed.map((f) => f.name).join("、")}` : ""),
	);
	process.exit(failed.length ? 1 : 0);
}

main().catch((err) => {
	console.error("检查失败：", err.message);
	process.exit(1);
});
