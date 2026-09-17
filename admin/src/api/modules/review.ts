import { get, post } from '../request'
import type { PageData, ReviewRecord, ReviewStatus } from '../types'

/** 审核列表查询参数 */
export interface ReviewQuery {
  page?: number // 0 基
  size?: number
  status?: ReviewStatus | ''
  entryType?: string
}

/** 审核列表（审核态快照，列表页用） */
export function listReviews(params: ReviewQuery): Promise<PageData<ReviewRecord>> {
  return get<PageData<ReviewRecord>>('/admin/reviews', params as Record<string, unknown>)
}

/**
 * 审核员审批通过（必填审批意见）。
 * <p>后端会写入审批意见并让内容上线，随后流转到「内容管理员审查」。</p>
 * @param id 工作流实例 ID
 */
export function approveReview(id: string, opinion: string): Promise<string> {
  return post<string>(`/admin/reviews/${id}/approve`, { opinion })
}

/**
 * 审核员退回（必填审批意见）。
 * <p>内容退回内容编辑修改，进入 revising 环节。</p>
 */
export function rejectReview(id: string, reason: string): Promise<string> {
  return post<string>(`/admin/reviews/${id}/reject`, { reason })
}

/** 内容管理员审查通过（内容保持在线，本轮闭环结束） */
export function inspectPassReview(id: string, opinion: string): Promise<string> {
  return post<string>(`/admin/reviews/${id}/inspect/pass`, { opinion })
}

/** 内容管理员审查发现问题：内容暂时下线并交内容编辑修改 */
export function inspectIssueReview(id: string, reason: string): Promise<string> {
  return post<string>(`/admin/reviews/${id}/inspect/issue`, { reason })
}

/** 内容编辑修改完成，重新提交审核员二次审批 */
export function reviseReview(
  id: string,
  revisionNote: string,
  needReapproval = true
): Promise<string> {
  return post<string>(`/admin/reviews/${id}/revise`, { revisionNote, needReapproval })
}

/** 提交内容审批（按内容类型 + 内容 ID） */
export function submitReview(
  entryType: string,
  entryId: string,
  note?: string
): Promise<string> {
  const qs = new URLSearchParams({ entryType, entryId })
  if (note) qs.set('note', note)
  return post<string>(`/admin/reviews/submit?${qs.toString()}`)
}
