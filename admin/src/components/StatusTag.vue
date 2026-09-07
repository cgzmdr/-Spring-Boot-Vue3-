<template>
  <el-tag :type="(tagType as any)" size="small" disable-transitions>{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  tag?: string
}>()

const tagType = computed(() => props.tag || 'info')
</script>
