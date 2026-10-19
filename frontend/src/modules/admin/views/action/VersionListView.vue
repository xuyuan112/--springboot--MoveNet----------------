<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageActionVersions, publishActionVersion, getActionVersionDetail } from '@/api/admin/action'
import type { AdminActionVersion, ActionVersionPageParams, AdminActionDetail } from '@/api/admin/action'
import { getActionDetail } from '@/api/admin/action'
import { formatDateTime } from '@/utils/datetime'
import VersionFormDialog from './VersionFormDialog.vue'

const route = useRoute()
const router = useRouter()
const actionId = Number(route.params.actionId)

// ===== 动作信息 =====
const actionDetail = ref<AdminActionDetail>()
const actionName = computed(() => actionDetail.value?.name ?? `动作 #${actionId}`)

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<AdminActionVersion[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({
  status: ''
})

const fetchActionDetail = async (): Promise<void> => {
  try {
    const res = await getActionDetail(actionId)
    actionDetail.value = res.data
  } catch {
    // 拦截器统一提示
  }
}

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const params: ActionVersionPageParams = {
      page: pagination.page,
      size: pagination.size,
      actionId,
      status: (query.status || undefined) as ActionVersionPageParams['status']
    }
    const res = await pageActionVersions(params)
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
const dialogRef = ref<InstanceType<typeof VersionFormDialog>>()

const handleAdd = (): void => {
  dialogRef.value?.open()
}

const handleEdit = (row: AdminActionVersion): void => {
  dialogRef.value?.open(row)
}

const handleSaved = (): void => {
  fetchData()
}

// ===== 发布 =====
const handlePublish = (row: AdminActionVersion): void => {
  ElMessageBox.confirm(
    `发布版本 V${row.versionNo} 前将校验规则权重之和是否为 1。发布后版本将不可修改，确认发布？`,
    '发布版本',
    { confirmButtonText: '确认发布', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      // 发布前前端预检：拉取版本详情计算规则权重和，不满足 =1 直接拦截（后端仍有权威校验）
      const detailRes = await getActionVersionDetail(row.versionId)
      const sum = (detailRes.data.ruleList ?? []).reduce((acc, r) => acc + Number(r.scoreWeight ?? 0), 0)
      const rounded = Math.round(sum * 1000) / 1000
      if (Math.abs(rounded - 1) > 0.001) {
        ElMessage.error(`规则权重和为 ${rounded}（差额 ${Math.round((1 - rounded) * 1000) / 1000}），请先在版本详情中调整规则`)
        return
      }
      await publishActionVersion(row.versionId)
      ElMessage.success('版本发布成功')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

// ===== 详情入口 =====
const handleDetail = (row: AdminActionVersion): void => {
  router.push({ name: 'AdminActionVersionDetail', params: { versionId: row.versionId } })
}

const statusTagType = (status: string): 'success' | 'warning' | 'info' => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'info'
}

onMounted(() => {
  fetchActionDetail()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部信息 + 筛选 -->
    <el-card shadow="never" class="filter-card">
      <div class="header-info">
        <el-button link type="primary" @click="router.push({ name: 'AdminActions' })">
          <el-icon><ArrowLeft /></el-icon>返回动作列表
        </el-button>
        <span class="divider">|</span>
        <span class="action-name">{{ actionName }}</span>
      </div>
      <el-form :inline="true" @submit.prevent style="margin-top: 12px">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已废弃" value="DEPRECATED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 版本列表 -->
    <el-card shadow="never" class="table-card">
      <div class="page-toolbar">
        <span class="table-title">版本列表</span>
        <el-button type="primary" @click="handleAdd">新增版本</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="versionId" label="ID" width="80" />
        <el-table-column label="版本号" width="100">
          <template #default="{ row }">V{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column prop="actionTypeName" label="动作类型" min-width="100" />
        <el-table-column prop="poseEngineId" label="姿态引擎" min-width="130" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row as AdminActionVersion)">
              详情
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 'DRAFT'"
              @click="handleEdit(row as AdminActionVersion)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="success"
              size="small"
              :disabled="row.status !== 'DRAFT'"
              @click="handlePublish(row as AdminActionVersion)"
            >
              发布
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

    <VersionFormDialog ref="dialogRef" :action-id="actionId" @saved="handleSaved" />
  </div>
</template>

<style scoped lang="scss">
.filter-card {
  margin-bottom: 16px;

  .header-info {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;

    .divider {
      color: #dcdfe6;
    }

    .action-name {
      font-weight: 600;
      color: #1f2937;
    }
  }

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
</style>
