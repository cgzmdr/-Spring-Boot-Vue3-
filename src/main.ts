import { createApp } from "./app";

/**
 * 兼容旧版 hash 链接。
 *
 * 本站此前用 hash 路由，站外可能已经存在 `/#/ethnic`、`/#/about` 这类链接
 * （用户收藏、外部转载、搜索引擎旧索引）。换到 History 路由后，
 * 这些链接的 `#/ethnic` 会被当成**页内锚点**：既匹配不到任何路由，
 * 又因为 `querySelector('#/ethnic')` 不是合法选择器而可能抛错。
 *
 * 这里在创建应用之前把 `#/xxx` 就地改写成 `/xxx`，
 * 让旧链接平滑落到新地址上，而不是 404 或白屏。
 */
function migrateLegacyHashUrl() {
	const { hash, pathname, search } = window.location;
	// 只处理 `#/` 开头的旧路由形态，普通的页内锚点（如 `#history`）不动
	if (!hash.startsWith("#/")) return;
	// 首页 + 旧 hash 时直接清掉 hash，避免多一次历史记录
	const legacyPath = hash.slice(1);
	if (pathname === "/" && legacyPath === "/") {
		window.history.replaceState(null, "", "/" + search);
		return;
	}
	window.history.replaceState(null, "", legacyPath + search);
}

migrateLegacyHashUrl();

const { app } = createApp();

// 应用持久化的页面字号设置（在挂载前生效，避免闪烁）
import { useSettingsStore } from "./stores/settings";
useSettingsStore().apply();

app.mount("#app");

/**
 * 首屏骨架退场。
 *
 * index.html 内联了一段关键 CSS + 静态骨架（见该文件的 #boot-skeleton）。
 * 预渲染页面在 body 里已有真实内容，骨架默认就不显示（由 CSS 控制）；
 * 纯 SPA 页面才需要它。这里统一做一次清理，两种情况都安全。
 */
function dismissBootSkeleton() {
	const el = document.getElementById("boot-skeleton");
	if (!el) return;
	el.classList.add("is-gone");
	window.setTimeout(() => el.remove(), 320);
}

/**
 * 关键：把「静默恢复登录态」移出首屏渲染路径。
 *
 * 原实现是在 app.mount() 之后立即调用 useAuthStore().restore()。
 * 它会发一个 me 请求，看似不阻塞渲染，但在慢网下这个请求会和
 * 首屏图片争抢同一条 3G 链路（浏览器对同源连接有并发上限），
 * 把封面图的下载推后数秒；同时失败链路还会触发登出。
 * 这里改为「先让页面完整可见，再在空闲时恢复登录态」。
 */
function scheduleAuthRestore() {
	const run = () => {
		import("./stores/auth").then(({ useAuthStore }) => {
			useAuthStore()
				.restore()
				.catch(() => {
					/* 静默失败：未登录也能正常浏览 */
				});
		});
	};

	dismissBootSkeleton();

	// 首屏图片/字体仍在下载时不要把带宽分给后台请求
	const w = window as Window & {
		requestIdleCallback?: (cb: () => void, opts?: { timeout: number }) => number;
	};
	if (typeof w.requestIdleCallback === "function") {
		w.requestIdleCallback(run, { timeout: 3000 });
	} else {
		window.setTimeout(run, 1200);
	}
}

scheduleAuthRestore();
