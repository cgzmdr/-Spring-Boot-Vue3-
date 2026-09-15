<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑内容来源' : '新增内容来源' }}</h2>
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
      <el-divider content-position="left">来源标识</el-divider>
      <el-row :gutter="24">
        <el-col :span="24">
          <el-form-item
            label="来源名称"
            prop="name"
          >
            <el-input
              v-model="form.name"
              placeholder="如：国家民委《中华各民族》栏目"
            />
            <div class="form-tip">唯一。页面「参考资料」中显示的主标题。</div>
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="层级">
            <el-select
              v-model="form.sourceType"
              style="width: 100%"
            >
              <el-option
                v-for="o in SOURCE_TYPE_OPTIONS"
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
          <el-form-item label="采集方式">
            <el-select
              v-model="form.collectMethod"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="o in COLLECT_METHOD_OPTIONS"
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
          <el-form-item label="发布机构">
            <el-input
              v-model="form.publisher"
              placeholder="全称，如：中华人民共和国国家民族事务委员会"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="12"
        >
          <el-form-item label="机构简称">
            <el-input
              v-model="form.publisherShort"
              placeholder="如：国家民委"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="文档 / 栏目名">
            <el-input
              v-model="form.documentTitle"
              placeholder="如：《中国人口普查年鉴-2020》表 2-1《全国各民族人口及比重》"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="18"
        >
          <el-form-item label="原始链接">
            <el-input
              v-model="form.url"
              placeholder="https://…（纸质名录等无链接可留空）"
            />
          </el-form-item>
        </el-col>
        <el-col
          :xs="24"
          :md="6"
        >
          <el-form-item label="排序">
            <el-input-number
              v-model="form.orderNum"
              :min="0"
              :max="999"
              style="width: 100%"
            />
            <div class="form-tip">数字小者靠前</div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-divider content-position="left">补充说明</el-divider>
      <el-form-item label="说明">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="4"
          placeholder="口径、校验方法、许可与署名要求等。此段会展示在 C 端「参考资料」区块中。"
        />
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
  createSource,
  getSource,
  updateSource,
  SOURCE_TYPE_OPTIONS,
  COLLECT_METHOD_OPTIONS,
  type ContentSource
} from '@/api/modules/bseries'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.query.id)

const form = reactive<ContentSource>({
  name: '',
  publisher: null,
  publisherShort: null,
  documentTitle: null,
  url: null,
  sourceType: 'official',
  collectMethod: null,
  remark: null,
  orderNum: 100
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入来源名称', trigger: 'blur' }]
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    Object.assign(form, await getSource(route.query.id as string))
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
      await updateSource(route.query.id as string, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createSource({ ...form })
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
