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
		<el-tooltip
			class="box-item"
			effect="light"
			content="回到顶部"
			placement="left"
		>
			<el-backtop
				:right="64"
				:bottom="64"
			/>
		</el-tooltip>
	</el-config-provider>
</template>
