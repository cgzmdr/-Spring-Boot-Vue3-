import { get, post, put, del } from '../request'
import type { EthnicGroup, PageData } from '../types'
import type { ContentStatus } from '../types'

/** 民族列表查询参数 */
export interface EthnicQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
  status?: ContentStatus | ''
}

/** 民族管理列表 */
export function listEthnicGroups(params: EthnicQuery): Promise<PageData<EthnicGroup>> {
  return get<PageData<EthnicGroup>>('/admin/ethnic-groups', params as Record<string, unknown>)
}

/** 民族后台详情 */
export function getEthnicGroup(id: string): Promise<EthnicGroup> {
  return get<EthnicGroup>(`/admin/ethnic-groups/${id}`)
}

/** 新增民族 */
export function createEthnicGroup(data: Partial<EthnicGroup>): Promise<string> {
  return post<string>('/admin/ethnic-groups', data)
}

/** 编辑民族 */
export function updateEthnicGroup(id: string, data: Partial<EthnicGroup>): Promise<string> {
  return put<string>(`/admin/ethnic-groups/${id}`, data)
}

/** 删除民族 */
export function deleteEthnicGroup(id: string): Promise<string> {
  return del<string>(`/admin/ethnic-groups/${id}`)
}

/** 56 民族全家福（简略列表，用于表单民族下拉） */
export function listAllEthnicGroups(): Promise<EthnicGroup[]> {
  return get<EthnicGroup[]>('/ethnic-groups/all')
}
