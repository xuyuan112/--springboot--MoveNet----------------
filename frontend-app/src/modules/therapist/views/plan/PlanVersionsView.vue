<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import { listPlanVersions } from '@/api/therapist/plan'
import type { TherapistPlan } from '@/api/therapist/plan'
import { PLAN_STATUS_NAMES, planStatusTagType } from '@/api/therapist/plan'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()

const planId = computed(() => Number(route.params.planId) || 0)
const SIZE = 10

/** @type {import('vue').Ref<TherapistPlan[]>} */
const versions = ref<TherapistPlan[]>([])
const page = ref(1)
const total = ref(0)
const listLoading = ref(false)
const listFinished = ref(false)
const refreshing = ref(false)

const onLoad = async (): Promise<void> => {
  try {
    const res = await listPlanVersions(planId.value, { page: page.value, size: SIZE })
    if (refreshing.value) {
      versions.value = []
      refreshing.value = false
    }
    versions.value.push(...res.data.records)
    total.value = res.data.total
    if (versions.value.length >= total.value || res.data.records.length === 0) {
      listFinished.value = true
    } else {
      page.value += 1
    }
  } catch {
    showFailToast('版本列表加载失败')
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

const resetAndReload = (): void => {
  page.value = 1
  listFinished.value = false
  listLoading.value = true
  void onLoad()
}

const statusText = (plan: TherapistPlan): string =>
  plan.statusName ?? PLAN_STATUS_NAMES[plan.status] ?? plan.status

/** 点击版本行进入对应版本的计划详情/编辑页 */
const goPlan = (plan: TherapistPlan): void => {
  router.push(`/therapist/plans/${plan.planId}`)
}
</script>

<template>
  <div class="page-container">
    <van-pull-refresh v-model="refreshing" @refresh="resetAndReload">
      <van-list
        v-model:loading="listLoading"
        :finished="listFinished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="listFinished && versions.length === 0" description="暂无计划版本" />

        <div v-for="plan in versions" :key="plan.planId" class="version-card" @click="goPlan(plan)">
          <div class="card-head">
            <span class="card-title">{{ plan.name }}</span>
            <span class="card-version">v{{ plan.versionNo ?? '—' }}</span>
            <van-tag :type="planStatusTagType(plan.status)">{{ statusText(plan) }}</van-tag>
          </div>
          <van-cell-group :border="false" class="card-cells">
            <van-cell
              title="起止日期"
              :value="`${plan.startDate ?? '—'} ~ ${plan.endDate ?? '—'}`"
            />
            <van-cell title="创建时间" :value="formatDateTime(plan.createdAt) || '—'" />
          </van-cell-group>
          <div class="card-more">
            进入此版本
            <van-icon name="arrow" />
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.version-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .card-head {
    display: flex;
    align-items: center;
    gap: 8px;

    .card-title {
      flex: 1;
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
    }

    .card-version {
      font-size: 12px;
      color: #9ca3af;
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
