<script setup lang="ts">
defineProps<{ no: string; title: string }>()
</script>

<template>
  <div class="section-rule">
    <span class="no">{{ no }}</span>
    <h3>{{ title }}</h3>
    <span class="line"></span>
  </div>
</template>
