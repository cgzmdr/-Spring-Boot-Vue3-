<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import PageHead from "@/components/PageHead.vue";
import { ethnicApi } from "@/api/modules";
import type { LanguageAtlas, LanguageGroupRef } from "@/api/types";
import { formatNumber } from "@/utils/format";
import { useLangStore } from "@/stores/lang";

/**
 * 民族语文专栏（B-3）
 *
 * 数据来源：`ethnic_group` 的 language_family（语系）/ languages（语言）/ scripts（文字）三字段，
 * 56 个民族均已录入，因此本页是「已有数据的重新组织」，不新增内容表。
 *
 * 口径提醒（页面上必须明示）：语言文字是文化事实，本页计数仅反映本库已录入的数据，
 * 且「一种语言/文字被多个民族使用」很常见（如汉字被 49 个民族使用），
 * 因此各类计数之和会大于 56，并非官方统计口径。
 */

const lang = useLangStore();
const router = useRouter();

const atlas = ref<LanguageAtlas | null>(null);
const loading = ref(true);
const errored = ref(false);
const tab = ref<"family" | "script">("family");

onMounted(async () => {
	try {
		atlas.value = await ethnicApi.languageAtlas();
	} catch {
		errored.value = true;
	} finally {
		loading.value = false;
	}
});

/** 按大语系归并（同一个大语系下可能有多个语族） */
interface FamilyNode {
	family: string;
	branches: { name: string; groups: LanguageGroupRef[]; population: number }[];
	population: number;
	groupIds: Set<string>;
}

const familyTree = computed(() => {
	const map = new Map<string, FamilyNode>();
	for (const f of atlas.value?.families || []) {
		let node = map.get(f.family);
		if (!node) {
			node = { family: f.family, branches: [], population: 0, groupIds: new Set() };
			map.set(f.family, node);
		}
		node.branches.push({ name: f.name, groups: f.groups, population: f.population });
		node.population += f.population;
		f.groups.forEach((g) => node!.groupIds.add(g.id));
	}
	return [...map.values()]
		.map((n) => ({ ...n, groupCount: n.groupIds.size }))
		.sort((a, b) => b.population - a.population);
});

/** 本民族传统文字（排除汉字 / 阿拉伯文等借用文字） */
const nativeScripts = computed(() => (atlas.value?.scripts || []).filter((s) => s.nativeScript));
/** 借用 / 通用文字 */
const borrowedScripts = computed(() => (atlas.value?.scripts || []).filter((s) => !s.nativeScript));

/** 已展开的语系 */
const openFamily = ref<string | null>(null);
function toggleFamily(name: string) {
	openFamily.value = openFamily.value === name ? null : name;
}

/** 选中的文字（用于下方民族对照） */
const activeScript = ref<string | null>(null);
const activeScriptGroups = computed<LanguageGroupRef[]>(() => {
	if (!activeScript.value) return [];
	return (atlas.value?.scripts || []).find((s) => s.name === activeScript.value)?.groups || [];
});

/** 全部语言清单（去重，按使用民族数排序） */
const languageList = computed(() => {
	const map = new Map<string, LanguageGroupRef[]>();
	for (const f of atlas.value?.families || []) {
		for (const g of f.groups) {
			for (const l of g.languages) {
				const key = l.trim();
				if (!key) continue;
				const arr = map.get(key) || [];
				if (!arr.some((x) => x.id === g.id)) arr.push(g);
				map.set(key, arr);
			}
		}
	}
	return [...map.entries()]
		.map(([name, groups]) => ({ name, groups }))
		.sort((a, b) => b.groups.length - a.groups.length || a.name.localeCompare(b.name));
});

const showAllLanguages = ref(false);
const visibleLanguages = computed(() =>
	showAllLanguages.value ? languageList.value : languageList.value.slice(0, 18),
);

function goEthnic(id: string) {
	router.push(`/ethnic/${id}`);
}
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<router-link to="/ethnic">{{ lang.pick("民族", "Ethnic Groups") }}</router-link> ›
			<span>{{ lang.pick("民族语文", "Languages") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('LANGUAGES & SCRIPTS · 民族语文', 'LANGUAGES & SCRIPTS')"
			:title="lang.pick('民族语言文字', 'Languages & Scripts')"
			:dek="
				lang.pick(
					'语言是文化的载体。中国各民族的语言分属五大语系，其中部分民族还保留着本民族的传统文字。这里按语系与文字两个维度，梳理各民族的语言文字面貌。',
					'Language carries culture. The languages of China\u2019s ethnic groups belong to several families, and many groups keep their own traditional scripts.',
				)
			"
		/>

		<el-skeleton
			v-if="loading"
			:rows="8"
			animated
		/>

		<div
			v-else-if="errored"
			class="ghost-block"
			style="text-align: center"
		>
			<el-empty :description="lang.pick('语文数据加载失败', 'Failed to load language data')" />
		</div>

		<template v-else-if="atlas">
			<!-- 概览 -->
			<div class="metrics la-metrics">
				<div class="metric">
					<div class="num">{{ atlas.summary.familyCount }}</div>
					<div class="lbl">{{ lang.pick("大语系", "Language families") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ atlas.summary.languageCount }}</div>
					<div class="lbl">{{ lang.pick("种语言", "Languages") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ atlas.summary.scriptCount }}</div>
					<div class="lbl">{{ lang.pick("种文字", "Scripts") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ atlas.groupsWithOwnScript }}</div>
					<div class="lbl">{{ lang.pick("个民族有传统文字", "With own script") }}</div>
				</div>
			</div>

			<!-- Tab 切换 -->
			<div class="la-tabs">
				<button
					class="la-tab"
					:class="{ active: tab === 'family' }"
					@click="tab = 'family'"
				>
					{{ lang.pick("按语系", "By family") }}
				</button>
				<button
					class="la-tab"
					:class="{ active: tab === 'script' }"
					@click="tab = 'script'"
				>
					{{ lang.pick("按文字", "By script") }}
				</button>
			</div>

			<!-- ============ 按语系 ============ -->
			<template v-if="tab === 'family'">
				<div class="fam-list">
					<div
						v-for="f in familyTree"
						:key="f.family"
						class="fam-item"
					>
						<button
							class="fam-head"
							:class="{ open: openFamily === f.family }"
							@click="toggleFamily(f.family)"
						>
							<span class="fam-bar">
								<span
									class="fam-fill"
									:style="{
										width: `${Math.max(3, (f.population / (familyTree[0]?.population || 1)) * 100)}%`,
									}"
								/>
							</span>
							<span class="fam-name">{{ f.family }}</span>
							<span class="fam-meta">
								{{ f.branches.length }} {{ lang.pick("个语族", "branches") }} ·
								{{ f.groupCount }} {{ lang.pick("个民族", "groups") }} ·
								{{ formatNumber(f.population) }}
							</span>
							<span class="fam-toggle">{{ openFamily === f.family ? "−" : "+" }}</span>
						</button>

						<div
							v-if="openFamily === f.family"
							class="fam-body"
						>
							<div
								v-for="b in f.branches"
								:key="b.name"
								class="branch"
							>
								<div class="br-head">
									<span class="br-name">{{ b.name }}</span>
									<span class="br-pop">{{ formatNumber(b.population) }}</span>
								</div>
								<div class="br-groups">
									<button
										v-for="g in b.groups"
										:key="g.id"
										class="g-chip"
										:style="{ borderColor: g.themeColor }"
										@click="goEthnic(g.id)"
									>
										<span class="gc-name">{{ g.name }}</span>
										<span class="gc-lang">{{ g.languages.join("/") }}</span>
									</button>
								</div>
							</div>
						</div>
					</div>
				</div>

				<!-- 语言清单 -->
				<section class="la-section">
					<div class="panel-head">
						<h3>{{ lang.pick("语言一览", "Language index") }}</h3>
						<span class="sh-note">
							{{ lang.pick("按使用民族数排序", "Sorted by number of groups") }}
						</span>
					</div>
					<div class="lang-grid">
						<div
							v-for="l in visibleLanguages"
							:key="l.name"
							class="lang-item"
						>
							<div class="li-name">{{ l.name }}</div>
							<div class="li-groups">
								<span
									v-for="g in l.groups.slice(0, 5)"
									:key="g.id"
									class="li-eth"
									>{{ g.name }}</span
								>
								<span
									v-if="l.groups.length > 5"
									class="li-more"
									>+{{ l.groups.length - 5 }}</span
								>
							</div>
						</div>
					</div>
					<button
						v-if="languageList.length > 18"
						class="chip la-more"
						@click="showAllLanguages = !showAllLanguages"
					>
						{{
							showAllLanguages
								? lang.pick("收起", "Show less")
								: lang.pick(`展开全部 ${languageList.length} 种语言`, `Show all ${languageList.length}`)
						}}
					</button>
				</section>
			</template>

			<!-- ============ 按文字 ============ -->
			<template v-else>
				<section class="la-section">
					<div class="panel-head">
						<h3>{{ lang.pick("本民族传统文字", "Traditional scripts") }}</h3>
						<span class="sh-note">{{ nativeScripts.length }} {{ lang.pick("种", "scripts") }}</span>
					</div>
					<div class="scr-grid">
						<button
							v-for="s in nativeScripts"
							:key="s.name"
							class="scr-card"
							:class="{ active: activeScript === s.name }"
							@click="activeScript = activeScript === s.name ? null : s.name"
						>
							<div class="sc-name">{{ s.name }}</div>
							<div class="sc-count">
								{{ s.groups.length }} {{ lang.pick("个民族使用", "groups") }}
							</div>
							<div class="sc-groups">
								{{ s.groups.slice(0, 3).map((g) => g.name).join("、") }}
								<template v-if="s.groups.length > 3">…</template>
							</div>
						</button>
					</div>
				</section>

				<!-- 选中文字的民族对照 -->
				<section
					v-if="activeScript"
					class="la-section active-detail"
				>
					<div class="panel-head">
						<h3>{{ activeScript }}</h3>
						<span class="sh-note">
							{{ lang.pick("使用该文字的民族", "Groups using this script") }}
						</span>
					</div>
					<div class="ad-grid">
						<div
							v-for="g in activeScriptGroups"
							:key="g.id"
							class="ad-card"
						>
							<button
								class="ad-name"
								:style="{ color: g.themeColor }"
								@click="goEthnic(g.id)"
							>
								{{ g.name }}
							</button>
							<div class="ad-row">
								<span class="ad-lbl">{{ lang.pick("语言", "Language") }}</span>
								<span>{{ g.languages.join("、") || "—" }}</span>
							</div>
							<div class="ad-row">
								<span class="ad-lbl">{{ lang.pick("文字", "Script") }}</span>
								<span>{{ g.scripts.join("、") || "—" }}</span>
							</div>
						</div>
					</div>
				</section>

				<!-- 借用 / 通用文字 -->
				<section
					v-if="borrowedScripts.length"
					class="la-section"
				>
					<div class="panel-head">
						<h3>{{ lang.pick("通用 / 借用文字", "Shared & borrowed scripts") }}</h3>
						<span class="sh-note">
							{{ lang.pick("非该民族传统文字，但为其通用或借用文字", "Not traditional to the group but in common use") }}
						</span>
					</div>
					<div class="scr-grid">
						<button
							v-for="s in borrowedScripts"
							:key="s.name"
							class="scr-card borrowed"
							:class="{ active: activeScript === s.name }"
							@click="activeScript = activeScript === s.name ? null : s.name"
						>
							<div class="sc-name">{{ s.name }}</div>
							<div class="sc-count">
								{{ s.groups.length }} {{ lang.pick("个民族使用", "groups") }}
							</div>
							<div class="sc-groups">
								{{ s.groups.slice(0, 3).map((g) => g.name).join("、") }}
								<template v-if="s.groups.length > 3">…</template>
							</div>
						</button>
					</div>
				</section>
			</template>

			<p class="la-note">
				{{
					lang.pick(
						`注：本页数据来自本站已录入的民族志资料，共收录 ${atlas.summary.groupCount} 个民族的语言文字信息。` +
							`一种语言或文字常被多个民族共同使用（如汉字为多个民族通用），因此各类计数之和会大于民族总数；` +
							`本页仅作文化展示，非官方统计口径。`,
						`Note: figures come from the site\u2019s own records for ${atlas.summary.groupCount} groups. A language or script is often shared by several groups, so counts exceed the number of groups. For cultural reference only.`,
					)
				}}
			</p>
		</template>
	</div>
</template>

<style scoped>
.la-metrics {
	margin-bottom: 26px;
}
.la-tabs {
	display: flex;
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin-bottom: 24px;
}
.la-tab {
	flex: 1;
	background: var(--paper);
	border: none;
	padding: 13px 10px;
	cursor: pointer;
	font-family: var(--serif);
	font-size: 15.5px;
	letter-spacing: 0.08em;
	color: var(--ink);
	transition: all var(--dur-fast) ease;
}
.la-tab:hover {
	background: var(--paper-2);
}
.la-tab.active {
	background: var(--ink);
	color: #fff;
}

/* ---- 语系 ---- */
.fam-list {
	border: 1px solid var(--line);
	background: var(--paper);
}
.fam-item + .fam-item {
	border-top: 1px solid var(--line);
}
.fam-head {
	display: grid;
	grid-template-columns: minmax(60px, 160px) auto 1fr 24px;
	align-items: center;
	gap: 14px;
	width: 100%;
	background: none;
	border: none;
	padding: 15px 18px;
	cursor: pointer;
	font-family: var(--font-sans);
	text-align: left;
	transition: background var(--dur-fast) ease;
}
.fam-head:hover {
	background: var(--paper-2);
}
.fam-head.open {
	background: color-mix(in srgb, var(--accent) 6%, var(--paper));
}
.fam-bar {
	height: 15px;
	background: var(--paper-2);
	border: 1px solid var(--line);
	overflow: hidden;
}
.fam-fill {
	display: block;
	height: 100%;
	background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent) 45%, var(--ink)));
}
.fam-name {
	font-family: var(--serif);
	font-size: 17px;
	letter-spacing: 0.03em;
	white-space: nowrap;
}
.fam-meta {
	font-size: 12px;
	color: var(--muted);
	font-variant-numeric: tabular-nums;
}
.fam-toggle {
	font-family: var(--serif);
	font-size: 18px;
	color: var(--accent);
	text-align: center;
}
.fam-body {
	padding: 4px 18px 20px;
	background: var(--paper-2);
	border-top: 1px dashed var(--line);
}
.branch {
	padding: 14px 0 4px;
}
.branch + .branch {
	border-top: 1px dashed var(--line);
}
.br-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	margin-bottom: 11px;
}
.br-name {
	font-family: var(--serif);
	font-size: 14.5px;
	letter-spacing: 0.04em;
}
.br-pop {
	font-size: 11.5px;
	color: var(--muted);
	font-variant-numeric: tabular-nums;
}
.br-groups {
	display: flex;
	flex-wrap: wrap;
	gap: 7px;
}
.g-chip {
	display: inline-flex;
	align-items: baseline;
	gap: 7px;
	background: var(--paper);
	border: 1px solid var(--line);
	border-left-width: 3px;
	padding: 5px 10px;
	cursor: pointer;
	font-family: var(--font-sans);
	transition: transform var(--dur-fast) ease, box-shadow var(--dur-fast) ease;
}
.g-chip:hover {
	transform: translateY(-2px);
	box-shadow: 0 5px 14px rgba(0, 0, 0, 0.1);
}
.gc-name {
	font-family: var(--serif);
	font-size: 13.5px;
}
.gc-lang {
	font-size: 11px;
	color: var(--muted);
}

/* ---- 通用区块 ---- */
.la-section {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 22px 24px;
	margin-top: 24px;
}
.la-section .panel-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 1px solid var(--line);
	padding-bottom: 12px;
	margin-bottom: 16px;
}
.la-section .panel-head h3 {
	font-family: var(--serif);
	font-size: 19px;
	letter-spacing: 0.06em;
}
.la-section .sh-note {
	font-size: 11.5px;
	color: var(--muted);
}

/* 语言一览 */
.lang-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.lang-item {
	background: var(--paper);
	padding: 12px 14px;
}
.li-name {
	font-family: var(--serif);
	font-size: 14px;
	letter-spacing: 0.02em;
}
.li-groups {
	display: flex;
	flex-wrap: wrap;
	gap: 4px;
	margin-top: 7px;
}
.li-eth {
	font-size: 11px;
	color: var(--muted);
	background: var(--paper-2);
	padding: 1px 6px;
}
.li-more {
	font-size: 11px;
	color: var(--accent);
	padding: 1px 4px;
}
.la-more {
	margin-top: 16px;
}

/* 文字卡片 */
.scr-grid {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.scr-card {
	background: var(--paper);
	border: none;
	border-top: 3px solid transparent;
	padding: 14px 15px 16px;
	cursor: pointer;
	font-family: var(--font-sans);
	text-align: left;
	transition: background var(--dur-fast) ease, border-color var(--dur-fast) ease;
	position: relative;
}
.scr-card:hover {
	background: var(--paper-2);
}
.scr-card.active {
	border-top-color: var(--accent);
	background: color-mix(in srgb, var(--accent) 7%, var(--paper));
}
.scr-card.borrowed {
	opacity: 0.86;
}
.sc-name {
	font-family: var(--serif);
	font-size: 16px;
	letter-spacing: 0.02em;
}
.sc-count {
	font-size: 11.5px;
	color: var(--accent);
	margin-top: 5px;
}
.sc-groups {
	font-size: 11.5px;
	color: var(--muted);
	margin-top: 7px;
	line-height: 1.6;
}

/* 选中文字详情 */
.active-detail {
	background: var(--paper-2);
}
.ad-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.ad-card {
	background: var(--paper);
	padding: 14px 16px;
}
.ad-name {
	background: none;
	border: none;
	padding: 0;
	font-family: var(--serif);
	font-size: 17px;
	cursor: pointer;
	letter-spacing: 0.02em;
}
.ad-name:hover {
	opacity: 0.75;
}
.ad-row {
	display: grid;
	grid-template-columns: 34px 1fr;
	gap: 8px;
	font-size: 12.5px;
	margin-top: 7px;
	line-height: 1.6;
}
.ad-lbl {
	color: var(--muted);
}

.la-note {
	font-size: 12.5px;
	color: var(--muted);
	line-height: 1.85;
	margin: 24px 0 8px;
	padding-left: 12px;
	border-left: 2px solid var(--line);
}

@media (max-width: 900px) {
	.fam-head {
		grid-template-columns: 60px auto 1fr 20px;
		gap: 10px;
		padding: 13px 14px;
	}
	.lang-grid,
	.scr-grid,
	.ad-grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}
@media (max-width: 560px) {
	.lang-grid,
	.scr-grid,
	.ad-grid {
		grid-template-columns: minmax(0, 1fr);
	}
	.fam-head {
		grid-template-columns: 1fr 20px;
	}
	.fam-bar,
	.fam-meta {
		display: none;
	}
}
</style>
