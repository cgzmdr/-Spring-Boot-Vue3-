<template>
  <div class="page-container">
    <div class="page-header">
      <h2>艺术管理</h2>
      <el-button type="primary" :icon="Plus" @click="goEdit()">新增艺术</el-button>
    </div>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="艺术名称 / 传承人"
          clearable
          style="width: 200px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" style="width: 140px" @change="loadData">
          <el-option
            v-for="o in CONTENT_STATUS_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <img v-if="row.coverImage" :src="resolveImageUrl(row.coverImage)" class="table-thumb" alt="" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="艺术名称" min-width="140" />
      <el-table-column label="所属民族" min-width="100">
        <template #default="{ row }">{{ row.ethnicGroup?.name || '-' }}</template>
      </el-table-column>
      <el-table-column label="类别" width="90">
        <template #default="{ row }">{{ ART_CATEGORY_MAP[row.category] || '-' }}</template>
      </el-table-column>
      <el-table-column label="非遗级别" width="100">
        <template #default="{ row }">{{ HERITAGE_MAP[row.intangibleHeritage] || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag
            :label="CONTENT_STATUS_MAP[row.status]?.label || row.status || '-'"
            :tag="CONTENT_STATUS_MAP[row.status]?.tag || 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="浏览量" width="90">
        <template #default="{ row }">{{ row.viewCount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goEdit(row as Art)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row as Art)">删除</el-button>
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
import { deleteArt, listArts, type ArtQuery } from '@/api/modules/art'
import { getViewCounts } from '@/api/modules/stats'
import type { Art } from '@/api/types'
import { ART_CATEGORY_MAP, CONTENT_STATUS_MAP, CONTENT_STATUS_OPTIONS, HERITAGE_MAP } from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import { uuidToStr } from '@/utils/uuid'
import { formatDateTime, resolveImageUrl } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const list = ref<Art[]>([])
const total = ref(0)
const query = ref<ArtQuery>({ keyword: '', status: '' })

/** 分页（0 基） */
const page = ref(0)
const size = ref(10)

async function loadData() {
  loading.value = true
  try {
    const res = await listArts({
      keyword: query.value.keyword || undefined,
      status: query.value.status || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = res.data || []
    total.value = res.total || 0
    await loadViewCounts()
  } finally {
    loading.value = false
  }
}

/** 批量合并当前页艺术浏览量 */
async function loadViewCounts() {
  const ids = list.value.map((r) => uuidToStr(r.id))
  if (!ids.length) return
  try {
    const counts = await getViewCounts('art', ids)
    list.value.forEach((r) => {
      r.viewCount = counts[uuidToStr(r.id)] ?? 0
    })
  } catch {
    /* 浏览量加载失败不阻塞列表 */
  }
}

function resetQuery() {
  query.value = { keyword: '', status: '' }
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


function goEdit(row?: Art) {
  router.push({ path: '/art/edit', query: row ? { id: uuidToStr(row.id) } : {} })
}

async function handleDelete(row: Art) {
  await ElMessageBox.confirm(`确定删除艺术「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteArt(uuidToStr(row.id))
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
</style>
