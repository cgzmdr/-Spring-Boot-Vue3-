import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from './types'

/** 统一错误码（对齐 API.md §11，以 openapi.json 实际返回为准） */
export const AUTH_CODES = [1003, 1005] // 未登录 / token 过期

/** 防止多个并发请求同时失效导致重复弹窗与重复跳转 */
let redirectingToLogin = false

/** 登录态失效统一处理：清空本地状态并返回登录页（保留当前地址便于登录后回跳） */
function handleAuthExpired() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  const userStore = useUserStore()
  userStore.reset()
  const current = location.pathname + location.search
  if (!current.startsWith('/login')) {
    ElMessage.error('登录已失效，请重新登录')
    router.replace({ path: '/login', query: { redirect: current } })
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
  // 同源代理下自动携带 Cookie（sa-token 登录态依赖 Cookie）；直连跨域时需后端 CORS 开启凭证
  withCredentials: true
})

// 请求拦截器：注入 satoken 请求头（sa-token 默认从 header 读取 token）
service.interceptors.request.use((config) => {
  const token = localStorage.getItem('satoken')
  if (token) {
    config.headers.satoken = token
  }
  return config
})

// 响应拦截器：统一判定 code === 0
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 任意一次成功响应说明链路可用，重置失效跳转标志（重新登录后生效）
    redirectingToLogin = false
    const res = response.data
    // 非标准包装（如 204 无内容）直接放行
    if (!res || typeof res !== 'object' || !('code' in res)) {
      return response
    }
    if (res.code === 0) {
      return response
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const res = error.response?.data as ApiResponse | undefined
    if (res && AUTH_CODES.includes(res.code)) {
      handleAuthExpired()
    } else if (res && res.code === 1004) {
      ElMessage.error('无权限执行该操作')
    } else {
      ElMessage.error(res?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

/** 泛型请求方法：直接返回 data */
export async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  const res = await service.request<ApiResponse<T>>(config)
  return res.data.data
}

/** GET */
export function get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: 'get', params })
}

/** POST */
export function post<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'post', data })
}

/** PUT */
export function put<T = unknown>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'put', data })
}

/** DELETE */
export function del<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: 'delete', params })
}

export default service
