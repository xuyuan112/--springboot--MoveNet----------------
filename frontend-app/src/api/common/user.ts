import { get } from '../request'
import type { ApiResult, PageResult } from '../request'

/** 本人登录日志项（与后端 LoginLogVO 对齐） */
export interface LoginLogVO {
  logId: number
  action: 'LOGIN' | 'LOGOUT'
  status: 'SUCCESS' | 'FAIL'
  ip: string | null
  deviceId: string | null
  message: string | null
  createdAt: string
}

/** GET /users/me/login-logs：分页查询本人登录日志（三端共用） */
export function getMyLoginLogs(params: { page: number; size: number }): Promise<ApiResult<PageResult<LoginLogVO>>> {
  return get<PageResult<LoginLogVO>>('/users/me/login-logs', params)
}
