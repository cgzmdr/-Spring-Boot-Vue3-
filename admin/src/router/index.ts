import {
	createRouter,
	createWebHashHistory,
	type RouteRecordRaw,
} from "vue-router";
import DefaultLayout from "@/layouts/DefaultLayout.vue";

const routes: RouteRecordRaw[] = [
	{
		path: "/login",
		name: "Login",
		component: () => import("@/pages/login/index.vue"),
		meta: { title: "登录", requiresAuth: false },
	},
	{
		path: "/",
		component: DefaultLayout,
		meta: { requiresAuth: true },
		redirect: "/dashboard",
		children: [
			{
				path: "dashboard",
				name: "Dashboard",
				component: () => import("@/pages/dashboard/index.vue"),
				meta: { title: "仪表盘" },
			},
			{
				path: "ethnic",
				name: "Ethnic",
				component: () => import("@/pages/ethnic/index.vue"),
				meta: { title: "民族管理" },
			},
			{
				path: "ethnic/edit",
				name: "EthnicEdit",
				component: () => import("@/pages/ethnic/Edit.vue"),
				meta: { title: "编辑民族", hidden: true },
			},
			{
				path: "festival",
				name: "Festival",
				component: () => import("@/pages/festival/index.vue"),
				meta: { title: "节日管理" },
			},
			{
				path: "festival/edit",
				name: "FestivalEdit",
				component: () => import("@/pages/festival/Edit.vue"),
				meta: { title: "编辑节日", hidden: true },
			},
			{
				path: "art",
				name: "Art",
				component: () => import("@/pages/art/index.vue"),
				meta: { title: "艺术管理" },
			},
			{
				path: "art/edit",
				name: "ArtEdit",
				component: () => import("@/pages/art/Edit.vue"),
				meta: { title: "编辑艺术", hidden: true },
			},
			{
				path: "topic",
				name: "Topic",
				component: () => import("@/pages/topic/index.vue"),
				meta: { title: "专题管理" },
			},
			{
				path: "topic/edit",
				name: "TopicEdit",
				component: () => import("@/pages/topic/Edit.vue"),
				meta: { title: "编辑专题", hidden: true },
			},
			{
				path: "form",
				name: "Form",
				component: () => import("@/pages/form/index.vue"),
				meta: { title: "表单配置" },
			},
			{
				path: "form/edit",
				name: "FormEdit",
				component: () => import("@/pages/form/Edit.vue"),
				meta: { title: "编辑表单", hidden: true },
			},
			{
				path: "review",
				name: "Review",
				component: () => import("@/pages/review/index.vue"),
				meta: { title: "审核管理" },
			},
			{
				path: "user",
				name: "User",
				component: () => import("@/pages/user/index.vue"),
				meta: { title: "用户管理" },
			},
			{
				path: "role",
				name: "Role",
				component: () => import("@/pages/role/index.vue"),
				meta: { title: "角色管理" },
			},
		],
	},
	{
		path: "/:pathMatch(.*)*",
		name: "NotFound",
		component: () => import("@/pages/error/404.vue"),
		meta: { title: "404", requiresAuth: false },
	},
];

const router = createRouter({
	history: createWebHashHistory(),
	routes,
});

export default router;
