import { createRouter, createWebHashHistory } from "vue-router";

const router = createRouter({
	history: createWebHashHistory(),
	scrollBehavior(to, _from, savedPosition) {
		if (savedPosition) return savedPosition;
		// Tab 锚点（如民族详情 #history / #customs）并非真实 DOM 元素，
		// 由页面自身切换到对应 Tab；此时回到顶部，避免无效选择器告警。
		if (to.hash && document.querySelector(to.hash)) {
			return { el: to.hash, behavior: "smooth", top: 120 };
		}
		return { top: 0 };
	},
	routes: [
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
	],
});

router.afterEach((to) => {
	const title = (to.meta.title as string) || "";
	document.title = title
		? `${title} · 走进多彩 56 个民族世界`
		: "走进多彩 56 个民族世界";
});

export default router;
