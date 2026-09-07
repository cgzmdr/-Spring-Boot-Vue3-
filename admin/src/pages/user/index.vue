<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-button v-if="isSuperAdmin" type="primary" @click="openCreate">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>新建用户
      </el-button>
    </div>

    <el-form :inline="true" class="search-bar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="账号 / 昵称 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
          @clear="loadData"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="头像" width="70">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatar">{{ row.nickname?.[0] || '?' }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="account" label="账号" min-width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="110" />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column label="角色" min-width="150">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="uuidToStr(r.id)" size="small" style="margin-right: 4px">
            {{ r.name || r.code }}
          </el-tag>
          <span v-if="!row.roles?.length" style="color: #9ca3af">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag
            :label="USER_STATUS_MAP[row.status]?.label || row.status || '-'"
            :tag="USER_STATUS_MAP[row.status]?.tag || 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" :width="isSuperAdmin ? 200 : 100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAssign(row as User)">分配角色</el-button>
          <template v-if="isSuperAdmin">
            <el-button link type="primary" @click="openEdit(row as User)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row as User)">删除</el-button>
          </template>
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

    <!-- 新建 / 编辑用户 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="520px" @closed="onFormClosed">
      <el-form label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="form.account" placeholder="登录账号（留空默认取昵称）" maxlength="64" />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="form.nickname" placeholder="昵称（4-16 位，支持中文/英文/数字/下划线）" maxlength="16" />
        </el-form-item>
        <el-form-item label="密码" :required="!isEdit">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空表示不修改密码' : '强密码：8 位以上且含大小写字母、数字、特殊字符'"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" placeholder="选填，可用于登录" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="选填，可用于登录" maxlength="128" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="active">正常</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="选择角色（不选默认普通用户）">
            <el-option
              v-for="r in roleOptions"
              :key="uuidToStr(r.id)"
              :label="r.name || r.code"
              :value="uuidToStr(r.id)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="assignVisible" title="分配角色" width="480px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ current?.nickname }}（{{ current?.account }}）</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple style="width: 100%" placeholder="选择角色">
            <el-option
              v-for="r in roleOptions"
              :key="uuidToStr(r.id)"
              :label="r.name || r.code"
              :value="uuidToStr(r.id)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignUserRoles,
  createUser,
  deleteUser,
  listRoles,
  listUsers,
  updateUser,
  type UserQuery,
  type UserSaveParams,
} from '@/api/modules/user'
import type { Role, User } from '@/api/types'
import { USER_STATUS_MAP } from '@/constants'
import StatusTag from '@/components/StatusTag.vue'
import { useUserStore } from '@/stores/user'
import { uuidToStr } from '@/utils/uuid'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref<User[]>([])
const total = ref(0)
const query = ref<UserQuery>({ keyword: '' })

/** 分页（0 基） */
const page = ref(0)
const size = ref(10)

/** 仅超级管理员可新建 / 编辑 / 删除用户 */
const isSuperAdmin = computed(() => userStore.hasRole('super_admin'))

/** 新建 / 编辑 */
const formVisible = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const form = ref<UserSaveParams>({ status: 'active', roleIds: [] })
const roleOptions = ref<Role[]>([])

/** 分配角色 */
const assignVisible = ref(false)
const current = ref<User | null>(null)
const selectedRoleIds = ref<string[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await listUsers({
      keyword: query.value.keyword || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roleOptions.value = (await listRoles()) || []
}

function resetForm() {
  form.value = { account: '', nickname: '', password: '', mobile: '', email: '', status: 'active', roleIds: [] }
  editingId.value = ''
}

function openCreate() {
  resetForm()
  isEdit.value = false
  formVisible.value = true
}

function openEdit(row: User) {
  resetForm()
  isEdit.value = true
  editingId.value = uuidToStr(row.id)
  form.value = {
    account: row.account || '',
    nickname: row.nickname || '',
    password: '',
    mobile: row.mobile || '',
    email: row.email || '',
    status: row.status === 'disabled' ? 'disabled' : 'active',
    roleIds: (row.roles || []).map((r) => uuidToStr(r.id)),
  }
  formVisible.value = true
}

function onFormClosed() {
  // 关闭后复位提交状态，避免下次打开仍处于 loading
  submitting.value = false
}

async function handleSave() {
  const f = form.value
  if (!f.nickname?.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  if (!isEdit.value && !f.password) {
    ElMessage.warning('请填写密码')
    return
  }
  if (f.password && !isEdit.value && !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/.test(f.password)) {
    ElMessage.warning('密码需 8 位以上且包含大小写字母、数字、特殊字符')
    return
  }
  submitting.value = true
  try {
    const payload: UserSaveParams = {
      account: f.account?.trim() || undefined,
      nickname: f.nickname.trim(),
      password: f.password || undefined,
      mobile: f.mobile?.trim() || undefined,
      email: f.email?.trim() || undefined,
      status: f.status,
      roleIds: f.roleIds || [],
    }
    if (isEdit.value) {
      await updateUser(editingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createUser(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: User) {
  await ElMessageBox.confirm(
    `确定删除用户「${row.nickname}（${row.account || row.nickname}）」吗？删除后不可恢复。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await deleteUser(uuidToStr(row.id))
  ElMessage.success('删除成功')
  // 删除当前页最后一条时回退一页
  if (list.value.length === 1 && page.value > 0) {
    page.value -= 1
  }
  loadData()
}

function openAssign(row: User) {
  current.value = row
  selectedRoleIds.value = (row.roles || []).map((r) => uuidToStr(r.id))
  assignVisible.value = true
}

async function handleAssign() {
  if (!current.value) return
  submitting.value = true
  try {
    await assignUserRoles(uuidToStr(current.value.id), selectedRoleIds.value)
    ElMessage.success('分配成功')
    assignVisible.value = false
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

onMounted(async () => {
  await loadRoles()
  await loadData()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  h2 {
    font-size: 18px;
    font-weight: 600;
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
</style>
