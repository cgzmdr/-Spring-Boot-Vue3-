<script setup lang="ts">
import {
	ref,
	computed,
	watchEffect,
	watch,
	onMounted,
	onUnmounted,
	defineAsyncComponent,
	shallowRef,
	nextTick,
} from "vue";
import type { Ref } from "vue";
import { ArrowRight } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import { useWindowScroll, useElementSize } from "@vueuse/core";
import { useLangStore } from "@/stores/lang";
import { useAuthStore } from "@/stores/auth";
import { useNotificationStore } from "@/stores/notification";
import { resolveStaticUrl } from "@/utils/format";

/**
 * 首屏按需加载（Element Plus 瘦身）
 *
 * 报头是每个页面的首屏组件，但它身上挂着好几个「只有用户点了才会出现」的
 * Element Plus 组件：
 *   · 登录弹窗 —— el-dialog / el-form / el-form-item / el-input / el-button
 *   · 移动端菜单抽屉 —— el-drawer（内含 overlay / focus-trap / 滚动锁）
 *
 * 这些组件的样式与逻辑此前会随入口一起下载解析，纯属浪费：
 * 3G 用户还没看到页面，就先为一个可能永远不会打开的弹窗付了流量。
 * 改为 async 组件后，它们被拆成独立 chunk，只在真正需要时才拉取。
 *
 * 注意：`defineAsyncComponent` 的加载器必须写成动态 import，
 * 否则打包器仍会把它并入首屏 chunk，优化失效。
 */
const LoginDialog = defineAsyncComponent(() => import("./LoginDialog.vue"));
const MobileNavDrawer = defineAsyncComponent(() => import("./MobileNavDrawer.vue"));

const route = useRoute();
const router = useRouter();
const lang = useLangStore();
const auth = useAuthStore();

/** 是否已经/需要渲染异步组件：挂载后再置位，避免异步 chunk 拖慢首屏 */
const loginDialogMounted = ref(false);
const drawerMounted = ref(false);

const loginDialog = shallowRef<{
	open: () => void;
} | null>(null);
const drawerOpen = ref(false);

const mastEl = ref<HTMLElement | null>(null);
const topBarEl = ref<HTMLElement | null>(null);
const titleInnerEl = ref<HTMLElement | null>(null);
const navEl = ref<HTMLElement | null>(null);
const burgerEl = ref<HTMLElement | null>(null);

/**
 * 收缩阈值采用「双阈值 + 迟滞」：向下超过 110 收缩，向上退回 50 以内才展开，
 * 避免在临界点附近反复切换。
 */
const ENTER_AT = 110;
const EXIT_AT = 50;
const compact = ref(false);

/**
 * 是否处于浏览器环境。
 *
 * 报头依赖 useWindowScroll / useElementSize 等 @vueuse/core 组合式函数，
 * 它们在**调用时**就会访问 window / IntersectionObserver，
 * 在 Node（SSR / 预渲染）里直接抛 `window is not defined`。
 * 因此服务端一律退化为静态值：报头以「展开态」直出，
 * 客户端接管后再挂上真实监听。
 */
const isBrowser = typeof window !== "undefined";

const scrollY = isBrowser
	? useWindowScroll().y
	: ref(0);

/**
 * 报头为固定定位（脱离文档流），正文用 body 的 padding-top 避让。
 * 占位高度 = 报头内各区块（顶栏 / 标题块 / 导航条 / 移动端菜单按钮）之和，
 * 一律按 border-box 量测，且与收缩状态无关：标题块量的是内层容器
 * （外层 max-height: 0 只裁剪显示，不影响内层自然高度；display:none 的区块量为 0）。
 * 因此收缩时文档不会重排，也就不会触发浏览器滚动锚定补偿
 * —— 这正是临界值处抖动、跳变的根因。
 * 注：报头若新增区块，需同步在此追加量测。
 */
const BORDER_BOX = { box: "border-box" } as const;
/**
 * 量测辅助：服务端没有布局，直接返回常量 0 的 ref。
 * 用同一个工厂保证调用顺序稳定（组合式函数不能在条件分支里跳过，
 * 否则客户端 hydration 时的 hook 顺序会与服务端不一致）。
 */
function useSize(el: Ref<HTMLElement | null>) {
	return isBrowser
		? useElementSize(el, { width: 0, height: 0 }, BORDER_BOX).height
		: ref(0);
}

const mastHeight = useSize(mastEl);
const topHeight = useSize(topBarEl);
const titleHeight = useSize(titleInnerEl);
const navHeight = useSize(navEl);
const burgerHeight = useSize(burgerEl);

watchEffect(() => {
	if (!isBrowser) return;
	const root = document.documentElement;
	// 当前实际高度：供吸顶 Tab / 侧栏偏移使用
	const current = Math.round(mastHeight.value);
	if (current > 0) root.style.setProperty("--mast-h", `${current}px`);
	// 文档占位高度：展开态高度（不含报头下边框，边框由 CSS 变量计入），恒定不变
	const expanded = Math.round(
		topHeight.value + titleHeight.value + navHeight.value + burgerHeight.value,
	);
	if (expanded > 0) root.style.setProperty("--mast-pad", `${expanded}px`);
});

watchEffect(() => {
	const y = scrollY.value;
	if (!compact.value && y > ENTER_AT) compact.value = true;
	else if (compact.value && y < EXIT_AT) compact.value = false;
});

/** 阅读进度（0~1）：显示在报头下沿的细进度条 */
const progress = computed(() => {
	if (typeof document === "undefined") return 0;
	const el = document.documentElement;
	const total = el.scrollHeight - el.clientHeight;
	return total > 0 ? Math.min(1, Math.max(0, scrollY.value / total)) : 0;
});

/**
 * 主导航（两级结构）
 *
 * 信息架构原则：
 * · **一级只放 6 个栏目** —— 首页 / 民族 / 文化 / 节日 / 社区 / 关于，
 *   数量克制，保证在任何视口下都能一屏排完、不需折行；
 * · **二级用下拉** —— 把原先平铺的 14 项收进各一级栏目之下；
 * · **三级用页内锚点**（如民族详情的 `#festivals`）—— 不占导航空间。
 *
 * 一级栏目本身尽量可点（民族 → /ethnic、文化 → /heritage、节日 → /festival），
 * 下拉只是「该栏目下的更多入口」，而不是把一级变成纯分类标签。
 */
interface NavChild {
	label: string;
	to: string;
	desc?: string;
}
interface NavGroup {
	key: string;
	label: string;
	/** 一级栏目自身的落地页 */
	to: string;
	children?: NavChild[];
}

const navGroups = computed<NavGroup[]>(() => [
	{
		key: "home",
		label: lang.t("nav_home"),
		to: "/",
	},
	{
		key: "ethnic",
		label: lang.t("nav_ethnic"),
		to: "/ethnic",
		children: [
			{ label: lang.t("nav_ethnic_all"), to: "/ethnic", desc: lang.t("nav_ethnic_all_desc") },
			{ label: lang.t("nav_languages"), to: "/ethnic/languages", desc: lang.t("nav_languages_desc") },
			{ label: lang.t("nav_costume"), to: "/culture/costume", desc: lang.t("nav_costume_desc") },
			{ label: lang.t("nav_dwelling"), to: "/culture/dwelling", desc: lang.t("nav_dwelling_desc") },
			{ label: lang.t("nav_map"), to: "/ethnic?view=map", desc: lang.t("nav_map_desc") },
			{ label: lang.t("nav_autonomous"), to: "/autonomous", desc: lang.t("nav_autonomous_desc") },
		],
	},
	{
		key: "culture",
		label: lang.t("nav_culture"),
		to: "/heritage",
		children: [
			{ label: lang.t("nav_art"), to: "/art", desc: lang.t("nav_art_desc") },
			{ label: lang.t("nav_heritage"), to: "/heritage", desc: lang.t("nav_heritage_desc") },
			{ label: lang.t("nav_persons"), to: "/persons", desc: lang.t("nav_persons_desc") },
			{ label: lang.t("nav_sports"), to: "/sports", desc: lang.t("nav_sports_desc") },
		],
	},
	{
		key: "festival",
		label: lang.t("nav_festival"),
		to: "/festival",
		children: [
			{ label: lang.t("nav_festival_all"), to: "/festival", desc: lang.t("nav_festival_all_desc") },
			{ label: lang.t("nav_calendar"), to: "/festival/calendar", desc: lang.t("nav_calendar_desc") },
		],
	},
	{
		key: "community",
		label: lang.t("nav_discussion"),
		to: "/discussion",
	},
	{
		// 兴趣与个性化推荐（方向 D）
		key: "interests",
		label: lang.t("nav_interests"),
		to: "/interests",
		desc: lang.t("nav_interests_desc"),
	},
	{
		key: "about",
		label: lang.t("nav_about"),
		to: "/about",
	},
]);

/** 当前路由是否落在某个一级栏目下（含其所有二级路径） */
function groupActive(g: NavGroup): boolean {
	if (g.to === "/") return route.path === "/";
	const paths = [g.to, ...(g.children || []).map((c) => c.to)];
	return paths.some((p) => {
		const base = p.split("?")[0];
		if (base === "/") return route.path === "/";
		// 精确匹配，或作为前缀匹配（如 /ethnic/xxx 命中 /ethnic）
		return route.path === base || route.path.startsWith(base + "/");
	});
}

/** 二级条目是否处于当前路由 */
function childActive(c: NavChild): boolean {
	const base = c.to.split("?")[0];
	return route.path === base || route.path.startsWith(base + "/");
}

/** 当前展开的下拉（hover 或点击触发） */
const openGroup = ref<string | null>(null);
let closeTimer: ReturnType<typeof setTimeout> | null = null;

function hoverOpen(key: string) {
	if (closeTimer) {
		clearTimeout(closeTimer);
		closeTimer = null;
	}
	openGroup.value = key;
}

function hoverClose() {
	// 延迟关闭，避免鼠标在下拉与触发项之间移动时闪烁
	if (closeTimer) clearTimeout(closeTimer);
	closeTimer = setTimeout(() => {
		openGroup.value = null;
	}, 160);
}

/** 路由变化时收起下拉 */
watch(
	() => route.fullPath,
	() => {
		openGroup.value = null;
	},
);

onUnmounted(() => {
	if (closeTimer) clearTimeout(closeTimer);
});

function goSearch() {
	router.push("/search");
}

/**
 * 打开登录弹窗。
 *
 * 弹窗是异步组件，首次调用时它还没挂载、chunk 也还没下载，
 * 因此不能简单 `await nextTick()` 后调 open()——那时 ref 仍是 null，
 * 表现就是「点了登录没反应」（这正是本组件引入过的真实缺陷）。
 *
 * 正确做法：把「打开」意图记在 pendingOpen 上，等组件真正挂载
 * （@vue:mounted / onMounted 后再 nextTick）再消费它。
 */
const pendingLoginOpen = ref(false);

function openLogin() {
	if (!loginDialogMounted.value) {
		// 触发挂载：浏览器开始拉取 chunk，弹窗渲染完成后由 onLoginDialogReady 接手
		pendingLoginOpen.value = true;
		loginDialogMounted.value = true;
		return;
	}
	loginDialog.value?.open();
}

/** 异步弹窗挂载完成：消费挂起的打开意图 */
async function onLoginDialogReady() {
	if (!pendingLoginOpen.value) return;
	pendingLoginOpen.value = false;
	await nextTick();
	loginDialog.value?.open();
}

/** 打开移动端菜单抽屉（同样按需挂载） */
function openDrawer() {
	drawerMounted.value = true;
	drawerOpen.value = true;
}

const userInitial = computed(() =>
	auth.user?.nickname ? auth.user.nickname.trim().slice(0, 1) : "",
);
const userAvatar = computed(() => resolveStaticUrl(auth.user?.avatar));

/** 站内通知角标：登录后启动轮询，退出登录即停止 */
const notice = useNotificationStore();
watch(
	() => auth.isLoggedIn,
	(logged) => {
		if (logged) notice.startPolling();
		else notice.stopPolling();
	},
	{ immediate: true },
);
onUnmounted(() => notice.stopPolling());

/**
 * 全局「需要登录」事件（由 api/request.ts 在未登录/登录失效时派发）。
 *
 * 注意：这段监听原先在 LoginDialog 内部。弹窗改成按需加载后，
 * 组件未挂载时就没人接这个事件——必须先由报头（始终存在）兜住，
 * 再按需把弹窗拉起来，否则未登录用户触发受限操作时会「毫无反应」。
 */
function onAuthRequired() {
	openLogin();
}
onMounted(() => window.addEventListener("auth:required", onAuthRequired));
onUnmounted(() => window.removeEventListener("auth:required", onAuthRequired));
</script>

<template>
	<nav
		ref="mastEl"
		class="mast"
		:class="{ compact }"
	>
		<div class="container">
			<div
				ref="topBarEl"
				class="mast-top"
			>
				<span>{{ lang.t("site_slogan") }}</span>
				<div
					style="width: fit-content; margin: 0 auto 0 1rem"
					v-if="route.name !== 'home'"
				>
					<el-link
						type="danger"
						:icon="ArrowRight"
						@click="
							() => {
								router.back();
							}
						"
						>{{ lang.t("back_prev") }}</el-link
					>
				</div>
				<div class="links">
					<router-link
						v-if="auth.isLoggedIn"
						to="/messages"
						class="bell"
						:title="lang.pick('私信', 'Messages')"
					>
						<svg
							width="15"
							height="15"
							viewBox="0 0 24 24"
							fill="none"
							stroke="currentColor"
							stroke-width="1.8"
						>
							<path
								d="M21 12a8 8 0 0 1-8 8H7l-4 3V12a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8Z"
								stroke-linecap="round"
								stroke-linejoin="round"
							/>
						</svg>
						<span
							v-if="notice.dmUnread"
							class="badge"
							>{{ notice.dmUnread > 99 ? "99+" : notice.dmUnread }}</span
						>
					</router-link>
					<router-link
						v-if="auth.isLoggedIn"
						to="/notifications"
						class="bell"
						:title="lang.pick('通知', 'Notifications')"
					>
						<svg
							width="15"
							height="15"
							viewBox="0 0 24 24"
							fill="none"
							stroke="currentColor"
							stroke-width="1.8"
						>
							<path
								d="M18 8a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0"
								stroke-linecap="round"
								stroke-linejoin="round"
							/>
						</svg>
						<span
							v-if="notice.unread"
							class="badge"
							>{{ notice.unread > 99 ? "99+" : notice.unread }}</span
						>
					</router-link>
					<span v-if="auth.isLoggedIn">·</span>
					<a @click="goSearch">{{ lang.t("search") }}</a>
					<span>·</span>
					<button
						class="lang-toggle"
						@click="lang.toggle()"
					>
						{{ lang.isEn ? "中" : "EN" }}
					</button>
					<template v-if="auth.isLoggedIn">
						<span>·</span>
						<router-link
							to="/profile"
							class="user-chip"
							:title="auth.user?.nickname || '个人中心'"
						>
							<img
								v-if="userAvatar"
								:src="userAvatar"
								class="chip-avatar"
								alt=""
							/>
							<template v-else>{{ userInitial }}</template>
						</router-link>
					</template>
					<template v-else>
						<span>·</span>
						<a @click="openLogin">{{ lang.t("login") }}</a>
					</template>
				</div>
			</div>

			<div class="mast-title">
				<div
					ref="titleInnerEl"
					class="mast-title-inner"
				>
					<h1 @click="router.push('/')">走进多彩 56 民族世界</h1>
					<div class="sub">{{ lang.t("site_sub") }}</div>
				</div>
			</div>

			<div
				ref="navEl"
				class="mast-nav"
			>
				<div
					v-for="g in navGroups"
					:key="g.key"
					class="nav-item"
					:class="{ 'has-children': !!g.children?.length, open: openGroup === g.key }"
					@mouseenter="g.children?.length && hoverOpen(g.key)"
					@mouseleave="g.children?.length && hoverClose()"
				>
					<router-link
						class="nav-link"
						:class="{ active: groupActive(g) }"
						:to="g.to"
					>
						{{ g.label }}
						<span
							v-if="g.children?.length"
							class="nav-caret"
							aria-hidden="true"
						/>
					</router-link>

					<!-- 二级下拉 -->
					<div
						v-if="g.children?.length"
						class="nav-drop"
					>
						<router-link
							v-for="c in g.children"
							:key="c.to"
							class="drop-item"
							:class="{ active: childActive(c) }"
							:to="c.to"
						>
							<span class="di-label">{{ c.label }}</span>
							<span
								v-if="c.desc"
								class="di-desc"
								>{{ c.desc }}</span
							>
						</router-link>
					</div>
				</div>
			</div>

			<div
				ref="burgerEl"
				class="mast-burger"
				@click="openDrawer"
			>
				<svg
					width="20"
					height="20"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="1.8"
				>
					<path
						d="M4 7h16M4 12h16M4 17h16"
						stroke-linecap="round"
					/>
				</svg>
				<span>菜单</span>
			</div>
		</div>

		<!-- 阅读进度条 -->
		<div
			class="mast-progress"
			:style="{ width: `${progress * 100}%` }"
			aria-hidden="true"
		/>
	</nav>

	<!--
		移动端导航抽屉：按需加载。
		仅在窄视口点击「菜单」时才挂载并拉取 chunk（el-drawer 及配套逻辑），
		桌面端用户全程不会下载它。
	-->
	<MobileNavDrawer
		v-if="drawerMounted"
		v-model="drawerOpen"
		:groups="navGroups"
		:current-path="route.path"
	/>

	<!--
		登录弹窗：按需加载。
		由「登录」、「注册」或 auth:required 事件触发；
		弹窗挂载完成后通过 @vue:mounted 通知父组件消费挂起的打开意图。
	-->
	<LoginDialog
		v-if="loginDialogMounted"
		ref="loginDialog"
		@vue:mounted="onLoginDialogReady"
	/>
</template>

<style scoped>
.user-chip {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 22px;
	height: 22px;
	border-radius: 50%;
	background: var(--ink);
	color: #fff;
	font-size: 12px;
	cursor: pointer;
	text-transform: uppercase;
	text-decoration: none;
	overflow: hidden;
}
.chip-avatar {
	width: 100%;
	height: 100%;
	object-fit: cover;
	display: block;
}
.mast-top .links a {
	cursor: pointer;
}
/* 通知铃铛与未读角标 */
.bell {
	position: relative;
	display: inline-flex;
	align-items: center;
	color: var(--muted);
	cursor: pointer;
}
.bell:hover {
	color: var(--accent);
}
.bell .badge {
	position: absolute;
	top: -7px;
	left: 9px;
	min-width: 15px;
	height: 15px;
	padding: 0 3px;
	border-radius: 8px;
	background: var(--accent);
	color: #fff;
	font-size: 10px;
	line-height: 15px;
	text-align: center;
	letter-spacing: 0;
}
</style>
