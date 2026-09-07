<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

/** 字段定义（与后端 form_config.schema 契约一致） */
export interface FormField {
  key: string
  label: string
  type: 'text' | 'textarea' | 'number' | 'select' | 'radio' | 'checkbox' | 'date'
  required?: boolean
  placeholder?: string
  options?: { label: string; value: string }[]
}

export interface FormSchema {
  fields: FormField[]
}

const props = defineProps<{
  /** 表单 schema（JSON 字符串或对象） */
  schema: string | FormSchema
  /** 提交按钮文案 */
  submitText?: string
}>()

const emit = defineEmits<{ submit: [data: Record<string, unknown>] }>()

const fields = ref<FormField[]>([])
const data = reactive<Record<string, unknown>>({})
const submitting = ref(false)

function normalizeSchema() {
  let sch: FormSchema
  if (typeof props.schema === 'string') {
    try {
      sch = JSON.parse(props.schema)
    } catch {
      sch = { fields: [] }
    }
  } else {
    sch = props.schema
  }
  fields.value = Array.isArray(sch?.fields) ? sch.fields : []
  for (const f of fields.value) {
    if (f.type === 'checkbox') data[f.key] = []
    else data[f.key] = ''
  }
}

watch(() => props.schema, normalizeSchema, { immediate: true })

function validate(): boolean {
  for (const f of fields.value) {
    const v = data[f.key]
    const empty = v === '' || v === null || v === undefined || (Array.isArray(v) && !v.length)
    if (f.required && empty) {
      ElMessage.warning(`请填写「${f.label}」`)
      return false
    }
  }
  return true
}

function submit() {
  if (!validate()) return
  submitting.value = true
  emit('submit', { ...data })
  setTimeout(() => (submitting.value = false), 300)
}
</script>

<template>
  <div class="dynamic-form">
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item v-for="f in fields" :key="f.key" :label="f.label" :required="f.required">
        <!-- 文本 -->
        <el-input v-if="f.type === 'text'" v-model="data[f.key] as string" :placeholder="f.placeholder" clearable />
        <!-- 多行文本 -->
        <el-input v-else-if="f.type === 'textarea'" v-model="data[f.key] as string" type="textarea" :rows="3" :placeholder="f.placeholder" />
        <!-- 数字 -->
        <el-input-number v-else-if="f.type === 'number'" v-model="data[f.key] as number" :placeholder="f.placeholder" style="width: 100%" />
        <!-- 下拉 -->
        <el-select v-else-if="f.type === 'select'" v-model="data[f.key] as string" :placeholder="f.placeholder || '请选择'" style="width: 100%">
          <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <!-- 单选 -->
        <el-radio-group v-else-if="f.type === 'radio'" v-model="data[f.key] as string">
          <el-radio v-for="o in f.options || []" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
        </el-radio-group>
        <!-- 复选 -->
        <el-checkbox-group v-else-if="f.type === 'checkbox'" v-model="data[f.key] as string[]">
          <el-checkbox v-for="o in f.options || []" :key="o.value" :value="o.value">{{ o.label }}</el-checkbox>
        </el-checkbox-group>
        <!-- 日期 -->
        <el-date-picker v-else-if="f.type === 'date'" v-model="data[f.key] as string" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="submitting">{{ submitText || '提交' }}</el-button>
        <el-button @click="normalizeSchema">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.dynamic-form { max-width: 560px; }
</style>
