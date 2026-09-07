<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  current: number // 0 基
  total: number
  size?: number
}>()

const emit = defineEmits<{ (e: 'change', page: number): void }>()

const size = computed(() => props.size || 10)
const pages = computed(() => Math.max(1, Math.ceil(props.total / size.value)))
const pageIndex = computed(() => props.current) // 0 基

function go(p: number) {
  if (p < 0 || p >= pages.value || p === pageIndex.value) return
  emit('change', p)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 页码窗口 */
const windowPages = computed(() => {
  const total = pages.value
  const cur = pageIndex.value
  const list: number[] = []
  const start = Math.max(0, Math.min(cur - 2, total - 5))
  const end = Math.min(total, start + 5)
  for (let i = start; i < end; i++) list.push(i)
  return list
})
</script>

<template>
  <nav v-if="pages > 1" class="pagination" aria-label="分页">
    <a class="disabled" :class="{ disabled: pageIndex === 0 }" @click.prevent="go(pageIndex - 1)">‹</a>
    <a v-for="p in windowPages" :key="p" :class="{ active: p === pageIndex }" @click.prevent="go(p)">{{ p + 1 }}</a>
    <a :class="{ disabled: pageIndex >= pages - 1 }" @click.prevent="go(pageIndex + 1)">›</a>
  </nav>
</template>
