/**
 * 预渲染（prerender）。
 *
 * 做什么：构建完成后，对一组**静态路由**逐个执行服务端渲染，
 * 把渲染结果写回 dist 里对应的 `index.html`，使这些页面
 * 在 JS 到达之前就已经有真实内容（导航 / 标题 / 页脚 / 骨架）。
 *
 * 为什么不用完整 SSR：本项目的取数全在 `onMounted` 里走浏览器 API，
 * 服务端渲染时不会执行。要做「带数据的 SSR」得把取数逻辑整体上移，
 * 改动面很大。预渲染静态骨架已经能解决两个核心问题——
 * 慢网白屏 与 每个页面独立 URL 的 SEO —— 性价比最高。
 *
 * 动态路由（`:id` 详情页）不在此列出：它们数量随内容增长，
 * 构建期无法穷举，交由 SPA 兜底（见 Nginx 的 try_files）。
 *
 * 用法：node scripts/prerender.mjs
 */
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const DIST = join(ROOT, "dist");
const SSR_ENTRY = join(ROOT, "dist-ssr", "entry-server.js");

/**
 * 要预渲染的路由。
 *
 * 只放**内容不依赖后端接口、或接口失败也能给出完整静态骨架**的页面。
 * 每条都要给出 title / description，直出到 <head>，
 * 这样即使不执行 JS，爬虫也能读到正确的页面信息。
 */
const ROUTES = [
	{
		path: "/",
		title: "走进多彩 56 个民族世界",
		description:
			"走进多彩 56 个民族世界 —— 一站式、沉浸式、可探索的中华民族数字文化博物馆。",
	},
	{
		path: "/ethnic",
		title: "五十六个民族",
		description:
			"56 个民族的卡片墙与筛选：按地域、语系、人口与非遗维度浏览各民族的风俗、节日与传统艺术。",
	},
	{
		path: "/heritage",
		title: "非遗名录",
		description: "中华民族非物质文化遗产名录：世界级、国家级与省级非遗项目。",
	},
	{
		path: "/festival",
		title: "节日",
		description: "各民族传统节日一览：节期、习俗与所属民族。",
	},
	{
		path: "/festival/calendar",
		title: "节日日历",
		description: "农历换算与今日节日，按月份查看各民族节庆。",
	},
	{
		path: "/art",
		title: "民族艺术",
		description: "民族音乐、舞蹈、戏剧与手工艺等传统艺术门类。",
	},
	{
		path: "/persons",
		title: "人物专栏",
		description: "非物质文化遗产传承人与民族文化名家。",
	},
	{
		path: "/sports",
		title: "传统体育",
		description: "全国少数民族传统体育运动会竞赛项目与民族传统体育项目。",
	},
	{
		path: "/autonomous",
		title: "民族自治地方",
		description: "5 个自治区、30 个自治州、120 个自治县/旗，共 155 个民族自治地方。",
	},
	{
		path: "/unity",
		title: "民族团结",
		description: "像石榴籽一样紧紧抱在一起 —— 民族团结进步主题内容。",
	},
	{
		path: "/about",
		title: "关于本站",
		description: "关于「走进多彩 56 个民族世界」：数据来源、版权说明与联系方式。",
	},
	{
		path: "/discussion",
		title: "讨论区",
		description: "民族文化社区讨论区。",
	},
];

/** 站点正式域名（用于 canonical 与 sitemap） */
const SITE = "https://czdr.work";

/** 转义 HTML 文本，避免标题里的引号破坏属性 */
const esc = (s) =>
	String(s)
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;");

/**
 * 把直出的 body 与 head 元信息注入 index.html 模板。
 *
 * 模板来自 Vite 构建产物，里面已含正确的 hashed 资源引用，
 * 这里只替换占位注释与 <title> / description。
 */
function inject(template, { html, title, description, path }) {
	let out = template;

	// 1. body：把 SSR 结果放进 #app
	out = out.replace(
		/<div id="app"><\/div>/,
		`<div id="app" data-prerender="${esc(path)}">${html}</div>`,
	);

	// 2. title
	out = out.replace(/<title>[\s\S]*?<\/title>/, `<title>${esc(title)}</title>`);

	// 3. description
	if (/<meta name="description"[^>]*>/.test(out)) {
		out = out.replace(
			/<meta name="description"[^>]*>/,
			`<meta name="description" content="${esc(description)}" />`,
		);
	}

	// 4. canonical：每个页面一个稳定 URL，帮助搜索引擎去重
	const canonical = `${SITE}${path === "/" ? "/" : path}`;
	if (/<link rel="canonical"[^>]*>/.test(out)) {
		out = out.replace(
			/<link rel="canonical"[^>]*>/,
			`<link rel="canonical" href="${canonical}" />`,
		);
	} else {
		out = out.replace(
			"</head>",
			`  <link rel="canonical" href="${canonical}" />\n  </head>`,
		);
	}

	// 5. Open Graph：让分享到社交平台时能显示正确的标题与描述
	const og = [
		`<meta property="og:type" content="website" />`,
		`<meta property="og:title" content="${esc(title)}" />`,
		`<meta property="og:description" content="${esc(description)}" />`,
		`<meta property="og:url" content="${canonical}" />`,
		`<meta property="og:site_name" content="走进多彩 56 个民族世界" />`,
	].join("\n    ");
	out = out.replace("</head>", `  ${og}\n  </head>`);

	return out;
}

/**
 * 生成 sitemap.xml。
 *
 * 只收录预渲染的静态路由 —— 它们有稳定的 URL 且内容确定；
 * 动态详情页（`/ethnic/:id` 等）随内容增长，构建期无法穷举，
 * 需要后端在内容变更时另行输出 sitemap 或提供 sitemap 索引。
 */
function buildSitemap(now = new Date().toISOString().slice(0, 10)) {
	const urls = ROUTES.map((r) => {
		const loc = `${SITE}${r.path === "/" ? "/" : r.path}`;
		// 首页优先级最高，其余按栏目重要度递减
		const priority = r.path === "/" ? "1.0" : "0.8";
		return `  <url>\n    <loc>${loc}</loc>\n    <lastmod>${now}</lastmod>\n    <changefreq>weekly</changefreq>\n    <priority>${priority}</priority>\n  </url>`;
	}).join("\n");
	return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`;
}

/** 生成 robots.txt：允许抓取，并指向 sitemap */
function buildRobots() {
	return [
		"User-agent: *",
		"Allow: /",
		"",
		"# 个人中心 / 私信等登录后内容无需收录",
		"Disallow: /profile",
		"Disallow: /messages",
		"Disallow: /notifications",
		"Disallow: /me/",
		"",
		`Sitemap: ${SITE}/sitemap.xml`,
		"",
	].join("\n");
}

async function main() {
	if (!existsSync(SSR_ENTRY)) {
		console.error(
			`[prerender] 找不到 SSR 产物：${SSR_ENTRY}\n` +
				`  请先执行：vite build --ssr src/entry-server.ts --outDir dist-ssr`,
		);
		process.exit(1);
	}
	if (!existsSync(join(DIST, "index.html"))) {
		console.error("[prerender] 找不到 dist/index.html，请先执行 vite build");
		process.exit(1);
	}

	const { render } = await import(pathToFileURL(SSR_ENTRY).href);

	/**
	 * 取「空壳模板」。
	 *
	 * 关键：不能直接读 dist/index.html —— 预渲染会就地把首页写进去，
	 * 于是**重复执行** prerender 时读到的是「已经直出过的首页」，
	 * 再把它当作模板，SPA 兜底壳和所有子页面就会带上首页内容
	 * （表现为访问任意子页都显示首页，且骨架被正文挤掉）。
	 * 优先复用上次留下的 _spa-shell.html，保证模板始终干净。
	 */
	const shellPath = join(DIST, "_spa-shell.html");
	const rawIndexPath = join(DIST, "index.html");
	const template = existsSync(shellPath)
		? await readFile(shellPath, "utf8")
		: await readFile(rawIndexPath, "utf8");

	if (/<div id="app"[^>]*>\s*[^<\s]/.test(template) || /data-prerender/.test(template)) {
		console.error(
			"[prerender] 模板不干净：dist/index.html 里已经有直出内容，且没有 _spa-shell.html 可复用。\n" +
				"  请先执行一次干净的 vite build 再预渲染。",
		);
		process.exit(1);
	}

	// 始终刷新兜底壳，确保它保持空壳状态
	await writeFile(shellPath, template, "utf8");

	let ok = 0;
	for (const route of ROUTES) {
		try {
			const html = await render(route.path);
			const page = inject(template, { html, ...route });

			const dir = route.path === "/" ? DIST : join(DIST, route.path);
			await mkdir(dir, { recursive: true });
			await writeFile(join(dir, "index.html"), page, "utf8");

			ok++;
			console.log(
				`  ✓ ${route.path.padEnd(22)} ${(html.length / 1024).toFixed(1)} KB HTML`,
			);
		} catch (err) {
			console.error(`  ✗ ${route.path}  预渲染失败：${err.message}`);
		}
	}

	// ---------------------------------------------------------------- 收尾产物
	// sitemap 与 robots：换到 History 路由之后，每个页面才有独立 URL 可被收录，
	// 这两份文件是让搜索引擎真正「发现」这些页面的入口。
	await writeFile(join(DIST, "sitemap.xml"), buildSitemap(), "utf8");
	await writeFile(join(DIST, "robots.txt"), buildRobots(), "utf8");
	console.log("  ✓ sitemap.xml / robots.txt");

	console.log(`\n[prerender] ${ok}/${ROUTES.length} 个页面已直出`);
	if (ok !== ROUTES.length) process.exit(1);
}

main().catch((err) => {
	console.error(err);
	process.exit(1);
});
