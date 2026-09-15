<template>
  <div class="page-container">
    <div class="page-header">
      <h2>人物档案</h2>
      <el-button
        type="primary"
        :icon="Plus"
        @click="goEdit()"
        >新增人物</el-button
      >
    </div>

    <el-alert type="info" :closable="false" class="hint">
      <template #title>
        人物档案承载「角色类型 / 领域 / 简介 / 生卒年」等扩展属性。
        <b>传承人姓名以「艺术 - 非遗项目」的传承人字段为准</b>，此处维护的是人物维度的补充信息；
        「姓名 + 民族」构成唯一键 —— 同名不同族会分别成条（如「马金山」同属回族花儿与东乡族花儿）。
      </template>
    </el-alert>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 民族"
          clearable
          style="width: 180px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
      </el-form-item>
      <el-form-item label="角色">
        <el-select
          v-model="query.roleType"
          clearable
          placeholder="全部"
          style="width: 160px"
          @change="onSearch"
        >
          <el-option
            v-for="o in ROLE_TYPE_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="领域">
        <el-select
          v-model="query.domain"
          clearable
          placeholder="全部"
          style="width: 130px"
          @change="onSearch"
        >
          <el-option
            v-for="d in DOMAIN_OPTIONS"
            :key="d"
            :label="d"
            :value="d"
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
        prop="personName"
        label="姓名"
        width="130"
      />
      <el-table-column
        prop="ethnicGroupName"
        label="民族"
        width="110"
      >
        <template #default="{ row }">{{ row.ethnicGroupName || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="角色"
        width="130"
      >
        <template #default="{ row }">
          <StatusTag
            :label="ROLE_TYPE_MAP[row.roleType] || row.roleType"
            :tag="row.roleType === 'master' ? 'info' : 'success'"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="domain"
        label="领域"
        width="90"
      >
        <template #default="{ row }">{{ row.domain || '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="lifespan"
        label="生卒年"
        width="110"
      >
        <template #default="{ row }">{{ row.lifespan || '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="bio"
        label="简介"
        min-width="220"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ row.bio || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="更新时间"
        width="160"
      >
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
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
            @click="goEdit(row as PersonProfile)"
            >编辑</el-button
          >
          <el-button
            link
            type="danger"
            @click="handleDelete(row as PersonProfile)"
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
  deletePerson,
  listPersons,
  ROLE_TYPE_OPTIONS,
  DOMAIN_OPTIONS,
  type PersonProfile
} from '@/api/modules/bseries'
import StatusTag from '@/components/StatusTag.vue'
import { formatDateTime } from '@/utils/format'

const ROLE_TYPE_MAP: Record<string, string> = Object.fromEntries(
  ROLE_TYPE_OPTIONS.map((o) => [o.value, o.label])
)

const router = useRouter()
const loading = ref(false)
const list = ref<PersonProfile[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const query = ref<{ keyword: string; roleType: string; domain: string }>({
  keyword: '',
  roleType: '',
  domain: ''
})

async function loadData() {
  loading.value = true
  try {
    const res = await listPersons({
      keyword: query.value.keyword || undefined,
      roleType: query.value.roleType || undefined,
      domain: query.value.domain || undefined,
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
  query.value = { keyword: '', roleType: '', domain: '' }
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

function goEdit(row?: PersonProfile) {
  router.push({ path: '/person/edit', query: row?.id ? { id: String(row.id) } : {} })
}

async function handleDelete(row: PersonProfile) {
  await ElMessageBox.confirm(`确定删除人物档案「${row.personName}」吗？`, '删除确认', {
    type: 'warning'
  })
  await deletePerson(String(row.id))
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
