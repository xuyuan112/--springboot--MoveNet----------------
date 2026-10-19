import { del, get, post, put } from '../request'
import type { PageResult } from '../request'

/** 内容类型：ARTICLE 图文文章 / VIDEO 视频 / GUIDE 指南 */
export type ContentType = 'ARTICLE' | 'VIDEO' | 'GUIDE'

/** 内容状态（以后端 ContentStatus 枚举为准，无 REJECTED 态，驳回回退 DRAFT） */
export type ContentStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'OFFLINE'

/** 附件类型：COVER 封面 / VIDEO 视频 / IMAGE 图片 / DOC 文档 */
export type AttachmentType = 'COVER' | 'VIDEO' | 'IMAGE' | 'DOC'

/** 审核动作 */
export type AuditDecision = 'APPROVE' | 'REJECT' | 'OFFLINE'

/** 内容分页查询参数 */
export interface ContentPageParams {
  page?: number
  size?: number
  /** 状态码 */
  status?: string
  /** 内容类型（后端查询参数名为 category） */
  category?: string
  keyword?: string
}

/** 内容列表项 */
export interface AdminContent {
  contentId: number
  title: string
  contentType: string
  contentTypeName: string
  targetPartCode?: string
  targetPartName?: string
  targetGroup?: string
  summary?: string
  status: string
  statusName: string
  /** 是否存在最近一次审核意见（DRAFT 态用于展示「已驳回」标记） */
  hasReviewRemark?: boolean
  reviewRemark?: string
  validFrom?: string
  validTo?: string
  publishedAt?: string
  authorId?: number
  createdAt?: string
  updatedAt?: string
}

/** 内容附件（详情内，含预签名地址，展示前需走 toMinioProxyUrl） */
export interface ContentAttachment {
  contentAttachmentId?: number
  fileId: number
  attachmentType: AttachmentType | string
  attachmentTypeName?: string
  sortNo?: number
  fileName?: string
  contentType?: string
  fileSize?: number
  url?: string
  urlExpiresAt?: string
}

/** 内容详情 */
export interface AdminContentDetail {
  contentId: number
  title: string
  contentType: string
  contentTypeName: string
  targetPartCode?: string
  targetPartName?: string
  targetGroup?: string
  summary?: string
  contentBody: string
  status: string
  statusName: string
  authorId?: number
  reviewerId?: number
  reviewRemark?: string
  validFrom?: string
  validTo?: string
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
  attachments: ContentAttachment[]
}

/** 附件保存载荷（fileId 需已完成上传确认） */
export interface ContentAttachmentPayload {
  fileId: number
  attachmentType: AttachmentType
  sortNo?: number
}

/** 内容创建/修改请求 */
export interface ContentSavePayload {
  title: string
  contentType: ContentType
  /** 关联康复部位编码（必填，DTO 可空但服务端强校验） */
  targetPartCode: string
  targetGroup?: string
  summary?: string
  contentBody: string
  /** 格式 yyyy-MM-dd HH:mm:ss；为空表示发布时生效 */
  validFrom?: string
  /** 格式 yyyy-MM-dd HH:mm:ss；为空表示长期有效 */
  validTo?: string
  attachments?: ContentAttachmentPayload[]
}

/**
 * 管理端 - 分页查询内容
 * GET /admin/contents
 */
export function pageContents(params: ContentPageParams) {
  return get<PageResult<AdminContent>>('/admin/contents', params)
}

/**
 * 管理端 - 创建内容草稿
 * POST /admin/contents
 */
export function createContent(data: ContentSavePayload) {
  return post<number>('/admin/contents', data)
}

/**
 * 管理端 - 查询内容详情（含附件预签名地址）
 * GET /admin/contents/{contentId}
 */
export function getContentDetail(contentId: number) {
  return get<AdminContentDetail>(`/admin/contents/${contentId}`)
}

/**
 * 管理端 - 编辑草稿（附件全量替换，历史审核标记清空）
 * PUT /admin/contents/{contentId}
 */
export function updateContent(contentId: number, data: ContentSavePayload) {
  return put<null>(`/admin/contents/${contentId}`, data)
}

/**
 * 管理端 - 内容审核：APPROVE 通过 / REJECT 驳回（comment 必填）/ OFFLINE 下架
 * POST /admin/contents/{contentId}/audit
 */
export function auditContent(contentId: number, decision: AuditDecision, comment?: string) {
  return post<null>(`/admin/contents/${contentId}/audit`, { decision, comment })
}

/**
 * 管理端 - 删除未发布草稿
 * DELETE /admin/contents/{contentId}
 */
export function deleteContent(contentId: number) {
  return del<null>(`/admin/contents/${contentId}`)
}
