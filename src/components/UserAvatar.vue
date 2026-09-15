<script setup lang="ts">
import { computed } from "vue";
import { resolveStaticUrl } from "@/utils/format";

/** 社区头像：优先真实头像，缺失时用昵称首字生成色块 */
const props = withDefaults(
	defineProps<{
		name?: string | null;
		src?: string | null;
		size?: number;
	}>(),
	{ name: "", src: null, size: 26 },
);

const initial = computed(() => (props.name || "友").trim().slice(0, 1));
const url = computed(() => resolveStaticUrl(props.src));
const hue = computed(() => {
	const text = props.name || "";
	let sum = 0;
	for (let i = 0; i < text.length; i += 1) sum += text.charCodeAt(i);
	return sum % 360;
});
const style = computed(() => ({
	width: `${props.size}px`,
	height: `${props.size}px`,
	fontSize: `${Math.max(10, Math.round(props.size * 0.45))}px`,
	background: `hsl(${hue.value} 42% 42%)`,
}));
</script>

<template>
	<span
		class="avatar"
		:style="style"
	>
		<img
			v-if="url"
			:src="url"
			:alt="name || ''"
			loading="lazy"
		/>
		<template v-else>{{ initial }}</template>
	</span>
</template>

<style scoped>
.avatar {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	border-radius: 50%;
	color: #fff;
	overflow: hidden;
	flex: none;
	line-height: 1;
}
.avatar img {
	width: 100%;
	height: 100%;
	object-fit: cover;
	display: block;
}
</style>
