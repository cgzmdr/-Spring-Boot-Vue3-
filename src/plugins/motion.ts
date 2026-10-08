import type { App } from "vue";
import { MotionPlugin } from "@vueuse/motion";
import type { Easing } from "@vueuse/motion";
import { autoAnimatePlugin } from "@formkit/auto-animate/vue";

/**
 * 全站动效统一入口（第三方动画库）
 *
 * - @vueuse/motion：声明式入场/滚动可见动效
 *   · v-motion-fade-up / v-motion-fade-in / v-motion-pop-in 预设指令（无参数场景，最精简）
 *   · v-motion  + 对象参数（需要延迟/位移等自定义场景，见 components/Reveal.vue）
 * - @formkit/auto-animate：v-auto-animate 指令，列表增删与 Tab 切换自动补间
 *
 * 已尊重系统「减少动态效果」偏好：prefers-reduced-motion 时全局关闭动效（见 common.css）。
 */
const EASE_OUT: Easing = [0.22, 1, 0.36, 1];

/**
 * SSR（预渲染）阶段用的空指令。
 *
 * 为什么不能「干脆不注册」：Vue 的 SSR 渲染器遇到模板里出现、
 * 但服务端未注册的自定义指令时，会去读 `directive.getSSRProps`，
 * 而 directive 是 undefined —— 直接抛
 * `TypeError: Cannot read properties of undefined (reading 'getSSRProps')`，
 * 整页预渲染失败。
 *
 * 为什么也不注册真实的 MotionPlugin：它依赖 IntersectionObserver / window，
 * Node 里没有；而且入场动效的初始态是 `opacity: 0`，
 * 若真渲染进 HTML，预渲染页面会以「全透明」直出 ——
 * 用户在 JS 迟迟不执行时会看到一片空白，比不做预渲染还糟。
 *
 * 所以这里提供「无 getSSRProps 的空指令」：元素以最终可见态直出，
 * 客户端接管后再由真实指令播动画。
 */
const ssrNoopDirective = {};

const SSR_DIRECTIVES: Record<string, object> = {
	"motion": ssrNoopDirective,
	"motion-fade-up": ssrNoopDirective,
	"motion-fade-in": ssrNoopDirective,
	"motion-from-left": ssrNoopDirective,
	"motion-from-right": ssrNoopDirective,
	"motion-pop-in": ssrNoopDirective,
	// v-auto-animate：列表增删补间，服务端无意义
	"auto-animate": ssrNoopDirective,
};

/**
 * 注册全站动效。
 *
 * 浏览器端挂真实的 MotionPlugin / autoAnimate；
 * 服务端只注册上面的空指令，保证预渲染能顺利产出「可见」的内容。
 */
export function setupMotion(app: App) {
	if (typeof window === "undefined") {
		for (const [name, dir] of Object.entries(SSR_DIRECTIVES)) {
			app.directive(name, dir);
		}
		return;
	}

	app.use(MotionPlugin, {
		directives: {
			/** 上浮淡入（滚动进入视口一次） */
			"fade-up": {
				initial: { opacity: 0, y: 22 },
				visibleOnce: {
					opacity: 1,
					y: 0,
					transition: { duration: 620, ease: EASE_OUT },
				},
			},
			/** 纯淡入（大标题 / 图片） */
			"fade-in": {
				initial: { opacity: 0 },
				visibleOnce: { opacity: 1, transition: { duration: 760, ease: EASE_OUT } },
			},
			/** 左侧滑入（正文栏） */
			"from-left": {
				initial: { opacity: 0, x: -26 },
				visibleOnce: {
					opacity: 1,
					x: 0,
					transition: { duration: 680, ease: EASE_OUT },
				},
			},
			/** 右侧滑入（侧栏） */
			"from-right": {
				initial: { opacity: 0, x: 26 },
				visibleOnce: {
					opacity: 1,
					x: 0,
					transition: { duration: 680, ease: EASE_OUT },
				},
			},
			/** 轻微放大淡入（图片、徽章） */
			"pop-in": {
				initial: { opacity: 0, scale: 0.96 },
				visibleOnce: {
					opacity: 1,
					scale: 1,
					transition: { duration: 700, ease: EASE_OUT },
				},
			},
		},
	});

	/** v-auto-animate：容器内子节点增删 / 排序 / Tab 切换自动补间 */
	app.use(autoAnimatePlugin);
}
