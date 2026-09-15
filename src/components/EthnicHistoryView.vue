<script setup lang="ts">
import { computed, ref } from "vue";
import type { EthnicHistory } from "@/api/types";

/**
 * 历史沿革结构化展示（方向 C-2）。
 *
 * 数据来自后端 `history` 字段，只收录**原文明写出具体年份**的段落；
 * 不推断、不归一化模糊年份（如「13世纪初」不会进入时间轴）。
 * 因此这里把「有时间锚点的段落」与「专题叙述段落」分开展示：
 * 前者按原文顺序列为时间轴，后者收进可点击的段落索引，
 * 并在顶部如实标注锚定比例，不用编造的年份把故事串成假年表。
 */
const props = defineProps<{
	history: EthnicHistory;
	name: string;
	theme: string;
}>();

const emit = defineEmits<{ (e: "jump", index: number): void }>();

/** 当前视图：timeline 时间轴 / eras 时代分期 / all 全文索引 */
const view = ref<"timeline" | "eras" | "all">("timeline");

const hasTimeline = computed(() => props.history.timelineCount > 0);
const hasEras = computed(() => props.history.eraCount > 0);

/** 首屏无时间轴时自动落到时代分期，避免出现空面板 */
if (!hasTimeline.value && hasEras.value) {
	view.value = "eras";
}

/** 锚定比例百分比（用于如实说明「其余为专题叙述」） */
const anchoredPct = computed(() =>
	Math.round((props.history.anchoredRatio || 0) * 100),
);

/** 公元前后年份显示：公元前以「前」表示 */
function yearLabel(item: { year: number; yearText: string }) {
	return item.yearText || (item.year < 0 ? `公元前${-item.year}年` : `${item.year}年`);
}

/** 展开/收起长段落 */
const expanded = ref<Set<number>>(new Set());
function toggle(index: number) {
	const next = new Set(expanded.value);
	if (next.has(index)) next.delete(index);
	else next.add(index);
	expanded.value = next;
}
function isLong(text: string) {
	return text.length > 160;
}
function shown(text: string, index: number) {
	return isLong(text) && !expanded.value.has(index)
		? `${text.slice(0, 160)}…`
		: text;
}
</script>

<template>
	<div class="hist" :style="{ '--hist-theme': theme }">
		<!-- 顶部：视图切换 + 结构概览（如实标注锚定比例） -->
		<div class="hist-bar">
			<div class="hist-views" role="tablist">
				<button
					v-if="hasTimeline"
					class="hist-hv"
					:class="{ on: view === 'timeline' }"
					role="tab"
					:aria-selected="view === 'timeline'"
					@click="view = 'timeline'"
				>
					时间轴
					<span class="hist-hv-n">{{ history.timelineCount }}</span>
				</button>
				<button
					v-if="hasEras"
					class="hist-hv"
					:class="{ on: view === 'eras' }"
					role="tab"
					:aria-selected="view === 'eras'"
					@click="view = 'eras'"
				>
					时代分期
					<span class="hist-hv-n">{{ history.eraCount }}</span>
				</button>
				<button
					class="hist-hv"
					:class="{ on: view === 'all' }"
					role="tab"
					:aria-selected="view === 'all'"
					@click="view = 'all'"
				>
					全文索引
					<span class="hist-hv-n">{{ history.paragraphCount }}</span>
				</button>
			</div>
			<p class="hist-note">
				共 {{ history.paragraphCount }} 段，其中 {{ anchoredPct }}%
				含明确年份（{{ history.timelineCount }} 条）已列入时间轴；
				其余为族源、社会形态等专题叙述，收在「全文索引」。
			</p>
		</div>

		<!-- 时间轴：仅原文写明具体年份的段落 -->
		<ol v-if="view === 'timeline'" class="hist-tl">
			<li
				v-for="item in history.timeline"
				:key="item.index"
				class="hist-tl-item"
			>
				<span class="hist-tl-dot" aria-hidden="true"></span>
				<div class="hist-tl-year">{{ yearLabel(item) }}</div>
				<div class="hist-tl-body">
					<p class="hist-tl-text">
						{{ shown(item.text, item.index) }}
					</p>
					<button
						v-if="isLong(item.text)"
						class="hist-tl-more"
						@click="toggle(item.index)"
					>
						{{ expanded.has(item.index) ? "收起" : "展开全文" }}
					</button>
				</div>
			</li>
		</ol>

		<!-- 时代分期：按原文自述的朝代词归类 -->
		<div v-else-if="view === 'eras'" class="hist-eras">
			<section
				v-for="era in history.eras"
				:key="era.name"
				class="hist-era"
			>
				<header class="hist-era-head">
					<h4>{{ era.name }}</h4>
					<span class="hist-era-n">{{ era.paragraphIndexes.length }} 段</span>
				</header>
				<ul class="hist-era-list">
					<li
						v-for="idx in era.paragraphIndexes"
						:key="idx"
						class="hist-era-p"
					>
						<button class="hist-era-jump" @click="emit('jump', idx)">
							{{ shown(history.paragraphs[idx]?.text || "", idx) }}
						</button>
					</li>
				</ul>
			</section>
		</div>

		<!-- 全文索引：全部段落，标注时代与年份 -->
		<div v-else class="hist-allp">
			<article
				v-for="p in history.paragraphs"
				:key="p.index"
				class="hist-ap"
				:id="`hist-p-${p.index}`"
			>
				<div class="hist-ap-meta">
					<span class="hist-ap-idx">#{{ p.index + 1 }}</span>
					<span v-if="p.year !== null" class="hist-ap-year">
						{{ p.year < 0 ? `公元前${-p.year}年` : `${p.year}年` }}
					</span>
					<span v-for="e in p.eras" :key="e" class="hist-ap-era">{{ e }}</span>
					<span v-if="!p.eras.length && p.year === null" class="hist-ap-plain">
						专题叙述
					</span>
				</div>
				<p class="hist-ap-text">{{ shown(p.text, p.index) }}</p>
				<button v-if="isLong(p.text)" class="hist-tl-more" @click="toggle(p.index)">
					{{ expanded.has(p.index) ? "收起" : "展开全文" }}
				</button>
			</article>
		</div>
	</div>
</template>

<style scoped>
.hist {
	--hist-line: color-mix(in srgb, var(--hist-theme) 30%, transparent);
}

.hist-bar {
	display: flex;
	flex-direction: column;
	gap: 10px;
	padding-bottom: 16px;
	margin-bottom: 20px;
	border-bottom: 1px solid var(--line, #e6e2da);
}

.hist-views {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}

.hist-hv {
	display: inline-flex;
	align-items: center;
	gap: 6px;
	padding: 7px 14px;
	border: 1px solid var(--line, #e6e2da);
	border-radius: 999px;
	background: transparent;
	color: var(--ink-2, #55504a);
	font-size: 13px;
	cursor: pointer;
	transition: all 0.18s ease;
}

.hist-hv:hover {
	border-color: var(--hist-theme);
	color: var(--hist-theme);
}

.hist-hv.on {
	border-color: var(--hist-theme);
	background: color-mix(in srgb, var(--hist-theme) 10%, transparent);
	color: var(--hist-theme);
	font-weight: 600;
}

.hist-hv-n {
	font-size: 11px;
	opacity: 0.75;
	font-variant-numeric: tabular-nums;
}

.hist-note {
	margin: 0;
	font-size: 12.5px;
	line-height: 1.7;
	color: var(--ink-3, #8a837a);
}

/* ---------------------------------------------------------------- 时间轴 */
.hist-tl {
	position: relative;
	margin: 0;
	padding: 0 0 0 22px;
	list-style: none;
}

.hist-tl::before {
	content: "";
	position: absolute;
	left: 5px;
	top: 6px;
	bottom: 6px;
	width: 1px;
	background: var(--hist-line);
}

.hist-tl-item {
	position: relative;
	padding: 0 0 22px;
}

.hist-tl-item:last-child {
	padding-bottom: 0;
}

.hist-tl-dot {
	position: absolute;
	left: -22px;
	top: 6px;
	width: 11px;
	height: 11px;
	border-radius: 50%;
	background: var(--hist-theme);
	box-shadow: 0 0 0 3px color-mix(in srgb, var(--hist-theme) 16%, transparent);
}

.hist-tl-year {
	font-size: 15px;
	font-weight: 700;
	color: var(--hist-theme);
	letter-spacing: 0.02em;
	margin-bottom: 6px;
	font-variant-numeric: tabular-nums;
}

.hist-tl-text {
	margin: 0;
	font-size: 15px;
	line-height: 1.95;
	color: var(--ink-1, #2f2b26);
	text-align: justify;
}

.hist-tl-more {
	margin-top: 6px;
	padding: 0;
	border: 0;
	background: none;
	color: var(--hist-theme);
	font-size: 13px;
	cursor: pointer;
}

.hist-tl-more:hover {
	text-decoration: underline;
}

/* ---------------------------------------------------------------- 时代分期 */
.hist-eras {
	display: flex;
	flex-direction: column;
	gap: 18px;
}

.hist-era-head {
	display: flex;
	align-items: baseline;
	gap: 10px;
	margin-bottom: 8px;
}

.hist-era-head h4 {
	margin: 0;
	font-size: 15px;
	color: var(--hist-theme);
}

.hist-era-n {
	font-size: 12px;
	color: var(--ink-3, #8a837a);
}

.hist-era-list {
	margin: 0;
	padding: 0;
	list-style: none;
	display: flex;
	flex-direction: column;
	gap: 8px;
}

.hist-era-p {
	padding-left: 12px;
	border-left: 2px solid var(--hist-line);
}

.era-jump {
	display: block;
	width: 100%;
	padding: 0;
	border: 0;
	background: none;
	text-align: left;
	font-size: 14px;
	line-height: 1.85;
	color: var(--ink-1, #2f2b26);
	cursor: pointer;
}

.hist-era-jump:hover {
	color: var(--hist-theme);
}

/* ---------------------------------------------------------------- 全文索引 */
.hist-allp {
	display: flex;
	flex-direction: column;
	gap: 18px;
}

.hist-ap {
	padding: 14px 16px;
	border-radius: 10px;
	background: color-mix(in srgb, var(--hist-theme) 4%, transparent);
	scroll-margin-top: 140px;
}

.hist-ap-meta {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 8px;
	margin-bottom: 8px;
}

.hist-ap-idx {
	font-size: 11px;
	color: var(--ink-3, #8a837a);
	font-variant-numeric: tabular-nums;
}

.hist-ap-year {
	font-size: 12px;
	font-weight: 600;
	color: var(--hist-theme);
	font-variant-numeric: tabular-nums;
}

.hist-ap-era {
	padding: 1px 8px;
	border-radius: 999px;
	font-size: 11px;
	color: var(--hist-theme);
	background: color-mix(in srgb, var(--hist-theme) 12%, transparent);
}

.hist-ap-plain {
	font-size: 11px;
	color: var(--ink-3, #8a837a);
}

.hist-ap-text {
	margin: 0;
	font-size: 14.5px;
	line-height: 1.9;
	color: var(--ink-1, #2f2b26);
	text-align: justify;
}

@media (max-width: 640px) {
	.hist-tl-text,
	.hist-ap-text {
		font-size: 14px;
		line-height: 1.85;
	}
}
</style>
