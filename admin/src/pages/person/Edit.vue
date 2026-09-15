<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑人物档案' : '新增人物档案' }}</h2>
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
      label-width="110px"
      v-loading="loading"
    >
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item
            label="姓名"
            prop="personName"
          >
            <el-input
              v-model="form.personName"
              placeholder="如：容亚美"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item
            label="民族"
            prop="ethnicGroupName"
          >
            <el-input
              v-model="form.ethnicGroupName"
              placeholder="如：黎族"
            />
            <div class="form-tip">
              必填。「姓名 + 民族」是唯一键 —— 同名不同族会分别成条，民族留空会导致误判重复。
            </div>
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="角色类型">
            <el-select
              v-model="form.roleType"
              style="width: 100%"
            >
              <el-option
                v-for="o in ROLE_TYPE_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="领域">
            <el-select
              v-model="form.domain"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="d in DOMAIN_OPTIONS"
                :key="d"
                :label="d"
                :value="d"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="生卒年">
            <el-input
              v-model="form.lifespan"
              placeholder="如：1894—1961（无可靠依据请留空）"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">人物简介</el-divider>
      <el-form-item label="简介">
        <el-input
          v-model="form.bio"
          type="textarea"
          :rows="5"
          placeholder="仅在有可靠依据时填写；无把握请留空，不要编造生平"
        />
        <div class="form-tip">
          未填写时 C 端只展示该人物的民族与所关联的非遗项目，不显示简介。
        </div>
      </el-form-item>

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
  createPerson,
  getPerson,
  updatePerson,
  ROLE_TYPE_OPTIONS,
  DOMAIN_OPTIONS,
  type PersonProfile
} from '@/api/modules/bseries'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = reactive<PersonProfile>({
  personName: '',
  ethnicGroupName: '',
  roleType: 'inheritor',
  domain: null,
  bio: null,
  lifespan: null
})

const rules: FormRules = {
  personName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  ethnicGroupName: [{ required: true, message: '请输入所属民族', trigger: 'blur' }]
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    Object.assign(form, await getPerson(route.query.id as string))
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePerson(route.query.id as string, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createPerson({ ...form })
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
