<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import UserAvatar from "@/components/UserAvatar.vue";
import { discussionApi, messageApi, socialApi } from "@/api/modules";
import type { Conversation, PrivateMessage } from "@/api/types";
import { useLangStore } from "@/stores/lang";
import { useNotificationStore } from "@/stores/notification";
import { resolveStaticUrl } from "@/utils/format";

/**
 * 私信会话：气泡视图 + 发送框 + 已读回执 + 轻量轮询（10 秒，页面隐藏时暂停）。
 */
const route = useRoute();
const router = useRouter();
const lang = useLangStore();
const notice = useNotificationStore();

const conversation = ref<Conversation | null>(null);
const messages = ref<PrivateMessage[]>([]);
const total = ref(0);
const page = ref(0);
const size = 30;
const loading = ref(true);
const sending = ref(false);
const draft = ref("");
const draftImages = ref<string[]>([]);
const uploading = ref(false);
const bodyEl = ref<HTMLElement | null>(null);
const peerMutual = ref(true);
const blocked = ref(false);

let timer: number | null = null;
const conversationId = () => route.params.id as string;

const sorted = computed(() => {
	// 接口按时间倒序返回（最新在前）；反转即为聊天视图的自然顺序。
	// 不依赖时间字符串排序：同一秒内的多条消息时间戳相同，字符串比较不稳定。
	return [...messages.value].reverse();
});

async function loadMessages(p = 0, silent = false) {
	if (!silent) loading.value = true;
	try {
		const res = await messageApi.messages(conversationId(), { page: p, size });
		messages.value = res.data;
		total.value = res.total;
		page.value = p;
		await messageApi.read(conversationId());
		notice.refresh();
		if (p === 0) await scrollToBottom();
	} catch {
		if (!silent) messages.value = [];
	} finally {
		loading.value = false;
	}
}

async function loadConversation() {
	try {
		conversation.value = await messageApi.conversation(conversationId());
		blocked.value = conversation.value.blocked;
		const profile = await socialApi.profile(conversation.value.peerId);
		peerMutual.value = profile.mutual;
	} catch {
		conversation.value = null;
	}
}

/** 上传私信图片（前端压缩后上传，最多 4 张） */
async function pickImages(event: Event) {
	const input = event.target as HTMLInputElement;
	const files = Array.from(input.files || []);
	input.value = "";
	if (!files.length) return;
	uploading.value = true;
	try {
		const { compressImage } = await import("@/utils/usercontent");
		for (const file of files.slice(0, 4 - draftImages.value.length)) {
			const compressed = await compressImage(file);
			const url = await discussionApi.uploadImage(compressed, compressed.name || "image.jpg");
			draftImages.value.push(url);
		}
	} catch {
		ElMessage.error(lang.pick("图片上传失败", "Upload failed"));
	} finally {
		uploading.value = false;
	}
}

async function scrollToBottom() {
	await nextTick();
	const el = bodyEl.value;
	if (el) el.scrollTop = el.scrollHeight;
}

async function send() {
	const text = draft.value.trim();
	if (!text && !draftImages.value.length) return;
	if (!peerMutual.value) {
		ElMessage.warning(lang.pick("仅互相关注的好友之间可以私信", "Mutual follow required"));
		return;
	}
	if (blocked.value) {
		ElMessage.warning(lang.pick("你已拉黑对方，解除拉黑后才能继续聊天", "You blocked this user"));
		return;
	}
	sending.value = true;
	try {
		await messageApi.send(conversationId(), text, lang.isEn ? "en" : "zh", draftImages.value);
		draft.value = "";
		draftImages.value = [];
		await loadMessages(0, true);
	} catch {
		/* 拦截器已提示 */
	} finally {
		sending.value = false;
	}
}

/** 撤回自己的消息（2 分钟内） */
async function recall(message: PrivateMessage) {
	try {
		await ElMessageBox.confirm(lang.pick("确定撤回这条消息？", "Recall this message?"), lang.pick("撤回", "Recall"), {
			type: "warning",
		});
	} catch {
		return;
	}
	try {
		await messageApi.recall(message.id);
		ElMessage.success(lang.pick("已撤回", "Recalled"));
		await loadMessages(0, true);
	} catch {
		/* 拦截器已提示 */
	}
}

/** 拉黑 / 解除拉黑 */
async function toggleBlock() {
	if (!conversation.value) return;
	const next = !blocked.value;
	try {
		await ElMessageBox.confirm(
			next
				? lang.pick("拉黑后将解除互相关注，双方不能再发私信。确定拉黑？", "Blocking removes mutual follow and stops messages. Continue?")
				: lang.pick("确定解除拉黑？解除后需重新互相关注才能私信。", "Unblock this user?"),
			lang.pick("拉黑设置", "Block"),
			{ type: "warning" },
		);
	} catch {
		return;
	}
	blocked.value = await messageApi.block(conversation.value.peerId, next);
	ElMessage.success(next ? lang.pick("已拉黑", "Blocked") : lang.pick("已解除拉黑", "Unblocked"));
	await loadConversation();
}

function onKeydown(event: Event) {
	const key = (event as KeyboardEvent).key;
	if (key === "Enter" && !(event as KeyboardEvent).shiftKey) {
		event.preventDefault();
		send();
	}
}

watch(
	() => route.params.id,
	async () => {
		messages.value = [];
		await Promise.all([loadConversation(), loadMessages(0)]);
	},
);

onMounted(async () => {
	await Promise.all([loadConversation(), loadMessages(0)]);
	timer = window.setInterval(() => {
		if (document.visibilityState === "visible") loadMessages(0, true);
	}, 10_000);
});

onUnmounted(() => {
	if (timer !== null) window.clearInterval(timer);
});
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/messages">{{ lang.pick("私信", "Messages") }}</router-link> ›
			<span>{{ conversation?.peerName || lang.t("loading") }}</span>
		</div>

		<el-empty
			v-if="!conversation && !loading"
			:description="lang.pick('会话不存在', 'Conversation not found')"
		>
			<el-button
				type="primary"
				@click="router.push('/messages')"
				>{{ lang.pick("返回私信列表", "Back") }}</el-button
			>
		</el-empty>

		<template v-else>
			<!-- 会话头 -->
			<div class="chat-head">
				<router-link
					class="peer"
					:to="`/user/${conversation?.peerId}`"
				>
					<UserAvatar
						:name="conversation?.peerName"
						:src="conversation?.peerAvatar"
						:size="34"
					/>
					<span class="name">{{ conversation?.peerName }}</span>
				</router-link>
				<span class="tip">{{ lang.pick("仅双方可见 · 请文明交流", "Private & respectful") }}</span>
				<button
					class="link"
					:class="{ danger: !blocked }"
					@click="toggleBlock"
				>
					{{ blocked ? lang.pick("解除拉黑", "Unblock") : lang.pick("拉黑", "Block") }}
				</button>
				<button
					class="link"
					@click="router.push('/messages')"
				>
					{{ lang.pick("全部私信", "All messages") }}
				</button>
			</div>

			<el-alert
				v-if="blocked"
				type="error"
				:closable="false"
				:title="lang.pick('你已拉黑对方：双方无法互发消息，且互相关注已解除', 'You blocked this user — messages are disabled')"
			/>
			<el-alert
				v-else-if="!peerMutual"
				type="warning"
				:closable="false"
				:title="lang.pick('你们已不是互相关注，暂时无法继续私信', 'Mutual follow required to continue')"
			/>

			<!-- 消息区 -->
			<div
				ref="bodyEl"
				class="chat-body"
			>
				<el-skeleton
					v-if="loading && !messages.length"
					:rows="4"
					animated
				/>
				<template v-else>
					<p
						v-if="total > size"
						class="more-tip"
					>
						{{ lang.pick("仅展示最近", "Showing latest") }} {{ size }} / {{ total }}
						{{ lang.pick("条消息", "messages") }}
					</p>
					<div
						v-for="m in sorted"
						:key="m.id"
						class="bubble-row"
						:class="{ mine: m.mine }"
					>
						<div
							class="bubble"
							:class="{ recalled: m.recalled }"
						>
							<p
								v-if="m.recalled"
								class="text recalled-text"
							>
								{{ lang.pick("消息已撤回", "Message recalled") }}
							</p>
							<template v-else>
								<p
									v-if="m.content"
									class="text"
								>
									{{ m.content }}
								</p>
								<div
									v-if="m.images?.length"
									class="msg-images"
								>
									<el-image
										v-for="(img, ii) in m.images"
										:key="img"
										class="msg-image"
										:src="resolveStaticUrl(img)"
										:preview-src-list="m.images.map((s) => resolveStaticUrl(s))"
										:initial-index="ii"
										fit="cover"
										preview-teleported
									/>
								</div>
							</template>
							<div class="meta">
								{{ m.createdAt?.slice(0, 16) }}
								<span v-if="m.mine && !m.recalled"> · {{ m.read ? lang.pick("已读", "read") : lang.pick("未读", "sent") }}</span>
								<button
									v-if="m.recallable"
									class="recall"
									@click="recall(m)"
								>
									{{ lang.pick("撤回", "Recall") }}
								</button>
							</div>
						</div>
					</div>
					<el-empty
						v-if="!messages.length"
						:description="lang.pick('还没有消息，发送第一条吧', 'Say hello!')"
						:image-size="70"
					/>
				</template>
			</div>

			<!-- 输入区 -->
			<div class="chat-input">
				<div
					v-if="draftImages.length"
					class="draft-images"
				>
					<div
						v-for="(img, i) in draftImages"
						:key="img"
						class="draft-thumb"
					>
						<img
							:src="resolveStaticUrl(img)"
							alt=""
						/>
						<button
							class="del"
							@click="draftImages.splice(i, 1)"
						>
							✕
						</button>
					</div>
				</div>
				<el-input
					v-model="draft"
					type="textarea"
					:rows="3"
					maxlength="2000"
					show-word-limit
					:disabled="!peerMutual || blocked"
					:placeholder="
						blocked
							? lang.pick('你已拉黑对方，无法发送消息', 'You blocked this user')
							: peerMutual
								? lang.pick('输入消息，Enter 发送 / Shift+Enter 换行', 'Enter to send, Shift+Enter for newline')
								: lang.pick('需互相关注后才能私信', 'Mutual follow required')
					"
					@keydown="onKeydown"
				/>
				<div class="send-bar">
					<label
						class="upload"
						:class="{ disabled: !peerMutual || blocked || draftImages.length >= 4 }"
					>
						<input
							type="file"
							accept="image/*"
							multiple
							hidden
							:disabled="!peerMutual || blocked || draftImages.length >= 4"
							@change="pickImages"
						/>
						<span>{{ uploading ? lang.pick("上传中…", "Uploading…") : lang.pick("图片", "Image") }}</span>
					</label>
					<button
						class="btn btn-accent"
						:disabled="sending || !peerMutual || blocked || (!draft.trim() && !draftImages.length)"
						@click="send"
					>
						{{ sending ? lang.pick("发送中…", "Sending…") : lang.pick("发送", "Send") }}
					</button>
				</div>
			</div>
		</template>
	</div>
</template>

<style scoped>
.chat-head {
	display: flex;
	align-items: center;
	gap: 14px;
	padding: 14px 18px;
	border: 1px solid var(--line);
	background: var(--paper);
	margin: 10px 0 0;
}
.peer {
	display: flex;
	align-items: center;
	gap: 10px;
}
.peer .name {
	font-size: 16px;
	font-weight: 600;
}
.peer:hover .name {
	color: var(--accent);
}
.chat-head .tip {
	flex: 1;
	font-size: 12px;
	color: var(--muted);
}
.link {
	background: none;
	border: none;
	font: inherit;
	font-size: 13px;
	color: var(--muted);
	cursor: pointer;
}
.link:hover {
	color: var(--accent);
}
.chat-body {
	border: 1px solid var(--line);
	border-top: none;
	background: var(--paper-2);
	padding: 18px;
	height: 52vh;
	min-height: 320px;
	overflow-y: auto;
}
.more-tip {
	text-align: center;
	font-size: 12px;
	color: var(--muted);
	margin-bottom: 14px;
}
.bubble-row {
	display: flex;
	margin-bottom: 12px;
}
.bubble-row.mine {
	justify-content: flex-end;
}
.bubble {
	max-width: 74%;
	background: var(--paper);
	border: 1px solid var(--line);
	padding: 10px 14px;
}
.bubble-row.mine .bubble {
	background: color-mix(in srgb, var(--accent) 8%, var(--paper));
	border-color: color-mix(in srgb, var(--accent) 30%, var(--line));
}
.bubble .text {
	margin: 0;
	font-size: 14.5px;
	line-height: 1.8;
	white-space: pre-wrap;
	overflow-wrap: anywhere;
}
.bubble .meta {
	font-size: 11px;
	color: var(--muted);
	margin-top: 6px;
	text-align: right;
	display: flex;
	align-items: center;
	justify-content: flex-end;
	gap: 8px;
}
.recall {
	background: none;
	border: none;
	padding: 0;
	font: inherit;
	font-size: 11px;
	color: var(--muted);
	cursor: pointer;
	text-decoration: underline;
}
.recall:hover {
	color: var(--accent);
}
.bubble.recalled {
	background: var(--paper-2);
	border-style: dashed;
}
.recalled-text {
	color: var(--muted);
	font-size: 13px;
	font-style: italic;
}
.msg-images {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
	gap: 6px;
	margin: 6px 0 2px;
	max-width: 360px;
}
.msg-image {
	width: 100%;
	aspect-ratio: 4 / 3;
	border: 1px solid var(--line);
	cursor: zoom-in;
}
.draft-images {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
	margin-bottom: 10px;
}
.draft-thumb {
	position: relative;
	width: 76px;
	height: 58px;
	border: 1px solid var(--line);
	overflow: hidden;
}
.draft-thumb img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.draft-thumb .del {
	position: absolute;
	top: 2px;
	right: 2px;
	border: none;
	background: rgba(0, 0, 0, 0.55);
	color: #fff;
	width: 16px;
	height: 16px;
	font-size: 10px;
	cursor: pointer;
}
.chat-input .upload {
	cursor: pointer;
	font-size: 12px;
	color: var(--muted);
	border: 1px dashed var(--line);
	padding: 6px 14px;
	margin-right: auto;
}
.chat-input .upload:hover {
	border-color: var(--ink);
	color: var(--ink);
}
.chat-input .upload.disabled {
	opacity: 0.45;
	cursor: not-allowed;
}
.chat-head .link.danger:hover {
	color: var(--accent);
}
.chat-input {
	border: 1px solid var(--line);
	border-top: none;
	background: var(--paper);
	padding: 14px 18px 16px;
}
.send-bar {
	display: flex;
	justify-content: flex-end;
	align-items: center;
	gap: 12px;
	margin-top: 10px;
}
</style>
