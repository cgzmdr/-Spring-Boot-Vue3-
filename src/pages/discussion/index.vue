<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import PageHead from "@/components/PageHead.vue";
import AppPagination from "@/components/AppPagination.vue";
import SubscribeButton from "@/components/SubscribeButton.vue";
import TranslateButton from "@/components/TranslateButton.vue";
import { discussionApi, socialApi } from "@/api/modules";
import type { DiscussionBoard, DiscussionTopicBrief } from "@/api/types";
import { resolveStaticUrl } from "@/utils/format";
import { contentExcerpt } from "@/utils/usercontent";
import { translateProviderLabel } from "@/utils/translate";
import { fadeUp } from "@/utils/motion";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";
import { useNotificationStore } from "@/stores/notification";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();
const notice = useNotificationStore();

/** 列表页只翻标题（成本可控）；键为帖子 ID，null/undefined 表示展示原文 */
const titleTrans = ref<Record<string, string>>({});
const titleProvider = ref<Record<string, string>>({});

const boards = ref<DiscussionBoard[]>([]);
const topics = ref<DiscussionTopicBrief[]>([]);
const total = ref(0);
const page = ref(0);
const size = 15;
const loading = ref(true);
const errored = ref(false);

const boardId = ref((route.query.board as string) || "");
const sort = ref<"latest" | "hot" | "featured">("latest");
const keyword = ref((route.query.keyword as string) || "");
/** 关注流：只看已关注用户的帖子 */
const followingOnly = ref(false);

const SORTS = computed(() => [
	{ key: "latest", label: lang.pick("最新", "Latest") },
	{ key: "hot", label: lang.pick("最热", "Hot") },
	{ key: "featured", label: lang.pick("精华", "Featured") },
]);

async function loadBoards() {
	try {
		boards.value = await discussionApi.boards();
	} catch {
		boards.value = [];
	}
}

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		const res = followingOnly.value
			? await socialApi.followingFeed({ page: page.value, size })
			: await discussionApi.topics({
					boardId: boardId.value || undefined,
					keyword: keyword.value || undefined,
					sort: sort.value,
					page: page.value,
					size,
				});
		topics.value = res.data;
		total.value = res.total;
	} catch {
		errored.value = true;
		topics.value = [];
		total.value = 0;
	} finally {
		loading.value = false;
	}
}

/** 切换关注流（需登录） */
function toggleFollowing() {
	if (!followingOnly.value && !auth.isLoggedIn) {
		ElMessage.warning(lang.pick("请先登录后查看关注流", "Sign in to see your feed"));
		window.dispatchEvent(new CustomEvent("auth:required"));
		return;
	}
	followingOnly.value = !followingOnly.value;
	page.value = 0;
	load();
}

function pickBoard(id: string) {
	boardId.value = boardId.value === id ? "" : id;
	page.value = 0;
	syncQuery();
	load();
}

function pickSort(key: "latest" | "hot" | "featured") {
	sort.value = key;
	page.value = 0;
	load();
}

function search() {
	page.value = 0;
	syncQuery();
	load();
}

function syncQuery() {
	router.replace({
		path: "/discussion",
		query: {
			...(boardId.value ? { board: boardId.value } : {}),
			...(keyword.value ? { keyword: keyword.value } : {}),
		},
	});
}

function onPage(p: number) {
	page.value = p;
	load();
	window.scrollTo({ top: 0, behavior: "smooth" });
}

function goCompose() {
	if (!auth.isLoggedIn) {
		ElMessage.warning(lang.pick("请先登录后再发帖", "Please sign in to post"));
		window.dispatchEvent(new CustomEvent("auth:required"));
		return;
	}
	router.push({ path: "/discussion/new", query: boardId.value ? { board: boardId.value } : {} });
}

function goTopic(id: string) {
	router.push(`/discussion/topic/${id}`);
}

const activeBoardName = computed(
	() => boards.value.find((b) => b.id === boardId.value)?.name || "",
);

watch(
	() => route.query.board,
	(v) => {
		const next = (v as string) || "";
		if (next !== boardId.value) {
			boardId.value = next;
			page.value = 0;
			load();
		}
	},
);

onMounted(async () => {
	await Promise.all([loadBoards(), load()]);
	notice.refresh();
});
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> › <span>{{ lang.pick("讨论区", "Community") }}</span>
			<template v-if="activeBoardName"> › <span>{{ activeBoardName }}</span></template>
		</div>

		<PageHead
			:kicker="lang.pick('COMMUNITY · 讨论区', 'COMMUNITY')"
			:title="lang.pick('与世界各地的朋友聊聊民族文化', 'Talk culture with the world')"
			:dek="
				lang.pick(
					'分享见闻、提问求证、结识同好。发言前请阅读社区规范：尊重差异、注明来源、拒绝广告与攻击。',
					'Share stories, ask questions, meet people. Be respectful and avoid ads or attacks.',
				)
			"
		/>

		<!-- 工具栏：板块 / 排序 / 搜索 / 发帖 -->
		<div class="board-bar">
			<div class="board-tabs">
				<button
					class="chip"
					:class="{ active: !boardId }"
					@click="pickBoard(boardId || '__all__')"
				>
					{{ lang.pick("全部", "All") }}
				</button>
				<button
					v-for="b in boards"
					:key="b.id"
					class="chip"
					:class="{ active: boardId === b.id }"
					@click="pickBoard(b.id)"
				>
					{{ lang.pick(b.name, b.nameEn) }}
					<span
						v-if="b.topicCount"
						class="cnt"
						>{{ b.topicCount }}</span
					>
				</button>
			</div>

			<div class="tools">
				<button
					v-if="auth.isLoggedIn"
					class="chip"
					:class="{ active: followingOnly }"
					@click="toggleFollowing"
				>
					{{ lang.pick("关注", "Following") }}
				</button>
				<template v-if="!followingOnly">
					<button
						v-for="s in SORTS"
						:key="s.key"
						class="chip"
						:class="{ active: sort === s.key }"
						@click="pickSort(s.key as 'latest' | 'hot' | 'featured')"
					>
						{{ s.label }}
					</button>
				</template>
				<el-input
					v-model="keyword"
					class="kw"
					size="small"
					:placeholder="lang.pick('搜索标题…', 'Search titles…')"
					clearable
					@keyup.enter="search"
					@clear="search"
				/>
				<button
					class="btn btn-accent post-btn"
					@click="goCompose"
				>
					{{ lang.pick("发帖", "New post") }}
				</button>
				<router-link
					v-if="auth.isLoggedIn"
					class="chip"
					to="/me/community"
				>
					{{ lang.pick("我的社区", "My community") }}
				</router-link>
				<SubscribeButton
					v-if="boardId"
					class="board-sub"
					target-type="board"
					:target-id="boardId"
					compact
				/>
			</div>
		</div>

		<div class="result-count">
			{{ lang.t("result_count", { n: total }) }}
			<template v-if="notice.unread"> · {{ lang.pick("未读通知", "Unread") }} {{ notice.unread }}</template>
		</div>

		<el-skeleton
			v-if="loading"
			:rows="6"
			animated
		/>
		<el-empty
			v-else-if="errored"
			:description="lang.pick('讨论区加载失败', 'Failed to load')"
		>
			<el-button
				type="primary"
				@click="load"
				>重试</el-button
			>
		</el-empty>

		<template v-else-if="topics.length">
			<div class="topic-list">
				<article
					v-for="(t, i) in topics"
					:key="t.id"
					class="topic-row"
					v-motion="fadeUp({ delay: Math.min(i * 40, 320), distance: 14 })"
					@click="goTopic(t.id)"
				>
					<div
						v-if="t.images?.length"
						class="thumb"
					>
						<img
							:src="resolveStaticUrl(t.images[0])"
							alt=""
							loading="lazy"
						/>
					</div>
					<div class="main">
						<div class="title-line">
							<span
								v-if="t.pinned"
								class="tag pinned"
								>{{ lang.pick("置顶", "Pinned") }}</span
							>
							<span
								v-if="t.featured"
								class="tag featured"
								>{{ lang.pick("精华", "Featured") }}</span
							>
							<span
								v-if="t.locked"
								class="tag locked"
								>{{ lang.pick("已锁定", "Locked") }}</span
							>
							<span
								v-if="t.status === 'pending'"
								class="tag pending"
								>{{ lang.pick("待审核", "In review") }}</span
							>
							<h3 :class="{ 'mt-text': !!titleTrans[t.id] }">
								{{ titleTrans[t.id] ?? t.title }}
							</h3>
							<TranslateButton
								v-model="titleTrans[t.id]"
								v-model:provider="titleProvider[t.id]"
								target-type="topic"
								:target-id="t.id"
								scope="title"
								:source-locale="t.lang"
								compact
							/>
							<span
								v-if="titleTrans[t.id]"
								class="mt-badge"
								>{{ translateProviderLabel(titleProvider[t.id]) }}</span
							>
						</div>
						<p class="excerpt">{{ contentExcerpt(t.excerpt, 110) }}</p>
						<div class="meta">
							<span class="author">{{ t.author?.nickname }}</span>
							<span class="sep">·</span>
							<span>{{ t.boardName }}</span>
							<span class="sep">·</span>
							<span>{{ t.createdAt }}</span>
							<span
								v-if="t.lang && t.lang !== 'zh'"
								class="lang"
								>{{ t.lang.toUpperCase() }}</span
							>
						</div>
					</div>
					<div class="counts">
						<div class="c">
							<strong>{{ t.replyCount || 0 }}</strong
							><span>{{ lang.pick("回复", "replies") }}</span>
						</div>
						<div class="c">
							<strong>{{ t.likeCount || 0 }}</strong
							><span>{{ lang.pick("点赞", "likes") }}</span>
						</div>
						<div class="c">
							<strong>{{ t.viewCount || 0 }}</strong
							><span>{{ lang.pick("浏览", "views") }}</span>
						</div>
					</div>
				</article>
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
			:description="
				followingOnly
					? lang.pick('关注的人还没有发帖，去社区发现更多朋友吧', 'No posts from people you follow yet')
					: keyword
						? lang.pick('没有找到相关讨论', 'No matching discussions')
						: lang.pick('这个板块还没有讨论，来发第一帖吧', 'No discussions yet — start one!')
			"
			style="padding: 40px 0"
		>
			<el-button
				type="primary"
				@click="goCompose"
				>{{ lang.pick("我要发帖", "Create a post") }}</el-button
			>
		</el-empty>

		<!-- 社区规范提示 -->
		<div class="rules">
			<h4>{{ lang.pick("社区规范", "Community rules") }}</h4>
			<ul>
				<li>{{ lang.pick("尊重各民族风俗与信仰差异，不发布歧视、攻击性内容。", "Respect all cultures; no discrimination or attacks.") }}</li>
				<li>{{ lang.pick("禁止广告、引流与重复刷屏，违者内容将被隐藏并可能被禁言。", "No ads or spam; violations may be hidden or muted.") }}</li>
				<li>{{ lang.pick("引用他人资料请注明来源；发现违规内容可点击「举报」。", "Cite sources; report violations.") }}</li>
			</ul>
		</div>
	</div>
</template>

<script lang="ts">
export default { name: "DiscussionList" };
</script>

<style scoped>
.board-bar {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	justify-content: space-between;
	gap: 14px;
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 14px 18px;
	margin: 20px 0 8px;
}
.board-tabs,
.tools {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 8px;
}
.chip .cnt {
	margin-left: 6px;
	font-size: 11px;
	opacity: 0.7;
}
.tools .kw {
	width: 180px;
}
.post-btn {
	padding: 8px 18px;
	font-size: 12px;
}
.topic-list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.topic-row {
	display: flex;
	gap: 18px;
	padding: 18px 20px;
	border-bottom: 1px solid var(--line);
	background: var(--paper);
	cursor: pointer;
	transition: background var(--dur-fast) ease;
}
.topic-row:hover {
	background: var(--paper-2);
}
.thumb {
	width: 132px;
	height: 92px;
	flex: none;
	overflow: hidden;
	border: 1px solid var(--line);
}
.thumb img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.main {
	flex: 1;
	min-width: 0;
}
.title-line {
	display: flex;
	align-items: center;
	gap: 8px;
	flex-wrap: wrap;
}
.title-line h3 {
	font-size: 18px;
	font-family: var(--serif);
	font-weight: 700;
	margin: 0;
	overflow-wrap: anywhere;
}
.tag {
	font-size: 11px;
	padding: 2px 8px;
	border: 1px solid var(--accent);
	color: var(--accent);
	letter-spacing: 0.06em;
}
.tag.featured {
	border-color: #b07d2b;
	color: #b07d2b;
}
.tag.pending {
	border-color: var(--muted);
	color: var(--muted);
}
.tag.locked {
	border-color: var(--ink);
	color: var(--ink);
}
.excerpt {
	font-size: 14px;
	color: var(--muted);
	margin: 8px 0 6px;
	overflow-wrap: anywhere;
}
.meta {
	font-size: 12px;
	color: var(--muted);
	display: flex;
	align-items: center;
	gap: 6px;
	flex-wrap: wrap;
}
.meta .author {
	color: var(--ink);
}
.meta .sep {
	opacity: 0.5;
}
.meta .lang {
	border: 1px solid var(--line);
	padding: 0 5px;
	font-size: 10px;
}
.counts {
	display: flex;
	gap: 18px;
	align-items: center;
	flex: none;
}
.counts .c {
	text-align: center;
	min-width: 44px;
}
.counts strong {
	display: block;
	font-family: var(--serif);
	font-size: 17px;
	color: var(--ink);
}
.counts span {
	font-size: 11px;
	color: var(--muted);
}
.rules {
	margin: 34px 0 10px;
	border: 1px dashed var(--line);
	padding: 16px 20px;
}
.rules h4 {
	font-size: 15px;
	margin-bottom: 8px;
}
.rules ul {
	margin: 0;
	padding-left: 18px;
}
.rules li {
	font-size: 13px;
	color: var(--muted);
	line-height: 1.9;
}
@media (max-width: 760px) {
	.topic-row {
		flex-wrap: wrap;
	}
	.thumb {
		width: 96px;
		height: 72px;
	}
	.counts {
		width: 100%;
		justify-content: flex-start;
	}
}
</style>
