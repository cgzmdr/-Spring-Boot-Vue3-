import { get, post, put, del } from '../request'
import type { Festival, PageData } from '../types'
import type { ContentStatus } from '../types'

/** 节日列表查询参数 */
export interface FestivalQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
  status?: ContentStatus | ''
}

/** 节日管理列表 */
export function listFestivals(params: FestivalQuery): Promise<PageData<Festival>> {
  return get<PageData<Festival>>('/admin/festivals', params as Record<string, unknown>)
}

/** 节日详情 */
export function getFestival(id: string): Promise<Festival> {
  return get<Festival>(`/admin/festivals/${id}`)
}

/** 新增节日 */
export function createFestival(data: Partial<Festival>): Promise<string> {
  return post<string>('/admin/festivals', data)
}

/** 编辑节日 */
export function updateFestival(id: string, data: Partial<Festival>): Promise<string> {
  return put<string>(`/admin/festivals/${id}`, data)
}

/** 删除节日 */
export function deleteFestival(id: string): Promise<string> {
  return del<string>(`/admin/festivals/${id}`)
}
