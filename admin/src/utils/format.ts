/** 通用格式化工具 */

/** 日期时间格式化：YYYY-MM-DD HH:mm:ss */
export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

/** 数字千分位格式化 */
export function formatNumber(value?: number | null): string {
  if (value === null || value === undefined) return '-'
  return value.toLocaleString('zh-CN')
}

const API_BASE: string = (import.meta.env.VITE_API_BASE_URL as string) || ''
const STATIC_BASE: string = (import.meta.env.VITE_STATIC_BASE_URL as string) || API_BASE

/**
 * 图片等静态资源直接指向后端静态服务，不依赖 Vite 代理。
 * 通过 VITE_STATIC_BASE_URL 指定后端静态资源地址。
 */
export function resolveImageUrl(src?: string | null): string {
  if (!src) return ''
  if (/^https?:\/\//i.test(src)) return src
  if (src.startsWith('/') && /^https?:\/\//i.test(STATIC_BASE)) {
    try {
      return new URL(STATIC_BASE).origin + src
    } catch {
      return STATIC_BASE.replace(/\/+$/, '') + src
    }
  }
  return src
}
