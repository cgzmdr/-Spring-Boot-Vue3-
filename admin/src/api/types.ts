/** 全局类型定义（对齐 openapi.json 契约） */

/** id 类型：后端返回 UUID 对象（{mostSigBits, leastSigBits}），path 参数传字符串 */
export type UuidLike = import('@/utils/uuid').UuidObject | string

/** 后端统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 列表响应结构：data.total + data.data（admin 接口不分页，全量返回） */
export interface FormConfig {
  id: string
  code: string
  name: string
  description?: string | null
  schema: string
  status: 'active' | 'disabled'
  createdAt?: string
  updatedAt?: string
}

export interface PageData<T> {
  total: number
  data: T[]
}

/** 内容状态 */
export type ContentStatus = 'draft' | 'pending' | 'published' | 'rejected' | 'offline'

/** 审核状态（content_review 快照：offline/revising 由内容审批工作流引入） */
export type ReviewStatus = 'pending' | 'approved' | 'rejected' | 'offline' | 'revising'

/** 角色 */
export interface Role {
  id: UuidLike
  code: string
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
  permissions?: Permission[]
}

/** 权限点 */
export interface Permission {
  id: UuidLike
  code: string
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

/** 用户 */
export interface User {
  id: UuidLike
  account: string
  passwordHash?: string
  nickname: string
  avatar?: string
  mobile?: string
  email?: string
  status: 'active' | 'disabled'
  createdAt: string
  updatedAt: string
  roles?: Role[]
  favorites?: unknown[]
}

/** 民族 */
export interface EthnicGroup {
  id: UuidLike
  slug?: string
  name: string
  nameEn?: string
  selfName?: string
  pinyin?: string
  population?: number
  languageFamily?: string
  region?: string
  languages?: string
  scripts?: string
  religion?: string
  summary?: string
  summaryEn?: string
  description?: string
  descriptionEn?: string
  coverImage?: string
  themeColor?: string
  tags?: string
  status?: ContentStatus
  /** 浏览量（后台列表合并展示） */
  viewCount?: number
  orderNum?: number
  createdAt?: string
  updatedAt?: string
  customs?: EthnicCustom[]
  festivals?: Festival[]
  arts?: Art[]
  foods?: Food[]
  locations?: EthnicLocation[]
}

/** 风俗 */
export interface EthnicCustom {
  id: UuidLike
  ethnicGroupId?: UuidLike
  category?: string
  title: string
  content?: string
  image?: string
  orderNum?: number
  createdAt?: string
  updatedAt?: string
}

/** 美食 */
export interface Food {
  id: UuidLike
  ethnicGroupId?: UuidLike
  name: string
  description?: string
  image?: string
  orderNum?: number
  createdAt?: string
  updatedAt?: string
}

/** 聚居地坐标 */
export interface EthnicLocation {
  id: UuidLike
  ethnicGroupId?: UuidLike
  name: string
  description?: string
  longitude?: number
  latitude?: number
  orderNum?: number
  createdAt?: string
  updatedAt?: string
}

/** 节日 */
export interface Festival {
  id: UuidLike
  slug?: string
  ethnicGroupId?: UuidLike
  name: string
  nameEn?: string
  type?: 'traditional' | 'religious' | 'agricultural'
  solarDate?: string
  lunarDate?: string
  origin?: string
  description?: string
  customs?: string
  images?: string
  coverImage?: string
  status?: ContentStatus
  /** 浏览量（后台列表合并展示） */
  viewCount?: number
  orderNum?: number
  createdAt?: string
  updatedAt?: string
  ethnicGroup?: EthnicGroup
}

/** 艺术 */
export interface Art {
  id: UuidLike
  slug?: string
  ethnicGroupId?: UuidLike
  name: string
  nameEn?: string
  category?: string
  intangibleHeritage?: string
  description?: string
  inheritors?: string
  coverImage?: string
  status?: ContentStatus
  /** 浏览量（后台列表合并展示） */
  viewCount?: number
  orderNum?: number
  createdAt?: string
  updatedAt?: string
  ethnicGroup?: EthnicGroup
}

/** 专题 */
export interface Topic {
  id: UuidLike
  slug?: string
  title: string
  subtitle?: string
  description?: string
  coverImage?: string
  status?: ContentStatus
  /** 浏览量（后台列表合并展示） */
  viewCount?: number
  orderNum?: number
  createdAt?: string
  updatedAt?: string
  entries?: unknown[]
}

/** 审核记录（content_review：审核态快照） */
export interface ReviewRecord {
  id: UuidLike
  entryType: string
  entryId: UuidLike
  status: ReviewStatus
  submitterId: UuidLike
  reviewerId?: UuidLike
  rejectReason?: string
  submittedAt?: string
  reviewedAt?: string
  /** 关联的工作流实例（Camunda 8） */
  instanceId?: UuidLike
  /** 内容版本号（第几版） */
  contentVersion?: number
  /** 最近一条审批/审查意见摘要 */
  lastOpinion?: string
}

/** 统计总览 */
export interface StatsOverview {
  userCount: number
  ethnicCount: number
  festivalCount: number
  artCount: number
  topicCount: number
  pendingReviewCount: number
}

/** 内容维度统计 */
export interface StatsContentItem {
  type: string
  total: number
  published: number
  draft: number
  pending: number
  offline: number
}

/** 用户反馈（C 端首页提交） */
export interface FeedbackItem {
  id: UuidLike
  name?: string | null
  contact?: string | null
  /** correction / suggestion / bug */
  topic: string
  /** good / ok / bad */
  rating?: string | null
  content: string
  visitDate?: string | null
  createdAt?: string
}
