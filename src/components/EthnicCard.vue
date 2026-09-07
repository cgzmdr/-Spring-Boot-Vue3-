<script setup lang="ts">
import { computed } from 'vue'
import CoverImage from './CoverImage.vue'

const props = defineProps<{
  id: string
  name: string
  meta?: string
  coverImage?: string | null
  themeColor?: string
}>()

const link = computed(() => `/ethnic/${props.id}`)
</script>

<template>
  <router-link :to="link" class="ethnic-card">
    <div class="img">
      <CoverImage :src="coverImage" :name="name" :theme="themeColor" :prompt="`${name} ethnic group in China in traditional costume, portrait, photography`" size="portrait_4_3" />
    </div>
    <div class="body">
      <div class="name">{{ name }}</div>
      <div class="meta">{{ meta }}</div>
    </div>
  </router-link>
</template>
