<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import PageHead from "@/components/PageHead.vue";
import AppPagination from "@/components/AppPagination.vue";
import { myDiscussionApi, subscriptionApi } from "@/api/modules";
import type { DiscussionPost, DiscussionReportItem, DiscussionTopicBrief, SubscriptionItem } from "@/api/types";
import { contentExcerpt } from "@/utils/usercontent";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";

/**
 * 个人中心 · 我的社区：发帖 / 回复 / 举报三栏。
 * 待审与驳回状态在此可见（驳回会在帖子里附理由）。
 */
const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();

type Tab = "topics" | "posts" | "reports" | "subs";

const tab = ref<Tab>("topics");
const page = ref(0);
const size = 10;
const total = ref(0);
const loading = ref(false);
const loaded = ref(false);

const topics = ref<DiscussionTopicBrief[]>([]);
const posts = ref<DiscussionPost[]>([]);
const reports = ref<DiscussionReportItem[]>([]);
const subs = ref<SubscriptionItem[]>([]);

const SUB_LEVEL: Record<string, { label: string; type: "success" | "warning" | "info" }> = {
	all: { label: "全部通知", type: "success" },
	mention: { label: "仅被 @ 时", type: "warning" },
	off: { label: "免打扰", type: "info" },
};

const SUB_TYPE: Record<string, string> = { topic: "帖子", board: "板块", user: "用户" };

async function removeSub(item: SubscriptionItem) {
	try {
		await ElMessageBox.confirm(`确定取消订阅「${item.targetName}」？`, "取消订阅", { type: "warning" });
	} catch {
		return;
	}
	await subscriptionApi.remove(item.targetType, item.targetId);
	ElMessage.success("已取消订阅");
	await load(0);
}

const STATUS_LABEL: Record<string, string> = {
	published: "已发布",
	pending: "待审核",
	hidden: "已隐藏",
	rejected: "未通过",
	deleted: "已删除",
};

const REPORT_STATUS: Record<string, { label: string; type: string }> = {
	pending: { label: "处理中", type: "warning" },
	accepted: { label: "举报成立", type: "success" },
	rejected: { label: "举报未成立", type: "info" },
};

const REASON_LABEL: Record<string, string> = {
	spam: "广告或垃圾信息",
	abuse: "辱骂、攻击或歧视",
	porn: "色情低俗",
	political: "违法违规内容",
	copyright: "侵犯版权",
	other: "其他",
};

async function load(p = page.value) {
	page.value = p;
	loading.value = true;
	try {
		if (tab.value === "subs") {
			subs.value = await subscriptionApi.mine();
			total.value = subs.value.length;
			loaded.value = true;
			return;
		}
		const res =
			tab.value === "topics"
				? await myDiscussionApi.topics({ page: p, size })
				: tab.value === "posts"
					? await myDiscussionApi.posts({ page: p, size })
					: await myDiscussionApi.reports({ page: p, size });
		total.value = res.total;
		if (tab.value === "topics") topics.value = res.data as DiscussionTopicBrief[];
		else if (tab.value === "posts") posts.value = res.data as DiscussionPost[];
		else reports.value = res.data as unknown as DiscussionReportItem[];
		loaded.value = true;
	} finally {
		loading.value = false;
	}
}

function switchTab(next: Tab) {
	tab.value = next;
	loaded.value = false;
	load(0);
}

function openTopic(id: string) {
	router.push(`/discussion/topic/${id}`);
}

onMounted(() => {
	if (!auth.isLoggedIn) {
		router.replace({ path: "/discussion" });
		return;
	}
	load(0);
});
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/discussion">{{ lang.pick("讨论区", "Community") }}</router-link> ›
			<span>{{ lang.pick("我的社区", "My community") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('MY COMMUNITY · 我的社区', 'MY COMMUNITY')"
			:title="lang.pick('我的发帖与回复', 'My posts & replies')"
			:dek="
				lang.pick(
					'这里可以看到你的发帖、回复与举报处理进度；待审内容仅你本人与审核员可见。',
					'Track your posts, replies and reports. Pending items are visible to you and reviewers only.',
				)
			"
		/>

		<div class="tabs-bar">
			<button
				v-for="t in [
					{ key: 'topics', label: lang.pick('我的发帖', 'Posts') },
					{ key: 'posts', label: lang.pick('我的回复', 'Replies') },
					{ key: 'reports', label: lang.pick('我的举报', 'Reports') },
					{ key: 'subs', label: lang.pick('我的订阅', 'Subscriptions') },
				]"
				:key="t.key"
				class="chip"
				:class="{ active: tab === t.key }"
				@click="switchTab(t.key as 'topics' | 'posts' | 'reports' | 'subs')"
			>
				{{ t.label }}
			</button>
			<span style="flex: 1"></span>
			<router-link
				class="btn btn-accent new-btn"
				to="/discussion/new"
				>{{ lang.pick("发新帖", "New post") }}</router-link
			>
		</div>

		<el-skeleton
			v-if="loading && !loaded"
			:rows="6"
			animated
		/>

		<!-- 我的发帖 -->
		<template v-else-if="tab === 'topics'">
			<div
				v-if="topics.length"
				class="list"
			>
				<article
					v-for="t in topics"
					:key="t.id"
					class="row"
					@click="openTopic(t.id)"
				>
					<div class="main">
						<h4>
							<span
								v-if="t.status !== 'published'"
								class="tag"
								:class="t.status"
								>{{ STATUS_LABEL[t.status] || t.status }}</span
							>
							{{ t.title }}
						</h4>
						<p>{{ contentExcerpt(t.excerpt, 90) }}</p>
						<div class="meta">
							{{ t.boardName }} · {{ t.createdAt }}
							<span v-if="t.lastReplyAt"> · 最后回复 {{ t.lastReplyAt }}</span>
						</div>
					</div>
					<div class="counts">
						<strong>{{ t.replyCount || 0 }}</strong><span>{{ lang.pick("回复", "replies") }}</span>
					</div>
					<div class="counts">
						<strong>{{ t.likeCount || 0 }}</strong><span>{{ lang.pick("点赞", "likes") }}</span>
					</div>
				</article>
			</div>
			<el-empty
				v-else
				:description="lang.pick('你还没有发过帖子', 'You have no posts yet')"
			>
				<el-button
					type="primary"
					@click="router.push('/discussion/new')"
					>{{ lang.pick("写第一篇", "Write your first post") }}</el-button
				>
			</el-empty>
		</template>

		<!-- 我的回复 -->
		<template v-else-if="tab === 'posts'">
			<div
				v-if="posts.length"
				class="list"
			>
				<article
					v-for="p in posts"
					:key="p.id"
					class="row"
					@click="openTopic(p.topicId)"
				>
					<div class="floor-tag">#{{ p.floorNo }}</div>
					<div class="main">
						<p class="post-content">{{ contentExcerpt(p.content, 140) }}</p>
						<div class="meta">
							{{ p.createdAt }}
							<span
								v-if="p.status === 'pending'"
								class="tag pending"
								>{{ lang.pick("审核中", "pending") }}</span
							>
							<span
								v-else-if="p.status === 'hidden'"
								class="tag hidden"
								>{{ lang.pick("已隐藏", "hidden") }}</span
							>
						</div>
					</div>
					<div class="counts">
						<strong>{{ p.likeCount || 0 }}</strong><span>{{ lang.pick("点赞", "likes") }}</span>
					</div>
				</article>
			</div>
			<el-empty
				v-else
				:description="lang.pick('你还没有回复过', 'You have no replies yet')"
			/>
		</template>

		<!-- 我的举报 -->
		<template v-else-if="tab === 'reports'">
			<div
				v-if="reports.length"
				class="list"
			>
				<article
					v-for="r in reports"
					:key="r.id"
					class="row static"
				>
					<div class="main">
						<h4>
							<el-tag
								size="small"
								:type="(REPORT_STATUS[r.status]?.type as 'warning' | 'success' | 'info') || 'info'"
							>
								{{ REPORT_STATUS[r.status]?.label || r.status }}
							</el-tag>
							<span class="reason">{{ REASON_LABEL[r.reason] || r.reason }}</span>
						</h4>
						<p>{{ r.targetExcerpt || "—" }}</p>
						<div class="meta">
							{{ lang.pick("被举报作者", "Author") }}：{{ r.targetAuthor || "—" }} · {{ r.createdAt }}
							<template v-if="r.resultNote"> · {{ lang.pick("处理说明", "note") }}：{{ r.resultNote }}</template>
						</div>
					</div>
					<button
						v-if="r.targetPath"
						class="link-btn"
						@click="router.push(r.targetPath)"
					>
						{{ lang.pick("查看", "View") }}
					</button>
				</article>
			</div>
			<el-empty
				v-else
				:description="lang.pick('你还没有提交过举报', 'No reports submitted')"
			/>
		</template>

		<!-- 我的订阅 -->
		<template v-else>
			<div
				v-if="subs.length"
				class="list"
			>
				<article
					v-for="s in subs"
					:key="s.id"
					class="row"
					@click="router.push(s.path)"
				>
					<div class="sub-type">{{ SUB_TYPE[s.targetType] || s.targetType }}</div>
					<div class="main">
						<h4>
							<el-tag
								size="small"
								:type="SUB_LEVEL[s.level]?.type || 'info'"
							>
								{{ SUB_LEVEL[s.level]?.label || s.level }}
							</el-tag>
							<span class="sub-name">{{ s.targetName }}</span>
						</h4>
						<div class="meta">{{ lang.pick("订阅于", "since") }} {{ s.createdAt }}</div>
					</div>
					<button
						class="link-btn"
						@click.stop="removeSub(s)"
					>
						{{ lang.pick("取消订阅", "Unsubscribe") }}
					</button>
				</article>
			</div>
			<el-empty
				v-else
				:description="lang.pick('还没有订阅；在帖子或板块上点「订阅」即可接收通知', 'No subscriptions yet')"
			/>
		</template>

		<AppPagination
			:current="page"
			:total="total"
			:size="size"
			@change="load"
		/>
	</div>
</template>

<style scoped>
.tabs-bar {
	display: flex;
	align-items: center;
	gap: 8px;
	margin: 20px 0 14px;
}
.new-btn {
	padding: 8px 18px;
	font-size: 12px;
}
.list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.row {
	display: flex;
	align-items: center;
	gap: 18px;
	padding: 16px 18px;
	border-bottom: 1px solid var(--line);
	background: var(--paper);
	cursor: pointer;
	transition: background var(--dur-fast) ease;
}
.row.static {
	cursor: default;
}
.row:not(.static):hover {
	background: var(--paper-2);
}
.main {
	flex: 1;
	min-width: 0;
}
.main h4 {
	font-size: 17px;
	margin: 0 0 6px;
	overflow-wrap: anywhere;
}
.main p {
	font-size: 13.5px;
	color: var(--muted);
	line-height: 1.8;
	margin: 0 0 6px;
}
.post-content {
	font-size: 14.5px !important;
	color: var(--ink-soft) !important;
}
.meta {
	font-size: 12px;
	color: var(--muted);
}
.tag {
	font-size: 11px;
	border: 1px solid var(--muted);
	color: var(--muted);
	padding: 1px 7px;
	margin-right: 6px;
}
.tag.pending {
	border-color: #b07d2b;
	color: #b07d2b;
}
.tag.rejected,
.tag.hidden {
	border-color: var(--accent);
	color: var(--accent);
}
.reason {
	margin-left: 8px;
	font-size: 13px;
	color: var(--muted);
}
.counts {
	flex: none;
	text-align: center;
	min-width: 46px;
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
.floor-tag {
	flex: none;
	width: 44px;
	text-align: center;
	font-family: var(--serif);
	font-size: 15px;
	color: var(--accent);
}
.link-btn {
	flex: none;
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 6px 14px;
	font-size: 12px;
	cursor: pointer;
}
.link-btn:hover {
	border-color: var(--accent);
	color: var(--accent);
}
</style>
