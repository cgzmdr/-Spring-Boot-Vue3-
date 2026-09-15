<script setup lang="ts">
import { computed } from "vue";
import { useReducedMotion } from "@vueuse/motion";
import type { MotionVariants } from "@vueuse/motion";
import { fadeUp, slideIn, popIn } from "@/utils/motion";

/**
 * 滚动入场包装组件：把 @vueuse/motion 的 v-motion 用法收敛为 props，
 * 页面里只需 <Reveal :delay="120">…</Reveal>，无需重复书写 variant 对象。
 */
const props = withDefaults(
	defineProps<{
		/** 动画延迟（毫秒），用于列表错峰 */
		delay?: number;
		/** 入场纵向位移（px） */
		y?: number;
		/** 入场横向位移（px）：正数自右滑入，负数自左滑入 */
		x?: number;
		/** 缩放起始值（1 表示不缩放） */
		scale?: number;
		/** 动画时长（毫秒） */
		duration?: number;
		/** 只在首次进入视口播放（false 则每次进入都播放） */
		once?: boolean;
		/** 渲染标签名 */
		tag?: string;
		/** 包裹层是否撑满高度（分栏布局用） */
		fill?: boolean;
	}>(),
	{
		delay: 0,
		y: 20,
		x: 0,
		scale: 1,
		duration: 620,
		once: true,
		tag: "div",
		fill: false,
	},
);

/** 系统「减少动态效果」偏好：开启时直接显示终态，不做位移 */
const reduced = useReducedMotion();

const variants = computed<MotionVariants<string>>(() => {
	const opts = { delay: props.delay, duration: props.duration, distance: props.y };
	const base: MotionVariants<string> = reduced.value
		? { initial: { opacity: 1 }, visibleOnce: { opacity: 1 } }
		: props.x
			? slideIn(props.x > 0 ? "right" : "left", { ...opts, distance: Math.abs(props.x) })
			: props.scale !== 1
				? popIn(opts)
				: fadeUp(opts);

	if (props.once) return base;
	const visible = base.visibleOnce;
	return { initial: base.initial, visible };
});
</script>

<template>
	<component
		:is="tag"
		v-motion="variants"
		:class="{ 'reveal-fill': fill }"
	>
		<slot />
	</component>
</template>

<style scoped>
/* 分栏场景：包裹层保持高度传递与宽度收敛 */
.reveal-fill {
	display: flex;
	flex-direction: column;
	min-width: 0;
}
</style>
