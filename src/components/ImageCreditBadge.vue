<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { imageCreditApi } from "@/api/modules";
import type { ImageCredit } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 图片署名角标（方向 C-4）
 *
 * 挂在图片容器内（父容器需 `position: relative`），默认只显示一个小角标，
 * hover / 点击才展开完整的作者、许可与来源链接 —— 既满足署名义务，
 * 又不干扰版面观感。
 *
 * 关键：**未核实的图片显示「来源待核」而非空白**。
 * 原始版权元数据已丢失，编造署名比留白更糟；如实标注反而是可查证的交代。
 */

const props = defineProps<{
	/** 图片路径（与后端 image_credit.image_path 一致） */
	path: string;
	/** 角标位置 */
	position?: "bottom-right" | "bottom-left";
}>();

const lang = useLangStore();
const credit = ref<ImageCredit | null>(null);
const open = ref(false);

async function load() {
	if (!props.path) return;
	try {
		const list = await imageCreditApi.byPaths([props.path]);
		credit.value = list[0] || null;
	} catch {
		credit.value = null;
	}
}

onMounted(load);
watch(() => props.path, load);

const posClass = computed(() => props.position || "bottom-right");
const isVerified = computed(() => credit.value?.creditStatus === "verified");
</script>

<template>
	<div
		v-if="credit"
		class="credit-badge"
		:class="[posClass, { open, verified: isVerified }]"
		@mouseenter="open = true"
		@mouseleave="open = false"
		@click.stop.prevent="open = !open"
	>
		<!-- 收起态：仅一个小图标 -->
		<span
			class="cb-trigger"
			:title="credit.creditStatusLabel"
		>
			{{ isVerified ? "©" : "?" }}
		</span>

		<!-- 展开态：完整署名信息 -->
		<div
			v-if="open"
			class="cb-panel"
		>
			<div class="cb-cap">{{ credit.caption }}</div>

			<template v-if="isVerified">
				<div class="cb-line">{{ credit.creditLine }}</div>
				<a
					v-if="credit.licenseUrl"
					class="cb-link"
					:href="credit.licenseUrl"
					target="_blank"
					rel="noopener noreferrer"
					>{{ credit.license }}</a
				>
				<span
					v-else-if="credit.license"
					class="cb-line"
					>{{ credit.license }}</span
				>
				<a
					v-if="credit.sourceUrl"
					class="cb-link"
					:href="credit.sourceUrl"
					target="_blank"
					rel="noopener noreferrer"
					>{{ lang.pick("查看来源", "View source") }} ↗</a
				>
			</template>

			<template v-else>
				<div class="cb-pending">
					{{ lang.pick("来源待核", "Source pending") }}
				</div>
				<div class="cb-note">{{ credit.remark }}</div>
			</template>
		</div>
	</div>
</template>

<style scoped>
.credit-badge {
	position: absolute;
	z-index: 4;
	display: flex;
	flex-direction: column;
	pointer-events: auto;
}
.credit-badge.bottom-right {
	right: 6px;
	bottom: 6px;
	align-items: flex-end;
}
.credit-badge.bottom-left {
	left: 6px;
	bottom: 6px;
	align-items: flex-start;
}

.cb-trigger {
	width: 20px;
	height: 20px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(20, 20, 20, 0.62);
	color: #fff;
	font-size: 12px;
	line-height: 1;
	cursor: pointer;
	border-radius: 50%;
	transition: background var(--dur-fast) ease;
}
.credit-badge.verified .cb-trigger {
	background: rgba(182, 64, 46, 0.85);
}
.credit-badge:hover .cb-trigger {
	background: var(--ink);
}

.cb-panel {
	position: absolute;
	bottom: 26px;
	width: 250px;
	background: var(--ink);
	color: #fff;
	padding: 11px 13px 12px;
	font-size: 11.5px;
	line-height: 1.65;
	box-shadow: 0 10px 26px rgba(0, 0, 0, 0.28);
	letter-spacing: 0.01em;
}
.credit-badge.bottom-right .cb-panel {
	right: 0;
}
.credit-badge.bottom-left .cb-panel {
	left: 0;
}
.cb-cap {
	font-family: var(--serif);
	font-size: 12.5px;
	padding-bottom: 7px;
	margin-bottom: 7px;
	border-bottom: 1px solid rgba(255, 255, 255, 0.2);
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.cb-line {
	color: rgba(255, 255, 255, 0.9);
}
.cb-link {
	display: block;
	color: #fff;
	text-decoration: underline;
	text-underline-offset: 3px;
	margin-top: 3px;
	opacity: 0.85;
}
.cb-link:hover {
	opacity: 1;
}
.cb-pending {
	color: #f0c674;
	font-weight: 700;
	margin-bottom: 4px;
}
.cb-note {
	color: rgba(255, 255, 255, 0.72);
	font-size: 11px;
	line-height: 1.6;
}
</style>
