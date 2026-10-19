<script setup lang="ts">
import { computed, ref } from 'vue'
import { showFailToast } from 'vant'
import { getMyTasks } from '@/api/patient/task'
import type { PatientDailyTask } from '@/api/patient/task'
import { formatDateTime } from '@/utils/datetime'

const tasks = ref<PatientDailyTask[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const statusFilter = ref('')

/** 日期范围（yyyy-MM-dd），通过 van-calendar 区间选择 */
const dateFrom = ref('')
const dateTo = ref('')
const calendarVisible = ref(false)

/** van-list 加载状态 */
const listLoading = ref(false)
const listFinished = ref(false)
const refreshing = ref(false)

const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '待训练', value: 'PENDING' },
  { text: '进行中', value: 'IN_PROGRESS' },
  { text: '已完成', value: 'COMPLETED' },
  { text: '部分完成', value: 'PARTIAL' },
  { text: '跳过', value: 'SKIPPED' },
  { text: '已过期', value: 'EXPIRED' }
]

/** 状态标签颜色（Vant tag type） */
const statusTagType = (status: string): 'success' | 'warning' | 'danger' | 'default' | 'primary' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'default' | 'primary'> = {
    PENDING: 'default',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
    PARTIAL: 'warning',
    SKIPPED: 'default',
    EXPIRED: 'danger'
  }
  return map[status] ?? 'default'
}

/** 日期范围展示文本 */
const dateRangeText = computed(() => {
  if (dateFrom.value && dateTo.value) {
    return `${dateFrom.value} ~ ${dateTo.value}`
  }
  return '选择日期范围'
})

const toYmd = (d: Date): string => {
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** 日历区间确认 */
const onCalendarConfirm = (range: Date[]): void => {
  dateFrom.value = toYmd(range[0])
  dateTo.value = toYmd(range[1])
  calendarVisible.value = false
  resetAndReload()
}

/** van-list 触底加载一页 */
const onLoad = async (): Promise<void> => {
  try {
    const res = await getMyTasks({
      page: page.value,
      size,
      from: dateFrom.value || undefined,
      to: dateTo.value || undefined,
      status: statusFilter.value || undefined
    })
    if (refreshing.value) {
      tasks.value = []
      refreshing.value = false
    }
    tasks.value.push(...res.data.records)
    total.value = res.data.total
    if (tasks.value.length >= total.value || res.data.records.length === 0) {
      listFinished.value = true
    } else {
      page.value += 1
    }
  } catch {
    showFailToast('任务列表加载失败')
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

/** 下拉刷新 / 条件切换：回到第一页重新加载 */
const resetAndReload = (): void => {
  page.value = 1
  listFinished.value = false
  listLoading.value = true
  onLoad()
}
</script>

<template>
  <div class="page-container">
    <van-dropdown-menu active-color="#2563eb">
      <van-dropdown-item :title="dateRangeText">
        <div class="date-filter">
          <van-cell is-link title="日期范围" :value="dateRangeText" @click="calendarVisible = true" />
          <van-button
            v-if="dateFrom"
            size="small"
            plain
            type="primary"
            class="clear-btn"
            @click="((dateFrom = ''), (dateTo = ''), resetAndReload())"
          >
            清除日期
          </van-button>
        </div>
      </van-dropdown-item>
      <van-dropdown-item v-model="statusFilter" :options="statusOptions" @change="resetAndReload" />
    </van-dropdown-menu>

    <van-calendar
      v-model:show="calendarVisible"
      type="range"
      :max-date="new Date()"
      @confirm="onCalendarConfirm"
    />

    <van-pull-refresh v-model="refreshing" @refresh="resetAndReload">
      <van-list
        v-model:loading="listLoading"
        :finished="listFinished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="listFinished && tasks.length === 0" description="暂无任务记录" />

        <div v-for="task in tasks" :key="task.taskId" class="task-card">
          <div class="task-head">
            <span class="task-date">{{ task.taskDate }}</span>
            <van-tag :type="statusTagType(task.status)">{{ task.statusName || task.status }}</van-tag>
          </div>
          <div class="task-line">{{ task.planName }} · {{ task.actionName || '训练动作' }}</div>
          <div class="task-line sub">目标：{{ task.targetSets }} 组 × {{ task.targetReps }} 次</div>
          <van-progress :percentage="Number(task.progressPercent)" stroke-width="6" class="task-progress" />
          <div v-if="task.completedAt" class="task-time">完成：{{ formatDateTime(task.completedAt) }}</div>
        </div>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped lang="scss">
.date-filter {
  padding: 8px 16px 16px;

  .clear-btn {
    margin-top: 8px;
  }
}

.task-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .task-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .task-date {
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .task-line {
    margin-top: 4px;
    font-size: 13px;
    color: #4b5563;

    &.sub {
      color: #6b7280;
    }
  }

  .task-progress {
    margin-top: 8px;
  }

  .task-time {
    margin-top: 6px;
    font-size: 12px;
    color: #9ca3af;
  }
}
</style>
