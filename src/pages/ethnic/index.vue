<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import PageHead from "@/components/PageHead.vue";
import EthnicCard from "@/components/EthnicCard.vue";
import AppPagination from "@/components/AppPagination.vue";
import { ethnicApi } from "@/api/modules";
import type { EthnicListItem } from "@/api/types";
import { formatNumber } from "@/utils/format";
import { useLangStore } from "@/stores/lang";
import { SortUp, SortDown } from "@element-plus/icons-vue";
const lang = useLangStore();

const REGIONS = ["东北", "西北", "西南", "中南", "东南", "内蒙古", "其他"];
const FAMILIES = [
	"汉藏语系",
	"阿尔泰语系",
	"南岛语系",
	"南亚语系",
	"印欧语系",
	"混合",
];
const SORTS = ref([
	{ key: "orderNum", value: "orderNum,asc", label: "默认排序" },
	{ key: "population", value: "population,asc", label: "人口", status: "Up" },
	{ key: "pinyin", value: "pinyin,asc", label: "拼音 A–Z" },
]);
// <el-icon><SortUp /></el-icon>
// <el-icon><SortDown /></el-icon>
const filter = reactive({
	region: "",
	languageFamily: "",
	sort: "orderNum,asc",
});
const list = ref<EthnicListItem[]>([]);
const total = ref(0);
const page = ref(0); // 0 基
const size = 12;
const loading = ref(false);
const errored = ref(false);

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		const res = await ethnicApi.list({
			page: page.value,
			size,
			sort: filter.sort,
			region: filter.region || undefined,
			languageFamily: filter.languageFamily || undefined,
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

function pickRegion(r: string) {
	filter.region = filter.region === r ? "" : r;
	page.value = 0;
	load();
}
function pickFamily(f: string) {
	filter.languageFamily = filter.languageFamily === f ? "" : f;
	page.value = 0;
	load();
}
function pickSort(key: string, s: string) {
	if (key === "population") {
		SORTS.value = SORTS.value.map((item) => {
			if (item.key === "population" && item.status === "Up") {
				item.value = "population,asc";
				s = "population,asc";
				item.status = "Down";
			} else if (item.key === "population" && item.status === "Down") {
				item.value = "population,desc";
				s = "population,desc";
				item.status = "Up";
			}
			return item;
		});
	}

	filter.sort = s;
	page.value = 0;
	load();
}
function reset() {
	filter.region = "";
	filter.languageFamily = "";
	filter.sort = "orderNum,asc";
	page.value = 0;
	load();
}
function onPage(p: number) {
	page.value = p;
	load();
}

onMounted(load);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> › <span>民族</span>
		</div>

		<PageHead
			:kicker="lang.pick('ETHNIC GROUPS · 民族频道', 'ETHNIC GROUPS')"
			:title="lang.pick('五十六个民族', 'The 56 Ethnic Groups')"
			:dek="
				lang.pick(
					'每一个民族都是中华文化图谱上不可或缺的一抹色彩。按地域、语系与人口，探索各民族的独特风情。',
					'Every group adds its own colour to the tapestry of Chinese culture. Explore by region, language family and population.',
				)
			"
		/>

		<div class="filter">
			<div class="row">
				<span class="label">地域</span>
				<button
					v-for="r in REGIONS"
					:key="r"
					class="chip"
					:class="{ active: filter.region === r }"
					@click="pickRegion(r)"
				>
					{{ r }}
				</button>
				<button
					v-if="filter.region"
					class="chip"
					@click="pickRegion(filter.region)"
				>
					✕ 清除
				</button>
			</div>
			<div class="row">
				<span class="label">语系</span>
				<button
					v-for="f in FAMILIES"
					:key="f"
					class="chip"
					:class="{ active: filter.languageFamily === f }"
					@click="pickFamily(f)"
				>
					{{ f }}
				</button>
				<button
					v-if="filter.languageFamily"
					class="chip"
					@click="pickFamily(filter.languageFamily)"
				>
					✕ 清除
				</button>
			</div>
			<div class="row">
				<span class="label">排序</span>
				<button
					v-for="s in SORTS"
					:key="s.value"
					class="chip"
					:class="{ active: filter.sort === s.value }"
					@click="pickSort(s.key, s.value)"
				>
					{{ s.label }}
					<el-icon v-if="s.key === 'population'">
						<SortUp v-if="s.status === 'Up'" />
						<SortDown v-if="s.status === 'Down'" />
					</el-icon>
				</button>
				<span style="flex: 1"></span>
				<button
					class="chip"
					@click="reset"
				>
					重置
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
			<el-empty description="民族数据加载失败">
				<el-button
					type="primary"
					@click="load"
					>重新加载</el-button
				>
			</el-empty>
		</div>

		<template v-else-if="list.length">
			<div class="grid grid-4">
				<EthnicCard
					v-for="e in list"
					:key="e.id"
					:id="e.id"
					:name="e.name"
					:cover-image="e.coverImage"
					:theme-color="e.themeColor"
					:meta="`${e.region?.[0] || '—'} · ${e.languageFamily || '—'} · 人口 ${formatNumber(e.population)}`"
				/>
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
			description="没有符合筛选条件的民族"
			style="padding: 48px 0"
		/>
	</div>
</template>
