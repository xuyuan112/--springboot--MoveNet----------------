<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { getMyPlans } from '@/api/patient/plan'
import type { PatientPlan } from '@/api/patient/plan'

const router = useRouter()

const plans = ref<PatientPlan[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const statusFilter = ref('')

/** van-list 加载状态 */
const listLoading = ref(false)
const listFinished = ref(false)
const refreshing = ref(false)

const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '进行中', value: 'PUBLISHED' },
  { text: '已完成', value: 'COMPLETED' },
  { text: '已暂停', value: 'PAUSED' },
  { text: '已归档', value: 'ARCHIVED' }
]

/** 状态标签颜色（Vant tag type） */
const statusTagType = (status: string): 'success' | 'warning' | 'danger' | 'default' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'default'> = {
    PUBLISHED: 'success',
    COMPLETED: 'default',
    PAUSED: 'warning',
    ARCHIVED: 'danger',
    DRAFT: 'default'
  }
  return map[status] ?? 'default'
}

/** 频次展示文本 */
const frequencyText = (plan: PatientPlan): string => {
  const typeMap: Record<string, string> = { DAILY: '每日', WEEKLY: '每周', CUSTOM: '自定义' }
  const type = typeMap[plan.frequencyType] ?? plan.frequencyType
  if (plan.frequencyValue != null && plan.frequencyValue > 0) {
    return `${type} (${plan.frequencyValue} 次)`
  }
  return type
}

/** van-list 触底加载一页 */
const onLoad = async (): Promise<void> => {
  try {
    const res = await getMyPlans({
      page: page.value,
      size,
      status: statusFilter.value || undefined
    })
    if (refreshing.value) {
      plans.value = []
      refreshing.value = false
    }
    plans.value.push(...res.data.records)
    total.value = res.data.total
    if (plans.value.length >= total.value || res.data.records.length === 0) {
      listFinished.value = true
    } else {
      page.value += 1
    }
  } catch {
    showFailToast('计划列表加载失败')
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

/** 下拉刷新 / 状态切换：回到第一页重新加载 */
const resetAndReload = (): void => {
  page.value = 1
  listFinished.value = false
  listLoading.value = true
  onLoad()
}

const handleStatusChange = (): void => {
  refreshing.value = true
  resetAndReload()
}

const goDetail = (planId: number): void => {
  router.push(`/patient/plans/${planId}`)
}
</script>

<template>
  <div class="page-container">
    <van-dropdown-menu active-color="#2563eb">
      <van-dropdown-item v-model="statusFilter" :options="statusOptions" @change="handleStatusChange" />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="resetAndReload">
      <van-list
        v-model:loading="listLoading"
        :finished="listFinished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="listFinished && plans.length === 0" description="暂无康复计划" />

        <div
          v-for="plan in plans"
          :key="plan.planId"
          class="plan-card"
          @click="goDetail(plan.planId)"
        >
          <div class="plan-head">
            <span class="plan-name">{{ plan.name }}</span>
            <van-tag :type="statusTagType(plan.status)">{{ plan.statusName || plan.status }}</van-tag>
          </div>
          <van-cell-group :border="false" class="plan-cells">
            <van-cell title="康复部位" :value="plan.rehabPartName || '—'" />
            <van-cell title="治疗师" :value="plan.therapistName || '—'" />
            <van-cell title="起止日期" :value="`${plan.startDate} ~ ${plan.endDate}`" />
            <van-cell title="频次" :value="frequencyText(plan)" />
          </van-cell-group>
          <div class="plan-more">
            查看详情
            <van-icon name="arrow" />
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.plan-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .plan-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 4px;

    .plan-name {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .plan-cells {
    :deep(.van-cell) {
      padding: 4px 0;
    }
  }

  .plan-more {
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
