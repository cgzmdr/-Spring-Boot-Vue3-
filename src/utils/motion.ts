import type { MotionVariants } from "@vueuse/motion";

/**
 * @vueuse/motion 变体工厂：把常用入场动效参数化，避免在页面里重复书写 variant 对象。
 * 配合 <div v-motion="fadeUp(120)"> 或 components/Reveal.vue 使用。
 */

const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1];

export interface FadeUpOptions {
	/** 延迟（毫秒），用于列表错峰入场 */
	delay?: number;
	/** 纵向位移（px） */
	distance?: number;
	/** 时长（毫秒） */
	duration?: number;
}

/** 上浮淡入（进入视口播放一次） */
export function fadeUp(options: FadeUpOptions = {}): MotionVariants<string> {
	const { delay = 0, distance = 22, duration = 620 } = options;
	return {
		initial: { opacity: 0, y: distance },
		visibleOnce: {
			opacity: 1,
			y: 0,
			transition: { delay, duration, ease: EASE },
		},
	};
}

/** 横向滑入淡出（左右分栏使用） */
export function slideIn(
	from: "left" | "right" = "left",
	options: FadeUpOptions = {},
): MotionVariants<string> {
	const { delay = 0, distance = 26, duration = 680 } = options;
	return {
		initial: { opacity: 0, x: from === "left" ? -distance : distance },
		visibleOnce: {
			opacity: 1,
			x: 0,
			transition: { delay, duration, ease: EASE },
		},
	};
}

/** 轻微放大淡入（图片 / 徽章） */
export function popIn(options: FadeUpOptions = {}): MotionVariants<string> {
	const { delay = 0, duration = 700 } = options;
	return {
		initial: { opacity: 0, scale: 0.96 },
		visibleOnce: {
			opacity: 1,
			scale: 1,
			transition: { delay, duration, ease: EASE },
		},
	};
}

/** 列表错峰：按序号返回延迟毫秒数（超过上限后不再叠加，避免末尾项等待过久） */
export function stagger(index: number, step = 60, max = 480) {
	return Math.min(index * step, max);
}
