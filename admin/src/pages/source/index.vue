<template>
  <div class="page-container">
    <div class="page-header">
      <h2>内容来源</h2>
      <el-button
        type="primary"
        :icon="Plus"
        @click="goEdit()"
        >新增来源</el-button
      >
    </div>

    <el-alert
      type="info"
      :closable="false"
      class="hint"
    >
      <template #title>
        站内数据的权威出处。来源以「发布机构 + 文档名 + 链接 + 采集方式」记录，
        并在 C 端各详情页的「参考资料」区块逐条展示。
        <b>删除来源会同时解除它与所有内容的关联</b>，请谨慎操作。
      </template>
    </el-alert>

    <el-form
      :inline="true"
      class="search-bar"
      @submit.prevent
    >
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="名称 / 机构"
          clearable
          style="width: 200px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
      </el-form-item>
      <el-form-item label="层级">
        <el-select
          v-model="query.sourceType"
          clearable
          placeholder="全部"
          style="width: 150px"
          @change="onSearch"
        >
          <el-option
            v-for="o in SOURCE_TYPE_OPTIONS"
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
        prop="orderNum"
        label="排序"
        width="70"
      />
      <el-table-column
        label="层级"
        width="110"
      >
        <template #default="{ row }">
          <StatusTag
            :label="TYPE_MAP[row.sourceType] || row.sourceType"
            :tag="row.sourceType === 'official' ? 'danger' : row.sourceType === 'open' ? 'success' : 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="publisherShort"
        label="机构"
        width="150"
      >
        <template #default="{ row }">{{ row.publisherShort || row.publisher || '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="name"
        label="来源名称"
        min-width="240"
        show-overflow-tooltip
      />
      <el-table-column
        label="采集方式"
        width="110"
      >
        <template #default="{ row }">{{ METHOD_MAP[row.collectMethod] || row.collectMethod || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="链接"
        width="80"
      >
        <template #default="{ row }">
          <el-link
            v-if="row.url"
            type="primary"
            :href="row.url"
            target="_blank"
            rel="noopener noreferrer"
            >打开</el-link
          >
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="200"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="goEdit(row as ContentSource)"
            >编辑</el-button
          >
          <el-button
            link
            type="info"
            @click="showUsage(row as ContentSource)"
            >引用数</el-button
          >
          <el-button
            link
            type="danger"
            @click="handleDelete(row as ContentSource)"
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
  deleteSource,
  listSources,
  sourceUsage,
  SOURCE_TYPE_OPTIONS,
  COLLECT_METHOD_OPTIONS,
  type ContentSource
} from '@/api/modules/bseries'
import StatusTag from '@/components/StatusTag.vue'

const TYPE_MAP: Record<string, string> = Object.fromEntries(
  SOURCE_TYPE_OPTIONS.map((o) => [o.value, o.label])
)
const METHOD_MAP: Record<string, string> = Object.fromEntries(
  COLLECT_METHOD_OPTIONS.map((o) => [o.value, o.label])
)

const router = useRouter()
const loading = ref(false)
const list = ref<ContentSource[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const query = ref<{ keyword: string; sourceType: string }>({ keyword: '', sourceType: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await listSources({
      keyword: query.value.keyword || undefined,
      sourceType: query.value.sourceType || undefined,
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
  query.value = { keyword: '', sourceType: '' }
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

function goEdit(row?: ContentSource) {
  router.push({ path: '/source/edit', query: row?.id ? { id: String(row.id) } : {} })
}

/** 查看该来源被多少条内容引用 —— 删除前评估影响面的依据 */
async function showUsage(row: ContentSource) {
  const n = await sourceUsage(String(row.id))
  ElMessage.info(`「${row.name}」被 ${n} 条内容引用`)
}

async function handleDelete(row: ContentSource) {
  const n = await sourceUsage(String(row.id)).catch(() => 0)
  await ElMessageBox.confirm(
    `确定删除来源「${row.name}」吗？` +
      (n > 0 ? `该来源正被 ${n} 条内容引用，删除后这些内容的「参考资料」将不再显示此来源。` : ''),
    '删除确认',
    { type: 'warning' }
  )
  await deleteSource(String(row.id))
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
