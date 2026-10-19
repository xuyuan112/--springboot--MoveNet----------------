<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { presignFile, completeFileUpload, toMinioProxyUrl } from '@/api/admin/file'
import type { AttachmentType } from '@/api/admin/content'

/** 附件项（上传中/已完成统一结构；编辑回填时带 url 等文件信息） */
export interface AttachmentItem {
  fileId?: number
  attachmentType: AttachmentType
  sortNo?: number
  fileName: string
  contentType: string
  fileSize: number
  /** 已存在附件的预签名预览地址（编辑回填） */
  url?: string
  /** uploading / done / error */
  status: 'uploading' | 'done' | 'error'
  progress: number
  /** 上传会话上下文（失败重试需要） */
  uploadId?: string
  errorMsg?: string
}

const props = defineProps<{
  modelValue: AttachmentItem[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: AttachmentItem[]): void
}>()

// 与后端 FileUploadPolicy.ATTACHMENT 白名单保持一致（100MB）
const ALLOWED_MIME: Record<string, AttachmentType> = {
  'image/jpeg': 'IMAGE',
  'image/png': 'IMAGE',
  'image/gif': 'IMAGE',
  'image/webp': 'IMAGE',
  'video/mp4': 'VIDEO',
  'application/pdf': 'DOC'
}
const MAX_SIZE = 100 * 1024 * 1024

const TYPE_OPTIONS: { value: AttachmentType; label: string }[] = [
  { value: 'COVER', label: '封面' },
  { value: 'VIDEO', label: '视频' },
  { value: 'IMAGE', label: '图片' },
  { value: 'DOC', label: '文档' }
]

const uploading = ref(false)
const items = computed<AttachmentItem[]>({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const formatSize = (size: number): string => {
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${size} B`
}

/** 计算浏览器端 SHA-256（小写十六进制），与后端 complete 校验一致 */
async function sha256Hex(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  const digest = await crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

/** el-upload 选择文件前校验类型/大小；返回 false 阻止自动上传 */
const beforeUpload = (file: File): boolean => {
  if (!ALLOWED_MIME[file.type]) {
    ElMessage.error(`「${file.name}」类型不支持，仅允许 JPG/PNG/GIF/WEBP/MP4/PDF`)
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error(`「${file.name}」超过 100MB 大小限制`)
    return false
  }
  if (file.size <= 0) {
    ElMessage.error(`「${file.name}」文件大小需大于 0`)
    return false
  }
  return true
}

/** XHR 直传 MinIO（走 /minio 同源代理），带进度回调 */
function putObject(url: string, file: File, headers: Record<string, string>,
                   onProgress: (percent: number) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', url, true)
    Object.entries(headers || {}).forEach(([k, v]) => xhr.setRequestHeader(k, v))
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(Math.round((e.loaded / e.total) * 100))
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`对象存储返回 HTTP ${xhr.status}`))
      }
    }
    xhr.onerror = () => reject(new Error('直传网络错误'))
    xhr.send(file)
  })
}

/** 自定义上传：预签名 → 直传 → 完成确认入库 */
const httpRequest = async (options: UploadRequestOptions): Promise<void> => {
  const file = options.file as File
  const item: AttachmentItem = {
    fileName: file.name,
    contentType: file.type,
    fileSize: file.size,
    attachmentType: ALLOWED_MIME[file.type] ?? 'DOC',
    status: 'uploading',
    progress: 0
  }
  items.value = [...items.value, item]
  uploading.value = true
  try {
    const sha256 = await sha256Hex(file)
    const presignRes = await presignFile({
      fileName: file.name,
      contentType: file.type,
      fileSize: file.size,
      purpose: 'ATTACHMENT'
    })
    const presign = presignRes.data
    item.uploadId = presign.uploadId
    await putObject(toMinioProxyUrl(presign.uploadUrl), file, presign.headers || {}, (p) => {
      item.progress = p
    })
    const completeRes = await completeFileUpload(presign.uploadId, sha256)
    item.fileId = completeRes.data
    item.status = 'done'
    item.progress = 100
  } catch (err) {
    item.status = 'error'
    item.errorMsg = err instanceof Error ? err.message : '上传失败'
  } finally {
    items.value = [...items.value]
    uploading.value = items.value.some((i) => i.status === 'uploading')
  }
}

/** 失败重试：仅支持直传阶段失败（预签名会话一次性消费，重试用新会话重新走完整流程） */
const retry = async (item: AttachmentItem): Promise<void> => {
  ElMessage.info('请重新选择该文件上传（预签名会话已失效）')
  removeItem(item)
}

const removeItem = (item: AttachmentItem): void => {
  items.value = items.value.filter((i) => i !== item)
}

const changeType = (item: AttachmentItem, type: AttachmentType): void => {
  item.attachmentType = type
  items.value = [...items.value]
}
</script>

<template>
  <div class="attachment-uploader">
    <el-upload
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="httpRequest"
      multiple
      accept="image/jpeg,image/png,image/gif,image/webp,video/mp4,application/pdf"
    >
      <el-button type="primary" plain :loading="uploading">选择文件上传</el-button>
      <template #tip>
        <div class="upload-tip">
          支持 JPG / PNG / GIF / WEBP / MP4 / PDF，单个文件不超过 100MB；上传完成后请选择附件用途（封面/视频/图片/文档）
        </div>
      </template>
    </el-upload>

    <el-table v-if="items.length" :data="items" border size="small" class="attach-table">
      <el-table-column label="文件" min-width="220">
        <template #default="{ row }">
          <div class="file-cell">
            <el-icon class="file-icon"><Document /></el-icon>
            <span class="file-name" :title="row.fileName">{{ row.fileName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="用途" width="120">
        <template #default="{ row }">
          <el-select
            :model-value="row.attachmentType"
            size="small"
            :disabled="row.status !== 'done'"
            @update:model-value="(v: AttachmentType) => changeType(row as AttachmentItem, v)"
          >
            <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="状态" min-width="170">
        <template #default="{ row }">
          <el-progress v-if="row.status === 'uploading'" :percentage="row.progress" :stroke-width="8" />
          <el-tag v-else-if="row.status === 'done'" type="success" size="small">已完成</el-tag>
          <div v-else class="error-cell">
            <el-tag type="danger" size="small">失败</el-tag>
            <span class="error-msg" :title="row.errorMsg">{{ row.errorMsg }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'error'"
            link
            type="primary"
            size="small"
            @click="retry(row as AttachmentItem)"
          >
            重新上传
          </el-button>
          <el-button link type="danger" size="small" @click="removeItem(row as AttachmentItem)">
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.upload-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.attach-table {
  margin-top: 10px;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 6px;

  .file-icon {
    color: #409eff;
    flex-shrink: 0;
  }

  .file-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.error-cell {
  display: flex;
  align-items: center;
  gap: 6px;

  .error-msg {
    font-size: 12px;
    color: #f56c6c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 110px;
  }
}
</style>
