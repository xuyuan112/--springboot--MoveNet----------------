import { get } from '@/api/request'
import type { ApiResult, PageResult } from '@/api/request'

/** 日级完成度（当日已完成 / 当日应执行，不含 SKIPPED） */
export interface DayLevel {
  date: string
  doneTasks: number
  totalTasks: number
  /** 完成百分比 0-100 */
  percent: number
}

/** 计划级完成度（查询范围内累计已完成 / 累计已生成，不含 SKIPPED） */
export interface PlanLevel {
  completedTasks: number
  totalTasks: number
  percent: number
}

/** 动作项级完成度 */
export interface ActionItemLevel {
  planItemId: number
  actionName: string
  planName: string
  completedTasks: number
  totalTasks: number
  percent: number
}

/** 三级完成度聚合（GET /patient/progress 的 summary） */
export interface ProgressSummary {
  day: DayLevel
  plan: PlanLevel
  actionItems: ActionItemLevel[]
}

/** 任务完成明细行 */
export interface TaskProgressItem {
  taskId: number
  taskDate: string
  planId: number
  planName: string
  planItemId: number
  actionName: string
  targetSets: number
  targetReps: number
  correctReps: number
  status: string
  statusName: string
  progressPercent: number
  completedAt?: string
}

/** GET /patient/progress 响应：三级完成度 + 任务完成明细分页 */
export interface ProgressPage {
  summary: ProgressSummary
  records: PageResult<TaskProgressItem>
}

/** 趋势数据点（排除 INSUFFICIENT 记录） */
export interface TrendPoint {
  /** DAY=yyyy-MM-dd；WEEK=该周周一日期 */
  date: string
  recordCount: number
  avgScore: number
  /** 平均达标率 0-1 */
  avgCompletionRate: number
  totalReps: number
  correctReps: number
}

/** 查询本人进度（三级完成度 + 任务明细分页） */
export function getProgress(params: {
  page?: number
  size?: number
  /** yyyy-MM-dd，任务日期过滤 */
  from?: string
  to?: string
  planId?: number
}) {
  return get<ProgressPage>('/patient/progress', params)
}

/** 查询训练趋势（得分/达标率/次数；granularity 仅支持 DAY/WEEK） */
export function getTrends(params: {
  granularity?: 'DAY' | 'WEEK'
  /** yyyy-MM-dd，按记录开始时间过滤 */
  from?: string
  to?: string
  actionVersionId?: number
}) {
  return get<TrendPoint[]>('/patient/trends', params)
}
