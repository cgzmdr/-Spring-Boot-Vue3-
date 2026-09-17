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

/** 审核状态（content_review 快照表；offline/revising 由内容审批工作流引入） */
export const REVIEW_STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'pending', label: '待审批' },
  { value: 'offline', label: '已暂时下线' },
  { value: 'revising', label: '待修改' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已驳回' }
]

export const REVIEW_STATUS_MAP: Record<string, { label: string; tag: string }> = {
  pending: { label: '待审批', tag: 'warning' },
  offline: { label: '已暂时下线', tag: 'danger' },
  revising: { label: '待修改', tag: 'warning' },
  approved: { label: '已通过', tag: 'success' },
  rejected: { label: '已驳回', tag: 'danger' }
}

// ============================================================================
// 内容审批工作流（Camunda 8）
// ============================================================================

/** 流程环节 -> 展示信息 */
export const WORKFLOW_STAGE_MAP: Record<string, { label: string; tag: string; role: string }> = {
  pending_review: { label: '待审核员审批', tag: 'warning', role: 'reviewer' },
  pending_inspect: { label: '待内容管理员审查', tag: 'warning', role: 'content_admin' },
  revising: { label: '待内容编辑修改', tag: 'danger', role: 'editor' },
  published: { label: '已上线', tag: 'success', role: '' },
  stopped: { label: '已终止', tag: 'info', role: '' }
}

export const WORKFLOW_STAGE_OPTIONS = [
  { value: '', label: '全部环节' },
  { value: 'pending_review', label: '待审核员审批' },
  { value: 'pending_inspect', label: '待内容管理员审查' },
  { value: 'revising', label: '待内容编辑修改' }
]

/** 实例状态 */
export const WORKFLOW_INSTANCE_STATUS_MAP: Record<string, { label: string; tag: string }> = {
  running: { label: '进行中', tag: 'warning' },
  completed: { label: '已完成', tag: 'success' },
  withdrawn: { label: '已撤回', tag: 'info' },
  superseded: { label: '已被取代', tag: 'info' }
}

/** 意见环节 -> 展示信息（含时间线图标与颜色） */
export const OPINION_STAGE_MAP: Record<
  string,
  { label: string; icon: string; color: string; tag: string }
> = {
  submit: { label: '提交审批', icon: 'Promotion', color: '#6b7280', tag: 'info' },
  approve: { label: '审核员审批', icon: 'DocumentChecked', color: '#2563eb', tag: 'primary' },
  publish: { label: '内容上线', icon: 'Upload', color: '#16a34a', tag: 'success' },
  inspect: { label: '内容管理员审查', icon: 'View', color: '#d97706', tag: 'warning' },
  offline: { label: '暂时下线', icon: 'Download', color: '#dc2626', tag: 'danger' },
  revise: { label: '内容编辑修改', icon: 'EditPen', color: '#7c3aed', tag: 'primary' }
}

/** 意见结论 */
export const OPINION_DECISION_MAP: Record<string, { label: string; tag: string }> = {
  submitted: { label: '已提交', tag: 'info' },
  approved: { label: '通过', tag: 'success' },
  rejected: { label: '退回', tag: 'danger' },
  issue: { label: '发现问题', tag: 'danger' },
  resolved: { label: '已修正', tag: 'primary' }
}

/** 内容类型 -> 中文名 */
export const ENTRY_TYPE_MAP: Record<string, string> = {
  ethnic: '民族',
  festival: '节日',
  art: '艺术',
  topic: '专题'
}

export const ENTRY_TYPE_OPTIONS = [
  { value: '', label: '全部类型' },
  { value: 'ethnic', label: '民族' },
  { value: 'festival', label: '节日' },
  { value: 'art', label: '艺术' },
  { value: 'topic', label: '专题' }
]

/** 内容类型 -> 前台/后台路由前缀（用于「查看内容」跳转） */
export const ENTRY_TYPE_ROUTE: Record<string, string> = {
  ethnic: '/ethnic',
  festival: '/festival',
  art: '/art',
  topic: '/topic'
}

/** Camunda Form 用途 */
export const FORM_PURPOSE_MAP: Record<string, string> = {
  approval: '审核员审批',
  inspection: '内容管理员审查',
  revision: '内容编辑修改'
}

export const FORM_PURPOSE_OPTIONS = [
  { value: 'approval', label: '审核员审批' },
  { value: 'inspection', label: '内容管理员审查' },
  { value: 'revision', label: '内容编辑修改' }
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

/**
 * 侧边栏菜单（按**业务域**分组，不按部门或表名）
 *
 * 分组原则：让使用者按「我要做什么」而非「数据在哪张表」来找到入口。
 *   · 工作台   —— 全局概览与待办
 *   · 内容运营 —— 站内所有对外展示的内容（民族/节日/艺术/专题/表单 + B 系列新增的文化资料）
 *   · 社区治理 —— 用户产生内容与合规处置
 *   · 系统设置 —— 账号权限与平台级配置
 */
export interface MenuItem {
  path: string
  title: string
  icon: string
  roles: string[]
}
export interface MenuGroup {
  key: string
  title: string
  icon: string
  /** 该组可见所需的角色（空数组表示所有人可见） */
  roles: string[]
  children: MenuItem[]
}

export const MENU_GROUPS: MenuGroup[] = [
  {
    key: 'workbench',
    title: '工作台',
    icon: 'Odometer',
    roles: [],
    children: [
      { path: '/dashboard', title: '数据概览', icon: 'Odometer', roles: [] },
      // 我的待办：内容审批工作流（Camunda 8）驱动的统一入口，各角色都从这里进入
      { path: '/todo', title: '我的待办', icon: 'List', roles: [] }
    ]
  },
  {
    key: 'content',
    title: '内容运营',
    icon: 'Collection',
    roles: [],
    children: [
      { path: '/ethnic', title: '民族', icon: 'Flag', roles: [] },
      { path: '/festival', title: '节日', icon: 'Calendar', roles: [] },
      { path: '/art', title: '艺术', icon: 'Headset', roles: [] },
      { path: '/topic', title: '专题', icon: 'Files', roles: [] },
      // —— 方向 B 新增的文化资料（原先只有 C 端展示，后台不可维护）——
      { path: '/person', title: '人物档案', icon: 'UserFilled', roles: ['super_admin', 'content_admin', 'editor'] },
      { path: '/area', title: '自治地方', icon: 'MapLocation', roles: ['super_admin', 'content_admin', 'editor'] },
      { path: '/sport', title: '传统体育', icon: 'Basketball', roles: ['super_admin', 'content_admin', 'editor'] },
      { path: '/form', title: '表单配置', icon: 'EditPen', roles: ['super_admin', 'content_admin'] }
    ]
  },
  {
    key: 'community',
    title: '社区治理',
    icon: 'ChatDotRound',
    roles: ['super_admin', 'reviewer', 'content_admin', 'operator'],
    children: [
      { path: '/discussion', title: '讨论区', icon: 'ChatDotRound', roles: ['super_admin', 'reviewer', 'content_admin', 'operator'] },
      { path: '/review', title: '内容审核', icon: 'DocumentChecked', roles: ['super_admin', 'reviewer', 'content_admin'] }
    ]
  },
  {
    key: 'workflow',
    title: '流程建模',
    icon: 'Share',
    roles: ['super_admin', 'content_admin', 'reviewer'],
    children: [
      // 网页版 Camunda Modeler：BPMN 流程建模 + Camunda Form 表单设计 + 引擎部署
      { path: '/modeler', title: 'Camunda Modeler', icon: 'Share', roles: ['super_admin', 'content_admin', 'reviewer'] }
    ]
  },
  {
    key: 'system',
    title: '系统设置',
    icon: 'Setting',
    roles: ['super_admin', 'content_admin'],
    children: [
      { path: '/user', title: '用户管理', icon: 'User', roles: ['super_admin', 'content_admin'] },
      { path: '/role', title: '角色权限', icon: 'Lock', roles: ['super_admin'] },
      { path: '/translate', title: '翻译词表', icon: 'MagicStick', roles: ['super_admin', 'content_admin'] },
      // 检索索引与兴趣标签属于平台级检索/推荐能力（服务全站内容），故归入系统设置
      { path: '/search-index', title: '检索与推荐', icon: 'Search', roles: ['super_admin', 'content_admin'] },
      // 内容来源与图片署名属平台级数据治理（服务于全站所有内容的溯源与合规），故归入系统设置
      { path: '/source', title: '内容来源', icon: 'Link', roles: ['super_admin', 'content_admin', 'editor'] },
      { path: '/credit', title: '图片署名', icon: 'Picture', roles: ['super_admin', 'content_admin', 'editor'] }
    ]
  }
]

/** 扁平化的菜单项（用于面包屑 / 权限过滤等需要遍历的场景） */
export const MENU_CONFIG: MenuItem[] = MENU_GROUPS.flatMap((g) => g.children)
