<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import PageHead from "@/components/PageHead.vue";
import CoverImage from "@/components/CoverImage.vue";
import { cultureTopicApi } from "@/api/modules";
import type { CultureTopic, CultureTopicEntry, CultureTopicItem } from "@/api/types";
import { heritageLabel } from "@/utils/format";
import { useLangStore } from "@/stores/lang";

/**
 * 文化专题页（B-1 民族服饰 / B-2 民居建筑）
 *
 * 一个页面驱动两个专题：由路由 meta.topic 决定取 costume 还是 dwelling。
 * 两者都不依赖新内容表 —— 后端把「风俗习惯」与「非遗项目」按民族合流，
 * 本页只负责呈现：概览 → 分类筛选 → 按民族的图鉴 → 条目详情跳转。
 */

const route = useRoute();
const lang = useLangStore();

const topicKey = computed(() => (route.meta.topic as "costume" | "dwelling") || "costume");

const data = ref<CultureTopic | null>(null);
const loading = ref(true);
const errored = ref(false);

/** 当前选中的分类（空 = 全部） */
const activeCategory = ref("");
/** 名称 / 民族关键词 */
const keyword = ref("");
/** 仅看非遗名录条目 */
const heritageOnly = ref(false);

async function load() {
	loading.value = true;
	errored.value = false;
	activeCategory.value = "";
	keyword.value = "";
	heritageOnly.value = false;
	try {
		data.value = await cultureTopicApi.get(topicKey.value);
	} catch {
		errored.value = true;
		data.value = null;
	} finally {
		loading.value = false;
	}
}

onMounted(load);
// 同一路由切换 topic（/culture/costume → /culture/dwelling）需重新取数
watch(topicKey, load);

/** 按分类 / 关键词过滤后的条目 */
const filtered = computed<CultureTopicEntry[]>(() => {
	const list = data.value?.entries || [];
	const kw = keyword.value.trim().toLowerCase();

	return list
		.map((e) => {
			let items = e.items;
			if (activeCategory.value) {
				items = items.filter((i) => i.category === activeCategory.value);
			}
			if (heritageOnly.value) {
				items = items.filter((i) => i.source === "art");
			}
			if (kw) {
				const hitGroup = e.ethnicGroupName.toLowerCase().includes(kw);
				items = items.filter(
					(i) =>
						hitGroup ||
						i.title.toLowerCase().includes(kw) ||
						(i.content || "").toLowerCase().includes(kw),
				);
			}
			return { ...e, items };
		})
		.filter((e) => e.items.length > 0);
});

const filteredCount = computed(() =>
	filtered.value.reduce((s, e) => s + e.items.length, 0),
);

const hasFilter = computed(
	() => !!(activeCategory.value || keyword.value || heritageOnly.value),
);

function resetFilter() {
	activeCategory.value = "";
	keyword.value = "";
	heritageOnly.value = false;
}

function pickCategory(code: string) {
	activeCategory.value = activeCategory.value === code ? "" : code;
}

/** 该民族条目的非遗数量（用于徽标提示） */
function heritageCountOf(entry: CultureTopicEntry): number {
	return entry.items.filter((i) => i.source === "art").length;
}

/** 正文摘要 */
function brief(text: string | null, max = 96): string {
	if (!text) return "";
	const t = text.replace(/\s+/g, " ").trim();
	return t.length > max ? t.slice(0, max) + "…" : t;
}

/** 条目是否可作为独立详情跳转（art 有详情页；custom 落到民族风俗 Tab） */
function isExternalItem(item: CultureTopicItem): boolean {
	return item.source === "art" && !!item.id;
}
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<span>{{ data?.title || lang.pick("文化专题", "Culture Topic") }}</span>
		</div>

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
			<el-empty :description="lang.pick('专题数据加载失败', 'Failed to load topic')">
				<el-button
					type="primary"
					@click="load"
					>{{ lang.pick("重新加载", "Retry") }}</el-button
				>
			</el-empty>
		</div>

		<template v-else>
			<PageHead
				:kicker="`TOPIC · ${data.titleEn.toUpperCase()}`"
				:title="data.title"
				:dek="data.intro"
			/>

			<!-- 概览 -->
			<div class="metrics ct-metrics">
				<div class="metric">
					<div class="num">{{ data.summary.groupCount }}</div>
					<div class="lbl">{{ lang.pick("个民族", "Ethnic groups") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.entryCount }}</div>
					<div class="lbl">{{ lang.pick("条记录", "Records") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.heritageCount }}</div>
					<div class="lbl">{{ lang.pick("项非遗名录", "Heritage items") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.withImage }}</div>
					<div class="lbl">{{ lang.pick("个民族有图", "With photo") }}</div>
				</div>
			</div>

			<!-- 分类筛选 -->
			<div class="filter">
				<div class="row">
					<span class="label">{{ lang.pick("分类", "Category") }}</span>
					<button
						v-for="c in data.categories"
						:key="c.code"
						class="chip"
						:class="{ active: activeCategory === c.code }"
						@click="pickCategory(c.code)"
					>
						{{ c.label }}（{{ c.count }}）
					</button>
				</div>
				<div class="row">
					<span class="label">{{ lang.pick("筛选", "Filter") }}</span>
					<el-input
						v-model="keyword"
						:placeholder="lang.pick('搜索民族或内容', 'Search group or content')"
						clearable
						style="width: 240px"
					/>
					<button
						class="chip"
						:class="{ active: heritageOnly }"
						@click="heritageOnly = !heritageOnly"
					>
						{{ lang.pick("仅看非遗名录", "Heritage only") }}
					</button>
					<span style="flex: 1"></span>
					<button
						v-if="hasFilter"
						class="chip"
						@click="resetFilter"
					>
						{{ lang.pick("重置", "Reset") }}
					</button>
				</div>
			</div>

			<div class="result-count">
				{{
					lang.pick(
						`${filtered.length} 个民族 · ${filteredCount} 条记录`,
						`${filtered.length} groups · ${filteredCount} records`,
					)
				}}
			</div>

			<!-- 图鉴 -->
			<div
				v-if="filtered.length"
				class="ct-grid"
			>
				<article
					v-for="entry in filtered"
					:key="entry.ethnicGroupId"
					class="ct-card"
				>
					<div class="ct-img">
						<CoverImage
							:src="entry.coverImage"
							:name="entry.ethnicGroupName"
							:theme="entry.themeColor"
							size="landscape_4_3"
						/>
						<span
							v-if="heritageCountOf(entry)"
							class="ct-badge"
						>
							{{ lang.pick("非遗", "Heritage") }} {{ heritageCountOf(entry) }}
						</span>
					</div>

					<div class="ct-body">
						<div class="ct-head">
							<h4 :style="{ color: entry.themeColor }">
								{{ entry.ethnicGroupName }}
							</h4>
							<span
								v-if="entry.region"
								class="ct-region"
								>{{ entry.region }}</span
							>
						</div>

						<ul class="ct-items">
							<li
								v-for="(it, i) in entry.items"
								:key="`${it.source}-${it.id}-${i}`"
								class="ct-item"
							>
								<router-link
									v-if="isExternalItem(it)"
									:to="it.detailPath"
									class="ci-link"
								>
									<span class="ci-title">{{ it.title }}</span>
									<span
										v-if="it.intangibleHeritage"
										class="ci-lv"
										:class="it.intangibleHeritage"
									>
										{{ heritageLabel[it.intangibleHeritage] || "" }}
									</span>
								</router-link>
								<router-link
									v-else
									:to="it.detailPath"
									class="ci-link"
								>
									<span class="ci-title">{{ it.title }}</span>
								</router-link>
								<p
									v-if="it.content"
									class="ci-desc"
								>
									{{ brief(it.content) }}
								</p>
							</li>
						</ul>
					</div>
				</article>
			</div>

			<el-empty
				v-else
				:description="lang.pick('没有符合条件的内容', 'Nothing matches the filters')"
				style="padding: 48px 0"
			/>

			<p class="ct-note">
				{{
					lang.pick(
						"本专题内容由「风俗习惯」与「非物质文化遗产名录」两类数据按民族合流而成；标有级别的条目为国家级及以上非遗项目，点击可查看该项目的传承人信息。",
						"This topic merges records from customs and the intangible heritage list by ethnic group. Level-tagged entries open the heritage item with its inheritors.",
					)
				}}
			</p>
		</template>
	</div>
</template>

<style scoped>
.ct-metrics {
	margin: 4px 0 24px;
}

/* ---- 图鉴栅格 ---- */
.ct-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.ct-card {
	background: var(--paper);
	display: flex;
	flex-direction: column;
	min-width: 0;
	transition: box-shadow var(--dur) var(--ease-out);
}
.ct-card:hover {
	box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1);
	z-index: 1;
}
.ct-img {
	position: relative;
	aspect-ratio: 4 / 3;
	overflow: hidden;
	background: var(--paper-2);
}
.ct-badge {
	position: absolute;
	left: 0;
	bottom: 0;
	background: var(--ink);
	color: #fff;
	font-size: 10.5px;
	letter-spacing: 0.08em;
	padding: 3px 9px;
}
.ct-body {
	padding: 15px 18px 18px;
	flex: 1;
	display: flex;
	flex-direction: column;
}
.ct-head {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
	gap: 10px;
	padding-bottom: 10px;
	border-bottom: 1px solid var(--line);
	margin-bottom: 10px;
}
.ct-head h4 {
	font-family: var(--serif);
	font-size: 19px;
	letter-spacing: 0.04em;
}
.ct-region {
	font-size: 11px;
	color: var(--muted);
	letter-spacing: 0.04em;
	white-space: nowrap;
}

.ct-items {
	list-style: none;
	display: flex;
	flex-direction: column;
	gap: 12px;
}
.ct-item + .ct-item {
	border-top: 1px dashed var(--line);
	padding-top: 11px;
}
.ci-link {
	display: flex;
	align-items: baseline;
	gap: 8px;
	flex-wrap: wrap;
}
.ci-title {
	font-family: var(--serif);
	font-size: 14.5px;
	letter-spacing: 0.02em;
	transition: color var(--dur-fast) ease;
}
.ci-link:hover .ci-title {
	color: var(--accent);
}
.ci-lv {
	font-size: 10px;
	letter-spacing: 0.05em;
	padding: 1px 5px;
	border: 1px solid var(--line);
	color: var(--muted);
	white-space: nowrap;
}
.ci-lv.world {
	border-color: var(--accent);
	color: var(--accent);
	font-weight: 700;
}
.ci-desc {
	font-size: 12.5px;
	line-height: 1.75;
	color: var(--muted);
	margin-top: 5px;
}

.ct-note {
	font-size: 12.5px;
	color: var(--muted);
	line-height: 1.85;
	margin: 24px 0 8px;
	padding-left: 12px;
	border-left: 2px solid var(--line);
}

@media (max-width: 900px) {
	.ct-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
	.filter .row {
		flex-wrap: wrap;
	}
}
@media (max-width: 560px) {
	.ct-grid {
		grid-template-columns: minmax(0, 1fr);
	}
}
</style>
