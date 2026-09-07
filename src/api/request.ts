import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types'

/** 是否已派发登录提示（避免并发重复弹窗） */
let authNoticeScheduled = false

/** 触发全局"需要登录"事件（登录弹窗监听） */
export function notifyLoginRequired() {
  if (authNoticeScheduled) return
  authNoticeScheduled = true
  window.dispatchEvent(new CustomEvent('auth:required'))
  setTimeout(() => (authNoticeScheduled = false), 1500)
}

/** Token 存取 */
export const tokenStore = {
  get: () => localStorage.getItem('cend_token') || '',
  set: (t: string) => localStorage.setItem('cend_token', t),
  clear: () => localStorage.removeItem('cend_token'),
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/backend-api',
  timeout: 20000,
})

request.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse): any => {
    const res: unknown = response.data
    // 兼容非统一包装（如 sitemap.xml 等原始响应）
    if (res && typeof res === 'object' && 'code' in res) {
      const body = res as ApiResponse
      if (body.code === 0) {
        return body.data
      }
      if (body.code === 1003 || body.code === 1005) {
        tokenStore.clear()
        notifyLoginRequired()
        return Promise.reject(new Error(body.message || '未登录'))
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return res
  },
  (error: unknown) => {
    const message =
      axios.isAxiosError(error) || error instanceof Error
        ? error.message
        : '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

/** 泛型请求：data 已解包为业务数据 */
export function http<T>(config: AxiosRequestConfig): Promise<T> {
  return request.request(config) as unknown as Promise<T>
}

export const get = <T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
  http<T>({ url, method: 'GET', params, ...config })

export const post = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  http<T>({ url, method: 'POST', data, ...config })

export const put = <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
  http<T>({ url, method: 'PUT', data, ...config })

export const del = <T>(url: string, config?: AxiosRequestConfig) =>
  http<T>({ url, method: 'DELETE', ...config })
