/**
 * UUID 兼容工具
 *
 * 后端（sa-token + Java UUID）将 UUID 序列化为对象：
 *   { "mostSigBits": <number>, "leastSigBits": <number>, ... }
 * 而非标准字符串。本工具提供对象 <-> 字符串双向转换。
 * 注意：Java long 可超过 JS Number.MAX_SAFE_INTEGER，回传时优先使用
 * 后端返回的原始对象，避免精度丢失；仅展示与 path 参数使用字符串。
 */

/** 后端返回的 UUID 对象形态 */
export interface UuidObject {
  mostSigBits: number
  leastSigBits: number
  mostSignificantBits?: number
  leastSignificantBits?: number
}

/** 判断是否为 UUID 对象 */
export function isUuidObject(v: unknown): v is UuidObject {
  return (
    !!v &&
    typeof v === 'object' &&
    'mostSigBits' in (v as object) &&
    'leastSigBits' in (v as object)
  )
}

/**
 * UUID 对象 -> 标准字符串（用于展示）
 * 算法：mostSigBits 高 64 位 + leastSigBits 低 64 位拼接为标准 UUID 格式
 */
export function uuidToStr(u: UuidObject | string | null | undefined): string {
  if (!u) return ''
  if (typeof u === 'string') return u
  const hex = (n: number) => {
    // 转无符号 64 位十六进制
    const big = BigInt(n)
    const h = (big & 0xffffffffffffffffn).toString(16)
    return h.padStart(16, '0')
  }
  const raw = hex(u.mostSigBits) + hex(u.leastSigBits)
  return (
    raw.slice(0, 8) +
    '-' +
    raw.slice(8, 12) +
    '-' +
    raw.slice(12, 16) +
    '-' +
    raw.slice(16, 20) +
    '-' +
    raw.slice(20)
  )
}

/**
 * 标准 UUID 字符串 -> UUID 对象（用于提交 body）
 * 若字符串过长导致超出安全整数范围，返回 null，调用方应改用原对象回传。
 */
export function strToUuid(s: string): UuidObject | null {
  const h = s.replace(/-/g, '')
  if (!/^[0-9a-fA-F]{32}$/.test(h)) return null
  const most = BigInt('0x' + h.slice(0, 16))
  const least = BigInt('0x' + h.slice(16))
  if (most > BigInt(Number.MAX_SAFE_INTEGER) || least > BigInt(Number.MAX_SAFE_INTEGER)) {
    return null // 超出安全范围，回传原对象
  }
  return { mostSigBits: Number(most), leastSigBits: Number(least) }
}

/** 取原始 id 值：对象或字符串均可，返回原始类型 */
export function rawId(id: UuidObject | string | undefined | null): UuidObject | string | undefined {
  if (id === null || id === undefined) return undefined
  return id
}
