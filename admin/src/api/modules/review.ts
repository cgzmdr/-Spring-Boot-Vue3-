import { get, post } from '../request'
import type { PageData, ReviewRecord, ReviewStatus } from '../types'

/** 审核列表查询参数 */
export interface ReviewQuery {
  page?: number // 0 基
  size?: number
  status?: ReviewStatus | ''
}

/** 审核列表 */
export function listReviews(params: ReviewQuery): Promise<PageData<ReviewRecord>> {
  return get<PageData<ReviewRecord>>('/admin/reviews', params as Record<string, unknown>)
}

/** 审核通过 */
export function approveReview(id: string): Promise<string> {
  return post<string>(`/admin/reviews/${id}/approve`)
}

/** 审核驳回 */
export function rejectReview(id: string, reason: string): Promise<string> {
  return post<string>(`/admin/reviews/${id}/reject`, { reason })
}
