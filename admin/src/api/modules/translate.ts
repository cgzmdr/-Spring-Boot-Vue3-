import service, { del, get, post } from '../request'
import type { PageData } from '../types'

/** 翻译词条（glossary 提供方的数据源） */
export interface GlossaryTerm {
  id: string
  sourceLocale: string
  targetLocale: string
  term: string
  translation: string
  enabled: boolean
  remark: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface GlossaryStats {
  total: number
  enabled: number
  locales: Record<string, number>
}

export function listGlossary(params: {
  keyword?: string
  targetLocale?: string
  page?: number
  size?: number
}): Promise<PageData<GlossaryTerm>> {
  return get<PageData<GlossaryTerm>>('/admin/translate/glossary', params as Record<string, unknown>)
}

export function saveGlossary(
  data: {
    sourceLocale?: string
    targetLocale?: string
    term: string
    translation: string
    enabled?: boolean
    remark?: string
  },
  /** 编辑 / 启停时传入当前术语（原术语），新增时留空 */
  currentTerm?: string,
): Promise<string> {
  const query = currentTerm ? `?currentTerm=${encodeURIComponent(currentTerm)}` : ''
  return post<string>(`/admin/translate/glossary${query}`, data)
}

export function deleteGlossary(id: string): Promise<void> {
  return del<void>(`/admin/translate/glossary/${id}`)
}

export function importGlossary(data: {
  text: string
  sourceLocale?: string
  targetLocale?: string
}): Promise<{ added: number; skipped: number; sourceLocale: string; targetLocale: string }> {
  return post<{ added: number; skipped: number; sourceLocale: string; targetLocale: string }>(
    '/admin/translate/glossary/import',
    data,
  )
}

export function glossaryStats(): Promise<GlossaryStats> {
  return get<GlossaryStats>('/admin/translate/glossary/stats')
}

export function glossaryLocales(): Promise<Record<string, number>> {
  return get<Record<string, number>>('/admin/translate/glossary/locales')
}

export function refreshGlossary(): Promise<{ refreshed: boolean; locales: Record<string, number> }> {
  return post<{ refreshed: boolean; locales: Record<string, number> }>('/admin/translate/glossary/refresh')
}

export function previewGlossary(data: {
  text: string
  sourceLocale?: string
  targetLocale?: string
}): Promise<{
  original: string
  translated: string
  hit: boolean
  hits: number
  coverage: number
  accepted: boolean
  minCoverage: number
}> {
  return post<{
    original: string
    translated: string
    hit: boolean
    hits: number
    coverage: number
    accepted: boolean
    minCoverage: number
  }>('/admin/translate/glossary/preview', data)
}

/** 导出全部词条（术语=译文，每行一条）：走 axios 以便带上 satoken 头，返回纯文本 */
export async function exportGlossaryText(targetLocale?: string): Promise<string> {
  const res = await service.get('/admin/translate/glossary/export', {
    params: { targetLocale },
    responseType: 'text',
  })
  return res.data as unknown as string
}
