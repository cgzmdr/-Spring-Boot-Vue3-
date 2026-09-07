<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑节日' : '新增节日' }}</h2>
      <el-button :icon="Back" @click="router.back()">返回</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="loading">
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col :xs="24" :md="12">
          <el-form-item label="节日名称" prop="name">
            <el-input v-model="form.name" placeholder="如：雪顿节" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="英文名称">
            <el-input v-model="form.nameEn" placeholder="Xuedun Festival" />
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
          <el-form-item label="类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option
                v-for="o in FESTIVAL_TYPE_OPTIONS.filter((x) => x.value)"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="公历日期">
            <el-date-picker
              v-model="form.solarDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="农历日期">
            <el-input v-model="form.lunarDate" placeholder="如：藏历六月底至七月初" />
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
      <el-form-item label="起源传说">
        <el-input v-model="form.origin" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="节日介绍">
        <el-input v-model="form.description" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item label="习俗活动（逗号分隔）">
        <el-input v-model="form.customs" type="textarea" :rows="3" placeholder="晒佛, 看藏戏, 过林卡" />
      </el-form-item>
      <el-form-item label="图集 URL（逗号分隔）">
        <el-input v-model="form.images" type="textarea" :rows="3" placeholder="https://..., https://..." />
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
import { createFestival, getFestival, updateFestival } from '@/api/modules/festival'
import { listAllEthnicGroups } from '@/api/modules/ethnic'
import type { EthnicGroup, Festival, UuidLike } from '@/api/types'
import { CONTENT_STATUS_OPTIONS, FESTIVAL_TYPE_OPTIONS } from '@/constants'
import ImageUpload from '@/components/ImageUpload.vue'
import { uuidToStr, type UuidObject } from '@/utils/uuid'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

/** 民族下拉选项 */
const ethnicOptions = ref<{ value: string; label: string }[]>([])
/** 记录 idStr -> 原始 UUID 对象，提交时还原，避免精度丢失 */
const ethnicIdMap = new Map<string, UuidObject>()

const form = reactive<Partial<Festival>>({
  name: '',
  nameEn: '',
  ethnicGroupId: '',
  type: 'traditional',
  solarDate: '',
  lunarDate: '',
  origin: '',
  description: '',
  customs: '',
  images: '',
  coverImage: '',
  status: 'draft',
  orderNum: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入节日名称', trigger: 'blur' }],
  ethnicGroupId: [{ required: true, message: '请选择所属民族', trigger: 'change' }]
}

function toStr(v: unknown): string {
  if (Array.isArray(v)) return v.join(', ')
  return (v as string) || ''
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
    const data = await getFestival(route.query.id as string)
    const ethnicId = uuidToStr(data.ethnicGroupId as UuidLike)
    Object.assign(form, data, {
      ethnicGroupId: ethnicId,
      customs: toStr(data.customs),
      images: toStr(data.images)
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
    // 还原 ethnicGroupId 为后端 UUID 对象；无法还原时保留字符串兜底
    const payload: Partial<Festival> = { ...form }
    const rawId = ethnicIdMap.get(form.ethnicGroupId as string)
    payload.ethnicGroupId = (rawId || form.ethnicGroupId) as UuidLike
    if (isEdit.value) {
      await updateFestival(route.query.id as string, payload)
      ElMessage.success('更新成功')
    } else {
      await createFestival(payload)
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
