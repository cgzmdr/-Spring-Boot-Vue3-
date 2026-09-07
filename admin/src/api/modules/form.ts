import { get, post, put, del } from '../request'
import type { FormConfig, PageData } from '../types'

/** 表单配置查询参数 */
export interface FormQuery {
  keyword?: string
  status?: 'active' | 'disabled' | ''
  page?: number
  size?: number
}

/** 表单配置列表 */
export function listForms(params: FormQuery): Promise<PageData<FormConfig>> {
  return get<PageData<FormConfig>>('/admin/forms', params as Record<string, unknown>)
}

/** 表单配置详情 */
export function getForm(id: string): Promise<FormConfig> {
  return get<FormConfig>(`/admin/forms/${id}`)
}

/** 新增表单配置 */
export function createForm(data: Partial<FormConfig>): Promise<string> {
  return post<string>('/admin/forms', data)
}

/** 编辑表单配置 */
export function updateForm(id: string, data: Partial<FormConfig>): Promise<string> {
  return put<string>(`/admin/forms/${id}`, data)
}

/** 删除表单配置 */
export function deleteForm(id: string): Promise<string> {
  return del<string>(`/admin/forms/${id}`)
}
