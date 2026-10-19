<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getTherapistDashboard } from '@/api/therapist/dashboard'
import type { TherapistDashboard } from '@/api/therapist/dashboard'

const router = useRouter()

/** @type {import('vue').Ref<TherapistDashboard | null>} */
const dashboard = ref<TherapistDashboard | null>(null)
const loading = ref(false)

const fetchDashboard = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getTherapistDashboard()
    dashboard.value = res.data
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 今日任务完成率展示：无任务时后端返回 null */
const completionRateText = computed<string>(() => {
  const rate = dashboard.value?.todayCompletionRate
  return rate == null ? '—' : `${rate}%`
})

/** 患者数卡与患者管理入口 */
const goPatients = (): void => {
  router.push('/therapist/patients')
}

/** 康复计划管理入口（预留：必须从患者详情进入，绑定校验在患者维度） */
const goPlanManagement = (): void => {
  showToast('请从患者详情进入计划管理')
}

onMounted(fetchDashboard)
</script>

<template>
  <div class="page-container">
    <van-pull-refresh v-model="loading" @refresh="fetchDashboard">
      <van-empty v-if="!loading && !dashboard" description="工作台数据加载失败，请下拉重试" />

      <template v-if="dashboard">
        <div class="stat-grid">
          <div class="stat-card clickable" @click="goPatients">
            <div class="stat-value">{{ dashboard.patientCount }}</div>
            <div class="stat-label">负责患者数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">
              {{ dashboard.todayTaskCompleted }}/{{ dashboard.todayTaskTotal }}
            </div>
            <div class="stat-label">今日任务（完成率 {{ completionRateText }}）</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ dashboard.pendingReportCount }}</div>
            <div class="stat-label">待审核报告数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ dashboard.pendingConsultCount }}</div>
            <div class="stat-label">待回复咨询数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ dashboard.lowScoreTodayCount }}</div>
            <div class="stat-label">今日低分训练数（仅提示）</div>
          </div>
        </div>

        <div class="section-title">快捷入口</div>
        <van-cell-group inset>
          <van-cell title="患者管理" icon="friends-o" is-link @click="goPatients" />
          <van-cell title="康复计划管理" icon="notes-o" is-link @click="goPlanManagement" />
        </van-cell-group>
      </template>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 12px;

  .stat-card {
    background: #ffffff;
    border-radius: 8px;
    padding: 16px 12px;
    text-align: center;

    &.clickable {
      cursor: pointer;
    }

    &:last-child {
      grid-column: 1 / -1;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: #2563eb;
    }

    .stat-label {
      margin-top: 6px;
      font-size: 12px;
      color: #6b7280;
    }
  }
}

.section-title {
  margin: 8px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
}
</style>
