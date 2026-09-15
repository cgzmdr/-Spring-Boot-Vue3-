import { defineStore } from "pinia";
import { ref } from "vue";
import { messageApi, notificationApi } from "@/api/modules";
import type { NotificationItem } from "@/api/types";
import { useAuthStore } from "@/stores/auth";

/**
 * 站内通知与私信角标：仅登录后轮询（默认 60 秒一次），页面不可见时暂停。
 */
export const useNotificationStore = defineStore("notification", () => {
	const unread = ref(0);
	/** 未读私信数 */
	const dmUnread = ref(0);
	const items = ref<NotificationItem[]>([]);
	const loading = ref(false);

	let timer: number | null = null;

	async function refresh() {
		const auth = useAuthStore();
		if (!auth.isLoggedIn) {
			unread.value = 0;
			dmUnread.value = 0;
			return;
		}
		try {
			unread.value = await notificationApi.unreadCount();
		} catch {
			/* 静默失败：角标不是关键路径 */
		}
		try {
			dmUnread.value = await messageApi.unreadCount();
		} catch {
			/* 静默失败 */
		}
	}

	async function loadList(unreadOnly = false, page = 0, size = 20) {
		loading.value = true;
		try {
			const res = await notificationApi.list({ unreadOnly, page, size });
			items.value = res.data;
			return res;
		} finally {
			loading.value = false;
		}
	}

	async function markAllRead() {
		await notificationApi.markRead([]);
		unread.value = 0;
		items.value = items.value.map((n) => ({ ...n, read: true }));
	}

	/** 启动轮询（登录后调用；重复调用会重置计时器） */
	function startPolling(intervalMs = 60_000) {
		stopPolling();
		refresh();
		timer = window.setInterval(() => {
			if (document.visibilityState === "visible") refresh();
		}, intervalMs);
	}

	function stopPolling() {
		if (timer !== null) {
			window.clearInterval(timer);
			timer = null;
		}
	}

	return { unread, dmUnread, items, loading, refresh, loadList, markAllRead, startPolling, stopPolling };
});
