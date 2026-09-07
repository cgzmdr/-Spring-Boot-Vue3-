<template>
  <div class="page-container">
    <div class="page-header">
      <h2>审核管理</h2>
    </div>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="状态">
        <el-radio-group v-model="query.status" @change="loadData">
          <el-radio-button v-for="o in REVIEW_STATUS_OPTIONS" :key="o.value" :value="o.value">
            {{ o.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="内容类型" width="110">
        <template #default="{ row }">{{ ENTRY_TYPE_MAP[row.entryType] || row.entryType || '-' }}</template>
      </el-table-column>
      <el-table-column prop="entryId" label="条目 ID" min-width="200">
        <template #default="{ row }">{{ uuidToStr(row.entryId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag
            :label="REVIEW_STATUS_MAP[row.status]?.label || row.status || '-'"
            :tag="REVIEW_STATUS_MAP[row.status]?.tag || 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column prop="rejectReason" label="驳回原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.reviewedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'pending'">
            <el-button link type="success" @click="handleApprove(row as ReviewRecord)">通过</el-button>
            <el-button link type="danger" @click="openReject(row as ReviewRecord)">驳回</el-button>
          </template>
          <span v-else style="color: #9ca3af; font-size: 13px">已处理</span>
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
  

    <el-dialog v-model="rejectVisible" title="驳回内容" width="480px">
      <el-form label-width="90px">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回原因（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { approveReview, listReviews, rejectReview, type ReviewQuery } from '@/api/modules/review'
import type { ReviewRecord } from '@/api/types'
import { REVIEW_STATUS_MAP, REVIEW_STATUS_OPTIONS } from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import { uuidToStr } from '@/utils/uuid'
import { formatDateTime } from '@/utils/format'

const ENTRY_TYPE_MAP: Record<string, string> = {
  ethnic: '民族',
  festival: '节日',
  art: '艺术',
  topic: '专题'
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<ReviewRecord[]>([])
const total = ref(0)
const query = ref<ReviewQuery>({ status: 'pending' })

/** 分页（0 基） */
const page = ref(0)
const size = ref(10)
const rejectVisible = ref(false)
const rejectReason = ref('')
let current: ReviewRecord | null = null

async function loadData() {
  loading.value = true
  try {
    const res = await listReviews({ status: query.value.status || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function handleApprove(row: ReviewRecord) {
  await approveReview(uuidToStr(row.id))
  ElMessage.success('已通过')
  loadData()
}

function openReject(row: ReviewRecord) {
  current = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!current) return
  submitting.value = true
  try {
    await rejectReview(uuidToStr(current.id), rejectReason.value.trim())
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
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



onMounted(loadData)
</script>

<style scoped lang="scss">
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
</style>
