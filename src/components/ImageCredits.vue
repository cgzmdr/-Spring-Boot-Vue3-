<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { imageCreditApi } from "@/api/modules";
import type { ImageCredit, ImageCreditStats } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 图片来源汇总（方向 C-4）
 *
 * 放在详情页底部，列出本页所有图片的来源与许可状态。
 *
 * 与前一轮的「参考资料」（`SourceReferences`，讲**文字数据**的出处）分工：
 *   · SourceReferences  → 这条内容的数据从哪来（民族志、普查、非遗名录）
 *   · ImageCredits      → 这条内容的**图片**从哪来、什么许可、是否需要署名
 *
 * 合规提示：CC BY / CC BY-SA 要求署名。本组件如实显示每张图的核实状态，
 * 未核实的明确标为「来源待核」，并给出整体核实进度，便于持续补齐。
 */

const props = defineProps<{
	targetType: "ethnic" | "festival" | "art" | "food" | "topic";
	targetId: string;
}>();

const lang = useLangStore();
const credits = ref<ImageCredit[]>([]);
const stats = ref<ImageCreditStats | null>(null);
const loaded = ref(false);

async function load() {
	if (!props.targetId) return;
	try {
		credits.value = await imageCreditApi.byContent(props.targetType, props.targetId);
	} catch {
		credits.value = [];
	} finally {
		loaded.value = true;
	}
}

onMounted(() => {
	load();
	// 整体核实进度：用于说明「全站还有多少张待核」，失败可忽略
	imageCreditApi
		.stats()
		.then((s) => (stats.value = s))
		.catch(() => {});
});
watch(() => [props.targetType, props.targetId], load);
</script>

<template>
	<section
		v-if="loaded && credits.length"
		class="img-credits"
	>
		<div class="ic-head">
			<h3>{{ lang.pick("图片来源", "Image credits") }}</h3>
			<span class="ic-note">
				{{ lang.pick("本页图片的来源与许可", "Source and licence of images on this page") }}
			</span>
		</div>

		<ul class="ic-list">
			<li
				v-for="c in credits"
				:key="c.imagePath"
				class="ic-item"
				:class="c.creditStatus"
			>
				<span
					class="ic-dot"
					aria-hidden="true"
				/>
				<div class="ic-body">
					<div class="ic-line">
						<span class="ic-cap">{{ c.caption || c.imagePath }}</span>
						<!-- 已核实：显示作者 / 许可 / 来源链接 -->
						<template v-if="c.creditStatus === 'verified'">
							<span class="ic-credit">{{ c.creditLine }}</span>
							<a
								v-if="c.licenseUrl"
								class="ic-link"
								:href="c.licenseUrl"
								target="_blank"
								rel="noopener noreferrer"
								>{{ c.license }}</a
							>
							<a
								v-if="c.sourceUrl"
								class="ic-link"
								:href="c.sourceUrl"
								target="_blank"
								rel="noopener noreferrer"
								>{{ lang.pick("来源", "Source") }} ↗</a
							>
						</template>
						<!-- 未核实：如实标注，不编造 -->
						<span
							v-else-if="c.creditStatus === 'unverified'"
							class="ic-pending"
							>{{ c.creditStatusLabel }}</span
						>
						<span
							v-else
							class="ic-own"
							>{{ c.creditStatusLabel }}</span
						>
					</div>
					<p
						v-if="c.creditStatus !== 'verified' && c.remark"
						class="ic-remark"
					>
						{{ c.remark }}
					</p>
					<p
						v-if="c.sourceSite"
						class="ic-site"
					>
						{{ c.sourceSite }}
					</p>
				</div>
			</li>
		</ul>

		<!-- 核实进度：如实交代全站还有多少张待核 -->
		<div
			v-if="stats"
			class="ic-foot"
		>
			<p>
				{{
					lang.pick(
						`站内共 ${stats.total} 张图片，已核实 ${stats.verified} 张（${stats.verifiedRate}%），待核 ${stats.unverified} 张。`,
						`${stats.total} images in total: ${stats.verified} verified (${stats.verifiedRate}%), ${stats.unverified} pending.`,
					)
				}}
			</p>
			<p class="ic-warn">
				{{
					lang.pick(
						`CC BY / CC BY-SA 许可要求署名；当前仍有 ${stats.pendingAttribution} 张需署名但尚未核实来源，正在逐张补齐。`,
						`CC BY / CC BY-SA licences require attribution; ${stats.pendingAttribution} images still need their source verified.`,
					)
				}}
			</p>
		</div>
	</section>
</template>

<style scoped>
.img-credits {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 22px 26px 20px;
	margin: 28px 0 8px;
}
.ic-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 1px solid var(--line);
	padding-bottom: 12px;
	margin-bottom: 14px;
}
.ic-head h3 {
	font-family: var(--serif);
	font-size: 19px;
	letter-spacing: 0.06em;
}
.ic-note {
	font-size: 11.5px;
	color: var(--muted);
}

.ic-list {
	list-style: none;
	display: flex;
	flex-direction: column;
	gap: 11px;
}
.ic-item {
	display: grid;
	grid-template-columns: 8px minmax(0, 1fr);
	gap: 10px;
	padding-top: 11px;
	border-top: 1px dashed var(--line);
}
.ic-item:first-child {
	border-top: none;
	padding-top: 0;
}
.ic-dot {
	width: 7px;
	height: 7px;
	border-radius: 50%;
	margin-top: 7px;
	background: var(--muted);
}
.ic-item.verified .ic-dot {
	background: var(--accent);
}
.ic-item.unverified .ic-dot {
	background: #d8a33a;
}
.ic-body {
	min-width: 0;
}
.ic-line {
	display: flex;
	flex-wrap: wrap;
	align-items: baseline;
	gap: 9px;
}
.ic-cap {
	font-family: var(--serif);
	font-size: 13.5px;
	letter-spacing: 0.01em;
}
.ic-credit {
	font-size: 12.5px;
	color: var(--ink-soft);
}
.ic-link {
	font-size: 12px;
	color: var(--accent);
	text-decoration: underline;
	text-underline-offset: 3px;
}
.ic-pending {
	font-size: 11px;
	letter-spacing: 0.05em;
	padding: 1px 6px;
	border: 1px dashed #d8a33a;
	color: #a8791c;
	white-space: nowrap;
}
.ic-own {
	font-size: 11px;
	padding: 1px 6px;
	border: 1px solid var(--line);
	color: var(--muted);
}
.ic-remark,
.ic-site {
	font-size: 11.5px;
	color: var(--muted);
	line-height: 1.7;
	margin-top: 4px;
}
.ic-foot {
	margin-top: 16px;
	padding-top: 12px;
	border-top: 1px solid var(--line);
	font-size: 11.5px;
	color: var(--muted);
	line-height: 1.75;
}
.ic-warn {
	color: #a8791c;
	margin-top: 5px;
}

@media (max-width: 560px) {
	.img-credits {
		padding: 18px 16px 16px;
	}
}
</style>
