<script setup lang="ts">
import { computed } from "vue";
import { useLangStore } from "@/stores/lang";

/**
 * 发帖 / 回复的「引用 + 提及」操作提示条。
 *
 * 目的：把内容排版约定（`> ` 引用、`@昵称` 提及）从 placeholder 里搬出来常驻展示，
 * 并根据当前状态给出反馈——已引用哪一楼、`@` 联想是否已展开。
 */

const props = withDefaults(
	defineProps<{
		/** 当前引用的楼层（回复框点击「引用」后传入） */
		quote?: { nickname?: string | null; floorNo?: number | null } | null;
		/** @提及联想面板是否展开（展开时高亮「提及」提示） */
		mentioning?: boolean;
		/** 精简模式：只保留引用 / 提及两条（发帖页等空间紧凑处） */
		compact?: boolean;
	}>(),
	{ quote: null, mentioning: false, compact: false },
);

const lang = useLangStore();

const quoteText = computed(() => {
	const q = props.quote;
	if (q && q.nickname) {
		return lang.pick(
			`已引用 ${q.nickname}${q.floorNo ? ` 的 #${q.floorNo} 楼` : ""}：发布后显示为引用块，并通知对方`,
			`Quoting ${q.nickname}${q.floorNo ? ` #${q.floorNo}` : ""} — shown as a quote block and they get notified`,
		);
	}
	return lang.pick(
		"点击楼层「引用」，或在行首输入「> 」引用原文",
		"Click Quote on a floor, or start a line with “> ”",
	);
});

const mentionText = computed(() =>
	props.mentioning
		? lang.pick("已打开用户提示：↑↓ 选择，Enter / Tab 插入", "Suggestions open: ↑↓ select, Enter / Tab to insert")
		: lang.pick("输入「@」自动提示用户（最多 10 条），被提及者会收到站内通知", "Type “@” to pick a user (up to 10); they get notified"),
);
</script>

<template>
	<div class="composer-tips">
		<span
			class="ct-item"
			:class="{ on: !!quote && !!quote.nickname }"
		>
			<b>{{ lang.pick("引用", "Quote") }}</b>
			{{ quoteText }}
		</span>
		<span
			class="ct-item"
			:class="{ on: mentioning }"
		>
			<b>{{ lang.pick("提及", "Mention") }}</b>
			{{ mentionText }}
		</span>
		<span
			v-if="!compact"
			class="ct-item"
		>
			<b>{{ lang.pick("排版", "Format") }}</b>
			{{ lang.pick("空行分段；行首「# 」为小标题；「![图注](地址)」插入配图", "Blank line = paragraph; “# ” = heading; “![caption](url)” = image") }}
		</span>
	</div>
</template>

<style scoped>
.composer-tips {
	display: flex;
	flex-wrap: wrap;
	gap: 6px 18px;
	margin-top: 10px;
	font-size: 12px;
	line-height: 1.7;
	color: var(--muted);
}
.ct-item {
	display: inline-flex;
	align-items: baseline;
	gap: 6px;
	transition: color var(--dur-fast) ease;
}
.ct-item b {
	flex: none;
	font-weight: 600;
	font-size: 11px;
	letter-spacing: 0.08em;
	color: var(--ink);
	border: 1px solid var(--line);
	padding: 0 6px;
}
.ct-item.on {
	color: var(--accent);
}
.ct-item.on b {
	border-color: var(--accent);
	color: var(--accent);
}
</style>
