<script setup lang="ts">
import { computed } from 'vue'
import CoverImage from './CoverImage.vue'
import { fadeUp, stagger } from '@/utils/motion'

const props = defineProps<{
  id: string
  name: string
  meta?: string
  coverImage?: string | null
  themeColor?: string
  /** 列表序号：用于错峰入场（@vueuse/motion） */
  index?: number
}>()

const link = computed(() => `/ethnic/${props.id}`)
/** 入场动效：按列表序号错峰上浮淡入 */
const motion = computed(() => fadeUp({ delay: stagger(props.index ?? 0) }))
</script>

<template>
  <router-link :to="link" class="ethnic-card" v-motion="motion">
    <div class="img">
      <CoverImage :src="coverImage" :name="name" :theme="themeColor" :prompt="`${name} ethnic group in China in traditional costume, portrait, photography`" size="portrait_4_3" />
    </div>
    <div class="body">
      <div class="name">{{ name }}</div>
      <div class="meta">{{ meta }}</div>
    </div>
  </router-link>
</template>
