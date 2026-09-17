<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的待办</h2>
      <div class="header-actions">
        <el-tag v-if="engineOk === false" type="danger" effect="dark" size="small">
          Camunda 引擎未连接
        </el-tag>
        <el-tag v-else-if="engineOk" type="success" effect="plain" size="small">
          Camunda {{ engineVersion }}
        </el-tag>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 环节筛选：对应审批闭环的三个角色环节 -->
    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="环节">
        <el-radio-group v-model="query.stage" @change="loadData">
          <el-radio-button v-for="o in WORKFLOW_STAGE_OPTIONS" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="内容类型">
        <el-select v-model="query.entryType" style="width: 140px" @change="loadData">
          <el-option
            v-for="o in ENTRY_TYPE_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="query.mineOnly" @change="loadData">仅显示我可处理</el-checkbox>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="内容类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.entryTypeLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="goDetail(row as WorkflowTask)">
            {{ row.entryTitle || '（未命名）' }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="版本" width="80">
        <template #default="{ row }">
          <span class="version-badge">v{{ row.contentVersion }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前环节" width="160">
        <template #default="{ row }">
          <StatusTag
            :label="row.currentStageLabel"
            :tag="WORKFLOW_STAGE_MAP[row.currentStage]?.tag || 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column label="流程任务" min-width="150">
        <template #default="{ row }">
          <div class="task-cell">
            <span>{{ row.taskName || '-' }}</span>
            <el-tag v-if="row.formId" size="small" type="info" effect="plain">Camunda Form</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="提交人" width="110">
        <template #default="{ row }">{{ row.submitterName || '-' }}</template>
      </el-table-column>
      <el-table-column label="最近意见" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ lastOpinion(row as WorkflowTask) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row as WorkflowTask)">
            {{ row.actionable ? '去处理' : '查看' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="result-count">
      共 {{ list.length }} 条待办
      <span v-if="!query.mineOnly" class="hint">（当前展示全部进行中的流程）</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import {
  getEngineTopology,
  listWorkflowTasks,
  type WorkflowStage,
  type WorkflowTask
} from '@/api/modules/workflow'
import {
  ENTRY_TYPE_OPTIONS,
  WORKFLOW_STAGE_MAP,
  WORKFLOW_STAGE_OPTIONS
} from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref<WorkflowTask[]>([])
const engineOk = ref<boolean | null>(null)
const engineVersion = ref('')
const query = ref<{ stage: WorkflowStage | ''; entryType: string; mineOnly: boolean }>({
  stage: '',
  entryType: '',
  mineOnly: false
})

async function loadData() {
  loading.value = true
  try {
    // 后端返回数组；此处兜底成 []，避免接口异常时模板里 .length 崩溃
    list.value = (await listWorkflowTasks({
      stage: query.value.stage || undefined,
      entryType: (query.value.entryType || undefined) as never,
      mineOnly: query.value.mineOnly
    })) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadEngine() {
  try {
    const topo = await getEngineTopology()
    engineOk.value = topo.healthy
    // Camunda 7 嵌入式引擎没有「网关」概念，这里展示引擎版本
    engineVersion.value = topo.version || ''
  } catch {
    engineOk.value = false
  }
}

/** 最近一条「审批/审查」意见，作为待办列表的上下文提示 */
function lastOpinion(row: WorkflowTask): string {
  const opinions = row.opinions || []
  for (let i = opinions.length - 1; i >= 0; i--) {
    const op = opinions[i]
    if (op.opinion) {
      return `[${op.stageLabel}] ${op.opinion}`
    }
  }
  return '-'
}

function goDetail(row: WorkflowTask) {
  router.push({ path: '/todo/detail', query: { instanceId: row.instanceId } })
}

onMounted(() => {
  loadData()
  loadEngine()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.search-bar {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 12px;
}

.result-count {
  margin-top: 12px;
  font-size: 13px;
  color: #6b7280;

  .hint {
    color: #9ca3af;
    margin-left: 6px;
  }
}

.version-badge {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 12px;
  font-weight: 600;
}

.task-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
