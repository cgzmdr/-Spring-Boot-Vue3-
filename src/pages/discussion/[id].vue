<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import InteractionBar from "@/components/InteractionBar.vue";
import UserContent from "@/components/UserContent.vue";
import AppPagination from "@/components/AppPagination.vue";
import UserAvatar from "@/components/UserAvatar.vue";
import SubscribeButton from "@/components/SubscribeButton.vue";
import TranslateButton from "@/components/TranslateButton.vue";
import MentionTextarea from "@/components/MentionTextarea.vue";
import ComposerTips from "@/components/ComposerTips.vue";
import { discussionApi, interactionApi } from "@/api/modules";
import type { DiscussionPost, DiscussionTopicDetail } from "@/api/types";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";
import { useNotificationStore } from "@/stores/notification";
import { translateProviderLabel } from "@/utils/translate";
import { fadeUp } from "@/utils/motion";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();
const notice = useNotificationStore();

const topic = ref<DiscussionTopicDetail | null>(null);
const posts = ref<DiscussionPost[]>([]);
const total = ref(0);
const page = ref(0);
const size = 15;
const loading = ref(true);
const error = ref(false);
const posting = ref(false);

const reply = ref("");
const replyImages = ref<string[]>([]);
const quote = ref<DiscussionPost | null>(null);
const uploading = ref(false);
const likedPosts = ref<Record<string, boolean>>({});

/** @提及联想：面板开关（用于提示条高亮）与最近一次插入的提及（轻反馈） */
const mentioning = ref(false);
const lastMention = ref<string | null>(null);
let mentionHintTimer: number | undefined;

/**
 * @提及本地候选：楼主 + 本页已参与楼层的作者。
 * 输入「@」即可立刻提示这些「同一话题里的人」，不必等服务端联想返回；
 * 服务端按昵称联想的结果会追加在后面（见 MentionTextarea）。
 */
const mentionLocals = computed(() => {
	const out: { id: string | null; nickname: string; avatar: string | null; hint?: string }[] = [];
	const seen = new Set<string>();
	const mineId = auth.user?.id;
	const push = (
		author: { id: string | null; nickname: string; avatar?: string | null } | null | undefined,
		hint?: string,
	) => {
		const nickname = author?.nickname?.trim();
		if (!nickname || seen.has(nickname) || (author?.id && author.id === mineId)) return;
		seen.add(nickname);
		out.push({ id: author?.id ?? null, nickname, avatar: author?.avatar ?? null, hint });
	};
	push(topic.value?.author, lang.pick("楼主", "OP"));
	posts.value.forEach((p) => push(p.author, `#${p.floorNo}`));
	return out;
});

/** 插入提及后的轻提示：说明「对方会收到通知」，避免用户以为只是普通文本 */
function onMentionPick(user: { nickname: string }) {
	lastMention.value = user.nickname;
	window.clearTimeout(mentionHintTimer);
	mentionHintTimer = window.setTimeout(() => (lastMention.value = null), 5000);
}

/* 机器翻译：标题 / 正文 / 各楼层各自独立，null 表示展示原文 */
const titleTrans = ref<string | null>(null);
const titleProvider = ref<string | null>(null);
const bodyTrans = ref<string | null>(null);
const bodyProvider = ref<string | null>(null);
const floorTrans = ref<Record<string, string>>({});
const floorProvider = ref<Record<string, string>>({});

const reportVisible = ref(false);
const reportTarget = ref<{ type: string; id: string; label: string } | null>(null);
const reportForm = ref({ reason: "spam", detail: "" });
const REPORT_REASONS = computed(() => [
	{ value: "spam", label: lang.pick("广告或垃圾信息", "Spam or ads") },
	{ value: "abuse", label: lang.pick("辱骂、攻击或歧视", "Abuse or hate") },
	{ value: "porn", label: lang.pick("色情低俗", "Adult content") },
	{ value: "political", label: lang.pick("违法违规内容", "Illegal content") },
	{ value: "copyright", label: lang.pick("侵犯版权", "Copyright") },
	{ value: "other", label: lang.pick("其他", "Other") },
]);

const topicId = computed(() => route.params.id as string);
const canReply = computed(
	() => !!topic.value && !topic.value.locked && topic.value.status === "published",
);

async function load() {
	loading.value = true;
	error.value = false;
	try {
		topic.value = await discussionApi.topic(topicId.value);
		await loadPosts();
		// 记录浏览量（复用现有互动体系）
		interactionApi.view("discussion_topic", topicId.value).catch(() => {});
	} catch {
		error.value = true;
	} finally {
		loading.value = false;
	}
}

async function loadPosts() {
	const res = await discussionApi.posts(topicId.value, { page: page.value, size });
	posts.value = res.data;
	total.value = res.total;
}

function onPage(p: number) {
	page.value = p;
	loadPosts();
}

function requireLogin(): boolean {
	if (auth.isLoggedIn) return true;
	ElMessage.warning(lang.pick("请先登录", "Please sign in first"));
	window.dispatchEvent(new CustomEvent("auth:required"));
	return false;
}

function startQuote(post: DiscussionPost) {
	if (!requireLogin()) return;
	if (!canReply.value) {
		ElMessage.warning(lang.pick("该帖子已锁定，无法回复", "This topic is locked"));
		return;
	}
	quote.value = post;
	ElMessage.success(
		lang.pick(
			`已引用 ${post.author?.nickname || "该用户"} 的 #${post.floorNo} 楼，输入内容后发布`,
			`Quoting ${post.author?.nickname || "this user"} #${post.floorNo} — write your reply and post`,
		),
	);
	document.getElementById("reply-box")?.scrollIntoView({ behavior: "smooth", block: "center" });
}

async function pickImages(event: Event) {
	const input = event.target as HTMLInputElement;
	const files = Array.from(input.files || []);
	input.value = "";
	if (!files.length) return;
	if (!requireLogin()) return;
	uploading.value = true;
	try {
		const { compressImage } = await import("@/utils/usercontent");
		for (const file of files.slice(0, 9 - replyImages.value.length)) {
			const compressed = await compressImage(file);
			const url = await discussionApi.uploadImage(compressed, compressed.name || "image.jpg");
			replyImages.value.push(url);
		}
	} catch {
		ElMessage.error(lang.pick("图片上传失败", "Upload failed"));
	} finally {
		uploading.value = false;
	}
}

async function submitReply() {
	if (!requireLogin() || !topic.value) return;
	if (!canReply.value) {
		ElMessage.warning(lang.pick("该帖子已锁定，无法回复", "This topic is locked"));
		return;
	}
	if (!reply.value.trim()) {
		ElMessage.warning(lang.pick("回复内容不能为空", "Reply cannot be empty"));
		return;
	}
	posting.value = true;
	try {
		const created = await discussionApi.createPost(topicId.value, {
			content: reply.value.trim(),
			images: replyImages.value,
			lang: lang.isEn ? "en" : "zh",
			quotePostId: quote.value?.id || null,
		});
		ElMessage.success(
			created.status === "pending"
				? lang.pick("回复含待复核内容，审核通过后展示", "Reply submitted for review")
				: lang.pick("回复成功", "Replied"),
		);
		reply.value = "";
		replyImages.value = [];
		quote.value = null;
		await Promise.all([loadPosts(), refreshTopicCounters()]);
	} catch {
		/* 拦截器已提示 */
	} finally {
		posting.value = false;
	}
}

async function refreshTopicCounters() {
	try {
		const fresh = await discussionApi.topic(topicId.value);
		if (topic.value) {
			topic.value.replyCount = fresh.replyCount;
			topic.value.likeCount = fresh.likeCount;
			topic.value.favoriteCount = fresh.favoriteCount;
			topic.value.locked = fresh.locked;
			topic.value.status = fresh.status;
		}
	} catch {
		/* 忽略 */
	}
}

async function togglePostLike(post: DiscussionPost) {
	if (!requireLogin()) return;
	const liked = likedPosts.value[post.id];
	try {
		if (liked) {
			const res = await interactionApi.unlike("discussion_post", post.id);
			likedPosts.value[post.id] = res.liked;
			post.likeCount = res.likeCount;
		} else {
			const res = await interactionApi.like("discussion_post", post.id);
			likedPosts.value[post.id] = res.liked;
			post.likeCount = res.likeCount;
		}
	} catch {
		/* 拦截器已提示 */
	}
}

async function removePost(post: DiscussionPost) {
	if (!requireLogin()) return;
	try {
		await ElMessageBox.confirm(
			lang.pick("确定删除这条回复？", "Delete this reply?"),
			lang.pick("删除确认", "Confirm"),
			{ type: "warning" },
		);
	} catch {
		return;
	}
	await discussionApi.deletePost(post.id);
	ElMessage.success(lang.pick("已删除", "Deleted"));
	await Promise.all([loadPosts(), refreshTopicCounters()]);
}

async function removeTopic() {
	if (!topic.value || !requireLogin()) return;
	try {
		await ElMessageBox.confirm(
			lang.pick("确定删除这篇帖子？删除后不可恢复。", "Delete this topic? This cannot be undone."),
			lang.pick("删除确认", "Confirm"),
			{ type: "warning" },
		);
	} catch {
		return;
	}
	await discussionApi.deleteTopic(topic.value.id);
	ElMessage.success(lang.pick("已删除", "Deleted"));
	router.replace("/discussion");
}

function openReport(type: string, id: string, label: string) {
	if (!requireLogin()) return;
	reportTarget.value = { type, id, label };
	reportForm.value = { reason: "spam", detail: "" };
	reportVisible.value = true;
}

async function submitReport() {
	if (!reportTarget.value) return;
	try {
		await discussionApi.report({
			targetType: reportTarget.value.type,
			targetId: reportTarget.value.id,
			reason: reportForm.value.reason,
			detail: reportForm.value.detail || undefined,
		});
		ElMessage.success(lang.pick("举报已提交，感谢反馈", "Report submitted"));
		reportVisible.value = false;
	} catch {
		/* 重复举报等错误由拦截器提示 */
	}
}

watch(topicId, () => {
	page.value = 0;
	reply.value = "";
	replyImages.value = [];
	quote.value = null;
	load();
});

onMounted(async () => {
	await load();
	notice.refresh();
});

onBeforeUnmount(() => window.clearTimeout(mentionHintTimer));
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/discussion">{{ lang.pick("讨论区", "Community") }}</router-link> ›
			<span>{{ topic?.title || lang.t("loading") }}</span>
		</div>
	</div>

	<div class="container">
		<el-skeleton
			v-if="loading"
			:rows="8"
			animated
		/>
		<el-empty
			v-else-if="error || !topic"
			:description="lang.pick('帖子不存在或已被删除', 'Topic not found')"
		>
			<el-button
				type="primary"
				@click="router.push('/discussion')"
				>{{ lang.pick("返回讨论区", "Back to community") }}</el-button
			>
		</el-empty>

		<template v-else>
			<!-- 帖子头部 -->
			<article class="topic-head">
				<div class="badges">
					<router-link
						class="board-chip"
						:to="{ path: '/discussion', query: { board: topic.boardId } }"
					>
						{{ topic.boardName }}
					</router-link>
					<span
						v-if="topic.pinned"
						class="tag"
						>{{ lang.pick("置顶", "Pinned") }}</span
					>
					<span
						v-if="topic.featured"
						class="tag featured"
						>{{ lang.pick("精华", "Featured") }}</span
					>
					<span
						v-if="topic.locked"
						class="tag"
						>{{ lang.pick("已锁定", "Locked") }}</span
					>
					<span
						v-if="topic.lang && topic.lang !== 'zh'"
						class="tag muted"
						>{{ topic.lang.toUpperCase() }}</span
					>
					<span
						v-if="titleTrans"
						class="mt-badge"
						>{{ translateProviderLabel(titleProvider) }}</span
					>
				</div>
				<h1
					v-motion-fade-up
					:class="{ 'mt-text': !!titleTrans }"
				>
					{{ titleTrans ?? topic.title }}
				</h1>
				<div class="author-line">
					<router-link
						class="author-link"
						:to="`/user/${topic.author?.id}`"
					>
						<UserAvatar
							:name="topic.author?.nickname"
							:src="topic.author?.avatar"
						/>
						<span class="name">{{ topic.author?.nickname }}</span>
					</router-link>
					<span class="dot">·</span>
					<span>{{ topic.createdAt }}</span>
					<template v-if="topic.editedAt">
						<span class="dot">·</span>
						<span>{{ lang.pick("已编辑", "edited") }} {{ topic.editedAt }}</span>
					</template>
					<div class="ops">
						<SubscribeButton
							target-type="topic"
							:target-id="topic.id"
						/>
						<TranslateButton
							v-model="titleTrans"
							v-model:provider="titleProvider"
							target-type="topic"
							:target-id="topic.id"
							scope="title"
							:source-locale="topic.lang"
						/>
						<button
							v-if="topic.mine"
							class="link"
							@click="removeTopic"
						>
							{{ lang.pick("删除", "Delete") }}
						</button>
						<button
							v-else
							class="link"
							@click="openReport('discussion_topic', topic.id, topic.title)"
						>
							{{ lang.pick("举报", "Report") }}
						</button>
					</div>
				</div>

				<el-alert
					v-if="topic.reviewNote"
					class="review-note"
					type="warning"
					:closable="false"
					:title="topic.reviewNote"
				/>

				<div class="body">
					<div class="body-bar">
						<span
							v-if="bodyTrans"
							class="mt-badge"
							>{{ translateProviderLabel(bodyProvider) }}</span
						>
						<TranslateButton
							v-model="bodyTrans"
							v-model:provider="bodyProvider"
							target-type="topic"
							:target-id="topic.id"
							scope="body"
							:source-locale="topic.lang"
							compact
						/>
					</div>
					<UserContent
						:class="{ 'mt-text': !!bodyTrans }"
						:content="bodyTrans ?? topic.content"
						:images="topic.images"
					/>
				</div>
			</article>

			<InteractionBar
				type="discussion_topic"
				:id="topic.id"
			/>

			<!-- 楼层 -->
			<div class="floor-title">
				<span>{{ lang.pick("全部回复", "Replies") }}</span>
				<strong>{{ total }}</strong>
			</div>

			<div
				v-if="posts.length"
				class="floors"
			>
				<div
					v-for="(p, i) in posts"
					:key="p.id"
					class="floor"
					v-motion="fadeUp({ delay: Math.min(i * 40, 320), distance: 12 })"
				>
					<div class="floor-no">
						<strong>{{ p.floorNo }}</strong>
						<span>{{ lang.pick("楼", "F") }}</span>
					</div>
					<div class="floor-main">
						<div class="floor-head">
							<router-link
								class="author-link"
								:to="`/user/${p.author?.id}`"
							>
								<UserAvatar
									:name="p.author?.nickname"
									:src="p.author?.avatar"
									:size="22"
								/>
								<span class="name">{{ p.author?.nickname }}</span>
							</router-link>
							<span
								v-if="p.author?.owner"
								class="owner"
								>{{ lang.pick("楼主", "OP") }}</span
							>
							<span class="dot">·</span>
							<span class="time">{{ p.createdAt }}</span>
							<span
								v-if="p.status === 'pending'"
								class="pend"
								>{{ lang.pick("审核中", "pending") }}</span
							>
						</div>

						<blockquote
							v-if="p.quoteExcerpt"
							class="quote"
						>
							<span class="q-author">{{ p.quoteAuthor }}</span>
							{{ p.quoteExcerpt }}
						</blockquote>

						<UserContent
							:class="{ 'mt-text': !!floorTrans[p.id] }"
							:content="floorTrans[p.id] ?? p.content"
							:images="p.images"
						/>

						<div class="floor-ops">
							<TranslateButton
								v-model="floorTrans[p.id]"
								v-model:provider="floorProvider[p.id]"
								target-type="post"
								:target-id="p.id"
								scope="body"
								:source-locale="p.lang"
								compact
							/>
							<span
								v-if="floorTrans[p.id]"
								class="mt-badge"
								>{{ translateProviderLabel(floorProvider[p.id]) }}</span
							>
							<button
								class="link"
								:class="{ on: likedPosts[p.id] }"
								@click="togglePostLike(p)"
							>
								{{ lang.pick("赞", "Like") }} {{ p.likeCount || 0 }}
							</button>
							<button
								class="link"
								:title="
									lang.pick(
										'引用该楼层：回复中会显示引用块，并通知被引用者',
										'Quote this floor: shown as a quote block, and the author gets notified',
									)
								"
								@click="startQuote(p)"
							>
								{{ lang.pick("引用", "Quote") }}
							</button>
							<button
								v-if="p.mine"
								class="link"
								@click="removePost(p)"
							>
								{{ lang.pick("删除", "Delete") }}
							</button>
							<button
								v-else
								class="link"
								@click="openReport('discussion_post', p.id, `#${p.floorNo}`)"
							>
								{{ lang.pick("举报", "Report") }}
							</button>
						</div>
					</div>
				</div>
			</div>
			<el-empty
				v-else
				:description="lang.pick('还没有回复，来说点什么吧', 'No replies yet')"
				:image-size="70"
			/>
			<AppPagination
				:current="page"
				:total="total"
				:size="size"
				@change="onPage"
			/>

			<!-- 回复框：引用 + @提及（输入「@」自动提示约 10 个用户） -->
			<div
				id="reply-box"
				class="reply-box"
			>
				<div class="reply-head">
					<span>{{ lang.pick("发表回复", "Write a reply") }}</span>
					<span
						v-if="quote"
						class="quote-chip"
					>
						{{ lang.pick("引用", "Quote") }} {{ quote.author?.nickname }} #{{ quote.floorNo }}
						<button
							class="link"
							@click="quote = null"
						>
							✕
						</button>
					</span>
					<span
						v-if="lastMention"
						class="mention-chip"
					>
						{{ lang.pick(`已提及 @${lastMention}`, `Mentioned @${lastMention}`) }}
						<span class="mc-sub">{{ lang.pick("发布后对方会收到通知", "they get notified after posting") }}</span>
					</span>
				</div>
				<MentionTextarea
					v-model="reply"
					:rows="5"
					:maxlength="5000"
					:disabled="!canReply"
					:locals="mentionLocals"
					:placeholder="
						canReply
							? lang.pick('友好交流；输入 @ 可自动提示用户，以「> 」开头表示引用，空行分段', 'Be kind. Type @ to pick a user, start a line with > to quote, blank line for a new paragraph')
							: lang.pick('该帖子已锁定，暂不可回复', 'This topic is locked')
					"
					@pick="onMentionPick"
					@toggle="mentioning = $event"
				/>
				<!-- 引用 / 提及 操作提示（常驻，随状态变化） -->
				<ComposerTips
					:quote="quote ? { nickname: quote.author?.nickname, floorNo: quote.floorNo } : null"
					:mentioning="mentioning"
				/>
				<div
					v-if="replyImages.length"
					class="reply-images"
				>
					<div
						v-for="(img, i) in replyImages"
						:key="img"
						class="thumb"
					>
						<img
							:src="img"
							alt=""
						/>
						<button
							class="del"
							@click="replyImages.splice(i, 1)"
						>
							✕
						</button>
					</div>
				</div>
				<div class="reply-actions">
					<label class="upload">
						<input
							type="file"
							accept="image/*"
							multiple
							hidden
							@change="pickImages"
						/>
						<span>{{ uploading ? lang.pick("上传中…", "Uploading…") : lang.pick("添加图片", "Add image") }}</span>
					</label>
					<button
						class="btn btn-accent"
						:disabled="!canReply || posting"
						@click="submitReply"
					>
						{{ posting ? lang.pick("提交中…", "Submitting…") : lang.pick("发表回复", "Reply") }}
					</button>
				</div>
			</div>
		</template>
	</div>

	<!-- 举报弹窗 -->
	<el-dialog
		v-model="reportVisible"
		:title="lang.pick('举报内容', 'Report content')"
		width="440"
	>
		<p class="report-target">{{ reportTarget?.label }}</p>
		<el-select
			v-model="reportForm.reason"
			style="width: 100%"
		>
			<el-option
				v-for="r in REPORT_REASONS"
				:key="r.value"
				:label="r.label"
				:value="r.value"
			/>
		</el-select>
		<el-input
			v-model="reportForm.detail"
			class="mt"
			type="textarea"
			:rows="3"
			maxlength="500"
			:placeholder="lang.pick('补充说明（可选）', 'Details (optional)')"
		/>
		<template #footer>
			<el-button @click="reportVisible = false">{{ lang.pick("取消", "Cancel") }}</el-button>
			<el-button
				type="primary"
				@click="submitReport"
				>{{ lang.pick("提交举报", "Submit") }}</el-button
			>
		</template>
	</el-dialog>
</template>

<script lang="ts">
export default { name: "DiscussionTopic" };
</script>

<style scoped>
.topic-head {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 26px 28px 24px;
	margin-top: 8px;
}
.badges {
	display: flex;
	gap: 8px;
	align-items: center;
	flex-wrap: wrap;
	margin-bottom: 12px;
}
.board-chip {
	font-size: 12px;
	padding: 3px 10px;
	border: 1px solid var(--ink);
	color: var(--ink);
}
.tag {
	font-size: 11px;
	padding: 2px 8px;
	border: 1px solid var(--accent);
	color: var(--accent);
}
.tag.featured {
	border-color: #b07d2b;
	color: #b07d2b;
}
.tag.muted {
	border-color: var(--line);
	color: var(--muted);
}
.topic-head h1 {
	font-size: clamp(22px, 3.2vw, 32px);
	line-height: 1.35;
	margin: 0 0 12px;
}
.author-line {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 13px;
	color: var(--muted);
	flex-wrap: wrap;
}
.author-line .name {
	color: var(--ink);
	font-weight: 600;
}
/* 作者可点击进入社区主页 */
.author-link {
	display: inline-flex;
	align-items: center;
	gap: 8px;
}
.author-link:hover .name {
	color: var(--accent);
}
.dot {
	opacity: 0.5;
}
.ops {
	margin-left: auto;
	display: flex;
	align-items: center;
	gap: 12px;
}
.review-note {
	margin: 14px 0 0;
}
.body {
	margin-top: 18px;
}
/* 正文顶部的翻译标注与「译 / 查看原文」按钮 */
.body-bar {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	gap: 8px;
	margin-bottom: 6px;
}
.floor-title {
	display: flex;
	align-items: baseline;
	gap: 10px;
	margin: 32px 0 12px;
	padding-bottom: 10px;
	border-bottom: 2px solid var(--ink);
	font-family: var(--serif);
	font-size: 20px;
}
.floor-title strong {
	color: var(--accent);
	font-size: 16px;
}
.floors {
	border: 1px solid var(--line);
	border-bottom: none;
}
.floor {
	display: flex;
	gap: 16px;
	padding: 18px 20px;
	border-bottom: 1px solid var(--line);
	background: var(--paper);
}
.floor-no {
	flex: none;
	width: 44px;
	text-align: center;
	color: var(--muted);
}
.floor-no strong {
	display: block;
	font-family: var(--serif);
	font-size: 20px;
	color: var(--accent);
}
.floor-no span {
	font-size: 11px;
}
.floor-main {
	flex: 1;
	min-width: 0;
}
.floor-head {
	display: flex;
	align-items: center;
	gap: 8px;
	font-size: 13px;
	color: var(--muted);
	margin-bottom: 8px;
}
.floor-head .name {
	color: var(--ink);
	font-weight: 600;
}
.floor-head .owner {
	font-size: 11px;
	border: 1px solid var(--accent);
	color: var(--accent);
	padding: 0 6px;
}
.floor-head .pend {
	font-size: 11px;
	color: #b07d2b;
}
.quote {
	margin: 0 0 10px;
	padding: 8px 12px;
	border-left: 3px solid var(--line);
	background: var(--paper-2);
	font-size: 13.5px;
	color: var(--muted);
}
.quote .q-author {
	color: var(--ink);
	margin-right: 6px;
}
.floor-ops {
	display: flex;
	gap: 16px;
	margin-top: 10px;
}
.link {
	background: none;
	border: none;
	padding: 0;
	font: inherit;
	font-size: 13px;
	color: var(--muted);
	cursor: pointer;
}
.link:hover {
	color: var(--accent);
}
.link.on {
	color: var(--accent);
	font-weight: 600;
}
.reply-box {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 18px 20px;
	margin: 26px 0 8px;
}
.reply-head {
	display: flex;
	align-items: center;
	gap: 12px;
	font-family: var(--serif);
	font-size: 17px;
	margin-bottom: 12px;
}
.quote-chip {
	font-family: var(--font-sans);
	font-size: 12px;
	color: var(--muted);
	border: 1px solid var(--line);
	padding: 2px 8px;
}
/* 插入 @提及 后的轻反馈：说明被提及者会收到通知 */
.mention-chip {
	display: inline-flex;
	align-items: baseline;
	gap: 6px;
	font-family: var(--font-sans);
	font-size: 12px;
	color: var(--accent);
	border: 1px solid var(--accent);
	padding: 2px 8px;
	animation: mention-chip-in 0.2s var(--ease-out);
}
.mention-chip .mc-sub {
	color: var(--muted);
	font-size: 11px;
}
@keyframes mention-chip-in {
	from {
		opacity: 0;
		transform: translateY(-2px);
	}
	to {
		opacity: 1;
		transform: none;
	}
}
.reply-images {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
	margin-top: 10px;
}
.reply-images .thumb {
	position: relative;
	width: 88px;
	height: 66px;
	border: 1px solid var(--line);
	overflow: hidden;
}
.reply-images img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.reply-images .del {
	position: absolute;
	top: 2px;
	right: 2px;
	border: none;
	background: rgba(0, 0, 0, 0.55);
	color: #fff;
	font-size: 11px;
	width: 18px;
	height: 18px;
	cursor: pointer;
}
.reply-actions {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 14px;
}
.upload {
	cursor: pointer;
	font-size: 13px;
	color: var(--muted);
	border: 1px dashed var(--line);
	padding: 6px 14px;
}
.upload:hover {
	border-color: var(--ink);
	color: var(--ink);
}
.report-target {
	font-size: 13px;
	color: var(--muted);
	margin-bottom: 10px;
	word-break: break-all;
}
.mt {
	margin-top: 10px;
}
</style>
