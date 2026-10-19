<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { pageLoginLogs } from '@/api/admin/user'
import type { LoginLog } from '@/api/admin/user'
import { formatDateTime } from '@/utils/datetime'

const props = defineProps<{
  /** 抽屉可见性（v-model） */
  modelValue: boolean
  /** 目标账号ID */
  userId?: number | null
  /** 目标账号显示名（抽屉标题用） */
  userName?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const logs = ref<LoginLog[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
/** 成功/失败筛选：'' 全部 / true 成功 / false 失败 */
const successFilter = ref<'' | 'true' | 'false'>('')

const fetchData = async (): Promise<void> => {
  if (props.userId == null) {
    return
  }
  loading.value = true
  try {
    const res = await pageLoginLogs(props.userId, {
      page: pagination.page,
      size: pagination.size,
      success: successFilter.value === '' ? undefined : successFilter.value === 'true'
    })
    logs.value = res.data.records ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      pagination.page = 1
      successFilter.value = ''
      fetchData()
    }
  }
)

const handlePageChange = (page: number): void => {
  pagination.page = page
  fetchData()
}

const handleSizeChange = (size: number): void => {
  pagination.size = size
  pagination.page = 1
  fetchData()
}

const handleFilterChange = (): void => {
  pagination.page = 1
  fetchData()
}

/** 操作类型中文标签 */
const actionLabel = (action: string): string => {
  if (action === 'LOGIN') {
    return '登录'
  }
  if (action === 'LOGOUT') {
    return '退出'
  }
  return action
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="`登录日志 - ${userName ?? ''}`"
    size="640px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="log-filter">
      <el-radio-group v-model="successFilter" size="small" @change="handleFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="true">成功</el-radio-button>
        <el-radio-button value="false">失败</el-radio-button>
      </el-radio-group>
    </div>

    <el-table v-loading="loading" :data="logs" border stripe style="width: 100%">
      <el-table-column label="结果" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
            {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ row }">{{ actionLabel(row.action) }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" min-width="120" />
      <el-table-column prop="deviceId" label="设备标识" min-width="120" show-overflow-tooltip />
      <el-table-column prop="message" label="说明" min-width="140" show-overflow-tooltip />
      <el-table-column label="时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        size="small"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
.log-filter {
  margin-bottom: 12px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
