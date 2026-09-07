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
	</el-config-provider>
</template>
