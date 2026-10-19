<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createPart,
  deletePart,
  pageParts,
  updatePart
} from '@/api/dict'
import type { PartGroupCode, RehabPart } from '@/api/dict'

/** 分组下拉选项 */
const GROUP_OPTIONS: Array<{ label: string; value: PartGroupCode }> = [
  { label: '上肢', value: 'UPPER' },
  { label: '下肢', value: 'LOWER' },
  { label: '脊柱', value: 'SPINE' },
  { label: '全身综合', value: 'OVERALL' }
]

// ===== 列表查询 =====
const loading = ref(false)
const tableData = ref<RehabPart[]>([])

/** 分页参数 */
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

/** 顶部筛选条件（占位 + 已接入参数） */
const query = reactive({
  groupCode: '',
  keyword: ''
})

const fetchData = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await pageParts({
      page: pagination.page,
      size: pagination.size,
      groupCode: query.groupCode || undefined,
      keyword: query.keyword || undefined
    })
    tableData.value = res.data.records ?? []
    pagination.total = res.data.total ?? 0
  } catch {
    // 错误提示已由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

const handleSearch = (): void => {
  pagination.page = 1
  fetchData()
}

const handleReset = (): void => {
  query.groupCode = ''
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

/** 切换启用状态（乐观更新，失败回滚） */
const handleToggleEnabled = async (row: RehabPart, enabled: boolean): Promise<void> => {
  const previous = row.enabled
  row.enabled = enabled
  try {
    await updatePart({ ...row, enabled })
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch {
    row.enabled = previous
  }
}

// ===== 新增 / 编辑弹窗 =====
interface PartForm {
  id?: number
  code: string
  name: string
  groupCode: PartGroupCode | string
  sortNo: number
  enabled: boolean
}

function createEmptyForm(): PartForm {
  return {
    id: undefined,
    code: '',
    name: '',
    groupCode: '',
    sortNo: 0,
    enabled: true
  }
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<PartForm>(createEmptyForm())

/** 是否编辑态（有主键 id 即为编辑） */
const isEdit = ref(false)

const rules: FormRules = {
  code: [{ required: true, message: '请输入部位编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入部位名称', trigger: 'blur' }],
  groupCode: [{ required: true, message: '请选择所属分组', trigger: 'change' }],
  sortNo: [{ required: true, message: '请输入排序号', trigger: 'blur' }]
}

const handleAdd = (): void => {
  Object.assign(form, createEmptyForm())
  isEdit.value = false
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleEdit = (row: RehabPart): void => {
  Object.assign(form, createEmptyForm(), row)
  isEdit.value = true
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

  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePart({ ...form })
      ElMessage.success('修改成功')
    } else {
      await createPart({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误提示已由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}

// ===== 删除 =====
const handleDelete = (row: RehabPart): void => {
  ElMessageBox.confirm(`确定删除康复部位「${row.name}」(${row.code}) 吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deletePart(row.code)
      ElMessage.success('删除成功')
      // 删除最后一页唯一一条数据时回退一页
      if (tableData.value.length === 1 && pagination.page > 1) {
        pagination.page -= 1
      }
      fetchData()
    })
    .catch(() => {
      // 用户取消删除，无需处理
    })
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 顶部筛选 + 新增 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="部位分组">
          <el-select
            v-model="query.groupCode"
            placeholder="全部分组"
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="item in GROUP_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="编码 / 名称"
            clearable
            style="width: 200px"
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
        <span class="table-title">康复部位字典</span>
        <el-button type="primary" @click="handleAdd">新增</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="code" label="编码" min-width="120" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="分组" min-width="110">
          <template #default="{ row }">
            <el-tag>{{ row.groupCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序号" min-width="90" />
        <el-table-column label="是否启用" min-width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="(val) => handleToggleEnabled(row as RehabPart, Boolean(val))"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row as RehabPart)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row as RehabPart)">删除</el-button>
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
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑康复部位' : '新增康复部位'"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="部位编码" prop="code">
          <el-input
            v-model="form.code"
            placeholder="如 KNEE"
            :disabled="isEdit"
            clearable
          />
        </el-form-item>
        <el-form-item label="部位名称" prop="name">
          <el-input v-model="form.name" placeholder="如 膝关节" clearable />
        </el-form-item>
        <el-form-item label="所属分组" prop="groupCode">
          <el-select v-model="form.groupCode" placeholder="请选择分组" style="width: 100%">
            <el-option
              v-for="item in GROUP_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
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
</style>
