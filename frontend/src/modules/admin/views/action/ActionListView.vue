<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageActions, offlineAction } from '@/api/admin/action'
import type { AdminAction, ActionPageParams } from '@/api/admin/action'
import { getEnabledParts } from '@/api/dict'
import type { RehabPart } from '@/api/dict'
import { formatDateTime } from '@/utils/datetime'
import ActionFormDialog from './ActionFormDialog.vue'

const router = useRouter()

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<AdminAction[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({
  targetPart: '',
  status: '',
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
    const params: ActionPageParams = {
      page: pagination.page,
      size: pagination.size,
      targetPart: query.targetPart || undefined,
      status: (query.status || undefined) as ActionPageParams['status'],
      keyword: query.keyword || undefined
    }
    const res = await pageActions(params)
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
  query.targetPart = ''
  query.status = ''
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
const dialogRef = ref<InstanceType<typeof ActionFormDialog>>()

const handleAdd = (): void => {
  dialogRef.value?.open()
}

const handleEdit = (row: AdminAction): void => {
  dialogRef.value?.open(row)
}

const handleSaved = (): void => {
  fetchData()
}

// ===== 下线 =====
const handleOffline = (row: AdminAction): void => {
  ElMessageBox.confirm(
    `下线后动作「${row.name}」将禁止被新计划引用，已有计划不受影响。确认下线？`,
    '动作下线',
    { confirmButtonText: '确认下线', cancelButtonText: '取消', type: 'warning' }
  )
    .then(async () => {
      await offlineAction(row.actionId)
      ElMessage.success('动作已下线')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

// ===== 版本管理入口 =====
const handleVersions = (row: AdminAction): void => {
  router.push({ name: 'AdminActionVersions', params: { actionId: row.actionId } })
}

const statusTagType = (status: string): 'success' | 'warning' | 'info' => {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'info'
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
        <el-form-item label="康复部位">
          <el-select v-model="query.targetPart" placeholder="全部部位" clearable style="width: 160px">
            <el-option v-for="item in partOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下线" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="动作名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
        <span class="table-title">动作库管理</span>
        <el-button type="primary" @click="handleAdd">新增动作</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="actionId" label="ID" width="80" />
        <el-table-column prop="name" label="动作名称" min-width="150" />
        <el-table-column prop="targetPartName" label="康复部位" min-width="110" />
        <el-table-column label="难度" min-width="90">
          <template #default="{ row }">
            <el-tag
              :type="row.difficulty === 'EASY' ? 'success' : row.difficulty === 'MEDIUM' ? 'warning' : 'danger'"
              size="small"
            >
              {{ row.difficultyName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleVersions(row as AdminAction)">
              版本管理
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 'DRAFT'"
              @click="handleEdit(row as AdminAction)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="row.status !== 'PUBLISHED'"
              @click="handleOffline(row as AdminAction)"
            >
              下线
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

    <ActionFormDialog ref="dialogRef" :part-options="partOptions" @saved="handleSaved" />
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
</style>
