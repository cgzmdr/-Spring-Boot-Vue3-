<script setup lang="ts">
defineProps<{ kicker: string; title: string; dek?: string }>();
</script>

<template>
	<div class="page-head">
		<div class="kicker">{{ kicker }}</div>
		<h2>{{ title }}</h2>
		<p
			v-if="dek"
			class="dek"
		>
			{{ dek }}
		</p>
	</div>
</template>
