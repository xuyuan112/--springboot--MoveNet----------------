/**
 * MinIO 预签名 URL 同源代理改写。
 *
 * 后端返回的预签名地址是 MinIO 绝对地址（http://192.168.10.102:9000/bucket/object?sig...），
 * 浏览器直连会有跨域限制；开发环境由 vite 的 /minio 代理转发（去掉前缀），
 * 生产环境需在 Nginx 配置同名反代：location /minio/ { proxy_pass http://minio-host:9000/; }。
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
