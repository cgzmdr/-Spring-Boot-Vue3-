/**
 * 对比改动前后「首屏必须先下载的资源总量」，并估算 3G 下的加载时间。
 *
 * 首屏阻塞资源 = index.html 内联引用的 /assets/*（入口脚本、modulepreload、样式表）
 *              + 首屏公共图片（favicon、封面占位图）
 *
 * 用法：node scripts/measure-first-screen.mjs <旧 dist> <新 dist>
 */
import { readFileSync, statSync, existsSync } from "node:fs";
import { join } from "node:path";
import { gzipSync, brotliCompressSync, constants } from "node:zlib";

/** 首屏一定会用到的 public 资源 */
const PUBLIC_ASSETS = ["/images/placeholders/placeholder.webp", "/favicon.svg"];

const kb = (n) => `${(n / 1024).toFixed(1)} KB`;

function sizes(root, rel) {
	const p = join(root, rel.replace(/^\//, ""));
	if (!existsSync(p)) return null;
	const buf = readFileSync(p);
	return {
		raw: statSync(p).size,
		gzip: gzipSync(buf, { level: 9 }).length,
		brotli: brotliCompressSync(buf, {
			params: {
				[constants.BROTLI_PARAM_QUALITY]: 11,
				[constants.BROTLI_PARAM_SIZE_HINT]: buf.length,
			},
		}).length,
	};
}

function analyze(root, label) {
	const html = readFileSync(join(root, "index.html"), "utf8");
	const refs = [...new Set(
		[...html.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map((m) => m[1]),
	)].sort();

	let raw = 0;
	let gzip = 0;
	let brotli = 0;
	const missing = [];
	const rows = [];

	for (const r of refs) {
		const s = sizes(root, r);
		if (!s) {
			missing.push(r);
			continue;
		}
		raw += s.raw;
		gzip += s.gzip;
		brotli += s.brotli;
		rows.push({ r, ...s });
	}

	let pub = 0;
	for (const r of PUBLIC_ASSETS) {
		const s = sizes(root, r);
		if (s) pub += s.raw;
	}

	console.log(`--- ${label} ---`);
	console.log(`  首屏阻塞资源数: ${rows.length}`);
	console.log(`  原始合计:  ${kb(raw).padStart(11)}`);
	console.log(`  gzip 后:   ${kb(gzip).padStart(11)}`);
	console.log(`  brotli 后: ${kb(brotli).padStart(11)}`);

	// 按类型拆开，便于判断瓶颈在 JS 还是 CSS
	const byType = { js: { raw: 0, brotli: 0 }, css: { raw: 0, brotli: 0 } };
	for (const row of rows) {
		const t = row.r.endsWith(".css") ? "css" : "js";
		byType[t].raw += row.raw;
		byType[t].brotli += row.brotli;
	}
	console.log(
		`    · JS  : ${kb(byType.js.raw).padStart(10)} raw / ${kb(byType.js.brotli)} brotli`,
	);
	console.log(
		`    · CSS : ${kb(byType.css.raw).padStart(10)} raw / ${kb(byType.css.brotli)} brotli`,
	);

	console.log(`  公共图片:  ${kb(pub).padStart(11)}  (favicon + 封面占位图)`);
	const grand = brotli + pub;
	console.log(`  首屏总量:  ${kb(grand).padStart(11)}`);
	if (missing.length) console.log("  缺失:", missing);
	return grand;
}

const [, , baseDir, newDir] = process.argv;
const before = analyze(baseDir, "改动前（线上现状）");
console.log();
const after = analyze(newDir, "改动后");
console.log();
console.log(
	`首屏总下载量: ${kb(before)} -> ${kb(after)}  (减少 ${((1 - after / before) * 100).toFixed(1)}%)`,
);
for (const [kbps, name] of [
	[400, "3G 较差"],
	[780, "3G 平均"],
	[1600, "3G 良好"],
]) {
	const b = (before * 8) / kbps / 1000;
	const a = (after * 8) / kbps / 1000;
	console.log(`  ${name} (${kbps} kbps): ${b.toFixed(1)}s -> ${a.toFixed(1)}s`);
}
