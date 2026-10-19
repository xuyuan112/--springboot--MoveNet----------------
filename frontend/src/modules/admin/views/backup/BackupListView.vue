<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createBackup,
  pageBackups,
  getBackupDetail,
  restoreBackup
} from '@/api/admin/backup'
import type { BackupTask } from '@/api/admin/backup'
import { formatDateTime } from '@/utils/datetime'

const loading = ref(false)
const creating = ref(false)
const tableData = ref<BackupTask[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({ status: '', taskType: 'BACKUP' })

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageBackups({
      page: pagination.page,
      size: pagination.size,
      status: query.status || undefined,
      taskType: query.taskType || undefined
    })
    tableData.value = res.data.records ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // 拦截器统一提示
  } finally {
    loading.value = false
  }
}

/** RUNNING 任务存在时自动轮询 */
let timer: ReturnType<typeof setInterval> | null = null
const startPolling = (): void => {
  if (timer) return
  timer = setInterval(async () => {
    const hasRunning = tableData.value.some((t) => t.status === 'RUNNING')
    if (hasRunning) {
      await fetchData()
    }
  }, 3000)
}

const handleSearch = (): void => {
  pagination.page = 1
  fetchData()
}

const handleReset = (): void => {
  query.status = ''
  query.taskType = 'BACKUP'
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

// ===== 创建备份 =====
const handleCreate = async (): Promise<void> => {
  const hasRunning = tableData.value.some((t) => t.status === 'RUNNING')
  if (hasRunning) {
    ElMessage.warning('已有备份任务正在执行，请等待完成')
    return
  }
  creating.value = true
  try {
    const res = await createBackup()
    ElMessage.success(`备份任务 #${res.data} 已创建，正在后台执行`)
    query.status = ''
    pagination.page = 1
    await fetchData()
  } catch {
    // 423 等错误由拦截器统一提示
  } finally {
    creating.value = false
  }
}

// ===== 恢复（二次确认） =====
const restoreVisible = ref(false)
const restoreSubmitting = ref(false)
const restoreTarget = ref<BackupTask | null>(null)
const restoreForm = reactive({
  confirmToken: '',
  reason: '',
  acknowledged: false
})

const openRestore = (row: BackupTask): void => {
  restoreTarget.value = row
  restoreForm.confirmToken = ''
  restoreForm.reason = ''
  restoreForm.acknowledged = false
  restoreVisible.value = true
}

/** 当前对话框是否为"用恢复前快照回退"场景（快照回退不再递归生成快照） */
const isSnapshotRollback = computed(
  () => restoreTarget.value?.taskType === 'PRE_RESTORE_SNAPSHOT'
)

const fetchConfirmToken = async (): Promise<void> => {
  if (!restoreTarget.value) return
  try {
    const res = await getBackupDetail(restoreTarget.value.backupId)
    if (!res.data.confirmToken) {
      ElMessage.warning('当前备份不可恢复（文件缺失或状态异常）')
      return
    }
    restoreForm.confirmToken = res.data.confirmToken
    ElMessage.success('确认码已获取，5 分钟内有效，使用后立即失效')
  } catch {
    // 拦截器统一提示
  }
}

const handleRestore = async (): Promise<void> => {
  if (!restoreTarget.value) return
  if (!restoreForm.confirmToken) {
    ElMessage.warning('请先获取一次性确认码')
    return
  }
  if (!restoreForm.reason.trim()) {
    ElMessage.warning('请填写恢复原因（审计留痕）')
    return
  }
  if (!restoreForm.acknowledged) {
    ElMessage.warning('请勾选已知晓恢复风险')
    return
  }
  restoreSubmitting.value = true
  try {
    await restoreBackup(restoreTarget.value.backupId, {
      confirmToken: restoreForm.confirmToken,
      reason: restoreForm.reason.trim()
    })
    ElMessage.success(
      isSnapshotRollback.value
        ? '快照回退完成，数据库已回到恢复操作前状态，操作已记录审计日志'
        : '恢复完成，已自动生成恢复前快照并记录审计日志'
    )
    restoreVisible.value = false
    fetchData()
  } catch {
    // 400/423/500 由拦截器统一提示；令牌一次性失效，需重新获取
    restoreForm.confirmToken = ''
  } finally {
    restoreSubmitting.value = false
  }
}

// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref<BackupTask | null>(null)

const openDetail = async (row: BackupTask): Promise<void> => {
  // 列表行信息已足够展示，快照原因/路径等直接展开；确认码不在详情抽屉自动拉取
  detail.value = row
  detailVisible.value = true
}

const formatSize = (bytes?: number): string => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(2)} MB`
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)} GB`
}

/** 空时间显示 - */
const formatTime = (v?: string): string => (v ? formatDateTime(v) : '-')

const statusTagType = (status: string): 'success' | 'info' | 'danger' | 'warning' => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'warning'
  return 'danger'
}

onMounted(() => {
  fetchData()
  startPolling()
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="类型">
          <el-select v-model="query.taskType" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="手工备份" value="BACKUP" />
            <el-option label="恢复前快照" value="PRE_RESTORE_SNAPSHOT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="执行中" value="RUNNING" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="page-toolbar">
        <span class="table-title">数据库备份与恢复</span>
        <el-button type="primary" :loading="creating" @click="handleCreate">立即备份</el-button>
      </div>
      <el-alert
        title="备份为全库逻辑备份（JDBC，无需 mysqldump）；恢复会用备份数据覆盖当前库，必须二次确认，系统会在恢复前自动生成安全快照。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="backupId" label="ID" width="70" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.taskType === 'BACKUP' ? 'primary' : 'info'" effect="plain">
              {{ row.taskTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ row.status === 'RUNNING' ? '执行中…' : row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="关联备份" width="90">
          <template #default="{ row }">{{ row.relatedBackupId ? '#' + row.relatedBackupId : '-' }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.startedAt) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">{{ formatTime(row.finishedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as BackupTask)">详情</el-button>
            <el-button
              v-if="row.status === 'SUCCESS'"
              link
              type="danger"
              size="small"
              @click="openRestore(row as BackupTask)"
            >{{ row.taskType === 'PRE_RESTORE_SNAPSHOT' ? '快照回退' : '恢复' }}</el-button>
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

    <!-- 恢复二次确认对话框 -->
    <el-dialog
      v-model="restoreVisible"
      :title="isSnapshotRollback ? '快照回退（高危操作）' : '数据库恢复（高危操作）'"
      width="580px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="error"
        :closable="false"
        show-icon
        :title="isSnapshotRollback
          ? '回退将用该恢复前快照覆盖当前全部业务数据，回到恢复操作执行前的状态！'
          : '恢复将用备份数据覆盖当前全部业务数据！系统会在恢复前自动创建安全快照，可用于回退。'"
        style="margin-bottom: 16px"
      />
      <el-form label-width="92px">
        <el-form-item label="目标备份">
          <span v-if="restoreTarget">
            #{{ restoreTarget.backupId }}（{{ formatSize(restoreTarget.fileSize) }}，
            {{ formatDateTime(restoreTarget.finishedAt) }}）
          </span>
        </el-form-item>
        <el-form-item label="确认码" required>
          <el-input
            v-model="restoreForm.confirmToken"
            placeholder="点击右侧按钮获取一次性确认码"
            readonly
            class="token-input"
          >
            <template #append>
              <el-button @click="fetchConfirmToken">获取确认码</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="恢复原因" required>
          <el-input
            v-model="restoreForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请填写恢复原因（必填，写入审计日志）"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="restoreForm.acknowledged">
            我已知晓恢复会覆盖当前数据，且已确认备份正确
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="restoreVisible = false">取消</el-button>
        <el-button type="danger" :loading="restoreSubmitting" @click="handleRestore">确认恢复</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="备份任务详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务ID">{{ detail.backupId }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.taskTypeName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatSize(detail.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.operatorName ?? '系统' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.relatedBackupId" label="关联备份">
            #{{ detail.relatedBackupId }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.restoreReason" label="恢复原因">
            {{ detail.restoreReason }}
          </el-descriptions-item>
          <el-descriptions-item label="开始/结束时间">
            {{ formatDateTime(detail.startedAt) }} / {{ formatDateTime(detail.finishedAt) }}
          </el-descriptions-item>
        </el-descriptions>
        <template v-if="detail.filePath">
          <div class="detail-label">备份文件</div>
          <pre class="detail-text">{{ detail.filePath }}</pre>
        </template>
        <template v-if="detail.sha256">
          <div class="detail-label">SHA-256</div>
          <pre class="detail-text">{{ detail.sha256 }}</pre>
        </template>
        <template v-if="detail.errorMsg">
          <div class="detail-label error">失败原因</div>
          <pre class="detail-text error">{{ detail.errorMsg }}</pre>
        </template>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.token-input {
  width: 100%;
}

.detail-label {
  margin: 14px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;

  &.error {
    color: #dc2626;
  }
}

.detail-text {
  margin: 0;
  padding: 10px;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;

  &.error {
    background: #fef2f2;
    color: #b91c1c;
  }
}
</style>
