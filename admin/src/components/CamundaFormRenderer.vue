<template>
  <div class="camunda-form-renderer">
    <div v-if="loadError" class="render-error">
      <el-alert type="error" :closable="false" :title="loadError" show-icon />
    </div>
    <div v-else-if="!schema" class="render-empty">
      <el-alert
        type="info"
        :closable="false"
        title="该环节未配置 Camunda Form，将使用内置审批表单"
        show-icon
      />
    </div>
    <div v-else ref="hostRef" class="form-host" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
// form-js 的渲染器：直接吃 Camunda Form（form-js schema），保证与 Camunda Modeler 中的表单一致
import { Form as FormRenderer } from '@bpmn-io/form-js-viewer'
import '@bpmn-io/form-js-viewer/dist/assets/form-js.css'
import '@bpmn-io/form-js-viewer/dist/assets/form-js-base.css'

const props = withDefaults(
  defineProps<{
    /** form-js schema（JSON 字符串或对象） */
    schema?: string | Record<string, unknown> | null
    /** 表单初始数据 */
    data?: Record<string, unknown>
  }>(),
  { schema: null, data: () => ({}) }
)

const emit = defineEmits<{
  (e: 'changed', data: Record<string, unknown>): void
}>()

const hostRef = ref<HTMLElement | null>(null)
const loadError = ref('')

let renderer: FormRenderer | null = null

/** schema -> JS 对象（容错：非法 JSON 时给出可读错误而不是白屏） */
function parseSchema(): Record<string, unknown> | null {
  if (!props.schema) return null
  if (typeof props.schema === 'object') return props.schema as Record<string, unknown>
  try {
    const parsed = JSON.parse(props.schema)
    if (!parsed || !Array.isArray(parsed.components)) {
      loadError.value = 'Camunda Form schema 缺少 components 字段'
      return null
    }
    return parsed
  } catch (e) {
    loadError.value = `Camunda Form schema 解析失败：${(e as Error).message}`
    return null
  }
}

async function render() {
  loadError.value = ''
  const schema = parseSchema()
  if (!schema || !hostRef.value) return

  destroy()
  renderer = new FormRenderer({ container: hostRef.value })
  try {
    await renderer.importSchema(schema, props.data || {})
    renderer.on('changed', () => {
      emit('changed', getData())
    })
  } catch (e) {
    loadError.value = `Camunda Form 渲染失败：${(e as Error).message}`
  }
}

function destroy() {
  if (renderer) {
    try {
      renderer.destroy()
    } catch {
      /* 忽略重复销毁 */
    }
    renderer = null
  }
}

/** 取当前表单数据 */
function getData(): Record<string, unknown> {
  if (!renderer) return {}
  try {
    return (renderer.submit().data || {}) as Record<string, unknown>
  } catch {
    return {}
  }
}

/** 提交并校验；返回 null 表示通过，否则返回首个错误信息 */
function submit(): { data: Record<string, unknown>; error: string | null } {
  if (!renderer) {
    return { data: {}, error: null }
  }
  const result = renderer.submit()
  const errors = result.errors || {}
  const keys = Object.keys(errors)
  if (keys.length > 0) {
    const first = errors[keys[0]] as { message?: string } | string | undefined
    const message =
      typeof first === 'string' ? first : first?.message || '请完整填写表单必填项'
    return { data: {}, error: message }
  }
  return { data: (result.data || {}) as Record<string, unknown>, error: null }
}

defineExpose({ getData, submit, render })

onMounted(render)
watch(() => props.schema, render)
onBeforeUnmount(destroy)
</script>

<style scoped lang="scss">
.camunda-form-renderer {
  .form-host {
    // form-js 自带样式，这里只做与 Element Plus 观感的对齐
    :deep(.fjs-container) {
      font-family: inherit;
      font-size: 14px;
    }

    :deep(.fjs-form-field-label) {
      font-size: 14px;
      font-weight: 400;
      color: #606266;
    }

    :deep(.fjs-input),
    :deep(.fjs-textarea) {
      border-radius: 4px;
      border-color: #dcdfe6;

      &:hover {
        border-color: #c0c4cc;
      }

      &:focus {
        border-color: #409eff;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
      }
    }

    :deep(.fjs-form-field-text) {
      h1,
      h2,
      h3,
      p {
        margin: 0 0 8px;
      }
    }

    :deep(.fjs-described-by) {
      font-size: 12px;
      color: #909399;
    }

    :deep(.fjs-form-field-error) {
      color: #f56c6c;
      font-size: 12px;
    }
  }

  .render-error,
  .render-empty {
    margin-bottom: 12px;
  }
}
</style>
