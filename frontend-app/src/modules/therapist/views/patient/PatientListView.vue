<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import {
  GENDER_NAMES,
  RELATION_TYPE_NAMES,
  listTherapistPatients
} from '@/api/therapist/patient'
import type { TherapistPatient } from '@/api/therapist/patient'
import { PLAN_STATUS_NAMES, planStatusTagType } from '@/api/therapist/plan'
import { getEnabledRehabParts } from '@/api/common/dict'

const router = useRouter()

const SIZE = 10

// ===== 筛选状态（关键词 + 主康复部位 + 最近计划状态） =====
const keyword = ref('')
const partCode = ref('')
const planStatus = ref('')

/** @type {import('vue').Ref<{ text: string; value: string }[]>} */
const partOptions = ref<{ text: string; value: string }[]>([{ text: '全部部位', value: '' }])

const loadParts = async (): Promise<void> => {
  try {
    const res = await getEnabledRehabParts()
    partOptions.value = [
      { text: '全部部位', value: '' },
      ...(res.data ?? []).map((p) => ({ text: p.name, value: p.code }))
    ]
  } catch {
    // 错误提示由响应拦截器统一处理
  }
}

const planStatusOptions = [
  { text: '全部状态', value: '' },
  ...Object.entries(PLAN_STATUS_NAMES).map(([value, text]) => ({ text, value }))
]

// ===== 列表分页（van-list 增量加载） =====
/** @type {import('vue').Ref<TherapistPatient[]>} */
const patients = ref<TherapistPatient[]>([])
const total = ref(0)
const page = ref(1)
const listLoading = ref(false)
const listFinished = ref(false)
const refreshing = ref(false)

const onLoad = async (): Promise<void> => {
  try {
    const res = await listTherapistPatients({
      page: page.value,
      size: SIZE,
      keyword: keyword.value.trim() || undefined,
      partCode: partCode.value || undefined,
      planStatus: planStatus.value || undefined
    })
    if (refreshing.value) {
      patients.value = []
      refreshing.value = false
    }
    patients.value.push(...res.data.records)
    total.value = res.data.total
    if (patients.value.length >= total.value || res.data.records.length === 0) {
      listFinished.value = true
    } else {
      page.value += 1
    }
  } catch {
    showFailToast('患者列表加载失败')
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

/** 下拉刷新 / 筛选变化：回到第一页重新加载 */
const resetAndReload = (): void => {
  page.value = 1
  listFinished.value = false
  listLoading.value = true
  void onLoad()
}

const handleFilterChange = (): void => {
  refreshing.value = true
  resetAndReload()
}

const handleSearch = (): void => {
  handleFilterChange()
}

// ===== 展示辅助 =====
/** 由出生日期计算年龄 */
const calcAge = (birthday?: string): number | null => {
  if (!birthday) return null
  const birth = new Date(birthday)
  if (Number.isNaN(birth.getTime())) return null
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const monthDiff = now.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
    age -= 1
  }
  return age
}

const genderAgeText = (p: TherapistPatient): string => {
  const gender = p.gender ? (GENDER_NAMES[p.gender] ?? p.gender) : '—'
  const age = calcAge(p.birthday)
  return `${gender} / ${age == null ? '—' : `${age} 岁`}`
}

const relationText = (p: TherapistPatient): string => {
  if (!p.relationType) return '—'
  return RELATION_TYPE_NAMES[p.relationType] ?? p.relationType
}

const latestStatusText = (p: TherapistPatient): string => {
  if (!p.latestPlanStatus) return '—'
  return p.latestPlanStatusName ?? PLAN_STATUS_NAMES[p.latestPlanStatus] ?? p.latestPlanStatus
}

/** 点击卡片进入患者详情 */
const goDetail = (patientId: number): void => {
  router.push(`/therapist/patients/${patientId}`)
}

onMounted(loadParts)
</script>

<template>
  <div class="page-container">
    <van-search
      v-model="keyword"
      placeholder="搜索姓名/手机号"
      @search="handleSearch"
      @clear="handleSearch"
    />
    <van-dropdown-menu active-color="#2563eb">
      <van-dropdown-item v-model="partCode" :options="partOptions" @change="handleFilterChange" />
      <van-dropdown-item
        v-model="planStatus"
        :options="planStatusOptions"
        @change="handleFilterChange"
      />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="resetAndReload">
      <van-list
        v-model:loading="listLoading"
        :finished="listFinished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="listFinished && patients.length === 0" description="暂无绑定患者" />

        <div
          v-for="p in patients"
          :key="p.patientId"
          class="patient-card"
          @click="goDetail(p.patientId)"
        >
          <div class="card-head">
            <span class="card-name">{{ p.name }}</span>
            <van-tag v-if="p.hasRiskHint" type="danger">风险提示</van-tag>
            <van-tag
              v-if="p.latestPlanStatus"
              :type="planStatusTagType(p.latestPlanStatus)"
            >
              {{ latestStatusText(p) }}
            </van-tag>
          </div>
          <van-cell-group :border="false" class="card-cells">
            <van-cell title="性别/年龄" :value="genderAgeText(p)" />
            <van-cell title="主康复部位" :value="p.rehabPartName || '—'" />
            <van-cell title="绑定关系" :value="relationText(p)" />
            <van-cell title="在训计划数" :value="String(p.activePlanCount ?? 0)" />
            <van-cell
              title="近7日完成率"
              :value="p.recentCompletionRate == null ? '—' : `${p.recentCompletionRate}%`"
            />
          </van-cell-group>
          <div class="card-more">
            查看详情
            <van-icon name="arrow" />
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.patient-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 4px;

    .card-name {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .card-cells {
    :deep(.van-cell) {
      padding: 4px 0;
    }
  }

  .card-more {
    margin-top: 4px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 2px;
    font-size: 13px;
    color: #2563eb;
  }
}
</style>
