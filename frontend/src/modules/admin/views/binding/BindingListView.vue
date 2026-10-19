<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageUsers } from '@/api/admin/user'
import type { AdminUser } from '@/api/admin/user'
import {
  createBinding,
  pageBindings,
  unbindBinding,
  updateBinding
} from '@/api/admin/binding'
import type { Binding, RelationType } from '@/api/admin/binding'
import { formatDateTime, toPickerDateTime } from '@/utils/datetime'

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<Binding[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const query = reactive({
  status: ''
})

/** 远程搜索下拉的独立 loading（与表格 loading 区分） */
const patientSearching = ref(false)
const therapistSearching = ref(false)

/** 远程搜索用户（按角色），供筛选与弹窗选择器复用 */
const searchUsers = async (keyword: string, role: 'PATIENT' | 'THERAPIST'): Promise<AdminUser[]> => {
  try {
    const res = await pageUsers({ page: 1, size: 20, role, keyword: keyword || undefined })
    return res.data.records ?? []
  } catch {
    return []
  }
}

const patientOptions = ref<AdminUser[]>([])
const therapistOptions = ref<AdminUser[]>([])

/** 下拉选项文案：编辑回显构造的 option 无 username 时只显示姓名，避免“姓名（）” */
const userLabel = (user: AdminUser): string =>
  user.username ? `${user.realName}（${user.username}）` : user.realName

const remoteSearchPatient = async (keyword: string): Promise<void> => {
  patientSearching.value = true
  try {
    patientOptions.value = await searchUsers(keyword, 'PATIENT')
  } finally {
    patientSearching.value = false
  }
}
const remoteSearchTherapist = async (keyword: string): Promise<void> => {
  therapistSearching.value = true
  try {
    therapistOptions.value = await searchUsers(keyword, 'THERAPIST')
  } finally {
    therapistSearching.value = false
  }
}

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageBindings({
      page: pagination.page,
      size: pagination.size,
      patientId: selectedPatientId.value ?? undefined,
      therapistId: selectedTherapistId.value ?? undefined,
      status: query.status || undefined
    })
    tableData.value = res.data.records ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // 拦截器统一提示
  } finally {
    loading.value = false
  }
}

/** 筛选用选中的ID（与弹窗表单隔离） */
const selectedPatientId = ref<number>()
const selectedTherapistId = ref<number>()

const handleSearch = (): void => {
  pagination.page = 1
  fetchData()
}

const handleReset = (): void => {
  query.status = ''
  selectedPatientId.value = undefined
  selectedTherapistId.value = undefined
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

// ===== 新建 / 编辑弹窗 =====
interface BindingFormState {
  patientId: number | undefined
  therapistId: number | undefined
  relationType: RelationType
  dateRange: [string, string] | null
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingBindingId = ref<number | null>(null)
const form = reactive<BindingFormState>({
  patientId: undefined,
  therapistId: undefined,
  relationType: 'PRIMARY',
  dateRange: null
})

const rules: FormRules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  therapistId: [{ required: true, message: '请选择治疗师', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }]
}

const dialogTitle = () => (editingBindingId.value == null ? '新增绑定' : '变更绑定')

const resetForm = (): void => {
  form.patientId = undefined
  form.therapistId = undefined
  form.relationType = 'PRIMARY'
  form.dateRange = null
}

const handleAdd = (): void => {
  editingBindingId.value = null
  resetForm()
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleEdit = (row: Binding): void => {
  editingBindingId.value = row.bindingId
  resetForm()
  form.patientId = row.patientId
  form.therapistId = row.therapistId
  form.relationType = row.relationType as RelationType
  form.dateRange =
    row.startAt && row.endAt
      ? [toPickerDateTime(row.startAt), toPickerDateTime(row.endAt)]
      : null
  // 保证选择器能直接显示姓名
  patientOptions.value = [
    { userId: row.patientId, realName: row.patientName, username: '' } as AdminUser
  ]
  therapistOptions.value = [
    { userId: row.therapistId, realName: row.therapistName, username: '' } as AdminUser
  ]
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleSubmit = async (): Promise<void> => {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const [startAt, endAt] = form.dateRange ?? []
  submitting.value = true
  try {
    if (editingBindingId.value == null) {
      await createBinding({
        patientId: form.patientId as number,
        therapistId: form.therapistId as number,
        relationType: form.relationType,
        startAt,
        endAt
      })
      ElMessage.success('绑定建立成功')
    } else {
      await updateBinding(editingBindingId.value, {
        relationType: form.relationType,
        startAt,
        endAt
      })
      ElMessage.success('绑定变更成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

// ===== 解除绑定 =====
const handleUnbind = (row: Binding): void => {
  ElMessageBox.prompt(`解除后患者「${row.patientName}」与治疗师「${row.therapistName}」的历史数据保留只读，请填写解绑原因`, '解除绑定', {
    confirmButtonText: '确认解绑',
    cancelButtonText: '取消',
    type: 'warning',
    inputType: 'textarea',
    inputPlaceholder: '请输入解绑原因（必填，最长 200 字）',
    inputValidator: (value: string) => {
      const v = value?.trim() ?? ''
      if (!v) {
        return '解绑原因不能为空'
      }
      return v.length <= 200 || '解绑原因最长 200 个字符'
    }
  })
    .then(async ({ value }) => {
      await unbindBinding(row.bindingId, value.trim())
      ElMessage.success('绑定已解除')
      fetchData()
    })
    .catch(() => {
      // 用户取消
    })
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="患者">
          <el-select
            v-model="selectedPatientId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="姓名/登录名搜索"
            :remote-method="remoteSearchPatient"
            :loading="patientSearching"
            style="width: 200px"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.userId"
              :label="userLabel(item)"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗师">
          <el-select
            v-model="selectedTherapistId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="姓名/登录名搜索"
            :remote-method="remoteSearchTherapist"
            :loading="therapistSearching"
            style="width: 200px"
          >
            <el-option
              v-for="item in therapistOptions"
              :key="item.userId"
              :label="userLabel(item)"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="已结束" value="ENDED" />
          </el-select>
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
        <span class="table-title">医患绑定管理</span>
        <el-button type="primary" @click="handleAdd">新增绑定</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="bindingId" label="ID" width="80" />
        <el-table-column prop="patientName" label="患者" min-width="110" />
        <el-table-column prop="therapistName" label="治疗师" min-width="110" />
        <el-table-column label="关系类型" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.relationType === 'PRIMARY' ? 'danger' : 'info'" size="small">
              {{ row.relationTypeName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生效时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="失效时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.endAt) || '长期有效' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 'ACTIVE'"
              @click="handleEdit(row as Binding)"
            >
              变更
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="row.status !== 'ACTIVE'"
              @click="handleUnbind(row as Binding)"
            >
              解绑
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

    <!-- 新增 / 变更弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle()"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="患者" prop="patientId">
          <el-select
            v-model="form.patientId"
            filterable
            remote
            clearable
            :disabled="editingBindingId != null"
            placeholder="输入姓名/登录名搜索患者"
            :remote-method="remoteSearchPatient"
            :loading="patientSearching"
            style="width: 100%"
          >
            <el-option
              v-for="item in patientOptions"
              :key="item.userId"
              :label="userLabel(item)"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="治疗师" prop="therapistId">
          <el-select
            v-model="form.therapistId"
            filterable
            remote
            clearable
            :disabled="editingBindingId != null"
            placeholder="输入姓名/登录名搜索治疗师"
            :remote-method="remoteSearchTherapist"
            :loading="therapistSearching"
            style="width: 100%"
          >
            <el-option
              v-for="item in therapistOptions"
              :key="item.userId"
              :label="userLabel(item)"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-radio-group v-model="form.relationType">
            <el-radio value="PRIMARY">主责</el-radio>
            <el-radio value="SECONDARY">协作</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="form.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间（留空取当前）"
            end-placeholder="结束时间（留空长期有效）"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
          <div v-if="editingBindingId != null" class="form-tip">
            不修改有效期时保持原时间不变；如需调整请重新选择完整的起止范围
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
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

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}
</style>
