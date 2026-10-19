<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  disableUser,
  enableUser,
  pageUsers,
  resetUserPassword
} from '@/api/admin/user'
import type { AdminUser } from '@/api/admin/user'
import { formatDateTime } from '@/utils/datetime'
import UserFormDialog from './UserFormDialog.vue'
import LoginLogDrawer from './LoginLogDrawer.vue'

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<AdminUser[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({
  role: '',
  status: '' as '' | '1' | '0',
  keyword: ''
})

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageUsers({
      page: pagination.page,
      size: pagination.size,
      role: query.role || undefined,
      status: query.status === '' ? undefined : Number(query.status),
      keyword: query.keyword || undefined
    })
    tableData.value = res.data.records ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

const handleSearch = (): void => {
  pagination.page = 1
  fetchData()
}

const handleReset = (): void => {
  query.role = ''
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
const formVisible = ref(false)
const editingUserId = ref<number | null>(null)

const handleAdd = (): void => {
  editingUserId.value = null
  formVisible.value = true
}

const handleEdit = (row: AdminUser): void => {
  editingUserId.value = row.userId
  formVisible.value = true
}

// ===== 登录日志抽屉 =====
const logVisible = ref(false)
const logUserId = ref<number | null>(null)
const logUserName = ref('')

const handleViewLogs = (row: AdminUser): void => {
  logUserId.value = row.userId
  logUserName.value = `${row.realName}（${row.username}）`
  logVisible.value = true
}

// ===== 启用 / 禁用 / 重置密码 =====
/** 启用账号 */
const handleEnable = async (row: AdminUser): Promise<void> => {
  try {
    await enableUser(row.userId)
    ElMessage.success('账号已启用')
    fetchData()
  } catch {
    // 拦截器已提示
  }
}

/** 禁用账号：必须填写原因（二次确认） */
const handleDisable = (row: AdminUser): void => {
  ElMessageBox.prompt(`禁用后账号「${row.username}」的登录令牌将立即失效，请填写禁用原因`, '禁用账号', {
    confirmButtonText: '确认禁用',
    cancelButtonText: '取消',
    type: 'warning',
    inputType: 'textarea',
    inputPlaceholder: '请输入禁用原因（必填，最长 200 字）',
    inputValidator: (value: string) => {
      const v = value?.trim() ?? ''
      if (!v) {
        return '禁用原因不能为空'
      }
      return v.length <= 200 || '禁用原因最长 200 个字符'
    }
  })
    .then(async ({ value }) => {
      await disableUser(row.userId, value.trim())
      ElMessage.success('账号已禁用，令牌已失效')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

/** 重置密码：二次确认（不返回明文） */
const handleResetPassword = (row: AdminUser): void => {
  ElMessageBox.confirm(
    `确认将账号「${row.username}」的密码重置为平台初始密码吗？重置后其现有登录状态立即失效。`,
    '重置密码确认',
    {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      await resetUserPassword(row.userId)
      ElMessage.success('密码已重置，请通过线下渠道将初始密码告知用户')
    })
    .catch(() => {
      // 用户取消
    })
}

/** 角色标签颜色 */
const roleTagType = (role: string): 'primary' | 'success' | 'warning' => {
  if (role === 'ADMIN') {
    return 'warning'
  }
  return role === 'THERAPIST' ? 'success' : 'primary'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 140px">
            <el-option label="患者" value="PATIENT" />
            <el-option label="治疗师" value="THERAPIST" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="登录名 / 姓名 / 手机号"
            clearable
            style="width: 220px"
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
        <span class="table-title">账号管理</span>
        <el-button type="primary" @click="handleAdd">新增账号</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="登录名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column label="角色" min-width="90">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.roleCode)" size="small">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) || '—' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <!-- ADMIN 为系统内置账号：仅允许查看登录日志，不可编辑/启停/重置密码 -->
            <el-button
              v-if="row.roleCode !== 'ADMIN'"
              link
              type="primary"
              size="small"
              @click="handleEdit(row as AdminUser)"
            >
              编辑
            </el-button>
            <el-button link type="primary" size="small" @click="handleViewLogs(row as AdminUser)">
              登录日志
            </el-button>
            <el-button
              v-if="row.roleCode !== 'ADMIN' && row.status !== 1"
              link
              type="success"
              size="small"
              @click="handleEnable(row as AdminUser)"
            >
              启用
            </el-button>
            <el-button
              v-else-if="row.roleCode !== 'ADMIN'"
              link
              type="warning"
              size="small"
              @click="handleDisable(row as AdminUser)"
            >
              禁用
            </el-button>
            <el-button
              v-if="row.roleCode !== 'ADMIN'"
              link
              type="danger"
              size="small"
              @click="handleResetPassword(row as AdminUser)"
            >
              重置密码
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

    <!-- 新增 / 编辑弹窗 -->
    <UserFormDialog v-model="formVisible" :user-id="editingUserId" @success="fetchData" />

    <!-- 登录日志抽屉 -->
    <LoginLogDrawer
      v-model="logVisible"
      :user-id="logUserId"
      :user-name="logUserName"
    />
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
