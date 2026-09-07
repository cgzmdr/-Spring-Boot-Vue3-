import { create } from 'zustand'
import AsyncStorage from '@react-native-async-storage/async-storage'
import { http } from '../api/client'
import type { UserInfo } from '../api/types'

const TOKEN_KEY = 'satoken'

interface AuthState {
  token: string | null
  user: UserInfo | null
  /** 登录态初始化中（App 启动恢复 token 时） */
  booting: boolean
  restore: () => Promise<void>
  login: (account: string, password: string) => Promise<void>
  register: (nickname: string, password: string) => Promise<void>
  fetchMe: () => Promise<UserInfo>
  logout: () => Promise<void>
  /** 仅清空本地（登录失效时由请求层调用） */
  clear: () => void
}

async function persistToken(token: string | null) {
  try {
    if (token) {
      await AsyncStorage.setItem(TOKEN_KEY, token)
    } else {
      await AsyncStorage.removeItem(TOKEN_KEY)
    }
  } catch {
    // 存储失败不阻断流程
  }
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  user: null,
  booting: true,

  /** 启动时从本地恢复 token 并拉取用户信息（失败不阻塞浏览） */
  restore: async () => {
    try {
      const token = await AsyncStorage.getItem(TOKEN_KEY)
      if (token) {
        set({ token })
        try {
          const user = await http<UserInfo>({ url: '/auth/me', method: 'GET' })
          set({ user })
        } catch {
          // token 失效由请求层统一清理
        }
      }
    } catch {
      // 忽略存储异常
    } finally {
      set({ booting: false })
    }
  },

  login: async (account, password) => {
    const token = await http<string>({
      url: '/auth/login',
      method: 'POST',
      data: { account, password },
    })
    set({ token })
    await persistToken(token)
    const user = await http<UserInfo>({ url: '/auth/me', method: 'GET' })
    set({ user })
  },

  register: async (nickname, password) => {
    await http<unknown>({
      url: '/auth/users',
      method: 'POST',
      data: { nickname, password },
    })
  },

  fetchMe: async () => {
    const user = await http<UserInfo>({ url: '/auth/me', method: 'GET' })
    set({ user })
    return user
  },

  logout: async () => {
    try {
      await http<unknown>({ url: '/auth/logout', method: 'POST' })
    } catch {
      // 服务端登出失败也继续清本地
    }
    set({ token: null, user: null })
    await persistToken(null)
  },

  clear: () => {
    set({ token: null, user: null })
    persistToken(null)
  },
}))

/** 便捷判断 */
export const isLoggedIn = () => !!useAuthStore.getState().token
