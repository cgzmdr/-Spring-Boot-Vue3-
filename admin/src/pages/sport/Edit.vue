<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑传统体育项目' : '新增传统体育项目' }}</h2>
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
          :md="12"
        >
          <el-form-item
            label="项目名称"
            prop="name"
          >
            <el-input
              v-model="form.name"
              placeholder="如：珍珠球"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item
            label="类别"
            prop="category"
          >
            <el-select
              v-model="form.category"
              style="width: 100%"
            >
              <el-option
                v-for="o in SPORT_CATEGORY_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="相关民族">
            <el-input
              v-model="ethnicText"
              placeholder="多个民族用顿号分隔，如：侗族、壮族"
            />
            <div class="form-tip">
              与 C 端按民族筛选绑定；名称带「族」字可与民族档案匹配跳转。
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="项目描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              placeholder="客观说明该项目是什么、怎么玩、来源"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">设项与规则</el-divider>
      <el-row :gutter="24">
        <el-col :span="24">
          <el-form-item label="子项">
            <el-input
              v-model="subText"
              placeholder="多子项项目填写，用顿号分隔，如：立姿、跪姿"
            />
            <div class="form-tip">
              如民族式摔跤的 6 个子项、民族马术的 5 个子项；无子项留空。
            </div>
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="场地规格">
            <el-input
              v-model="form.venue"
              placeholder="如：长 28 米、宽 15 米（不确定留空）"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="参赛人数">
            <el-input
              v-model="form.teamSize"
              placeholder="如：每队上场 7 人（不确定留空）"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="主要器材">
            <el-input
              v-model="form.equipment"
              placeholder="如：珍珠球、球拍、抄网"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="6"
        >
          <el-form-item label="入会年份">
            <el-input-number
              v-model="form.firstEventYear"
              :min="1953"
              :max="2100"
              :controls="false"
              placeholder="不确定留空"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="6"
        >
          <el-form-item label="相关非遗">
            <el-input
              v-model="form.heritageLink"
              placeholder="如：满族珍珠球"
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
  createSport,
  getSport,
  updateSport,
  SPORT_CATEGORY_OPTIONS,
  type TraditionalSport
} from '@/api/modules/bseries'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = reactive<TraditionalSport>({
  name: '',
  category: 'ball',
  ethnicOrigins: [],
  description: null,
  equipment: null,
  venue: null,
  teamSize: null,
  firstEventYear: null,
  subEvents: [],
  heritageLink: null
})

const ethnicText = ref('')
const subText = ref('')

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
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }]
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const d = await getSport(route.query.id as string)
    Object.assign(form, d)
    ethnicText.value = parseList(d.ethnicOrigins).join('、')
    subText.value = parseList(d.subEvents).join('、')
  } finally {
    loading.value = false
  }
}

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
    const payload: TraditionalSport = {
      ...form,
      ethnicOrigins: toArray(ethnicText.value),
      subEvents: toArray(subText.value)
    }
    if (isEdit.value) {
      await updateSport(route.query.id as string, payload)
      ElMessage.success('更新成功')
    } else {
      await createSport(payload)
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
