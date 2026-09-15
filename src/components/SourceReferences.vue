<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { sourceApi } from "@/api/modules";
import type { ContentSource } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 参考资料（方向 C-1：可溯源）
 *
 * 站内每条内容的权威出处。详情页底部引用本组件即可。
 *
 * 设计取舍：
 * · 请求带 `silent`，接口不可用时整块不渲染 —— 来源是「增强信息」，
 *   不应因为它的失败影响正文阅读；
 * · 同时以 title 显示发布机构与采集方式，读者可判断该条数据的可信度与获得方式；
 * · CC BY / CC BY-SA 等许可**要求署名**，因此图片类来源（Wikimedia Commons）
 *   在列表中会带出 remark 中的许可提示，避免上线后遗漏署名义务。
 */

const props = defineProps<{
	/** 内容类型 */
	targetType: "ethnic" | "festival" | "art" | "food" | "topic" | "person" | "area" | "sport";
	/** 内容 ID */
	targetId: string;
	/** 区块标题（默认「参考资料」） */
	title?: string;
}>();

const lang = useLangStore();
const sources = ref<ContentSource[]>([]);
const loaded = ref(false);

async function load() {
	if (!props.targetId) return;
	try {
		sources.value = await sourceApi.byContent(props.targetType, props.targetId);
	} catch {
		sources.value = [];
	} finally {
		loaded.value = true;
	}
}

onMounted(load);
watch(() => [props.targetType, props.targetId], load);
</script>

<template>
	<section
		v-if="loaded && sources.length"
		class="sources"
	>
		<div class="src-head">
			<h3>{{ title || lang.pick("参考资料", "References") }}</h3>
			<span class="src-note">
				{{ lang.pick("本页内容的权威出处", "Sources for this page") }}
			</span>
		</div>

		<ol class="src-list">
			<li
				v-for="(s, i) in sources"
				:key="s.id"
				class="src-item"
			>
				<span class="src-idx">{{ String(i + 1).padStart(2, "0") }}</span>
				<div class="src-body">
					<div class="src-line">
						<span
							class="src-type"
							:class="s.sourceType"
							>{{ s.sourceTypeLabel }}</span
						>
						<span class="src-pub">{{ s.publisherShort || s.publisher }}</span>
						<!-- 有链接则给出原文入口；无链接（如纸质名录）只展示名称 -->
						<a
							v-if="s.url"
							class="src-name link"
							:href="s.url"
							target="_blank"
							rel="noopener noreferrer"
							>{{ s.name }}</a
						>
						<span
							v-else
							class="src-name"
							>{{ s.name }}</span
						>
						<span class="src-method">{{ s.collectMethodLabel }}</span>
					</div>

					<p
						v-if="s.documentTitle"
						class="src-doc"
					>
						{{ s.documentTitle }}
					</p>
					<!-- 本条内容使用该来源的具体部分 -->
					<p
						v-if="s.note"
						class="src-use"
					>
						{{ lang.pick("本页取自：", "Used here: ") }}{{ s.note }}
					</p>
				</div>
			</li>
		</ol>

		<p class="src-foot">
			{{
				lang.pick(
					"标注「开放图库」的来源为自由许可图片（CC BY / CC BY-SA / CC0 / 公有领域），CC BY 与 CC BY-SA 要求署名。",
					'"Open gallery" sources are freely licensed images (CC BY / CC BY-SA / CC0 / public domain); CC BY and CC BY-SA require attribution.',
				)
			}}
		</p>
	</section>
</template>

<style scoped>
.sources {
	border: 1px solid var(--line);
	background: var(--paper-2);
	padding: 22px 26px 20px;
	margin: 36px 0 8px;
}
.src-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 1px solid var(--line);
	padding-bottom: 12px;
	margin-bottom: 14px;
}
.src-head h3 {
	font-family: var(--serif);
	font-size: 19px;
	letter-spacing: 0.06em;
}
.src-note {
	font-size: 11.5px;
	color: var(--muted);
}

.src-list {
	list-style: none;
	display: flex;
	flex-direction: column;
	gap: 12px;
}
.src-item {
	display: grid;
	grid-template-columns: 26px minmax(0, 1fr);
	gap: 10px;
}
.src-item + .src-item {
	border-top: 1px dashed var(--line);
	padding-top: 12px;
}
.src-idx {
	font-family: var(--serif);
	font-size: 12px;
	font-weight: 700;
	color: var(--accent);
	padding-top: 2px;
}
.src-body {
	min-width: 0;
}
.src-line {
	display: flex;
	flex-wrap: wrap;
	align-items: baseline;
	gap: 8px;
}
.src-type {
	font-size: 10.5px;
	letter-spacing: 0.05em;
	padding: 1px 6px;
	border: 1px solid var(--muted);
	color: var(--muted);
	white-space: nowrap;
}
.src-type.official {
	border-color: var(--accent);
	color: var(--accent);
}
.src-type.open {
	border-color: #8a8b52;
	color: #6f7042;
}
.src-pub {
	font-size: 12.5px;
	color: var(--ink-soft);
	white-space: nowrap;
}
.src-name {
	font-family: var(--serif);
	font-size: 14px;
	letter-spacing: 0.01em;
}
.src-name.link {
	color: var(--accent);
	text-decoration: underline;
	text-underline-offset: 3px;
}
.src-name.link:hover {
	opacity: 0.8;
}
.src-method {
	font-size: 10.5px;
	color: var(--muted);
	border: 1px solid var(--line);
	padding: 0 5px;
	background: var(--paper);
	white-space: nowrap;
}
.src-doc {
	font-size: 12px;
	color: var(--muted);
	line-height: 1.7;
	margin-top: 4px;
}
.src-use {
	font-size: 12px;
	color: var(--ink-soft);
	line-height: 1.7;
	margin-top: 4px;
	padding-left: 10px;
	border-left: 2px solid color-mix(in srgb, var(--accent) 40%, transparent);
}
.src-foot {
	font-size: 11.5px;
	color: var(--muted);
	line-height: 1.7;
	margin-top: 16px;
	padding-top: 12px;
	border-top: 1px solid var(--line);
}

@media (max-width: 560px) {
	.sources {
		padding: 18px 16px 16px;
	}
}
</style>
