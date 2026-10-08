import { createRouter, createWebHistory, createMemoryHistory } from "vue-router";
import type { Router, RouteRecordRaw } from "vue-router";

/**
 * History 路由（而非 hash）。
 *
 * 为什么从 hash 换过来：
 * · hash 模式（`/#/ethnic`）下，`#` 之后的内容**不会发给服务器**，
 *   服务端永远只看到 `/`，无法针对具体页面做 SSR / 预渲染；
 * · 搜索引擎也只会收录一个 URL，子页面等于不存在；
 * · 换成 History 后每个页面有真实路径（`/ethnic`、`/about`），
 *   才能既做首屏直出，又让 SEO 逐个收录。
 *
 * 代价：需要 nginx 加 `try_files $uri $uri/ /index.html` 兜底，
 * 否则刷新子页面会 404（见 docs/performance-3g.md）。
 */

const routes: RouteRecordRaw[] = [
		{
			path: "/",
			name: "home",
			component: () => import("@/pages/home/index.vue"),
			meta: { title: "首页" },
		},
		{
			path: "/ethnic",
			name: "ethnic-list",
			component: () => import("@/pages/ethnic/index.vue"),
			meta: { title: "民族" },
		},
		{
			path: "/ethnic/languages",
			name: "ethnic-languages",
			component: () => import("@/pages/ethnic/languages.vue"),
			meta: { title: "民族语文" },
		},
		{
			path: "/culture/costume",
			name: "culture-costume",
			component: () => import("@/pages/culture/index.vue"),
			meta: { title: "民族服饰", topic: "costume" },
		},
		{
			path: "/culture/dwelling",
			name: "culture-dwelling",
			component: () => import("@/pages/culture/index.vue"),
			meta: { title: "民居建筑", topic: "dwelling" },
		},
		{
			path: "/ethnic/:id",
			name: "ethnic-detail",
			component: () => import("@/pages/ethnic/[id].vue"),
			meta: { title: "民族详情" },
		},
		{
			path: "/festival",
			name: "festival-list",
			component: () => import("@/pages/festival/index.vue"),
			meta: { title: "节日" },
		},
		{
			path: "/festival/calendar",
			name: "festival-calendar",
			component: () => import("@/pages/festival/calendar.vue"),
			meta: { title: "节日日历" },
		},
		{
			path: "/festival/:id",
			name: "festival-detail",
			component: () => import("@/pages/festival/[id].vue"),
			meta: { title: "节日详情" },
		},
		{
			path: "/custom/:id",
			name: "custom-detail",
			component: () => import("@/pages/custom/[id].vue"),
			meta: { title: "风俗详情" },
		},
		{
			path: "/food/:id",
			name: "food-detail",
			component: () => import("@/pages/food/[id].vue"),
			meta: { title: "美食详情" },
		},
		{
			path: "/art",
			name: "art-list",
			component: () => import("@/pages/art/index.vue"),
			meta: { title: "艺术" },
		},
		{
			path: "/heritage",
			name: "heritage-directory",
			component: () => import("@/pages/heritage/index.vue"),
			meta: { title: "非遗名录" },
		},
		{
			path: "/persons",
			name: "person-directory",
			component: () => import("@/pages/persons/index.vue"),
			meta: { title: "人物专栏" },
		},
		{
			path: "/autonomous",
			name: "autonomous-areas",
			component: () => import("@/pages/autonomous/index.vue"),
			meta: { title: "民族自治地方" },
		},
		{
			path: "/sports",
			name: "traditional-sports",
			component: () => import("@/pages/sports/index.vue"),
			meta: { title: "传统体育" },
		},
		{
			path: "/art/:id",
			name: "art-detail",
			component: () => import("@/pages/art/[id].vue"),
			meta: { title: "艺术详情" },
		},
		{
			path: "/discussion",
			name: "discussion-list",
			component: () => import("@/pages/discussion/index.vue"),
			meta: { title: "讨论区" },
		},
		{
			path: "/discussion/new",
			name: "discussion-new",
			component: () => import("@/pages/discussion/new.vue"),
			meta: { title: "发表新帖" },
		},
		{
			path: "/discussion/topic/:id",
			name: "discussion-topic",
			component: () => import("@/pages/discussion/[id].vue"),
			meta: { title: "帖子详情" },
		},
		{
			path: "/notifications",
			name: "notifications",
			component: () => import("@/pages/notifications/index.vue"),
			meta: { title: "通知" },
		},
		{
			path: "/me/community",
			name: "my-community",
			component: () => import("@/pages/me/community.vue"),
			meta: { title: "我的社区" },
		},
		{
			path: "/user/:id",
			name: "community-user",
			component: () => import("@/pages/user/[id].vue"),
			meta: { title: "用户主页" },
		},
		{
			path: "/messages",
			name: "messages",
			component: () => import("@/pages/messages/index.vue"),
			meta: { title: "私信" },
		},
		{
			path: "/messages/:id",
			name: "message-chat",
			component: () => import("@/pages/messages/[id].vue"),
			meta: { title: "私信会话" },
		},
		{
			path: "/search",
			name: "search",
			component: () => import("@/pages/search/index.vue"),
			meta: { title: "搜索" },
		},
		{
			// 兴趣与推荐（方向 D）：显式兴趣标签 + 个性化推荐
			path: "/interests",
			name: "interests",
			component: () => import("@/pages/interests/index.vue"),
			meta: { title: "兴趣与推荐" },
		},
		{
			path: "/unity",
			name: "unity",
			component: () => import("@/pages/unity/index.vue"),
			meta: { title: "民族团结" },
		},
		{
			path: "/topic/:id",
			name: "topic-detail",
			component: () => import("@/pages/topic/[id].vue"),
			meta: { title: "专题详情" },
		},
		{
			path: "/form/:id",
			name: "form-detail",
			component: () => import("@/pages/form/[id].vue"),
			meta: { title: "表单" },
		},
		{
			path: "/profile",
			name: "profile",
			component: () => import("@/pages/profile/index.vue"),
			meta: { title: "个人中心" },
		},
		{
			path: "/about",
			name: "about",
			component: () => import("@/pages/about/index.vue"),
			meta: { title: "关于" },
		},
	{
		path: "/:pathMatch(.*)*",
		name: "not-found",
		component: () => import("@/pages/error/404.vue"),
		meta: { title: "404" },
	},
];

/**
 * 每个请求都要一个**全新的** router 实例。
 *
 * SSR / 预渲染时若复用同一个实例，路由状态会在请求之间串味
 * （上一个 URL 残留、导航守卫重复注册）。客户端则只创建一次。
 *
 * history 实现必须按环境区分：`createWebHistory` 在创建时就会读取
 * `window.location` / `window.history`，在 Node 里直接抛
 * `window is not defined`；服务端要用 `createMemoryHistory`
 * （它把当前地址保存在内存里，正是预渲染「访问某个 URL」所需）。
 */
export function createAppRouter(): Router {
	const router = createRouter({
		history:
			typeof window !== "undefined"
				? createWebHistory(import.meta.env.BASE_URL)
				: createMemoryHistory(import.meta.env.BASE_URL),
		scrollBehavior(to, _from, savedPosition) {
			if (savedPosition) return savedPosition;
			if (!to.hash || typeof document === "undefined") return { top: 0 };

			/*
			 * 只是校验「这个锚点在页面上存不存在」，存在才滚动过去。
			 *
			 * 必须 try/catch：换成 History 路由后，`#` 仍是合法的 URL 片段，
			 * 但不再是路由载体，历史遗留的 `/#/ethnic` 这类链接会走到这里。
			 * `document.querySelector("#/ethnic")` 是**非法选择器**，
			 * 会直接抛 SyntaxError 并中断整个导航 —— 页面因此卡住不渲染。
			 * 另外 `#` 后也可能是不符合 CSS 标识符规则的字符（如数字开头）。
			 */
			let el: Element | null = null;
			try {
				el = document.querySelector(to.hash);
			} catch {
				el = null;
			}
			return el ? { el: to.hash, behavior: "smooth", top: 120 } : { top: 0 };
		},
		routes,
	});

	router.afterEach((to) => {
		if (typeof document === "undefined") return;
		const title = (to.meta.title as string) || "";
		document.title = title
			? `${title} · 走进多彩 56 个民族世界`
			: "走进多彩 56 个民族世界";
	});

	return router;
}

export default createAppRouter;
