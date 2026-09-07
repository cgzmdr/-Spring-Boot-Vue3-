<script setup lang="ts">
import AppMasthead from "@/components/AppMasthead.vue";
import AppFooter from "@/components/AppFooter.vue";
</script>

<template>
	<AppMasthead />
	<main>
		<router-view />
	</main>
	<AppFooter />
</template>
