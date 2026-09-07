<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import CoverImage from "@/components/CoverImage.vue";
import InteractionBar from "@/components/InteractionBar.vue";
import Badge from "@/components/Badge.vue";
import { ethnicApi } from "@/api/modules";
import type { EthnicDetail } from "@/api/types";
import {
	formatNumber,
	parseArray,
	artCategoryLabel,
	festivalTypeLabel,
	ethnicImagePrompt,
} from "@/utils/format";
import { useLangStore } from "@/stores/lang";

const route = useRoute();
const lang = useLangStore();

const detail = ref<EthnicDetail | null>(null);
const loading = ref(true);
const error = ref(false);

const TABS = [
	{ key: "overview", label: "概况" },
	{ key: "customs", label: "风俗" },
	{ key: "festivals", label: "节日" },
	{ key: "arts", label: "艺术" },
	{ key: "foods", label: "美食" },
	{ key: "locations", label: "聚居地" },
	{ key: "gallery", label: "图集" },
];

const activeTab = ref("overview");

function tabFromHash() {
	const h = route.hash.replace("#", "");
	if (h && TABS.some((t) => t.key === h)) activeTab.value = h;
}

watch(() => route.hash, tabFromHash);

async function load() {
	loading.value = true;
	error.value = false;
	try {
		const id = route.params.id as string;
		detail.value = await ethnicApi.detail(id);
	} catch {
		error.value = true;
	} finally {
		loading.value = false;
	}
}

onMounted(() => {
	tabFromHash();
	load();
});

const festivals = computed(() =>
	(detail.value?.festivals || []).map((f) => ({
		...f,
		customs: parseArray(f.customs),
	})),
);
const arts = computed(() =>
	(detail.value?.arts || []).map((a) => ({
		...a,
		inheritors: parseArray(a.inheritors),
	})),
);
// 长文 description：按空行分段，【】行作为小节标题
const descriptionBlocks = computed(() => {
	const text =
		lang.pick(
			detail.value?.description || "",
			detail.value?.descriptionEn || "",
		) || "";
	return text
		.split(/\n{2,}/)
		.map((p) => p.trim())
		.filter(Boolean)
		.map((p) =>
			/^【.+】/.test(p)
				? { kind: "heading" as const, text: p }
				: { kind: "para" as const, text: p },
		);
});
const galleryImages = computed(() => {
	const imgs: { src: string; caption: string }[] = [];
	const d = detail.value;
	if (!d) return imgs;
	const seen = new Set<string>();
	const push = (src: string | null | undefined, caption: string) => {
		if (!src || seen.has(src)) return;
		seen.add(src);
		imgs.push({ src, caption });
	};
	if (d.coverImage) push(d.coverImage, d.name);
	d.festivals.forEach((f) =>
		parseArray(f.images).forEach((i) => push(i, `${d.name} · ${f.name}`)),
	);
	d.foods.forEach((fd) => push(fd.image, `${d.name} · ${fd.name}`));
	d.customs.forEach((c) => push(c.image, c.title));
	return imgs;
});

const heroImageSrc = computed(() => detail.value?.coverImage || "");
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/ethnic">民族</router-link> ›
			<span>{{ detail?.name || "加载中" }}</span>
		</div>
	</div>

	<!-- Hero 横幅 -->
	<div
		v-if="detail"
		class="detail-hero"
	>
		<div class="container">
			<div class="inner">
				<div>
					<div
						class="badge-wrap"
						style="margin-bottom: 14px"
					>
						<Badge
							:text="
								lang.pick(detail.languageFamily || '民族', detail.nameEn || '')
							"
						/>
					</div>
					<h1>{{ lang.pick(detail.name, detail.nameEn) }}</h1>
					<div
						v-if="detail.selfName"
						class="self-name"
					>
						{{ detail.selfName }}
					</div>
					<p class="summary">
						{{ lang.pick(detail.summary, detail.summaryEn) }}
					</p>

					<div class="metrics">
						<div class="metric">
							<div class="num">{{ formatNumber(detail.population) }}</div>
							<div class="lbl">人口 Population</div>
						</div>
						<div class="metric">
							<div class="num">{{ detail.region?.length || 0 }}</div>
							<div class="lbl">聚居省区 Regions</div>
						</div>
						<div class="metric">
							<div class="num">{{ festivals.length }}</div>
							<div class="lbl">节日 Festivals</div>
						</div>
						<div class="metric">
							<div class="num">{{ arts.length }}</div>
							<div class="lbl">艺术 Arts</div>
						</div>
					</div>
				</div>
				<div class="hero-img">
					<CoverImage
						:src="heroImageSrc"
						:name="detail.name"
						:theme="detail.themeColor"
						:prompt="ethnicImagePrompt(detail.name)"
						size="landscape_16_9"
					/>
				</div>
			</div>
		</div>
	</div>

	<div class="container detail-body">
		<el-skeleton
			v-if="loading"
			:rows="8"
			animated
		/>
		<el-empty
			v-else-if="error"
			description="民族信息加载失败"
		>
			<el-button
				type="primary"
				@click="load"
				>重试</el-button
			>
		</el-empty>

		<template v-else-if="detail">
			<InteractionBar
				type="ethnic"
				:id="detail.id"
			/>

			<div
				class="tabs"
				role="tablist"
			>
				<button
					v-for="t in TABS"
					:key="t.key"
					class="tab"
					:class="{ active: activeTab === t.key }"
					role="tab"
					:aria-selected="activeTab === t.key"
					@click="activeTab = t.key"
				>
					{{ t.label }}
				</button>
			</div>

			<!-- 概况 -->
			<div
				v-show="activeTab === 'overview'"
				class="tab-pane"
			>
				<div class="duo">
					<div class="panel">
						<h3>民族简介</h3>
						<div class="article">
							<template v-if="descriptionBlocks.length">
								<template
									v-for="(b, i) in descriptionBlocks"
									:key="i"
								>
									<h4
										v-if="b.kind === 'heading'"
										class="sec-title"
									>
										{{ b.text }}
									</h4>
									<p
										v-else
										:class="{ dropcap: i === 0 }"
									>
										{{ b.text }}
									</p>
								</template>
							</template>
							<p
								v-else
								class="dropcap"
							>
								{{ lang.pick(detail.summary, detail.summaryEn) }}
							</p>
						</div>
					</div>
					<div class="panel">
						<h3>基本资料</h3>
						<div class="info-card">
							<dl>
								<dt>人口</dt>
								<dd>{{ formatNumber(detail.population) }}</dd>
								<dt>语系</dt>
								<dd>{{ detail.languageFamily }}</dd>
								<dt>语言</dt>
								<dd>{{ detail.languages?.join("、") }}</dd>
								<dt>文字</dt>
								<dd>{{ detail.scripts?.join("、") }}</dd>
								<dt>宗教</dt>
								<dd>{{ detail.religion?.join("、") }}</dd>
								<dt>聚居地</dt>
								<dd>{{ detail.region?.join("、") }}</dd>
								<dt>标签</dt>
								<dd>
									<div class="tags">
										<Badge
											v-for="tag in detail.tags"
											:key="tag"
											:text="tag"
											ink
										/>
									</div>
								</dd>
							</dl>
						</div>
					</div>
				</div>
			</div>

			<!-- 风俗 -->
			<div
				v-show="activeTab === 'customs'"
				class="tab-pane"
			>
				<div class="panel-block">
					<h3>风俗习惯</h3>
					<template v-if="detail.customs?.length">
						<div
							v-for="c in detail.customs"
							:key="c.id"
							class="custom-item"
						>
							<span class="cat">{{ c.category }}</span>
							<h4>{{ c.title }}</h4>
							<p>{{ c.content.slice(0, 120) }}...</p>
							<router-link
								class="custom-more"
								:to="`/custom/${c.id}`"
								>查看详细风俗 &gt;&gt;</router-link
							>
						</div>
					</template>
					<el-empty
						v-else
						description="暂无风俗记录"
					/>
				</div>
			</div>

			<!-- 节日 -->
			<div
				v-show="activeTab === 'festivals'"
				class="tab-pane"
			>
				<div class="panel-block">
					<h3>节日庆典</h3>
					<template v-if="festivals.length">
						<div class="grid grid-3">
							<router-link
								v-for="f in festivals"
								:key="f.id"
								class="feature"
								:to="`/festival/${f.id}`"
							>
								<div class="img">
									<CoverImage
										:src="f.coverImage"
										:name="f.name"
										:theme="detail.themeColor"
										:prompt="`${f.name} festival celebration, photography`"
										size="landscape_4_3"
									/>
								</div>
								<div class="t">
									<span class="no">{{
										festivalTypeLabel[f.type] || f.type
									}}</span>
									<h4>{{ f.name }}</h4>
									<p>{{ f.lunarDate || f.solarDate || "日期待考" }}</p>
									<p>{{ (f.customs as string[]).slice(0, 3).join(" · ") }}</p>
								</div>
							</router-link>
						</div>
					</template>
					<el-empty
						v-else
						description="暂无节日记录"
					/>
				</div>
			</div>

			<!-- 艺术 -->
			<div
				v-show="activeTab === 'arts'"
				class="tab-pane"
			>
				<div class="panel-block">
					<h3>传统艺术</h3>
					<template v-if="arts.length">
						<div class="grid grid-3">
							<router-link
								v-for="a in arts"
								:key="a.id"
								class="feature"
								:to="`/art/${a.id}`"
							>
								<div class="img">
									<CoverImage
										:src="a.coverImage"
										:name="a.name"
										:theme="detail.themeColor"
										:prompt="`${a.name} traditional art, photography`"
										size="landscape_4_3"
									/>
								</div>
								<div class="t">
									<span class="no">{{
										artCategoryLabel[a.category] || a.category
									}}</span>
									<h4>{{ a.name }}</h4>
									<p v-if="a.description">{{ a.description }}</p>
									<p
										v-if="a.origin"
										class="origin"
									>
										发展沿革：{{ a.origin }}
									</p>
									<p>
										传承人：{{ (a.inheritors as string[]).join("、") || "—" }}
									</p>
								</div>
							</router-link>
						</div>
					</template>
					<el-empty
						v-else
						description="暂无艺术记录"
					/>
				</div>
			</div>

			<!-- 美食 -->
			<div
				v-show="activeTab === 'foods'"
				class="tab-pane"
			>
				<div class="panel-block">
					<h3>特色美食</h3>
					<template v-if="detail.foods?.length">
						<div class="grid grid-3">
							<div
								v-for="fd in detail.foods"
								:key="fd.id"
								class="feature"
								style="cursor: default"
							>
								<div class="img">
									<CoverImage
										:src="fd.image"
										:name="fd.name"
										:theme="detail.themeColor"
										:prompt="`${fd.name} ethnic food, food photography`"
										size="landscape_4_3"
									/>
								</div>
								<div class="t">
									<span class="no">{{ lang.pick("FOOD · 美食", "FOOD") }}</span>
									<h4>{{ lang.pick(fd.name, fd.nameEn) }}</h4>
									<p>{{ fd.description }}</p>
									<p
										v-if="fd.origin"
										class="origin"
									>
										发展沿革：{{ fd.origin }}
									</p>
								</div>
							</div>
						</div>
					</template>
					<el-empty
						v-else
						description="暂无美食记录"
					/>
				</div>
			</div>

			<!-- 聚居地 -->
			<div
				v-show="activeTab === 'locations'"
				class="tab-pane"
			>
				<div class="panel-block">
					<h3>主要聚居地</h3>
					<template v-if="detail.locations?.length">
						<div class="timeline">
							<div
								v-for="loc in detail.locations"
								:key="loc.id"
								class="tl-item"
							>
								<div class="date">{{ loc.province }}</div>
								<h5>{{ loc.city }}</h5>
								<p v-if="loc.description">{{ loc.description }}</p>
								<p v-if="loc.longitude">
									坐标：{{ loc.longitude }}, {{ loc.latitude }}
								</p>
							</div>
						</div>
					</template>
					<el-empty
						v-else
						description="暂无聚居地数据"
					/>
				</div>
			</div>

			<!-- 图集 -->
			<div
				v-show="activeTab === 'gallery'"
				class="tab-pane"
			>
				<div class="panel-block">
					<h3>图集 / 影像</h3>
					<template v-if="galleryImages.length">
						<div class="gallery">
							<figure
								v-for="(g, i) in galleryImages"
								:key="i"
								class="g-item"
							>
								<CoverImage
									:src="g.src"
									:name="detail.name"
									:theme="detail.themeColor"
									:prompt="ethnicImagePrompt(detail.name)"
									size="square"
								/>
								<figcaption>{{ g.caption }}</figcaption>
							</figure>
						</div>
					</template>
					<el-empty
						v-else
						description="暂无影像资料"
					/>
				</div>
			</div>
		</template>
	</div>
</template>

<style scoped>
.custom-more {
	display: inline-block;
	margin-top: 6px;
	font-size: 13px;
	color: var(--accent);
	text-decoration: none;
}
.custom-more:hover {
	text-decoration: underline;
}
</style>
