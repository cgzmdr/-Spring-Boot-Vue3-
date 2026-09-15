<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import PageHead from "@/components/PageHead.vue";
import AppPagination from "@/components/AppPagination.vue";
import UserAvatar from "@/components/UserAvatar.vue";
import { useNotificationStore } from "@/stores/notification";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";

const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();
const notice = useNotificationStore();

const page = ref(0);
const size = 20;
const total = ref(0);
const unreadOnly = ref(false);
const loaded = ref(false);

const TYPE_LABEL: Record<string, string> = {
	topic_reply: "帖子回复",
	post_reply: "回复被引用",
	mention: "@提及",
	like: "点赞",
	favorite: "收藏",
	review_result: "审核结果",
	report_result: "举报处理",
	system: "系统通知",
};

const typeLabel = (type: string) => TYPE_LABEL[type] || type;

async function load(p = page.value) {
	page.value = p;
	const res = await notice.loadList(unreadOnly.value, p, size);
	total.value = res.total;
	loaded.value = true;
}

function toggleUnreadOnly() {
	unreadOnly.value = !unreadOnly.value;
	load(0);
}

async function markAll() {
	await notice.markAllRead();
	await load(0);
}

function open(item: { targetType: string | null; targetId: string | null; type: string; link?: string | null }) {
	// 站内公告（后台 OA 群发）可带自定义站内跳转
	if (item.link) {
		router.push(item.link);
		return;
	}
	if (item.targetType === "discussion_topic" && item.targetId) {
		router.push(`/discussion/topic/${item.targetId}`);
		return;
	}
	if (item.targetType === "discussion_post" && item.targetId) {
		router.push(`/discussion/topic/${item.targetId}`);
		return;
	}
	router.push("/discussion");
}

onMounted(async () => {
	if (!auth.isLoggedIn) {
		router.replace({ path: "/", query: { login: "1" } });
		return;
	}
	await load(0);
	notice.refresh();
});

const emptyText = computed(() =>
	unreadOnly.value ? lang.pick("没有未读通知", "No unread notifications") : lang.pick("暂无通知", "No notifications yet"),
);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> › <span>{{ lang.pick("通知", "Notifications") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('NOTIFICATIONS · 通知中心', 'NOTIFICATIONS')"
			:title="lang.pick('我的通知', 'My notifications')"
			:dek="lang.pick('回复、引用、审核结果与举报处理都会在这里提醒你。', 'Replies, mentions, review results and report updates.')"
		/>

		<div class="bar">
			<button
				class="chip"
				:class="{ active: !unreadOnly }"
				@click="unreadOnly ? toggleUnreadOnly() : null"
			>
				{{ lang.pick("全部", "All") }}
			</button>
			<button
				class="chip"
				:class="{ active: unreadOnly }"
				@click="!unreadOnly ? toggleUnreadOnly() : null"
			>
				{{ lang.pick("未读", "Unread") }}
				<span
					v-if="notice.unread"
					class="cnt"
					>{{ notice.unread }}</span
				>
			</button>
			<span style="flex: 1"></span>
			<button
				class="chip"
				:disabled="!notice.unread"
				@click="markAll"
			>
				{{ lang.pick("全部标为已读", "Mark all read") }}
			</button>
		</div>

		<el-skeleton
			v-if="!loaded"
			:rows="5"
			animated
		/>

		<div
			v-else-if="notice.items.length"
			class="list"
		>
			<article
				v-for="n in notice.items"
				:key="n.id"
				class="row"
				:class="{ unread: !n.read }"
				@click="open(n)"
			>
				<UserAvatar
					:name="n.actorName || '系统'"
					:src="n.actorAvatar"
					:size="30"
				/>
				<div class="body">
					<div class="head">
						<span class="type">{{ typeLabel(n.type) }}</span>
						<strong>{{ n.title }}</strong>
					</div>
					<p class="content">{{ n.content }}</p>
					<div class="time">{{ n.createdAt }}</div>
				</div>
				<span
					v-if="!n.read"
					class="dot"
				/>
			</article>
		</div>

		<el-empty
			v-else
			:description="emptyText"
			style="padding: 40px 0"
		/>

		<AppPagination
			:current="page"
			:total="total"
			:size="size"
			@change="load"
		/>
	</div>
</template>

<style scoped>
.bar {
	display: flex;
	align-items: center;
	gap: 8px;
	margin: 20px 0 14px;
}
.chip .cnt {
	margin-left: 6px;
	font-size: 11px;
}
.list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.row {
	display: flex;
	gap: 12px;
	padding: 16px 18px;
	border-bottom: 1px solid var(--line);
	background: var(--paper);
	cursor: pointer;
	position: relative;
	transition: background var(--dur-fast) ease;
}
.row:hover {
	background: var(--paper-2);
}
.row.unread {
	background: color-mix(in srgb, var(--accent) 4%, var(--paper));
}
.body {
	flex: 1;
	min-width: 0;
}
.head {
	display: flex;
	align-items: center;
	gap: 10px;
	flex-wrap: wrap;
}
.type {
	font-size: 11px;
	border: 1px solid var(--line);
	padding: 1px 8px;
	color: var(--muted);
}
.head strong {
	font-size: 15px;
}
.content {
	font-size: 13.5px;
	color: var(--muted);
	margin: 6px 0 4px;
	overflow-wrap: anywhere;
}
.time {
	font-size: 12px;
	color: var(--muted);
	opacity: 0.8;
}
.dot {
	position: absolute;
	top: 18px;
	right: 16px;
	width: 8px;
	height: 8px;
	border-radius: 50%;
	background: var(--accent);
}
</style>
