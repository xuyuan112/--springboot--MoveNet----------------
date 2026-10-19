import { get } from '@/api/request'
import type { ApiResult, PageResult } from '@/api/request'

/** 患者端康复计划 */
export interface PatientPlan {
  planId: number
  name: string
  rehabPartCode: string
  rehabPartName?: string
  therapistName?: string
  versionNo: number
  startDate: string
  endDate: string
  frequencyType: string
  frequencyValue?: number
  status: string
  statusName?: string
  remark?: string
  createdAt: string
}

/** 患者端计划动作项 */
export interface PatientPlanItem {
  planItemId: number
  planId: number
  actionVersionId: number
  actionId?: number
  actionName?: string
  versionNo?: number
  sequenceNo: number
  targetSets: number
  targetReps: number
  targetAmplitude?: number
  durationSec?: number
  restSec?: number
  instruction?: string
}

/** 分页查询本人计划 */
export function getMyPlans(params: { page?: number; size?: number; status?: string }) {
  return get<PageResult<PatientPlan>>('/patient/plans', params)
}

/** 查询计划详情 */
export function getMyPlanDetail(planId: number) {
  return get<PatientPlan>(`/patient/plans/${planId}`)
}

/** 查询计划动作项 */
export function getMyPlanItems(planId: number) {
  return get<PatientPlanItem[]>(`/patient/plans/${planId}/items`)
}
