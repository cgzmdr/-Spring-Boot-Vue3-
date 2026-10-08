import { createSSRApp } from "vue";
import { createPinia } from "pinia";
import type { App } from "vue";
import { ID_INJECTION_KEY, ZINDEX_INJECTION_KEY } from "element-plus";

import Root from "./App.vue";
import { createAppRouter } from "./router";
import { setupMotion } from "./plugins/motion";

import "./styles/common.css";
// Element Plus 函数式组件（ElMessage）样式按需引入
import "element-plus/es/components/message/style/css";
import "./styles/index.scss";

/**
 * Element Plus 的 SSR id 注入种子。
 *
 * Element Plus 内部用自增计数器生成组件 id（如 el-id-1-2）。
 * 服务端与客户端若各自从 0 开始计数、顺序又因渲染路径不同而不一致，
 * hydration 时 id 会对不上，Vue 报「hydration mismatch」并丢弃整棵子树重建
 * —— 预渲染的收益会被抹掉。
 * 固定一个 prefix 让两端从同样的起点、同样的规则生成 id。
 */
const EP_ID_PREFIX = 1024;

/**
 * 应用工厂：客户端与服务端（预渲染）共用同一套组装逻辑。
 *
 * 关键约束：**每个请求都要一份全新的 app / pinia / router 实例**。
 * 若复用单例，预渲染多个页面时状态会互相污染
 * （上一个页面的路由与 store 数据残留到下一个），
 * 产出的静态 HTML 会带着别的页面的内容。
 *
 * 注意用的是 `createSSRApp` 而不是 `createApp`：
 * 只有它才会在客户端「接管」服务端产出的 HTML（hydration），
 * 把复用已有 DOM 而不是全部重建。用错会导致整页重新渲染、
 * 白白浪费掉预渲染带来的首屏优势。
 */
export function createApp(): { app: App; router: ReturnType<typeof createAppRouter> } {
	const app = createSSRApp(Root);
	const router = createAppRouter();

	app.use(createPinia());
	app.use(router);

	// 两端使用同一套 id 生成规则，保证 hydration 对齐
	app.provide(ID_INJECTION_KEY, {
		prefix: EP_ID_PREFIX,
		current: 0,
	});

	/**
	 * z-index 注入。
	 *
	 * Element Plus 的弹层（popper / dialog / drawer）用自增计数器分配 z-index。
	 * 服务端渲染时若没有这个 provider，它同样会报警并在两端算出不同的初值，
	 * 造成 hydration 不一致。显式提供固定起点让两端一致。
	 */
	app.provide(ZINDEX_INJECTION_KEY, { current: 0 });

	// 全站动效（@vueuse/motion + @formkit/auto-animate）
	setupMotion(app);

	return { app, router };
}
