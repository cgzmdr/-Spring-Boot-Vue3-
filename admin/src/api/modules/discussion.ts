import { del, get, post } from '../request'
import type { PageData } from '../types'

/** 待审条目 */
export interface DiscussionReviewItem {
  targetType: string
  targetId: string
  title: string | null
  excerpt: string | null
  context: string | null
  authorId: string | null
  authorName: string | null
  riskLevel: string | null
  hitWords: string | null
  reportCount: number
  status: string
  createdAt: string | null
}

/** 举报记录 */
export interface DiscussionReport {
  id: string
  targetType: string
  targetId: string
  targetExcerpt: string | null
  targetAuthor: string | null
  targetPath: string | null
  reporter: string | null
  reason: string
  detail: string | null
  status: string
  resultNote: string | null
  createdAt: string | null
  handledAt: string | null
}

/** 敏感词 */
export interface SensitiveWord {
  id: string
  word: string
  locale: string
  level: 'block' | 'watch' | 'replace'
  enabled: boolean
  remark: string | null
  createdAt: string | null
}

/** 待审队列 */
export function listReviewItems(params: {
  targetType?: string
  status?: string
  sort?: 'oldest' | 'priority'
  page?: number
  size?: number
}): Promise<PageData<DiscussionReviewItem>> {
  return get<PageData<DiscussionReviewItem>>('/admin/discussion/reviews', params as Record<string, unknown>)
}

/** 批量审核 */
export function reviewItemsBatch(data: {
  targetType: string
  ids: string[]
  status: 'approved' | 'rejected'
  reason?: string
}): Promise<{ success: number; failed: number; failures: string[] }> {
  return post<{ success: number; failed: number; failures: string[] }>('/admin/discussion/reviews/batch', data)
}

/** 批量导入敏感词 */
export function importWords(data: { text: string; level: string; locale?: string }): Promise<{ added: number; skipped: number }> {
  return post<{ added: number; skipped: number }>('/admin/discussion/words/import', data)
}

/** 敏感词导出地址（浏览器直接下载） */
export function wordsExportUrl(): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || ''
  return `${base}/admin/discussion/words/export`
}

/** 审核处置 */
export function reviewItem(data: {
  targetType: string
  targetId: string
  status: 'approved' | 'rejected'
  reason?: string
}): Promise<void> {
  return post<void>('/admin/discussion/reviews', data)
}

/** 举报列表 */
export function listReports(params: { status?: string; page?: number; size?: number }): Promise<PageData<DiscussionReport>> {
  return get<PageData<DiscussionReport>>('/admin/discussion/reports', params as Record<string, unknown>)
}

/** 处理举报 */
export function handleReport(
  id: string,
  data: { status: 'accepted' | 'rejected'; note?: string; contentAction?: 'none' | 'hide' | 'delete'; muteDays?: number },
): Promise<void> {
  return post<void>(`/admin/discussion/reports/${id}`, data)
}

/** 置顶 / 精华 / 锁定 */
export function setTopicFlag(id: string, flag: 'pinned' | 'featured' | 'locked', value: boolean): Promise<void> {
  return post<void>(`/admin/discussion/topics/${id}/flag?flag=${flag}&value=${value}`)
}

/** 隐藏内容 */
export function hideContent(targetType: string, targetId: string, reason?: string): Promise<void> {
  const query = reason ? `&reason=${encodeURIComponent(reason)}` : ''
  return post<void>(`/admin/discussion/content/hide?targetType=${targetType}&targetId=${targetId}${query}`)
}

/** 禁言 / 解除禁言 */
export function muteUser(userId: string, days: number, reason?: string): Promise<void> {
  const query = reason ? `&reason=${encodeURIComponent(reason)}` : ''
  return post<void>(`/admin/discussion/users/${userId}/mute?days=${days}${query}`)
}

/** 敏感词 */
export function listWords(params: { keyword?: string; page?: number; size?: number }): Promise<PageData<SensitiveWord>> {
  return get<PageData<SensitiveWord>>('/admin/discussion/words', params as Record<string, unknown>)
}

export function saveWord(data: {
  word: string
  locale?: string
  level: string
  enabled?: boolean
  remark?: string
}): Promise<string> {
  return post<string>('/admin/discussion/words', data)
}

export function deleteWord(id: string): Promise<void> {
  return del<void>(`/admin/discussion/words/${id}`)
}

/** 社区看板 */
export function discussionStats(): Promise<Record<string, number>> {
  return get<Record<string, number>>('/admin/discussion/stats')
}

/* ---------------------------- 板块管理 ---------------------------- */

export interface AdminBoard {
  id: string
  slug: string
  name: string
  nameEn: string | null
  description: string | null
  orderNum: number
  status: string
  postPolicy: string
  topicCount: number
  createdAt: string | null
}

export function listBoards(): Promise<AdminBoard[]> {
  return get<AdminBoard[]>('/admin/discussion/boards')
}

export function saveBoard(data: {
  id?: string
  name: string
  nameEn?: string
  description?: string
  orderNum?: number
  postPolicy: string
}): Promise<string> {
  return post<string>('/admin/discussion/boards', data)
}

export function toggleBoard(id: string, active: boolean): Promise<void> {
  return post<void>(`/admin/discussion/boards/${id}/status?active=${active}`)
}

/* ---------------------------- 站内公告（OA 群发） ---------------------------- */

/** 公告受众预览 */
export interface BroadcastAudience {
  all: number
  active: number
  maxRecipients: number
  activeWindowDays: number
}

export interface BroadcastResult {
  broadcastId: string
  title: string
  audience: string
  recipients: number
  email: boolean
}

export function broadcastAudience(): Promise<BroadcastAudience> {
  return get<BroadcastAudience>('/admin/discussion/broadcast/audience')
}

export function sendBroadcast(data: {
  title: string
  content: string
  email: boolean
  audience: 'all' | 'active'
  link?: string
}): Promise<BroadcastResult> {
  return post<BroadcastResult>('/admin/discussion/broadcast', data)
}
