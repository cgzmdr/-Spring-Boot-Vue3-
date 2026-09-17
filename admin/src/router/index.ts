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
				// 我的待办：内容审批工作流（Camunda 8）的统一入口
				path: "todo",
				name: "Todo",
				component: () => import("@/pages/todo/index.vue"),
				meta: { title: "我的待办" },
			},
			{
				path: "todo/detail",
				name: "TodoDetail",
				component: () => import("@/pages/todo/detail.vue"),
				meta: { title: "审核处理", hidden: true },
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
				path: "person",
				name: "Person",
				component: () => import("@/pages/person/index.vue"),
				meta: { title: "人物档案" },
			},
			{
				path: "person/edit",
				name: "PersonEdit",
				component: () => import("@/pages/person/Edit.vue"),
				meta: { title: "编辑人物档案", hidden: true },
			},
			{
				path: "area",
				name: "Area",
				component: () => import("@/pages/area/index.vue"),
				meta: { title: "自治地方" },
			},
			{
				path: "area/edit",
				name: "AreaEdit",
				component: () => import("@/pages/area/Edit.vue"),
				meta: { title: "编辑自治地方", hidden: true },
			},
			{
				path: "sport",
				name: "Sport",
				component: () => import("@/pages/sport/index.vue"),
				meta: { title: "传统体育" },
			},
			{
				path: "sport/edit",
				name: "SportEdit",
				component: () => import("@/pages/sport/Edit.vue"),
				meta: { title: "编辑传统体育", hidden: true },
			},
			{
				path: "source",
				name: "Source",
				component: () => import("@/pages/source/index.vue"),
				meta: { title: "内容来源" },
			},
			{
				path: "source/edit",
				name: "SourceEdit",
				component: () => import("@/pages/source/Edit.vue"),
				meta: { title: "编辑内容来源", hidden: true },
			},
			{
				path: "credit",
				name: "Credit",
				component: () => import("@/pages/credit/index.vue"),
				meta: { title: "图片署名" },
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
				meta: { title: "内容审核" },
			},
			{
				// 网页版 Camunda Modeler：BPMN 流程建模 + Camunda Form 设计
				path: "modeler",
				name: "Modeler",
				component: () => import("@/pages/modeler/index.vue"),
				meta: { title: "Camunda Modeler" },
			},
			{
				path: "discussion",
				name: "Discussion",
				component: () => import("@/pages/discussion/index.vue"),
				meta: { title: "讨论区治理" },
			},
			{
				path: "translate",
				name: "Translate",
				component: () => import("@/pages/translate/index.vue"),
				meta: { title: "翻译词表" },
			},
			{
				// 检索索引与兴趣标签（方向 D）
				path: "search-index",
				name: "SearchIndex",
				component: () => import("@/pages/search-index/index.vue"),
				meta: { title: "检索与推荐" },
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
] as const;

const router = createRouter({
	history: createWebHashHistory(),
	routes,
});

export default router;
