import { del, get, post, put } from '@/api/request'
import type { PageResult } from '@/api/request'

// ==================== 状态名/标签映射（与后端枚举及患者端展示口径一致） ====================

/** 计划状态中文名映射（PlanStatus：DRAFT/PUBLISHED/PAUSED/COMPLETED/ARCHIVED） */
export const PLAN_STATUS_NAMES: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  PAUSED: '已暂停',
  COMPLETED: '已完成',
  ARCHIVED: '已归档'
}

/** 计划状态 → Vant Tag type（与患者端 PlanListView 口径一致） */
export function planStatusTagType(
  status: string
): 'success' | 'warning' | 'danger' | 'default' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'default'> = {
    DRAFT: 'default',
    PUBLISHED: 'success',
    COMPLETED: 'default',
    PAUSED: 'warning',
    ARCHIVED: 'danger'
  }
  return map[status] ?? 'default'
}

/** 每日任务状态中文名映射（DailyTaskStatus） */
export const DAILY_TASK_STATUS_NAMES: Record<string, string> = {
  PENDING: '待完成',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  SKIPPED: '已跳过',
  EXPIRED: '已过期'
}

/** 每日任务状态 → Vant Tag type（与患者端 TodayTasksView 口径一致） */
export function dailyTaskStatusTagType(
  status: string
): 'success' | 'warning' | 'danger' | 'default' | 'primary' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'default' | 'primary'> = {
    PENDING: 'default',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
    SKIPPED: 'default',
    EXPIRED: 'danger'
  }
  return map[status] ?? 'default'
}

/** 频次类型中文名映射（FrequencyType） */
export const FREQUENCY_TYPE_NAMES: Record<string, string> = {
  DAILY: '每日',
  WEEKLY: '每周',
  CUSTOM: '自定义'
}

// ==================== 数据结构（与后端 VO/DTO 字段一一对应） ====================

/** 治疗师端康复计划（列表行与版本行共用，TherapistPlanVO） */
export interface TherapistPlan {
  planId: number
  patientId?: number
  patientName?: string
  name: string
  rehabPartCode?: string
  rehabPartName?: string
  /** 计划版本号（同名计划链内递增） */
  versionNo?: number
  /** 开始日期（yyyy-MM-dd） */
  startDate?: string
  /** 结束日期（yyyy-MM-dd） */
  endDate?: string
  frequencyType?: string
  frequencyValue?: number
  status: string
  statusName?: string
  remark?: string
  /** 动作项数量 */
  itemCount?: number
  createdAt?: string
  updatedAt?: string
}

/** 治疗师端计划动作项（TherapistPlanItemVO） */
export interface TherapistPlanItem {
  planItemId: number
  planId: number
  actionVersionId: number
  actionId?: number
  actionName?: string
  versionNo?: number
  sequenceNo?: number
  targetSets?: number
  targetReps?: number
  /** 目标幅度(度) */
  targetAmplitude?: number
  /** 单组时长(秒) */
  durationSec?: number
  /** 组间休息(秒) */
  restSec?: number
  instruction?: string
}

/** 治疗师端计划详情（TherapistPlanDetailVO：基本信息+动作项+执行星期） */
export interface TherapistPlanDetail extends TherapistPlan {
  /** 执行星期（1周一~7周日，DAILY 为空） */
  weekdays?: number[]
  /** 动作项列表（按顺序号升序） */
  items?: TherapistPlanItem[]
}

/** 创建计划的动作项载荷（PlanCreateRequest.PlanItemPayload） */
export interface PlanCreateItemPayload {
  actionVersionId: number
  /** 组数 */
  sets: number
  /** 每组目标次数 */
  targetReps: number
  /** 目标幅度（度） */
  targetAngle?: number
  /** 组间休息（秒） */
  restSec?: number
}

/** 创建计划载荷（POST /therapist/patients/{patientId}/plans，初始为 DRAFT） */
export interface PlanCreatePayload {
  name: string
  rehabPartCode?: string
  startDate: string
  endDate: string
  remark?: string
  items: PlanCreateItemPayload[]
}

/** 草稿计划基本信息修改载荷（PUT /therapist/plans/{planId}，仅 DRAFT，字段可选） */
export interface PlanUpdatePayload {
  name?: string
  rehabPartCode?: string
  startDate?: string
  endDate?: string
  remark?: string
}

/** 计划动作项新增/修改载荷（PlanItemRequest） */
export interface PlanItemPayload {
  actionVersionId: number
  sequenceNo?: number
  targetSets: number
  targetReps: number
  targetAmplitude?: number
  durationSec?: number
  restSec?: number
  instruction?: string
}

/** 执行日程频次类型 */
export type FrequencyTypeValue = 'DAILY' | 'WEEKLY' | 'CUSTOM'

/** 计划执行日程设置载荷（PlanScheduleRequest） */
export interface PlanSchedulePayload {
  frequencyType: FrequencyTypeValue
  /** 频次值（如每周训练次数） */
  frequencyValue?: number
  /** 执行星期（1 周一 ~ 7 周日），WEEKLY/CUSTOM 时必填 */
  weekdays?: number[]
}

// ==================== 接口封装 ====================

/** 创建 DRAFT 计划（校验患者绑定，含动作项） */
export function createPlan(patientId: number, payload: PlanCreatePayload) {
  return post<TherapistPlanDetail>(`/therapist/patients/${patientId}/plans`, payload)
}

/** 查询计划详情（基本信息+动作项+执行日程） */
export function getPlanDetail(planId: number) {
  return get<TherapistPlanDetail>(`/therapist/plans/${planId}`)
}

/** 修改草稿计划基本信息（仅 DRAFT） */
export function updatePlan(planId: number, payload: PlanUpdatePayload) {
  return put<TherapistPlanDetail>(`/therapist/plans/${planId}`, payload)
}

/** 删除草稿计划（仅 DRAFT） */
export function deleteDraftPlan(planId: number) {
  return del<void>(`/therapist/plans/${planId}`)
}

/** 发布计划（DRAFT→PUBLISHED） */
export function publishPlan(planId: number) {
  return post<TherapistPlanDetail>(`/therapist/plans/${planId}/publish`)
}

/** 暂停计划（PUBLISHED→PAUSED，原因必填） */
export function pausePlan(planId: number, reason: string) {
  return post<void>(`/therapist/plans/${planId}/pause`, { reason })
}

/** 恢复计划（PAUSED→PUBLISHED） */
export function resumePlan(planId: number) {
  return post<void>(`/therapist/plans/${planId}/resume`)
}

/** 归档计划（PUBLISHED/PAUSED→ARCHIVED，原因必填） */
export function archivePlan(planId: number, reason: string) {
  return post<void>(`/therapist/plans/${planId}/archive`, { reason })
}

/** 版本复制（复制当前计划创建新 DRAFT 版本） */
export function createPlanVersion(planId: number) {
  return post<TherapistPlanDetail>(`/therapist/plans/${planId}/versions`)
}

/** 分页查询同名计划版本链 */
export function listPlanVersions(planId: number, params: { page?: number; size?: number }) {
  return get<PageResult<TherapistPlan>>(`/therapist/plans/${planId}/versions`, params)
}

/** 分页查询计划动作项 */
export function listPlanItems(planId: number, params: { page?: number; size?: number }) {
  return get<PageResult<TherapistPlanItem>>(`/therapist/plans/${planId}/items`, params)
}

/** 新增草稿动作项（仅 DRAFT） */
export function addPlanItem(planId: number, payload: PlanItemPayload) {
  return post<TherapistPlanItem>(`/therapist/plans/${planId}/items`, payload)
}

/** 修改草稿动作项（仅 DRAFT） */
export function updatePlanItem(planId: number, itemId: number, payload: PlanItemPayload) {
  return put<TherapistPlanItem>(`/therapist/plans/${planId}/items/${itemId}`, payload)
}

/** 删除草稿动作项（仅 DRAFT） */
export function deletePlanItem(planId: number, itemId: number) {
  return del<void>(`/therapist/plans/${planId}/items/${itemId}`)
}

/** 设置执行日程（DAILY/WEEKLY 频率+执行星期，仅 DRAFT） */
export function updatePlanSchedule(planId: number, payload: PlanSchedulePayload) {
  return put<TherapistPlanDetail>(`/therapist/plans/${planId}/schedule`, payload)
}
