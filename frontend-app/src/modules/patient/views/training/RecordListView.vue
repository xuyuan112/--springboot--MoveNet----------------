<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listTrainingRecords } from '@/api/patient/training'
import type { TrainingRecordItem } from '@/api/patient/training'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()

const SIZE = 10

// ===== 筛选状态（日期范围 + 计划，动作版本筛选由计划详情页跳转场景使用 query 透传） =====
const planId = ref<number | null>(route.query.planId ? Number(route.query.planId) : null)
const actionVersionId = ref<number | null>(
  route.query.actionVersionId ? Number(route.query.actionVersionId) : null
)
const fromDate = ref('')
const toDate = ref('')
const calendarVisible = ref(false)
const calendarValue = ref<Date[]>([])

const pad = (n: number): string => String(n).padStart(2, '0')
const fmtDay = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const rangeText = (): string => {
  if (fromDate.value && toDate.value) return `${fromDate.value} ~ ${toDate.value}`
  if (fromDate.value) return `${fromDate.value} 起`
  if (toDate.value) return `${toDate.value} 止`
  return '全部日期'
}

const hasFilter = (): boolean =>
  Boolean(fromDate.value || toDate.value || planId.value || actionVersionId.value)

const onCalendarConfirm = (dates: Date[]): void => {
  if (dates && dates.length === 2) {
    fromDate.value = fmtDay(dates[0])
    toDate.value = fmtDay(dates[1])
  } else {
    fromDate.value = ''
    toDate.value = ''
  }
  calendarVisible.value = false
  reload()
}

const clearPlanFilter = (): void => {
  planId.value = null
  reload()
}

const clearActionFilter = (): void => {
  actionVersionId.value = null
  reload()
}

const clearAllFilters = (): void => {
  fromDate.value = ''
  toDate.value = ''
  planId.value = null
  actionVersionId.value = null
  reload()
}

// ===== 列表分页（van-list 增量加载） =====
const records = ref<TrainingRecordItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const fetchPage = async (): Promise<void> => {
  const res = await listTrainingRecords({
    page: page.value,
    size: SIZE,
    from: fromDate.value || undefined,
    to: toDate.value || undefined,
    planId: planId.value ?? undefined,
    actionVersionId: actionVersionId.value ?? undefined
  })
  const list = res.data.records ?? []
  if (page.value === 1) {
    records.value = list
  } else {
    records.value.push(...list)
  }
  total.value = res.data.total ?? 0
  page.value += 1
  if (records.value.length >= total.value || list.length < SIZE) {
    finished.value = true
  }
}

/** 首屏/筛选变化：重置后拉第一页 */
const reload = async (): Promise<void> => {
  loading.value = true
  records.value = []
  page.value = 1
  finished.value = false
  try {
    await fetchPage()
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

/** van-list 滚动到底触发翻页 */
const onLoad = async (): Promise<void> => {
  if (finished.value) {
    loading.value = false
    return
  }
  try {
    await fetchPage()
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

// ===== 展示辅助 =====
const qualityTagType = (quality: string): 'success' | 'warning' | 'danger' | 'default' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'default'> = {
    GOOD: 'success',
    FAIR: 'warning',
    POOR: 'danger',
    INSUFFICIENT: 'default'
  }
  return map[quality] ?? 'default'
}

const openDetail = (record: TrainingRecordItem): void => {
  router.push({ name: 'PatientRecordDetail', params: { recordId: record.recordId } })
}

onMounted(reload)
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="训练记录" />

    <!-- 筛选条 -->
    <div class="filter-bar">
      <van-button size="small" icon="calendar-o" plain type="primary" @click="calendarVisible = true">
        {{ rangeText() }}
      </van-button>
      <van-tag v-if="planId" closeable size="medium" type="primary" @close="clearPlanFilter">
        按计划筛选
      </van-tag>
      <van-tag v-if="actionVersionId" closeable size="medium" type="primary" @close="clearActionFilter">
        按动作版本筛选
      </van-tag>
      <van-button v-if="hasFilter()" size="small" plain @click="clearAllFilters">清除</van-button>
    </div>

    <van-pull-refresh v-model="refreshing" @refresh="reload">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="!loading && records.length === 0" description="暂无训练记录" />

        <div
          v-for="record in records"
          :key="record.recordId"
          class="record-card"
          :class="{ insufficient: record.poseQuality === 'INSUFFICIENT' }"
          @click="openDetail(record)"
        >
          <div class="record-head">
            <span class="action-name">{{ record.actionName || '训练动作' }}</span>
            <van-tag :type="qualityTagType(record.poseQuality)">
              {{ record.poseQuality === 'INSUFFICIENT' ? '数据不足' : record.poseQuality }}
            </van-tag>
          </div>

          <div class="record-meta">
            <span>{{ record.correctReps }} / {{ record.totalReps }} 次</span>
            <span>总分 {{ record.totalScore }}</span>
          </div>

          <div v-if="record.poseQuality === 'INSUFFICIENT'" class="insufficient-tip">
            有效数据不足，本次成绩不计入任务进度
          </div>

          <div class="record-time">
            <span v-if="record.planName">{{ record.planName }}</span>
            <span v-if="record.startedAt">{{ formatDateTime(record.startedAt) }}</span>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 日期范围选择 -->
    <van-calendar
      v-model:show="calendarVisible"
      type="range"
      title="选择训练日期范围"
      :min-date="new Date(2020, 0, 1)"
      :max-date="new Date()"
      :default-date="calendarValue"
      @confirm="onCalendarConfirm"
    />
  </div>
</template>

<style scoped lang="scss">
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
}

.record-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 0 12px 12px;

  &.insufficient {
    background: #fffbe8;
  }

  .record-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .action-name {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .record-meta {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #4b5563;
    margin: 8px 0 4px;
  }

  .insufficient-tip {
    font-size: 12px;
    color: #b45309;
    margin: 4px 0;
  }

  .record-time {
    margin-top: 6px;
    display: flex;
    justify-content: space-between;
    gap: 8px;
    font-size: 12px;
    color: #9ca3af;
  }
}
</style>
