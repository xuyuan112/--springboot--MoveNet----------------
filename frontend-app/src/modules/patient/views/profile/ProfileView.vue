<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showFailToast, showToast } from 'vant'
import {
  getMyTherapist,
  getProfile,
  updateProfile
} from '@/api/patient/profile'
import type { PatientProfileVO, PatientTherapistVO } from '@/api/patient/profile'
import { getEnabledRehabParts } from '@/api/common/dict'
import type { RehabPartDictVO } from '@/api/common/dict'
import { formatDateTime } from '@/utils/datetime'
import LoginLogPopup from './LoginLogDrawer.vue'

const router = useRouter()

const loading = ref(false)
const profile = ref<PatientProfileVO | null>(null)
const therapist = ref<PatientTherapistVO | null>(null)
const rehabParts = ref<RehabPartDictVO[]>([])

/** 登录日志弹层 */
const loginLogVisible = ref(false)

/** 编辑弹窗 */
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  name: '',
  gender: 'U' as 'M' | 'F' | 'U',
  birthday: '',
  heightCm: undefined as number | undefined,
  weightKg: undefined as number | undefined,
  rehabPartCode: '',
  medicalSummary: '',
  allergyInfo: '',
  contraindications: ''
})

/** 出生日期选择弹窗（van-date-picker 的 v-model 为 [年, 月, 日] 字符串数组） */
const birthdayPickerVisible = ref(false)
const birthdayPickerValue = ref<string[]>(['1990', '01', '01'])
const birthdayMinDate = new Date(1900, 0, 1)
const birthdayMaxDate = new Date()

/** 康复部位选择弹窗 */
const partPickerVisible = ref(false)
const partPickerColumns = computed(() =>
  rehabParts.value.map((p) => ({ text: p.name, value: p.code }))
)

/** 打开出生日期选择器：回显当前值 */
const openBirthdayPicker = (): void => {
  if (form.birthday) {
    birthdayPickerValue.value = form.birthday.split('-')
  }
  birthdayPickerVisible.value = true
}

/** van-date-picker 确认回调 */
const onBirthdayConfirm = ({ selectedValues }: { selectedValues: string[] }): void => {
  form.birthday = selectedValues.join('-')
  birthdayPickerVisible.value = false
}

/** 康复部位选中项的展示文本 */
const rehabPartText = computed(() => {
  return rehabParts.value.find((p) => p.code === form.rehabPartCode)?.name ?? ''
})

/** van-picker 确认回调 */
const onPartConfirm = ({ selectedValues }: { selectedValues: string[] }): void => {
  form.rehabPartCode = selectedValues[0] ?? ''
  partPickerVisible.value = false
}

/** 性别展示文本 */
const genderText = computed(() => {
  const map: Record<string, string> = { M: '男', F: '女', U: '未知' }
  return map[profile.value?.gender ?? 'U'] ?? '未知'
})

/** 页面展示用年龄（由出生日期换算，不存储） */
const age = computed(() => {
  const birthday = profile.value?.birthday
  if (!birthday) {
    return null
  }
  const birth = new Date(birthday)
  const now = new Date()
  let years = now.getFullYear() - birth.getFullYear()
  const m = now.getMonth() - birth.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) {
    years--
  }
  return years >= 0 ? years : null
})

const fetchAll = async (): Promise<void> => {
  loading.value = true
  try {
    const [profileRes, therapistRes, partsRes] = await Promise.all([
      getProfile(),
      getMyTherapist(),
      getEnabledRehabParts()
    ])
    profile.value = profileRes.data
    therapist.value = therapistRes.data
    rehabParts.value = partsRes.data
  } catch {
    showFailToast('资料加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 打开编辑弹窗：回显当前资料 */
const openEdit = (): void => {
  if (!profile.value) {
    return
  }
  form.name = profile.value.name ?? ''
  form.gender = profile.value.gender ?? 'U'
  form.birthday = profile.value.birthday ?? ''
  form.heightCm = profile.value.heightCm ?? undefined
  form.weightKg = profile.value.weightKg ?? undefined
  form.rehabPartCode = profile.value.rehabPartCode ?? ''
  form.medicalSummary = profile.value.medicalSummary ?? ''
  form.allergyInfo = profile.value.allergyInfo ?? ''
  form.contraindications = profile.value.contraindications ?? ''
  dialogVisible.value = true
}

const handleSave = async (): Promise<void> => {
  if (!form.name.trim()) {
    showToast('请输入姓名')
    return
  }
  saving.value = true
  try {
    const res = await updateProfile({
      name: form.name,
      gender: form.gender,
      birthday: form.birthday || undefined,
      heightCm: form.heightCm,
      weightKg: form.weightKg,
      rehabPartCode: form.rehabPartCode || undefined,
      medicalSummary: form.medicalSummary || undefined,
      allergyInfo: form.allergyInfo || undefined,
      contraindications: form.contraindications || undefined
    })
    profile.value = res.data
    dialogVisible.value = false
    showSuccessToast('资料已更新')
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    saving.value = false
  }
}

onMounted(fetchAll)
</script>

<template>
  <div class="page-container">
    <van-loading v-if="loading" class="page-loading" vertical>加载中…</van-loading>

    <template v-if="profile">
      <!-- 我的资料 -->
      <van-cell-group inset title="我的资料" class="info-group">
        <van-cell title="姓名" :value="profile.name || '—'" />
        <van-cell title="性别" :value="genderText" />
        <van-cell title="出生日期" :value="profile.birthday || '—'" />
        <van-cell title="年龄" :value="age !== null ? `${age} 岁` : '—'" />
        <van-cell title="身高" :value="profile.heightCm ? `${profile.heightCm} cm` : '—'" />
        <van-cell title="体重" :value="profile.weightKg ? `${profile.weightKg} kg` : '—'" />
        <van-cell title="主康复部位" :value="profile.rehabPartName || '—'" />
        <van-cell title="手机号" :value="profile.phone || '—'" />
        <van-cell title="既往病史" :label="profile.medicalSummary || '—'" />
        <van-cell title="过敏史" :label="profile.allergyInfo || '—'" />
        <van-cell title="禁忌症" :label="profile.contraindications || '—'" />
        <van-cell title="最后登录" :value="formatDateTime(profile.lastLoginAt)" />
        <van-cell title="资料更新" :value="formatDateTime(profile.updatedAt)" />
      </van-cell-group>

      <div class="action-btns">
        <van-button size="small" type="default" @click="loginLogVisible = true">登录日志</van-button>
        <van-button size="small" type="primary" @click="openEdit">编辑资料</van-button>
      </div>

      <!-- 快捷入口 -->
      <van-cell-group inset title="训练数据" class="info-group">
        <van-cell title="训练进度与趋势" is-link @click="router.push({ name: 'PatientProgress' })" />
        <van-cell title="训练记录" is-link @click="router.push({ name: 'PatientRecords' })" />
        <van-cell title="动作库" is-link @click="router.push({ name: 'PatientActions' })" />
        <van-cell title="评估报告" is-link @click="router.push({ name: 'PatientReports' })" />
        <van-cell title="健康内容收藏" is-link @click="router.push({ name: 'PatientContents' })" />
        <van-cell title="AI 健康问答" is-link @click="router.push({ name: 'PatientAiQa' })" />
      </van-cell-group>

      <!-- 我的治疗师 -->
      <div class="section-title">我的治疗师</div>
      <div v-if="therapist" class="therapist-card">
        <van-cell title="姓名" :value="therapist.name" />
        <van-cell title="职称" :value="therapist.title || '—'" />
        <van-cell title="专长" :value="therapist.specialty || '—'" />
        <van-cell title="关系类型">
          <template #value>
            <van-tag :type="therapist.relationType === 'PRIMARY' ? 'primary' : 'default'">
              {{ therapist.relationType === 'PRIMARY' ? '主责治疗师' : '协作治疗师' }}
            </van-tag>
          </template>
        </van-cell>
        <van-cell title="绑定时间" :value="formatDateTime(therapist.startAt)" />
        <van-cell title="简介" :label="therapist.introduction || '—'" />
      </div>
      <van-empty v-else description="暂未绑定治疗师，请联系管理员为您分配" />
    </template>

    <!-- 编辑资料弹窗 -->
    <van-dialog
      v-model:show="dialogVisible"
      title="编辑资料"
      show-cancel-button
      :confirm-button-loading="saving"
      @confirm="handleSave"
    >
      <van-form class="edit-form">
        <van-field
          v-model="form.name"
          label="姓名"
          placeholder="请输入真实姓名"
          maxlength="50"
          :rules="[{ required: true, message: '请输入姓名' }]"
        />
        <van-field name="gender" label="性别">
          <template #input>
            <van-radio-group v-model="form.gender" direction="horizontal">
              <van-radio name="M">男</van-radio>
              <van-radio name="F">女</van-radio>
              <van-radio name="U">未知</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
          v-model="form.birthday"
          label="出生日期"
          placeholder="选择出生日期"
          is-link
          readonly
          @click="openBirthdayPicker"
        />
        <van-popup v-model:show="birthdayPickerVisible" position="bottom" round>
          <van-date-picker
            v-model="birthdayPickerValue"
            title="选择出生日期"
            :min-date="birthdayMinDate"
            :max-date="birthdayMaxDate"
            @confirm="onBirthdayConfirm"
            @cancel="birthdayPickerVisible = false"
          />
        </van-popup>

        <van-field label="身高(cm)">
          <template #input>
            <van-stepper v-model="form.heightCm" :min="50" :max="250" :decimal-length="1" placeholder="50~250" />
          </template>
        </van-field>
        <van-field label="体重(kg)">
          <template #input>
            <van-stepper v-model="form.weightKg" :min="10" :max="300" :decimal-length="1" placeholder="10~300" />
          </template>
        </van-field>
        <van-field
          :model-value="rehabPartText"
          label="主康复部位"
          is-link
          readonly
          placeholder="请选择康复部位"
          @click="partPickerVisible = true"
        />
        <van-popup v-model:show="partPickerVisible" position="bottom" round>
          <van-picker
            :columns="partPickerColumns"
            @confirm="onPartConfirm"
            @cancel="partPickerVisible = false"
          />
        </van-popup>

        <van-field
          v-model="form.medicalSummary"
          label="既往病史"
          type="textarea"
          rows="2"
          maxlength="1000"
          show-word-limit
        />
        <van-field
          v-model="form.allergyInfo"
          label="过敏史"
          type="textarea"
          rows="2"
          maxlength="500"
          show-word-limit
        />
        <van-field
          v-model="form.contraindications"
          label="禁忌症"
          type="textarea"
          rows="2"
          maxlength="500"
          show-word-limit
        />
      </van-form>
    </van-dialog>

    <!-- 登录日志弹层 -->
    <LoginLogPopup v-model:visible="loginLogVisible" />
  </div>
</template>

<style scoped lang="scss">
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.info-group {
  margin-bottom: 12px;
}

.action-btns {
  display: flex;
  gap: 8px;
  padding: 0 4px;
  margin-bottom: 16px;

  .van-button {
    flex: 1;
  }
}

.section-title {
  margin: 0 4px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
}

.therapist-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 4px 0;
  margin-bottom: 12px;
}

.edit-form {
  padding: 8px 0;
}
</style>
