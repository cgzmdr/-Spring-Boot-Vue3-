<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑民族' : '新增民族' }}</h2>
      <el-button :icon="Back" @click="router.back()">返回</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="loading">
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <el-form-item label="民族名称" prop="name">
            <el-input v-model="form.name" placeholder="如：藏族" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="英文名称">
            <el-input v-model="form.nameEn" placeholder="Tibetan" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="民族语自称">
            <el-input v-model="form.selfName" placeholder="བོད་པ" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="拼音">
            <el-input v-model="form.pinyin" placeholder="zangzu" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="人口">
            <el-input-number v-model="form.population" :min="0" :controls="false" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="语系">
            <el-select v-model="form.languageFamily" style="width: 100%" allow-create filterable>
              <el-option v-for="f in LANGUAGE_FAMILY_OPTIONS" :key="f" :label="f || '（未填）'" :value="f" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="聚居地（逗号分隔）">
            <el-input v-model="form.region" placeholder="西藏, 青海, 四川, 云南, 甘肃" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="语言（逗号分隔）">
            <el-input v-model="form.languages" placeholder="藏语" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="文字（逗号分隔）">
            <el-input v-model="form.scripts" placeholder="藏文" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="宗教（逗号分隔）">
            <el-input v-model="form.religion" placeholder="藏传佛教" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="标签（逗号分隔）">
            <el-input v-model="form.tags" placeholder="高原, 雪域" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="主题色">
            <el-color-picker v-model="form.themeColor" />
            <span class="form-tip" style="margin-left: 8px">{{ form.themeColor }}</span>
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

      <el-divider content-position="left">简介与正文</el-divider>
      <el-form-item label="一句话简介">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="生活在青藏高原的古老民族" />
      </el-form-item>
      <el-form-item label="简介（英文）">
        <el-input v-model="form.summaryEn" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="详细介绍">
        <el-input v-model="form.description" type="textarea" :rows="6" placeholder="民族的起源、历史与文化…" />
      </el-form-item>
      <el-form-item label="详细介绍（英文）">
        <el-input v-model="form.descriptionEn" type="textarea" :rows="6" />
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
import { createEthnicGroup, getEthnicGroup, updateEthnicGroup } from '@/api/modules/ethnic'
import type { EthnicGroup } from '@/api/types'
import { CONTENT_STATUS_OPTIONS, LANGUAGE_FAMILY_OPTIONS } from '@/constants'
import ImageUpload from '@/components/ImageUpload.vue'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = reactive<Partial<EthnicGroup>>({
  name: '',
  nameEn: '',
  selfName: '',
  pinyin: '',
  population: undefined,
  languageFamily: '',
  region: '',
  languages: '',
  scripts: '',
  religion: '',
  summary: '',
  summaryEn: '',
  description: '',
  descriptionEn: '',
  coverImage: '',
  themeColor: '#B6402E',
  tags: '',
  status: 'draft',
  orderNum: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入民族名称', trigger: 'blur' }]
}

/** 数组字段兼容：后端可能返回数组或逗号分隔字符串 */
function toStr(v: unknown): string {
  if (Array.isArray(v)) return v.join(', ')
  return (v as string) || ''
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getEthnicGroup(route.query.id as string)
    Object.assign(form, data, {
      region: toStr(data.region),
      languages: toStr(data.languages),
      scripts: toStr(data.scripts),
      religion: toStr(data.religion),
      tags: toStr(data.tags)
    })
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
      await updateEthnicGroup(route.query.id as string, form)
      ElMessage.success('更新成功')
    } else {
      await createEthnicGroup(form)
      ElMessage.success('新增成功')
    }
    router.back()
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>
