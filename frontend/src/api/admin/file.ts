import { del, get, post } from '../request'

/** 文件用途：TEMPLATE 模板样本 / ATTACHMENT 业务附件 / MESSAGE 消息 / AVATAR 头像 */
export type FilePurpose = 'TEMPLATE' | 'ATTACHMENT' | 'MESSAGE' | 'AVATAR'

/** 预签名申请请求 */
export interface FilePresignPayload {
  fileName: string
  contentType: string
  fileSize: number
  purpose: FilePurpose
}

/** 预签名申请响应 */
export interface FilePresignResult {
  /** 直传会话ID（完成确认时回传） */
  uploadId: string
  /** MinIO 预签名 PUT 地址（直传前需经 toMinioProxyUrl 改写为同源代理） */
  uploadUrl: string
  /** HTTP 方法，固定 PUT */
  method: string
  /** 直传时必须携带的请求头 */
  headers: Record<string, string>
  /** 预签名有效期（秒） */
  expiresInSec: number
}

/** 文件预签名预览地址响应 */
export interface FileUrlInfo {
  fileId: number
  fileName: string
  contentType: string
  fileSize: number
  /** 短时预签名下载地址（展示前需经 toMinioProxyUrl 改写） */
  url: string
  urlExpiresAt: string
}

/**
 * 管理端 - 申请预签名直传地址
 * POST /admin/files/presign
 */
export function presignFile(data: FilePresignPayload) {
  return post<FilePresignResult>('/admin/files/presign', data)
}

/**
 * 管理端 - 直传完成确认（后端校验大小、类型、SHA-256）
 * POST /admin/files/{uploadId}/complete
 * @returns file_object 主键 fileId
 */
export function completeFileUpload(uploadId: string, sha256: string) {
  return post<number>(`/admin/files/${uploadId}/complete`, { sha256 })
}

/**
 * 管理端 - 获取文件预签名预览地址
 * GET /admin/files/{fileId}/url
 */
export function getFileUrl(fileId: number) {
  return get<FileUrlInfo>(`/admin/files/${fileId}/url`)
}

/**
 * 管理端 - 删除未被内容/模板/消息引用的文件
 * DELETE /admin/files/{fileId}
 */
export function deleteFile(fileId: number) {
  return del<null>(`/admin/files/${fileId}`)
}

/**
 * 将后端下发的 MinIO 绝对预签名 URL 改写为同源代理路径，规避浏览器直连 MinIO 的 CORS 限制。
 *
 * 开发环境由 vite 的 /minio 代理转发（去掉前缀）；
 * 生产环境需在 Nginx 配置同名反代：location /minio/ { proxy_pass http://minio-host:9000/; }
 *
 * @example toMinioProxyUrl('http://192.168.10.102:9000/rehab/content/x.pdf?X-Amz-Signature=..')
 *          // => '/minio/rehab/content/x.pdf?X-Amz-Signature=..'
 */
export function toMinioProxyUrl(rawUrl: string): string {
  if (!rawUrl) return rawUrl
  try {
    const u = new URL(rawUrl, window.location.origin)
    return `/minio${u.pathname}${u.search}`
  } catch {
    return rawUrl
  }
}
