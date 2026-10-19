<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listReports } from '@/api/patient/report'
import type { ReportItem } from '@/api/patient/report'
import { formatDateTime } from '@/utils/datetime'

const router = useRouter()
const SIZE = 10

const reports = ref<ReportItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const error = ref(false)
const refreshing = ref(false)

const fetchPage = async (): Promise<void> => {
  // 患者端仅可见已生效报告（status=EFFECTIVE），无需传参
  const res = await listReports({ page: page.value, size: SIZE })
  const list = res.data.records ?? []
  if (page.value === 1) {
    reports.value = list
  } else {
    reports.value.push(...list)
  }
  total.value = res.data.total ?? 0
  page.value += 1
  if (list.length < SIZE || reports.value.length >= total.value) {
    finished.value = true
  }
}

const reload = async (): Promise<void> => {
  loading.value = true
  reports.value = []
  page.value = 1
  finished.value = false
  error.value = false
  try {
    await fetchPage()
  } catch {
    error.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const onLoad = async (): Promise<void> => {
  if (finished.value) {
    loading.value = false
    return
  }
  try {
    await fetchPage()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

const openDetail = (item: ReportItem): void => {
  router.push({ name: 'PatientReportDetail', params: { reportId: item.reportId } })
}

onMounted(reload)
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="评估报告" />

    <van-pull-refresh v-model="refreshing" @refresh="reload">
      <van-list
        v-model:loading="loading"
        v-model:error="error"
        :finished="finished"
        error-text="加载失败，点击重试"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="!loading && reports.length === 0" description="暂无已生效的评估报告" />

        <div v-for="item in reports" :key="item.reportId" class="report-card" @click="openDetail(item)">
          <div class="report-head">
            <span class="report-plan">{{ item.planName }}</span>
            <van-tag type="success">{{ item.statusName || '已生效' }}</van-tag>
          </div>
          <div class="report-period">
            统计周期：{{ item.periodStart }} ~ {{ item.periodEnd }}
          </div>
          <div class="report-foot">
            <span class="report-version">计划 V{{ item.planVersion ?? '-' }}</span>
            <span class="report-time">生成于 {{ formatDateTime(item.createdAt) }}</span>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.report-card {
  margin: 12px 16px 0;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;

  &:active {
    background: #f2f3f5;
  }
}

.report-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-plan {
  font-size: 15px;
  font-weight: 600;
  color: #323233;
}

.report-period {
  margin: 8px 0;
  font-size: 13px;
  color: #646566;
}

.report-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #969799;
}
</style>
