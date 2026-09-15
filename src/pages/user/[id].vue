<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import UserAvatar from "@/components/UserAvatar.vue";
import AppPagination from "@/components/AppPagination.vue";
import { messageApi, socialApi, discussionApi } from "@/api/modules";
import type { CommunityUser, DiscussionTopicBrief } from "@/api/types";
import { contentExcerpt } from "@/utils/usercontent";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";

/**
 * 社区用户主页：资料 + 关注/粉丝 + TA 的帖子 + 私信入口。
 * 私信入口仅在互相关注时可用（后端同样校验）。
 */
const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();

const userId = () => route.params.id as string;

const profile = ref<CommunityUser | null>(null);
const topics = ref<DiscussionTopicBrief[]>([]);
const total = ref(0);
const page = ref(0);
const size = 8;
const loading = ref(true);
const followBusy = ref(false);
const followTab = ref<"following" | "followers">("following");
const followList = ref<CommunityUser[]>([]);
const followTotal = ref(0);
const followPage = ref(0);

async function loadProfile() {
	loading.value = true;
	try {
		profile.value = await socialApi.profile(userId());
		await Promise.all([loadTopics(0), loadFollows("following", 0)]);
	} catch {
		profile.value = null;
	} finally {
		loading.value = false;
	}
}

async function loadTopics(p: number) {
	page.value = p;
	const res = await discussionApi.topics({ authorId: userId(), page: p, size, sort: "latest" });
	topics.value = res.data;
	total.value = res.total;
}

async function loadFollows(type: "following" | "followers", p: number) {
	followTab.value = type;
	followPage.value = p;
	const res = await socialApi.follows(userId(), type, { page: p, size: 12 });
	followList.value = res.data;
	followTotal.value = res.total;
}

function requireLogin(): boolean {
	if (auth.isLoggedIn) return true;
	ElMessage.warning(lang.pick("请先登录", "Please sign in first"));
	window.dispatchEvent(new CustomEvent("auth:required"));
	return false;
}

async function toggleFollow(target?: string) {
	const id = target || userId();
	if (!requireLogin()) return;
	followBusy.value = true;
	try {
		const isSelf = profile.value?.self && !target;
		const followed = isSelf ? false : profile.value?.followed;
		if (target) {
			// 关注列表内直接关注/取关
			await socialApi.follow(target);
			ElMessage.success(lang.pick("已关注", "Followed"));
			await loadFollows(followTab.value, followPage.value);
		} else if (followed) {
			await socialApi.unfollow(id);
			ElMessage.success(lang.pick("已取消关注", "Unfollowed"));
			await loadProfile();
		} else {
			await socialApi.follow(id);
			ElMessage.success(lang.pick("已关注", "Followed"));
			await loadProfile();
		}
	} catch {
		/* 拦截器已提示 */
	} finally {
		followBusy.value = false;
	}
}

async function openChat() {
	if (!requireLogin() || !profile.value) return;
	if (!profile.value.mutual) {
		ElMessage.warning(
			lang.pick("仅互相关注的好友之间可以私信，先互相关注吧", "Direct messages need a mutual follow"),
		);
		return;
	}
	try {
		const conversation = await messageApi.openWith(profile.value.id);
		router.push(`/messages/${conversation.id}`);
	} catch {
		/* 拦截器已提示 */
	}
}

watch(
	() => route.params.id,
	() => {
		profile.value = null;
		loadProfile();
	},
);

onMounted(loadProfile);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/discussion">{{ lang.pick("讨论区", "Community") }}</router-link> ›
			<span>{{ profile?.nickname || lang.t("loading") }}</span>
		</div>

		<el-skeleton
			v-if="loading"
			:rows="6"
			animated
		/>
		<el-empty
			v-else-if="!profile"
			:description="lang.pick('用户不存在', 'User not found')"
		/>

		<template v-else>
			<!-- 资料卡 -->
			<div
				class="card"
				v-motion-fade-up
			>
				<UserAvatar
					:name="profile.nickname"
					:src="profile.avatar"
					:size="72"
				/>
				<div class="info">
					<div class="name-line">
						<h1>{{ profile.nickname }}</h1>
						<span
							v-if="profile.trustLevel >= 2"
							class="veteran"
							>{{ lang.pick("活跃用户", "Active") }}</span
						>
						<span
							v-if="profile.mutual"
							class="mutual"
							>{{ lang.pick("互相关注", "Mutual") }}</span
						>
					</div>
					<p class="bio">
						{{ profile.bio || lang.pick("这位朋友还没有填写简介", "No bio yet") }}
					</p>
					<div class="stats">
						<div class="s">
							<strong>{{ profile.topicCount }}</strong
							><span>{{ lang.pick("发帖", "posts") }}</span>
						</div>
						<div class="s">
							<strong>{{ profile.followingCount }}</strong
							><span>{{ lang.pick("关注", "following") }}</span>
						</div>
						<div class="s">
							<strong>{{ profile.followerCount }}</strong
							><span>{{ lang.pick("粉丝", "followers") }}</span>
						</div>
						<div
							v-if="profile.joinedAt"
							class="s wide"
						>
							<span>{{ lang.pick("加入于", "Joined") }} {{ profile.joinedAt }}</span>
						</div>
					</div>
				</div>
				<div class="actions">
					<template v-if="!profile.self">
						<button
							class="btn"
							:class="{ 'btn-solid': !profile.followed }"
							:disabled="followBusy"
							@click="toggleFollow()"
						>
							{{ profile.followed ? lang.pick("已关注", "Following") : lang.pick("关注", "Follow") }}
						</button>
						<button
							class="btn talk"
							:title="profile.mutual ? '' : lang.pick('仅互相关注可私信', 'Mutual follow required')"
							@click="openChat"
						>
							{{ lang.pick("私信", "Message") }}
						</button>
					</template>
					<router-link
						v-else
						class="btn"
						to="/me/community"
						>{{ lang.pick("我的社区", "My community") }}</router-link
					>
				</div>
			</div>

			<div class="cols">
				<!-- TA 的帖子 -->
				<section class="col-main">
					<h3 class="col-title">{{ lang.pick("TA 的帖子", "Posts") }}</h3>
					<div
						v-if="topics.length"
						class="topic-list"
					>
						<router-link
							v-for="t in topics"
							:key="t.id"
							class="topic-row"
							:to="`/discussion/topic/${t.id}`"
						>
							<div class="main">
								<h4>{{ t.title }}</h4>
								<p>{{ contentExcerpt(t.excerpt, 86) }}</p>
								<div class="meta">{{ t.createdAt }} · {{ t.replyCount || 0 }} 回复</div>
							</div>
						</router-link>
					</div>
					<el-empty
						v-else
						:description="lang.pick('TA 还没有发过帖子', 'No posts yet')"
					/>
					<AppPagination
						:current="page"
						:total="total"
						:size="size"
						@change="loadTopics"
					/>
				</section>

				<!-- 关注 / 粉丝 -->
				<aside class="col-side">
					<div class="side-head">
						<button
							class="chip"
							:class="{ active: followTab === 'following' }"
							@click="loadFollows('following', 0)"
						>
							{{ lang.pick("关注", "Following") }} {{ profile.followingCount }}
						</button>
						<button
							class="chip"
							:class="{ active: followTab === 'followers' }"
							@click="loadFollows('followers', 0)"
						>
							{{ lang.pick("粉丝", "Followers") }} {{ profile.followerCount }}
						</button>
					</div>
					<div
						v-if="followList.length"
						class="user-list"
					>
						<div
							v-for="u in followList"
							:key="u.id"
							class="user-row"
						>
							<router-link
								class="user-info"
								:to="`/user/${u.id}`"
							>
								<UserAvatar
									:name="u.nickname"
									:src="u.avatar"
									:size="30"
								/>
								<span class="uname">{{ u.nickname }}</span>
							</router-link>
							<span
								v-if="u.bio"
								class="ubio"
								>{{ contentExcerpt(u.bio, 24) }}</span
							>
						</div>
					</div>
					<el-empty
						v-else
						:description="lang.pick('暂无数据', 'No data')"
						:image-size="60"
					/>
					<AppPagination
						:current="followPage"
						:total="followTotal"
						:size="12"
						@change="(p: number) => loadFollows(followTab, p)"
					/>
				</aside>
			</div>
		</template>
	</div>
</template>

<style scoped>
.card {
	display: flex;
	align-items: flex-start;
	gap: 22px;
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 26px 28px;
	margin: 10px 0 26px;
	flex-wrap: wrap;
}
.info {
	flex: 1;
	min-width: 240px;
}
.name-line {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-wrap: wrap;
}
.name-line h1 {
	font-size: 26px;
	margin: 0;
}
.veteran,
.mutual {
	font-size: 11px;
	border: 1px solid var(--accent);
	color: var(--accent);
	padding: 2px 8px;
}
.mutual {
	border-color: #2e6b4f;
	color: #2e6b4f;
}
.bio {
	font-size: 14px;
	color: var(--muted);
	margin: 10px 0 14px;
	max-width: 60ch;
}
.stats {
	display: flex;
	gap: 26px;
	flex-wrap: wrap;
}
.stats .s strong {
	display: block;
	font-family: var(--serif);
	font-size: 19px;
	color: var(--ink);
}
.stats .s span {
	font-size: 12px;
	color: var(--muted);
}
.stats .wide {
	align-self: flex-end;
}
.actions {
	display: flex;
	flex-direction: column;
	gap: 10px;
	flex: none;
}
.actions .btn {
	padding: 9px 22px;
	font-size: 12px;
	text-align: center;
}
.cols {
	display: grid;
	grid-template-columns: 1.5fr 1fr;
	gap: 26px;
}
.col-title {
	font-family: var(--serif);
	font-size: 20px;
	margin-bottom: 14px;
	padding-bottom: 10px;
	border-bottom: 1px solid var(--line);
}
.topic-list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.topic-row {
	display: block;
	padding: 16px 18px;
	border-bottom: 1px solid var(--line);
	transition: background var(--dur-fast) ease;
}
.topic-row:hover {
	background: var(--paper-2);
}
.topic-row h4 {
	font-size: 17px;
	margin: 0 0 6px;
}
.topic-row p {
	font-size: 13.5px;
	color: var(--muted);
	line-height: 1.8;
	margin: 0 0 6px;
}
.topic-row .meta {
	font-size: 12px;
	color: var(--muted);
}
.side-head {
	display: flex;
	gap: 8px;
	margin-bottom: 12px;
}
.user-list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.user-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
	padding: 10px 14px;
	border-bottom: 1px solid var(--line);
}
.user-info {
	display: flex;
	align-items: center;
	gap: 10px;
	min-width: 0;
}
.user-info .uname {
	font-size: 14px;
}
.user-info:hover .uname {
	color: var(--accent);
}
.ubio {
	font-size: 12px;
	color: var(--muted);
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	max-width: 40%;
}
@media (max-width: 900px) {
	.cols {
		grid-template-columns: 1fr;
	}
}
</style>
