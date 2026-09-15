/**
 * 用户生成内容（帖子/回复）渲染与图片处理工具。
 *
 * 安全前提：后端只回传纯文本，前端一律以文本插值渲染（Vue 自动转义），
 * 不解析 HTML，避免 XSS；仅识别少量排版约定：
 * · 空行分段；`> 引用`；`![图注](地址)` 内嵌配图；# 开头为小标题
 */

export interface UserContentBlock {
	kind: "para" | "quote" | "heading" | "image";
	text: string;
	src?: string;
}

const IMAGE_LINE = /^!\[([^\]]*)\]\(([^)\s]+)\)$/;
const HEADING_LINE = /^#{1,3}\s*(.+)$/;

/** 解析用户内容为可渲染块（纯文本 → 结构块） */
export function parseUserContent(input?: string | null): UserContentBlock[] {
	if (!input) return [];
	const blocks: UserContentBlock[] = [];
	for (const chunk of input.split(/\n{2,}/)) {
		const lines = chunk
			.split(/\n/)
			.map((line) => line.trim())
			.filter(Boolean);
		let para: UserContentBlock | null = null;
		for (const line of lines) {
			const image = line.match(IMAGE_LINE);
			if (image) {
				blocks.push({ kind: "image", text: image[1] || "", src: image[2] });
				para = null;
				continue;
			}
			const heading = line.match(HEADING_LINE);
			if (heading) {
				blocks.push({ kind: "heading", text: heading[1].trim() });
				para = null;
				continue;
			}
			if (line.startsWith(">")) {
				blocks.push({ kind: "quote", text: line.replace(/^>\s?/, "") });
				para = null;
				continue;
			}
			if (para) para.text = `${para.text}${line}`;
			else {
				para = { kind: "para", text: line };
				blocks.push(para);
			}
		}
	}
	return blocks;
}

/** 文本摘要（列表展示） */
export function contentExcerpt(text?: string | null, max = 120): string {
	if (!text) return "";
	const flat = text.replace(/\s+/g, " ").trim();
	return flat.length > max ? `${flat.slice(0, max)}…` : flat;
}

/**
 * 浏览器端图片压缩：讨论区配图没必要上传原图。
 * 统一缩放到最长边 maxSize、按 canvas 质量转 JPEG/WebP，显著降低流量与存储。
 * 失败时回退为原文件（仍受服务端 5MB 限制）。
 */
export async function compressImage(file: File, maxSize = 1600, quality = 0.85): Promise<File> {
	if (!file.type.startsWith("image/") || file.type === "image/gif") return file;
	try {
		const bitmap = await createImageBitmap(file);
		const scale = Math.min(1, maxSize / Math.max(bitmap.width, bitmap.height));
		if (scale >= 1 && file.size < 1_200_000) return file;
		const width = Math.round(bitmap.width * scale);
		const height = Math.round(bitmap.height * scale);
		const canvas = document.createElement("canvas");
		canvas.width = width;
		canvas.height = height;
		const ctx = canvas.getContext("2d");
		if (!ctx) return file;
		ctx.drawImage(bitmap, 0, 0, width, height);
		const blob = await new Promise<Blob | null>((resolve) =>
			canvas.toBlob(resolve, "image/jpeg", quality),
		);
		if (!blob) return file;
		return new File([blob], file.name.replace(/\.[^.]+$/, "") + ".jpg", { type: "image/jpeg" });
	} catch {
		return file;
	}
}
