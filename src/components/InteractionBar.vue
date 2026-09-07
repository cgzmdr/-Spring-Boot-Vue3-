<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { interactionApi, shareApi } from "@/api/modules";
import { useAuthStore } from "@/stores/auth";

const props = defineProps<{
	type: "ethnic" | "festival" | "art" | "topic";
	id: string;
}>();

const auth = useAuthStore();

const likeCount = ref(0);
const favoriteCount = ref(0);
const viewCount = ref(0);
const liked = ref(false);
const favorited = ref(false);
const shareUrl = ref("");
const busy = ref(false);

async function loadStats() {
	try {
		const s = await interactionApi.stats(props.type, props.id);
		likeCount.value = s.likeCount;
		favoriteCount.value = s.favoriteCount;
		viewCount.value = s.viewCount;
	} catch {
		/* 统计失败不阻塞页面 */
	}
}

/** 页面加载即记录一次浏览量（失败不影响页面） */
async function recordView() {
	try {
		viewCount.value = await interactionApi.view(props.type, props.id);
	} catch {
		/* 忽略浏览量上报失败 */
	}
}

async function toggleLike() {
	if (!auth.isLoggedIn) {
		ElMessage({
			message: "请登录",
			type: "warning",
			duration: 3000,
		});
		return;
	}
	busy.value = true;
	try {
		if (liked.value) {
			const r = await interactionApi.unlike(props.type, props.id);
			liked.value = r.liked;
			likeCount.value = r.likeCount;
		} else {
			const r = await interactionApi.like(props.type, props.id);
			liked.value = r.liked;
			likeCount.value = r.likeCount;
		}
	} catch {
		/* 拦截器已提示 */
	} finally {
		busy.value = false;
	}
}

async function toggleFavorite() {
	if (!auth.isLoggedIn) {
		ElMessage({
			message: "请登录",
			type: "warning",
			duration: 3000,
		});
		return;
	}
	busy.value = true;
	try {
		if (favorited.value) {
			await interactionApi.unfavorite(props.type, props.id);
			favorited.value = false;
			favoriteCount.value = Math.max(0, favoriteCount.value - 1);
		} else {
			await interactionApi.favorite(props.type, props.id);
			favorited.value = true;
			favoriteCount.value += 1;
		}
	} catch {
		/* 拦截器已提示 */
	} finally {
		busy.value = false;
	}
}

async function share() {
	try {
		const r = await shareApi.create(props.type, props.id);
		shareUrl.value = r.shareUrl;
		navigator.clipboard?.writeText(r.shareUrl).catch(() => {});
		ElMessage({
			message: "分享链接已生成并复制",
			type: "success",
			duration: 3000,
		});
	} catch {
		ElMessage.error("分享生成失败");
	}
}

onMounted(() => {
	recordView();
	loadStats();
});
</script>

<template>
	<div class="interact-bar">
		<el-button
			class="interact-btn"
			:class="{ on: liked }"
			:disabled="busy"
			@click="toggleLike"
			plain
			:aria-pressed="liked"
		>
			<svg
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="1.8"
			>
				<path
					d="M7 10v11M7 10l4-8c1 0 2 1 2 3v3h6.4c1 0 1.8.9 1.6 1.9l-1.2 6c-.2.7-.8 1.1-1.6 1.1H7"
					stroke-linecap="round"
					stroke-linejoin="round"
				/>
			</svg>
			<span>{{ liked ? "已赞" : "点赞" }} · {{ likeCount }}</span>
		</el-button>

		<el-button
			class="interact-btn"
			:class="{ on: favorited }"
			:disabled="busy"
			@click="toggleFavorite"
			:aria-pressed="favorited"
		>
			<svg
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="1.8"
			>
				<path
					d="M12 20.3 4.6 12.9a4.9 4.9 0 0 1 0-6.9 4.7 4.7 0 0 1 6.9 0l.5.6.5-.6a4.7 4.7 0 0 1 6.9 0 4.9 4.9 0 0 1 0 6.9L12 20.3Z"
					stroke-linecap="round"
					stroke-linejoin="round"
				/>
			</svg>
			<span>{{ favorited ? "已收藏" : "收藏" }} · {{ favoriteCount }}</span>
		</el-button>

		<el-button
			disabled
			class="interact-stat"
			title="浏览量"
		>
			<svg
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="1.8"
			>
				<path
					d="M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12Z"
					stroke-linecap="round"
					stroke-linejoin="round"
				/>
				<circle
					cx="12"
					cy="12"
					r="2.6"
				/>
			</svg>
			<span>浏览 · {{ viewCount }}</span>
		</el-button>

		<el-button
			class="interact-btn"
			@click="share"
		>
			<svg
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="1.8"
			>
				<path
					d="M12 15V4m0 0 4 4m-4-4-4 4M5 14v5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-5"
					stroke-linecap="round"
					stroke-linejoin="round"
				/>
			</svg>
			<span>分享</span>
		</el-button>
	</div>
</template>
