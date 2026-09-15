<template>
  <div class="page-container">
    <div class="page-header">
      <h2>民族自治地方</h2>
      <el-button
        type="primary"
        :icon="Plus"
        @click="goEdit()"
        >新增自治地方</el-button
      >
    </div>

    <el-alert
      type="info"
      :closable="false"
      class="hint"
    >
      <template #title>
        全国共 <b>155 个</b>民族自治地方：5 个自治区 + 30 个自治州 + 120 个自治县（自治旗）。
        「自治民族」可填多个，用顿号或逗号分隔（如
        <code>拉祜族、佤族、布朗族、傣族</code>）。
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
          placeholder="如：延边"
          clearable
          style="width: 180px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
      </el-form-item>
      <el-form-item label="级别">
        <el-select
          v-model="query.level"
          clearable
          placeholder="全部"
          style="width: 170px"
          @change="onSearch"
        >
          <el-option
            v-for="o in AREA_LEVEL_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="省级行政区">
        <el-input
          v-model="query.province"
          placeholder="如：吉林省"
          clearable
          style="width: 160px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
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
        label="名称"
        min-width="220"
        show-overflow-tooltip
      />
      <el-table-column
        label="级别"
        width="130"
      >
        <template #default="{ row }">
          <StatusTag
            :label="LEVEL_MAP[row.level] || row.level"
            :tag="LEVEL_TAG[row.level] || 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="自治民族"
        min-width="180"
      >
        <template #default="{ row }">
          <span>{{ parseList(row.ethnicGroups).join('、') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="province"
        label="省级行政区"
        width="140"
      />
      <el-table-column
        label="成立年份"
        width="100"
      >
        <template #default="{ row }">{{ row.establishedYear ?? '-' }}</template>
      </el-table-column>
      <el-table-column
        label="行政中心"
        width="120"
      >
        <template #default="{ row }">{{ row.seat || '-' }}</template>
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
            @click="goEdit(row as AutonomousArea)"
            >编辑</el-button
          >
          <el-button
            link
            type="danger"
            @click="handleDelete(row as AutonomousArea)"
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
  deleteArea,
  listAreas,
  AREA_LEVEL_OPTIONS,
  type AutonomousArea
} from '@/api/modules/bseries'
import StatusTag from '@/components/StatusTag.vue'

const LEVEL_MAP: Record<string, string> = Object.fromEntries(
  AREA_LEVEL_OPTIONS.map((o) => [o.value, o.label])
)
const LEVEL_TAG: Record<string, string> = {
  autonomous_region: 'danger',
  autonomous_prefecture: 'warning',
  autonomous_county: 'info'
}

const router = useRouter()
const loading = ref(false)
const list = ref<AutonomousArea[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const query = ref<{ keyword: string; level: string; province: string }>({
  keyword: '',
  level: '',
  province: ''
})

/** 后端 jsonb 字段可能以字符串或数组返回，统一解析 */
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
    const res = await listAreas({
      keyword: query.value.keyword || undefined,
      level: query.value.level || undefined,
      province: query.value.province || undefined,
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
  query.value = { keyword: '', level: '', province: '' }
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

function goEdit(row?: AutonomousArea) {
  router.push({ path: '/area/edit', query: row?.id ? { id: String(row.id) } : {} })
}

async function handleDelete(row: AutonomousArea) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteArea(String(row.id))
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
