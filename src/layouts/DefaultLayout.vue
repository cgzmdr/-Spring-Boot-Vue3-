<script setup lang="ts">
import { ref, onMounted } from "vue";
import AppMasthead from "@/components/AppMasthead.vue";
import AppFooter from "@/components/AppFooter.vue";

/**
 * 是否启用路由过渡。
 *
 * **预渲染的关键开关。**
 * `<Transition>` 在 SSR 阶段会把 `-enter-from` 类写进 HTML
 * （`.route-fade-enter-from { opacity: 0 }`），也就是说服务端直出的正文
 * 带着 `opacity: 0` —— 在 JS 执行、过渡跑完之前**完全不可见**。
 *
 * 这会让预渲染的意义归零：用户拿到的仍是一片空白，
 * 只是「空白的原因」从「没有 HTML」变成了「HTML 被 CSS 藏起来了」，
 * 比不做还隐蔽（爬虫读得到，人看不到）。
 *
 * 因此首帧不套 Transition，等客户端挂载完成后再启用。
 * 代价仅是「首次进入页面」没有淡入动画（本来也没有 —— 首屏不该有入场动画），
 * 之后的每次路由切换动画完全不变。
 */
const transitionsReady = ref(false);
onMounted(() => {
	transitionsReady.value = true;
});
</script>

<template>
	<AppMasthead />
	<main>
		<!-- 路由级过渡：外层包一层元素，保证多根节点页面（详情页等）也能参与过渡动画 -->
		<router-view v-slot="{ Component, route }">
			<Transition
				v-if="transitionsReady"
				name="route-fade"
				mode="out-in"
			>
				<div
					:key="route.path"
					class="route-wrap"
				>
					<component :is="Component" />
				</div>
			</Transition>
			<div
				v-else
				:key="route.path"
				class="route-wrap"
			>
				<component :is="Component" />
			</div>
		</router-view>
	</main>
	<AppFooter />
</template>
