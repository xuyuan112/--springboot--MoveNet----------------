<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import type { Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast } from 'vant'
import {
  GENDER_NAMES,
  RELATION_TYPE_NAMES,
  getTherapistPatientDetail,
  listPatientAlertsOf,
  listPatientPlansOf,
  listPatientTasksOf
} from '@/api/therapist/patient'
import type {
  TherapistAlert,
  TherapistPatientDetail,
  TherapistPatientTask
} from '@/api/therapist/patient'
import type { TherapistPlan } from '@/api/therapist/plan'
import {
  DAILY_TASK_STATUS_NAMES,
  PLAN_STATUS_NAMES,
  dailyTaskStatusTagType,
  planStatusTagType
} from '@/api/therapist/plan'
import { formatDateTime } from '@/utils/datetime'
import type { ApiResult, PageResult } from '@/api/request'

const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 10
const patientId = computed(() => Number(route.params.patientId) || 0)

// ==================== 通用分页列表（van-list 增量加载 + 下拉刷新） ====================
interface PagedList<T> {
  items: Ref<T[]>
  loading: Ref<boolean>
  finished: Ref<boolean>
  refreshing: Ref<boolean>
  onLoad: () => Promise<void>
  reload: () => Promise<void>
}

function usePagedList<T>(
  fetcher: (page: number) => Promise<ApiResult<PageResult<T>>>
): PagedList<T> {
  // shallowRef 保持 T[] 精确类型（避免泛型深层解包类型不兼容），赋值整体替换触发响应
  const items = shallowRef<T[]>([]) as Ref<T[]>
  const page = ref(1)
  const total = ref(0)
  const loading = ref(false)
  const finished = ref(false)
  const refreshing = ref(false)

  const fetchPage = async (): Promise<void> => {
    const res = await fetcher(page.value)
    const list = res.data.records ?? []
    if (page.value === 1) {
      items.value = list
    } else {
      items.value = [...items.value, ...list]
    }
    total.value = res.data.total ?? 0
    page.value += 1
    if (items.value.length >= total.value || list.length < PAGE_SIZE) {
      finished.value = true
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
      finished.value = true
    } finally {
      loading.value = false
    }
  }

  const reload = async (): Promise<void> => {
    loading.value = true
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

  return { items, loading, finished, refreshing, onLoad, reload }
}

// ==================== 患者详情 ====================
/** @type {import('vue').Ref<TherapistPatientDetail | null>} */
const detail = ref<TherapistPatientDetail | null>(null)
const detailLoading = ref(false)

const fetchDetail = async (): Promise<void> => {
  if (!patientId.value) {
    showFailToast('缺少患者 ID')
    return
  }
  detailLoading.value = true
  try {
    const res = await getTherapistPatientDetail(patientId.value)
    detail.value = res.data
  } catch {
    showFailToast('患者详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const genderAgeText = computed<string>(() => {
  const p = detail.value
  const gender = p?.gender ? (GENDER_NAMES[p.gender] ?? p.gender) : '—'
  if (!p?.birthday) return `${gender} / —`
  const birth = new Date(p.birthday)
  if (Number.isNaN(birth.getTime())) return `${gender} / —`
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const monthDiff = now.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
    age -= 1
  }
  return `${gender} / ${age} 岁`
})

const heightWeightText = computed<string>(() => {
  const p = detail.value
  const h = p?.heightCm != null ? `${p.heightCm}cm` : '—'
  const w = p?.weightKg != null ? `${p.weightKg}kg` : '—'
  return `${h} / ${w}`
})

const relationText = computed<string>(() => {
  const type = detail.value?.relationType
  if (!type) return '—'
  return RELATION_TYPE_NAMES[type] ?? type
})

// ==================== 页签一：康复计划 ====================
const activeTab = ref('plans')

const planStatusFilter = ref('')
const planStatusOptions = [
  { text: '全部状态', value: '' },
  ...Object.entries(PLAN_STATUS_NAMES).map(([value, text]) => ({ text, value }))
]

const {
  items: planItems,
  loading: planLoading,
  finished: planFinished,
  refreshing: planRefreshing,
  onLoad: planOnLoad,
  reload: planReload
} = usePagedList<TherapistPlan>((page) =>
  listPatientPlansOf(patientId.value, {
    page,
    size: PAGE_SIZE,
    status: planStatusFilter.value || undefined
  })
)

const planStatusText = (plan: TherapistPlan): string =>
  plan.statusName ?? PLAN_STATUS_NAMES[plan.status] ?? plan.status

const goPlanDetail = (plan: TherapistPlan): void => {
  router.push(`/therapist/plans/${plan.planId}`)
}

/** 新建计划：进入创建模式 */
const goCreatePlan = (): void => {
  router.push(`/therapist/patients/${patientId.value}/plans/new`)
}

// ==================== 页签二：每日任务 ====================
const taskStatusFilter = ref('')
const taskStatusOptions = [
  { text: '全部状态', value: '' },
  ...Object.entries(DAILY_TASK_STATUS_NAMES).map(([value, text]) => ({ text, value }))
]

const taskFromDate = ref('')
const taskToDate = ref('')
const taskCalendarVisible = ref(false)
const taskDefaultRange = ref<Date[] | undefined>(undefined)

const {
  items: taskItems,
  loading: taskLoading,
  finished: taskFinished,
  refreshing: taskRefreshing,
  onLoad: taskOnLoad,
  reload: taskReload
} = usePagedList<TherapistPatientTask>((page) =>
  listPatientTasksOf(patientId.value, {
    page,
    size: PAGE_SIZE,
    from: taskFromDate.value || undefined,
    to: taskToDate.value || undefined,
    status: taskStatusFilter.value || undefined
  })
)

const pad = (n: number): string => String(n).padStart(2, '0')
const fmtDay = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const taskRangeText = (): string => {
  if (taskFromDate.value && taskToDate.value) return `${taskFromDate.value} ~ ${taskToDate.value}`
  if (taskFromDate.value) return `${taskFromDate.value} 起`
  if (taskToDate.value) return `${taskToDate.value} 止`
  return '全部日期'
}

const onTaskCalendarConfirm = (dates: Date[]): void => {
  if (dates && dates.length === 2) {
    taskFromDate.value = fmtDay(dates[0])
    taskToDate.value = fmtDay(dates[1])
  } else {
    taskFromDate.value = ''
    taskToDate.value = ''
  }
  taskCalendarVisible.value = false
  void taskReload()
}

const taskStatusText = (task: TherapistPatientTask): string =>
  task.statusName ?? DAILY_TASK_STATUS_NAMES[task.status] ?? task.status

// ==================== 页签三：异常提示 ====================
const {
  items: alertItems,
  loading: alertLoading,
  finished: alertFinished,
  refreshing: alertRefreshing,
  onLoad: alertOnLoad,
  reload: alertReload
} = usePagedList<TherapistAlert>((page) =>
  listPatientAlertsOf(patientId.value, { page, size: PAGE_SIZE })
)

const alertTagType = (type: string): 'primary' | 'warning' | 'danger' | 'default' => {
  const map: Record<string, 'primary' | 'warning' | 'danger' | 'default'> = {
    LOW_SCORE: 'danger',
    NO_COMPLETION_3D: 'warning',
    PAIN_FEEDBACK: 'primary'
  }
  return map[type] ?? 'default'
}

onMounted(fetchDetail)
</script>

<template>
  <div class="page-container">
    <van-loading v-if="detailLoading" class="page-loading" vertical>加载中…</van-loading>

    <template v-else>
      <!-- 顶部患者信息卡 -->
      <div class="patient-card">
        <div class="patient-head">
          <span class="patient-name">{{ detail?.name || '患者详情' }}</span>
          <van-tag type="primary">{{ relationText }}</van-tag>
          <van-tag v-if="detail?.hasRiskHint" type="danger">风险提示</van-tag>
        </div>
        <van-cell-group :border="false" class="patient-cells">
          <van-cell title="性别/年龄" :value="genderAgeText" />
          <van-cell title="身高/体重" :value="heightWeightText" />
          <van-cell title="手机号" :value="detail?.phone || '—'" />
          <van-cell title="主康复部位" :value="detail?.rehabPartName || '—'" />
          <van-cell title="绑定开始时间" :value="formatDateTime(detail?.bindingStartAt) || '—'" />
        </van-cell-group>
      </div>

      <!-- 病史/过敏/禁忌 -->
      <div class="section-title">病史信息</div>
      <van-cell-group inset>
        <van-cell title="既往病史" :label="detail?.medicalHistory || '—'" />
        <van-cell title="过敏史" :label="detail?.allergyInfo || '—'" />
        <van-cell title="禁忌症" :label="detail?.contraindication || '—'" />
      </van-cell-group>

      <!-- 风险提示 -->
      <div class="section-title">风险提示</div>
      <van-cell-group inset>
        <div class="risk-area">
          <template v-if="detail?.riskHints && detail.riskHints.length > 0">
            <van-tag
              v-for="(hint, index) in detail.riskHints"
              :key="index"
              type="warning"
              size="large"
              class="risk-tag"
            >
              {{ hint }}
            </van-tag>
          </template>
          <span v-else class="risk-empty">暂无风险提示</span>
          <div class="risk-note">以上提示由量化口径自动生成，仅提示，不构成医学诊断。</div>
        </div>
      </van-cell-group>

      <!-- 三个页签 -->
      <van-tabs v-model:active="activeTab" sticky class="detail-tabs">
        <!-- 康复计划 -->
        <van-tab title="康复计划" name="plans">
          <div class="tab-toolbar">
            <van-dropdown-menu class="toolbar-menu" active-color="#2563eb">
              <van-dropdown-item
                v-model="planStatusFilter"
                :options="planStatusOptions"
                @change="planReload"
              />
            </van-dropdown-menu>
            <van-button type="primary" size="small" icon="plus" @click="goCreatePlan">
              新建计划
            </van-button>
          </div>

          <van-pull-refresh v-model="planRefreshing" @refresh="planReload">
            <van-list
              v-model:loading="planLoading"
              :finished="planFinished"
              finished-text="没有更多了"
              @load="planOnLoad"
            >
              <van-empty v-if="planFinished && planItems.length === 0" description="暂无康复计划" />

              <div
                v-for="plan in planItems"
                :key="plan.planId"
                class="plan-card"
                @click="goPlanDetail(plan)"
              >
                <div class="card-head">
                  <span class="card-title">{{ plan.name }}</span>
                  <van-tag :type="planStatusTagType(plan.status)">{{ planStatusText(plan) }}</van-tag>
                </div>
                <van-cell-group :border="false" class="card-cells">
                  <van-cell title="版本" :value="`v${plan.versionNo ?? '—'}`" />
                  <van-cell
                    title="起止日期"
                    :value="`${plan.startDate ?? '—'} ~ ${plan.endDate ?? '—'}`"
                  />
                </van-cell-group>
                <div class="card-more">
                  进入计划
                  <van-icon name="arrow" />
                </div>
              </div>
            </van-list>
          </van-pull-refresh>
        </van-tab>

        <!-- 每日任务 -->
        <van-tab title="每日任务" name="tasks">
          <div class="tab-toolbar">
            <van-button
              size="small"
              icon="calendar-o"
              plain
              type="primary"
              @click="taskCalendarVisible = true"
            >
              {{ taskRangeText() }}
            </van-button>
            <van-dropdown-menu class="toolbar-menu" active-color="#2563eb">
              <van-dropdown-item
                v-model="taskStatusFilter"
                :options="taskStatusOptions"
                @change="taskReload"
              />
            </van-dropdown-menu>
          </div>

          <van-pull-refresh v-model="taskRefreshing" @refresh="taskReload">
            <van-list
              v-model:loading="taskLoading"
              :finished="taskFinished"
              finished-text="没有更多了"
              @load="taskOnLoad"
            >
              <van-empty v-if="taskFinished && taskItems.length === 0" description="暂无每日任务" />

              <div v-for="task in taskItems" :key="task.taskId" class="task-row">
                <div class="card-head">
                  <span class="card-title">{{ task.actionName || '训练动作' }}</span>
                  <van-tag :type="dailyTaskStatusTagType(task.status)">
                    {{ taskStatusText(task) }}
                  </van-tag>
                </div>
                <div class="row-sub">{{ task.taskDate }} · {{ task.planName || '—' }}</div>
                <div class="row-meta">
                  目标：{{ task.targetSets ?? '—' }} 组 × {{ task.targetReps ?? '—' }} 次
                </div>
              </div>
            </van-list>
          </van-pull-refresh>
        </van-tab>

        <!-- 异常提示 -->
        <van-tab title="异常提示" name="alerts">
          <van-pull-refresh v-model="alertRefreshing" @refresh="alertReload">
            <van-list
              v-model:loading="alertLoading"
              :finished="alertFinished"
              finished-text="没有更多了"
              @load="alertOnLoad"
            >
              <van-empty
                v-if="alertFinished && alertItems.length === 0"
                description="近 7 日暂无异常提示"
              />

              <div v-for="(alert, index) in alertItems" :key="index" class="alert-row">
                <div class="card-head">
                  <van-tag :type="alertTagType(alert.alertType)">
                    {{ alert.alertTypeName || alert.alertType }}
                  </van-tag>
                  <span class="row-date">{{ alert.alertDate }}</span>
                </div>
                <div class="alert-content">{{ alert.content }}</div>
              </div>
            </van-list>
          </van-pull-refresh>
          <div class="alert-footnote">异常提示由量化口径自动生成，仅提示，不构成医学诊断。</div>
        </van-tab>
      </van-tabs>

      <!-- 任务日期范围选择 -->
      <van-calendar
        v-model:show="taskCalendarVisible"
        type="range"
        title="选择任务日期范围"
        :min-date="new Date(2020, 0, 1)"
        :max-date="new Date()"
        :default-date="taskDefaultRange"
        @confirm="onTaskCalendarConfirm"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.patient-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 12px;

  .patient-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;

    .patient-name {
      font-size: 17px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .patient-cells {
    :deep(.van-cell) {
      padding: 4px 0;
    }
  }
}

.section-title {
  margin: 16px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
}

.risk-area {
  padding: 12px;

  .risk-tag {
    margin: 0 8px 8px 0;
  }

  .risk-empty {
    font-size: 13px;
    color: #9ca3af;
  }

  .risk-note {
    margin-top: 4px;
    font-size: 12px;
    color: #9ca3af;
  }
}

.detail-tabs {
  margin-top: 12px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px 0;

  .toolbar-menu {
    flex: 1;
    min-width: 0;
  }
}

.plan-card,
.task-row,
.alert-row {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 12px;

  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
    }

    .row-date {
      font-size: 12px;
      color: #9ca3af;
    }
  }

  .card-cells {
    :deep(.van-cell) {
      padding: 4px 0;
    }
  }

  .row-sub {
    margin-top: 4px;
    font-size: 12px;
    color: #6b7280;
  }

  .row-meta {
    margin-top: 4px;
    font-size: 13px;
    color: #4b5563;
  }

  .alert-content {
    margin-top: 8px;
    font-size: 13px;
    color: #4b5563;
    line-height: 1.5;
  }
}

.plan-card {
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

.alert-footnote {
  padding: 0 16px 16px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}
</style>
