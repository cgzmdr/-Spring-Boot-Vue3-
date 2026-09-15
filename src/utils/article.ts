/**
 * 长文解析：把后端下发的纯文本长文（民族简介 / 风俗详情 / 节日起源等）
 * 解析为结构化段落，供图文并茂的正文组件（components/RichArticle.vue）渲染。
 *
 * 支持的内容约定（与后台录入的文本格式保持一致）：
 * - 空行分段（段内软换行自动拼接，中文不加空格、英文补空格）
 * - `【小节标题】` 或 Markdown `## 小节标题` → 小标题
 * - `> 引用文字` → 引言块
 * - `![图注](图片地址)` → 内嵌配图（单独成行）
 */

export type ArticleKind = "heading" | "para" | "quote" | "image";

export interface ArticleBlock {
	kind: ArticleKind;
	/** 文本内容（image 块中为图注） */
	text: string;
	/** 仅 image 块：图片地址 */
	src?: string;
}

/** 正文可插入的配图（components/RichArticle.vue） */
export interface ArticleFigure {
	/** 图片地址（为空则退化为主题色兜底块） */
	src?: string | null;
	/** 图注 */
	caption?: string;
	/** 占位图提示词 */
	prompt?: string;
	/** 兜底主题色 */
	theme?: string;
}

const IMAGE_LINE = /^!\[([^\]]*)\]\(([^)\s]+)\)$/;
const HEADING_LINE = /^(?:【(.+)】|#{1,4}\s*(.+))$/;
const CJK_END = /[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]$/;
const CJK_START = /^[\u3000-\u303f\u4e00-\u9fff\uff00-\uffef]/;

/** 拼接同一段落内的软换行 */
function joinLine(prev: string, next: string) {
	if (!prev) return next;
	if (CJK_END.test(prev) || CJK_START.test(next)) return prev + next;
	return `${prev} ${next}`;
}

/** 解析长文为结构化段落 */
export function parseArticle(input?: string | null): ArticleBlock[] {
	if (!input) return [];
	const blocks: ArticleBlock[] = [];

	// 空行分段：段内的软换行合并为同一段落，段与段之间严格分隔
	for (const chunk of input.split(/\n{2,}/)) {
		const lines = chunk
			.split(/\n/)
			.map((line) => line.trim())
			.filter(Boolean);
		/** 当前段落（段内软换行依次并入） */
		let para: ArticleBlock | null = null;

		for (const line of lines) {
			const image = line.match(IMAGE_LINE);
			if (image) {
				blocks.push({ kind: "image", text: image[1] || "", src: image[2] });
				para = null;
				continue;
			}

			const heading = line.match(HEADING_LINE);
			if (heading) {
				blocks.push({ kind: "heading", text: (heading[1] || heading[2] || "").trim() });
				para = null;
				continue;
			}

			if (line.startsWith(">")) {
				blocks.push({ kind: "quote", text: line.replace(/^>\s?/, "") });
				para = null;
				continue;
			}

			if (para) para.text = joinLine(para.text, line);
			else {
				para = { kind: "para", text: line };
				blocks.push(para);
			}
		}
	}

	return blocks;
}

export interface SectionSplit {
	/** 去掉该小节后的剩余段落（用于「民族简介」正文） */
	rest: ArticleBlock[];
	/** 抽离出的小节段落 */
	section: ArticleBlock[];
	/** 小节标题（含关键词的原文标题） */
	title: string;
	/** 是否命中该小节 */
	found: boolean;
}

/**
 * 从长文中抽离指定小节（如「历史沿革」），使其可以单独成栏展示。
 * 命中规则：1) `【历史沿革】` 之类的小标题；2) 段落以关键词开头（兜底）。
 */
export function extractSection(blocks: ArticleBlock[], keyword: string): SectionSplit {
	const isHeadingMatch = (b: ArticleBlock) => b.kind === "heading" && b.text.includes(keyword);
	const headIndex = blocks.findIndex(isHeadingMatch);
	const paraIndex = headIndex < 0 ? blocks.findIndex((b) => b.kind === "para" && b.text.startsWith(keyword)) : -1;

	if (headIndex < 0 && paraIndex < 0) {
		return { rest: blocks, section: [], title: keyword, found: false };
	}

	const from = headIndex >= 0 ? headIndex + 1 : paraIndex;
	const title = headIndex >= 0 ? blocks[headIndex].text : keyword;
	let to = blocks.findIndex((b, i) => i >= from && b.kind === "heading");
	if (to < 0) to = blocks.length;

	const section = blocks.slice(from, to).map((b, i) => {
		// 兜底命中时，去掉段首的「历史沿革：」前缀
		if (paraIndex >= 0 && i === 0 && b.kind === "para") {
			return { ...b, text: b.text.replace(new RegExp(`^${keyword}\\s*[:：]?\\s*`), "") };
		}
		return b;
	});

	const rest = [...blocks.slice(0, headIndex >= 0 ? headIndex : from), ...blocks.slice(to)];
	return { rest, section: section.filter((b) => b.text), title, found: true };
}

/** 统计段落数量（用于是否需要配图布局判断） */
export function countParagraphs(blocks: ArticleBlock[]) {
	return blocks.reduce((n, b) => (b.kind === "para" ? n + 1 : n), 0);
}
