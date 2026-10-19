import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse } from 'axios'
import { showFailToast, showToast } from 'vant'

/**
 * 后端统一响应结构
 * @example {"code":"0","message":"OK","data":{},"traceId":"01J..."}
 */
export interface ApiResult<T = unknown> {
  /** 业务状态码，"0" 表示成功 */
  code: string
  message: string
  data: T
  traceId?: string
}

/**
 * 分页数据结构（page 从 1 开始）
 * @example { "records": [], "page": 1, "size": 20, "total": 0 }
 */
export interface PageResult<T> {
  records: T[]
  page: number
  size: number
  total: number
}

const TOKEN_KEY = 'rehab_token'
const USER_KEY = 'rehab_user'

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

// ===== 请求拦截器：注入 Bearer Token =====
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

// ===== 响应拦截器：统一解包与错误处理 =====
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const body = response.data

    // HTTP 200：以业务码 code === '0' 作为成功判定
    if (body.code === '0') {
      // 拦截器在运行时直接返回响应体，类型在下方 http 封装中校正
      return body as unknown as AxiosResponse
    }

    showFailToast(body.message || '请求失败')
    return Promise.reject(new Error(body.message || 'Business Error'))
  },
  (error: AxiosError<ApiResult>) => {
    const status = error.response?.status

    // HTTP 401 有两类语义：
    // 1) 登录接口本身返回 401（密码/账号错误）：不清理登录态、不跳转，仅提示业务消息
    // 2) 其他接口返回 401（令牌缺失/过期）：清理登录态并跳转登录页
    const isLoginRequest = error.config?.url?.includes('/auth/login') ?? false
    if (status === 401 && isLoginRequest) {
      const message = error.response?.data?.message || '用户名或密码错误'
      showFailToast(message)
      return Promise.reject(error)
    }

    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      showToast('登录状态已过期，请重新登录')

      const currentPath = window.location.pathname + window.location.search
      if (!currentPath.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
      } else {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }

    const message = error.response?.data?.message || error.message || '网络异常，请稍后重试'
    showFailToast(message)
    return Promise.reject(error)
  }
)

/**
 * GET 请求封装
 * @returns 后端完整响应体 { code, message, data, traceId }
 */
export function get<T = unknown>(url: string, params?: object): Promise<ApiResult<T>> {
  return service.get(url, { params }) as unknown as Promise<ApiResult<T>>
}

/**
 * POST 请求封装
 * @param headers 可选自定义请求头（如 X-Pose-Engine-Id 引擎校验）
 * @returns 后端完整响应体 { code, message, data, traceId }
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  headers?: Record<string, string>
): Promise<ApiResult<T>> {
  return service.post(url, data, headers ? { headers } : undefined) as unknown as Promise<ApiResult<T>>
}

/**
 * PUT 请求封装
 * @returns 后端完整响应体 { code, message, data, traceId }
 */
export function put<T = unknown>(url: string, data?: unknown): Promise<ApiResult<T>> {
  return service.put(url, data) as unknown as Promise<ApiResult<T>>
}

/**
 * DELETE 请求封装
 * @param params 可选查询参数
 * @param data 可选请求体（如撤回训练记录必须提交原因）
 * @returns 后端完整响应体 { code, message, data, traceId }
 */
export function del<T = unknown>(url: string, params?: object, data?: unknown): Promise<ApiResult<T>> {
  return service.delete(url, { params, data }) as unknown as Promise<ApiResult<T>>
}

export default service
