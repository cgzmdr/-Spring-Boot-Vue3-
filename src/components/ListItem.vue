<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  idx: string
  title: string
  desc?: string
  meta?: string
  to?: string
  target?: string
}>()

const link = computed(() => {
  if (props.target) return { name: props.target, params: { id: props.to } }
  return props.to || ''
})
</script>

<template>
  <component :is="to || target ? 'router-link' : 'div'" v-bind="to || target ? { to: link } : {}" class="list-item">
    <div class="idx">{{ idx }}</div>
    <h5>{{ title }}</h5>
    <p v-if="desc">{{ desc }}</p>
    <div v-if="meta" class="meta">{{ meta }}</div>
  </component>
</template>
