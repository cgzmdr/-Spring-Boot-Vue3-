<template>
  <div class="opinion-timeline">
    <el-empty v-if="!opinions.length" description="暂无审批/审查意见" :image-size="70" />

    <el-timeline v-else>
      <el-timeline-item
        v-for="op in sortedOpinions"
        :key="uuidToStr(op.id)"
        :timestamp="formatDateTime(op.createdAt)"
        :color="stageColor(op.stage)"
        placement="top"
      >
        <div class="opinion-card" :class="{ 'is-current': highlightVersion === op.contentVersion }">
          <div class="opinion-head">
            <el-tag :type="stageTag(op.stage)" size="small" effect="dark">              {{ op.stageLabel || OPINION_STAGE_MAP[op.stage]?.label || op.stage }}
            </el-tag>
            <el-tag
              v-if="op.decision"
              :type="(decisionTag(op.decision) as 'primary' | 'success' | 'warning' | 'info' | 'danger')"
              size="small"
              effect="plain"
            >
              {{ op.decisionLabel || OPINION_DECISION_MAP[op.decision]?.label || op.decision }}
            </el-tag>
            <el-tag size="small" type="info" effect="plain">第 {{ op.contentVersion }} 版</el-tag>
            <span class="opinion-meta">
              {{ op.operatorName || '系统' }}
              <template v-if="op.operatorRole && op.operatorRole !== 'system'">
                （{{ ROLE_LABEL[op.operatorRole] || op.operatorRole }}）
              </template>
            </span>
          </div>
          <div v-if="op.opinion" class="opinion-body">{{ op.opinion }}</div>
          <div v-else class="opinion-body is-empty">（未填写意见）</div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WorkflowOpinion } from '@/api/modules/workflow'
import { OPINION_DECISION_MAP, OPINION_STAGE_MAP, ROLE_LABEL } from '@/constants'
import { formatDateTime } from '@/utils/format'
import { uuidToStr } from '@/utils/uuid'

const props = withDefaults(
  defineProps<{
    opinions: WorkflowOpinion[]
    /** 高亮某个内容版本的意见 */
    highlightVersion?: number
  }>(),
  { highlightVersion: undefined }
)

/** 按时间正序展示（后端已排序，这里兜底） */
const sortedOpinions = computed(() =>
  [...props.opinions].sort((a, b) =>
    (a.createdAt || '').localeCompare(b.createdAt || '')
  )
)

function stageColor(stage: string): string {
  return OPINION_STAGE_MAP[stage]?.color || '#9ca3af'
}

function stageTag(stage: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  return (OPINION_STAGE_MAP[stage]?.tag || 'info') as 'primary' | 'success' | 'warning' | 'info' | 'danger'
}

function decisionTag(decision: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  return (OPINION_DECISION_MAP[decision]?.tag || 'info') as
    | 'primary'
    | 'success'
    | 'warning'
    | 'info'
    | 'danger'
}
</script>

<style scoped lang="scss">
.opinion-timeline {
  :deep(.el-timeline) {
    padding-left: 4px;
  }
}

.opinion-card {
  background: #fafafa;
  border: 1px solid #eef0f3;
  border-radius: 6px;
  padding: 10px 12px;

  &.is-current {
    background: #f0f7ff;
    border-color: #bfdbfe;
  }

  .opinion-head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 6px;

    .opinion-meta {
      font-size: 12px;
      color: #6b7280;
    }
  }

  .opinion-body {
    font-size: 13px;
    line-height: 1.7;
    color: #374151;
    white-space: pre-wrap;
    word-break: break-word;

    &.is-empty {
      color: #9ca3af;
      font-style: italic;
    }
  }
}
</style>
