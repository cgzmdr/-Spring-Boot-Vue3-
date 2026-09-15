<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑自治地方' : '新增自治地方' }}</h2>
      <el-button
        :icon="Back"
        @click="router.back()"
        >返回</el-button
      >
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      v-loading="loading"
    >
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col
          :xs="24"
          :md="14"
        >
          <el-form-item
            label="官方全称"
            prop="name"
          >
            <el-input
              v-model="form.name"
              placeholder="如：延边朝鲜族自治州"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="10"
        >
          <el-form-item
            label="级别"
            prop="level"
          >
            <el-select
              v-model="form.level"
              style="width: 100%"
            >
              <el-option
                v-for="o in AREA_LEVEL_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="自治民族">
            <el-input
              v-model="ethnicText"
              placeholder="多个民族用顿号分隔，如：拉祜族、佤族、布朗族、傣族"
            />
            <div class="form-tip">
              与 C 端「按自治民族」浏览绑定；名称建议带「族」字以便与民族档案匹配跳转。
            </div>
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="所属省级行政区">
            <el-input
              v-model="form.province"
              placeholder="如：吉林省（自治区本身填其自身名称）"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="6"
        >
          <el-form-item label="成立年份">
            <el-input-number
              v-model="form.establishedYear"
              :min="1900"
              :max="2100"
              :controls="false"
              placeholder="不可考请留空"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="6"
        >
          <el-form-item label="行政中心">
            <el-input
              v-model="form.seat"
              placeholder="如：延吉市"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          >保存</el-button
        >
        <el-button @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import {
  createArea,
  getArea,
  updateArea,
  AREA_LEVEL_OPTIONS,
  type AutonomousArea
} from '@/api/modules/bseries'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = reactive<AutonomousArea>({
  name: '',
  level: 'autonomous_prefecture',
  ethnicGroups: [],
  province: '',
  establishedYear: null,
  seat: null
})

/** 民族以「顿号/逗号分隔的文本」编辑，提交时转数组 */
const ethnicText = ref('')

function parseList(v: string[] | string | null | undefined): string[] {
  if (!v) return []
  if (Array.isArray(v)) return v
  try {
    const p = JSON.parse(v)
    return Array.isArray(p) ? p : []
  } catch {
    return v ? [v] : []
  }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入官方全称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }]
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const d = await getArea(route.query.id as string)
    Object.assign(form, d)
    ethnicText.value = parseList(d.ethnicGroups).join('、')
  } finally {
    loading.value = false
  }
}

/** 文本 → 数组（兼容中文顿号、英文逗号、空格） */
function toArray(text: string): string[] {
  return text
    .split(/[、,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: AutonomousArea = {
      ...form,
      ethnicGroups: toArray(ethnicText.value)
    }
    if (isEdit.value) {
      await updateArea(route.query.id as string, payload)
      ElMessage.success('更新成功')
    } else {
      await createArea(payload)
      ElMessage.success('新增成功')
    }
    router.back()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}
</style>
