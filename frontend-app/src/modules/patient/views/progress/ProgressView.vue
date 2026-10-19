<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { getProgress, getTrends } from '@/api/patient/progress'
import type { ProgressSummary, TaskProgressItem, TrendPoint } from '@/api/patient/progress'
import { formatDateTime } from '@/utils/datetime'

// ===== 三级完成度 =====
const summary = ref<ProgressSummary | null>(null)
const summaryLoading = ref(false)

// ===== 趋势 =====
type Granularity = 'DAY' | 'WEEK'
const granularity = ref<Granularity>('DAY')
/** 快捷范围（天数），0 表示全部 */
const rangeDays = ref(7)
const rangeOptions = [
  { text: '近 7 天', value: 7 },
  { text: '近 30 天', value: 30 },
  { text: '近 90 天', value: 90 },
  { text: '全部', value: 0 }
]
const trend = ref<TrendPoint[]>([])
const trendLoading = ref(false)

// ===== 任务完成明细（增量分页） =====
const records = ref<TaskProgressItem[]>([])
const page = ref(1)
const SIZE = 10
const total = ref(0)
const listLoading = ref(false)
const listFinished = ref(false)
const listError = ref(false)
const refreshing = ref(false)

const fetchSummary = async (): Promise<void> => {
  const res = await getProgress({ page: 1, size: 1 })
  summary.value = res.data.summary
}

const rangeFrom = (): string | undefined => {
  if (rangeDays.value <= 0) return undefined
  const d = new Date()
  d.setDate(d.getDate() - rangeDays.value + 1)
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const today = (): string => {
  const d = new Date()
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const fetchTrends = async (): Promise<void> => {
  const res = await getTrends({
    granularity: granularity.value,
    from: rangeFrom(),
    to: today()
  })
  trend.value = res.data ?? []
  renderChart()
}

const loadSummary = async (): Promise<void> => {
  summaryLoading.value = true
  try {
    await fetchSummary()
  } finally {
    summaryLoading.value = false
  }
}

const loadTrends = async (): Promise<void> => {
  trendLoading.value = true
  try {
    await fetchTrends()
  } finally {
    trendLoading.value = false
  }
}

// ===== ECharts 折线图 =====
const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const renderChart = (): void => {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value)
  }
  if (trend.value.length === 0) {
    chart.clear()
    return
  }
  const hasRate = trend.value.some((p) => p.avgCompletionRate != null)
  chart.setOption({
    grid: { left: 44, right: 44, top: 36, bottom: 28 },
    tooltip: { trigger: 'axis' },
    legend: { top: 0, itemWidth: 14, textStyle: { fontSize: 11 } },
    xAxis: {
      type: 'category',
      data: trend.value.map((p) => p.date.slice(5)),
      boundaryGap: false
    },
    yAxis: [
      { type: 'value', name: '分', min: 0, max: 100, splitLine: { show: true } },
      hasRate
        ? {
            type: 'value',
            name: '%',
            min: 0,
            max: 100,
            axisLabel: { formatter: '{value}' },
            splitLine: { show: false }
          }
        : { show: false }
    ],
    series: [
      {
        name: '平均得分',
        type: 'line',
        smooth: true,
        data: trend.value.map((p) => p.avgScore ?? 0),
        itemStyle: { color: '#1989fa' }
      },
      {
        name: '平均达标率(%)',
        type: 'line',
        smooth: true,
        yAxisIndex: hasRate ? 1 : 0,
        data: trend.value.map((p) =>
          p.avgCompletionRate != null ? Math.round(p.avgCompletionRate * 1000) / 10 : null
        ),
        itemStyle: { color: '#07c160' }
      }
    ]
  })
}

const onResize = (): void => chart?.resize()

// ===== 任务明细分页 =====
const fetchPage = async (): Promise<void> => {
  const res = await getProgress({ page: page.value, size: SIZE })
  const list = res.data.records?.records ?? []
  if (page.value === 1) {
    records.value = list
  } else {
    records.value.push(...list)
  }
  total.value = res.data.records?.total ?? 0
  page.value += 1
  if (records.value.length >= total.value || list.length < SIZE) {
    listFinished.value = true
  }
}

const reload = async (): Promise<void> => {
  listLoading.value = true
  records.value = []
  page.value = 1
  listFinished.value = false
  listError.value = false
  try {
    await fetchPage()
  } catch {
    listError.value = true
  } finally {
    listLoading.value = false
    refreshing.value = false
  }
}

const onLoad = async (): Promise<void> => {
  if (listFinished.value) {
    listLoading.value = false
    return
  }
  try {
    await fetchPage()
  } catch {
    listError.value = true
  } finally {
    listLoading.value = false
  }
}

const statusTagType = (status: string): 'success' | 'warning' | 'primary' | 'default' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'primary' | 'default' | 'danger'> = {
    COMPLETED: 'success',
    IN_PROGRESS: 'primary',
    PENDING: 'warning',
    SKIPPED: 'default',
    EXPIRED: 'danger'
  }
  return map[status] ?? 'default'
}

const rangeText = computed(() => rangeOptions.find((o) => o.value === rangeDays.value)?.text ?? '')

const onRangeChange = (): void => {
  loadTrends()
}

onMounted(async () => {
  window.addEventListener('resize', onResize)
  await Promise.all([loadSummary(), loadTrends()])
  // 图表容器在数据返回后才有内容，再渲染一次保证尺寸正确
  renderChart()
  await reload()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="page-container">
    <!-- 三级完成度 -->
    <van-cell-group inset title="完成度" class="block">
      <div v-if="summaryLoading" class="loading-box"><van-loading vertical>加载中…</van-loading></div>
      <template v-else-if="summary">
        <div class="circle-row">
          <div class="circle-item">
            <van-circle
              :current-rate="summary.day.percent ?? 0"
              :rate="summary.day.percent ?? 0"
              size="88px"
              layer-color="#ebedf0"
              :text="`今日 ${summary.day.percent ?? 0}%`"
            />
            <span class="circle-label">{{ summary.day.doneTasks ?? 0 }}/{{ summary.day.totalTasks ?? 0 }} 项今日任务</span>
          </div>
          <div class="circle-item">
            <van-circle
              :current-rate="summary.plan.percent ?? 0"
              :rate="summary.plan.percent ?? 0"
              size="88px"
              layer-color="#ebedf0"
              color="#07c160"
              :text="`累计 ${summary.plan.percent ?? 0}%`"
            />
            <span class="circle-label">
              {{ summary.plan.completedTasks ?? 0 }}/{{ summary.plan.totalTasks ?? 0 }} 项累计任务
            </span>
          </div>
        </div>

        <div v-if="summary.actionItems?.length" class="action-items">
          <div v-for="item in summary.actionItems" :key="item.planItemId" class="action-item">
            <div class="action-item-head">
              <span class="action-name">{{ item.actionName }}</span>
              <span class="action-percent">{{ item.percent ?? 0 }}%</span>
            </div>
            <van-progress :percentage="item.percent ?? 0" stroke-width="6" />
            <span class="action-sub">
              {{ item.planName }} · {{ item.completedTasks ?? 0 }}/{{ item.totalTasks ?? 0 }} 次任务完成
            </span>
          </div>
        </div>
      </template>
    </van-cell-group>

    <!-- 趋势 -->
    <van-cell-group inset title="训练趋势" class="block">
      <van-tabs v-model:active="granularity" shrink @change="loadTrends">
        <van-tab title="按日" name="DAY" />
        <van-tab title="按周" name="WEEK" />
      </van-tabs>
      <van-dropdown-menu direction="up">
        <van-dropdown-item v-model="rangeDays" :options="rangeOptions" @change="onRangeChange" />
      </van-dropdown-menu>
      <div class="range-hint">{{ rangeText }} · {{ granularity === 'DAY' ? '按自然日' : '按 ISO 周' }}聚合</div>
      <div v-show="trendLoading" class="loading-box"><van-loading vertical>加载中…</van-loading></div>
      <div v-show="!trendLoading && trend.length === 0" class="loading-box">
        <van-empty description="所选范围内暂无有效训练数据" image-size="80" />
      </div>
      <div v-show="!trendLoading && trend.length > 0" ref="chartEl" class="trend-chart" />
    </van-cell-group>

    <!-- 任务完成明细 -->
    <van-cell-group inset title="任务完成明细" class="block">
      <van-list
        v-model:loading="listLoading"
        v-model:error="listError"
        :finished="listFinished"
        error-text="加载失败，点击重试"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <div v-for="task in records" :key="task.taskId" class="task-row">
          <div class="task-head">
            <span class="task-name">{{ task.actionName }}</span>
            <van-tag :type="statusTagType(task.status)">{{ task.statusName }}</van-tag>
          </div>
          <div class="task-meta">
            {{ task.taskDate }} · {{ task.planName }} · 目标 {{ task.targetSets }} 组 × {{ task.targetReps }} 次
          </div>
          <van-progress :percentage="task.progressPercent ?? 0" stroke-width="6" />
          <div class="task-foot">
            <span>达标 {{ task.correctReps ?? 0 }} 次</span>
            <span v-if="task.completedAt">完成于 {{ formatDateTime(task.completedAt) }}</span>
          </div>
        </div>
      </van-list>
      <van-empty v-if="!listLoading && records.length === 0" description="暂无任务数据" image-size="80" />
    </van-cell-group>
  </div>
</template>

<style scoped lang="scss">
.block {
  margin-bottom: 12px;
}

.loading-box {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.circle-row {
  display: flex;
  justify-content: space-around;
  padding: 16px 0 8px;
}

.circle-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.circle-label {
  font-size: 12px;
  color: #969799;
}

.action-items {
  padding: 4px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.action-name {
  font-size: 14px;
  font-weight: 600;
}

.action-percent {
  font-size: 14px;
  color: #1989fa;
  font-weight: 600;
}

.action-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #969799;
}

.range-hint {
  padding: 4px 16px;
  font-size: 12px;
  color: #969799;
}

.trend-chart {
  width: 100%;
  height: 240px;
}

.task-row {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.task-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.task-name {
  font-size: 14px;
  font-weight: 600;
}

.task-meta {
  margin: 2px 0 6px;
  font-size: 12px;
  color: #969799;
}

.task-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 12px;
  color: #969799;
}
</style>
