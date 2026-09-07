import type { ContentStatus } from '@/api/types'

/** 内容状态映射 */
export const CONTENT_STATUS_OPTIONS: { value: ContentStatus | ''; label: string; tag: string }[] = [
  { value: '', label: '全部状态', tag: 'info' },
  { value: 'draft', label: '草稿', tag: 'info' },
  { value: 'pending', label: '待审核', tag: 'warning' },
  { value: 'published', label: '已发布', tag: 'success' },
  { value: 'rejected', label: '已驳回', tag: 'danger' }
]

export const CONTENT_STATUS_MAP: Record<string, { label: string; tag: string }> = {
  draft: { label: '草稿', tag: 'info' },
  pending: { label: '待审核', tag: 'warning' },
  published: { label: '已发布', tag: 'success' },
  rejected: { label: '已驳回', tag: 'danger' }
}

/** 审核状态 */
export const REVIEW_STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已驳回' }
]

export const REVIEW_STATUS_MAP: Record<string, { label: string; tag: string }> = {
  pending: { label: '待审核', tag: 'warning' },
  approved: { label: '已通过', tag: 'success' },
  rejected: { label: '已驳回', tag: 'danger' }
}

/** 用户状态 */
export const USER_STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'active', label: '正常' },
  { value: 'disabled', label: '禁用' }
]

export const USER_STATUS_MAP: Record<string, { label: string; tag: string }> = {
  active: { label: '正常', tag: 'success' },
  disabled: { label: '禁用', tag: 'danger' }
}

/** 节日类型 */
export const FESTIVAL_TYPE_OPTIONS = [
  { value: '', label: '全部类型' },
  { value: 'traditional', label: '传统节日' },
  { value: 'religious', label: '宗教节日' },
  { value: 'agricultural', label: '农事节日' }
]

export const FESTIVAL_TYPE_MAP: Record<string, string> = {
  traditional: '传统节日',
  religious: '宗教节日',
  agricultural: '农事节日'
}

/** 艺术类别 */
export const ART_CATEGORY_OPTIONS = [
  { value: '', label: '全部类别' },
  { value: 'music', label: '音乐' },
  { value: 'dance', label: '舞蹈' },
  { value: 'drama', label: '戏剧' },
  { value: 'costume', label: '服饰' },
  { value: 'craft', label: '手工艺' },
  { value: 'architecture', label: '建筑' }
]

export const ART_CATEGORY_MAP: Record<string, string> = {
  music: '音乐',
  dance: '舞蹈',
  drama: '戏剧',
  costume: '服饰',
  craft: '手工艺',
  architecture: '建筑'
}

/** 非遗级别 */
export const HERITAGE_OPTIONS = [
  { value: '', label: '全部级别' },
  { value: 'world', label: '世界级' },
  { value: 'national', label: '国家级' },
  { value: 'provincial', label: '省级' }
]

export const HERITAGE_MAP: Record<string, string> = {
  world: '世界级',
  national: '国家级',
  provincial: '省级'
}

/** 语系 */
export const LANGUAGE_FAMILY_OPTIONS = [
  '',
  '汉藏语系',
  '阿尔泰语系',
  '南岛语系',
  '南亚语系',
  '印欧语系',
  '混合'
]

/** 地域 */
export const REGION_OPTIONS = [
  '',
  '东北',
  '西北',
  '西南',
  '中南',
  '东南',
  '内蒙古',
  '其他'
]

/** 侧边栏菜单（roles 权限控制） */
export const MENU_CONFIG = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer', roles: [] },
  { path: '/ethnic', title: '民族管理', icon: 'Flag', roles: [] },
  { path: '/festival', title: '节日管理', icon: 'Calendar', roles: [] },
  { path: '/art', title: '艺术管理', icon: 'Headset', roles: [] },
  { path: '/topic', title: '专题管理', icon: 'Collection', roles: [] },
  { path: '/form', title: '表单配置', icon: 'EditPen', roles: ['super_admin', 'content_admin'] },
  { path: '/review', title: '审核管理', icon: 'DocumentChecked', roles: ['super_admin', 'reviewer', 'content_admin'] },
  { path: '/user', title: '用户管理', icon: 'User', roles: ['super_admin', 'content_admin'] },
  { path: '/role', title: '角色管理', icon: 'Lock', roles: ['super_admin'] }
]

/** 角色编码 -> 名称（后端 /auth/me 返回角色编码） */
export const ROLE_LABEL: Record<string, string> = {
  super_admin: '超级管理员',
  content_admin: '内容管理员',
  editor: '内容编辑',
  reviewer: '审核员',
  operator: '运营',
  user: '普通用户'
}
