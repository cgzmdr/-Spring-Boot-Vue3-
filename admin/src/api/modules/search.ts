import { get, post, del } from '../request'
import type { PageData } from '../types'

/* ==========================================================================
   检索索引与兴趣标签（方向 D）
   对应后端 /admin/search-index/* 与 /admin/interest-tags/*
   ========================================================================== */

/** 索引统计项 */
export interface IndexTypeStat {
  docType: string
  count: number
}

/** 索引统计 */
export interface IndexStats {
  total: number
  byType: IndexTypeStat[]
  supportedTypes: string[]
}

/** 重建结果 */
export interface RebuildResult {
  total: number
  byType: Record<string, number>
  elapsedMs: number
}

/** 兴趣标签 */
export interface InterestTag {
  id?: string
  /** ethnic 民族 / region 地域 / type 内容类型 / topic 主题 */
  dimension: string
  name: string
  nameEn: string | null
  description: string | null
  color: string | null
  enabled: boolean
  orderNum: number
  createdAt?: string
  updatedAt?: string
}

/** 检索索引统计：各内容类型的文档数 */
export const searchIndexApi = {
  stats: () => get<IndexStats>('/admin/search-index/stats'),
  /** 重建索引；type 留空为全量 */
  rebuild: (type?: string) =>
    post<RebuildResult>('/admin/search-index/rebuild' + (type ? `?type=${type}` : '')),
}

/** 兴趣标签维护 */
export const interestTagApi = {
  list: (params?: { dimension?: string; keyword?: string; page?: number; size?: number }) =>
    get<PageData<InterestTag>>('/admin/interest-tags', (params || {}) as Record<string, unknown>),
  stats: () => get<Record<string, number>>('/admin/interest-tags/stats'),
  save: (body: InterestTag) => post<string>('/admin/interest-tags', body),
  remove: (id: string) => del(`/admin/interest-tags/${id}`),
}
