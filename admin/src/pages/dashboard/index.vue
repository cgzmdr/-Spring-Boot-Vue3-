<template>
  <div>
    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="8" :md="4">
        <div class="stat-card" style="margin-bottom: 16px">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="page-container">
      <div class="page-header">
        <h2>内容维度统计</h2>
        <el-tag type="warning" v-if="overview?.pendingReviewCount">待审核 {{ overview.pendingReviewCount }} 条</el-tag>
      </div>
      <div ref="chartRef" style="height: 380px"></div>
    </div>

    <!-- 用户反馈：展示在内容维度统计下方（仅超级管理员可见） -->
    <div v-if="isSuperAdmin" class="page-container feedback-container">
      <div class="page-header">
        <h2>用户反馈</h2>
        <el-tag type="info">共 {{ feedbackTotal }} 条</el-tag>
      </div>
      <el-table :data="feedbackList" v-loading="feedbackLoading" border stripe size="small">
        <el-table-column label="称呼" width="110">
          <template #default="{ row }">{{ row.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="联系方式" width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.contact || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <StatusTag :label="TOPIC_LABEL[row.topic] || row.topic" :tag="TOPIC_TAG[row.topic] || 'info'" />
          </template>
        </el-table-column>
        <el-table-column label="满意度" width="90">
          <template #default="{ row }">{{ RATING_LABEL[row.rating] || row.rating || '-' }}</template>
        </el-table-column>
        <el-table-column label="反馈内容" prop="content" min-width="280" show-overflow-tooltip />
        <el-table-column label="访问日期" width="110">
          <template #default="{ row }">{{ row.visitDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!feedbackLoading && feedbackList.length === 0" description="暂无反馈" :image-size="72" />
      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :total="feedbackTotal"
          :current-page="fbPage + 1"
          :page-size="fbSize"
          :page-sizes="[5, 10, 20, 50]"
          @current-change="onFbPageChange"
          @size-change="onFbSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { getStatsContent, getStatsOverview, listFeedback } from '@/api/modules/stats'
import type { FeedbackItem, StatsContentItem, StatsOverview } from '@/api/types'
import StatusTag from '@/components/StatusTag.vue'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'

echarts.use([BarChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const userStore = useUserStore()

const loading = ref(false)
const overview = ref<StatsOverview | null>(null)
const contentStats = ref<StatsContentItem[]>([])
const chartRef = ref<HTMLElement>()
let chart: ECharts | null = null

/** 用户反馈 */
const isSuperAdmin = computed(() => userStore.hasRole('super_admin'))
const feedbackLoading = ref(false)
const feedbackList = ref<FeedbackItem[]>([])
const feedbackTotal = ref(0)
const fbPage = ref(0)
const fbSize = ref(5)

const TOPIC_LABEL: Record<string, string> = {
  correction: '内容纠错',
  suggestion: '建议',
  bug: '问题反馈'
}
const TOPIC_TAG: Record<string, string> = {
  correction: 'warning',
  suggestion: 'success',
  bug: 'danger'
}
const RATING_LABEL: Record<string, string> = {
  good: '满意',
  ok: '一般',
  bad: '不满意'
}

const TYPE_LABELS: Record<string, string> = {
  ethnic: '民族',
  festival: '节日',
  art: '艺术',
  topic: '专题'
}

const statCards = computed(() => [
  { label: '用户总数', value: overview.value?.userCount ?? '-', icon: 'User', color: '#3b82f6' },
  { label: '民族总数', value: overview.value?.ethnicCount ?? '-', icon: 'Flag', color: '#c0392b' },
  { label: '节日总数', value: overview.value?.festivalCount ?? '-', icon: 'Calendar', color: '#10b981' },
  { label: '艺术总数', value: overview.value?.artCount ?? '-', icon: 'Headset', color: '#f59e0b' },
  { label: '专题总数', value: overview.value?.topicCount ?? '-', icon: 'Collection', color: '#8b5cf6' },
  { label: '待审核', value: overview.value?.pendingReviewCount ?? '-', icon: 'DocumentChecked', color: '#ef4444' }
])

async function loadData() {
  loading.value = true
  try {
    const [ov, cs] = await Promise.all([getStatsOverview(), getStatsContent()])
    overview.value = ov
    contentStats.value = cs || []
    renderChart()
  } finally {
    loading.value = false
  }
}

async function loadFeedback() {
  feedbackLoading.value = true
  try {
    const res = await listFeedback({ page: fbPage.value, size: fbSize.value })
    feedbackList.value = res.data || []
    feedbackTotal.value = res.total || 0
  } finally {
    feedbackLoading.value = false
  }
}

function onFbPageChange(p: number) {
  fbPage.value = p - 1
  loadFeedback()
}

function onFbSizeChange(sz: number) {
  fbSize.value = sz
  fbPage.value = 0
  loadFeedback()
}

function renderChart() {
  if (!chartRef.value) return
  chart = chart || echarts.init(chartRef.value)
  const types = contentStats.value.map((i) => TYPE_LABELS[i.type] || i.type)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总数', '已发布', '草稿', '待审核', '已下线'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: types },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '总数', type: 'bar', data: contentStats.value.map((i) => i.total), itemStyle: { color: '#1f2937' } },
      { name: '已发布', type: 'bar', data: contentStats.value.map((i) => i.published), itemStyle: { color: '#10b981' } },
      { name: '草稿', type: 'bar', data: contentStats.value.map((i) => i.draft), itemStyle: { color: '#9ca3af' } },
      { name: '待审核', type: 'bar', data: contentStats.value.map((i) => i.pending), itemStyle: { color: '#f59e0b' } },
      { name: '已下线', type: 'bar', data: contentStats.value.map((i) => i.offline), itemStyle: { color: '#ef4444' } }
    ]
  })
}

function handleResize() {
  chart?.resize()
}

onMounted(() => {
  loadData()
  if (isSuperAdmin.value) {
    loadFeedback()
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.feedback-container {
  margin-top: 16px;
}

.feedback-container .pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
