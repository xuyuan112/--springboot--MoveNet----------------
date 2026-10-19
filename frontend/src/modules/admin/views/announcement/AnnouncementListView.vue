<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAnnouncements,
  createAnnouncement,
  getAnnouncementDetail,
  updateAnnouncement,
  publishAnnouncement,
  offlineAnnouncement,
  deleteAnnouncement
} from '@/api/admin/announcement'
import type { AdminAnnouncement, AnnouncementScope } from '@/api/admin/announcement'
import { formatDateTime } from '@/utils/datetime'

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<AdminAnnouncement[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ status: '', scope: '', keyword: '' })

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageAnnouncements({
      page: pagination.page,
      size: pagination.size,
      status: query.status || undefined,
      scope: query.scope || undefined,
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
  query.scope = ''
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
interface AnnouncementForm {
  announcementId?: number
  title: string
  content: string
  scope: AnnouncementScope | string
  period: [string, string] | null
}

const dialogVisible = ref(false)
const dialogSubmitting = ref(false)
const dialogTitle = ref('新增公告')
const form = reactive<AnnouncementForm>({
  title: '',
  content: '',
  scope: 'ALL',
  period: null
})

const resetForm = (): void => {
  form.announcementId = undefined
  form.title = ''
  form.content = ''
  form.scope = 'ALL'
  form.period = null
}

const handleAdd = (): void => {
  resetForm()
  dialogTitle.value = '新增公告'
  dialogVisible.value = true
}

const handleEdit = async (row: AdminAnnouncement): Promise<void> => {
  try {
    const res = await getAnnouncementDetail(row.announcementId)
    const d = res.data
    resetForm()
    form.announcementId = d.announcementId
    form.title = d.title
    form.content = d.content
    form.scope = d.scope
    form.period = [d.startTime, d.endTime]
    dialogTitle.value = '编辑公告'
    dialogVisible.value = true
  } catch {
    // 拦截器统一提示
  }
}

const handleSubmit = async (): Promise<void> => {
  if (!form.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请填写公告正文')
    return
  }
  if (form.period && form.period.length === 2 && (!form.period[0] !== !form.period[1])) {
    ElMessage.warning('展示有效期需同时填写开始和结束时间，或都不填')
    return
  }
  const payload = {
    title: form.title.trim(),
    content: form.content,
    scope: form.scope,
    startTime: form.period?.[0] || undefined,
    endTime: form.period?.[1] || undefined
  }
  dialogSubmitting.value = true
  try {
    if (form.announcementId) {
      await updateAnnouncement(form.announcementId, payload)
      ElMessage.success('公告已修改')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('公告草稿已创建')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 拦截器统一提示
  } finally {
    dialogSubmitting.value = false
  }
}

// ===== 发布（设置有效期） =====
const publishVisible = ref(false)
const publishSubmitting = ref(false)
const publishTarget = ref<AdminAnnouncement | null>(null)
const publishForm = reactive({ period: null as [string, string] | null })

const openPublish = (row: AdminAnnouncement): void => {
  publishTarget.value = row
  publishForm.period = [row.startTime, row.endTime]
  publishVisible.value = true
}

const handlePublish = async (): Promise<void> => {
  if (!publishTarget.value || !publishForm.period || publishForm.period.length !== 2) {
    ElMessage.warning('请选择展示有效期')
    return
  }
  publishSubmitting.value = true
  try {
    await publishAnnouncement(publishTarget.value.announcementId, {
      startTime: publishForm.period[0],
      endTime: publishForm.period[1]
    })
    ElMessage.success('公告已发布')
    publishVisible.value = false
    fetchData()
  } catch {
    // 拦截器统一提示
  } finally {
    publishSubmitting.value = false
  }
}

// ===== 下架 / 删除 =====
const handleOffline = async (row: AdminAnnouncement): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      `确定下架公告「${row.title}」吗？下架后患者/治疗师端将不再展示，可重新发布。`,
      '下架确认',
      { confirmButtonText: '下架', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await offlineAnnouncement(row.announcementId)
    ElMessage.success('已下架')
    fetchData()
  } catch {
    // 拦截器统一提示
  }
}

const handleDelete = async (row: AdminAnnouncement): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      `确定删除公告「${row.title}」吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteAnnouncement(row.announcementId)
    ElMessage.success('已删除')
    fetchData()
  } catch {
    // 拦截器统一提示
  }
}

// ===== 详情抽屉 =====
const detailVisible = ref(false)
const detail = ref<AdminAnnouncement | null>(null)

const openDetail = async (row: AdminAnnouncement): Promise<void> => {
  try {
    const res = await getAnnouncementDetail(row.announcementId)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    // 拦截器统一提示
  }
}

const scopeTagType = (scope: string): 'primary' | 'success' | 'warning' => {
  if (scope === 'ALL') return 'primary'
  return scope === 'PATIENT' ? 'success' : 'warning'
}

const statusTagType = (status: string): 'info' | 'success' | 'danger' => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'OFFLINE') return 'info'
  return 'danger'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="query.scope" placeholder="全部范围" clearable style="width: 140px">
            <el-option label="全部用户" value="ALL" />
            <el-option label="仅患者" value="PATIENT" />
            <el-option label="仅治疗师" value="THERAPIST" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="query.keyword" placeholder="公告标题关键词" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="page-toolbar">
        <span class="table-title">平台公告管理</span>
        <el-button type="primary" @click="handleAdd">新增公告</el-button>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="announcementId" label="ID" width="70" />
        <el-table-column label="标题" min-width="220" show-overflow-tooltip prop="title" />
        <el-table-column label="范围" width="100">
          <template #default="{ row }">
            <el-tag :type="scopeTagType(row.scope)" size="small">{{ row.scopeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展示有效期" min-width="260">
          <template #default="{ row }">
            <span class="period-text">
              {{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="110" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as AdminAnnouncement)">详情</el-button>
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status === 'PUBLISHED'"
              @click="handleEdit(row as AdminAnnouncement)"
            >编辑</el-button>
            <el-button
              v-if="row.status !== 'PUBLISHED'"
              link
              type="success"
              size="small"
              @click="openPublish(row as AdminAnnouncement)"
            >发布</el-button>
            <el-button
              v-else
              link
              type="warning"
              size="small"
              @click="handleOffline(row as AdminAnnouncement)"
            >下架</el-button>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="row.status === 'PUBLISHED'"
              @click="handleDelete(row as AdminAnnouncement)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" :close-on-click-modal="false">
      <el-form label-width="92px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="form.scope">
            <el-radio value="ALL">全部用户</el-radio>
            <el-radio value="PATIENT">仅患者</el-radio>
            <el-radio value="THERAPIST">仅治疗师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="正文" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告正文"
          />
        </el-form-item>
        <el-form-item label="展示有效期">
          <el-date-picker
            v-model="form.period"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <div class="form-tip">草稿阶段可不填（默认创建起 30 天）；发布时需确认有效期</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogSubmitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发布对话框 -->
    <el-dialog v-model="publishVisible" title="发布公告" width="520px" :close-on-click-modal="false">
      <el-alert
        title="发布后患者/治疗师端将在有效期内展示该公告；有效期结束自动隐藏，也可手动下架。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-form label-width="92px">
        <el-form-item label="展示有效期" required>
          <el-date-picker
            v-model="publishForm.period"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="success" :loading="publishSubmitting" @click="handlePublish">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="公告详情" size="520px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ detail.announcementId }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="可见范围">
            <el-tag :type="scopeTagType(detail.scope)" size="small">{{ detail.scopeName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="展示有效期">
            {{ formatDateTime(detail.startTime) }} ~ {{ formatDateTime(detail.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.creatorName ?? detail.createdBy }}</el-descriptions-item>
          <el-descriptions-item label="创建/更新时间">
            {{ formatDateTime(detail.createdAt) }} / {{ formatDateTime(detail.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-content">{{ detail.content }}</div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.period-text {
  font-size: 13px;
  color: #475569;
  white-space: nowrap;
}

.form-tip {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
}

.detail-content {
  margin-top: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
