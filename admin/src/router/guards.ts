import router from './index'
import { useUserStore } from '@/stores/user'

/**
 * 注册全局路由守卫。
 *
 * **调用时机（见 main.ts）：`app.use(createPinia())` 之后、`app.use(router)` 之前。**
 *  - 必须在 pinia 之后：守卫里使用 `useUserStore()`，需要 Pinia 上下文；
 *  - 必须在 router 之前：vue-router 在 `use()` 时会立即发起首次导航，
 *    晚注册的守卫不会作用于首个导航，未登录用户会直接落到受保护路由。
 *
 * 因此把守卫从「模块顶层副作用」改造成显式初始化函数，消除时序依赖。
 */
export function setupRouterGuards() {
  router.beforeEach(async (to) => {
    document.title = to.meta.title ? `${to.meta.title as string} · 56民族OA` : '56民族OA'
    const userStore = useUserStore()

    // 每次导航先与 localStorage 对齐 token：
    // store 只在创建时读一次 localStorage；若登录/登出发生在别的上下文
    // （另一标签页、自动化脚本、守卫之前的一次 fetch 登录），不同步就会出现
    // 「明明有 token 却反复被弹回登录页」。
    userStore.syncTokenFromStorage()

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
}

export default setupRouterGuards
