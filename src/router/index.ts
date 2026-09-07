import { createRouter, createWebHashHistory } from "vue-router";

const router = createRouter({
	history: createWebHashHistory(),
	scrollBehavior(to, _from, savedPosition) {
		if (savedPosition) return savedPosition;
		if (to.hash) return { el: to.hash, behavior: "smooth", top: 120 };
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
			path: "/art",
			name: "art-list",
			component: () => import("@/pages/art/index.vue"),
			meta: { title: "艺术" },
		},
		{
			path: "/art/:id",
			name: "art-detail",
			component: () => import("@/pages/art/[id].vue"),
			meta: { title: "艺术详情" },
		},
		{
			path: "/search",
			name: "search",
			component: () => import("@/pages/search/index.vue"),
			meta: { title: "搜索" },
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
