import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

/** 公告状态 */
export type AnnouncementStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'

/** 可见范围 */
export type AnnouncementScope = 'ALL' | 'PATIENT' | 'THERAPIST'

/** 公告分页查询参数 */
export interface AnnouncementPageParams {
  page?: number
  size?: number
  status?: string
  scope?: string
  keyword?: string
}

/** 公告（列表/详情） */
export interface AdminAnnouncement {
  announcementId: number
  title: string
  content: string
  scope: string
  scopeName: string
  status: string
  statusName: string
  startTime: string
  endTime: string
  createdBy?: number
  creatorName?: string
  createdAt?: string
  updatedAt?: string
}

/** 公告创建/修改载荷（时间格式 yyyy-MM-dd HH:mm:ss，草稿可空） */
export interface AnnouncementSavePayload {
  title: string
  content: string
  scope: AnnouncementScope | string
  startTime?: string
  endTime?: string
}

/** 发布载荷（必须指定有效期） */
export interface AnnouncementPublishPayload {
  startTime: string
  endTime: string
}

/**
 * GET /admin/announcements 分页查询公告
 */
export function pageAnnouncements(params: AnnouncementPageParams) {
  return get<PageResult<AdminAnnouncement>>('/admin/announcements', params)
}

/**
 * POST /admin/announcements 创建公告草稿
 */
export function createAnnouncement(data: AnnouncementSavePayload) {
  return post<number>('/admin/announcements', data)
}

/**
 * GET /admin/announcements/{id} 公告详情
 */
export function getAnnouncementDetail(id: number) {
  return get<AdminAnnouncement>(`/admin/announcements/${id}`)
}

/**
 * PUT /admin/announcements/{id} 修改公告（草稿/已下架）
 */
export function updateAnnouncement(id: number, data: AnnouncementSavePayload) {
  return put<null>(`/admin/announcements/${id}`, data)
}

/**
 * POST /admin/announcements/{id}/publish 发布并设置有效期
 */
export function publishAnnouncement(id: number, data: AnnouncementPublishPayload) {
  return post<null>(`/admin/announcements/${id}/publish`, data)
}

/**
 * POST /admin/announcements/{id}/offline 下架
 */
export function offlineAnnouncement(id: number) {
  return post<null>(`/admin/announcements/${id}/offline`, {})
}

/**
 * DELETE /admin/announcements/{id} 删除（草稿/已下架）
 */
export function deleteAnnouncement(id: number) {
  return del<null>(`/admin/announcements/${id}`)
}
