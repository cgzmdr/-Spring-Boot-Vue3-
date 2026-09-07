import { del, get, post, put } from '../request'
import type { PageData, Permission, Role, User } from '../types'
import { uuidToStr } from '@/utils/uuid'

/** 用户列表查询参数 */
export interface UserQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
}

/** 新建 / 编辑用户参数 */
export interface UserSaveParams {
  /** 账号（新建留空时后端默认取昵称） */
  account?: string
  /** 昵称（可用于登录） */
  nickname?: string
  /** 密码（新建必填；编辑留空表示不重置） */
  password?: string
  mobile?: string
  email?: string
  status?: 'active' | 'disabled'
  /** 角色 ID 列表（标准 UUID 字符串） */
  roleIds?: string[]
}

/** 用户列表 */
export function listUsers(params: UserQuery): Promise<PageData<User>> {
  return get<PageData<User>>('/admin/users', params as Record<string, unknown>)
}

/** 用户详情 */
export function getUser(id: string): Promise<User> {
  return get<User>(`/admin/users/${id}`)
}

/** 新建用户，返回新用户 ID */
export function createUser(data: UserSaveParams): Promise<string> {
  return post<string>('/admin/users', data)
}

/** 编辑用户（未提交的字段保持不变） */
export function updateUser(id: string, data: UserSaveParams): Promise<string> {
  return put<string>(`/admin/users/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id: string): Promise<string> {
  return del<string>(`/admin/users/${id}`)
}

/**
 * 分配用户角色
 * 注意：后端契约 body 为 List&lt;UUID&gt;，这里直接传标准 UUID 字符串数组
 */
export function assignUserRoles(id: string, roleIds: string[]): Promise<string> {
  return put<string>(`/admin/users/${id}/roles`, roleIds)
}

/** 角色列表 */
export function listRoles(): Promise<Role[]> {
  return get<Role[]>('/admin/roles')
}

/** 权限列表 */
export function listPermissions(): Promise<Permission[]> {
  return get<Permission[]>('/admin/permissions')
}

/** 新增角色 */
export function createRole(data: Partial<Role>): Promise<string> {
  return post<string>('/admin/roles', data)
}

/**
 * 分配角色权限
 * 注意：后端契约 body 为 List&lt;UUID&gt;，这里直接传标准 UUID 字符串数组
 */
export function assignRolePermissions(id: string, permissionIds: string[]): Promise<string> {
  return put<string>(`/admin/roles/${id}/permissions`, permissionIds)
}

/** 角色展示名（id -> code/name） */
export function roleLabel(role: Role): string {
  return role.name || role.code || uuidToStr(role.id)
}

/** 从角色列表提取权限点（去重） */
export function collectPermissions(roles: Role[]): Permission[] {
  const map = new Map<string, Permission>()
  roles.forEach((r) =>
    (r.permissions || []).forEach((p) => {
      const key = p.code || uuidToStr(p.id)
      if (!map.has(key)) map.set(key, p)
    })
  )
  return Array.from(map.values())
}
