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

export function setupMotion(app: App) {
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
