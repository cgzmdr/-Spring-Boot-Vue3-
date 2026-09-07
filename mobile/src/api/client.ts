import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { API_BASE } from '../config'
import { useAuthStore } from '../stores/auth'
import type { ApiResponse } from './types'

/** 认证失效（未登录 / token 过期）错误码 */
const AUTH_CODES = [1003, 1005]

type AuthExpiredHandler = () => void
let authExpiredHandler: AuthExpiredHandler | null = null

/**
 * 注册「登录失效」回调（导航层调用，跳转登录页）。
 * 同一时刻仅一个监听者。
 */
export function onAuthExpired(handler: AuthExpiredHandler | null) {
  authExpiredHandler = handler
}

const service: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：注入 satoken 请求头
service.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.satoken = token
  }
  return config
})

// 响应拦截器：解包 { code, message, data }
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse | undefined
    if (!res || typeof res !== 'object' || !('code' in res)) {
      return response
    }
    if (res.code === 0) {
      return response
    }
    if (AUTH_CODES.includes(res.code)) {
      // 清空本地登录态并通知导航层跳转登录页
      useAuthStore.getState().clear()
      authExpiredHandler?.()
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => Promise.reject(error)
)

/** 发起请求并直接返回业务 data */
export async function http<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await service.request<ApiResponse<T>>(config)
  return res.data.data as T
}
