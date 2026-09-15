<script setup lang="ts">
import { computed } from 'vue'
import CoverImage from './CoverImage.vue'
import { fadeUp } from '@/utils/motion'

const props = defineProps<{
  no?: string
  title: string
  desc?: string
  coverImage?: string | null
  prompt?: string
  to?: string
  /** 列表序号：错峰入场（@vueuse/motion） */
  index?: number
}>()

const motion = computed(() => fadeUp({ delay: (props.index ?? 0) * 70 }))
</script>

<template>
  <component :is="to ? 'router-link' : 'div'" :to="to || undefined" class="feature" v-motion="motion">
    <div class="img">
      <CoverImage :src="coverImage" :name="title" :prompt="prompt || `${title} traditional culture, photography`" size="landscape_4_3" />
    </div>
    <div class="t">
      <span v-if="no" class="no">{{ no }}</span>
      <h4>{{ title }}</h4>
      <p v-if="desc">{{ desc }}</p>
    </div>
  </component>
</template>
