<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { translateApi } from "@/api/modules";
import { useLangStore } from "@/stores/lang";
import { useTranslateStore } from "@/stores/translate";

/**
 * 「译 / 查看原文」按钮（机器翻译）。
 *
 * 用法：父组件用 v-model 接收译文（null = 展示原文），并自行渲染「机器翻译」标注：
 *   <TranslateButton v-model="titleTrans" v-model:provider="titleProvider" target-type="topic" :target-id="id" scope="title" :source-locale="topic.lang" />
 *   <h1>{{ titleTrans ?? topic.title }}</h1>
 *   <span v-if="titleTrans" class="mt-badge">{{ translate.label }}</span>
 *
 * 站点未接入翻译服务（provider=none）或原文已是当前语言时按钮自动隐藏。
 */
const props = withDefaults(
	defineProps<{
		targetType: "topic" | "post" | "message" | "board";
		targetId: string;
		/** title 仅标题（列表页）/ body / all 标题+正文 */
		scope?: "title" | "body" | "all";
		/** 原文语言（lang 字段），与当前界面语言相同时不显示按钮 */
		sourceLocale?: string | null;
		compact?: boolean;
	}>(),
	{ scope: "all", sourceLocale: null, compact: false },
);

/** 译文；null 表示展示原文 */
const translated = defineModel<string | null>({ default: null });
/** 产出来源（libretranslate / ollama / glossary） */
const provider = defineModel<string | null>("provider", { default: null });

const lang = useLangStore();
const translate = useTranslateStore();

const loading = ref(false);
const hint = ref<string | null>(null);
let hintTimer: number | null = null;

const targetLocale = computed(() => (lang.isEn ? "en" : "zh"));

/** 原文语言与目标语言一致时无需翻译 */
const needed = computed(() => {
	const src = (props.sourceLocale || "").trim().toLowerCase();
	if (!src) return true;
	return src.slice(0, 2) !== targetLocale.value.slice(0, 2);
});

const visible = computed(() => translate.enabled && needed.value && !!props.targetId);
/** 注意：列表里用 v-model="map[id]" 时初值是 undefined，所以只认非空字符串 */
const active = computed(() => typeof translated.value === "string" && translated.value.length > 0);

function flash(text: string) {
	hint.value = text;
	if (hintTimer !== null) window.clearTimeout(hintTimer);
	hintTimer = window.setTimeout(() => {
		hint.value = null;
	}, 3600);
}

async function toggle() {
	if (active.value) {
		translated.value = null;
		provider.value = null;
		return;
	}
	loading.value = true;
	try {
		const res = await translateApi.translate(props.targetType, props.targetId, targetLocale.value, props.scope);
		if (res.translated && res.content) {
			translated.value = res.content;
			provider.value = res.provider;
		} else {
			flash(res.message || lang.pick("暂无法翻译，已显示原文", "Translation unavailable, showing original"));
		}
	} catch {
		flash(lang.pick("翻译服务暂不可用", "Translation service unavailable"));
	} finally {
		loading.value = false;
	}
}

onMounted(() => {
	translate.ensureLoaded();
});
</script>

<template>
	<span
		v-if="visible"
		class="tb-wrap"
	>
		<button
			type="button"
			class="tb"
			:class="{ compact, active }"
			:disabled="loading"
			:title="
				active
					? lang.pick('查看原文', 'Show original')
					: lang.pick('翻译为当前界面语言（机器翻译）', 'Translate (machine translation)')
			"
			@click.stop.prevent="toggle"
		>
			{{ loading ? "…" : active ? lang.pick("查看原文", "Original") : lang.pick("译", "Translate") }}
		</button>
		<span
			v-if="hint"
			class="tb-hint"
			>{{ hint }}</span
		>
	</span>
</template>

<style scoped>
.tb-wrap {
	display: inline-flex;
	align-items: center;
	gap: 6px;
	vertical-align: middle;
}
.tb {
	font: inherit;
	font-size: 11.5px;
	line-height: 1;
	padding: 3px 8px;
	border: 1px solid var(--line);
	background: var(--paper);
	color: var(--muted);
	cursor: pointer;
	transition:
		color var(--dur-fast) ease,
		border-color var(--dur-fast) ease,
		background var(--dur-fast) ease;
}
.tb:hover:not(:disabled) {
	color: var(--accent);
	border-color: var(--accent);
}
.tb.active {
	color: var(--accent);
	border-color: var(--accent);
	background: color-mix(in srgb, var(--accent) 6%, var(--paper));
}
.tb:disabled {
	cursor: default;
	opacity: 0.6;
}
.tb.compact {
	padding: 2px 6px;
	font-size: 11px;
}
.tb-hint {
	font-size: 11.5px;
	color: var(--muted);
}
</style>
