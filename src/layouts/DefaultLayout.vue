<script setup lang="ts">
import AppMasthead from "@/components/AppMasthead.vue";
import AppFooter from "@/components/AppFooter.vue";
</script>

<template>
	<AppMasthead />
	<main>
		<!-- 路由级过渡：外层包一层元素，保证多根节点页面（详情页等）也能参与过渡动画 -->
		<router-view v-slot="{ Component, route }">
			<Transition
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
		</router-view>
	</main>
	<AppFooter />
</template>
