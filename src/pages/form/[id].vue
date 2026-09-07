<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { formApi } from '@/api/modules'
import type { FormConfig } from '@/api/types'
import DynamicForm from '@/components/DynamicForm.vue'
import PageHead from '@/components/PageHead.vue'

const route = useRoute()
const loading = ref(false)
const error = ref('')
const config = ref<FormConfig | null>(null)
const result = ref<Record<string, unknown> | null>(null)

onMounted(async () => {
  loading.value = true
  try {
    config.value = await formApi.get(route.params.id as string)
  } catch {
    error.value = '表单加载失败'
  } finally {
    loading.value = false
  }
})

function onSubmitted(data: Record<string, unknown>) {
  result.value = data
  ElMessage.success('提交成功（演示模式：数据未持久化）')
}
</script>

<template>
  <div class="page">
    <template v-if="config">
      <PageHead :kicker="'FORM'" :title="config.name" :dek="config.description || ''" />
      <div class="container form-body">
        <DynamicForm :schema="config.schema" @submit="onSubmitted" />
        <div v-if="result" class="result-card">
          <h4>提交内容（演示）</h4>
          <pre>{{ JSON.stringify(result, null, 2) }}</pre>
        </div>
      </div>
    </template>
    <el-skeleton v-else-if="loading" :rows="5" animated class="container" />
    <el-empty v-else :description="error || '表单不存在'" class="container" />
  </div>
</template>

<style scoped>
.form-body { max-width: 760px; padding: 24px 0 64px; }
.result-card { margin-top: 24px; padding: 16px 20px; border: 1px dashed var(--line); border-radius: 10px; background: var(--paper-1); }
.result-card h4 { margin: 0 0 8px; font-family: var(--serif); }
.result-card pre { margin: 0; font-size: 13px; white-space: pre-wrap; word-break: break-all; }
</style>
