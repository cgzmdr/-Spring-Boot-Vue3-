<template>
  <div class="page-container">
    <div class="page-header">
      <h2>传统体育</h2>
      <el-button
        type="primary"
        :icon="Plus"
        @click="goEdit()"
        >新增项目</el-button
      >
    </div>

    <el-alert
      type="info"
      :closable="false"
      class="hint"
    >
      <template #title>
        内容主体为全国少数民族传统体育运动会竞赛项目。
        <b>不确定的字段请留空</b>（如入会年份、场地规格、参赛人数）——
        无依据的填充会在答辩与对外展示中造成事实性错误。
      </template>
    </el-alert>

    <el-form
      :inline="true"
      class="search-bar"
      @submit.prevent
    >
      <el-form-item label="名称">
        <el-input
          v-model="query.keyword"
          placeholder="如：珍珠球"
          clearable
          style="width: 180px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
      </el-form-item>
      <el-form-item label="类别">
        <el-select
          v-model="query.category"
          clearable
          placeholder="全部"
          style="width: 160px"
          @change="onSearch"
        >
          <el-option
            v-for="o in SPORT_CATEGORY_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          @click="onSearch"
          >查询</el-button
        >
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
    >
      <el-table-column
        prop="name"
        label="项目"
        width="140"
      />
      <el-table-column
        label="类别"
        width="110"
      >
        <template #default="{ row }">{{ CATEGORY_MAP[row.category] || row.category }}</template>
      </el-table-column>
      <el-table-column
        label="相关民族"
        min-width="160"
      >
        <template #default="{ row }">{{ parseList(row.ethnicOrigins).join('、') || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="子项"
        min-width="200"
      >
        <template #default="{ row }">
          <el-tag
            v-for="se in parseList(row.subEvents)"
            :key="se"
            size="small"
            style="margin-right: 4px"
            >{{ se }}</el-tag
          >
          <span v-if="!parseList(row.subEvents).length">-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="入会年份"
        width="100"
      >
        <template #default="{ row }">{{ row.firstEventYear ?? '-' }}</template>
      </el-table-column>
      <el-table-column
        label="相关非遗"
        width="130"
      >
        <template #default="{ row }">{{ row.heritageLink || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="140"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="goEdit(row as TraditionalSport)"
            >编辑</el-button
          >
          <el-button
            link
            type="danger"
            @click="handleDelete(row as TraditionalSport)"
            >删除</el-button
          >
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  deleteSport,
  listSports,
  SPORT_CATEGORY_OPTIONS,
  type TraditionalSport
} from '@/api/modules/bseries'

const CATEGORY_MAP: Record<string, string> = Object.fromEntries(
  SPORT_CATEGORY_OPTIONS.map((o) => [o.value, o.label])
)

const router = useRouter()
const loading = ref(false)
const list = ref<TraditionalSport[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const query = ref<{ keyword: string; category: string }>({ keyword: '', category: '' })

function parseList(v: string[] | string | null | undefined): string[] {
  if (!v) return []
  if (Array.isArray(v)) return v
  try {
    const p = JSON.parse(v)
    return Array.isArray(p) ? p : []
  } catch {
    return v ? [v] : []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listSports({
      keyword: query.value.keyword || undefined,
      category: query.value.category || undefined,
      page: page.value,
      size: size.value
    })
    list.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 0
  loadData()
}
function resetQuery() {
  query.value = { keyword: '', category: '' }
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

function goEdit(row?: TraditionalSport) {
  router.push({ path: '/sport/edit', query: row?.id ? { id: String(row.id) } : {} })
}

async function handleDelete(row: TraditionalSport) {
  await ElMessageBox.confirm(`确定删除项目「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteSport(String(row.id))
  ElMessage.success('删除成功')
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
.hint {
  margin-bottom: 12px;
}
</style>
