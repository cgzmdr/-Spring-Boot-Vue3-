<script setup lang="ts">
import { computed } from 'vue'
import { fadeUp } from '@/utils/motion'

const props = defineProps<{
  idx: string
  title: string
  desc?: string
  meta?: string
  to?: string
  target?: string
  /** 列表序号：错峰入场（@vueuse/motion） */
  index?: number
}>()

const link = computed(() => {
  if (props.target) return { name: props.target, params: { id: props.to } }
  return props.to || ''
})

const motion = computed(() => fadeUp({ delay: (props.index ?? 0) * 70, distance: 16 }))
</script>

<template>
  <component
    :is="to || target ? 'router-link' : 'div'"
    v-bind="to || target ? { to: link } : {}"
    class="list-item"
    v-motion="motion"
  >
    <div class="idx">{{ idx }}</div>
    <h5>{{ title }}</h5>
    <p v-if="desc">{{ desc }}</p>
    <div v-if="meta" class="meta">{{ meta }}</div>
  </component>
</template>
