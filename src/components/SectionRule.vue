<script setup lang="ts">
defineProps<{ no: string; title: string }>()
</script>

<template>
  <!-- v-motion-fade-up：@vueuse/motion 预设指令，滚动进入视口时上浮淡入 -->
  <div class="section-rule" v-motion-fade-up>
    <span class="no">{{ no }}</span>
    <h3>{{ title }}</h3>
    <span class="line"></span>
  </div>
</template>
