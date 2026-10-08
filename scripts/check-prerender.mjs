/**
 * 预渲染产物自检。
 *
 * 检查 dist 下每个预渲染页面是否真的包含「不用执行 JS 就能读到的内容」，
 * 以及 head 里的 title / description / canonical 是否正确。
 * 这些正是预渲染的全部意义，必须能自动验证。
 *
 * 用法：node scripts/check-prerender.mjs
 */
import { readFileSync, existsSync } from "node:fs";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const DIST = join(ROOT, "dist");

/** 期望被直出的页面：路径 → 标题里应出现的关键词 */
const EXPECTED = [
	{ path: "/", keywords: ["走进多彩 56 民族世界", "COVER STORY", "五十六个民族"] },
	{ path: "/ethnic", keywords: ["五十六个民族", "ETHNIC GROUPS"] },
	{ path: "/heritage", keywords: ["非遗"] },
	{ path: "/festival", keywords: ["节日"] },
	{ path: "/festival/calendar", keywords: ["日历"] },
	{ path: "/art", keywords: ["艺术"] },
	{ path: "/persons", keywords: ["人物"] },
	{ path: "/sports", keywords: ["体育"] },
	{ path: "/autonomous", keywords: ["自治"] },
	{ path: "/unity", keywords: ["民族团结"] },
	{ path: "/about", keywords: ["关于"] },
	{ path: "/discussion", keywords: ["讨论"] },
];
/** 每个预渲染页面都必须有的公共骨架（导航 + 页脚） */
const COMMON = ["中华民族 · 多元一体", "走进多彩 56 民族世界", "闽ICP备"];

let pass = 0;
let fail = 0;

function check(name, ok, detail = "") {
	if (ok) {
		pass++;
		console.log(`  PASS  ${name}`);
	} else {
		fail++;
		console.log(`  FAIL  ${name}${detail ? "  — " + detail : ""}`);
	}
}

/** 取出 #app 里的直出 HTML（即 hydration 会接管的那部分） */
function appHtml(html) {
	const m = html.match(/<div id="app"[^>]*>([\s\S]*)<\/div>\s*(?:<div id="boot-skeleton"|<\/body>)/);
	return m ? m[1] : "";
}

console.log("=== 预渲染产物自检 ===\n");

for (const page of EXPECTED) {
	const file =
		page.path === "/" ? join(DIST, "index.html") : join(DIST, page.path, "index.html");

	if (!existsSync(file)) {
		check(`${page.path} 存在`, false, "文件缺失");
		continue;
	}

	const html = readFileSync(file, "utf8");
	const body = appHtml(html);

	// 1. 必须有直出内容（排除空壳）
	check(
		`${page.path.padEnd(20)} 有直出内容`,
		body.length > 800,
		`#app 内 ${body.length} 字符`,
	);

	// 2. 标记位：告诉客户端这是预渲染页
	check(`${page.path.padEnd(20)} 带 data-prerender 标记`, html.includes("data-prerender"));

	// 3. 页面关键词出现在直出内容里（证明该路由真的被渲染了，而不是共用一个壳）
	const missing = page.keywords.filter((k) => !body.includes(k) && !html.includes(k));
	check(
		`${page.path.padEnd(20)} 含本页关键词`,
		missing.length === 0,
		missing.length ? `缺少 ${missing.join("、")}` : "",
	);

	// 4. 公共骨架（导航 / 站名 / 页脚）——体现「JS 到达前就能看到站点结构」
	const missingCommon = COMMON.filter((k) => !body.includes(k));
	check(
		`${page.path.padEnd(20)} 含导航与页脚骨架`,
		missingCommon.length === 0,
		missingCommon.length ? `缺少 ${missingCommon.join("、")}` : "",
	);

	// 5. head 元信息
	const title = (html.match(/<title>([^<]*)<\/title>/) || [])[1] || "";
	check(`${page.path.padEnd(20)} title 已直出`, title.length > 0, title);

	const canonical = (html.match(/<link rel="canonical" href="([^"]*)"/) || [])[1] || "";
	check(
		`${page.path.padEnd(20)} canonical 正确`,
		canonical === `https://czdr.work${page.path === "/" ? "/" : page.path}`,
		canonical || "缺失",
	);

	const desc = (html.match(/<meta name="description" content="([^"]*)"/) || [])[1] || "";
	// 只要求「存在且是本页专属文案」；长度阈值放宽到 10，
	// 因为部分页面的描述本身就很短（如「人物专栏」由 title 复用得来）
	check(`${page.path.padEnd(20)} description 已直出`, desc.length >= 10, `${desc.length} 字符`);

	console.log("");
}

// SPA 兜底壳必须保留（详情页等动态路由用它）
const shell = join(DIST, "_spa-shell.html");
check("SPA 兜底壳 _spa-shell.html 已保留", existsSync(shell));
if (existsSync(shell)) {
	const s = readFileSync(shell, "utf8");
	const m = s.match(/<div id="app"[^>]*>([\s\S]*?)<\/div>/);
	const inner = m ? m[1].trim() : "(未匹配到 #app)";
	check("兜底壳的 #app 为空（不含直出内容）", inner === "", `内容为 ${JSON.stringify(inner.slice(0, 60))}`);
}

// ---------------------------------------------------------------- SEO 产物
const sitemapPath = join(DIST, "sitemap.xml");
check("sitemap.xml 已生成", existsSync(sitemapPath));
if (existsSync(sitemapPath)) {
	const sm = readFileSync(sitemapPath, "utf8");
	const locs = [...sm.matchAll(/<loc>([^<]+)<\/loc>/g)].map((m) => m[1]);
	// 每个预渲染路由都应出现在 sitemap 里
	const missing = EXPECTED.map((p) => `https://czdr.work${p.path === "/" ? "/" : p.path}`).filter(
		(u) => !locs.includes(u),
	);
	check(
		"sitemap 覆盖全部预渲染路由",
		missing.length === 0 && locs.length === EXPECTED.length,
		missing.length ? `缺少 ${missing.join("、")}` : `${locs.length} 条`,
	);
}

const robotsPath = join(DIST, "robots.txt");
check("robots.txt 已生成", existsSync(robotsPath));
if (existsSync(robotsPath)) {
	const rb = readFileSync(robotsPath, "utf8");
	check("robots.txt 指向 sitemap", rb.includes("Sitemap: https://czdr.work/sitemap.xml"));
	check(
		"robots.txt 屏蔽了登录后私有页面",
		["/profile", "/messages", "/notifications", "/me/"].every((p) => rb.includes(`Disallow: ${p}`)),
	);
}

console.log(`\n${pass} 项通过，${fail} 项失败`);
process.exit(fail ? 1 : 0);
