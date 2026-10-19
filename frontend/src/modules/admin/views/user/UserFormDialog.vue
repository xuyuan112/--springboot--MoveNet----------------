<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createUser,
  getUserDetail,
  updateUser
} from '@/api/admin/user'
import type {
  AdminUserCreatePayload,
  AdminUserUpdatePayload,
  GenderCode,
  ManagedRole
} from '@/api/admin/user'

const props = defineProps<{
  /** 弹窗可见性（v-model） */
  modelValue: boolean
  /** 传入 userId 为编辑态，否则为新建态 */
  userId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  /** 新建/修改成功后通知列表刷新 */
  success: []
}>()

const GENDER_OPTIONS: Array<{ label: string; value: GenderCode }> = [
  { label: '男', value: 'M' },
  { label: '女', value: 'F' },
  { label: '未知', value: 'U' }
]

interface UserFormState {
  username: string
  roleCode: ManagedRole | ''
  phone: string
  realName: string
  gender: GenderCode | ''
  birthDate: string
  heightCm: number | null
  weightKg: number | null
  medicalHistory: string
  allergyInfo: string
  contraindication: string
  title: string
  specialty: string
  licenseNo: string
  introduction: string
}

function emptyForm(): UserFormState {
  return {
    username: '',
    roleCode: 'PATIENT',
    phone: '',
    realName: '',
    gender: '',
    birthDate: '',
    heightCm: null,
    weightKg: null,
    medicalHistory: '',
    allergyInfo: '',
    contraindication: '',
    title: '',
    specialty: '',
    licenseNo: '',
    introduction: ''
  }
}

const formRef = ref<FormInstance>()
const form = reactive<UserFormState>(emptyForm())
const submitting = ref(false)
const detailLoading = ref(false)

const isEdit = () => props.userId != null

const dialogTitle = () => (isEdit() ? '编辑账号资料' : '新增账号')

const rules: FormRules = {
  username: [
    { required: true, message: '请输入登录名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]{3,49}$/,
      message: '字母开头，仅含字母/数字/下划线，4~50 位',
      trigger: 'blur'
    }
  ],
  roleCode: [{ required: true, message: '请选择角色', trigger: 'change' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  title: [{ required: true, message: '请输入职称', trigger: 'blur' }],
  specialty: [{ required: true, message: '请输入专长', trigger: 'blur' }]
}

const closeDialog = (): void => {
  emit('update:modelValue', false)
}

/** 弹窗打开时：编辑态拉取详情，新建态重置表单 */
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) {
      return
    }
    Object.assign(form, emptyForm())
    formRef.value?.clearValidate()
    if (isEdit()) {
      detailLoading.value = true
      try {
        const res = await getUserDetail(props.userId as number)
        const d = res.data
        Object.assign(form, {
          username: d.username,
          roleCode: (d.roleCode === 'PATIENT' || d.roleCode === 'THERAPIST' ? d.roleCode : '') as ManagedRole | '',
          phone: d.phone ?? '',
          realName: d.realName ?? '',
          gender: (d.gender ?? '') as GenderCode | '',
          birthDate: d.birthDate ?? '',
          heightCm: d.heightCm ?? null,
          weightKg: d.weightKg ?? null,
          medicalHistory: d.medicalHistory ?? '',
          allergyInfo: d.allergyInfo ?? '',
          contraindication: d.contraindication ?? '',
          title: d.title ?? '',
          specialty: d.specialty ?? '',
          licenseNo: d.licenseNo ?? '',
          introduction: d.introduction ?? ''
        })
      } catch {
        // 错误提示由响应拦截器统一处理
        closeDialog()
      } finally {
        detailLoading.value = false
      }
    }
  }
)

/** 去掉空字符串，避免后端收到 "" 与可选字段语义冲突 */
function blankToUndefined(value: string): string | undefined {
  return value.trim() === '' ? undefined : value.trim()
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
    if (isEdit()) {
      const payload: AdminUserUpdatePayload = {
        phone: blankToUndefined(form.phone),
        realName: form.realName.trim(),
        heightCm: form.heightCm ?? undefined,
        weightKg: form.weightKg ?? undefined,
        medicalHistory: form.medicalHistory || undefined,
        allergyInfo: form.allergyInfo || undefined,
        contraindication: form.contraindication || undefined,
        title: form.title || undefined,
        specialty: form.specialty || undefined,
        licenseNo: form.licenseNo || undefined,
        introduction: form.introduction || undefined
      }
      if (form.roleCode === 'PATIENT') {
        payload.gender = (form.gender || undefined) as GenderCode
        payload.birthDate = form.birthDate || undefined
      }
      await updateUser(props.userId as number, payload)
      ElMessage.success('修改成功')
    } else {
      const payload: AdminUserCreatePayload = {
        username: form.username.trim(),
        roleCode: form.roleCode as ManagedRole,
        phone: blankToUndefined(form.phone),
        realName: form.realName.trim()
      }
      if (form.roleCode === 'PATIENT') {
        payload.gender = form.gender as GenderCode
        payload.birthDate = form.birthDate
        payload.heightCm = form.heightCm ?? undefined
        payload.weightKg = form.weightKg ?? undefined
        payload.medicalHistory = form.medicalHistory || undefined
        payload.allergyInfo = form.allergyInfo || undefined
        payload.contraindication = form.contraindication || undefined
      } else {
        payload.title = form.title || undefined
        payload.specialty = form.specialty || undefined
        payload.licenseNo = form.licenseNo || undefined
        payload.introduction = form.introduction || undefined
      }
      const res = await createUser(payload)
      ElMessage.success(
        `账号创建成功，初始密码为平台统一初始密码（线下交付：${res.data.initialPasswordDelivered ? '已交付' : '待交付'}）`
      )
    }
    emit('success')
    closeDialog()
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle()"
    width="640px"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      v-loading="detailLoading"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-divider content-position="left">账号信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="登录名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="字母开头，4~50 位"
              :disabled="isEdit()"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="角色" prop="roleCode">
            <el-select
              v-model="form.roleCode"
              :disabled="isEdit()"
              style="width: 100%"
            >
              <el-option label="患者" value="PATIENT" />
              <el-option label="治疗师" value="THERAPIST" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="选填，11 位手机号" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 患者档案字段 -->
      <template v-if="form.roleCode === 'PATIENT'">
        <el-divider content-position="left">患者档案</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width: 100%">
                <el-option
                  v-for="item in GENDER_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                placeholder="选择出生日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身高(cm)">
              <el-input-number
                v-model="form.heightCm"
                :min="50"
                :max="250"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体重(kg)">
              <el-input-number
                v-model="form.weightKg"
                :min="10"
                :max="300"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="既往病史">
              <el-input v-model="form.medicalHistory" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="过敏史">
              <el-input v-model="form.allergyInfo" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="禁忌症">
              <el-input v-model="form.contraindication" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>

      <!-- 治疗师档案字段 -->
      <template v-else-if="form.roleCode === 'THERAPIST'">
        <el-divider content-position="left">治疗师档案</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-input v-model="form.title" placeholder="如 主治医师" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专长" prop="specialty">
              <el-input v-model="form.specialty" placeholder="如 运动损伤康复" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资质编号">
              <el-input v-model="form.licenseNo" placeholder="选填" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="个人简介">
              <el-input v-model="form.introduction" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
