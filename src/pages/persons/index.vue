<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import PageHead from "@/components/PageHead.vue";
import { personApi } from "@/api/modules";
import type { PersonDirectory, PersonItem } from "@/api/types";
import { heritageLabel } from "@/utils/format";
import { useLangStore } from "@/stores/lang";

/**
 * 人物专栏（B-7）：非遗代表性传承人 + 历史文化名家
 *
 * 设计要点 —— **角色区分**：
 * 数据中同时存在两类人物：现行认定的「代表性传承人」，以及京剧大师梅兰芳、
 * 书法家王羲之这类**历史文化名家**。两者性质不同，混同展示会造成事实性错误，
 * 因此本页在筛选、卡片徽标、详情提示三处都明确区分。
 */

const lang = useLangStore();

const data = ref<PersonDirectory | null>(null);
const loading = ref(true);
const errored = ref(false);

const keyword = ref("");
const domain = ref("");
const ethnic = ref("");
const roleType = ref("");

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		data.value = await personApi.list({
			keyword: keyword.value || undefined,
			domain: domain.value || undefined,
			ethnic: ethnic.value || undefined,
			roleType: roleType.value || undefined,
		});
	} catch {
		errored.value = true;
		data.value = null;
	} finally {
		loading.value = false;
	}
}

onMounted(load);

function pickDomain(v: string) {
	domain.value = domain.value === v ? "" : v;
	load();
}
function pickRole(v: string) {
	roleType.value = roleType.value === v ? "" : v;
	load();
}
function onEthnicChange() {
	load();
}
function reset() {
	keyword.value = "";
	domain.value = "";
	ethnic.value = "";
	roleType.value = "";
	load();
}

const hasFilter = computed(
	() => !!(keyword.value || domain.value || ethnic.value || roleType.value),
);

/** 名家单独成组展示（与在世传承人区分） */
const masters = computed(() => (data.value?.persons || []).filter((p) => p.roleType === "master"));
const inheritors = computed(() =>
	(data.value?.persons || []).filter((p) => p.roleType === "inheritor"),
);

/** 是否处于「只看名家」状态 */
const mastersOnly = computed(() => roleType.value === "master");
const inheritorsOnly = computed(() => roleType.value === "inheritor");

function personKey(p: PersonItem): string {
	return `${p.name}|${p.ethnicGroupName || ""}`;
}
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<router-link to="/heritage">{{ lang.pick("非遗名录", "Heritage") }}</router-link> ›
			<span>{{ lang.pick("人物", "People") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('PEOPLE · 人物专栏', 'PEOPLE')"
			:title="lang.pick('传承人与文化名家', 'Inheritors & Masters')"
			:dek="
				lang.pick(
					'文化的延续最终落在人的身上。这里收录非物质文化遗产代表性传承人，以及在中国艺术史上留下印记的历史文化名家。',
					'Culture lives through people. This directory gathers representative inheritors of intangible cultural heritage alongside historical masters of Chinese art.',
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
			<el-empty :description="lang.pick('人物数据加载失败', 'Failed to load people')">
				<el-button
					type="primary"
					@click="load"
					>{{ lang.pick("重新加载", "Retry") }}</el-button
				>
			</el-empty>
		</div>

		<template v-else>
			<!-- 概览 -->
			<div class="metrics pd-metrics">
				<div class="metric">
					<div class="num">{{ data.summary.personCount }}</div>
					<div class="lbl">{{ lang.pick("位人物", "People") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.inheritorCount }}</div>
					<div class="lbl">{{ lang.pick("位代表性传承人", "Inheritors") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.masterCount }}</div>
					<div class="lbl">{{ lang.pick("位历史文化名家", "Masters") }}</div>
				</div>
				<div class="metric">
					<div class="num">{{ data.summary.ethnicCount }}</div>
					<div class="lbl">{{ lang.pick("个民族", "Ethnic groups") }}</div>
				</div>
			</div>

			<!-- 角色说明：明确两类人物的区别 -->
			<div class="role-note">
				<div class="rn-item inheritor">
					<span class="rn-tag">{{ lang.pick("代表性传承人", "Inheritor") }}</span>
					<span class="rn-desc">
						{{
							lang.pick(
								"由文化和旅游部认定的国家级非物质文化遗产代表性项目代表性传承人（第 1–6 批）。",
								"Representative inheritors designated by the Ministry of Culture and Tourism (batches 1–6).",
							)
						}}
					</span>
				</div>
				<div class="rn-item master">
					<span class="rn-tag">{{ lang.pick("历史文化名家", "Historical master") }}</span>
					<span class="rn-desc">
						{{
							lang.pick(
								"在相关艺术领域具有历史地位的代表性人物（如京剧大师、书法名家），并非现行认定的非遗传承人。",
								"Historically significant figures in the field — not currently designated heritage inheritors.",
							)
						}}
					</span>
				</div>
			</div>

			<!-- 筛选 -->
			<div class="filter">
				<div class="row">
					<span class="label">{{ lang.pick("角色", "Role") }}</span>
					<button
						v-for="r in data.filters.roles"
						:key="r.value"
						class="chip"
						:class="{ active: roleType === r.value }"
						@click="pickRole(r.value)"
					>
						{{ r.label }}（{{ r.count }}）
					</button>
				</div>
				<div class="row">
					<span class="label">{{ lang.pick("领域", "Domain") }}</span>
					<button
						v-for="d in data.filters.domains"
						:key="d.value"
						class="chip"
						:class="{ active: domain === d.value }"
						@click="pickDomain(d.value)"
					>
						{{ d.label }}（{{ d.count }}）
					</button>
				</div>
				<div class="row">
					<span class="label">{{ lang.pick("民族", "Group") }}</span>
					<el-select
						v-model="ethnic"
						:placeholder="lang.pick('全部民族', 'All groups')"
						clearable
						filterable
						style="width: 190px"
						@change="onEthnicChange"
					>
						<el-option
							v-for="e in data.filters.ethnics"
							:key="e.value"
							:label="`${e.label}（${e.count}）`"
							:value="e.value"
						/>
					</el-select>
					<span
						class="label"
						style="margin-left: 8px"
						>{{ lang.pick("搜索", "Search") }}</span
					>
					<el-input
						v-model="keyword"
						:placeholder="lang.pick('姓名或非遗项目', 'Name or heritage item')"
						clearable
						style="width: 240px"
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
				{{ lang.pick(`共 ${data.total} 位人物`, `${data.total} people`) }}
			</div>

			<!-- 历史文化名家（置顶，与传承人分组） -->
			<section
				v-if="masters.length && !inheritorsOnly"
				class="pd-section"
			>
				<div class="panel-head">
					<h3>{{ lang.pick("历史文化名家", "Historical masters") }}</h3>
					<span class="sh-note">
						{{ lang.pick("非现行认定的非遗传承人", "Not currently designated inheritors") }}
					</span>
				</div>
				<div class="pd-grid masters">
					<article
						v-for="p in masters"
						:key="personKey(p)"
						class="pd-card master"
					>
						<div class="pc-head">
							<span class="pc-avatar master">{{ p.name.slice(0, 1) }}</span>
							<div class="pc-id">
								<h4>{{ p.name }}</h4>
								<span class="pc-meta">
									{{ p.ethnicGroupName }}
									<template v-if="p.lifespan"> · {{ p.lifespan }}</template>
								</span>
							</div>
							<span class="pc-role master">{{ p.roleLabel }}</span>
						</div>
						<p
							v-if="p.bio"
							class="pc-bio"
						>
							{{ p.bio }}
						</p>
						<div class="pc-projects">
							<router-link
								v-for="pr in p.projects"
								:key="pr.id"
								class="pc-proj"
								:to="pr.detailPath"
							>
								<span class="pp-name">{{ pr.name }}</span>
								<span
									v-if="pr.intangibleHeritage"
									class="pp-lv"
									:class="pr.intangibleHeritage"
								>
									{{ heritageLabel[pr.intangibleHeritage] || "" }}
								</span>
							</router-link>
						</div>
					</article>
				</div>
			</section>

			<!-- 代表性传承人 -->
			<section
				v-if="inheritors.length && !mastersOnly"
				class="pd-section"
			>
				<div
					v-if="masters.length"
					class="panel-head"
				>
					<h3>{{ lang.pick("代表性传承人", "Representative inheritors") }}</h3>
					<span class="sh-note">{{ inheritors.length }} {{ lang.pick("位", "people") }}</span>
				</div>
				<div class="pd-grid">
					<article
						v-for="p in inheritors"
						:key="personKey(p)"
						class="pd-card"
					>
						<div class="pc-head">
							<span
								class="pc-avatar"
								:class="{ world: p.topLevel === 'world' }"
								>{{ p.name.slice(0, 1) }}</span
							>
							<div class="pc-id">
								<h4>{{ p.name }}</h4>
								<span class="pc-meta">
									{{ p.ethnicGroupName }}
									<template v-if="p.domain"> · {{ p.domain }}</template>
								</span>
							</div>
							<span
								v-if="p.topLevel"
								class="pc-level"
								:class="p.topLevel"
							>
								{{ heritageLabel[p.topLevel] || "" }}
							</span>
						</div>
						<div class="pc-projects">
							<router-link
								v-for="pr in p.projects"
								:key="pr.id"
								class="pc-proj"
								:to="pr.detailPath"
							>
								<span class="pp-name">{{ pr.name }}</span>
								<span
									v-if="pr.intangibleHeritage && pr.intangibleHeritage !== p.topLevel"
									class="pp-lv"
									:class="pr.intangibleHeritage"
								>
									{{ heritageLabel[pr.intangibleHeritage] || "" }}
								</span>
							</router-link>
						</div>
					</article>
				</div>
			</section>

			<el-empty
				v-if="!masters.length && !inheritors.length"
				:description="lang.pick('没有符合条件的人物', 'No people match the filters')"
				style="padding: 48px 0"
			/>

			<p class="pd-note">
				{{
					lang.pick(
						"传承人数据来源于文化和旅游部公布的国家级非物质文化遗产代表性项目代表性传承人第 1–6 批名单；同名不同族的人物（如「马金山」同为回族花儿与东乡族花儿的传承人）分别成条。未收录生平的传承人仅展示其民族与项目，不作推测性描述。",
						'Inheritor data comes from the Ministry of Culture and Tourism designations (batches 1–6). People sharing a name but belonging to different ethnic groups are listed separately. No speculative biographies are added.',
					)
				}}
			</p>
		</template>
	</div>
</template>

<style scoped>
.pd-metrics {
	margin: 4px 0 20px;
}

/* 角色说明 */
.role-note {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin-bottom: 22px;
}
.rn-item {
	background: var(--paper);
	padding: 13px 16px;
	display: flex;
	align-items: baseline;
	gap: 10px;
	flex-wrap: wrap;
}
.rn-tag {
	font-size: 11px;
	letter-spacing: 0.06em;
	padding: 2px 8px;
	border: 1px solid var(--accent);
	color: var(--accent);
	white-space: nowrap;
}
.rn-item.master .rn-tag {
	border-color: var(--muted);
	color: var(--muted);
}
.rn-desc {
	font-size: 12px;
	line-height: 1.7;
	color: var(--muted);
	flex: 1;
	min-width: 0;
}

/* 分组 */
.pd-section {
	margin-bottom: 30px;
}
.pd-section .panel-head {
	display: flex;
	align-items: baseline;
	gap: 12px;
	border-bottom: 2px solid var(--ink);
	padding-bottom: 10px;
	margin-bottom: 16px;
}
.pd-section .panel-head h3 {
	font-family: var(--serif);
	font-size: 21px;
	letter-spacing: 0.06em;
}
.pd-section .sh-note {
	font-size: 11.5px;
	color: var(--muted);
}

/* 网格 */
.pd-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.pd-grid.masters {
	grid-template-columns: repeat(2, minmax(0, 1fr));
}
.pd-card {
	background: var(--paper);
	padding: 16px 18px 18px;
	min-width: 0;
	transition: background var(--dur-fast) ease;
}
.pd-card:hover {
	background: var(--paper-2);
}
.pd-card.master {
	background: color-mix(in srgb, var(--ink) 3%, var(--paper));
}

/* 卡片头部 */
.pc-head {
	display: flex;
	align-items: center;
	gap: 11px;
	margin-bottom: 11px;
}
.pc-avatar {
	width: 38px;
	height: 38px;
	flex: none;
	display: flex;
	align-items: center;
	justify-content: center;
	font-family: var(--serif);
	font-size: 19px;
	font-weight: 700;
	color: #fff;
	background: var(--muted);
}
.pc-avatar.world {
	background: var(--accent);
}
.pc-avatar.master {
	background: var(--ink);
}
.pc-id {
	flex: 1;
	min-width: 0;
}
.pc-id h4 {
	font-family: var(--serif);
	font-size: 17.5px;
	letter-spacing: 0.02em;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.pc-meta {
	font-size: 11.5px;
	color: var(--muted);
}
.pc-level,
.pc-role {
	font-size: 10px;
	letter-spacing: 0.05em;
	padding: 2px 6px;
	border: 1px solid var(--line);
	color: var(--muted);
	white-space: nowrap;
	flex: none;
}
.pc-level.world {
	border-color: var(--accent);
	color: var(--accent);
	font-weight: 700;
}
.pc-role.master {
	border-color: var(--muted);
	color: var(--muted);
}

.pc-bio {
	font-size: 12.5px;
	line-height: 1.8;
	color: var(--ink-soft);
	margin-bottom: 10px;
}

/* 关联项目 */
.pc-projects {
	display: flex;
	flex-direction: column;
	gap: 6px;
	padding-top: 10px;
	border-top: 1px dashed var(--line);
}
.pc-proj {
	display: flex;
	align-items: baseline;
	gap: 7px;
	flex-wrap: wrap;
}
.pp-name {
	font-family: var(--serif);
	font-size: 13.5px;
	transition: color var(--dur-fast) ease;
}
.pc-proj:hover .pp-name {
	color: var(--accent);
}
.pp-lv {
	font-size: 10px;
	padding: 1px 5px;
	border: 1px solid var(--line);
	color: var(--muted);
	white-space: nowrap;
}
.pp-lv.world {
	border-color: var(--accent);
	color: var(--accent);
}

.pd-note {
	font-size: 12.5px;
	color: var(--muted);
	line-height: 1.85;
	margin: 10px 0 8px;
	padding-left: 12px;
	border-left: 2px solid var(--line);
}

@media (max-width: 900px) {
	.role-note {
		grid-template-columns: minmax(0, 1fr);
	}
	.pd-grid,
	.pd-grid.masters {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
	.filter .row {
		flex-wrap: wrap;
	}
}
@media (max-width: 560px) {
	.pd-grid,
	.pd-grid.masters {
		grid-template-columns: minmax(0, 1fr);
	}
}
</style>
