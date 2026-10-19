<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import {
  pageContents,
  getContentDetail,
  auditContent,
  deleteContent
} from '@/api/admin/content'
import type { AdminContent, AdminContentDetail, ContentAttachment } from '@/api/admin/content'
import { getEnabledParts } from '@/api/dict'
import type { RehabPart } from '@/api/dict'
import { formatDateTime } from '@/utils/datetime'
import { toMinioProxyUrl } from '@/api/admin/file'
import ContentFormDialog from './ContentFormDialog.vue'

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<AdminContent[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({
  status: '',
  category: '',
  keyword: ''
})

const partOptions = ref<RehabPart[]>([])

const fetchParts = async (): Promise<void> => {
  try {
    const res = await getEnabledParts()
    partOptions.value = res.data ?? []
  } catch {
    // 拦截器统一提示
  }
}

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageContents({
      page: pagination.page,
      size: pagination.size,
      status: query.status || undefined,
      category: query.category || undefined,
      keyword: query.keyword || undefined
    })
    tableData.value = res.data.records ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // 拦截器统一提示
  } finally {
    loading.value = false
  }
}

const handleSearch = (): void => {
  pagination.page = 1
  fetchData()
}

const handleReset = (): void => {
  query.status = ''
  query.category = ''
  query.keyword = ''
  pagination.page = 1
  fetchData()
}

const handlePageChange = (page: number): void => {
  pagination.page = page
  fetchData()
}

const handleSizeChange = (size: number): void => {
  pagination.size = size
  pagination.page = 1
  fetchData()
}

// ===== 新增 / 编辑弹窗 =====
const dialogRef = ref<InstanceType<typeof ContentFormDialog>>()

const handleAdd = (): void => {
  dialogRef.value?.open()
}

const handleEdit = async (row: AdminContent): Promise<void> => {
  try {
    const res = await getContentDetail(row.contentId)
    dialogRef.value?.open(res.data)
  } catch {
    // 拦截器统一提示
  }
}

const handleSaved = (): void => {
  fetchData()
  if (detailVisible.value && detail.value) {
    openDetail({ contentId: detail.value.contentId } as AdminContent)
  }
}

// ===== 详情抽屉 =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminContentDetail | null>(null)

const openDetail = async (row: AdminContent): Promise<void> => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getContentDetail(row.contentId)
    detail.value = res.data
  } catch {
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const proxyUrl = (a: ContentAttachment): string => toMinioProxyUrl(a.url ?? '')
const imageAttachments = (): ContentAttachment[] =>
  (detail.value?.attachments ?? []).filter(
    (a) => a.attachmentType === 'COVER' || a.attachmentType === 'IMAGE'
  )
const videoAttachments = (): ContentAttachment[] =>
  (detail.value?.attachments ?? []).filter((a) => a.attachmentType === 'VIDEO')
const docAttachments = (): ContentAttachment[] =>
  (detail.value?.attachments ?? []).filter((a) => a.attachmentType === 'DOC')

const formatSize = (size?: number): string => {
  if (!size) return ''
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${size} B`
}

const previewImages = (startUrl: string): string[] => {
  const urls = imageAttachments().map(proxyUrl)
  const idx = urls.indexOf(startUrl)
  return idx >= 0 ? [...urls.slice(idx), ...urls.slice(0, idx)] : urls
}

const openDoc = (a: ContentAttachment): void => {
  window.open(proxyUrl(a), '_blank')
}

// ===== 审核操作 =====
const handleApprove = (row: AdminContent): void => {
  ElMessageBox.confirm(`确认通过内容「${row.title}」？通过后将对患者端可见。`, '内容审核通过', {
    confirmButtonText: '确认通过',
    cancelButtonText: '取消',
    type: 'success'
  })
    .then(async () => {
      await auditContent(row.contentId, 'APPROVE')
      ElMessage.success('已通过发布')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

const handleReject = (row: AdminContent): void => {
  ElMessageBox.prompt('请输入驳回理由（提交后内容回退草稿态，作者可修改后重新提交）', '驳回内容', {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    type: 'warning',
    inputType: 'textarea',
    inputPlaceholder: '驳回理由必填',
    inputValidator: (v: string) => (v && v.trim() ? true : '驳回理由不能为空')
  })
    .then(async ({ value }) => {
      await auditContent(row.contentId, 'REJECT', value.trim())
      ElMessage.success('已驳回')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

const handleOffline = (row: AdminContent): void => {
  ElMessageBox.prompt('可填写下架原因（选填），下架后患者端将不可见', '下架内容', {
    confirmButtonText: '确认下架',
    cancelButtonText: '取消',
    type: 'warning',
    inputType: 'textarea',
    inputPlaceholder: '下架原因（可选）'
  })
    .then(async ({ value }) => {
      await auditContent(row.contentId, 'OFFLINE', value && value.trim() ? value.trim() : undefined)
      ElMessage.success('已下架')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

const handleDelete = (row: AdminContent): void => {
  ElMessageBox.confirm(`确认删除「${row.title}」？删除后不可恢复。`, '删除内容', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteContent(row.contentId)
      ElMessage.success('已删除')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

const statusTagType = (status: string): 'success' | 'warning' | 'info' | 'primary' => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'PENDING_REVIEW') return 'primary'
  if (status === 'DRAFT') return 'warning'
  return 'info'
}

const typeTagType = (type: string): 'primary' | 'success' | 'danger' => {
  if (type === 'VIDEO') return 'danger'
  if (type === 'GUIDE') return 'success'
  return 'primary'
}

onMounted(() => {
  fetchParts()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.category" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="图文文章" value="ARTICLE" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="指南" value="GUIDE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="标题/摘要关键词"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <div class="page-toolbar">
        <span class="table-title">公共内容管理</span>
        <el-button type="primary" @click="handleAdd">新增内容</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="contentId" label="ID" width="70" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="title-text" :title="row.title">{{ row.title }}</span>
              <el-tooltip
                v-if="row.hasReviewRemark && row.status === 'DRAFT'"
                :content="`驳回理由：${row.reviewRemark}`"
                placement="top"
              >
                <el-tag type="danger" size="small" effect="plain">已驳回</el-tag>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.contentType)" size="small">{{ row.contentTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetPartName" label="部位" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="260">
          <template #default="{ row }">
            <span class="period-text">
              {{ formatDateTime(row.validFrom) || '—' }} 至
              {{ formatDateTime(row.validTo) || '长期有效' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as AdminContent)">详情</el-button>
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 'DRAFT'"
              @click="handleEdit(row as AdminContent)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'PENDING_REVIEW'"
              link
              type="success"
              size="small"
              @click="handleApprove(row as AdminContent)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'PENDING_REVIEW'"
              link
              type="danger"
              size="small"
              @click="handleReject(row as AdminContent)"
            >
              驳回
            </el-button>
            <el-button
              v-if="row.status === 'PUBLISHED'"
              link
              type="warning"
              size="small"
              @click="handleOffline(row as AdminContent)"
            >
              下架
            </el-button>
            <el-tooltip
              v-if="row.status === 'PUBLISHED'"
              content="已发布内容请先下架再删除"
              placement="top"
            >
              <span>
                <el-button link type="danger" size="small" disabled>
                  删除
                </el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              link
              type="danger"
              size="small"
              @click="handleDelete(row as AdminContent)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <ContentFormDialog ref="dialogRef" :part-options="partOptions" @saved="handleSaved" />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="内容详情" size="560px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="ID">{{ detail.contentId }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ detail.contentTypeName }}</el-descriptions-item>
            <el-descriptions-item label="康复部位">{{ detail.targetPartName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="目标人群">{{ detail.targetGroup || '—' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusName }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="有效期">
              {{ formatDateTime(detail.validFrom) || '—' }} 至
              {{ formatDateTime(detail.validTo) || '长期有效' }}
            </el-descriptions-item>
            <el-descriptions-item label="发布时间">
              {{ formatDateTime(detail.publishedAt) || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="摘要">{{ detail.summary || '—' }}</el-descriptions-item>
            <el-descriptions-item label="审核意见">
              <span v-if="detail.reviewRemark" class="remark-text">{{ detail.reviewRemark }}</span>
              <span v-else>—</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          </el-descriptions>

          <div class="body-section">
            <div class="section-title">正文</div>
            <div class="body-text">{{ detail.contentBody }}</div>
          </div>

          <div v-if="detail.attachments?.length" class="body-section">
            <div class="section-title">附件（{{ detail.attachments.length }}）</div>

            <div v-if="imageAttachments().length" class="attach-group">
              <div class="group-label">封面/图片</div>
              <div class="image-list">
                <el-image
                  v-for="a in imageAttachments()"
                  :key="a.fileId"
                  :src="proxyUrl(a)"
                  :preview-src-list="previewImages(proxyUrl(a))"
                  preview-teleported
                  fit="cover"
                  class="attach-image"
                >
                  <template #placeholder>
                    <div class="image-loading">加载中…</div>
                  </template>
                </el-image>
              </div>
            </div>

            <div v-if="videoAttachments().length" class="attach-group">
              <div class="group-label">视频</div>
              <video
                v-for="a in videoAttachments()"
                :key="a.fileId"
                :src="proxyUrl(a)"
                controls
                preload="metadata"
                class="attach-video"
              />
            </div>

            <div v-if="docAttachments().length" class="attach-group">
              <div class="group-label">文档</div>
              <div
                v-for="a in docAttachments()"
                :key="a.fileId"
                class="doc-row"
              >
                <el-icon><Document /></el-icon>
                <span class="doc-name" :title="a.fileName">{{ a.fileName }}</span>
                <span class="doc-size">{{ formatSize(a.fileSize) }}</span>
                <el-button link type="primary" size="small" @click="openDoc(a)">新窗口打开</el-button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.filter-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.table-card {
  .table-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
  }
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .title-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.period-text {
  font-size: 12px;
  color: #606266;
}

.remark-text {
  color: #f56c6c;
}

.body-section {
  margin-top: 18px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
  }

  .body-text {
    white-space: pre-wrap;
    line-height: 1.7;
    color: #303133;
    background: #f7f8fa;
    border-radius: 6px;
    padding: 12px;
    font-size: 13px;
  }
}

.attach-group {
  margin-bottom: 14px;

  .group-label {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
  }
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attach-image {
  width: 140px;
  height: 100px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.image-loading {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
}

.attach-video {
  width: 100%;
  max-height: 260px;
  border-radius: 6px;
  background: #000;
}

.doc-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;

  .doc-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
  }

  .doc-size {
    font-size: 12px;
    color: #909399;
  }
}
</style>
