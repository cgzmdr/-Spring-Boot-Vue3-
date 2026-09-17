<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <h2>
        审核处理
        <el-tag v-if="task" size="small" type="info" effect="plain" class="ml">
          {{ task.entryTypeLabel }} · {{ task.entryTitle }}
        </el-tag>
        <el-tag v-if="task" size="small" class="ml" effect="dark">
          第 {{ task.contentVersion }} 版
        </el-tag>
      </h2>
      <div class="header-actions">
        <el-button :icon="Back" @click="router.back()">返回</el-button>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="!task" description="未找到该工作流实例" />

    <template v-else>
      <!-- 流程进度：把闭环画出来，让使用者一眼看到当前在哪一步 -->
      <el-card shadow="never" class="stage-card">
        <div class="stage-flow">
          <template v-for="(s, i) in stageFlow" :key="s.key">
            <div
              class="stage-node"
              :class="{
                'is-active': s.key === task.currentStage,
                'is-done': s.done
              }"
            >
              <el-icon><component :is="s.icon" /></el-icon>
              <span>{{ s.label }}</span>
            </div>
            <el-icon v-if="i < stageFlow.length - 1" class="stage-arrow"><Right /></el-icon>
          </template>
        </div>
        <div class="stage-meta">
          <span>流程实例：{{ task.processDefinitionId }} v{{ task.processDefinitionVersion }}</span>
          <el-divider direction="vertical" />
          <span>提交人：{{ task.submitterName || '-' }}</span>
          <el-divider direction="vertical" />
          <span>提交时间：{{ formatDateTime(task.startedAt) }}</span>
          <el-divider direction="vertical" />
          <span>
            实例状态：
            <StatusTag
              :label="WORKFLOW_INSTANCE_STATUS_MAP[task.status]?.label || task.status"
              :tag="WORKFLOW_INSTANCE_STATUS_MAP[task.status]?.tag || 'info'"
            />
          </span>
        </div>
      </el-card>

      <el-row :gutter="16">
        <!-- 左：处理区（Camunda Form + 操作按钮） -->
        <el-col :xs="24" :lg="14">
          <el-card shadow="never" class="op-card">
            <template #header>
              <div class="card-head">
                <span>当前环节：{{ task.currentStageLabel }}</span>
                <el-tag v-if="task.formId" size="small" type="success" effect="plain">
                  Camunda Form：{{ task.formId }}
                </el-tag>
              </div>
            </template>

            <el-alert
              v-if="!isRunning"
              type="info"
              :closable="false"
              show-icon
              title="该流程已结束，以下为历史留档内容"
              class="mb"
            />

            <el-alert
              v-else-if="!task.actionable"
              type="warning"
              :closable="false"
              show-icon
              title="当前登录用户无权处理该环节，仅可查看"
              class="mb"
            />

            <!-- Camunda Form：由 form-js 渲染器直接渲染引擎里的表单 schema -->
            <CamundaFormRenderer
              v-if="isRunning && task.actionable"
              ref="formRef"
              :schema="task.formSchema"
              :data="formInitialData"
            />

            <!-- 未配置表单时的兜底：内置操作表单（仅用于「审批/审查」两类动作） -->
            <el-form v-if="isRunning && task.actionable" label-width="90px" class="fallback-form">
              <el-form-item :label="opinionLabel" required>
                <el-input
                  v-model="opinion"
                  type="textarea"
                  :rows="4"
                  :placeholder="opinionPlaceholder"
                />
              </el-form-item>

              <el-form-item v-if="task.currentStage === 'revising'" label="再次审批">
                <el-switch v-model="needReapproval" active-text="交审核员二次审批" />
              </el-form-item>
            </el-form>

            <div v-if="isRunning && task.actionable" class="action-bar">
              <template v-if="task.currentStage === 'pending_review'">
                <el-button type="success" :loading="submitting" @click="handleApprove">
                  审批通过并上线
                </el-button>
                <el-button type="danger" :loading="submitting" @click="handleReject">
                  退回内容编辑修改
                </el-button>
              </template>

              <template v-else-if="task.currentStage === 'pending_inspect'">
                <el-button type="success" :loading="submitting" @click="handleInspectPass">
                  审查通过（保持在线）
                </el-button>
                <el-button type="danger" :loading="submitting" @click="handleInspectIssue">
                  发现问题并暂时下线
                </el-button>
              </template>

              <template v-else-if="task.currentStage === 'revising'">
                <el-button type="primary" :loading="submitting" @click="handleRevise">
                  {{ needReapproval ? '修改完成并提交二次审批' : '修改完成并直接重新上线' }}
                </el-button>
              </template>
            </div>

            <!-- 内容编辑在本环节需要方便地跳到内容编辑页 -->
            <div v-if="isRunning && task.currentStage === 'revising'" class="edit-hint">
              <el-button link type="primary" :icon="EditPen" @click="goEditContent">
                先去编辑「{{ task.entryTitle }}」内容
              </el-button>
              <span class="hint-text">（先改内容，再回到这里填写修改说明并提交）</span>
            </div>
          </el-card>

          <!-- 内容概要 -->
          <el-card shadow="never" class="op-card">
            <template #header>
              <div class="card-head">
                <span>内容概要</span>
                <el-button link type="primary" @click="goEditContent">查看/编辑内容</el-button>
              </div>
            </template>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="内容类型">
                {{ task.entryTypeLabel }}
              </el-descriptions-item>
              <el-descriptions-item label="内容版本">v{{ task.contentVersion }}</el-descriptions-item>
              <el-descriptions-item label="内容状态">
                <StatusTag
                  :label="contentStatusLabel"
                  :tag="contentStatusTag"
                />
              </el-descriptions-item>
              <el-descriptions-item label="业务键">
                {{ task.instanceId }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- 右：历史意见（含前序所有版本的审批/审查意见） -->
        <el-col :xs="24" :lg="10">
          <el-card shadow="never" class="op-card">
            <template #header>
              <div class="card-head">
                <span>审批 / 审查意见</span>
                <el-tag size="small" type="info" effect="plain">
                  共 {{ task.opinions.length }} 条
                </el-tag>
              </div>
            </template>
            <el-alert
              type="info"
              :closable="false"
              class="mb"
              title="内容编辑可在此查看前序全部审批意见与审查意见，按意见修改后再次提交"
              show-icon
            />
            <OpinionTimeline :opinions="task.opinions" :highlight-version="task.contentVersion" />
          </el-card>

          <!-- 版本历史：体现「第一版 / 第二版 ……」的迭代 -->
          <el-card shadow="never" class="op-card">
            <template #header>
              <div class="card-head">
                <span>版本与流转历史</span>
              </div>
            </template>
            <el-table :data="timeline?.instances || []" size="small" border>
              <el-table-column label="版本" width="70">
                <template #default="{ row }">v{{ row.contentVersion }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <StatusTag
                    :label="row.statusLabel"
                    :tag="WORKFLOW_INSTANCE_STATUS_MAP[row.status]?.tag || 'info'"
                  />
                </template>
              </el-table-column>
              <el-table-column label="环节" min-width="130">
                <template #default="{ row }">{{ row.currentStageLabel }}</template>
              </el-table-column>
              <el-table-column label="提交时间" width="150">
                <template #default="{ row }">{{ formatDateTime(row.startedAt) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, EditPen, Refresh, Right } from '@element-plus/icons-vue'
import {
  approveWorkflowTask,
  getWorkflowTask,
  getWorkflowTimeline,
  inspectIssue,
  inspectPass,
  rejectWorkflowTask,
  reviseAndResubmit,
  type WorkflowTask,
  type WorkflowTimeline
} from '@/api/modules/workflow'
import {
  CONTENT_STATUS_MAP,
  ENTRY_TYPE_ROUTE,
  WORKFLOW_INSTANCE_STATUS_MAP
} from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import OpinionTimeline from '@/components/OpinionTimeline.vue'
import CamundaFormRenderer from '@/components/CamundaFormRenderer.vue'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const task = ref<WorkflowTask | null>(null)
const timeline = ref<WorkflowTimeline | null>(null)
/** 内容概要（各内容类型的公共字段） */
interface ContentBrief {
  name?: string
  title?: string
  summary?: string
  description?: string
  status?: string
}

const content = ref<ContentBrief>({})

const opinion = ref('')
const needReapproval = ref(true)
const formRef = ref<InstanceType<typeof CamundaFormRenderer> | null>(null)

const instanceId = computed(() => String(route.query.instanceId || ''))
const isRunning = computed(() => task.value?.status === 'running')

/** 内容状态展示（内容类型不同，status 字段可能缺省） */
const contentStatusLabel = computed(() => {
  const s = content.value.status || ''
  return CONTENT_STATUS_MAP[s]?.label || s || '-'
})

const contentStatusTag = computed(
  () =>
    (CONTENT_STATUS_MAP[content.value.status || '']?.tag || 'info') as
      | 'primary'
      | 'success'
      | 'warning'
      | 'info'
      | 'danger'
)

/** 当前环节对应的意见标签 */
const opinionLabel = computed(() => {
  switch (task.value?.currentStage) {
    case 'pending_inspect':
      return '审查意见'
    case 'revising':
      return '修改说明'
    default:
      return '审批意见'
  }
})

const opinionPlaceholder = computed(() => {
  switch (task.value?.currentStage) {
    case 'pending_inspect':
      return '请填写审查意见：无问题则通过；发现问题请写明需修改之处（内容将暂时下线）'
    case 'revising':
      return '请说明本次针对前面的审批意见 / 审查意见做了哪些修改'
    default:
      return '请填写审批意见（会留档并展示给内容编辑）'
  }
})

/** 传给 Camunda Form 的初始数据（把内容信息带进去，表单可只读展示） */
const formInitialData = computed(() => ({
  entryTitle: task.value?.entryTitle || '',
  entryTypeLabel: task.value?.entryTypeLabel || '',
  contentVersion: task.value?.contentVersion || 0,
  ...content.value
}))

/** 闭环进度条 */
const stageFlow = computed(() => {
  const stage = task.value?.currentStage
  const done = (k: string) => {
    const order = ['pending_review', 'pending_inspect', 'revising', 'published']
    return order.indexOf(stage || '') > order.indexOf(k)
  }
  return [
    { key: 'pending_review', label: '审核员审批', icon: 'DocumentChecked', done: done('pending_review') },
    { key: 'pending_inspect', label: '内容管理员审查', icon: 'View', done: done('pending_inspect') },
    { key: 'revising', label: '内容编辑修改', icon: 'EditPen', done: done('revising') },
    { key: 'published', label: '已上线', icon: 'CircleCheck', done: stage === 'published' }
  ]
})

async function loadAll() {
  if (!instanceId.value) return
  loading.value = true
  try {
    task.value = await getWorkflowTask(instanceId.value)
    opinion.value = ''
    needReapproval.value = true
    if (task.value) {
      timeline.value = await getWorkflowTimeline(task.value.entryType, task.value.entryId)
      await loadContent(task.value.entryType, task.value.entryId)
    }
  } catch {
    task.value = null
  } finally {
    loading.value = false
  }
}

/** 拉内容概要（按内容类型走对应的后台详情接口） */
async function loadContent(entryType: string, entryId: string) {
  content.value = {}
  try {
    if (entryType === 'ethnic') {
      const { getEthnicGroup } = await import('@/api/modules/ethnic')
      const d = await getEthnicGroup(entryId)
      content.value = { name: d.name, summary: d.summary }
    } else if (entryType === 'festival') {
      const { getFestival } = await import('@/api/modules/festival')
      const d = await getFestival(entryId)
      content.value = { name: d.name, description: d.description }
    } else if (entryType === 'art') {
      const { getArt } = await import('@/api/modules/art')
      const d = await getArt(entryId)
      content.value = { name: d.name, description: d.description }
    } else if (entryType === 'topic') {
      const { getTopic } = await import('@/api/modules/topic')
      const d = await getTopic(entryId)
      content.value = { title: d.title, description: d.description }
    }
  } catch {
    /* 内容概要失败不阻塞审批操作 */
  }
}

function goEditContent() {
  if (!task.value) return
  const base = ENTRY_TYPE_ROUTE[task.value.entryType]
  if (base) {
    router.push({ path: `${base}/edit`, query: { id: task.value.entryId } })
  }
}

/** 读取意见：优先 Camunda Form 的值，其次内置 textarea */
function resolveOpinion(): string {
  const fromForm = (formRef.value?.getData?.() || {}) as Record<string, unknown>
  const candidates = ['approvalOpinion', 'inspectionOpinion', 'revisionNote', 'opinion']
  for (const key of candidates) {
    const v = fromForm[key]
    if (typeof v === 'string' && v.trim()) return v.trim()
  }
  return opinion.value.trim()
}

/** Camunda Form 提交（含必填校验） */
function submitForm(): { data: Record<string, unknown>; error: string | null } {
  if (formRef.value) {
    const result = formRef.value.submit()
    if (result.error) return result
    return result
  }
  return { data: {}, error: null }
}

function requireOpinion(label: string): string | null {
  const text = resolveOpinion()
  if (!text) {
    ElMessage.warning(`请填写${label}`)
    return null
  }
  return text
}

async function handleApprove() {
  if (!task.value) return
  const { error } = submitForm()
  if (error) {
    ElMessage.warning(error)
    return
  }
  const text = requireOpinion('审批意见')
  if (!text) return
  submitting.value = true
  try {
    await approveWorkflowTask(task.value.instanceId, text)
    ElMessage.success('审批通过，内容已上线并流转到内容管理员审查')
    await loadAll()
  } finally {
    submitting.value = false
  }
}

async function handleReject() {
  if (!task.value) return
  const text = requireOpinion('审批意见（退回必须说明原因）')
  if (!text) return
  submitting.value = true
  try {
    await rejectWorkflowTask(task.value.instanceId, text)
    ElMessage.success('已退回内容编辑修改')
    await loadAll()
  } finally {
    submitting.value = false
  }
}

async function handleInspectPass() {
  if (!task.value) return
  const { error } = submitForm()
  if (error) {
    ElMessage.warning(error)
    return
  }
  const text = resolveOpinion()
  submitting.value = true
  try {
    await inspectPass(task.value.instanceId, text || '审查通过，无问题')
    ElMessage.success('审查通过，本轮审批闭环结束，内容保持在线')
    await loadAll()
  } finally {
    submitting.value = false
  }
}

async function handleInspectIssue() {
  if (!task.value) return
  const text = requireOpinion('审查意见（说明问题所在）')
  if (!text) return
  await ElMessageBox.confirm(
    '确认后将把该内容暂时下线，并交给内容编辑修改。',
    '暂时下线确认',
    { type: 'warning' }
  )
  submitting.value = true
  try {
    await inspectIssue(task.value.instanceId, text)
    ElMessage.success('内容已暂时下线，待内容编辑修改')
    await loadAll()
  } finally {
    submitting.value = false
  }
}

async function handleRevise() {
  if (!task.value) return
  const { error } = submitForm()
  if (error) {
    ElMessage.warning(error)
    return
  }
  const text = requireOpinion('修改说明')
  if (!text) return
  submitting.value = true
  try {
    await reviseAndResubmit(task.value.instanceId, text, needReapproval.value)
    ElMessage.success(
      needReapproval.value ? '已提交审核员二次审批' : '已重新上线并进入内容管理员审查'
    )
    await loadAll()
  } finally {
    submitting.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    display: flex;
    align-items: center;

    .ml {
      margin-left: 8px;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.stage-card {
  margin-bottom: 16px;

  .stage-flow {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    padding: 4px 0 12px;

    .stage-node {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      border-radius: 20px;
      background: #f3f4f6;
      color: #9ca3af;
      font-size: 13px;
      border: 1px solid transparent;
      transition: all 0.2s;

      &.is-done {
        background: #ecfdf5;
        color: #059669;
      }

      &.is-active {
        background: #eff6ff;
        color: #2563eb;
        border-color: #93c5fd;
        font-weight: 600;
      }
    }

    .stage-arrow {
      color: #d1d5db;
    }
  }

  .stage-meta {
    font-size: 12px;
    color: #6b7280;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
}

.op-card {
  margin-bottom: 16px;

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
  }
}

.mb {
  margin-bottom: 12px;
}

.fallback-form {
  margin-top: 8px;
}

.action-bar {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.edit-hint {
  margin-top: 10px;
  font-size: 13px;

  .hint-text {
    color: #9ca3af;
    margin-left: 4px;
  }
}
</style>
