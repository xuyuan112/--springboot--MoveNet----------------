import { del, get, post } from '@/api/request'
import type { ApiResult, PageResult } from '@/api/request'

/** 患者端内容列表项（仅 PUBLISHED 且有效期内可见） */
export interface ContentItem {
  contentId: number
  title: string
  /** ARTICLE / VIDEO / GUIDE */
  contentType: string
  contentTypeName: string
  targetPartCode?: string
  targetPartName?: string
  targetGroup?: string
  summary?: string
  /** 封面预签名地址（无封面为空） */
  coverUrl?: string
  validFrom?: string
  validTo?: string
  publishedAt?: string
}

/** 内容附件（预签名短时地址） */
export interface ContentAttachment {
  id: number
  /** COVER / VIDEO / IMAGE / DOC */
  type: string
  typeName: string
  name: string
  contentType: string
  size: number
  url: string
  urlExpiresAt: string
}

/** 患者端内容详情（正文 + 附件；不含审核人等管理端字段） */
export interface ContentDetail extends ContentItem {
  contentBody: string
  attachments: ContentAttachment[]
}

/** 收藏列表项；内容下架/过期后 available=false，仅保留历史 */
export interface CollectionItem {
  collectionId: number
  contentId: number
  title: string
  contentType: string
  contentTypeName: string
  targetPartCode?: string
  targetPartName?: string
  summary?: string
  coverUrl?: string
  available: boolean
  collectedAt: string
}

/** 分页查询已发布且在有效期内的内容（部位/类型/关键词筛选） */
export function listContents(params: {
  page?: number
  size?: number
  partCode?: string
  category?: string
  keyword?: string
}) {
  return get<PageResult<ContentItem>>('/patient/contents', params)
}

/** 查询内容正文与附件预签名地址 */
export function getContentDetail(contentId: number) {
  return get<ContentDetail>(`/patient/contents/${contentId}`)
}

/** 分页查询本人收藏列表 */
export function listCollections(params: { page?: number; size?: number }) {
  return get<PageResult<CollectionItem>>('/patient/collections', params)
}

/** 收藏内容（重复请求幂等，不报错） */
export function collectContent(contentId: number) {
  return post<void>(`/patient/contents/${contentId}/collect`)
}

/** 取消收藏（未收藏时幂等成功） */
export function uncollectContent(contentId: number) {
  return del<void>(`/patient/contents/${contentId}/collect`)
}
