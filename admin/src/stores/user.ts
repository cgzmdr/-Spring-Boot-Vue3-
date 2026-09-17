import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, me as meApi } from '@/api/modules/auth'

const TOKEN_KEY = 'satoken'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const nickname = ref('')
  const avatar = ref('')
  const roles = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /**
   * 从 localStorage 同步 token。
   *
   * 为什么需要它：`token` 只在 store **首次创建**时读一次 localStorage。
   * 若外部（另一个标签页、自动化脚本、或守卫之前的一次 fetch 登录）写了 satoken，
   * 已存在的 store 实例不会自动感知，`isLoggedIn` 会一直是 false，
   * 表现为「明明有 token 却反复被弹回登录页」。进入路由守卫前显式同步即可消除该竞态。
   */
  function syncTokenFromStorage() {
    const stored = localStorage.getItem(TOKEN_KEY) || ''
    if (stored !== token.value) {
      token.value = stored
    }
    return token.value
  }

  /** 登录：openapi 契约登录响应 data 为字符串 token */
  async function login(account: string, password: string) {
    const tk = await loginApi({ account, password })
    token.value = tk
    localStorage.setItem(TOKEN_KEY, tk)
    await fetchUserInfo()
    return tk
  }

  /** 拉取当前用户信息 */
  async function fetchUserInfo() {
    const info = await meApi()
    nickname.value = info.nickname
    avatar.value = info.avatar || ''
    roles.value = info.roles || []
    return info
  }

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常
    }
    reset()
  }

  /** 本地清理（token 失效时调用） */
  function reset() {
    token.value = ''
    nickname.value = ''
    avatar.value = ''
    roles.value = []
    localStorage.removeItem(TOKEN_KEY)
  }

  /** 是否有指定角色（admin 后端角色 code 如 super_admin / reviewer 等） */
  function hasRole(...codes: string[]) {
    return roles.value.some((r) => codes.includes(r)) || roles.value.includes('super_admin')
  }

  return {
    token,
    nickname,
    avatar,
    roles,
    isLoggedIn,
    login,
    fetchUserInfo,
    logout,
    reset,
    hasRole,
    syncTokenFromStorage
  }
})
