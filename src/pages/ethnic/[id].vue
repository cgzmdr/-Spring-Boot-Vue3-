<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch, watchEffect } from "vue";
import { useRoute } from "vue-router";
import { useElementSize, useWindowScroll } from "@vueuse/core";
import CoverImage from "@/components/CoverImage.vue";
import InteractionBar from "@/components/InteractionBar.vue";
import Badge from "@/components/Badge.vue";
import Reveal from "@/components/Reveal.vue";
import RichArticle from "@/components/RichArticle.vue";
import EthnicHistoryView from "@/components/EthnicHistoryView.vue";
import LinkedDiscussion from "@/components/LinkedDiscussion.vue";
import SourceReferences from "@/components/SourceReferences.vue";
import ImageCredits from "@/components/ImageCredits.vue";
import ImageCreditBadge from "@/components/ImageCreditBadge.vue";
import { ethnicApi } from "@/api/modules";
import type { EthnicDetail } from "@/api/types";
import {
	formatNumber,
	parseArray,
	artCategoryLabel,
	festivalTypeLabel,
	ethnicImagePrompt,
} from "@/utils/format";
import { parseArticle, extractSection } from "@/utils/article";
import type { ArticleFigure } from "@/utils/article";
import { useLangStore } from "@/stores/lang";
import { useViewTracking } from "@/composables/useViewTracking";

const route = useRoute();
const lang = useLangStore();

const detail = ref<EthnicDetail | null>(null);
const loading = ref(true);
const error = ref(false);

// 浏览行为上报（方向 D：个性化推荐的隐式信号；未登录时后端静默忽略）。
// 必须放在 detail 声明之后：getter 引用 detail，提前定义会命中暂时性死区。
useViewTracking("ethnic", () => detail.value?.id);

/** 「历史沿革」小节关键词（后端民族简介以此作为小节标题） */
const HISTORY_KEY = "历史沿革";
/** 侧栏「基本资料」收起状态（记忆用户偏好，切换为单栏阅读） */
const SIDEBAR_KEY = "cnd_ethnic_sidebar_collapsed";
const sidebarCollapsed = ref(localStorage.getItem(SIDEBAR_KEY) === "1");

/** 侧栏内容实测高度：作为收起动画的 max-height 起点，避免用固定像素造成过渡迟滞 */
const sidebarBodyEl = ref<HTMLElement | null>(null);
const { height: sidebarBodyHeight } = useElementSize(
	sidebarBodyEl,
	{ width: 0, height: 0 },
	{ box: "border-box" },
);
const panelBodyHeight = ref(0);
watchEffect(() => {
	// 仅在展开态量测并锁定：收起过程中栏宽变化会引起内容重排，其高度不能作为动画依据
	if (!sidebarCollapsed.value && sidebarBodyHeight.value > 0) {
		panelBodyHeight.value = Math.round(sidebarBodyHeight.value) + 130;
	}
});

const sidebarStyle = computed(() => ({
	"--panel-body-h": panelBodyHeight.value
		? `${panelBodyHeight.value}px`
		: undefined,
}));

function toggleSidebar() {
	sidebarCollapsed.value = !sidebarCollapsed.value;
	localStorage.setItem(SIDEBAR_KEY, sidebarCollapsed.value ? "1" : "0");
}

const activeTab = ref("overview");

/* ---------------------------------------------------------------------------
   「民族简介」头部吸顶（sticky）
   · 长文简介滚动时始终保留「民族简介 + 收起/展开基本资料」这一行操作入口；
   · 吸顶基准线 = 报头高度（--mast-h，报头收缩后会变化）+ 吸顶 Tabs 实测高度；
   · 吸顶后加一层投影，与滚动中的正文分层。
   --------------------------------------------------------------------------- */
const introHeadEl = ref<HTMLElement | null>(null);
const introHeadStuck = ref(false);
const tabsEl = ref<HTMLElement | null>(null);
const { height: tabsHeight } = useElementSize(
	tabsEl,
	{ width: 0, height: 0 },
	{ box: "border-box" },
);

/** 吸顶基准线（px）：报头实际高度 + Tabs 实际高度（未量到高度时用与 CSS 一致的兜底值） */
function stickyOffset(): number {
	const raw = getComputedStyle(document.documentElement).getPropertyValue("--mast-h");
	const mast = Number.parseFloat(raw) || 0;
	const tabs = tabsHeight.value || 46;
	return mast + Math.round(tabs);
}

/** 头部是否已吸顶（贴住基准线即视为吸顶） */
function syncIntroHeadStuck() {
	const el = introHeadEl.value;
	introHeadStuck.value = !!el && el.getBoundingClientRect().top <= stickyOffset() + 1;
}

const { y: windowScrollY } = useWindowScroll();
watch(windowScrollY, syncIntroHeadStuck);
watch([tabsHeight, detail, activeTab], () => nextTick(syncIntroHeadStuck));

const TAB_DEFS = [
	{ key: "overview", label: "概况" },
	{ key: "history", label: "历史沿革" },
	{ key: "customs", label: "风俗" },
	{ key: "festivals", label: "节日" },
	{ key: "arts", label: "艺术" },
	{ key: "foods", label: "美食" },
	{ key: "locations", label: "聚居地" },
	{ key: "gallery", label: "图集" },
	{ key: "sources", label: "参考资料" },
	{ key: "discussion", label: "讨论" },
];

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

/* ---------------------------------------------------------------------------
   关联信息（信息量增强）
   后端按民族名匹配得到；这里只做展示层排序与文案映射。
   --------------------------------------------------------------------------- */

/** 关联人物：代表性传承人排在历史文化名家之前（与人物专栏的分类口径一致） */
const relatedPersons = computed(() => {
	const list = [...(detail.value?.persons || [])];
	const weight = (r: string | null) =>
		r === "inheritor" ? 0 : r === "master" ? 1 : 2;
	return list.sort((a, b) => weight(a.roleType) - weight(b.roleType));
});

/** 关联自治地方：后端已按级别排序，这里保持原顺序 */
const relatedAreas = computed(() => detail.value?.autonomousAreas || []);

/** 人物角色中文名（与人物专栏保持一致） */
function personRoleLabel(role: string | null) {
	if (role === "inheritor") return "代表性传承人";
	if (role === "master") return "历史文化名家";
	return "人物";
}

/** 自治地方级别中文名 */
function areaLevelLabel(level: string | null) {
	switch (level) {
		case "autonomous_region":
			return "自治区";
		case "autonomous_prefecture":
			return "自治州";
		case "autonomous_county":
			return "自治县·旗";
		default:
			return "自治地方";
	}
}

/** 长文简介：解析为结构化段落，并把「历史沿革」小节单独拆出成一栏 */
const article = computed(() => {
	const text =
		lang.pick(
			detail.value?.description || "",
			detail.value?.descriptionEn || "",
		) || "";
	const blocks = parseArticle(text);
	return extractSection(blocks, HISTORY_KEY);
});
/** 民族简介正文（已剔除历史沿革） */
const introBlocks = computed(() => article.value.rest);
/** 历史沿革段落 */
const historyBlocks = computed(() => article.value.section);

/**
 * 结构化历史沿革（方向 C-2）：后端解析【历史沿革】小节得到时间轴 / 时代分期 / 段落索引。
 * 仅在中文语境下使用——该结构由中文原文解析而来（年份、朝代词均为中文表述），
 * 英文语境下回退到按英文正文分段的图文阅读。
 */
const historyData = computed(() => {
	if (lang.isEn) return null;
	const h = detail.value?.history;
	return h && h.paragraphCount > 0 ? h : null;
});

/** 「全文索引」中被跳转定位到的高亮段落下标 */
const historyJumpIndex = ref<number | null>(null);

/** 点击时代分期中的段落 → 滚动到「全文索引」里的对应段落并高亮 */
function jumpToParagraph(index: number) {
	historyJumpIndex.value = index;
	nextTick(() => {
		const el = document.getElementById(`hist-p-${index}`);
		el?.scrollIntoView({ behavior: "smooth", block: "center" });
	});
}

const hasHistory = computed(
	() => article.value.found && historyBlocks.value.length > 0,
);

/** 实际展示的 Tab：无历史沿革数据时自动隐藏该栏 */
const tabs = computed(() =>
	TAB_DEFS.filter((t) => t.key !== "history" || hasHistory.value),
);

function tabFromHash() {
	const h = route.hash.replace("#", "");
	if (h && tabs.value.some((t) => t.key === h)) activeTab.value = h;
}

watch(() => route.hash, tabFromHash);

/** 图集素材（用于正文配图与图集栏） */
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

/** 正文配图池：封面已在 Hero 展示，优先用节日 / 美食 / 风俗图片穿插 */
const introFigures = computed<ArticleFigure[]>(() => {
	const d = detail.value;
	if (!d) return [];
	const pool: ArticleFigure[] = [];
	const push = (src: string | null | undefined, caption: string) => {
		if (src) pool.push({ src, caption, theme: d.themeColor });
	};
	d.customs.forEach((c) => push(c.image, `${c.title} · ${c.category}`));
	d.foods.forEach((fd) => push(fd.image, `${fd.name} · 特色美食`));
	d.festivals.forEach((f) => push(f.coverImage, `${f.name} · 节日庆典`));
	pool.push({
		src: d.coverImage,
		caption: `${d.name} · 传统服饰`,
		theme: d.themeColor,
	});
	return pool;
});

/** 历史沿革配图池（用封面 / 聚居地做点缀） */
const historyFigures = computed<ArticleFigure[]>(() => {
	const d = detail.value;
	if (!d) return [];
	return [
		{ src: d.coverImage, caption: `${d.name} · 历史影像`, theme: d.themeColor },
	];
});

const heroImageSrc = computed(() => detail.value?.coverImage || "");

/** 美食详情页地址（携带所属民族，便于直接打开时定位数据） */
function foodLink(id: string) {
	return { path: `/food/${id}`, query: { ethnic: detail.value?.id || "" } };
}

/** 长文本摘要（模板中做截断展示） */
function brief(text?: string | null, max = 64) {
	if (!text) return "";
	return text.length > max ? `${text.slice(0, max)}…` : text;
}

/** 标签 → 标签搜索页：`/search?q=标签&from=tag`（搜索页据此展示「标签」来源提示） */
function tagSearch(tag: string) {
	return { path: "/search", query: { q: tag, from: "tag" } };
}
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
						v-motion-fade-in
					>
						<Badge
							:text="
								lang.pick(detail.languageFamily || '民族', detail.nameEn || '')
							"
						/>
					</div>
					<h1 v-motion-fade-up>{{ lang.pick(detail.name, detail.nameEn) }}</h1>
					<div
						v-if="detail.selfName"
						class="self-name"
						v-motion-fade-up
					>
						{{ detail.selfName }}
					</div>
					<p
						class="summary"
						v-motion-fade-up
					>
						{{ lang.pick(detail.summary, detail.summaryEn) }}
					</p>

					<div
						class="metrics"
						v-motion-fade-up
					>
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
				<div
					class="hero-img"
					v-motion-pop-in
				>
					<CoverImage
						:src="heroImageSrc"
						:name="detail.name"
						:theme="detail.themeColor"
						:prompt="ethnicImagePrompt(detail.name)"
						size="landscape_16_9"
					/>
					<!-- 图片署名角标（方向 C-4）：hover 显示作者 / 许可 / 来源 -->
					<ImageCreditBadge
						v-if="heroImageSrc"
						:path="heroImageSrc"
					/>
				</div>
			</div>
		</div>
	</div>

	<div
		class="container detail-body"
		:style="{ '--tabs-h': tabsHeight ? `${Math.round(tabsHeight)}px` : undefined }"
	>
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
				ref="tabsEl"
				class="tabs"
				role="tablist"
			>
				<button
					v-for="t in tabs"
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

			<!-- 概况：民族简介（图文并茂） + 基本资料（可收起，两栏 ⇄ 一栏） -->
			<div
				v-show="activeTab === 'overview'"
				class="tab-pane"
				v-auto-animate
			>
				<div
					class="duo"
					:class="{ 'is-single': sidebarCollapsed }"
					:style="sidebarStyle"
				>
					<div class="panel intro-panel">
						<!-- 吸顶标题行：长文滚动时仍可收起 / 展开基本资料 -->
						<div
							ref="introHeadEl"
							class="panel-head intro-head"
							:class="{ 'is-stuck': introHeadStuck }"
						>
							<h3>民族简介</h3>
							<span
								v-if="introBlocks.length"
								class="panel-hint"
							>
								{{ introBlocks.length }} 段落 · 图文并茂
							</span>
							<!-- 收起 / 展开基本资料：按钮始终固定在标题行最右侧 -->
							<button
								class="panel-toggle overview-toggle"
								:class="{ 'is-collapsed': sidebarCollapsed }"
								:aria-expanded="!sidebarCollapsed"
								:title="
									sidebarCollapsed
										? '展开基本资料，恢复两栏'
										: '收起基本资料，专注阅读民族简介'
								"
								@click="toggleSidebar"
							>
								<span>{{
									sidebarCollapsed ? "展开基本资料" : "收起基本资料"
								}}</span>
								<svg
									viewBox="0 0 24 24"
									fill="none"
									stroke="currentColor"
									stroke-width="2"
								>
									<path
										d="M6 15l6-6 6 6"
										stroke-linecap="round"
										stroke-linejoin="round"
									/>
								</svg>
							</button>
						</div>
						<RichArticle
							v-if="introBlocks.length"
							:blocks="introBlocks"
							:images="introFigures"
							:theme="detail.themeColor"
						/>
						<p
							v-else
							class="article"
						>
							{{ lang.pick(detail.summary, detail.summaryEn) }}
						</p>
					</div>

					<div class="panel">
						<div
							ref="sidebarBodyEl"
							class="panel-body"
						>
							<h3>基本资料</h3>
							<div
								class="info-card"
								style="border: none; padding: 0"
							>
								<dl>
									<dt>人口</dt>
									<dd>
										{{ formatNumber(detail.population) }}
										<!-- 人口定位：在 56 个民族中的位次，比孤立数字更有参照 -->
										<span
											v-if="detail.populationRank"
											class="rank-hint"
											:title="`按 2020 年七普口径人口降序，在 ${detail.populationTotal} 个已收录民族中排第 ${detail.populationRank} 位`"
										>
											第 {{ detail.populationRank }} / {{ detail.populationTotal }}
										</span>
									</dd>
									<dt>语系</dt>
									<dd>{{ detail.languageFamily }}</dd>
									<dt>语言</dt>
									<dd>{{ detail.languages?.join("、") || "—" }}</dd>
									<dt>文字</dt>
									<dd>{{ detail.scripts?.join("、") || "—" }}</dd>
									<dt>宗教</dt>
									<dd>{{ detail.religion?.join("、") || "—" }}</dd>
									<dt>聚居地</dt>
									<dd>{{ detail.region?.join("、") || "—" }}</dd>
									<dt>标签</dt>
									<dd>
										<!-- 标签可点击：跳转搜索页按标签检索（民族标签参与搜索命中） -->
										<div class="tags">
											<router-link
												v-for="tag in detail.tags"
												:key="tag"
												class="tag-link"
												:to="tagSearch(tag)"
												:title="`搜索标签「${tag}」相关内容`"
											>
												<Badge
													:text="tag"
													ink
												/>
											</router-link>
										</div>
									</dd>
								</dl>
							</div>
						</div>
					</div>

					<!--
						关联信息：把库中已有、但原先详情页未展示的两个维度补齐
						——「相关人物」与「自治地方」。两者都按民族名匹配（非外键），
						匹配不到时不渲染该区块，不做占位。
					-->
					<div
						v-if="relatedPersons.length || relatedAreas.length"
						class="panel related-panel"
					>
						<h3>关联信息</h3>

						<!-- 自治地方：按「自治区 → 自治州 → 自治县·旗」排序 -->
						<div
							v-if="relatedAreas.length"
							class="related-block"
						>
							<div class="related-title">
								民族自治地方
								<span class="related-count">{{ relatedAreas.length }} 处</span>
							</div>
							<ul class="related-list">
								<li
									v-for="a in relatedAreas"
									:key="a.id"
								>
									<span
										class="rel-badge"
										:style="{ background: detail.themeColor }"
									>
										{{ areaLevelLabel(a.level) }}
									</span>
									<router-link
										class="rel-name"
										:to="`/autonomous?keyword=${encodeURIComponent(a.name)}`"
										:title="`在「民族自治地方」中查看 ${a.name}`"
									>
										{{ a.name }}
									</router-link>
									<span
										v-if="a.establishedYear"
										class="rel-meta"
									>
										{{ a.establishedYear }} 年设立
									</span>
								</li>
							</ul>
						</div>

						<!-- 相关人物：代表性传承人 / 历史文化名家 -->
						<div
							v-if="relatedPersons.length"
							class="related-block"
						>
							<div class="related-title">
								相关人物
								<span class="related-count">{{ relatedPersons.length }} 位</span>
							</div>
							<ul class="related-list">
								<li
									v-for="p in relatedPersons"
									:key="p.id"
								>
									<span
										class="rel-badge"
										:style="{ background: detail.themeColor }"
									>
										{{ personRoleLabel(p.roleType) }}
									</span>
									<router-link
										class="rel-name"
										:to="`/persons?keyword=${encodeURIComponent(p.personName)}`"
										:title="`在「人物专栏」中查看 ${p.personName}`"
									>
										{{ p.personName }}
									</router-link>
									<span
										v-if="p.lifespan"
										class="rel-meta"
									>
										{{ p.lifespan }}
									</span>
									<span
										v-if="p.domain"
										class="rel-meta"
									>
										{{ p.domain }}
									</span>
								</li>
							</ul>
						</div>
					</div>
				</div>
			</div>

			<!-- 历史沿革：从民族简介中单独拆出的一栏 -->
			<div
				v-show="activeTab === 'history'"
				class="tab-pane"
				v-auto-animate
			>
				<div class="panel-block">
					<Reveal :y="16">
						<h3>历史沿革</h3>
						<p class="block-dek">{{ detail.name }}的历史脉络与变迁</p>
					</Reveal>
					<!-- 结构化视图（方向 C-2）：时间轴 / 时代分期 / 全文索引 -->
					<EthnicHistoryView
						v-if="historyData && historyData.paragraphCount > 0"
						:history="historyData"
						:name="detail.name"
						:theme="detail.themeColor"
						@jump="jumpToParagraph"
					/>
					<div v-else class="history-wrap">
						<RichArticle
							:blocks="historyBlocks"
							:images="historyFigures"
							:theme="detail.themeColor"
							:max-figures="2"
							:dropcap="false"
						/>
					</div>
				</div>
			</div>

			<!-- 风俗 -->
			<div
				v-show="activeTab === 'customs'"
				class="tab-pane"
				v-auto-animate
			>
				<div class="panel-block">
					<h3>风俗习惯</h3>
					<template v-if="detail.customs?.length">
						<div class="custom-list">
							<router-link
								v-for="c in detail.customs"
								:key="c.id"
								class="custom-item"
								:to="`/custom/${c.id}`"
								v-motion-fade-up
							>
								<div
									v-if="c.image"
									class="custom-thumb"
								>
									<CoverImage
										:src="c.image"
										:name="c.title"
										:theme="detail.themeColor"
										size="landscape_4_3"
									/>
								</div>
								<div class="custom-text">
									<span class="cat">{{ c.category }}</span>
									<h4>{{ c.title }}</h4>
									<p>{{ c.content.slice(0, 120) }}...</p>
									<span class="custom-more">查看详细风俗 &gt;&gt;</span>
								</div>
							</router-link>
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
				v-auto-animate
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
								v-motion-fade-up
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
				v-auto-animate
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
								v-motion-fade-up
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

			<!-- 美食：卡片可进入美食详情页 -->
			<div
				v-show="activeTab === 'foods'"
				class="tab-pane"
				v-auto-animate
			>
				<div class="panel-block">
					<h3>特色美食</h3>
					<template v-if="detail.foods?.length">
						<div class="grid grid-3">
							<router-link
								v-for="fd in detail.foods"
								:key="fd.id"
								class="feature"
								:to="foodLink(fd.id)"
								v-motion-fade-up
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
										发展沿革：{{ brief(fd.origin) }}
									</p>
									<span class="more-link">查看美食详情 &gt;&gt;</span>
								</div>
							</router-link>
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
				v-auto-animate
			>
				<div class="panel-block">
					<h3>主要聚居地</h3>
					<template v-if="detail.locations?.length">
						<div class="timeline">
							<div
								v-for="loc in detail.locations"
								:key="loc.id"
								class="tl-item"
								v-motion-fade-up
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
				v-auto-animate
			>
				<div class="panel-block">
					<h3>图集 / 影像</h3>
					<template v-if="galleryImages.length">
						<div class="gallery">
							<figure
								v-for="(g, i) in galleryImages"
								:key="i"
								class="g-item"
								v-motion-fade-up
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
			<!-- 参考资料：本页数据出处（方向 C-1 可溯源） -->
			<div
				v-show="activeTab === 'sources'"
				class="tab-pane"
				v-auto-animate
			>
				<div class="panel-block">
					<SourceReferences
						target-type="ethnic"
						:target-id="detail.id"
						:title="lang.pick('本页参考资料', 'References for this page')"
					/>
					<!-- 图片来源与许可（方向 C-4 署名） -->
					<ImageCredits
						target-type="ethnic"
						:target-id="detail.id"
					/>
				</div>
			</div>

			<!-- 讨论：与该民族相关的社区帖子（社区联动） -->
			<div
				v-show="activeTab === 'discussion'"
				class="tab-pane"
				v-auto-animate
			>
				<div class="panel-block">
					<!-- 复用通用「相关讨论」组件（与节日/艺术/美食页一致） -->
					<LinkedDiscussion
						linked-type="ethnic"
						:linked-id="detail.id"
						:label="detail.name"
					/>
				</div>
			</div>
		</template>
	</div>
</template>

<style scoped>
/* ==========================================================================
   关联信息（信息量增强）：相关人物 / 自治地方
   ========================================================================== */
/* 人口位次：跟在人口数字后的弱化提示 */
.rank-hint {
	margin-left: 6px;
	font-size: 12px;
	color: var(--ink-3, #6b6b6b);
}

.related-panel {
	margin-top: 16px;
}
.related-block + .related-block {
	margin-top: 14px;
	padding-top: 14px;
	border-top: 1px dashed var(--line, rgba(0, 0, 0, 0.12));
}
.related-title {
	display: flex;
	align-items: baseline;
	gap: 8px;
	margin-bottom: 8px;
	font-weight: 600;
	font-size: 14px;
}
.related-count {
	font-weight: 400;
	font-size: 12px;
	color: var(--ink-3, #6b6b6b);
}
.related-list {
	list-style: none;
	margin: 0;
	padding: 0;
}
.related-list li {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 8px;
	padding: 5px 0;
}
/* 级别/角色徽标：与主题色联动，保持民族频道的一致性 */
.rel-badge {
	flex: none;
	padding: 1px 7px;
	border-radius: 3px;
	font-size: 11px;
	color: #fff;
	opacity: 0.9;
	white-space: nowrap;
}
.rel-name {
	font-size: 13px;
	color: inherit;
	text-decoration: none;
	border-bottom: 1px solid transparent;
}
.rel-name:hover {
	border-bottom-color: currentColor;
}
.rel-meta {
	font-size: 12px;
	color: var(--ink-3, #6b6b6b);
}

/* ==========================================================================
   民族简介头部吸顶
   · .duo > .panel 默认 overflow: hidden，会让 sticky 相对该 overflow 容器定位
     （容器自身不滚动 → 吸顶失效），因此正文面板需放开裁剪；
   · 吸顶基准线：报头实际高度（--mast-h，随报头收缩变化）+ 吸顶 Tabs 实测高度（--tabs-h）；
   · padding-bottom 合并原 padding+margin，保证吸顶时背景不出现「透视缝」。
   ========================================================================== */
.intro-panel {
	overflow: visible;
}
.intro-head {
	position: sticky;
	top: calc(var(--mast-h) + var(--tabs-h, 46px));
	z-index: 21;
	background: var(--paper);
	padding-bottom: 26px;
	margin-bottom: 0;
	transition: box-shadow var(--dur-fast) ease;
}
.intro-head.is-stuck {
	box-shadow: 0 12px 16px -14px rgba(0, 0, 0, 0.45);
}
/* 基本资料里的标签：可点击跳转标签搜索页（hover 反馈 + 手型） */
.tag-link {
	cursor: pointer;
}
.tag-link :deep(.badge) {
	transition:
		background var(--dur-fast) ease,
		color var(--dur-fast) ease,
		border-color var(--dur-fast) ease;
}
.tag-link:hover :deep(.badge),
.tag-link:focus-visible :deep(.badge) {
	background: var(--ink);
	color: var(--paper);
	border-color: var(--ink);
}
.tag-link:focus-visible {
	outline: 2px solid var(--accent);
	outline-offset: 2px;
}
/* 风俗条目：左图右文，避免纯文本罗列 */
.custom-list {
	display: flex;
	flex-direction: column;
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.custom-item {
	display: grid;
	grid-template-columns: 240px 1fr;
	gap: 22px;
	align-items: start;
	padding: 20px;
	background: var(--paper);
	transition: background var(--dur-fast) ease;
}
.custom-item:hover {
	background: var(--paper-2);
}
.custom-thumb {
	aspect-ratio: 4 / 3;
	overflow: hidden;
	border: 1px solid var(--line);
}
.custom-thumb :deep(img) {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.custom-item h4 {
	font-size: 19px;
	margin: 4px 0 8px;
}
.custom-item p {
	color: var(--muted);
	font-size: 14.5px;
	line-height: 1.9;
}
.custom-more,
.more-link {
	display: inline-block;
	margin-top: 8px;
	font-size: 13px;
	color: var(--accent);
}
.custom-item:hover .custom-more,
.feature:hover .more-link {
	text-decoration: underline;
}
.panel-hint {
	font-size: 12px;
	letter-spacing: 0.06em;
	color: var(--muted);
}
.block-dek {
	font-size: 15px;
	color: var(--muted);
	margin: -6px 0 20px;
}
.history-wrap {
	max-width: 52em;
}
/* 关联讨论（社区联动） */
.discuss-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	flex-wrap: wrap;
	margin-bottom: 18px;
	padding-bottom: 14px;
	border-bottom: 1px solid var(--line);
}
.discuss-head h3 {
	border-bottom: none;
	padding: 0;
	margin: 0;
}
.discuss-btn {
	padding: 9px 20px;
	font-size: 12px;
}
.discuss-list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.discuss-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18px;
	padding: 16px 18px;
	border-bottom: 1px solid var(--line);
	transition: background var(--dur-fast) ease;
}
.discuss-row:hover {
	background: var(--paper-2);
}
.d-main h4 {
	font-size: 17px;
	margin: 0 0 6px;
}
.d-main p {
	font-size: 13.5px;
	color: var(--muted);
	line-height: 1.8;
	margin: 0 0 6px;
}
.d-meta {
	font-size: 12px;
	color: var(--muted);
}
.d-counts {
	flex: none;
	text-align: center;
	min-width: 52px;
}
.d-counts strong {
	display: block;
	font-family: var(--serif);
	font-size: 18px;
	color: var(--accent);
}
.d-counts span {
	font-size: 11px;
	color: var(--muted);
}
.discuss-more {
	text-align: right;
	margin-top: 10px;
}
/* 窄屏本身即单栏布局，收起功能无意义：隐藏按钮，避免状态歧义 */
@media (max-width: 900px) {
	.overview-toggle {
		display: none;
	}
}
@media (max-width: 760px) {
	.custom-item {
		grid-template-columns: 1fr;
	}
}
</style>
