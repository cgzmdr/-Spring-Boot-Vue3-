import Constants from 'expo-constants'
import { Platform } from 'react-native'

/**
 * 后端接口地址解析（后端无 /api/v1 前缀，端口 20256）：
 * 1. 优先读取 EXPO_PUBLIC_API_BASE（.env 或 shell 环境变量，支持显式覆盖）；
 * 2. 开发模式从 Expo 的 hostUri 推断开发机局域网 IP（真机经 Expo Go 直连开发机）；
 * 3. 兜底 Android 模拟器 10.0.2.2 / iOS 模拟器 localhost。
 */
export const API_BASE: string = resolveBaseUrl()

function resolveBaseUrl(): string {
  const explicit = process.env.EXPO_PUBLIC_API_BASE
  if (explicit) return explicit

  const hostUri = Constants.expoConfig?.hostUri
  if (hostUri) {
    const host = hostUri.split(':')[0]
    if (host) return `http://${host}:20256`
  }
  return Platform.OS === 'android' ? 'http://10.0.2.2:20256' : 'http://localhost:20256'
}

/** 站点名称 */
export const SITE_NAME = '走进多彩 56 个民族世界'

/**
 * 业务图片地址解析：后端下发的图片为相对路径（如 /images/ethnic/han/cover.jpg），
 * 移动端需拼接后端地址才能加载；已是绝对 URL（http/https/data/file）则原样返回。
 */
export function resolveAssetUrl(uri?: string | null): string | null {
  if (!uri) return null
  if (/^(https?:|data:|file:)/.test(uri)) return uri
  if (uri.startsWith('/')) return `${API_BASE}${uri}`
  return uri
}
