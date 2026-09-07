<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑艺术' : '新增艺术' }}</h2>
      <el-button :icon="Back" @click="router.back()">返回</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="loading">
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <el-form-item label="艺术名称" prop="name">
            <el-input v-model="form.name" placeholder="如：十二木卡姆" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="英文名称">
            <el-input v-model="form.nameEn" placeholder="Muqam" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="所属民族" prop="ethnicGroupId">
            <el-select
              v-model="form.ethnicGroupId"
              style="width: 100%"
              filterable
              placeholder="选择民族"
            >
              <el-option
                v-for="g in ethnicOptions"
                :key="g.value"
                :label="g.label"
                :value="g.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="类别">
            <el-select v-model="form.category" style="width: 100%">
              <el-option
                v-for="o in ART_CATEGORY_OPTIONS.filter((x) => x.value)"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="非遗级别">
            <el-select v-model="form.intangibleHeritage" style="width: 100%" clearable>
              <el-option
                v-for="o in HERITAGE_OPTIONS.filter((x) => x.value)"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="传承人（逗号分隔）">
            <el-input v-model="form.inheritors" placeholder="如：热合木都拉·艾依提" />
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
      <el-form-item label="艺术介绍">
        <el-input v-model="form.description" type="textarea" :rows="6" />
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
import { createArt, getArt, updateArt } from '@/api/modules/art'
import { listAllEthnicGroups } from '@/api/modules/ethnic'
import type { Art, EthnicGroup, UuidLike } from '@/api/types'
import { ART_CATEGORY_OPTIONS, CONTENT_STATUS_OPTIONS, HERITAGE_OPTIONS } from '@/constants'
import ImageUpload from '@/components/ImageUpload.vue'
import { uuidToStr, type UuidObject } from '@/utils/uuid'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const ethnicOptions = ref<{ value: string; label: string }[]>([])
const ethnicIdMap = new Map<string, UuidObject>()

const form = reactive<Partial<Art>>({
  name: '',
  nameEn: '',
  ethnicGroupId: '',
  category: 'music',
  intangibleHeritage: '',
  description: '',
  inheritors: '',
  coverImage: '',
  status: 'draft',
  orderNum: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入艺术名称', trigger: 'blur' }],
  ethnicGroupId: [{ required: true, message: '请选择所属民族', trigger: 'change' }]
}

async function loadEthnicOptions() {
  const all = await listAllEthnicGroups()
  ethnicOptions.value = all.map((g: EthnicGroup) => {
    const idStr = uuidToStr(g.id)
    if (g.id && typeof g.id !== 'string') ethnicIdMap.set(idStr, g.id as UuidObject)
    return { value: idStr, label: g.name }
  })
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getArt(route.query.id as string)
    Object.assign(form, data, {
      ethnicGroupId: uuidToStr(data.ethnicGroupId as UuidLike),
      inheritors: Array.isArray(data.inheritors)
        ? (data.inheritors as string[]).join(', ')
        : (data.inheritors as string) || ''
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
    const payload: Partial<Art> = { ...form }
    const rawId = ethnicIdMap.get(form.ethnicGroupId as string)
    payload.ethnicGroupId = (rawId || form.ethnicGroupId) as UuidLike
    if (isEdit.value) {
      await updateArt(route.query.id as string, payload)
      ElMessage.success('更新成功')
    } else {
      await createArt(payload)
      ElMessage.success('新增成功')
    }
    router.back()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadEthnicOptions()
  await loadDetail()
})
</script>
