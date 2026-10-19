import { get, post, put } from '../request'
import type { PageResult } from '../request'

/** 关系类型：PRIMARY 主责 / SECONDARY 协作 */
export type RelationType = 'PRIMARY' | 'SECONDARY'

/** 绑定状态：ACTIVE 有效 / ENDED 已结束 */
export type BindingStatus = 'ACTIVE' | 'ENDED'

/** 医患绑定关系项 */
export interface Binding {
  bindingId: number
  patientId: number
  patientName: string
  therapistId: number
  therapistName: string
  relationType: RelationType | string
  relationTypeName: string
  status: BindingStatus | string
  statusName: string
  startAt?: string
  endAt?: string
  createdBy?: number
  createdAt?: string
}

/** 建立绑定请求 */
export interface BindingCreatePayload {
  patientId: number
  therapistId: number
  relationType: RelationType
  startAt?: string
  endAt?: string
}

/** 变更绑定请求（字段均可选） */
export interface BindingUpdatePayload {
  relationType?: RelationType
  startAt?: string
  endAt?: string
}

/** 绑定分页查询参数 */
export interface BindingPageParams {
  page?: number
  size?: number
  patientId?: number
  therapistId?: number
  status?: string
}

/**
 * 管理端 - 分页查询绑定关系
 * GET /admin/bindings
 */
export function pageBindings(params: BindingPageParams) {
  return get<PageResult<Binding>>('/admin/bindings', params)
}

/**
 * 管理端 - 建立医患绑定
 * POST /admin/bindings
 */
export function createBinding(data: BindingCreatePayload) {
  return post<number>('/admin/bindings', data)
}

/**
 * 管理端 - 变更关系类型或生效期
 * PUT /admin/bindings/{bindingId}
 */
export function updateBinding(bindingId: number, data: BindingUpdatePayload) {
  return put<null>(`/admin/bindings/${bindingId}`, data)
}

/**
 * 管理端 - 解除绑定（历史数据保留只读）
 * POST /admin/bindings/{bindingId}/unbind
 */
export function unbindBinding(bindingId: number, reason: string) {
  return post<null>(`/admin/bindings/${bindingId}/unbind`, { reason })
}
