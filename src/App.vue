<script setup lang="ts">
import { computed } from "vue";
import DefaultLayout from "@/layouts/DefaultLayout.vue";
import { useLangStore } from "@/stores/lang";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import en from "element-plus/es/locale/lang/en";

const lang = useLangStore();
const locale = computed(() => (lang.isEn ? en : zhCn));
</script>

<template>
	<el-config-provider :locale="locale">
		<DefaultLayout />
		<!--
			回到顶部。
			原先外面套了一层 <el-tooltip content="回到顶部">，但它只是给一个
			悬浮按钮加鼠标提示——为此却要把整条 popper 链（ElTooltip /
			ElPopperContent / ElFocusTrap，约 45 KB 未压缩）拖进首屏。
			改用原生 title：零成本、同样有提示（移动端本就没有 hover 语义）。
		-->
		<el-backtop
			:right="64"
			:bottom="64"
			:title="lang.pick('回到顶部', 'Back to top')"
		/>
	</el-config-provider>
</template>
