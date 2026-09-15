<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { ElMessage } from "element-plus";
import { subscriptionApi } from "@/api/modules";
import type { SubscriptionLevel } from "@/api/modules";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";

/**
 * 订阅 / 免打扰控制（帖子 / 板块 / 用户通用）。
 * 三级：全部通知 / 仅被 @ 时通知 / 免打扰。
 */
const props = withDefaults(
	defineProps<{
		targetType: "topic" | "board" | "user";
		targetId: string;
		/** 紧凑模式（列表工具栏内使用） */
		compact?: boolean;
	}>(),
	{ compact: false },
);

const auth = useAuthStore();
const lang = useLangStore();

const level = ref<SubscriptionLevel | null>(null);
const busy = ref(false);

const OPTIONS = computed(() => [
	{ value: "all" as SubscriptionLevel, label: lang.pick("全部通知", "All activity") },
	{ value: "mention" as SubscriptionLevel, label: lang.pick("仅被 @ 时", "Only mentions") },
	{ value: "off" as SubscriptionLevel, label: lang.pick("免打扰", "Mute") },
]);

const label = computed(() => {
	if (!auth.isLoggedIn) return lang.pick("订阅", "Subscribe");
	const found = OPTIONS.value.find((o) => o.value === level.value);
	return found ? found.label : lang.pick("订阅", "Subscribe");
});

async function load() {
	if (!auth.isLoggedIn || !props.targetId) {
		level.value = null;
		return;
	}
	try {
		level.value = await subscriptionApi.level(props.targetType, props.targetId);
	} catch {
		level.value = null;
	}
}

async function pick(value: SubscriptionLevel) {
	if (!auth.isLoggedIn) {
		ElMessage.warning(lang.pick("请先登录后再订阅", "Sign in to subscribe"));
		window.dispatchEvent(new CustomEvent("auth:required"));
		return;
	}
	busy.value = true;
	try {
		level.value = await subscriptionApi.set(props.targetType, props.targetId, value);
		ElMessage.success(
			value === "off"
				? lang.pick("已设为免打扰", "Muted")
				: value === "mention"
					? lang.pick("仅在有人 @ 你时通知", "Mentions only")
					: lang.pick("已订阅，将收到全部通知", "Subscribed"),
		);
	} catch {
		/* 拦截器已提示 */
	} finally {
		busy.value = false;
	}
}

async function unsubscribe() {
	if (!auth.isLoggedIn) return;
	busy.value = true;
	try {
		await subscriptionApi.remove(props.targetType, props.targetId);
		level.value = null;
		ElMessage.success(lang.pick("已取消订阅", "Unsubscribed"));
	} finally {
		busy.value = false;
	}
}

defineExpose({ load });
watch(() => [props.targetType, props.targetId, auth.isLoggedIn], load, { immediate: true });
</script>

<template>
	<div
		class="subscribe"
		:class="{ compact }"
	>
		<el-dropdown
			trigger="click"
			:disabled="busy"
			@command="pick"
		>
			<button
				class="sub-btn"
				:class="{ on: level && level !== 'off', muted: level === 'off' }"
				type="button"
			>
				<svg
					width="13"
					height="13"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="1.8"
				>
					<path
						d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"
						stroke-linecap="round"
						stroke-linejoin="round"
					/>
				</svg>
				<span>{{ label }}</span>
				<svg
					class="caret"
					width="10"
					height="10"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
				>
					<path
						d="m6 9 6 6 6-6"
						stroke-linecap="round"
						stroke-linejoin="round"
					/>
				</svg>
			</button>
			<template #dropdown>
				<el-dropdown-menu>
					<el-dropdown-item
						v-for="o in OPTIONS"
						:key="o.value"
						:command="o.value"
					>
						<span :class="{ current: level === o.value }">{{ o.label }}</span>
					</el-dropdown-item>
					<el-dropdown-item
						v-if="level"
						divided
						command="__remove"
						@click="unsubscribe"
					>
						{{ lang.pick("取消订阅", "Unsubscribe") }}
					</el-dropdown-item>
				</el-dropdown-menu>
			</template>
		</el-dropdown>
	</div>
</template>

<style scoped>
.subscribe {
	display: inline-flex;
}
.sub-btn {
	display: inline-flex;
	align-items: center;
	gap: 6px;
	border: 1px solid var(--line);
	background: var(--paper);
	color: var(--muted);
	font: inherit;
	font-size: 12px;
	letter-spacing: 0.04em;
	padding: 7px 12px;
	cursor: pointer;
	transition: all var(--dur-fast) ease;
}
.sub-btn:hover {
	border-color: var(--ink);
	color: var(--ink);
}
.sub-btn.on {
	border-color: var(--accent);
	color: var(--accent);
}
.sub-btn.muted {
	border-color: var(--line);
	color: var(--muted);
	opacity: 0.85;
}
.sub-btn .caret {
	opacity: 0.7;
}
.compact .sub-btn {
	padding: 5px 10px;
	font-size: 11.5px;
}
.current {
	color: var(--accent);
	font-weight: 600;
}
</style>
