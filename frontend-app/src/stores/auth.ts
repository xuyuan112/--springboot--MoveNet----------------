import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { LoginParams, Role, UserInfo } from '@/api/auth'

const TOKEN_KEY = 'rehab_token'
const USER_KEY = 'rehab_user'

interface AuthState {
  token: string
  userInfo: UserInfo | null
}

/** 从 localStorage 恢复用户信息（容错：损坏的 JSON 按未登录处理） */
function readStoredUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    // 页面刷新时从 localStorage 恢复登录态
    token: localStorage.getItem(TOKEN_KEY) ?? '',
    userInfo: readStoredUser()
  }),

  getters: {
    isLoggedIn: (state): boolean => !!state.token,
    role: (state): Role | null => state.userInfo?.role ?? null
  },

  actions: {
    /**
     * 登录：调接口成功后持久化 token 与用户信息
     * @returns 当前登录用户信息
     */
    async login(payload: LoginParams): Promise<UserInfo> {
      const res = await authApi.login(payload)
      const { accessToken, user } = res.data

      this.token = accessToken
      this.userInfo = user
      localStorage.setItem(TOKEN_KEY, accessToken)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },

    /** 退出登录：通知后端失效令牌，无论接口成功与否都清理本地登录态 */
    async logout(): Promise<void> {
      try {
        await authApi.logout()
      } catch {
        // 令牌可能已在服务端失效，忽略接口错误，本地仍需清理
      }
      this.reset()
    },

    /** 清理全部本地登录态 */
    reset(): void {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
