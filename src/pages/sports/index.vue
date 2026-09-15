<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import PageHead from "@/components/PageHead.vue";
import { traditionalSportApi } from "@/api/modules";
import type { TraditionalSport, TraditionalSportDirectory } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 传统体育（B-5）
 *
 * 内容主体为**全国少数民族传统体育运动会竞赛项目**，
 * 数据来源：国家民委、国家体育总局《总规程》竞赛项目清单；
 * 湖北省民宗委《少数民族传统体育项目》项目简介。
 *
 * 诚实性说明：多数项目的「首次成为竞赛项目年份」在权威来源中未逐一给出，
 * 因此该字段多为空 —— 页面不显示空缺字段，也不编造。
 */

const lang = useLangStore();

const data = ref<TraditionalSportDirectory | null>(null);
const loading = ref(true);
const errored = ref(false);

const category = ref("");
const ethnic = ref("");
const keyword = ref("");
/** 展开详情的项目名 */
const expanded = ref<string | null>(null);

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		data.value = await traditionalSportApi.list({
			category: category.value || undefined,
			ethnic: ethnic.value || undefined,
			keyword: keyword.value || undefined,
		});
	} catch {
		errored.value = true;
		data.value = null;
	} finally {
		loading.value = false;
	}
}

onMounted(load);

function pickCategory(v: string) {
	category.value = category.value === v ? "" : v;
	load();
}
function reset() {
	category.value = "";
	ethnic.value = "";
	keyword.value = "";
	load();
}

const hasFilter = computed(() => !!(category.value || ethnic.value || keyword.value));

/** 筛选用的类别列表（从项目列表去重，保持后端返回顺序） */
const categories = computed(() => {
	const seen = new Map<string, string>();
	for (const s of data.value?.sports || []) {
		if (!seen.has(s.category)) seen.set(s.category, s.categoryLabel);
	}
	return [...seen.entries()].map(([value, label]) => ({ value, label }));
});

/** 筛选用的民族列表（从项目去重） */
const ethnics = computed(() => {
	const set = new Set<string>();
	for (const s of data.value?.sports || []) {
		for (const e of s.ethnicOrigins) set.add(e);
	}
	return [...set].sort((a, b) => a.localeCompare(b, "zh"));
});

/** 按类别分组展示 */
const grouped = computed(() => {
	const map = new Map<string, { label: string; sports: TraditionalSport[] }>();
	for (const s of data.value?.sports || []) {
		let g = map.get(s.category);
		if (!g) {
			g = { label: s.categoryLabel, sports: [] };
			map.set(s.category, g);
		}
		g.sports.push(s);
	}
	return [...map.entries()].map(([code, g]) => ({ code, ...g }));
});

function toggle(name: string) {
	expanded.value = expanded.value === name ? null : name;
}

function gotoEthnic(slug: string) {
	window.location.hash = `#/search?q=${encodeURIComponent(slug)}`;
}
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<router-link to="/ethnic">{{ lang.pick("民族", "Ethnic Groups") }}</router-link> ›
			<span>{{ lang.pick("传统体育", "Traditional Sports") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('TRADITIONAL SPORTS · 传统体育', 'TRADITIONAL SPORTS')"
			:title="lang.pick('民族传统体育', 'Traditional Ethnic Sports')"
			:dek="
				lang.pick(
					'许多传统体育项目源自各民族的生产劳动与节庆习俗。这里以全国少数民族传统体育运动会的竞赛项目为主体，介绍这些项目的来历与玩法。',
					'Many traditional sports grew out of daily labour and festivals. This section introduces the events of the National Traditional Games of Ethnic Minorities.',
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
			<el-empty :description="lang.pick('传统体育数据加载失败', 'Failed to load data')">
				<el-button
					type="primary"
					@click="load"
					>{{ lang.pick("重新加载", "Retry") }}</el-button
				>
			</el-empty>
		</div>

		<template v-else>
			<!-- 概览 -->
			<div class="metrics ts-metrics">
				<div class="metric">
					<div class="num">{{ data.summary.total }}</div>
					<div class="lbl">{{ lang.pick("个传统体育项目", "Sports") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.categoryCount }}</div>
					<div class="lbl">{{ lang.pick("个类别", "Categories") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.ethnicCount }}</div>
					<div class="lbl">{{ lang.pick("个相关民族", "Ethnic groups") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.withSubEvents }}</div>
					<div class="lbl">{{ lang.pick("项含子项", "With sub-events") }}</div>
				</div>
			</div>

			<!-- 筛选 -->
			<div class="filter">
				<div class="row">
					<span class="label">{{ lang.pick("类别", "Category") }}</span>
					<button
						v-for="c in categories"
						:key="c.value"
						class="chip"
						:class="{ active: category === c.value }"
						@click="pickCategory(c.value)"
					>
						{{ c.label }}
					</button>
				</div>
				<div class="row">
					<span class="label">{{ lang.pick("民族", "Group") }}</span>
					<el-select
						v-model="ethnic"
						:placeholder="lang.pick('全部民族', 'All groups')"
						clearable
						filterable
						style="width: 180px"
						@change="load"
					>
						<el-option
							v-for="e in ethnics"
							:key="e"
							:label="e"
							:value="e"
						/>
					</el-select>
					<span
						class="label"
						style="margin-left: 8px"
						>{{ lang.pick("搜索", "Search") }}</span
					>
					<el-input
						v-model="keyword"
						:placeholder="lang.pick('项目名称或描述', 'Name or description')"
						clearable
						style="width: 220px"
						@keyup.enter="load"
						@clear="load"
					/>
					<el-button
						type="primary"
						@click="load"
						>{{ lang.pick("搜索", "Search") }}</el-button
					>
					<span style="flex: 1"></span>
					<button
						v-if="hasFilter"
						class="chip"
						@click="reset"
					>
						{{ lang.pick("重置", "Reset") }}
					</button>
				</div>
			</div>

			<div class="result-count">
				{{ lang.pick(`共 ${data.sports.length} 个项目`, `${data.sports.length} sports`) }}
			</div>

			<!-- 按类别分组 -->
			<section
				v-for="g in grouped"
				:key="g.code"
				class="ts-section"
			>
				<div class="panel-head">
					<h3>{{ g.label }}</h3>
					<span class="sh-note">{{ g.sports.length }} {{ lang.pick("项", "sports") }}</span>
				</div>

				<div class="ts-grid">
					<article
						v-for="s in g.sports"
						:key="s.name"
						class="ts-card"
						:class="{ open: expanded === s.name }"
					>
						<header
							class="ts-head"
							@click="toggle(s.name)"
						>
							<div class="th-main">
								<h4>{{ s.name }}</h4>
								<div
									v-if="s.ethnicOrigins.length"
									class="th-ethnics"
								>
									<button
										v-for="e in s.ethnicOrigins"
										:key="e"
										class="eth-chip"
										:title="lang.pick('查看该民族', 'View group')"
										@click.stop="
											() => {
												const m = s.matchedEthnics.find((x) => x.name === e);
												if (m) gotoEthnic(m.slug);
											}
										"
										:class="{ linked: s.matchedEthnics.some((x) => x.name === e) }"
									>
										{{ e }}
									</button>
								</div>
								<div
									v-else
									class="th-ethnics"
								>
									<span class="eth-none">{{ lang.pick("多民族共有", "Shared") }}</span>
								</div>
							</div>
							<span class="th-toggle">{{ expanded === s.name ? "−" : "+" }}</span>
						</header>

						<p class="ts-desc">{{ s.description }}</p>

						<!-- 子项 -->
						<div
							v-if="s.subEvents.length"
							class="ts-subs"
						>
							<span class="sub-label">{{ lang.pick("设项", "Events") }}</span>
							<span
								v-for="se in s.subEvents"
								:key="se"
								class="sub-chip"
								>{{ se }}</span
							>
						</div>

						<!-- 展开详情 -->
						<dl
							v-if="expanded === s.name"
							class="ts-facts"
						>
							<template v-if="s.venue">
								<dt>{{ lang.pick("场地", "Venue") }}</dt>
								<dd>{{ s.venue }}</dd>
							</template>
							<template v-if="s.teamSize">
								<dt>{{ lang.pick("人数", "Team size") }}</dt>
								<dd>{{ s.teamSize }}</dd>
							</template>
							<template v-if="s.equipment">
								<dt>{{ lang.pick("器材", "Equipment") }}</dt>
								<dd>{{ s.equipment }}</dd>
							</template>
							<template v-if="s.firstEventYear">
								<dt>{{ lang.pick("入会年份", "First held") }}</dt>
								<dd>{{ s.firstEventYear }} {{ lang.pick("年", "") }}</dd>
							</template>
							<template v-if="s.heritageLink">
								<dt>{{ lang.pick("相关非遗", "Heritage") }}</dt>
								<dd>
									<router-link
										class="heritage-link"
										to="/heritage"
										>{{ s.heritageLink }}</router-link
									>
								</dd>
							</template>
							<dt
								v-if="
									!s.venue && !s.teamSize && !s.equipment && !s.firstEventYear && !s.heritageLink
								"
							>
								{{ lang.pick("说明", "Note") }}
							</dt>
							<dd v-if="!s.venue && !s.teamSize && !s.equipment && !s.firstEventYear && !s.heritageLink">
								{{ lang.pick("该项目暂无更多已收录资料", "No further details recorded yet") }}
							</dd>
						</dl>
					</article>
				</div>
			</section>

			<el-empty
				v-if="!data.sports.length"
				:description="lang.pick('没有符合条件的项目', 'No sports match the filters')"
				style="padding: 48px 0"
			/>

			<p class="ts-note">
				{{
					lang.pick(
						"数据来源：国家民委、国家体育总局《全国少数民族传统体育运动会总规程》竞赛项目清单，以及湖北省民族宗教事务委员会《少数民族传统体育项目》项目简介。未见于权威来源的字段（如部分项目的入会年份、场地规格）留空，不作推测。",
						'Sources: the competition event list in the National Traditional Games regulations issued by the National Ethnic Affairs Commission and the General Administration of Sport, plus the project introductions published by the Hubei Ethnic and Religious Affairs Commission. Unknown fields are left blank rather than guessed.',
					)
				}}
			</p>
		</template>
	</div>
</template>

<style scoped>
.ts-metrics {
	margin: 4px 0 22px;
}

.ts-section {
	margin-bottom: 28px;
}
.ts-section .panel-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 2px solid var(--ink);
	padding-bottom: 10px;
	margin-bottom: 16px;
}
.ts-section .panel-head h3 {
	font-family: var(--serif);
	font-size: 20px;
	letter-spacing: 0.06em;
}
.ts-section .sh-note {
	font-size: 11.5px;
	color: var(--muted);
}

.ts-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	align-items: start;
}
.ts-card {
	background: var(--paper);
	padding: 15px 18px 17px;
	min-width: 0;
	transition: background var(--dur-fast) ease;
}
.ts-card:hover {
	background: color-mix(in srgb, var(--accent) 3%, var(--paper));
}
.ts-card.open {
	background: color-mix(in srgb, var(--accent) 6%, var(--paper));
	grid-column: span 1;
}

.ts-head {
	display: flex;
	align-items: flex-start;
	gap: 10px;
	cursor: pointer;
	margin-bottom: 9px;
}
.th-main {
	flex: 1;
	min-width: 0;
}
.ts-head h4 {
	font-family: var(--serif);
	font-size: 17.5px;
	letter-spacing: 0.02em;
	margin-bottom: 6px;
}
.th-ethnics {
	display: flex;
	flex-wrap: wrap;
	gap: 4px;
}
.eth-chip {
	font-size: 11px;
	padding: 1px 6px;
	border: 1px solid var(--line);
	background: transparent;
	color: var(--muted);
	font-family: var(--font-sans);
	cursor: default;
}
.eth-chip.linked {
	color: var(--accent);
	border-color: color-mix(in srgb, var(--accent) 45%, var(--line));
	cursor: pointer;
}
.eth-chip.linked:hover {
	background: var(--accent);
	color: #fff;
}
.eth-none {
	font-size: 11px;
	color: var(--muted);
	font-style: normal;
}
.th-toggle {
	font-family: var(--serif);
	font-size: 18px;
	color: var(--accent);
	flex: none;
	line-height: 1;
	padding-top: 2px;
}

.ts-desc {
	font-size: 13px;
	line-height: 1.85;
	color: var(--ink-soft);
}

.ts-subs {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 5px;
	margin-top: 11px;
	padding-top: 10px;
	border-top: 1px dashed var(--line);
}
.sub-label {
	font-size: 11px;
	color: var(--muted);
	letter-spacing: 0.06em;
}
.sub-chip {
	font-size: 11.5px;
	padding: 1px 7px;
	background: var(--paper-2);
	color: var(--ink-soft);
}

.ts-facts {
	display: grid;
	grid-template-columns: 62px 1fr;
	row-gap: 7px;
	column-gap: 10px;
	margin-top: 12px;
	padding-top: 11px;
	border-top: 1px solid var(--line);
	font-size: 12.5px;
}
.ts-facts dt {
	color: var(--muted);
	letter-spacing: 0.04em;
}
.ts-facts dd {
	color: var(--ink);
	line-height: 1.6;
}
.heritage-link {
	color: var(--accent);
	text-decoration: underline;
	text-underline-offset: 3px;
}

.ts-note {
	font-size: 12.5px;
	color: var(--muted);
	line-height: 1.85;
	margin: 10px 0 8px;
	padding-left: 12px;
	border-left: 2px solid var(--line);
}

@media (max-width: 900px) {
	.ts-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
	.filter .row {
		flex-wrap: wrap;
	}
}
@media (max-width: 560px) {
	.ts-grid {
		grid-template-columns: minmax(0, 1fr);
	}
	.ts-facts {
		grid-template-columns: 1fr;
	}
}
</style>
