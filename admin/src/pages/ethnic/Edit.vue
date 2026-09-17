<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑民族' : '新增民族' }}</h2>
      <el-button :icon="Back" @click="router.back()">返回</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" v-loading="loading">
      <!-- 审批状态提示条：让编辑者知道当前内容处在审批闭环的哪一步 -->
      <el-alert
        v-if="isEdit && workflowTask"
        :type="workflowAlertType"
        :closable="false"
        show-icon
        class="workflow-alert"
      >
        <template #title>
          当前环节：{{ workflowTask.currentStageLabel }}（第 {{ workflowTask.contentVersion }} 版）
        </template>
        <div class="workflow-alert-body">
          <span v-if="workflowTask.currentStage === 'revising'">
            内容管理员审查发现问题，已在下方保留其审查意见，请修改后到「我的待办」提交二次审批。
          </span>
          <span v-else-if="workflowTask.currentStage === 'pending_review'">
            已提交，等待审核员审批。审批期间内容状态由流程控制，无法手动切换。
          </span>
          <span v-else-if="workflowTask.currentStage === 'pending_inspect'">
            已上线，等待内容管理员审查。
          </span>
          <el-button link type="primary" @click="goWorkflow">
            {{ workflowTask.actionable ? '去处理' : '查看进度' }}
          </el-button>
        </div>
      </el-alert>

      <!-- 前序审批/审查意见：内容编辑修改前必须能看到 -->
      <div v-if="isEdit && opinions.length" class="opinion-block">
        <el-divider content-position="left">审批 / 审查意见（供修改参考）</el-divider>
        <OpinionTimeline :opinions="opinions" />
      </div>

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
            <el-select v-model="form.status" style="width: 100%" :disabled="statusLocked">
              <el-option
                v-for="o in CONTENT_STATUS_OPTIONS.filter((x) => x.value)"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
            <span v-if="statusLocked" class="form-tip" style="margin-left: 8px">
              审批中，状态由流程控制
            </span>
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

        <!-- 审批闭环操作：保存后提交审批 / 修改完成后重新提交 -->
        <el-button
          v-if="isEdit && canSubmitForReview"
          type="success"
          :loading="submittingWorkflow"
          @click="handleSubmitForReview"
        >
          {{ form.status === 'offline' || form.status === 'rejected' ? '修改完成，重新提交审批' : '保存并提交审批' }}
        </el-button>
        <el-button
          v-else-if="isEdit && workflowTask && workflowTask.actionable"
          type="warning"
          :loading="submittingWorkflow"
          @click="goWorkflow"
        >
          去「我的待办」处理
        </el-button>

        <el-button @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import { createEthnicGroup, getEthnicGroup, updateEthnicGroup } from '@/api/modules/ethnic'
import {
  getActiveTask,
  listWorkflowOpinions,
  reviseAndResubmit,
  submitForReview,
  type WorkflowOpinion,
  type WorkflowTask
} from '@/api/modules/workflow'
import type { EthnicGroup } from '@/api/types'
import { CONTENT_STATUS_OPTIONS, LANGUAGE_FAMILY_OPTIONS } from '@/constants'
import ImageUpload from '@/components/ImageUpload.vue'
import OpinionTimeline from '@/components/OpinionTimeline.vue'

const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const submittingWorkflow = ref(false)
const isEdit = computed(() => !!route.query.id)

/** 当前活跃审批任务（有则表示内容正在流程中） */
const workflowTask = ref<WorkflowTask | null>(null)
/** 该内容全部历史审批/审查意见 */
const opinions = ref<WorkflowOpinion[]>([])

/** 审批中时状态字段由流程掌控，禁止手动切换（后端也会忽略该字段） */
const statusLocked = computed(
  () => !!workflowTask.value && workflowTask.value.status === 'running'
)

/** 可否从本页发起审批：编辑态、没有活跃流程、且当前不是待审核 */
const canSubmitForReview = computed(() => {
  if (!isEdit.value) return false
  if (workflowTask.value) return false
  return form.status !== 'pending'
})

const workflowAlertType = computed<'info' | 'warning' | 'success' | 'error'>(() => {
  switch (workflowTask.value?.currentStage) {
    case 'revising':
      return 'error'
    case 'pending_review':
      return 'warning'
    case 'pending_inspect':
      return 'info'
    default:
      return 'success'
  }
})

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
    const id = route.query.id as string
    const data = await getEthnicGroup(id)
    Object.assign(form, data, {
      region: toStr(data.region),
      languages: toStr(data.languages),
      scripts: toStr(data.scripts),
      religion: toStr(data.religion),
      tags: toStr(data.tags)
    })
    await loadWorkflow(id)
  } finally {
    loading.value = false
  }
}

/** 拉当前审批任务与历史意见（失败不阻塞编辑） */
async function loadWorkflow(id: string) {
  try {
    workflowTask.value = await getActiveTask('ethnic', id)
  } catch {
    workflowTask.value = null
  }
  try {
    opinions.value = (await listWorkflowOpinions('ethnic', id)) || []
  } catch {
    opinions.value = []
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

/**
 * 保存并提交审批。
 * 若内容处于「待修改」环节（内容管理员审查发现问题下线过），
 * 先保存内容，再调用工作流的「修改完成」动作把任务交回审核员二次审批。
 */
async function handleSubmitForReview() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const isRevision = form.status === 'offline' || form.status === 'rejected'
  try {
    await ElMessageBox.confirm(
      isRevision
        ? '将先保存本次修改，然后把任务重新提交给审核员二次审批。确定继续？'
        : '将先保存内容，然后提交进入审批流程（内容状态变为待审核）。确定继续？',
      isRevision ? '修改完成并重新提交' : '保存并提交审批',
      { type: 'info', confirmButtonText: '确定' }
    )
  } catch {
    return
  }

  submittingWorkflow.value = true
  try {
    const id = route.query.id as string
    // 审批中不允许改状态，这里保留原状态提交，避免把 offline 直接改成 published 绕过流程
    await updateEthnicGroup(id, { ...form, status: form.status } as Partial<EthnicGroup>)

    if (workflowTask.value && workflowTask.value.currentStage === 'revising') {
      await reviseAndResubmit(workflowTask.value.instanceId, '内容编辑已在内容编辑页完成修改', true)
      ElMessage.success('修改已保存并提交审核员二次审批')
    } else {
      await submitForReview('ethnic', id, '内容编辑提交审批')
      ElMessage.success('已提交审批，请到「我的待办」跟进')
    }
    await loadWorkflow(id)
  } finally {
    submittingWorkflow.value = false
  }
}

function goWorkflow() {
  if (!workflowTask.value) return
  router.push({ path: '/todo/detail', query: { instanceId: workflowTask.value.instanceId } })
}

onMounted(loadDetail)
</script>

<style scoped lang="scss">
.workflow-alert {
  margin-bottom: 16px;

  .workflow-alert-body {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 4px;
  }
}

.opinion-block {
  margin-bottom: 8px;
}
</style>
