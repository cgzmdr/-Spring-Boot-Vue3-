<script setup lang="ts">
import { computed } from "vue";
import { useLangStore } from "@/stores/lang";

/**
 * 英文正文来源提示（方向 C-3）。
 *
 * 站点大量英文正文由 AI 机器翻译生成（`descriptionEnSource='machine'`），
 * 如实标注来源比假装是人工译文更负责：读者能据此判断是否需回看中文原文。
 *
 * 显示条件（两个都要满足）：
 *   1. 当前处于英文语境（`lang.isEn`）；
 *   2. 页面**确实在展示英文正文**——由调用方通过 `active` 显式传入
 *      （即 descriptionEn 非空且被选用）。只凭 source 判断会在「有译文但当前显示中文」时误报。
 *
 * 注意：Boolean 类型的 prop 在 Vue 中**缺省即为 false**（不是 undefined），
 * 所以这里没有给 active 设默认值——调用方必须显式传，避免默认值把条件悄悄置真/置假。
 */
const props = defineProps<{
	/** 英文正文来源：machine / reviewed / manual / null */
	source?: string | null;
	/** 当前是否确实在展示英文正文（由调用方计算传入） */
	active: boolean;
}>();

const lang = useLangStore();

const visible = computed(
	() => lang.isEn && props.active === true && props.source === "machine",
);
</script>

<template>
	<p v-if="visible" class="mt-notice">
		<span class="mt-tag">MT</span>
		{{ lang.t("mt_notice") }}
	</p>
</template>

<style scoped>
.mt-notice {
	display: flex;
	align-items: flex-start;
	gap: 8px;
	margin: 0 0 14px;
	padding: 8px 12px;
	border-radius: 8px;
	background: color-mix(in srgb, var(--brand-color, #b6402e) 6%, transparent);
	font-size: 12.5px;
	line-height: 1.6;
	color: var(--ink-3, #8a837a);
}

.mt-tag {
	flex: none;
	padding: 1px 6px;
	border-radius: 4px;
	background: color-mix(in srgb, var(--brand-color, #b6402e) 14%, transparent);
	color: var(--brand-color, #b6402e);
	font-size: 10px;
	font-weight: 700;
	letter-spacing: 0.04em;
	line-height: 1.6;
}
</style>
