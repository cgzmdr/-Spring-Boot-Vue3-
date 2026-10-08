/**
 * 生成首屏占位图（LQIP）。
 *
 * 背景：public/images/placeholders/placeholder.webp 是一张 16:9 的实拍图，
 * 未压缩前 450 KB。它会被首页「封面故事」的 Hero 以及所有缺图的卡片引用，
 * 在慢网（3G）下单个文件就要 30 秒以上，是首屏白屏的主要成因之一。
 *
 * 本脚本由 public/images/placeholders/placeholder.webp 生成两个产物：
 *   1. public/images/placeholders/placeholder.webp  —— 420×236 的轻量清晰图（约 15 KB）
 *   2. src/utils/placeholder-lqip.ts                —— 内联 base64 微缩图（约 0.3 KB）
 *
 * LQIP（Low Quality Image Placeholder）内联在 JS/CSS 里，不产生额外请求，
 * 可让封面在清晰图到达前就先有内容，避免大片留白。
 *
 * 用法：node scripts/gen-placeholder.mjs [源图路径]
 * 依赖：sharp（仅构建期需要，见 package.json devDependencies）
 */
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const SRC = resolve(process.argv[2] || resolve(ROOT, "public/images/placeholders/placeholder.webp"));
const FULL_OUT = resolve(ROOT, "public/images/placeholders/placeholder.webp");
const LQIP_OUT = resolve(ROOT, "src/utils/placeholder-lqip.ts");

/**
 * 清晰图规格。源图是 2:3 竖幅，而首页 Hero 按 16:9 渲染（object-fit: cover），
 * 因此这里直接裁成 16:9 —— 与最终显示一致，既避免把竖幅顶部天空压成主视觉，
 * 也让同样的码率花在真正会被看到的画面上。
 */
const FULL_WIDTH = 320;
const FULL_HEIGHT = 180;
const FULL_QUALITY = 46;
/** LQIP 微缩图宽度：够铺满模糊效果即可 */
const LQIP_WIDTH = 24;

async function main() {
	let sharp;
	try {
		({ default: sharp } = await import("sharp"));
	} catch {
		console.error(
			"[gen-placeholder] 缺少 sharp。请先安装：pnpm add -D sharp\n" +
				"（也可以直接沿用仓库中已生成的 placeholder.webp 与 placeholder-lqip.ts，无需重新运行本脚本）",
		);
		process.exit(1);
	}

	const input = await readFile(SRC);

	const full = await sharp(input)
		.resize({
			width: FULL_WIDTH,
			height: FULL_HEIGHT,
			fit: "cover",
			position: "attention",
			withoutEnlargement: false,
		})
		.webp({ quality: FULL_QUALITY, effort: 6 })
		.toBuffer();
	await mkdir(dirname(FULL_OUT), { recursive: true });
	await writeFile(FULL_OUT, full);

	const lqip = await sharp(full)
		.resize({ width: LQIP_WIDTH })
		.webp({ quality: 32, effort: 6 })
		.toBuffer();
	const dataUrl = `data:image/webp;base64,${lqip.toString("base64")}`;

	const ts = `/**
 * 自动生成，请勿手工修改。
 * 生成命令：node scripts/gen-placeholder.mjs
 *
 * 用途：图片加载期间的内联微缩占位图（LQIP）。
 * 直接内联为 data URL，不产生额外网络请求，因此不拖慢首屏。
 */
export const PLACEHOLDER_LQIP =
	"${dataUrl}";
`;
	await writeFile(LQIP_OUT, ts, "utf8");

	console.log(
		`[gen-placeholder] ${SRC}\n` +
			`  → ${FULL_OUT}  (${(full.length / 1024).toFixed(1)} KB, ${FULL_WIDTH}×${FULL_HEIGHT})\n` +
			`  → ${LQIP_OUT}  (${(dataUrl.length / 1024).toFixed(2)} KB inline)`,
	);
}

main().catch((err) => {
	console.error(err);
	process.exit(1);
});
