import { get, post, put } from '../request'
import type { PageResult } from '../request'

/** 角色编码（管理端仅创建患者/治疗师） */
export type ManagedRole = 'PATIENT' | 'THERAPIST'

/** 性别码值 */
export type GenderCode = 'M' | 'F' | 'U'

/** 管理端账号列表项 */
export interface AdminUser {
  userId: number
  username: string
  roleCode: string
  roleName: string
  phone: string
  realName: string
  /** 1 启用 / 0 禁用 */
  status: number
  lastLoginAt?: string
  createdAt?: string
}

/** 管理端账号详情 */
export interface AdminUserDetail {
  userId: number
  username: string
  roleCode: string
  roleName: string
  phone?: string
  status: number
  realName?: string
  gender?: GenderCode
  birthDate?: string
  heightCm?: number
  weightKg?: number
  medicalHistory?: string
  allergyInfo?: string
  contraindication?: string
  title?: string
  specialty?: string
  licenseNo?: string
  introduction?: string
  lastLoginAt?: string
  createdAt?: string
}

/** 创建账号请求 */
export interface AdminUserCreatePayload {
  username: string
  roleCode: ManagedRole
  phone?: string
  realName: string
  gender?: GenderCode
  birthDate?: string
  heightCm?: number
  weightKg?: number
  medicalHistory?: string
  allergyInfo?: string
  contraindication?: string
  title?: string
  specialty?: string
  licenseNo?: string
  introduction?: string
}

/** 修改账号资料请求（字段均可选） */
export type AdminUserUpdatePayload = Partial<Omit<AdminUserCreatePayload, 'username' | 'roleCode'>>

/** 创建账号结果 */
export interface AdminUserCreateResult {
  userId: number
  username: string
  roleCode: string
  initialPasswordDelivered: boolean
  mustChangePassword: boolean
}

/** 登录日志项 */
export interface LoginLog {
  logId: number
  userId?: number
  action: string
  status: string
  ip?: string
  deviceId?: string
  message?: string
  createdAt?: string
}

/** 账号分页查询参数 */
export interface UserPageParams {
  page?: number
  size?: number
  role?: string
  status?: number
  keyword?: string
}

/** 登录日志查询参数 */
export interface LoginLogPageParams {
  page?: number
  size?: number
  success?: boolean
}

/**
 * 管理端 - 分页查询账号
 * GET /admin/users
 */
export function pageUsers(params: UserPageParams) {
  return get<PageResult<AdminUser>>('/admin/users', params)
}

/**
 * 管理端 - 创建患者/治疗师账号
 * POST /admin/users
 */
export function createUser(data: AdminUserCreatePayload) {
  return post<AdminUserCreateResult>('/admin/users', data)
}

/**
 * 管理端 - 查询账号详情
 * GET /admin/users/{userId}
 */
export function getUserDetail(userId: number) {
  return get<AdminUserDetail>(`/admin/users/${userId}`)
}

/**
 * 管理端 - 修改账号非敏感资料
 * PUT /admin/users/{userId}
 */
export function updateUser(userId: number, data: AdminUserUpdatePayload) {
  return put<null>(`/admin/users/${userId}`, data)
}

/**
 * 管理端 - 启用账号
 * POST /admin/users/{userId}/enable
 */
export function enableUser(userId: number) {
  return post<null>(`/admin/users/${userId}/enable`)
}

/**
 * 管理端 - 禁用账号（令牌立即失效）
 * POST /admin/users/{userId}/disable
 */
export function disableUser(userId: number, reason: string) {
  return post<null>(`/admin/users/${userId}/disable`, { reason })
}

/**
 * 管理端 - 重置密码（不返回明文）
 * POST /admin/users/{userId}/reset-password
 */
export function resetUserPassword(userId: number) {
  return post<null>(`/admin/users/${userId}/reset-password`)
}

/**
 * 管理端 - 分页查询登录日志
 * GET /admin/users/{userId}/login-logs
 */
export function pageLoginLogs(userId: number, params: LoginLogPageParams) {
  return get<PageResult<LoginLog>>(`/admin/users/${userId}/login-logs`, params)
}
