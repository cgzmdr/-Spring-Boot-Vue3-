<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import PageHead from "@/components/PageHead.vue";
import { autonomousAreaApi } from "@/api/modules";
import type { AutonomousArea, AutonomousAreaDirectory } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 民族自治地方（B-6）
 *
 * 定位：民族区域自治是「多元一体」的**制度落点**。
 * 与已有的「民族分布地图」（讲聚居地）不同，本页讲的是**行政区划事实** ——
 * 5 个自治区、30 个自治州、120 个自治县/旗，共 155 个民族自治地方。
 *
 * 三种浏览方式：按级别 / 按自治民族 / 按省级行政区。
 */

const lang = useLangStore();

const data = ref<AutonomousAreaDirectory | null>(null);
const loading = ref(true);
const errored = ref(false);

/** 浏览维度 */
const view = ref<"level" | "ethnic" | "province">("level");
/** 关键词 */
const keyword = ref("");
/** 展开的省份 */
const openProvince = ref<string | null>(null);
/** 选中的民族 */
const activeEthnic = ref<string | null>(null);

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		data.value = await autonomousAreaApi.list({ keyword: keyword.value || undefined });
	} catch {
		errored.value = true;
		data.value = null;
	} finally {
		loading.value = false;
	}
}

onMounted(load);

/** 选中的民族聚合项 */
const ethnicDetail = computed(
	() => data.value?.ethnics.find((e) => e.ethnic === activeEthnic.value) || null,
);

/** 民族是否可以跳转到民族详情 */
function hasEthnicLink(e: { matchedSlug: string | null }): boolean {
	return !!e.matchedSlug;
}

function goEthnic(slug: string | null) {
	if (!slug) return;
	// 民族详情按 id 路由；此处用库里匹配到的 slug 无法直接取 id，
	// 故用搜索页承载跳转（搜索按 slug/名称均可命中）
	window.location.hash = `#/search?q=${encodeURIComponent(slug)}`;
}

/** 某个自治地方的冠名民族是否在内容库中有对应民族 */
function ethnoLabel(a: AutonomousArea): string {
	return a.ethnicGroups.join("、");
}

const totalShown = computed(() =>
	(data.value?.levels || []).reduce((s, l) => s + l.count, 0),
);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<router-link to="/ethnic">{{ lang.pick("民族", "Ethnic Groups") }}</router-link> ›
			<span>{{ lang.pick("民族自治地方", "Autonomous Areas") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('AUTONOMOUS AREAS · 民族自治地方', 'AUTONOMOUS AREAS')"
			:title="lang.pick('民族自治地方', 'Ethnic Autonomous Areas')"
			:dek="
				lang.pick(
					'民族区域自治是中国的基本政治制度。全国共设立 5 个自治区、30 个自治州、120 个自治县（旗），共 155 个民族自治地方。',
					'Regional ethnic autonomy is a basic political system of China. There are 5 autonomous regions, 30 autonomous prefectures and 120 autonomous counties/banners — 155 in total.',
				)
			"
		/>

		<el-skeleton
			v-if="loading"
			:rows="8"
			animated
		/>

		<div
			v-else-if="errored || !data"
			class="ghost-block"
			style="text-align: center"
		>
			<el-empty :description="lang.pick('自治地方数据加载失败', 'Failed to load data')">
				<el-button
					type="primary"
					@click="load"
					>{{ lang.pick("重新加载", "Retry") }}</el-button
				>
			</el-empty>
		</div>

		<template v-else>
			<!-- 概览 -->
			<div class="metrics aa-metrics">
				<div class="metric">
					<div class="num">{{ data.summary.total }}</div>
					<div class="lbl">{{ lang.pick("个民族自治地方", "Autonomous areas") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.regionCount }}</div>
					<div class="lbl">{{ lang.pick("个自治区", "Autonomous regions") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.prefectureCount }}</div>
					<div class="lbl">{{ lang.pick("个自治州", "Autonomous prefectures") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.countyCount }}</div>
					<div class="lbl">{{ lang.pick("个自治县 / 旗", "Autonomous counties") }}</div>
				</div>
			</div>

			<!-- 浏览维度切换 + 搜索 -->
			<div class="filter">
				<div class="row">
					<span class="label">{{ lang.pick("浏览", "Browse") }}</span>
					<button
						class="chip"
						:class="{ active: view === 'level' }"
						@click="view = 'level'"
					>
						{{ lang.pick("按级别", "By level") }}
					</button>
					<button
						class="chip"
						:class="{ active: view === 'ethnic' }"
						@click="view = 'ethnic'"
					>
						{{ lang.pick("按自治民族", "By ethnic group") }}
					</button>
					<button
						class="chip"
						:class="{ active: view === 'province' }"
						@click="view = 'province'"
					>
						{{ lang.pick("按省级行政区", "By province") }}
					</button>
				</div>
				<div class="row">
					<span class="label">{{ lang.pick("搜索", "Search") }}</span>
					<el-input
						v-model="keyword"
						:placeholder="lang.pick('输入自治地方名称', 'Search by name')"
						clearable
						style="width: 260px"
						@keyup.enter="load"
						@clear="load"
					/>
					<el-button
						type="primary"
						@click="load"
						>{{ lang.pick("搜索", "Search") }}</el-button
					>
					<span
						v-if="keyword"
						class="aa-hit"
					>
						{{ lang.pick(`匹配 ${totalShown} 个`, `${totalShown} matched`) }}
					</span>
				</div>
			</div>

			<!-- ========== 按级别 ========== -->
			<template v-if="view === 'level'">
				<section
					v-for="g in data.levels"
					:key="g.level"
					class="aa-section"
				>
					<div class="panel-head">
						<h3>{{ g.label }}</h3>
						<span class="sh-note">{{ g.count }} {{ lang.pick("个", "areas") }}</span>
					</div>

					<!-- 自治区：卡片式（数量少、层级高） -->
					<div
						v-if="g.level === 'autonomous_region'"
						class="aa-region-grid"
					>
						<article
							v-for="a in g.areas"
							:key="a.name"
							class="aa-region-card"
						>
							<h4>{{ a.name }}</h4>
							<div class="arc-ethnics">
								<span
									v-for="e in a.ethnicGroups"
									:key="e"
									class="arc-ethnic"
									>{{ e }}</span
								>
							</div>
							<div class="arc-meta">
								<span v-if="a.seat">{{ lang.pick("首府", "Seat") }}：{{ a.seat }}</span>
								<span v-if="a.establishedYear"> · {{ a.establishedYear }} {{ lang.pick("年成立", "") }}</span>
							</div>
						</article>
					</div>

					<!-- 自治州：列表 -->
					<div
						v-else-if="g.level === 'autonomous_prefecture'"
						class="aa-list"
					>
						<div
							v-for="a in g.areas"
							:key="a.name"
							class="aa-row"
						>
							<span class="ar-name">{{ a.name }}</span>
							<span class="ar-ethnics">{{ ethnoLabel(a) }}</span>
							<span class="ar-province">{{ a.province }}</span>
							<span
								v-if="a.establishedYear"
								class="ar-year"
								>{{ a.establishedYear }}</span
							>
						</div>
					</div>

					<!-- 自治县：紧凑多列 -->
					<div
						v-else
						class="aa-county-grid"
					>
						<div
							v-for="a in g.areas"
							:key="a.name"
							class="aa-county"
							:title="`${a.name}｜${ethnoLabel(a)}｜${a.province || ''}`"
						>
							<span class="ac-name">{{ a.name }}</span>
							<span class="ac-province">{{ a.province }}</span>
						</div>
					</div>
				</section>
			</template>

			<!-- ========== 按自治民族 ========== -->
			<template v-else-if="view === 'ethnic'">
				<div class="aa-ethnic-grid">
					<button
						v-for="e in data.ethnics"
						:key="e.ethnic"
						class="aa-ethnic-card"
						:class="{ active: activeEthnic === e.ethnic, linked: hasEthnicLink(e) }"
						:style="e.themeColor ? { borderTopColor: e.themeColor } : undefined"
						@click="activeEthnic = activeEthnic === e.ethnic ? null : e.ethnic"
					>
						<span class="aec-name">{{ e.ethnic }}</span>
						<span class="aec-count">{{ e.count }}</span>
						<span
							v-if="hasEthnicLink(e)"
							class="aec-link"
							>{{ lang.pick("有民族档案", "Profile") }}</span
						>
					</button>
				</div>

				<section
					v-if="ethnicDetail"
					class="aa-section"
				>
					<div class="panel-head">
						<h3>{{ ethnicDetail.ethnic }}{{ lang.pick("冠名的自治地方", " autonomous areas") }}</h3>
						<span class="sh-note">{{ ethnicDetail.count }} {{ lang.pick("个", "areas") }}</span>
						<button
							v-if="hasEthnicLink(ethnicDetail)"
							class="chip aec-goto"
							@click="goEthnic(ethnicDetail.matchedSlug)"
						>
							{{ lang.pick("查看该民族档案 →", "Open ethnic profile →") }}
						</button>
					</div>
					<ul class="aa-detail-list">
						<li
							v-for="a in ethnicDetail.areas"
							:key="a.name"
						>
							<span class="adl-level">{{ a.levelLabel }}</span>
							<span class="adl-name">{{ a.name }}</span>
							<span class="adl-province">{{ a.province }}</span>
						</li>
					</ul>
				</section>
			</template>

			<!-- ========== 按省级行政区 ========== -->
			<template v-else>
				<div class="aa-prov-list">
					<div
						v-for="p in data.provinces"
						:key="p.province"
						class="aa-prov-item"
					>
						<button
							class="aa-prov-head"
							:class="{ open: openProvince === p.province }"
							@click="openProvince = openProvince === p.province ? null : p.province"
						>
							<span class="aph-name">{{ p.province }}</span>
							<span class="aph-count">
								{{ p.areas.length }} {{ lang.pick("个自治地方", "areas") }}
							</span>
							<span class="aph-toggle">{{ openProvince === p.province ? "−" : "+" }}</span>
						</button>
						<div
							v-if="openProvince === p.province"
							class="aa-prov-body"
						>
							<template v-if="p.areas.length">
								<span
									v-for="a in p.areas"
									:key="a.name"
									class="apb-item"
								>
									<span class="apb-name">{{ a.name }}</span>
									<span class="apb-eth">{{ ethnoLabel(a) }}</span>
								</span>
							</template>
							<span
								v-else
								class="apb-empty"
								>{{ lang.pick("（自治区本级）", "(region itself)") }}</span
							>
						</div>
					</div>
				</div>
			</template>

			<p class="aa-note">
				{{
					lang.pick(
						"注：一个自治地方可能冠名多个自治民族（如「双江拉祜族佤族布朗族傣族自治县」），在「按自治民族」视图中会分别计入各民族，因此该维度计数之和大于 155。内蒙古的县级自治地方称「自治旗」。",
						'Note: an area may be named after several ethnic groups, so counts in the "by ethnic group" view exceed 155. County-level autonomous areas in Inner Mongolia are called "banners".',
					)
				}}
			</p>
		</template>
	</div>
</template>

<style scoped>
.aa-metrics {
	margin: 4px 0 22px;
}
.aa-hit {
	font-size: 12px;
	color: var(--accent);
	margin-left: 6px;
}

.aa-section {
	margin-bottom: 30px;
}
.aa-section .panel-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 2px solid var(--ink);
	padding-bottom: 10px;
	margin-bottom: 16px;
	flex-wrap: wrap;
}
.aa-section .panel-head h3 {
	font-family: var(--serif);
	font-size: 21px;
	letter-spacing: 0.06em;
}
.aa-section .sh-note {
	font-size: 11.5px;
	color: var(--muted);
}
.aec-goto {
	margin-left: auto;
	text-decoration: none;
	color: var(--accent);
	border-color: var(--accent);
	font-size: 12px;
}
.aec-goto:hover {
	background: var(--accent);
	color: #fff;
}

/* 自治区卡片 */
.aa-region-grid {
	display: grid;
	grid-template-columns: repeat(5, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.aa-region-card {
	background: var(--paper);
	padding: 16px 16px 18px;
	border-top: 3px solid var(--accent);
}
.aa-region-card h4 {
	font-family: var(--serif);
	font-size: 17px;
	letter-spacing: 0.02em;
	margin-bottom: 10px;
}
.arc-ethnics {
	display: flex;
	flex-wrap: wrap;
	gap: 5px;
	margin-bottom: 9px;
}
.arc-ethnic {
	font-size: 11.5px;
	padding: 1px 7px;
	background: color-mix(in srgb, var(--accent) 9%, transparent);
	color: var(--accent);
}
.arc-meta {
	font-size: 11.5px;
	color: var(--muted);
	line-height: 1.7;
}

/* 自治州列表 */
.aa-list {
	border: 1px solid var(--line);
	background: var(--paper);
}
.aa-row {
	display: grid;
	grid-template-columns: minmax(0, 1.6fr) minmax(0, 1.1fr) minmax(0, 1fr) 56px;
	gap: 12px;
	align-items: baseline;
	padding: 9px 16px;
	border-bottom: 1px solid var(--line);
	font-size: 13.5px;
}
.aa-row:last-child {
	border-bottom: none;
}
.aa-row:hover {
	background: var(--paper-2);
}
.ar-name {
	font-family: var(--serif);
	font-size: 14.5px;
}
.ar-ethnics {
	color: var(--accent);
	font-size: 12.5px;
}
.ar-province {
	color: var(--muted);
	font-size: 12.5px;
}
.ar-year {
	color: var(--muted);
	font-size: 12px;
	text-align: right;
	font-variant-numeric: tabular-nums;
}

/* 自治县紧凑网格 */
.aa-county-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.aa-county {
	background: var(--paper);
	padding: 8px 12px;
	display: flex;
	flex-direction: column;
	gap: 2px;
	min-width: 0;
	transition: background var(--dur-fast) ease;
}
.aa-county:hover {
	background: var(--paper-2);
}
.ac-name {
	font-family: var(--serif);
	font-size: 13px;
	line-height: 1.4;
}
.ac-province {
	font-size: 11px;
	color: var(--muted);
}

/* 按民族 */
.aa-ethnic-grid {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin-bottom: 26px;
}
.aa-ethnic-card {
	background: var(--paper);
	border: none;
	border-top: 3px solid var(--line);
	padding: 11px 13px 12px;
	cursor: pointer;
	font-family: var(--font-sans);
	text-align: left;
	display: flex;
	flex-wrap: wrap;
	align-items: baseline;
	gap: 6px;
	transition: background var(--dur-fast) ease;
}
.aa-ethnic-card:hover {
	background: var(--paper-2);
}
.aa-ethnic-card.active {
	background: color-mix(in srgb, var(--accent) 8%, var(--paper));
}
.aec-name {
	font-family: var(--serif);
	font-size: 14.5px;
	letter-spacing: 0.02em;
}
.aec-count {
	margin-left: auto;
	font-family: var(--serif);
	font-weight: 700;
	color: var(--accent);
	font-variant-numeric: tabular-nums;
}
.aec-link {
	font-size: 10px;
	color: var(--muted);
	border: 1px solid var(--line);
	padding: 0 4px;
	width: 100%;
}
.aa-ethnic-card.linked .aec-link {
	color: var(--accent);
	border-color: color-mix(in srgb, var(--accent) 45%, var(--line));
}

.aa-detail-list {
	list-style: none;
	border: 1px solid var(--line);
	background: var(--paper);
}
.aa-detail-list li {
	display: grid;
	grid-template-columns: 84px minmax(0, 1fr) minmax(0, 1fr);
	gap: 12px;
	align-items: baseline;
	padding: 9px 16px;
	border-bottom: 1px solid var(--line);
	font-size: 13.5px;
}
.aa-detail-list li:last-child {
	border-bottom: none;
}
.adl-level {
	font-size: 11px;
	color: var(--accent);
	border: 1px solid var(--line);
	padding: 1px 6px;
	text-align: center;
}
.adl-name {
	font-family: var(--serif);
}
.adl-province {
	color: var(--muted);
	font-size: 12.5px;
}

/* 按省份 */
.aa-prov-list {
	border: 1px solid var(--line);
	background: var(--paper);
}
.aa-prov-item + .aa-prov-item {
	border-top: 1px solid var(--line);
}
.aa-prov-head {
	display: grid;
	grid-template-columns: minmax(0, 1fr) auto 24px;
	align-items: center;
	gap: 14px;
	width: 100%;
	background: none;
	border: none;
	padding: 13px 18px;
	cursor: pointer;
	font-family: var(--font-sans);
	text-align: left;
	transition: background var(--dur-fast) ease;
}
.aa-prov-head:hover {
	background: var(--paper-2);
}
.aa-prov-head.open {
	background: color-mix(in srgb, var(--accent) 6%, var(--paper));
}
.aph-name {
	font-family: var(--serif);
	font-size: 16px;
	letter-spacing: 0.03em;
}
.aph-count {
	font-size: 12px;
	color: var(--muted);
	font-variant-numeric: tabular-nums;
}
.aph-toggle {
	font-family: var(--serif);
	font-size: 18px;
	color: var(--accent);
	text-align: center;
}
.aa-prov-body {
	padding: 12px 18px 16px;
	background: var(--paper-2);
	border-top: 1px dashed var(--line);
	display: flex;
	flex-wrap: wrap;
	gap: 7px;
}
.apb-item {
	display: inline-flex;
	align-items: baseline;
	gap: 6px;
	background: var(--paper);
	border: 1px solid var(--line);
	padding: 4px 10px;
}
.apb-name {
	font-family: var(--serif);
	font-size: 13px;
}
.apb-eth {
	font-size: 11px;
	color: var(--accent);
}
.apb-empty {
	font-size: 12.5px;
	color: var(--muted);
}

.aa-note {
	font-size: 12.5px;
	color: var(--muted);
	line-height: 1.85;
	margin: 22px 0 8px;
	padding-left: 12px;
	border-left: 2px solid var(--line);
}

@media (max-width: 1100px) {
	.aa-region-grid {
		grid-template-columns: repeat(3, minmax(0, 1fr));
	}
	.aa-county-grid {
		grid-template-columns: repeat(3, minmax(0, 1fr));
	}
}
@media (max-width: 900px) {
	.aa-region-grid,
	.aa-county-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
	.aa-row {
		grid-template-columns: minmax(0, 1fr) auto;
		row-gap: 4px;
	}
	.ar-province,
	.ar-year {
		font-size: 11.5px;
	}
	.aa-detail-list li {
		grid-template-columns: 78px minmax(0, 1fr);
	}
	.adl-province {
		grid-column: 2;
	}
}
@media (max-width: 560px) {
	.aa-region-grid,
	.aa-county-grid {
		grid-template-columns: minmax(0, 1fr);
	}
}
</style>
