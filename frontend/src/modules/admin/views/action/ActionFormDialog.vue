<script setup lang="ts">
import { reactive, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createAction, updateAction } from '@/api/admin/action'
import type { ActionSavePayload, AdminAction } from '@/api/admin/action'
import type { RehabPart } from '@/api/dict'

interface Props {
  partOptions: RehabPart[]
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

// ===== 弹窗与表单 =====
const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const form = reactive<ActionSavePayload>({
  name: '',
  targetPartCode: '',
  difficulty: 'EASY',
  description: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入动作名称', trigger: 'blur' },
    { max: 50, message: '名称最多 50 个字符', trigger: 'blur' }
  ],
  targetPartCode: [{ required: true, message: '请选择康复部位', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

const dialogTitle = (): string => (editingId.value == null ? '新增动作' : '编辑动作')

const resetForm = (): void => {
  form.name = ''
  form.targetPartCode = ''
  form.difficulty = 'EASY'
  form.description = ''
}

const open = (row?: AdminAction): void => {
  editingId.value = row ? row.actionId : null
  resetForm()
  if (row) {
    form.name = row.name
    form.targetPartCode = row.targetPartCode
    form.difficulty = row.difficulty as ActionSavePayload['difficulty']
  }
  visible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

defineExpose({ open })

// ===== 提交 =====
const handleSubmit = async (): Promise<void> => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload: ActionSavePayload = {
      name: form.name,
      targetPartCode: form.targetPartCode,
      difficulty: form.difficulty,
      description: form.description || undefined
    }
    if (editingId.value == null) {
      await createAction(payload)
      ElMessage.success('动作创建成功')
    } else {
      await updateAction(editingId.value, payload)
      ElMessage.success('动作更新成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 拦截器统一提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle()" width="520px" :close-on-click-modal="false" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="动作名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入动作名称" clearable maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="康复部位" prop="targetPartCode">
        <el-select v-model="form.targetPartCode" placeholder="请选择康复部位" style="width: 100%">
          <el-option v-for="item in partOptions" :key="item.code" :label="item.name" :value="item.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="难度" prop="difficulty">
        <el-radio-group v-model="form.difficulty">
          <el-radio value="EASY">简单</el-radio>
          <el-radio value="MEDIUM">中等</el-radio>
          <el-radio value="HARD">困难</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入动作描述（可选）" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
