import { get, put } from '../request'
import type { ApiResult } from '../request'

/** 患者本人资料视图对象（与后端 PatientProfileVO 对齐） */
export interface PatientProfileVO {
  userId: number
  username: string
  phone: string
  name: string
  gender: 'M' | 'F' | 'U'
  birthday: string | null
  heightCm: number | null
  weightKg: number | null
  rehabPartCode: string | null
  rehabPartName: string | null
  medicalSummary: string | null
  allergyInfo: string | null
  contraindications: string | null
  lastLoginAt: string | null
  updatedAt: string | null
}

/** 患者当前有效负责治疗师视图对象（与后端 PatientTherapistVO 对齐） */
export interface PatientTherapistVO {
  bindingId: number
  therapistId: number
  name: string
  title: string | null
  specialty: string | null
  introduction: string | null
  relationType: 'PRIMARY' | 'SECONDARY'
  startAt: string
}

/** PUT /patient/profile 请求体（与后端 PatientProfileUpdateRequest 对齐） */
export interface PatientProfileUpdateRequest {
  name?: string
  gender?: 'M' | 'F' | 'U'
  birthday?: string
  heightCm?: number
  weightKg?: number
  rehabPartCode?: string
  medicalSummary?: string
  allergyInfo?: string
  contraindications?: string
}

/** GET /patient/profile：查询本人完整资料 */
export function getProfile(): Promise<ApiResult<PatientProfileVO>> {
  return get<PatientProfileVO>('/patient/profile')
}

/** PUT /patient/profile：修改本人开放字段资料 */
export function updateProfile(data: PatientProfileUpdateRequest): Promise<ApiResult<PatientProfileVO>> {
  return put<PatientProfileVO>('/patient/profile', data)
}

/** GET /patient/therapist：查询当前有效负责治疗师（无绑定返回 data:null） */
export function getMyTherapist(): Promise<ApiResult<PatientTherapistVO | null>> {
  return get<PatientTherapistVO | null>('/patient/therapist')
}
