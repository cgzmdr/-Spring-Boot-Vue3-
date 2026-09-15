<script setup lang="ts">
defineProps<{ kicker: string; title: string; dek?: string }>();
</script>

<template>
	<div class="page-head">
		<div
			class="kicker"
			v-motion-fade-in
		>
			{{ kicker }}
		</div>
		<h2 v-motion-fade-up>{{ title }}</h2>
		<p
			v-if="dek"
			class="dek"
			v-motion-fade-up
		>
			{{ dek }}
		</p>
	</div>
</template>
