import { get, post } from '../request'
import type { PageResult } from '../request'

/** 备份任务类型 */
export type BackupTaskType = 'BACKUP' | 'PRE_RESTORE_SNAPSHOT'

/** 备份任务状态 */
export type BackupTaskStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'

/** 备份任务分页参数 */
export interface BackupPageParams {
  page?: number
  size?: number
  status?: string
  taskType?: string
}

/** 备份任务 */
export interface BackupTask {
  backupId: number
  taskType: BackupTaskType | string
  taskTypeName: string
  status: BackupTaskStatus | string
  statusName: string
  filePath?: string
  fileSize: number
  sha256?: string
  errorMsg?: string
  triggeredBy?: number
  operatorName?: string
  relatedBackupId?: number
  restoreReason?: string
  startedAt: string
  finishedAt?: string
  createdAt: string
  /** 仅详情接口、对成功备份一次性下发（5 分钟有效） */
  confirmToken?: string
}

/** 恢复请求载荷 */
export interface BackupRestorePayload {
  confirmToken: string
  reason: string
}

/**
 * POST /admin/backups 创建备份（异步），返回任务ID
 */
export function createBackup() {
  return post<number>('/admin/backups', {})
}

/**
 * GET /admin/backups 分页查询备份任务
 */
export function pageBackups(params: BackupPageParams) {
  return get<PageResult<BackupTask>>('/admin/backups', params)
}

/**
 * GET /admin/backups/{id} 详情（成功备份返回一次性确认码）
 */
export function getBackupDetail(id: number) {
  return get<BackupTask>(`/admin/backups/${id}`)
}

/**
 * POST /admin/backups/{id}/restore 二次确认恢复
 */
export function restoreBackup(id: number, data: BackupRestorePayload) {
  return post<null>(`/admin/backups/${id}/restore`, data)
}
