<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createForm, getForm, updateForm } from '@/api/modules/form'
import type { FormConfig } from '@/api/types'
import { uuidToStr } from '@/utils/uuid'

interface FieldRow {
  key: string
  label: string
  type: 'text' | 'textarea' | 'number' | 'select' | 'radio' | 'checkbox' | 'date'
  required: boolean
  placeholder: string
  optionsText: string
}

const route = useRoute()
const router = useRouter()
const id = ref(route.query.id as string | undefined)
const saving = ref(false)
const loading = ref(false)

const form = reactive({
  code: '',
  name: '',
  description: '',
  status: 'active' as 'active' | 'disabled',
})

const FIELD_TYPES = [
  { value: 'text', label: '单行文本' },
  { value: 'textarea', label: '多行文本' },
  { value: 'number', label: '数字' },
  { value: 'select', label: '下拉选择' },
  { value: 'radio', label: '单选' },
  { value: 'checkbox', label: '多选' },
  { value: 'date', label: '日期' },
]

const fields = ref<FieldRow[]>([])
const activeField = ref(0)
const activeFieldName = computed(() => (fields.value[activeField.value]?.label || '字段') + ` #${activeField.value + 1}`)

function addField() {
  fields.value.push({ key: '', label: '', type: 'text', required: false, placeholder: '', optionsText: '' })
  activeField.value = fields.value.length - 1
}

function removeField(i: number) {
  fields.value.splice(i, 1)
  if (activeField.value >= fields.value.length) activeField.value = Math.max(0, fields.value.length - 1)
}

function moveField(i: number, dir: -1 | 1) {
  const j = i + dir
  if (j < 0 || j >= fields.value.length) return
  ;[fields.value[i], fields.value[j]] = [fields.value[j], fields.value[i]]
}

function parseOptions(text: string): { label: string; value: string }[] {
  return text
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
    .map((s) => {
      const [v, label] = s.split(':')
      return { value: v.trim(), label: (label || v).trim() }
    })
}

function buildSchema(): string {
  const sch = {
    fields: fields.value.map((f) => {
      const base: Record<string, unknown> = {
        key: f.key.trim(),
        label: f.label.trim() || f.key.trim(),
        type: f.type,
      }
      if (f.required) base.required = true
      if (f.placeholder.trim()) base.placeholder = f.placeholder.trim()
      if (f.type === 'select' || f.type === 'radio' || f.type === 'checkbox') {
        base.options = parseOptions(f.optionsText)
      }
      return base
    }),
  }
  return JSON.stringify(sch, null, 2)
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请输入表单名称')
  if (!form.code.trim()) return ElMessage.warning('请输入表单编码')
  if (!fields.value.length) return ElMessage.warning('请至少添加一个字段')
  for (const f of fields.value) {
    if (!f.key.trim()) return ElMessage.warning('存在未填写 key 的字段')
  }
  saving.value = true
  try {
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      status: form.status,
      schema: buildSchema(),
    }
    if (id.value) {
      await updateForm(id.value, payload)
      ElMessage.success('已保存')
    } else {
      await createForm(payload)
      ElMessage.success('已创建')
    }
    router.push('/form')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!id.value) return
  loading.value = true
  try {
    const data: FormConfig = await getForm(id.value)
    form.code = data.code
    form.name = data.name
    form.description = data.description || ''
    form.status = data.status === 'disabled' ? 'disabled' : 'active'
    try {
      const sch = JSON.parse(data.schema || '{}')
      fields.value = (sch.fields || []).map((f: any) => ({
        key: f.key || '',
        label: f.label || '',
        type: f.type || 'text',
        required: !!f.required,
        placeholder: f.placeholder || '',
        optionsText: Array.isArray(f.options) ? f.options.map((o: any) => `${o.value}:${o.label}`).join('\n') : '',
      }))
      activeField.value = 0
    } catch {
      fields.value = []
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ id ? '编辑表单' : '新增表单' }}</h2>
      <div>
        <el-button @click="router.push('/form')">返回</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>

    <el-form v-loading="loading" label-width="90px" class="base-form">
      <el-row :gutter="16">
        <el-col :span="10">
          <el-form-item label="表单名称" required>
            <el-input v-model="form.name" placeholder="如：意见反馈" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="表单编码" required>
            <el-input v-model="form.code" placeholder="如：feedback（前端按编码/ID 引用）" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" value="active" />
              <el-option label="停用" value="disabled" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="说明">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="表单用途说明" />
      </el-form-item>
    </el-form>

    <el-row :gutter="16">
      <el-col :span="10">
        <div class="panel">
          <div class="panel-head">
            <h3>字段列表（{{ fields.length }}）</h3>
            <el-button size="small" type="primary" :icon="'Plus'" @click="addField">添加字段</el-button>
          </div>
          <div class="field-list">
            <div
              v-for="(f, i) in fields"
              :key="i"
              class="field-row"
              :class="{ active: activeField === i }"
              @click="activeField = i"
            >
              <span class="idx">{{ i + 1 }}</span>
              <div class="f-main">
                <b>{{ f.label || '(未命名)' }}</b>
                <span class="f-meta">{{ f.type }}<template v-if="f.required"> · 必填</template></span>
              </div>
              <div class="f-ops">
                <el-button link size="small" :disabled="i === 0" @click.stop="moveField(i, -1)">↑</el-button>
                <el-button link size="small" :disabled="i === fields.length - 1" @click.stop="moveField(i, 1)">↓</el-button>
                <el-button link type="danger" size="small" @click.stop="removeField(i)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!fields.length" description="暂无字段，请添加" :image-size="60" />
          </div>
        </div>
      </el-col>

      <el-col :span="14">
        <div v-if="fields.length" class="panel">
          <div class="panel-head"><h3>编辑 {{ activeFieldName }}</h3></div>
          <el-form label-width="80px">
            <el-form-item label="Key" required>
              <el-input v-model="fields[activeField].key" placeholder="字段标识（英文/下划线）" />
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="fields[activeField].label" placeholder="展示名称" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="fields[activeField].type" style="width: 100%">
                <el-option v-for="t in FIELD_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="占位提示">
              <el-input v-model="fields[activeField].placeholder" placeholder="可选" />
            </el-form-item>
            <el-form-item label="必填">
              <el-switch v-model="fields[activeField].required" />
            </el-form-item>
            <el-form-item v-if="['select', 'radio', 'checkbox'].includes(fields[activeField].type)" label="选项">
              <el-input
                v-model="fields[activeField].optionsText"
                type="textarea"
                :rows="4"
                placeholder="每行一项：值:标签&#10;例如：&#10;bj:北京&#10;sh:上海"
              />
            </el-form-item>
          </el-form>
        </div>
        <div v-else class="panel tip">请先在左侧添加字段</div>
      </el-col>
    </el-row>

    <div class="panel preview">
      <div class="panel-head"><h3>Schema 预览</h3></div>
      <pre>{{ fields.length ? buildSchema() : '{}' }}</pre>
    </div>
  </div>
</template>

<style scoped lang="scss">
.base-form { margin-bottom: 16px; }
.panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 16px;
  background: #fff;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  h3 { margin: 0; font-size: 15px; }
}
.field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  &.active { border-color: #409eff; background: #ecf5ff; }
}
.idx { width: 20px; height: 20px; border-radius: 50%; background: #f0f2f5; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.f-main { flex: 1; min-width: 0; b { display: block; font-size: 13px; } }
.f-meta { font-size: 12px; color: #909399; }
.f-ops { display: flex; gap: 2px; flex-shrink: 0; }
.tip { color: #909399; text-align: center; padding: 32px 0; }
.preview pre {
  background: #1f2937; color: #e5e7eb; border-radius: 6px;
  padding: 12px; font-size: 12px; max-height: 260px; overflow: auto; margin: 0;
}
</style>
