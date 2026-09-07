<script setup lang="ts">
import CoverImage from './CoverImage.vue'

defineProps<{
  no?: string
  title: string
  desc?: string
  coverImage?: string | null
  prompt?: string
  to?: string
}>()
</script>

<template>
  <component :is="to ? 'router-link' : 'div'" :to="to || undefined" class="feature">
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
