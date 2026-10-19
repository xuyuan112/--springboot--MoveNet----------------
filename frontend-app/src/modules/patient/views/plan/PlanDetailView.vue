<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast } from 'vant'
import { getMyPlanDetail, getMyPlanItems } from '@/api/patient/plan'
import type { PatientPlan, PatientPlanItem } from '@/api/patient/plan'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const loading = ref(false)
const plan = ref<PatientPlan | null>(null)
const items = ref<PatientPlanItem[]>([])

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

const frequencyText = (p: PatientPlan): string => {
  const typeMap: Record<string, string> = { DAILY: '每日', WEEKLY: '每周', CUSTOM: '自定义' }
  const type = typeMap[p.frequencyType] ?? p.frequencyType
  return p.frequencyValue ? `${type} (${p.frequencyValue} 次)` : type
}

const fetchDetail = async (): Promise<void> => {
  const planId = Number(route.params.planId)
  if (!planId) {
    showFailToast('缺少计划 ID')
    return
  }
  loading.value = true
  try {
    const [planRes, itemsRes] = await Promise.all([getMyPlanDetail(planId), getMyPlanItems(planId)])
    plan.value = planRes.data
    items.value = itemsRes.data
  } catch {
    showFailToast('计划详情加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchDetail)
</script>

<template>
  <div class="page-container">
    <van-loading v-if="loading" class="page-loading" vertical>加载中…</van-loading>

    <template v-if="plan">
      <div class="plan-header">
        <span class="plan-title">{{ plan.name }}</span>
        <van-tag :type="statusTagType(plan.status)">{{ plan.statusName || plan.status }}</van-tag>
      </div>

      <van-cell-group inset>
        <van-cell title="计划ID" :value="String(plan.planId)" />
        <van-cell title="版本号" :value="`v${plan.versionNo}`" />
        <van-cell title="治疗师" :value="plan.therapistName || '—'" />
        <van-cell title="康复部位" :value="plan.rehabPartName || plan.rehabPartCode || '—'" />
        <van-cell title="频次" :value="frequencyText(plan)" />
        <van-cell title="周期" :value="`${plan.startDate} ~ ${plan.endDate}`" />
        <van-cell title="创建时间" :value="formatDateTime(plan.createdAt)" />
        <van-cell title="注意事项" :label="plan.remark || '—'" />
      </van-cell-group>

      <div class="section-title">动作项（{{ items.length }} 项）</div>
      <van-empty v-if="items.length === 0" description="该计划暂无动作项" />

      <div v-for="item in items" :key="item.planItemId" class="item-card">
        <div class="item-head">
          <span class="item-seq">{{ item.sequenceNo }}</span>
          <span class="item-name">{{ item.actionName || '训练动作' }}</span>
          <span class="item-version">v{{ item.versionNo ?? '—' }}</span>
        </div>
        <van-cell-group :border="false" class="item-cells">
          <van-cell title="组数 × 次数" :value="`${item.targetSets} × ${item.targetReps}`" />
          <van-cell title="目标幅度" :value="item.targetAmplitude != null ? `${item.targetAmplitude}°` : '—'" />
          <van-cell title="单组时长" :value="item.durationSec != null ? `${item.durationSec} 秒` : '—'" />
          <van-cell title="组间休息" :value="item.restSec != null ? `${item.restSec} 秒` : '—'" />
        </van-cell-group>
        <div v-if="item.instruction" class="item-instruction">动作指导：{{ item.instruction }}</div>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.plan-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 4px 12px;

  .plan-title {
    font-size: 17px;
    font-weight: 600;
    color: #1f2937;
  }
}

.section-title {
  margin: 16px 4px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
}

.item-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .item-head {
    display: flex;
    align-items: center;
    gap: 8px;

    .item-seq {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #2563eb;
      color: #ffffff;
      font-size: 12px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .item-name {
      flex: 1;
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
    }

    .item-version {
      font-size: 12px;
      color: #9ca3af;
    }
  }

  .item-cells {
    :deep(.van-cell) {
      padding: 4px 0;
    }
  }

  .item-instruction {
    margin-top: 6px;
    font-size: 12px;
    color: #6b7280;
    line-height: 1.5;
  }
}
</style>
