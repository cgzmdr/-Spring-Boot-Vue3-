<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import PageHead from "@/components/PageHead.vue";
import MentionTextarea from "@/components/MentionTextarea.vue";
import ComposerTips from "@/components/ComposerTips.vue";
import { discussionApi } from "@/api/modules";
import type { DiscussionBoard } from "@/api/types";
import { compressImage } from "@/utils/usercontent";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();

const boards = ref<DiscussionBoard[]>([]);
const form = ref({
	boardId: (route.query.board as string) || "",
	title: "",
	content: "",
	images: [] as string[],
});
const linked = computed(() => ({
	type: (route.query.linkedType as string) || "",
	id: (route.query.linkedId as string) || "",
	label: (route.query.linkedLabel as string) || "",
}));
const submitting = ref(false);
const uploading = ref(false);
/** @提及联想面板展开状态（提示条据此高亮） */
const mentioning = ref(false);

async function loadBoards() {
	try {
		boards.value = await discussionApi.boards();
		if (!form.value.boardId && boards.value.length) form.value.boardId = boards.value[0].id;
	} catch {
		boards.value = [];
	}
}

async function pickImages(event: Event) {
	const input = event.target as HTMLInputElement;
	const files = Array.from(input.files || []);
	input.value = "";
	if (!files.length) return;
	uploading.value = true;
	try {
		for (const file of files.slice(0, 9 - form.value.images.length)) {
			const compressed = await compressImage(file);
			const url = await discussionApi.uploadImage(compressed, compressed.name || "image.jpg");
			form.value.images.push(url);
		}
	} catch {
		ElMessage.error(lang.pick("图片上传失败", "Upload failed"));
	} finally {
		uploading.value = false;
	}
}

async function submit() {
	if (!auth.isLoggedIn) {
		ElMessage.warning(lang.pick("请先登录", "Please sign in first"));
		return;
	}
	if (!form.value.boardId) {
		ElMessage.warning(lang.pick("请选择板块", "Choose a board"));
		return;
	}
	const title = form.value.title.trim();
	const content = form.value.content.trim();
	if (title.length < 2) {
		ElMessage.warning(lang.pick("标题至少 2 个字", "Title needs at least 2 characters"));
		return;
	}
	if (!content) {
		ElMessage.warning(lang.pick("正文不能为空", "Content cannot be empty"));
		return;
	}
	submitting.value = true;
	try {
		const id = await discussionApi.createTopic({
			boardId: form.value.boardId,
			title,
			content,
			images: form.value.images,
			lang: lang.isEn ? "en" : "zh",
			linkedType: linked.value.type || null,
			linkedId: linked.value.id || null,
		});
		ElMessage.success(lang.pick("发布成功", "Published"));
		router.replace(`/discussion/topic/${id}`);
	} catch {
		/* 拦截器已提示 */
	} finally {
		submitting.value = false;
	}
}

function cancel() {
	router.back();
}

onMounted(loadBoards);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/discussion">{{ lang.pick("讨论区", "Community") }}</router-link> ›
			<span>{{ lang.pick("发表新帖", "New post") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('NEW POST · 发帖', 'NEW POST')"
			:title="lang.pick('发表新帖', 'Create a post')"
			:dek="
				lang.pick(
					'内容将公开可见并进入社区审核抽查；请勿发布广告、攻击性内容与他人隐私。',
					'Your post is public and subject to review. No ads, attacks, or private data.',
				)
			"
		/>

		<div class="compose">
			<el-alert
				v-if="linked.type"
				class="mb"
				type="info"
				:closable="false"
				:title="`${lang.pick('关联内容', 'Linked content')}：${linked.label || linked.type}`"
			/>

			<div class="field">
				<label>{{ lang.pick("选择板块", "Board") }}</label>
				<div class="board-picker">
					<button
						v-for="b in boards"
						:key="b.id"
						class="chip"
						:class="{ active: form.boardId === b.id }"
						@click="form.boardId = b.id"
					>
						{{ lang.pick(b.name, b.nameEn) }}
					</button>
				</div>
				<p
					v-if="boards.find((b) => b.id === form.boardId)?.description"
					class="hint"
				>
					{{ boards.find((b) => b.id === form.boardId)?.description }}
				</p>
			</div>

			<div class="field">
				<label>{{ lang.pick("标题", "Title") }}</label>
				<el-input
					v-model="form.title"
					maxlength="200"
					show-word-limit
					:placeholder="lang.pick('一句话说清你想聊什么', 'Summarize your topic in one line')"
				/>
			</div>

			<div class="field">
				<label>{{ lang.pick("正文", "Content") }}</label>
				<!-- 输入「@」自动提示用户（最多 10 条），被提及者会收到站内通知 -->
				<MentionTextarea
					v-model="form.content"
					:rows="12"
					:maxlength="20000"
					:placeholder="
						lang.pick(
							'空行分段；以「> 」开头表示引用；输入 @ 自动提示用户；可插入图片。',
							'Blank line = new paragraph; start a line with > to quote; type @ to pick a user; images supported.',
						)
					"
					@toggle="mentioning = $event"
				/>
				<ComposerTips :mentioning="mentioning" />
			</div>

			<div class="field">
				<label>{{ lang.pick("配图", "Images") }}</label>
				<div class="images">
					<div
						v-for="(img, i) in form.images"
						:key="img"
						class="thumb"
					>
						<img
							:src="img"
							alt=""
						/>
						<button
							class="del"
							@click="form.images.splice(i, 1)"
						>
							✕
						</button>
					</div>
					<label
						v-if="form.images.length < 9"
						class="uploader"
					>
						<input
							type="file"
							accept="image/*"
							multiple
							hidden
							@change="pickImages"
						/>
						<span>{{ uploading ? lang.pick("上传中…", "Uploading…") : "+" }}</span>
					</label>
				</div>
				<p class="hint">
					{{ lang.pick("最多 9 张，自动压缩后上传（单张 ≤ 5MB）", "Up to 9 images, auto-compressed (≤5MB each)") }}
				</p>
			</div>

			<div class="actions">
				<button
					class="btn"
					@click="cancel"
				>
					{{ lang.pick("取消", "Cancel") }}
				</button>
				<button
					class="btn btn-accent"
					:disabled="submitting"
					@click="submit"
				>
					{{ submitting ? lang.pick("发布中…", "Publishing…") : lang.pick("发布", "Publish") }}
				</button>
			</div>
		</div>
	</div>
</template>

<style scoped>
.compose {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 26px 28px 30px;
	margin-top: 16px;
}
.mb {
	margin-bottom: 18px;
}
.field {
	margin-bottom: 22px;
}
.field > label {
	display: block;
	font-family: var(--serif);
	font-size: 16px;
	margin-bottom: 10px;
}
.board-picker {
	display: flex;
	flex-wrap: wrap;
	gap: 8px;
}
.hint {
	font-size: 12px;
	color: var(--muted);
	margin-top: 8px;
}
.images {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
}
.thumb {
	position: relative;
	width: 104px;
	height: 78px;
	border: 1px solid var(--line);
	overflow: hidden;
}
.thumb img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.thumb .del {
	position: absolute;
	top: 2px;
	right: 2px;
	border: none;
	background: rgba(0, 0, 0, 0.55);
	color: #fff;
	width: 18px;
	height: 18px;
	font-size: 11px;
	cursor: pointer;
}
.uploader {
	width: 104px;
	height: 78px;
	border: 1px dashed var(--line);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 22px;
	color: var(--muted);
	cursor: pointer;
}
.uploader:hover {
	border-color: var(--ink);
	color: var(--ink);
}
.actions {
	display: flex;
	justify-content: flex-end;
	gap: 12px;
	margin-top: 8px;
}
</style>
