import { get } from '@/api/request'
import type { PageResult } from '@/api/request'
import type { TherapistPlan } from './plan'

/** 性别展示名映射（M男/F女/U未知） */
export const GENDER_NAMES: Record<string, string> = { M: '男', F: '女', U: '未知' }

/** 绑定关系展示名映射（PRIMARY主治/SECONDARY协作） */
export const RELATION_TYPE_NAMES: Record<string, string> = { PRIMARY: '主治', SECONDARY: '协作' }

/** 治疗师端患者列表项（GET /therapist/patients，与后端 TherapistPatientVO 对齐） */
export interface TherapistPatient {
  patientId: number
  name: string
  gender?: string
  /** 出生日期（yyyy-MM-dd） */
  birthday?: string
  /** 手机号（后端已脱敏） */
  phone?: string
  rehabPartName?: string
  relationType?: string
  /** 绑定开始时间 */
  bindingStartAt?: string
  /** 进行中（PUBLISHED/PAUSED）计划数 */
  activePlanCount?: number
  /** 最近一次计划状态（无计划为 null） */
  latestPlanStatus?: string
  latestPlanStatusName?: string
  /** 近 7 日任务完成率（0-100，无任务为 null） */
  recentCompletionRate?: number | null
  /** 是否存在风险提示（仅提示，不构成医学判断） */
  hasRiskHint?: boolean
}

/** 治疗师端患者详情（GET /therapist/patients/{patientId}，与后端 TherapistPatientDetailVO 对齐） */
export interface TherapistPatientDetail extends TherapistPatient {
  /** 身高(cm) */
  heightCm?: number
  /** 体重(kg) */
  weightKg?: number
  medicalHistory?: string
  allergyInfo?: string
  contraindication?: string
  /** 风险提示列表（量化口径启发式提示，非医学诊断） */
  riskHints?: string[]
}

/** 治疗师端患者每日任务项（GET /therapist/patients/{patientId}/tasks，与后端 TherapistPatientTaskVO 对齐） */
export interface TherapistPatientTask {
  taskId: number
  /** 任务日期（yyyy-MM-dd） */
  taskDate: string
  status: string
  statusName?: string
  planId?: number
  planName?: string
  actionName?: string
  sequenceNo?: number
  targetSets?: number
  targetReps?: number
}

/** 治疗师端患者训练异常提示（GET /therapist/patients/{patientId}/alerts，与后端 TherapistAlertVO 对齐） */
export interface TherapistAlert {
  /** 提示类型：LOW_SCORE/NO_COMPLETION_3D/PAIN_FEEDBACK */
  alertType: string
  alertTypeName?: string
  /** 发生日期（yyyy-MM-dd） */
  alertDate: string
  /** 提示内容（规避医学诊断表述） */
  content: string
  refId?: number
}

/** 分页查询当前有效绑定患者（keyword 匹配姓名/手机号） */
export function listTherapistPatients(params: {
  page?: number
  size?: number
  keyword?: string
  partCode?: string
  planStatus?: string
}) {
  return get<PageResult<TherapistPatient>>('/therapist/patients', params)
}

/** 查询患者详情（档案+绑定信息+风险提示，敏感字段脱敏） */
export function getTherapistPatientDetail(patientId: number) {
  return get<TherapistPatientDetail>(`/therapist/patients/${patientId}`)
}

/** 分页查询患者计划（本人创建，可按状态过滤） */
export function listPatientPlansOf(
  patientId: number,
  params: { page?: number; size?: number; status?: string }
) {
  return get<PageResult<TherapistPlan>>(`/therapist/patients/${patientId}/plans`, params)
}

/** 分页查询患者每日任务（仅本人创建计划产生的任务） */
export function listPatientTasksOf(
  patientId: number,
  params: { page?: number; size?: number; from?: string; to?: string; status?: string }
) {
  return get<PageResult<TherapistPatientTask>>(`/therapist/patients/${patientId}/tasks`, params)
}

/** 查询患者训练异常提示（近 7 日窗口，仅提示不构成医学诊断） */
export function listPatientAlertsOf(patientId: number, params: { page?: number; size?: number }) {
  return get<PageResult<TherapistAlert>>(`/therapist/patients/${patientId}/alerts`, params)
}
