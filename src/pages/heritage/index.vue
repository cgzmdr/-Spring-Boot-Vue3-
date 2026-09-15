<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import PageHead from "@/components/PageHead.vue";
import CoverImage from "@/components/CoverImage.vue";
import AppPagination from "@/components/AppPagination.vue";
import { artApi, ethnicApi } from "@/api/modules";
import type { ArtListItem, EthnicBrief, HeritageStats } from "@/api/types";
import { artCategoryLabel, heritageLabel, artImagePrompt } from "@/utils/format";
import { useLangStore } from "@/stores/lang";

/**
 * 非物质文化遗产名录（B-4）
 *
 * 定位：把原先「艺术频道的一个筛选标签」升级为**可检索的名录**。
 * 与 /art 艺术频道的分工：
 * · /art       —— 按类别浏览艺术形式（音乐/舞蹈/戏剧…），偏「鉴赏」
 * · /heritage  —— 按非遗级别成体系检索（世界级/国家级），突出**传承人**与名录属性
 */

const lang = useLangStore();

const list = ref<ArtListItem[]>([]);
const total = ref(0);
const page = ref(0);
const size = 12;
const level = ref("");
const category = ref("");
const ethnicId = ref("");
const keyword = ref("");
const loading = ref(false);
const errored = ref(false);
const stats = ref<HeritageStats | null>(null);
const ethnics = ref<EthnicBrief[]>([]);

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		const res = await artApi.list({
			page: page.value,
			size,
			category: category.value || undefined,
			intangibleHeritage: level.value || undefined,
			ethnicGroupId: ethnicId.value || undefined,
			keyword: keyword.value || undefined,
		});
		list.value = res.data;
		total.value = res.total;
	} catch {
		errored.value = true;
		list.value = [];
		total.value = 0;
	} finally {
		loading.value = false;
	}
}

onMounted(async () => {
	// 统计与民族清单为「锦上添花」，失败不阻塞名录主体
	artApi.heritageStats().then((s) => (stats.value = s)).catch(() => {});
	ethnicApi.all().then((e) => (ethnics.value = e)).catch(() => {});
	load();
});

function pickLevel(v: string) {
	level.value = level.value === v ? "" : v;
	page.value = 0;
	load();
}
function pickCategory(v: string) {
	category.value = category.value === v ? "" : v;
	page.value = 0;
	load();
}
function onEthnicChange(v: string) {
	ethnicId.value = v;
	page.value = 0;
	load();
}
function doSearch() {
	page.value = 0;
	load();
}
function reset() {
	level.value = "";
	category.value = "";
	ethnicId.value = "";
	keyword.value = "";
	page.value = 0;
	load();
}
function onPage(p: number) {
	page.value = p;
	load();
	window.scrollTo({ top: 0, behavior: "smooth" });
}

/** 是否有任一筛选条件生效 */
const hasFilter = computed(
	() => !!(level.value || category.value || ethnicId.value || keyword.value),
);

/** 传承人覆盖率（用于概览区提示数据完整度） */
const coverage = computed(() => {
	const s = stats.value;
	if (!s || !s.total) return 0;
	return Math.round((s.withInheritor / s.total) * 100);
});

/** 统计概览中的类别最大值，用于条形归一 */
const categoryMax = computed(() =>
	Math.max(1, ...(stats.value?.categories || []).map((c) => c.count)),
);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<router-link to="/art">{{ lang.pick("艺术", "Arts") }}</router-link> ›
			<span>{{ lang.pick("非遗名录", "Heritage List") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('INTANGIBLE HERITAGE · 非遗名录', 'INTANGIBLE HERITAGE')"
			:title="lang.pick('非物质文化遗产名录', 'Intangible Cultural Heritage')"
			:dek="
				lang.pick(
					'以名录的方式检索各民族的非物质文化遗产：按级别、类别、所属民族逐层查看，并了解每一项技艺背后的传承人。',
					'Browse the intangible cultural heritage of China\u2019s ethnic groups by level, category and group, and meet the inheritors behind each tradition.',
				)
			"
		/>

		<!-- 统计概览 -->
		<section
			v-if="stats"
			class="hd-overview"
		>
			<div class="metrics">
				<div class="metric">
					<div class="num">{{ stats.total }}</div>
					<div class="lbl">{{ lang.pick("个非遗项目", "Heritage items") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ stats.ethnicCount }}</div>
					<div class="lbl">{{ lang.pick("个民族", "Ethnic groups") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ stats.withInheritor }}</div>
					<div class="lbl">{{ lang.pick("项有传承人记录", "With inheritors") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ coverage }}%</div>
					<div class="lbl">{{ lang.pick("传承人覆盖率", "Coverage") }}</div>
				</div>
			</div>

			<div class="hd-dist">
				<!-- 级别分布 -->
				<div class="dist-block">
					<h4>{{ lang.pick("按级别", "By level") }}</h4>
					<button
						v-for="l in stats.levels"
						:key="l.code"
						class="dist-row"
						:class="{ active: level === l.code }"
						@click="pickLevel(l.code)"
					>
						<span
							class="dot"
							:class="l.code"
						/>
						<span class="d-name">{{ l.label }}</span>
						<span class="d-count">{{ l.count }}</span>
					</button>
				</div>

				<!-- 类别分布 -->
				<div class="dist-block wide">
					<h4>{{ lang.pick("按类别", "By category") }}</h4>
					<button
						v-for="c in stats.categories"
						:key="c.code"
						class="dist-bar"
						:class="{ active: category === c.code }"
						@click="pickCategory(c.code)"
					>
						<span class="b-name">{{ c.label }}</span>
						<span class="b-track">
							<span
								class="b-fill"
								:style="{ width: `${Math.max(3, (c.count / categoryMax) * 100)}%` }"
							/>
						</span>
						<span class="b-count">{{ c.count }}</span>
					</button>
				</div>
			</div>
		</section>

		<!-- 传承人聚焦 -->
		<section
			v-if="stats?.spotlight?.length"
			class="spotlight"
		>
			<div class="panel-head">
				<h3>{{ lang.pick("传承人聚焦", "Inheritors in focus") }}</h3>
				<span class="sh-note">{{ lang.pick("记录传承人最多的项目", "Items with the most recorded inheritors") }}</span>
			</div>
			<div class="sp-grid">
				<router-link
					v-for="s in stats.spotlight.slice(0, 8)"
					:key="s.id"
					class="sp-card"
					:to="`/art/${s.id}`"
				>
					<div class="sp-head">
						<span class="sp-name">{{ s.name }}</span>
						<span
							class="sp-level"
							:class="s.intangibleHeritage"
							>{{ heritageLabel[s.intangibleHeritage] || "" }}</span
					>
					</div>
					<div class="sp-eth">{{ s.ethnicGroupName }}</div>
					<div class="sp-inh">
						<span
							v-for="p in s.inheritors"
							:key="p"
							class="inh-chip"
							>{{ p }}</span
						>
					</div>
				</router-link>
			</div>
		</section>

		<!-- 筛选 -->
		<div class="filter">
			<div class="row">
				<span class="label">{{ lang.pick("级别", "Level") }}</span>
				<button
					v-for="l in stats?.levels || []"
					:key="l.code"
					class="chip"
					:class="{ active: level === l.code }"
					@click="pickLevel(l.code)"
				>
					{{ l.label }}（{{ l.count }}）
				</button>
				<button
					v-if="level"
					class="chip"
					@click="pickLevel(level)"
				>
					✕ {{ lang.pick("清除", "Clear") }}
				</button>
			</div>
			<div class="row">
				<span class="label">{{ lang.pick("类别", "Category") }}</span>
				<button
					v-for="c in stats?.categories || []"
					:key="c.code"
					class="chip"
					:class="{ active: category === c.code }"
					@click="pickCategory(c.code)"
				>
					{{ c.label }}
				</button>
			</div>
			<div class="row">
				<span class="label">{{ lang.pick("民族", "Group") }}</span>
				<el-select
					v-model="ethnicId"
					:placeholder="lang.pick('全部民族', 'All groups')"
					clearable
					filterable
					style="width: 200px"
					@change="onEthnicChange"
				>
					<el-option
						v-for="e in ethnics"
						:key="e.id"
						:label="e.name"
						:value="e.id"
					/>
				</el-select>
				<span
					class="label"
					style="margin-left: 8px"
					>{{ lang.pick("名称", "Name") }}</span
				>
				<el-input
					v-model="keyword"
					:placeholder="lang.pick('搜索项目名称', 'Search by name')"
					clearable
					style="width: 220px"
					@keyup.enter="doSearch"
					@clear="doSearch"
				/>
				<el-button
					type="primary"
					@click="doSearch"
					>{{ lang.pick("搜索", "Search") }}</el-button
				>
				<span style="flex: 1"></span>
				<router-link
					class="chip ct-entry"
					to="/persons"
					>👤 传承人专栏</router-link
				>
				<router-link
					class="chip ct-entry"
					to="/culture/costume"
					>👘 民族服饰专题</router-link
				>
				<button
					v-if="hasFilter"
					class="chip"
					@click="reset"
				>
					{{ lang.pick("重置", "Reset") }}
				</button>
			</div>
		</div>

		<div class="result-count">{{ lang.t("result_count", { n: total }) }}</div>

		<el-skeleton
			v-if="loading"
			:rows="6"
			animated
		/>

		<div
			v-else-if="errored"
			class="ghost-block"
			style="text-align: center"
		>
			<el-empty :description="lang.pick('非遗名录加载失败', 'Failed to load heritage list')">
				<el-button
					type="primary"
					@click="load"
					>{{ lang.pick("重新加载", "Retry") }}</el-button
				>
			</el-empty>
		</div>

		<template v-else-if="list.length">
			<div class="grid grid-3">
				<router-link
					v-for="a in list"
					:key="a.id"
					class="feature"
					:to="`/art/${a.id}`"
				>
					<div class="img">
						<CoverImage
							:src="a.coverImage"
							:name="a.name"
							:prompt="artImagePrompt(a.name)"
							size="landscape_4_3"
						/>
					</div>
					<div class="t">
						<div class="t-top">
							<span
								class="lv"
								:class="a.intangibleHeritage"
								>{{ heritageLabel[a.intangibleHeritage] || "" }}</span
							>
							<span class="cat">{{ artCategoryLabel[a.category] || a.category }}</span>
						</div>
						<h4>{{ a.name }}</h4>
						<p class="eth">{{ a.ethnicGroupName }}</p>
						<p class="desc">{{ a.description }}</p>
						<div
							v-if="a.inheritors?.length"
							class="inh"
						>
							<span class="inh-label">{{ lang.pick("传承人", "Inheritors") }}</span>
							<span
								v-for="p in a.inheritors.slice(0, 3)"
								:key="p"
								class="inh-chip"
								>{{ p }}</span
							>
						</div>
					</div>
				</router-link>
			</div>
			<AppPagination
				:current="page"
				:total="total"
				:size="size"
				@change="onPage"
			/>
		</template>

		<el-empty
			v-else
			:description="lang.pick('没有符合条件的非遗项目', 'No heritage items match the filters')"
			style="padding: 48px 0"
		/>
	</div>
</template>

<style scoped>
/* ---- 概览 ---- */
.hd-overview {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 22px 24px;
	margin-bottom: 26px;
}
.hd-overview .metrics {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin: 0 0 20px;
}
.hd-overview .metric {
	background: var(--paper);
	padding: 14px 16px;
}
.hd-overview .metric .num {
	font-family: var(--serif);
	font-size: 24px;
	font-weight: 700;
	color: var(--accent);
	font-variant-numeric: tabular-nums;
	line-height: 1.2;
}
.hd-overview .metric .lbl {
	font-size: 11px;
	letter-spacing: 0.08em;
	color: var(--muted);
	text-transform: uppercase;
	margin-top: 3px;
}
.hd-dist {
	display: grid;
	grid-template-columns: minmax(0, 1fr) minmax(0, 1.5fr);
	gap: 28px;
}
.dist-block h4 {
	font-family: var(--serif);
	font-size: 15px;
	letter-spacing: 0.06em;
	margin-bottom: 12px;
	padding-bottom: 9px;
	border-bottom: 1px solid var(--line);
}
.dist-row {
	display: grid;
	grid-template-columns: 10px 1fr auto;
	align-items: center;
	gap: 9px;
	width: 100%;
	background: none;
	border: none;
	padding: 7px 6px;
	cursor: pointer;
	font-family: var(--font-sans);
	font-size: 13.5px;
	text-align: left;
	transition: background var(--dur-fast) ease;
}
.dist-row:hover {
	background: var(--paper-2);
}
.dist-row.active {
	background: color-mix(in srgb, var(--accent) 9%, transparent);
}
.dist-row .dot {
	width: 9px;
	height: 9px;
	display: block;
	background: var(--muted);
}
.dist-row .dot.world {
	background: var(--accent);
}
.dist-row .dot.national {
	background: var(--ink);
	opacity: 0.65;
}
.dist-row .d-count {
	font-family: var(--serif);
	font-weight: 700;
	color: var(--accent);
	font-variant-numeric: tabular-nums;
}
.dist-bar {
	display: grid;
	grid-template-columns: 58px 1fr 34px;
	align-items: center;
	gap: 10px;
	width: 100%;
	background: none;
	border: none;
	padding: 5px 6px;
	cursor: pointer;
	font-family: var(--font-sans);
	font-size: 13px;
	transition: background var(--dur-fast) ease;
}
.dist-bar:hover {
	background: var(--paper-2);
}
.dist-bar.active {
	background: color-mix(in srgb, var(--accent) 9%, transparent);
}
.dist-bar .b-name {
	text-align: left;
	color: var(--ink-soft);
}
.dist-bar .b-track {
	height: 10px;
	background: var(--paper-2);
	border: 1px solid var(--line);
	overflow: hidden;
}
.dist-bar .b-fill {
	display: block;
	height: 100%;
	background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent) 50%, var(--ink)));
}
.dist-bar .b-count {
	font-variant-numeric: tabular-nums;
	color: var(--muted);
	text-align: right;
}

/* ---- 传承人聚焦 ---- */
.spotlight {
	border: 1px solid var(--line);
	background: var(--paper-2);
	padding: 20px 24px 22px;
	margin-bottom: 26px;
}
.spotlight .panel-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 1px solid var(--line);
	padding-bottom: 12px;
	margin-bottom: 16px;
}
.spotlight .panel-head h3 {
	font-family: var(--serif);
	font-size: 19px;
	letter-spacing: 0.06em;
}
.spotlight .sh-note {
	font-size: 11.5px;
	color: var(--muted);
}
.sp-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.sp-card {
	background: var(--paper);
	padding: 15px 16px 17px;
	display: block;
	transition: background var(--dur-fast) ease;
}
.sp-card:hover {
	background: var(--paper-2);
}
.sp-head {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 8px;
}
.sp-name {
	font-family: var(--serif);
	font-size: 16px;
	letter-spacing: 0.02em;
	line-height: 1.35;
}
.sp-level {
	font-size: 10px;
	letter-spacing: 0.06em;
	padding: 1px 5px;
	border: 1px solid var(--line);
	color: var(--muted);
	white-space: nowrap;
	flex: none;
}
.sp-level.world {
	border-color: var(--accent);
	color: var(--accent);
}
.sp-eth {
	font-size: 12px;
	color: var(--muted);
	margin-top: 5px;
}
.sp-inh {
	display: flex;
	flex-wrap: wrap;
	gap: 4px;
	margin-top: 10px;
}

/* 传承人标签 */
.inh-chip {
	font-size: 11.5px;
	padding: 2px 7px;
	background: color-mix(in srgb, var(--accent) 9%, transparent);
	color: var(--accent);
	white-space: nowrap;
}

/* ---- 名录卡片 ---- */
.feature .t-top {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-bottom: 8px;
}
.feature .lv {
	font-size: 10.5px;
	letter-spacing: 0.06em;
	padding: 1px 6px;
	border: 1px solid var(--line);
	color: var(--muted);
}
.feature .lv.world {
	border-color: var(--accent);
	color: var(--accent);
	font-weight: 700;
}
.feature .cat {
	font-size: 11px;
	color: var(--muted);
	letter-spacing: 0.06em;
}
.feature .t h4 {
	margin-top: 0;
}
.feature .eth {
	font-size: 12.5px;
	color: var(--accent);
	margin-top: 5px;
}
.feature .desc {
	display: -webkit-box;
	-webkit-line-clamp: 3;
	line-clamp: 3;
	-webkit-box-orient: vertical;
	overflow: hidden;
}
.feature .inh {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 5px;
	margin-top: 11px;
	padding-top: 10px;
	border-top: 1px dashed var(--line);
}
.feature .inh-label {
	font-size: 11px;
	color: var(--muted);
	letter-spacing: 0.06em;
}
/* 服饰专题入口：与筛选 chip 同形，作为链接可跳转 */
.ct-entry {
	text-decoration: none;
	color: var(--accent);
	border-color: var(--accent);
}
.ct-entry:hover {
	background: var(--accent);
	color: #fff;
	border-color: var(--accent);
}

@media (max-width: 900px) {
	.hd-overview .metrics {
		grid-template-columns: repeat(2, 1fr);
	}
	.hd-dist {
		grid-template-columns: minmax(0, 1fr);
		gap: 20px;
	}
	.sp-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
	.filter .row {
		flex-wrap: wrap;
	}
}
@media (max-width: 560px) {
	.sp-grid {
		grid-template-columns: minmax(0, 1fr);
	}
}
</style>
