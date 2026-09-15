<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { interestTagApi, searchIndexApi } from '@/api/modules/search'
import type { IndexStats, InterestTag } from '@/api/modules/search'
import type { PageData } from '@/api/types'

/**
 * 检索与推荐管理（方向 D）。
 *
 * 两块内容：
 *  1. 检索索引 —— 展示各内容类型的收录条数，并可一键重建（内容批量维护后用）
 *  2. 兴趣标签 —— 推荐系统的显式信号，后台可增删改与启停
 */
const DOC_TYPE_LABEL: Record<string, string> = {
  ethnic: '民族',
  festival: '节日',
  art: '艺术',
  food: '美食',
  custom: '风俗',
  person: '人物',
  area: '自治地方',
  sport: '传统体育',
}
const DIMENSION_LABEL: Record<string, string> = {
  ethnic: '民族',
  region: '地域',
  type: '内容类型',
  topic: '主题',
}

const stats = ref<IndexStats | null>(null)
const statsLoading = ref(false)
const rebuilding = ref(false)

const tagPage = ref<PageData<InterestTag> | null>(null)
const tagLoading = ref(false)
const tagQuery = reactive({ dimension: '', keyword: '', page: 1, size: 20 })
const tagStats = ref<Record<string, number>>({})

async function loadStats() {
  statsLoading.value = true
  try {
    stats.value = await searchIndexApi.stats()
  } catch {
    ElMessage.error('索引统计加载失败')
  } finally {
    statsLoading.value = false
  }
}

async function rebuild(type?: string) {
  const label = type ? DOC_TYPE_LABEL[type] || type : '全部类型'
  try {
    await ElMessageBox.confirm(
      `确认重建「${label}」的检索索引？重建期间该类型内容短暂不可搜。`,
      '重建索引',
      { type: 'warning' },
    )
  } catch {
    return
  }
  rebuilding.value = true
  try {
    const r = await searchIndexApi.rebuild(type)
    ElMessage.success(`重建完成：${r.total} 条，耗时 ${r.elapsedMs} ms`)
    await loadStats()
  } catch {
    ElMessage.error('重建失败')
  } finally {
    rebuilding.value = false
  }
}

async function loadTags() {
  tagLoading.value = true
  try {
    tagPage.value = await interestTagApi.list({
      dimension: tagQuery.dimension || undefined,
      keyword: tagQuery.keyword || undefined,
      page: tagQuery.page - 1,
      size: tagQuery.size,
    })
  } catch {
    ElMessage.error('兴趣标签加载失败')
  } finally {
    tagLoading.value = false
  }
}

async function loadTagStats() {
  try {
    tagStats.value = await interestTagApi.stats()
  } catch {
    tagStats.value = {}
  }
}

/* ---------------- 标签编辑 ---------------- */
const dialogVisible = ref(false)
const form = reactive<InterestTag>({
  dimension: 'topic',
  name: '',
  nameEn: null,
  description: null,
  color: null,
  enabled: true,
  orderNum: 0,
})

function openCreate() {
  Object.assign(form, {
    id: undefined,
    dimension: 'topic',
    name: '',
    nameEn: null,
    description: null,
    color: null,
    enabled: true,
    orderNum: 0,
  })
  dialogVisible.value = true
}

function openEdit(row: InterestTag) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function saveTag() {
  if (!form.name.trim()) {
    ElMessage.warning('标签名不能为空')
    return
  }
  try {
    await interestTagApi.save({ ...form })
    ElMessage.success(form.id ? '已更新' : '已新增')
    dialogVisible.value = false
    await Promise.all([loadTags(), loadTagStats()])
  } catch {
    ElMessage.error('保存失败')
  }
}

async function removeTag(row: InterestTag) {
  try {
    await ElMessageBox.confirm(
      `删除标签「${row.name}」？该标签下所有用户的兴趣关联也会一并移除。`,
      '删除标签',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await interestTagApi.remove(row.id!)
    ElMessage.success('已删除')
    await Promise.all([loadTags(), loadTagStats()])
  } catch {
    ElMessage.error('删除失败')
  }
}

function onSearch() {
  tagQuery.page = 1
  loadTags()
}

onMounted(() => {
  loadStats()
  loadTags()
  loadTagStats()
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <h2>检索与推荐</h2>
      <p class="dek">
        检索索引把站内 8 类内容聚合成统一检索库；兴趣标签是个性化推荐的显式信号。
      </p>
    </div>

    <!-- 检索索引 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-head">
          <span>检索索引</span>
          <div class="head-actions">
            <el-button size="small" :loading="statsLoading" @click="loadStats">刷新统计</el-button>
            <el-button
              type="primary"
              size="small"
              :loading="rebuilding"
              @click="rebuild()"
            >
              全量重建
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 14px"
      >
        <template #title>
          索引覆盖 8 类内容，支持中文子串、拼音全拼（mengguzu）、拼音首字母（mgz）与英文检索。
          内容批量维护后点「全量重建」即可让新内容立即可搜。
        </template>
      </el-alert>

      <el-skeleton v-if="statsLoading && !stats" :rows="3" animated />

      <div v-else-if="stats" class="stat-grid">
        <div class="stat-total">
          <span class="st-num">{{ stats.total }}</span>
          <span class="st-label">收录总条数</span>
        </div>
        <div v-for="t in stats.byType" :key="t.docType" class="stat-item">
          <span class="si-num">{{ t.count }}</span>
          <span class="si-label">{{ DOC_TYPE_LABEL[t.docType] || t.docType }}</span>
          <el-button link size="small" @click="rebuild(t.docType)">重建</el-button>
        </div>
      </div>
    </el-card>

    <!-- 兴趣标签 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-head">
          <span>
            兴趣标签
            <el-tag
              v-for="(n, d) in tagStats"
              :key="d"
              size="small"
              type="info"
              style="margin-left: 8px"
            >
              {{ d === 'total' ? '合计' : DIMENSION_LABEL[d] || d }} {{ n }}
            </el-tag>
          </span>
          <div class="head-actions">
            <el-button type="primary" size="small" @click="openCreate">新增标签</el-button>
          </div>
        </div>
      </template>

      <div class="filters">
        <el-select v-model="tagQuery.dimension" placeholder="全部维度" clearable style="width: 140px" @change="onSearch">
          <el-option label="民族" value="ethnic" />
          <el-option label="地域" value="region" />
          <el-option label="内容类型" value="type" />
          <el-option label="主题" value="topic" />
        </el-select>
        <el-input
          v-model="tagQuery.keyword"
          placeholder="标签名 / 英文名"
          clearable
          style="width: 200px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>

      <el-table :data="tagPage?.data || []" v-loading="tagLoading" border size="small">
        <el-table-column label="维度" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ DIMENSION_LABEL[row.dimension] || row.dimension }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="标签名" min-width="130" />
        <el-table-column prop="nameEn" label="英文名" min-width="120" />
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="配色" width="80">
          <template #default="{ row }">
            <span v-if="row.color" class="swatch" :style="{ background: row.color }"></span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="70" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row as InterestTag)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeTag(row as InterestTag)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="tagPage && tagPage.total > tagQuery.size"
        style="margin-top: 14px; justify-content: flex-end"
        layout="total, prev, pager, next"
        :total="tagPage.total"
        :page-size="tagQuery.size"
        :current-page="tagQuery.page"
        @current-change="(p: number) => { tagQuery.page = p; loadTags() }"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑兴趣标签' : '新增兴趣标签'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="维度" required>
          <el-select v-model="form.dimension" :disabled="!!form.id" style="width: 100%">
            <el-option label="民族" value="ethnic" />
            <el-option label="地域" value="region" />
            <el-option label="内容类型" value="type" />
            <el-option label="主题" value="topic" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签名" required>
          <el-input v-model="form.name" :disabled="!!form.id" placeholder="如：藏族 / 西南 / 非遗技艺" />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="form.nameEn" placeholder="可选" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="展示给用户，帮助理解标签含义" />
        </el-form-item>
        <el-form-item label="配色">
          <el-input v-model="form.color" placeholder="#B6402E（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch
            :model-value="form.enabled !== false"
            @update:model-value="(v: string | number | boolean) => (form.enabled = !!v)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 20px;
}
.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}
.dek {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.block {
  margin-bottom: 18px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}
.head-actions {
  display: flex;
  gap: 8px;
}
.stat-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.stat-total {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 12px 22px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
}
.st-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.1;
}
.st-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  min-width: 132px;
}
.si-num {
  font-size: 17px;
  font-weight: 600;
}
.si-label {
  font-size: 12.5px;
  color: var(--el-text-color-regular);
}
.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.swatch {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 3px;
  border: 1px solid var(--el-border-color);
}
.muted {
  color: var(--el-text-color-placeholder);
}
</style>
