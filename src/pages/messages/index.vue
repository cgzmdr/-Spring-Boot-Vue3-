<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import PageHead from "@/components/PageHead.vue";
import AppPagination from "@/components/AppPagination.vue";
import UserAvatar from "@/components/UserAvatar.vue";
import { messageApi } from "@/api/modules";
import type { Conversation } from "@/api/types";
import { useLangStore } from "@/stores/lang";
import { useNotificationStore } from "@/stores/notification";

const router = useRouter();
const lang = useLangStore();
const notice = useNotificationStore();

const items = ref<Conversation[]>([]);
const total = ref(0);
const page = ref(0);
const size = 20;
const loading = ref(true);

async function load(p = page.value) {
	page.value = p;
	loading.value = true;
	try {
		const res = await messageApi.conversations({ page: p, size });
		items.value = res.data;
		total.value = res.total;
	} catch {
		items.value = [];
	} finally {
		loading.value = false;
	}
}

onMounted(async () => {
	await load(0);
	notice.refresh();
});
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/discussion">{{ lang.pick("讨论区", "Community") }}</router-link> ›
			<span>{{ lang.pick("私信", "Messages") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('MESSAGES · 私信', 'MESSAGES')"
			:title="lang.pick('好友私信', 'Direct messages')"
			:dek="
				lang.pick(
					'仅互相关注的好友之间可以私信，欢迎与志同道合的朋友深入交流。',
					'Only mutual followers can message each other — keep it friendly.',
				)
			"
		/>

		<el-skeleton
			v-if="loading && !items.length"
			:rows="5"
			animated
		/>

		<div
			v-else-if="items.length"
			class="list"
		>
			<article
				v-for="c in items"
				:key="c.id"
				class="row"
				:class="{ unread: c.unread > 0 }"
				@click="router.push(`/messages/${c.id}`)"
			>
				<UserAvatar
					:name="c.peerName"
					:src="c.peerAvatar"
					:size="40"
				/>
				<div class="main">
					<div class="head">
						<span class="name">{{ c.peerName }}</span>
						<span class="time">{{ c.lastMessageAt || "" }}</span>
					</div>
					<p class="preview">
						<span v-if="c.lastMine">{{ lang.pick("我：", "Me: ") }}</span>
						{{ c.lastPreview || lang.pick("还没有消息，去打个招呼吧", "No messages yet") }}
					</p>
				</div>
				<span
					v-if="c.unread > 0"
					class="badge"
					>{{ c.unread > 99 ? "99+" : c.unread }}</span
				>
			</article>
		</div>

		<el-empty
			v-else
			:description="lang.pick('还没有私信会话，去用户主页点「私信」开始聊天', 'No conversations yet')"
			style="padding: 40px 0"
		>
			<el-button
				type="primary"
				@click="router.push('/discussion')"
				>{{ lang.pick("去社区逛逛", "Explore community") }}</el-button
			>
		</el-empty>

		<AppPagination
			:current="page"
			:total="total"
			:size="size"
			@change="load"
		/>
	</div>
</template>

<style scoped>
.list {
	border: 1px solid var(--line);
	border-bottom: none;
	margin-top: 18px;
}
.row {
	display: flex;
	align-items: center;
	gap: 14px;
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
.main {
	flex: 1;
	min-width: 0;
}
.head {
	display: flex;
	align-items: baseline;
	justify-content: space-between;
	gap: 12px;
}
.head .name {
	font-weight: 600;
	font-size: 15px;
}
.head .time {
	font-size: 12px;
	color: var(--muted);
}
.preview {
	font-size: 13.5px;
	color: var(--muted);
	margin: 4px 0 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
.badge {
	flex: none;
	min-width: 20px;
	height: 20px;
	padding: 0 6px;
	border-radius: 10px;
	background: var(--accent);
	color: #fff;
	font-size: 11px;
	line-height: 20px;
	text-align: center;
}
</style>
