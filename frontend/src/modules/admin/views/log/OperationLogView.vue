<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { pageOperationLogs } from '@/api/admin/log'
import type { OperationLog } from '@/api/admin/log'
import { formatDateTime } from '@/utils/datetime'

/** 模块筛选项（与后端 @OperationLog module 取值一致） */
const MODULE_OPTIONS = [
  { label: '登录认证', value: 'AUTH' },
  { label: '账号', value: 'USER' },
  { label: '绑定关系', value: 'BINDING' },
  { label: '动作库', value: 'ACTION' },
  { label: '内容', value: 'CONTENT' },
  { label: '公告', value: 'NOTICE' },
  { label: '字典', value: 'DICT' },
  { label: '备份恢复', value: 'BACKUP' }
]

const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const query = reactive({
  module: '',
  operatorId: '' as string | number,
  period: null as [string, string] | null
})

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageOperationLogs({
      page: pagination.page,
      size: pagination.size,
      module: query.module || undefined,
      operatorId: query.operatorId ? Number(query.operatorId) : undefined,
      from: query.period?.[0] || undefined,
      to: query.period?.[1] || undefined
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
  query.module = ''
  query.operatorId = ''
  query.period = null
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

// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref<OperationLog | null>(null)
const detailJson = ref('')

const openDetail = (row: OperationLog): void => {
  detail.value = row
  if (row.detail) {
    try {
      detailJson.value = JSON.stringify(JSON.parse(row.detail), null, 2)
    } catch {
      detailJson.value = row.detail
    }
  } else {
    detailJson.value = '（无详情）'
  }
  detailVisible.value = true
}

const copyDetail = async (): Promise<void> => {
  try {
    await navigator.clipboard.writeText(detailJson.value)
  } catch {
    // 剪贴板不可用时忽略
  }
}

const moduleName = (code: string): string =>
  MODULE_OPTIONS.find((m) => m.value === code)?.label ?? code

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 150px">
            <el-option v-for="m in MODULE_OPTIONS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人ID">
          <el-input
            v-model="query.operatorId"
            placeholder="用户ID"
            clearable
            type="number"
            style="width: 130px"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="query.period"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="page-toolbar">
        <span class="table-title">操作审计日志</span>
      </div>
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="logId" label="日志ID" width="90" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">{{ row.userName ?? '系统' }}</template>
        </el-table-column>
        <el-table-column label="模块" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ moduleName(row.module) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="130" />
        <el-table-column label="对象" min-width="160">
          <template #default="{ row }">
            <span v-if="row.targetType">{{ row.targetType }}<template v-if="row.targetId"> #{{ row.targetId }}</template></span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row as OperationLog)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="日志ID">{{ detail.logId }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ detail.userName ?? '系统' }}<template v-if="detail.userId">（ID: {{ detail.userId }}）</template>
          </el-descriptions-item>
          <el-descriptions-item label="模块 / 操作">
            {{ moduleName(detail.module) }} / {{ detail.action }}
          </el-descriptions-item>
          <el-descriptions-item label="目标对象">
            <span v-if="detail.targetType">{{ detail.targetType }}<template v-if="detail.targetId"> #{{ detail.targetId }}</template></span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="IP / 设备">
            {{ detail.ip }}<template v-if="detail.deviceId"> / {{ detail.deviceId }}</template>
          </el-descriptions-item>
          <el-descriptions-item label="结果">
            <el-tag :type="detail.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ detail.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-json-head">
          <span>请求参数 / 详情（JSON）</span>
          <el-button link type="primary" size="small" @click="copyDetail">复制</el-button>
        </div>
        <pre class="detail-json">{{ detailJson }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.detail-json-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.detail-json {
  margin: 0;
  padding: 12px;
  max-height: 50vh;
  overflow: auto;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
