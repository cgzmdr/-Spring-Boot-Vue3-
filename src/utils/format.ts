/** 通用格式化工具 */

import { PLACEHOLDER_LQIP } from './placeholder-lqip'

/** 内置占位图路径（public/ 下，随版本固定命名，便于 Nginx 长期强缓存） */
export const PLACEHOLDER_PATH = '/images/placeholders/placeholder.webp'

/** 内置占位图的模糊微缩预览（data URL，零请求，用于图片就绪前铺底） */
export const placeholderLqip = PLACEHOLDER_LQIP

/** 数字千分位 */
export function formatNumber(n: number | string | null | undefined): string {
  if (n === null || n === undefined || n === '') return '—'
  const num = Number(n)
  if (Number.isNaN(num)) return String(n)
  return num.toLocaleString('zh-CN')
}

/**
 * 后端 JSONB 字段在"详情嵌套"时可能以 JSON 字符串返回（如 customs / inheritors / images）。
 * 统一解析为数组；已是数组则原样返回。
 */
export function parseArray<T = string>(value: T[] | string | null | undefined): T[] {
  if (value === null || value === undefined) return []
  if (Array.isArray(value)) return value
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return value ? ([value] as T[]) : []
    }
  }
  return []
}

/** 简短 UUID（前 8 位） */
export function shortId(id: string): string {
  return id ? id.slice(0, 8) : ''
}

/**
 * 构建占位图 URL（本地静态资源，已替代远程 AI 图源）
 * 当内容缺少真实图片时作为兜底展示。
 * 注：原实现调用远程 AI 生成服务（coresg-normal.trae.ai），
 * 现改为项目内置占位图，避免对外部网络资源的依赖。
 *
 * 该文件已压缩为 320×180 / 约 12 KB（见 scripts/gen-placeholder.mjs），
 * 慢网首屏可接受；更小的同源图可显式传入 `src` 覆盖。
 */
export function aiImage(_prompt: string, _size = 'landscape_16_9'): string {
  return PLACEHOLDER_PATH
}

/** 民族主题色 → 描述文案（用于占位图） */
export function ethnicImagePrompt(name: string): string {
  return `${name} ethnic group in China wearing traditional costume, cultural photography, high quality`
}

/** 节日占位图 */
export function festivalImagePrompt(name: string): string {
  return `Chinese ethnic minority festival ${name} celebration, traditional customs, photography`
}

/** 艺术占位图 */
export function artImagePrompt(name: string): string {
  return `Chinese ethnic minority traditional art ${name}, craftsmanship, photography`
}

/** 节日类型中文标签 */
export const festivalTypeLabel: Record<string, string> = {
  traditional: '传统节日',
  religious: '宗教节日',
  agricultural: '农事节日',
}

/** 艺术类别中文标签 */
export const artCategoryLabel: Record<string, string> = {
  music: '音乐',
  dance: '舞蹈',
  drama: '戏剧',
  costume: '服饰',
  craft: '手工艺',
  architecture: '建筑',
  fine_art: '美术',
}

/** 非遗级别中文标签 */
export const heritageLabel: Record<string, string> = {
  world: '世界级非遗',
  national: '国家级非遗',
  provincial: '省级非遗',
}

/** 收藏内容类型中文标签 */
export const favoriteTypeLabel: Record<string, string> = {
  ethnic: '民族',
  festival: '节日',
  art: '艺术',
  topic: '专题',
  food: '美食',
}

/** 收藏内容类型 → 详情页路由名 */
export const favoriteRouteName: Record<string, string> = {
  ethnic: 'ethnic-detail',
  festival: 'festival-detail',
  art: 'art-detail',
  topic: 'topic-detail',
  food: 'food-detail',
}

/** 收藏内容类型 → 详情页路由路径前缀 */
export const favoriteRoutePath: Record<string, string> = {
  ethnic: '/ethnic',
  festival: '/festival',
  art: '/art',
  topic: '/topic',
  food: '/food',
}

const API_BASE: string = (import.meta.env.VITE_API_BASE as string) || ''
const STATIC_BASE: string = (import.meta.env.VITE_STATIC_BASE as string) || API_BASE

/**
 * 随前端一起发布的静态资源前缀。
 *
 * 只有 `public/images/placeholders/` 下的文件在 dist 里（占位图、LQIP）。
 * **不能**把整个 `/images/` 都当成本地资源：
 * `/images/ethnic/...`、`/images/topic/...` 这些真实内容图存在**后端**，
 * 必须继续指向 `VITE_STATIC_BASE`，否则 dev 下 Vite 会把未知路径
 * 回退成 index.html（HTTP 200 + text/html），浏览器解码失败 →
 * 图片全部裂开；线上则会 404。
 */
export const LOCAL_ASSET_PREFIXES = ['/images/placeholders/'] as const

/** 该路径是否由前端自身提供（而非后端静态服务） */
export function isLocalAsset(src: string): boolean {
  return LOCAL_ASSET_PREFIXES.some((p) => src.startsWith(p))
}

/**
 * 解析后端下发的静态资源地址（头像 / 封面等）：
 * 绝对 URL 原样返回；Data URL 原样返回；前端自带资源保持同源；
 * 其余以 / 开头的路径拼上后端静态服务 origin。
 */
export function resolveStaticUrl(src?: string | null): string {
  if (!src) return ''
  if (/^(https?:)?\/\//i.test(src) || /^data:/i.test(src)) return src
  // 前端自带的占位图：同源直出
  if (isLocalAsset(src)) return src
  if (src.startsWith('/') && /^https?:\/\//i.test(STATIC_BASE)) {
    try {
      return new URL(STATIC_BASE).origin + src
    } catch {
      return STATIC_BASE.replace(/\/+$/, '') + src
    }
  }
  return src
}

/** 时间格式化（后端 LocalDateTime ISO 字符串 → yyyy-MM-dd HH:mm） */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 日期格式化（yyyy-MM-dd） */
export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
