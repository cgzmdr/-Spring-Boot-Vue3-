import { get, post, put, del } from '../request'
import type { Art, PageData } from '../types'
import type { ContentStatus } from '../types'

/** 艺术列表查询参数 */
export interface ArtQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
  status?: ContentStatus | ''
}

/** 艺术管理列表 */
export function listArts(params: ArtQuery): Promise<PageData<Art>> {
  return get<PageData<Art>>('/admin/arts', params as Record<string, unknown>)
}

/** 艺术详情 */
export function getArt(id: string): Promise<Art> {
  return get<Art>(`/admin/arts/${id}`)
}

/** 新增艺术 */
export function createArt(data: Partial<Art>): Promise<string> {
  return post<string>('/admin/arts', data)
}

/** 编辑艺术 */
export function updateArt(id: string, data: Partial<Art>): Promise<string> {
  return put<string>(`/admin/arts/${id}`, data)
}

/** 删除艺术 */
export function deleteArt(id: string): Promise<string> {
  return del<string>(`/admin/arts/${id}`)
}
