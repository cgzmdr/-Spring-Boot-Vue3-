import { defineStore } from "pinia";
import { ref } from "vue";
import { translateApi } from "@/api/modules";

/**
 * 机器翻译能力状态：站点未接入翻译服务（app.translate.provider=none）时
 * 全站隐藏「译」按钮，避免用户点了没反应。只请求一次并缓存。
 */
export const useTranslateStore = defineStore("translate", () => {
	const enabled = ref(false);
	const provider = ref<string>("none");
	/** 前端展示用的提供方文案（如「机器翻译 · 词表兜底」） */
	const label = ref("机器翻译");
	const loaded = ref(false);

	let inflight: Promise<void> | null = null;

	async function load() {
		try {
			const status = await translateApi.status();
			enabled.value = !!status.enabled;
			provider.value = status.provider;
			label.value = status.label || "机器翻译";
		} catch {
			// 翻译是增强能力：拿不到状态就当作未启用
			enabled.value = false;
		} finally {
			loaded.value = true;
		}
	}

	/** 惰性加载能力状态（幂等，多组件并发调用只发一次请求） */
	async function ensureLoaded() {
		if (loaded.value) return;
		if (!inflight) {
			inflight = load().finally(() => {
				inflight = null;
			});
		}
		await inflight;
	}

	return { enabled, provider, label, loaded, ensureLoaded };
});
