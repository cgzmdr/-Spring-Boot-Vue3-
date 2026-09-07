<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right: 4px"><Plus /></el-icon>新增角色
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="角色名称" min-width="130" />
      <el-table-column prop="code" label="角色编码" min-width="130" />
      <el-table-column label="权限" min-width="300">
        <template #default="{ row }">
          <el-tag
            v-for="p in row.permissions?.slice(0, 5)"
            :key="uuidToStr(p.id)"
            size="small"
            type="info"
            style="margin-right: 4px"
          >
            {{ p.name || p.code }}
          </el-tag>
          <el-tag v-if="(row.permissions?.length || 0) > 5" size="small" type="warning">
            +{{ row.permissions!.length - 5 }}
          </el-tag>
          <span v-if="!row.permissions?.length" style="color: #9ca3af">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAssign(row as Role)">分配权限</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="result-count">共 {{ total }} 条</div>

    <!-- 新增角色 -->
    <el-dialog v-model="createVisible" title="新增角色" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="角色编码" required>
          <el-input v-model="createForm.code" placeholder="如：content_admin" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="createForm.name" placeholder="如：内容管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="角色职责说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="assignVisible" title="分配权限" width="520px" @opened="onAssignOpened">
      <div class="assign-hint">
        角色：<b>{{ current?.name }}</b>（{{ current?.code }}）
      </div>
      <el-tree
        ref="permTreeRef"
        :data="permTree"
        :props="{ label: 'label', children: 'children' }"
        node-key="key"
        show-checkbox
        default-expand-all
        class="perm-tree"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { assignRolePermissions, createRole, listPermissions, listRoles } from '@/api/modules/user'
import type { Permission, Role } from '@/api/types'
import { uuidToStr } from '@/utils/uuid'
import { formatDateTime } from '@/utils/format'

/** 权限树节点 */
interface PermNode {
  key: string
  label: string
  children?: PermNode[]
  permission?: Permission
}

/** 权限 code 前缀 -> 模块名 */
const MODULE_LABEL: Record<string, string> = {
  ethnic: '民族管理',
  festival: '节日管理',
  art: '艺术管理',
  topic: '专题管理',
  review: '审核管理',
  user: '用户管理',
  role: '角色管理',
  stats: '统计报表',
  search: '搜索维护',
  content: '互动管理',
  share: '分享'
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<Role[]>([])
const total = ref(0)

const createVisible = ref(false)
const createForm = ref({ code: '', name: '', description: '' })

const assignVisible = ref(false)
const current = ref<Role | null>(null)
const permTree = ref<PermNode[]>([])
const permTreeRef = ref<{ setCheckedKeys: (keys: string[]) => void; getCheckedNodes: (leafOnly: boolean, includeHalfChecked: boolean) => PermNode[] }>()

async function loadData() {
  loading.value = true
  try {
    const roles = await listRoles()
    list.value = roles || []
    total.value = roles?.length || 0
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  const permissions = await listPermissions()
  const roots: PermNode[] = []
  const groupMap = new Map<string, PermNode>()
  ;(permissions || []).forEach((p) => {
    const idStr = uuidToStr(p.id)
    const code = p.code
    if (code === '*') {
      roots.push({ key: idStr, label: `${p.name}（${code}）`, permission: p })
      return
    }
    const prefix = code.split(':')[0]
    const groupLabel = MODULE_LABEL[prefix] || prefix
    let group = groupMap.get(prefix)
    if (!group) {
      group = { key: `group-${prefix}`, label: groupLabel, children: [] }
      groupMap.set(prefix, group)
      roots.push(group)
    }
    group.children!.push({ key: idStr, label: `${p.name}（${code}）`, permission: p })
  })
  permTree.value = roots
}

function openCreate() {
  createForm.value = { code: '', name: '', description: '' }
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.value.code.trim() || !createForm.value.name.trim()) {
    ElMessage.warning('请填写角色编码与名称')
    return
  }
  submitting.value = true
  try {
    await createRole({
      code: createForm.value.code.trim(),
      name: createForm.value.name.trim(),
      description: createForm.value.description.trim() || undefined
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openAssign(row: Role) {
  current.value = row
  assignVisible.value = true
}

function onAssignOpened() {
  const keys = (current.value?.permissions || []).map((p) => uuidToStr(p.id))
  nextTick(() => permTreeRef.value?.setCheckedKeys(keys))
}

function getCheckedPermissionIds(): string[] {
  const nodes = permTreeRef.value?.getCheckedNodes(false, false) || []
  return nodes.filter((n) => n.permission).map((n) => n.key)
}

async function handleAssign() {
  if (!current.value) return
  submitting.value = true
  try {
    // 直接传权限 ID 字符串数组（与后端 List<UUID> 契约一致）
    await assignRolePermissions(uuidToStr(current.value.id), getCheckedPermissionIds())
    ElMessage.success('分配成功')
    assignVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadPermissions()
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

.result-count {
  margin-top: 12px;
  font-size: 13px;
  color: #6b7280;
}

.assign-hint {
  margin-bottom: 12px;
  font-size: 14px;
  color: #374151;
}

.perm-tree {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px;
  max-height: 420px;
  overflow-y: auto;
}
</style>
