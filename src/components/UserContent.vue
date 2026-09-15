<script setup lang="ts">
import { computed } from "vue";
import { parseUserContent } from "@/utils/usercontent";
import { resolveStaticUrl } from "@/utils/format";

/**
 * 用户生成内容渲染：纯文本 + 少量排版约定（段落 / 引用 / 小标题 / 内嵌图）。
 * 全程文本插值，不渲染 HTML，避免 XSS。
 */
const props = withDefaults(
	defineProps<{
		content?: string | null;
		/** 配图（来自接口的图片数组，渲染在正文之后） */
		images?: string[];
		/** 是否显示配图 */
		showImages?: boolean;
	}>(),
	{ content: "", images: () => [], showImages: true },
);

const blocks = computed(() => parseUserContent(props.content));

/** @提及高亮：把文本切分为「普通片段 / 提及片段」 */
const MENTION = /@[\p{L}\p{N}_\-·]{1,32}/gu;
function segments(text: string): { text: string; mention: boolean }[] {
	const parts: { text: string; mention: boolean }[] = [];
	let last = 0;
	for (const match of text.matchAll(MENTION)) {
		const index = match.index ?? 0;
		if (index > last) parts.push({ text: text.slice(last, index), mention: false });
		parts.push({ text: match[0], mention: true });
		last = index + match[0].length;
	}
	if (last < text.length) parts.push({ text: text.slice(last), mention: false });
	return parts.length ? parts : [{ text, mention: false }];
}
</script>

<template>
	<div class="user-content">
		<template
			v-for="(b, i) in blocks"
			:key="i"
		>
			<h4
				v-if="b.kind === 'heading'"
				class="uc-heading"
			>
				{{ b.text }}
			</h4>
			<blockquote
				v-else-if="b.kind === 'quote'"
				class="uc-quote"
			>
				<template
					v-for="(s, si) in segments(b.text)"
					:key="si"
				>
					<span
						v-if="s.mention"
						class="mention"
						>{{ s.text }}</span
					>
					<template v-else>{{ s.text }}</template>
				</template>
			</blockquote>
			<figure
				v-else-if="b.kind === 'image'"
				class="uc-figure"
			>
				<img
					:src="resolveStaticUrl(b.src)"
					:alt="b.text"
					loading="lazy"
					decoding="async"
				/>
				<figcaption v-if="b.text">{{ b.text }}</figcaption>
			</figure>
			<p
				v-else
				class="uc-para"
			>
				<template
					v-for="(s, si) in segments(b.text)"
					:key="si"
				>
					<span
						v-if="s.mention"
						class="mention"
						>{{ s.text }}</span
					>
					<template v-else>{{ s.text }}</template>
				</template>
			</p>
		</template>

		<div
			v-if="showImages && images?.length"
			class="uc-images"
			:class="{ single: images.length === 1 }"
		>
			<el-image
				v-for="(src, i) in images"
				:key="i"
				class="uc-image"
				:src="resolveStaticUrl(src)"
				:preview-src-list="images.map((s) => resolveStaticUrl(s))"
				:initial-index="i"
				fit="cover"
				preview-teleported
				loading="lazy"
			/>
		</div>
	</div>
</template>

<style scoped>
.user-content {
	font-size: 16px;
	line-height: 1.9;
	color: var(--ink-soft);
}
.uc-para {
	margin: 0 0 12px;
	white-space: pre-wrap;
	overflow-wrap: break-word;
}
.uc-para:last-child {
	margin-bottom: 0;
}
.uc-heading {
	font-family: var(--serif);
	font-size: 18px;
	margin: 18px 0 10px;
	color: var(--ink);
}
.uc-quote {
	margin: 12px 0;
	padding: 10px 16px;
	border-left: 3px solid var(--accent);
	background: color-mix(in srgb, var(--accent) 6%, transparent);
	color: var(--muted);
	font-size: 15px;
}
/* @提及高亮（被提及者会收到站内通知） */
.mention {
	color: var(--accent);
	font-weight: 600;
}
.uc-figure {
	margin: 14px 0;
}
.uc-figure img {
	max-width: 100%;
	height: auto;
	border: 1px solid var(--line);
}
.uc-figure figcaption {
	font-size: 12.5px;
	color: var(--muted);
	margin-top: 6px;
}
.uc-images {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
	gap: 8px;
	margin-top: 12px;
}
.uc-images.single {
	grid-template-columns: minmax(0, 420px);
}
.uc-image {
	width: 100%;
	aspect-ratio: 4 / 3;
	border: 1px solid var(--line);
	cursor: zoom-in;
}
</style>
