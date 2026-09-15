<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { discussionApi } from "@/api/modules";
import type { DiscussionTopicBrief } from "@/api/types";
import { contentExcerpt } from "@/utils/usercontent";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";

/**
 * 内容页「相关讨论」区块（民族 / 节日 / 艺术 / 美食 / 专题通用）。
 * 复用 /discussion/topics/linked 接口，点「发起讨论」会把关联内容带到发帖页。
 */
const props = withDefaults(
	defineProps<{
		linkedType: string;
		linkedId: string;
		label?: string;
		size?: number;
	}>(),
	{ label: "", size: 4 },
);

const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();

const topics = ref<DiscussionTopicBrief[]>([]);
const loading = ref(true);

async function load() {
	if (!props.linkedType || !props.linkedId) {
		loading.value = false;
		return;
	}
	loading.value = true;
	try {
		topics.value = await discussionApi.linkedTopics(props.linkedType, props.linkedId, props.size);
	} catch {
		topics.value = [];
	} finally {
		loading.value = false;
	}
}

function startDiscussion() {
	if (!auth.isLoggedIn) {
		window.dispatchEvent(new CustomEvent("auth:required"));
		return;
	}
	router.push({
		path: "/discussion/new",
		query: { linkedType: props.linkedType, linkedId: props.linkedId, linkedLabel: props.label },
	});
}

onMounted(load);
</script>

<template>
	<section class="linked-discussion">
		<div class="head">
			<div class="rule">
				<span class="no">讨论</span>
				<h3>{{ label ? `关于「${label}」的讨论` : lang.pick("相关讨论", "Related discussions") }}</h3>
				<span class="line" />
			</div>
			<button
				class="btn btn-accent talk-btn"
				@click="startDiscussion"
			>
				{{ lang.pick("发起讨论", "Start a discussion") }}
			</button>
		</div>

		<el-skeleton
			v-if="loading"
			:rows="3"
			animated
		/>
		<template v-else-if="topics.length">
			<div class="list">
				<router-link
					v-for="t in topics"
					:key="t.id"
					class="row"
					:to="`/discussion/topic/${t.id}`"
					v-motion-fade-up
				>
					<div class="main">
						<h4>
							<span
								v-if="t.featured"
								class="tag"
								>{{ lang.pick("精华", "Featured") }}</span
							>
							{{ t.title }}
						</h4>
						<p>{{ contentExcerpt(t.excerpt, 96) }}</p>
						<div class="meta">
							{{ t.author?.nickname }} · {{ t.createdAt }}
							<span v-if="t.boardName"> · {{ t.boardName }}</span>
							<span
								v-if="t.lang && t.lang !== 'zh'"
								class="lang"
								>{{ t.lang.toUpperCase() }}</span
							>
						</div>
					</div>
					<div class="counts">
						<strong>{{ t.replyCount || 0 }}</strong>
						<span>{{ lang.pick("回复", "replies") }}</span>
					</div>
				</router-link>
			</div>
			<div class="more">
				<router-link
					class="more-link"
					:to="{ path: '/discussion', query: label ? { keyword: label } : {} }"
				>
					{{ lang.pick("查看更多相关讨论", "More discussions") }} &gt;&gt;
				</router-link>
			</div>
		</template>
		<el-empty
			v-else
			:description="
				lang.pick(
					`还没有相关讨论${label ? `，来聊聊「${label}」` : ''}`,
					'No discussions yet — start the first one',
				)
			"
			:image-size="80"
		>
			<el-button
				type="primary"
				@click="startDiscussion"
				>{{ lang.pick("发起讨论", "Start a discussion") }}</el-button
			>
		</el-empty>
	</section>
</template>

<style scoped>
.linked-discussion {
	margin: 34px 0 10px;
}
.head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
	flex-wrap: wrap;
	margin-bottom: 16px;
}
.rule {
	display: flex;
	align-items: center;
	gap: 14px;
	flex: 1;
	min-width: 0;
}
.rule .no {
	font-family: var(--serif);
	font-size: 13px;
	color: var(--accent);
	font-weight: 700;
	letter-spacing: 0.1em;
	white-space: nowrap;
}
.rule h3 {
	font-family: var(--serif);
	font-size: 22px;
	letter-spacing: 0.06em;
	margin: 0;
	white-space: nowrap;
}
.rule .line {
	flex: 1;
	height: 1px;
	background: var(--line);
}
.talk-btn {
	padding: 9px 20px;
	font-size: 12px;
}
.list {
	border: 1px solid var(--line);
	border-bottom: none;
}
.row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 18px;
	padding: 16px 18px;
	border-bottom: 1px solid var(--line);
	background: var(--paper);
	transition: background var(--dur-fast) ease;
}
.row:hover {
	background: var(--paper-2);
}
.main {
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
.meta {
	font-size: 12px;
	color: var(--muted);
}
.meta .lang {
	border: 1px solid var(--line);
	padding: 0 5px;
	margin-left: 6px;
	font-size: 10px;
}
.tag {
	font-size: 11px;
	color: #b07d2b;
	border: 1px solid #b07d2b;
	padding: 1px 6px;
	margin-right: 6px;
}
.counts {
	flex: none;
	text-align: center;
	min-width: 52px;
}
.counts strong {
	display: block;
	font-family: var(--serif);
	font-size: 18px;
	color: var(--accent);
}
.counts span {
	font-size: 11px;
	color: var(--muted);
}
.more {
	text-align: right;
	margin-top: 10px;
}
.more-link {
	font-size: 13px;
	color: var(--accent);
}
.more-link:hover {
	text-decoration: underline;
}
@media (max-width: 700px) {
	.rule h3 {
		font-size: 18px;
		white-space: normal;
	}
	.rule .line {
		display: none;
	}
}
</style>
