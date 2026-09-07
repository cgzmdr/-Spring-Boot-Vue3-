import router from './index'
import { useUserStore } from '@/stores/user'

// 全局前置守卫：未登录跳转登录页
router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title as string} · 56民族OA` : '56民族OA'
  const userStore = useUserStore()

  if (to.meta.requiresAuth === false) {
    return true
  }

  // 有 token 但用户信息为空（如整页刷新后 store 被重置）时重新拉取
  if (localStorage.getItem('satoken') && !userStore.roles.length) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (!userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})
