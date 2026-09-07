<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { deleteForm, listForms, type FormQuery } from '@/api/modules/form'
import type { FormConfig } from '@/api/types'
import { uuidToStr } from '@/utils/uuid'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref<FormConfig[]>([])
const total = ref(0)
const query = ref<FormQuery>({ keyword: '', status: '' })

/** 分页（0 基） */
const page = ref(0)
const size = ref(10)

async function loadData() {
  loading.value = true
  try {
    const res = await listForms({
      keyword: query.value.keyword || undefined,
      status: query.value.status || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.value = { keyword: '', status: '' }
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

function goEdit(row?: FormConfig) {
  router.push({ path: '/form/edit', query: row ? { id: uuidToStr(row.id) } : {} })
}

async function handleDelete(row: FormConfig) {
  await ElMessageBox.confirm(`确定删除表单「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteForm(uuidToStr(row.id))
  ElMessage.success('删除成功')
  loadData()
}

function fieldCount(row: { schema?: string }): number {
  try {
    const sch = JSON.parse(row.schema || '{}')
    return Array.isArray(sch.fields) ? sch.fields.length : 0
  } catch {
    return 0
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>表单配置</h2>
      <el-button type="primary" :icon="Plus" @click="goEdit()">新增表单</el-button>
    </div>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="表单名称 / 编码"
          clearable
          style="width: 200px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" style="width: 140px" @change="loadData">
          <el-option label="全部" value="" />
          <el-option label="启用" value="active" />
          <el-option label="停用" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="表单名称" min-width="140" />
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column label="字段数" width="90">
        <template #default="{ row }">{{ fieldCount(row) }}</template>
      </el-table-column>
      <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
            {{ row.status === 'active' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goEdit(row as FormConfig)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row as FormConfig)">删除</el-button>
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

<style scoped lang="scss">
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

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
