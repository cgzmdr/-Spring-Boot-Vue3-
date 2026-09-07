<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑专题' : '新增专题' }}</h2>
      <el-button :icon="Back" @click="router.back()">返回</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="loading">
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <el-form-item label="专题标题" prop="title">
            <el-input v-model="form.title" placeholder="如：多元一体 · 中华民族共同体" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="副标题">
            <el-input v-model="form.subtitle" placeholder="讲好中华民族共同体的故事" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="排序号">
            <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option
                v-for="o in CONTENT_STATUS_OPTIONS.filter((x) => x.value)"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="封面图">
            <ImageUpload v-model="form.coverImage" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">内容</el-divider>
      <el-form-item label="专题介绍">
        <el-input v-model="form.description" type="textarea" :rows="6" />
      </el-form-item>
      <el-form-item label="关联条目">
        <el-input
          :model-value="entriesText"
          disabled
          type="textarea"
          :rows="2"
          placeholder="条目关联能力预留（跨类型多选民族/节日/艺术）"
        />
        <div class="form-tip">当前版本预留，可后续扩展跨类型关联选择器</div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
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
import { createTopic, getTopic, updateTopic } from '@/api/modules/topic'
import type { Topic } from '@/api/types'
import { CONTENT_STATUS_OPTIONS } from '@/constants'
import ImageUpload from '@/components/ImageUpload.vue'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = reactive<Partial<Topic>>({
  title: '',
  subtitle: '',
  description: '',
  coverImage: '',
  status: 'draft',
  orderNum: 0
})

/** 关联条目展示（预留） */
const entriesText = computed(() => {
  const entries = form.entries
  if (!entries || !entries.length) return ''
  return entries.map((e) => JSON.stringify(e)).join(', ')
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入专题标题', trigger: 'blur' }]
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    Object.assign(form, await getTopic(route.query.id as string))
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
      await updateTopic(route.query.id as string, form)
      ElMessage.success('更新成功')
    } else {
      await createTopic(form)
      ElMessage.success('新增成功')
    }
    router.back()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>
