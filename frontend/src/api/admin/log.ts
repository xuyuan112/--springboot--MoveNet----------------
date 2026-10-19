import { get } from '../request'
import type { PageResult } from '../request'

/** 操作日志查询参数（时间格式 yyyy-MM-dd HH:mm:ss） */
export interface OperationLogPageParams {
  page?: number
  size?: number
  module?: string
  operatorId?: number
  from?: string
  to?: string
}

/** 操作审计日志 */
export interface OperationLog {
  logId: number
  userId?: number
  userName?: string
  module: string
  action: string
  targetType?: string
  targetId?: number
  ip: string
  deviceId?: string
  /** SUCCESS / FAIL */
  status: string
  /** JSON 字符串：请求参数或失败原因 */
  detail?: string
  createdAt: string
}

/**
 * GET /admin/operation-logs 分页审计查询
 */
export function pageOperationLogs(params: OperationLogPageParams) {
  return get<PageResult<OperationLog>>('/admin/operation-logs', params)
}
