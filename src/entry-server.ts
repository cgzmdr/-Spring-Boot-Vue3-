import { renderToString } from "vue/server-renderer";
import { createApp } from "./app";

/**
 * 预渲染入口（Node 端）。
 *
 * 由 scripts/prerender.mjs 调用：给它一个 URL，它返回该路由的 HTML 片段。
 * 注意这里**不做数据预取**——
 * 所有页面的数据都在 onMounted 里通过 API 拉取，服务端渲染时不会执行，
 * 所以直出的是「骨架 + 静态文案 + 导航」，而不是完整内容。
 *
 * 这已经能解决主要问题：
 *   · 用户在 JS 到达前就能看到站点结构（导航 / 标题 / 页脚），不再是白屏；
 *   · 搜索引擎能看到真实文字与每个页面独立的 URL、title、description。
 *
 * 若要进一步直出数据，需要把 onMounted 的取数逻辑上移到
 * setup + useAsyncData 之类的模式，改动面较大，见文档「下一步」。
 */
export async function render(url: string): Promise<string> {
	const { app, router } = createApp();

	// 把路由推到目标地址。必须 await：懒加载的路由组件需要先解析完，
	// 否则 renderToString 会因为「组件是 Promise」而渲染出空壳。
	await router.push(url);
	await router.isReady();

	return renderToString(app);
}
