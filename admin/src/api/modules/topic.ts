import { get, post, put, del } from '../request'
import type { PageData, Topic } from '../types'
import type { ContentStatus } from '../types'

/** 专题列表查询参数 */
export interface TopicQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
  status?: ContentStatus | ''
}

/** 专题管理列表 */
export function listTopics(params: TopicQuery): Promise<PageData<Topic>> {
  return get<PageData<Topic>>('/admin/topics', params as Record<string, unknown>)
}

/** 专题详情 */
export function getTopic(id: string): Promise<Topic> {
  return get<Topic>(`/admin/topics/${id}`)
}

/** 新增专题 */
export function createTopic(data: Partial<Topic>): Promise<string> {
  return post<string>('/admin/topics', data)
}

/** 编辑专题 */
export function updateTopic(id: string, data: Partial<Topic>): Promise<string> {
  return put<string>(`/admin/topics/${id}`, data)
}

/** 删除专题 */
export function deleteTopic(id: string): Promise<string> {
  return del<string>(`/admin/topics/${id}`)
}
