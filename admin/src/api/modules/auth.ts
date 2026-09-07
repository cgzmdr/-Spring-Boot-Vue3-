import { get, post } from '../request'

/** 登录账号密码 */
export interface LoginParams {
  account: string
  password: string
}

/** 当前用户信息（/auth/me） */
export interface MeInfo {
  id: unknown
  nickname: string
  avatar?: string
  roles: string[]
}

/**
 * 用户登录
 * 注意：openapi 中登录响应 data 为字符串 token（非 {tokenName, tokenValue}）
 */
export function login(data: LoginParams): Promise<string> {
  return post<string>('/auth/login', data)
}

/** 用户登出 */
export function logout(): Promise<string> {
  return post<string>('/auth/logout')
}

/** 当前用户信息 */
export function me(): Promise<MeInfo> {
  return get<MeInfo>('/auth/me')
}

/** 刷新 Token */
export function refresh(): Promise<string> {
  return post<string>('/auth/refresh')
}
