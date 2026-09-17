<template>
  <div class="page-container">
    <div class="page-header">
      <h2>内容审核</h2>
      <div class="header-actions">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="header-tip"
          title="审批动作请到「我的待办」处理；此处用于查看全部审核态与流转进度"
        />
        <el-button :icon="List" type="primary" @click="router.push('/todo')">我的待办</el-button>
      </div>
    </div>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="审核状态">
        <el-radio-group v-model="query.status" @change="resetAndLoad">
          <el-radio-button v-for="o in REVIEW_STATUS_OPTIONS" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="内容类型">
        <el-select v-model="query.entryType" style="width: 140px" @change="resetAndLoad">
          <el-option
            v-for="o in ENTRY_TYPE_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="内容类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">
            {{ ENTRY_TYPE_MAP[row.entryType] || row.entryType || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="160">
        <template #default="{ row }">
          <el-link v-if="row.instanceId" type="primary" @click="goDetail(row as ReviewRecord)">
            {{ titleOf(row as ReviewRecord) }}
          </el-link>
          <span v-else>{{ shortId(row.entryId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="版本" width="80">
        <template #default="{ row }">
          <span v-if="row.contentVersion" class="version-badge">v{{ row.contentVersion }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="120">
        <template #default="{ row }">
          <StatusTag
            :label="REVIEW_STATUS_MAP[row.status]?.label || row.status || '-'"
            :tag="REVIEW_STATUS_MAP[row.status]?.tag || 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column label="最近意见" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.lastOpinion || row.rejectReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.instanceId" link type="primary" @click="goDetail(row as ReviewRecord)">
            查看过程
          </el-button>
          <el-button
            v-if="row.instanceId && row.status === 'revising'"
            link
            type="warning"
            @click="goDetail(row as ReviewRecord)"
          >
            去修改
          </el-button>
          <span v-if="!row.instanceId" class="muted">历史数据</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="result-count">共 {{ total }} 条</div>
    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :total="total"
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { List, Refresh } from '@element-plus/icons-vue'
import { listReviews, type ReviewQuery } from '@/api/modules/review'
import type { ReviewRecord } from '@/api/types'
import {
  ENTRY_TYPE_MAP,
  ENTRY_TYPE_OPTIONS,
  REVIEW_STATUS_MAP,
  REVIEW_STATUS_OPTIONS
} from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import { formatDateTime } from '@/utils/format'
import { uuidToStr } from '@/utils/uuid'

const router = useRouter()
const loading = ref(false)
const list = ref<ReviewRecord[]>([])
const total = ref(0)
const query = ref<ReviewQuery>({ status: 'pending', entryType: '' })

/** 分页（0 基） */
const page = ref(0)
const size = ref(10)

/** entryId -> 标题缓存（列表里只有 ID，需要展示可读标题） */
const titleCache = ref<Record<string, string>>({})

async function loadData() {
  loading.value = true
  try {
    const res = await listReviews({
      status: query.value.status || undefined,
      entryType: query.value.entryType || undefined,
      page: page.value,
      size: size.value
    })
    list.value = res.data || []
    total.value = res.total || 0
    await loadTitles()
  } finally {
    loading.value = false
  }
}

/**
 * 批量补标题：审核记录只存 entryId，列表要显示内容名。
 * 按类型分组并发拉一次列表，用 ID 建索引，避免逐条请求。
 */
async function loadTitles() {
  const missing = list.value.filter(
    (r) => r.entryType === 'ethnic' && !titleCache.value[uuidToStr(r.entryId)]
  )
  if (!missing.length) return
  try {
    const { listEthnicGroups } = await import('@/api/modules/ethnic')
    const res = await listEthnicGroups({ page: 0, size: 200 })
    const map: Record<string, string> = { ...titleCache.value }
    ;(res.data || []).forEach((e) => {
      map[uuidToStr(e.id)] = e.name
    })
    titleCache.value = map
  } catch {
    /* 标题补全失败不阻塞列表 */
  }
}

function titleOf(row: ReviewRecord): string {
  return titleCache.value[uuidToStr(row.entryId)] || shortId(row.entryId)
}

function shortId(id: unknown): string {
  const s = uuidToStr(id as never)
  return s ? `${s.slice(0, 8)}…` : '-'
}

function resetAndLoad() {
  page.value = 0
  loadData()
}

function onPageChange(p: number) {
  page.value = p - 1
  loadData()
}

function onSizeChange(sz: number) {
  size.value = sz
  page.value = 0
  loadData()
}

function goDetail(row: ReviewRecord) {
  if (!row.instanceId) return
  router.push({ path: '/todo/detail', query: { instanceId: uuidToStr(row.instanceId) } })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  .header-actions {
    display: flex;
    align-items: center;
    gap: 10px;

    .header-tip {
      padding: 4px 10px;
      width: auto;
    }
  }
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
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

.muted {
  color: #9ca3af;
  font-size: 13px;
}
</style>
