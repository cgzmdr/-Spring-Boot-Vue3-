<script setup lang="ts">
import { computed } from "vue";
import type { EthnicPopulationStats } from "@/api/types";
import { formatNumber } from "@/utils/format";
import { useLangStore } from "@/stores/lang";

/**
 * 民族人口可视化（第七次全国人口普查口径）
 *
 * 纯 CSS/SVG 实现，不引入图表库：
 * · 数据结构固定（56 条），用条形 + 环形即可表达清楚；
 * · 与 C 端「杂志编辑风」的排版语言一致（细线、衬线数字、无多余装饰）。
 */

const props = defineProps<{
	stats: EthnicPopulationStats | null;
}>();

const lang = useLangStore();

/** 榜单最大值，用于条形长度归一 */
const topMax = computed(() =>
	Math.max(1, ...(props.stats?.topGroups || []).map((g) => g.population)),
);

const families = computed(() => props.stats?.languageFamilies || []);
const regions = computed(() => props.stats?.regions || []);
const buckets = computed(() => props.stats?.buckets || []);

const regionMax = computed(() => Math.max(1, ...regions.value.map((r) => r.population)));

/** 环形图：语系人口占比（取前 5 大 + 其他） */
const donut = computed(() => {
	const items = families.value.slice(0, 5);
	const rest = families.value.slice(5);
	const otherPop = rest.reduce((s, r) => s + r.population, 0);
	const total = props.stats?.totalPopulation || 1;

	const segments = items.map((f) => ({
		name: f.name,
		population: f.population,
		ratio: f.population / total,
	}));
	if (otherPop > 0) {
		segments.push({ name: lang.pick("其他语系", "Other"), population: otherPop, ratio: otherPop / total });
	}

	// 用 SVG stroke-dasharray 绘制圆环
	const R = 54;
	const C = 2 * Math.PI * R;
	let offset = 0;
	return segments.map((s, i) => {
		const len = Math.max(0, s.ratio * C);
		const seg = {
			...s,
			index: i,
			circumference: C,
			dash: `${len} ${C - len}`,
			dashOffset: -offset,
		};
		offset += len;
		return seg;
	});
});

/** 语系配色（与全站色板协调的暖色序列） */
const DONUT_COLORS = ["#b6402e", "#c9772e", "#8a8b52", "#3f6d6a", "#6b5b7b", "#a8a49b"];

/** 人口规模分档的最大数量，用于横向条归一 */
const bucketMax = computed(() => Math.max(1, ...buckets.value.map((b) => b.groupCount)));
</script>

<template>
	<div
		v-if="stats"
		class="pop-viz"
	>
		<!-- 概览指标 -->
		<div class="metrics">
			<div class="metric">
				<div class="num">{{ formatNumber(stats.totalPopulation) }}</div>
				<div class="lbl">{{ lang.pick(`${stats.censusYear} 年普查总人口`, `Census ${stats.censusYear}`) }}</div>
			</div>
			<div class="metric">
				<div class="num">{{ stats.totalGroups }}</div>
				<div class="lbl">{{ lang.pick("个民族", "Groups") }}</div>
			</div>
			<div class="metric">
				<div class="num">{{ stats.largestGroupName || "—" }}</div>
				<div class="lbl">{{ lang.pick("人口最多的民族", "Largest group") }}</div>
			</div>
			<div class="metric">
				<div class="num">{{ formatNumber(stats.largestGroupPopulation) }}</div>
				<div class="lbl">{{ lang.pick("该民族人口", "Its population") }}</div>
			</div>
		</div>

		<div class="viz-grid">
			<!-- 人口 Top 10 -->
			<section class="viz-card">
				<h4>{{ lang.pick(`人口前 ${stats.topGroups.length} 位民族`, `Top ${stats.topGroups.length} by population`) }}</h4>
				<div class="bars">
					<div
						v-for="(g, i) in stats.topGroups"
						:key="g.name"
						class="bar-row"
					>
						<span class="rank">{{ String(i + 1).padStart(2, "0") }}</span>
						<span class="name">{{ g.name }}</span>
						<span class="track">
							<span
								class="fill"
								:style="{ width: `${Math.max(2, (g.population / topMax) * 100)}%` }"
							/>
						</span>
						<span class="val">{{ formatNumber(g.population) }}</span>
					</div>
				</div>
			</section>

			<!-- 语系分布环形图 -->
			<section class="viz-card">
				<h4>{{ lang.pick("语系人口构成", "Language families") }}</h4>
				<div class="donut-wrap">
					<svg
						class="donut"
						viewBox="0 0 140 140"
						role="img"
						:aria-label="lang.pick('语系人口构成环形图', 'Language family composition')"
					>
						<g transform="translate(70,70) rotate(-90)">
							<circle
								r="54"
								class="donut-bg"
							/>
							<circle
								v-for="s in donut"
								:key="s.name"
								r="54"
								class="donut-seg"
								:stroke="DONUT_COLORS[s.index % DONUT_COLORS.length]"
								:stroke-dasharray="s.dash"
								:stroke-dashoffset="s.dashOffset"
							/>
						</g>
						<text
							x="70"
							y="66"
							class="donut-num"
						>
							{{ stats.totalGroups }}
						</text>
						<text
							x="70"
							y="80"
							class="donut-sub"
						>
							{{ lang.pick("个民族", "groups") }}
						</text>
					</svg>
					<ul class="legend">
						<li
							v-for="(s, i) in donut"
							:key="s.name"
						>
							<span
								class="dot"
								:style="{ background: DONUT_COLORS[i % DONUT_COLORS.length] }"
							/>
							<span class="lg-name">{{ s.name }}</span>
							<span class="lg-val">{{ (s.ratio * 100).toFixed(1) }}%</span>
						</li>
					</ul>
				</div>
			</section>

			<!-- 聚居地域 -->
			<section class="viz-card">
				<h4>{{ lang.pick("按聚居地域分布", "By settlement region") }}</h4>
				<div class="bars compact">
					<div
						v-for="r in regions.slice(0, 9)"
						:key="r.name"
						class="bar-row"
					>
						<span class="name wide">{{ r.name }}</span>
						<span class="track">
							<span
								class="fill alt"
								:style="{ width: `${Math.max(2, (r.population / regionMax) * 100)}%` }"
							/>
						</span>
						<span class="val">{{ r.groupCount }} {{ lang.pick("族", "grp") }}</span>
					</div>
				</div>
				<p class="viz-note">
					{{ lang.pick("一个民族可分布于多个地域，故各族计数之和大于 56。", "A group may span several regions, so counts sum above 56.") }}
				</p>
			</section>

			<!-- 人口规模分档 -->
			<section class="viz-card">
				<h4>{{ lang.pick("人口规模分档", "Population size bands") }}</h4>
				<div class="buckets">
					<div
						v-for="b in buckets"
						:key="b.label"
						class="bucket"
					>
						<div class="bk-label">{{ b.label }}</div>
						<div class="bk-bar">
							<span
								class="bk-fill"
								:style="{ width: `${Math.max(4, (b.groupCount / bucketMax) * 100)}%` }"
							/>
						</div>
						<div class="bk-meta">
							<b>{{ b.groupCount }}</b> {{ lang.pick("族", "groups") }} ·
							{{ formatNumber(b.population) }}
						</div>
					</div>
				</div>
			</section>
		</div>
	</div>
</template>

<style scoped>
.pop-viz {
	margin-top: 4px;
}
.metrics {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin-bottom: 22px;
}
.metrics .metric {
	background: var(--paper);
	padding: 16px 18px;
}
.metrics .num {
	font-family: var(--serif);
	font-size: 21px;
	font-weight: 700;
	color: var(--accent);
	font-variant-numeric: tabular-nums;
	line-height: 1.25;
}
.metrics .lbl {
	font-size: 11px;
	letter-spacing: 0.08em;
	color: var(--muted);
	text-transform: uppercase;
	margin-top: 4px;
}
.viz-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 20px;
}
.viz-card {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 20px 22px 22px;
	min-width: 0;
}
.viz-card h4 {
	font-family: var(--serif);
	font-size: 18px;
	letter-spacing: 0.04em;
	padding-bottom: 12px;
	border-bottom: 1px solid var(--line);
	margin-bottom: 16px;
}

/* ---- 条形 ---- */
.bars {
	display: flex;
	flex-direction: column;
	gap: 9px;
}
.bar-row {
	display: grid;
	grid-template-columns: 22px 62px 1fr auto;
	align-items: center;
	gap: 9px;
	font-size: 13px;
}
.bars.compact .bar-row {
	grid-template-columns: 92px 1fr auto;
}
.bar-row .rank {
	font-family: var(--serif);
	font-size: 11px;
	font-weight: 700;
	color: var(--accent);
}
.bar-row .name {
	font-family: var(--serif);
	letter-spacing: 0.02em;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.bar-row .name.wide {
	font-size: 12.5px;
}
.bar-row .track {
	height: 11px;
	background: var(--paper-2);
	border: 1px solid var(--line);
	overflow: hidden;
}
.bar-row .fill {
	display: block;
	height: 100%;
	background: var(--accent);
	transition: width var(--dur-slow) var(--ease-out);
}
.bar-row .fill.alt {
	background: var(--ink);
	opacity: 0.72;
}
.bar-row .val {
	font-size: 12px;
	color: var(--muted);
	font-variant-numeric: tabular-nums;
	white-space: nowrap;
}

/* ---- 环形图 ---- */
.donut-wrap {
	display: flex;
	align-items: center;
	gap: 18px;
}
.donut {
	width: 150px;
	height: 150px;
	flex: none;
}
.donut-bg {
	fill: none;
	stroke: var(--paper-2);
	stroke-width: 20;
}
.donut-seg {
	fill: none;
	stroke-width: 20;
	transition: stroke-dasharray var(--dur-slow) var(--ease-out);
}
.donut-num {
	text-anchor: middle;
	font-family: var(--serif);
	font-size: 22px;
	font-weight: 700;
	fill: var(--ink);
}
.donut-sub {
	text-anchor: middle;
	font-size: 9px;
	fill: var(--muted);
}
.legend {
	list-style: none;
	display: flex;
	flex-direction: column;
	gap: 8px;
	min-width: 0;
	flex: 1;
}
.legend li {
	display: grid;
	grid-template-columns: 10px 1fr auto;
	align-items: center;
	gap: 8px;
	font-size: 12.5px;
}
.legend .dot {
	width: 10px;
	height: 10px;
	display: block;
}
.legend .lg-name {
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	color: var(--ink-soft);
}
.legend .lg-val {
	color: var(--muted);
	font-variant-numeric: tabular-nums;
}

/* ---- 分档 ---- */
.buckets {
	display: flex;
	flex-direction: column;
	gap: 13px;
}
.bucket .bk-label {
	font-size: 12.5px;
	color: var(--ink-soft);
	margin-bottom: 5px;
}
.bucket .bk-bar {
	height: 9px;
	background: var(--paper-2);
	border: 1px solid var(--line);
	overflow: hidden;
}
.bucket .bk-fill {
	display: block;
	height: 100%;
	background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent) 55%, var(--ink)));
}
.bucket .bk-meta {
	font-size: 11.5px;
	color: var(--muted);
	margin-top: 5px;
	font-variant-numeric: tabular-nums;
}
.bucket .bk-meta b {
	color: var(--accent);
	font-family: var(--serif);
	font-size: 13px;
}
.viz-note {
	font-size: 11.5px;
	color: var(--muted);
	line-height: 1.7;
	margin-top: 12px;
}
@media (max-width: 900px) {
	.metrics {
		grid-template-columns: repeat(2, 1fr);
	}
	.viz-grid {
		grid-template-columns: 1fr;
	}
	.donut-wrap {
		flex-direction: column;
		align-items: flex-start;
	}
}
</style>
