import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/modules'
import { tokenStore } from '@/api/request'
import { useLangStore } from '@/stores/lang'
import type { UserInfo } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStore.get())
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value && !!user.value)

  function applyToken(t: string) {
    token.value = t
    tokenStore.set(t)
  }

  async function login(account: string, password: string) {
    const t = await authApi.login(account, password)
    applyToken(t)
    await fetchMe()
  }

  async function register(nickname: string, password: string) {
    await authApi.register(nickname, password)
    await login(nickname, password)
  }

  async function fetchMe() {
    try {
      user.value = await authApi.me()
      // 登录后恢复用户持久化的界面语言（i18n 偏好随账号存储）
      const langStore = useLangStore()
      if (user.value?.lang) langStore.applyFromUser(user.value.lang)
    } catch {
      logout()
      throw new Error('登录状态失效')
    }
  }

  async function logout() {
    try {
      if (token.value) await authApi.logout()
    } catch {
      /* 忽略登出异常，本地态照常清理 */
    }
    token.value = ''
    tokenStore.clear()
    user.value = null
  }

  /** 应用启动时静默恢复登录态 */
  async function restore() {
    if (!token.value) return false
    try {
      await fetchMe()
      return true
    } catch {
      return false
    }
  }

  return { token, user, isLoggedIn, applyToken, login, register, fetchMe, logout, restore }
})
