<script setup lang="ts">
withDefaults(defineProps<{ text: string; ink?: boolean }>(), { ink: false });
</script>

<template>
	<span
		class="badge"
		:class="{ ink }"
		>{{ text }}</span
	>
</template>
