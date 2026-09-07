import { get } from '../request'
import type { FeedbackItem, PageData, StatsContentItem, StatsOverview } from '../types'

/** 统计总览 */
export function getStatsOverview(): Promise<StatsOverview> {
  return get<StatsOverview>('/admin/stats/overview')
}

/** 内容维度统计 */
export function getStatsContent(): Promise<StatsContentItem[]> {
  return get<StatsContentItem[]>('/admin/stats/content')
}

/** 反馈列表查询参数 */
export interface FeedbackQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
}

/** 用户反馈分页列表 */
export function listFeedback(params: FeedbackQuery): Promise<PageData<FeedbackItem>> {
  return get<PageData<FeedbackItem>>('/admin/feedback', params as Record<string, unknown>)
}

/**
 * 批量查询浏览量（{ id: count }）
 * @param type 内容类型：ethnic / festival / art / topic
 * @param ids 内容 ID 列表（标准 UUID 字符串）
 */
export function getViewCounts(type: string, ids: string[]): Promise<Record<string, number>> {
  return get<Record<string, number>>('/admin/view-counts', { type, ids: ids.join(',') })
}
