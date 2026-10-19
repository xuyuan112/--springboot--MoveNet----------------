<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  showConfirmDialog,
  showFailToast,
  showSuccessToast,
  showToast
} from 'vant'
import {
  FREQUENCY_TYPE_NAMES,
  PLAN_STATUS_NAMES,
  addPlanItem,
  archivePlan,
  createPlan,
  createPlanVersion,
  deleteDraftPlan,
  deletePlanItem,
  getPlanDetail,
  pausePlan,
  publishPlan,
  resumePlan,
  updatePlan,
  updatePlanItem,
  updatePlanSchedule,
  planStatusTagType
} from '@/api/therapist/plan'
import type {
  FrequencyTypeValue,
  PlanItemPayload,
  PlanSchedulePayload,
  TherapistPlanDetail,
  TherapistPlanItem
} from '@/api/therapist/plan'
import { listTherapistActions } from '@/api/therapist/action'
import type { TherapistActionItem } from '@/api/therapist/action'
import { getEnabledRehabParts } from '@/api/common/dict'
import type { RehabPartDictVO } from '@/api/common/dict'

const route = useRoute()
const router = useRouter()

/** 创建模式路由：/therapist/patients/:patientId/plans/new（planId 参数为 'new'） */
const isCreate = computed(() => route.name === 'TherapistPlanCreate')
const patientId = computed(() => Number(route.params.patientId) || 0)
const planId = computed(() => Number(route.params.planId) || 0)

const MIN_DATE = new Date(2020, 0, 1)
const MAX_DATE = new Date(2030, 11, 31)

/** @type {import('vue').Ref<TherapistPlanDetail | null>} */
const plan = ref<TherapistPlanDetail | null>(null)
const loading = ref(false)
const status = computed(() => plan.value?.status ?? 'DRAFT')
/** 创建模式与 DRAFT 状态下基本信息/动作项可编辑 */
const editable = computed(() => isCreate.value || status.value === 'DRAFT')

// ==================== 基本信息 ====================
const baseForm = reactive({
  name: '',
  rehabPartCode: '',
  startDate: '',
  endDate: '',
  remark: ''
})

/** @type {import('vue').Ref<RehabPartDictVO[]>} */
const rehabParts = ref<RehabPartDictVO[]>([])
const partPickerVisible = ref(false)
const partColumns = computed(() => rehabParts.value.map((p) => ({ text: p.name, value: p.code })))
const rehabPartName = computed(() => {
  const hit = rehabParts.value.find((p) => p.code === baseForm.rehabPartCode)
  return hit?.name ?? baseForm.rehabPartCode
})

const onPartConfirm = (params: { selectedValues: Array<string | number> }): void => {
  baseForm.rehabPartCode = String(params.selectedValues[0] ?? '')
  partPickerVisible.value = false
}

const dateRangeVisible = ref(false)
const pad = (n: number): string => String(n).padStart(2, '0')
const fmtDay = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const dateRangeText = computed(() => {
  if (baseForm.startDate && baseForm.endDate) {
    return `${baseForm.startDate} ~ ${baseForm.endDate}`
  }
  return '请选择'
})

const defaultRange = computed<Date[] | undefined>(() => {
  if (baseForm.startDate && baseForm.endDate) {
    return [new Date(baseForm.startDate), new Date(baseForm.endDate)]
  }
  return undefined
})

const onDateRangeConfirm = (dates: Date[]): void => {
  if (dates && dates.length === 2) {
    baseForm.startDate = fmtDay(dates[0])
    baseForm.endDate = fmtDay(dates[1])
  }
  dateRangeVisible.value = false
}

// ==================== 动作项（创建模式本地维护 / DRAFT 调接口） ====================
/** 创建模式下的本地动作项（随 PlanCreateRequest.items 内嵌提交） */
interface LocalPlanItem {
  actionVersionId: number
  actionName: string
  versionNo?: number
  targetSets: number
  targetReps: number
  targetAmplitude?: number
  durationSec?: number
  restSec?: number
}

/** @type {import('vue').Ref<LocalPlanItem[]>} */
const localItems = ref<LocalPlanItem[]>([])

/** 列表渲染统一视图（本地项与持久化项共用） */
interface DisplayItem {
  key: string
  planItemId?: number
  localIndex?: number
  sequenceNo?: number
  actionVersionId: number
  actionName?: string
  versionNo?: number
  targetSets?: number
  targetReps?: number
  targetAmplitude?: number
  durationSec?: number
  restSec?: number
}

const displayItems = computed<DisplayItem[]>(() => {
  if (isCreate.value) {
    return localItems.value.map((it, idx) => ({
      key: `local-${idx}`,
      localIndex: idx,
      actionVersionId: it.actionVersionId,
      actionName: it.actionName,
      versionNo: it.versionNo,
      targetSets: it.targetSets,
      targetReps: it.targetReps,
      targetAmplitude: it.targetAmplitude,
      durationSec: it.durationSec,
      restSec: it.restSec
    }))
  }
  return (plan.value?.items ?? []).map((it) => ({
    key: `item-${it.planItemId}`,
    planItemId: it.planItemId,
    sequenceNo: it.sequenceNo,
    actionVersionId: it.actionVersionId,
    actionName: it.actionName,
    versionNo: it.versionNo,
    targetSets: it.targetSets,
    targetReps: it.targetReps,
    targetAmplitude: it.targetAmplitude,
    durationSec: it.durationSec,
    restSec: it.restSec
  }))
})

// ===== 动作选择弹层（GET /therapist/actions 分页搜索） =====
const ACTION_PAGE_SIZE = 20
const actionPickerVisible = ref(false)
const actionKeyword = ref('')
/** @type {import('vue').Ref<TherapistActionItem[]>} */
const actions = ref<TherapistActionItem[]>([])
const actionPage = ref(1)
const actionTotal = ref(0)
const actionLoading = ref(false)
const actionFinished = ref(false)

const fetchActionPage = async (): Promise<void> => {
  const res = await listTherapistActions({
    page: actionPage.value,
    size: ACTION_PAGE_SIZE,
    keyword: actionKeyword.value.trim() || undefined
  })
  const list = res.data.records ?? []
  if (actionPage.value === 1) {
    actions.value = list
  } else {
    actions.value.push(...list)
  }
  actionTotal.value = res.data.total ?? 0
  actionPage.value += 1
  if (actions.value.length >= actionTotal.value || list.length < ACTION_PAGE_SIZE) {
    actionFinished.value = true
  }
}

const onActionLoad = async (): Promise<void> => {
  if (actionFinished.value) {
    actionLoading.value = false
    return
  }
  try {
    await fetchActionPage()
  } catch {
    actionFinished.value = true
  } finally {
    actionLoading.value = false
  }
}

const reloadActions = async (): Promise<void> => {
  actionLoading.value = true
  actionPage.value = 1
  actionFinished.value = false
  try {
    await fetchActionPage()
  } catch {
    actionFinished.value = true
  } finally {
    actionLoading.value = false
  }
}

const openActionPicker = (): void => {
  actionPickerVisible.value = true
  if (actions.value.length === 0) {
    void reloadActions()
  }
}

// ===== 动作项参数弹窗 =====
const itemDialogVisible = ref(false)
const itemSubmitting = ref(false)
/** 当前编辑目标：null 表示新增 */
const editingTarget = ref<DisplayItem | null>(null)

const itemForm = reactive({
  actionVersionId: 0,
  actionName: '',
  versionNo: undefined as number | undefined,
  targetSets: '3',
  targetReps: '10',
  targetAmplitude: '',
  durationSec: '',
  restSec: '30'
})

const toPositiveInt = (v: string): number | null => {
  if (!v) return null
  const n = Number(v)
  if (!Number.isFinite(n) || n < 1) return null
  return Math.round(n)
}

const toNonNegativeInt = (v: string): number | undefined => {
  if (!v) return undefined
  const n = Number(v)
  if (!Number.isFinite(n) || n < 0) return undefined
  return Math.round(n)
}

const toNumber = (v: string): number | undefined => {
  if (!v) return undefined
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
}

const resetItemForm = (action: TherapistActionItem): void => {
  itemForm.actionVersionId = action.actionVersionId
  itemForm.actionName = action.name
  itemForm.versionNo = action.versionNo
  itemForm.targetSets = '3'
  itemForm.targetReps = '10'
  itemForm.targetAmplitude = ''
  itemForm.durationSec = ''
  itemForm.restSec = '30'
}

const openAddItem = (): void => {
  editingTarget.value = null
  openActionPicker()
}

const openEditItem = (it: DisplayItem): void => {
  editingTarget.value = it
  itemForm.actionVersionId = it.actionVersionId
  itemForm.actionName = it.actionName ?? ''
  itemForm.versionNo = it.versionNo
  itemForm.targetSets = it.targetSets != null ? String(it.targetSets) : '3'
  itemForm.targetReps = it.targetReps != null ? String(it.targetReps) : '10'
  itemForm.targetAmplitude = it.targetAmplitude != null ? String(it.targetAmplitude) : ''
  itemForm.durationSec = it.durationSec != null ? String(it.durationSec) : ''
  itemForm.restSec = it.restSec != null ? String(it.restSec) : '30'
  itemDialogVisible.value = true
}

/** 弹层中选中动作：弹窗未打开时先打开参数弹窗，否则回填动作 */
const handleActionSelect = (action: TherapistActionItem): void => {
  actionPickerVisible.value = false
  if (itemDialogVisible.value) {
    itemForm.actionVersionId = action.actionVersionId
    itemForm.actionName = action.name
    itemForm.versionNo = action.versionNo
    return
  }
  resetItemForm(action)
  itemDialogVisible.value = true
}

const handleItemConfirm = async (): Promise<void> => {
  if (!itemForm.actionVersionId) {
    showToast('请先选择动作')
    return
  }
  const sets = toPositiveInt(itemForm.targetSets)
  if (sets == null) {
    showToast('组数至少为 1')
    return
  }
  const reps = toPositiveInt(itemForm.targetReps)
  if (reps == null) {
    showToast('目标次数至少为 1')
    return
  }
  const amplitude = toNumber(itemForm.targetAmplitude)
  const durationSec = toNonNegativeInt(itemForm.durationSec)
  const restSec = toNonNegativeInt(itemForm.restSec)

  const target = editingTarget.value

  // 创建模式：本地维护，随创建请求内嵌提交
  if (isCreate.value || target?.localIndex != null) {
    const localItem: LocalPlanItem = {
      actionVersionId: itemForm.actionVersionId,
      actionName: itemForm.actionName,
      versionNo: itemForm.versionNo,
      targetSets: sets,
      targetReps: reps,
      targetAmplitude: amplitude,
      durationSec,
      restSec
    }
    if (target?.localIndex != null) {
      localItems.value.splice(target.localIndex, 1, localItem)
      showSuccessToast('动作项已更新')
    } else {
      localItems.value.push(localItem)
      showSuccessToast('动作项已添加')
    }
    itemDialogVisible.value = false
    return
  }

  // DRAFT：调用接口持久化
  itemSubmitting.value = true
  try {
    const payload: PlanItemPayload = {
      actionVersionId: itemForm.actionVersionId,
      sequenceNo: target?.sequenceNo ?? (plan.value?.items?.length ?? 0) + 1,
      targetSets: sets,
      targetReps: reps,
      targetAmplitude: amplitude,
      durationSec,
      restSec
    }
    if (target?.planItemId != null) {
      await updatePlanItem(planId.value, target.planItemId, payload)
      showSuccessToast('动作项已更新')
    } else {
      await addPlanItem(planId.value, payload)
      showSuccessToast('动作项已添加')
    }
    itemDialogVisible.value = false
    await fetchDetail()
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    itemSubmitting.value = false
  }
}

const handleDeleteItem = async (it: DisplayItem): Promise<void> => {
  try {
    await showConfirmDialog({
      title: '删除动作项',
      message: `确定删除动作「${it.actionName || '未命名'}」吗？`
    })
  } catch {
    return
  }
  if (it.localIndex != null) {
    localItems.value.splice(it.localIndex, 1)
    showSuccessToast('动作项已删除')
    return
  }
  if (it.planItemId != null) {
    try {
      await deletePlanItem(planId.value, it.planItemId)
      showSuccessToast('动作项已删除')
      await fetchDetail()
    } catch {
      // 错误提示由响应拦截器统一处理
    }
  }
}

// ==================== 执行日程（仅 DRAFT 可编辑） ====================
const WEEKDAY_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]
const WEEKDAY_MAP: Record<number, string> = Object.fromEntries(
  WEEKDAY_OPTIONS.map((d) => [d.value, d.label])
)

const scheduleFrequencyType = ref<string>('DAILY')
/** @type {import('vue').Ref<number[]>} */
const scheduleWeekdays = ref<number[]>([])
const scheduleSaving = ref(false)

const frequencyText = computed(() => {
  if (!plan.value) return '—'
  const type = FREQUENCY_TYPE_NAMES[plan.value.frequencyType ?? ''] ?? plan.value.frequencyType ?? '—'
  if (plan.value.frequencyValue != null && plan.value.frequencyValue > 0) {
    return `${type} (${plan.value.frequencyValue} 次)`
  }
  return type
})

const weekdaysText = computed(() => {
  const ws = plan.value?.weekdays ?? []
  if (ws.length === 0) return ''
  return [...ws]
    .sort((a, b) => a - b)
    .map((w) => WEEKDAY_MAP[w] ?? String(w))
    .join('、')
})

const handleSaveSchedule = async (): Promise<void> => {
  if (!planId.value) return
  if (scheduleFrequencyType.value === 'WEEKLY' && scheduleWeekdays.value.length === 0) {
    showToast('请至少选择一个执行星期')
    return
  }
  scheduleSaving.value = true
  try {
    const type: FrequencyTypeValue =
      scheduleFrequencyType.value === 'WEEKLY' ? 'WEEKLY' : 'DAILY'
    const payload: PlanSchedulePayload =
      type === 'WEEKLY'
        ? { frequencyType: type, frequencyValue: scheduleWeekdays.value.length, weekdays: [...scheduleWeekdays.value] }
        : { frequencyType: type, frequencyValue: 1 }
    const res = await updatePlanSchedule(planId.value, payload)
    plan.value = res.data
    showSuccessToast('执行日程已保存')
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    scheduleSaving.value = false
  }
}

// ==================== 详情加载 ====================
const syncFormFromDetail = (data: TherapistPlanDetail): void => {
  baseForm.name = data.name ?? ''
  baseForm.rehabPartCode = data.rehabPartCode ?? ''
  baseForm.startDate = data.startDate ?? ''
  baseForm.endDate = data.endDate ?? ''
  baseForm.remark = data.remark ?? ''
  scheduleFrequencyType.value = data.frequencyType === 'WEEKLY' ? 'WEEKLY' : 'DAILY'
  scheduleWeekdays.value = [...(data.weekdays ?? [])]
}

const fetchDetail = async (): Promise<void> => {
  if (isCreate.value) return
  if (!planId.value) {
    showFailToast('缺少计划 ID')
    return
  }
  loading.value = true
  try {
    const res = await getPlanDetail(planId.value)
    plan.value = res.data
    syncFormFromDetail(res.data)
  } catch {
    showFailToast('计划详情加载失败')
  } finally {
    loading.value = false
  }
}

// 路由在 创建→编辑 / 版本间跳转 时组件被复用，监听 planId 变化重新拉取
watch(
  () => route.params.planId,
  () => {
    void fetchDetail()
  },
  { immediate: true }
)

const loadDict = async (): Promise<void> => {
  try {
    const res = await getEnabledRehabParts()
    rehabParts.value = res.data ?? []
  } catch {
    // 错误提示由响应拦截器统一处理
  }
}

onMounted(loadDict)

// ==================== 状态机操作 ====================
const saving = ref(false)
const handleSaveBase = async (): Promise<void> => {
  if (!baseForm.name.trim()) {
    showToast('请填写计划名称')
    return
  }
  if (!baseForm.startDate || !baseForm.endDate) {
    showToast('请选择起止日期')
    return
  }
  saving.value = true
  try {
    if (isCreate.value) {
      if (localItems.value.length === 0) {
        showToast('请至少添加一个动作项')
        return
      }
      const res = await createPlan(patientId.value, {
        name: baseForm.name.trim(),
        rehabPartCode: baseForm.rehabPartCode || undefined,
        startDate: baseForm.startDate,
        endDate: baseForm.endDate,
        remark: baseForm.remark.trim() || undefined,
        items: localItems.value.map((it) => ({
          actionVersionId: it.actionVersionId,
          sets: it.targetSets,
          targetReps: it.targetReps,
          targetAngle: it.targetAmplitude,
          restSec: it.restSec
        }))
      })
      showSuccessToast('计划已创建')
      router.replace(`/therapist/plans/${res.data.planId}`)
      return
    }
    const res = await updatePlan(planId.value, {
      name: baseForm.name.trim(),
      rehabPartCode: baseForm.rehabPartCode || undefined,
      startDate: baseForm.startDate,
      endDate: baseForm.endDate,
      remark: baseForm.remark.trim() || undefined
    })
    plan.value = res.data
    syncFormFromDetail(res.data)
    showSuccessToast('基本信息已保存')
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    saving.value = false
  }
}

const publishing = ref(false)
const handlePublish = async (): Promise<void> => {
  try {
    await showConfirmDialog({
      title: '发布计划',
      message: '发布后患者端可见，并将开始生成每日训练任务，确定发布吗？'
    })
  } catch {
    return
  }
  publishing.value = true
  try {
    const res = await publishPlan(planId.value)
    plan.value = res.data
    showSuccessToast('计划已发布')
    // 发布后计划 ID 不变，仅同步状态；保留跳转以对齐「发布后进入计划页」口径
    if (res.data.planId !== planId.value) {
      router.replace(`/therapist/plans/${res.data.planId}`)
    }
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    publishing.value = false
  }
}

const deleting = ref(false)
const handleDeleteDraft = async (): Promise<void> => {
  try {
    await showConfirmDialog({
      title: '删除草稿',
      message: '删除后不可恢复，确定删除该草稿计划吗？'
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteDraftPlan(planId.value)
    showSuccessToast('草稿已删除')
    router.back()
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    deleting.value = false
  }
}

// ===== 暂停/归档原因弹窗 =====
const reasonDialogVisible = ref(false)
const reasonAction = ref<'pause' | 'archive'>('pause')
const reasonText = ref('')
const reasonSubmitting = ref(false)

const openReasonDialog = (action: 'pause' | 'archive'): void => {
  reasonAction.value = action
  reasonText.value = ''
  reasonDialogVisible.value = true
}

const handleReasonSubmit = async (): Promise<void> => {
  if (!reasonText.value.trim()) {
    showToast('请填写原因')
    return
  }
  reasonSubmitting.value = true
  try {
    if (reasonAction.value === 'pause') {
      await pausePlan(planId.value, reasonText.value.trim())
      showSuccessToast('计划已暂停')
    } else {
      await archivePlan(planId.value, reasonText.value.trim())
      showSuccessToast('计划已归档')
    }
    reasonDialogVisible.value = false
    await fetchDetail()
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    reasonSubmitting.value = false
  }
}

const resuming = ref(false)
const handleResume = async (): Promise<void> => {
  resuming.value = true
  try {
    await resumePlan(planId.value)
    showSuccessToast('计划已恢复')
    await fetchDetail()
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    resuming.value = false
  }
}

const adjusting = ref(false)
const handleNewVersion = async (): Promise<void> => {
  try {
    await showConfirmDialog({
      title: '调整计划',
      message: '将复制当前计划创建新的草稿版本，确定继续吗？'
    })
  } catch {
    return
  }
  adjusting.value = true
  try {
    const res = await createPlanVersion(planId.value)
    showSuccessToast('新版本已创建')
    router.replace(`/therapist/plans/${res.data.planId}`)
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    adjusting.value = false
  }
}

const goVersions = (): void => {
  router.push(`/therapist/plans/${planId.value}/versions`)
}

const statusText = computed(() => {
  if (!plan.value) return '草稿'
  return plan.value.statusName ?? PLAN_STATUS_NAMES[plan.value.status] ?? plan.value.status
})
</script>

<template>
  <div class="page-container">
    <van-loading v-if="loading" class="page-loading" vertical>加载中…</van-loading>

    <template v-else>
      <div class="plan-header">
        <span class="plan-title">{{ isCreate ? '新建康复计划' : plan?.name || '康复计划' }}</span>
        <van-tag :type="planStatusTagType(plan?.status ?? 'DRAFT')">{{ statusText }}</van-tag>
      </div>

      <!-- 基本信息 -->
      <div class="section-title">基本信息</div>
      <van-cell-group inset>
        <van-field
          v-if="editable"
          v-model="baseForm.name"
          label="计划名称"
          placeholder="请输入计划名称（必填）"
          maxlength="100"
        />
        <van-cell v-else title="计划名称" :value="plan?.name || '—'" />

        <van-cell
          v-if="editable"
          title="康复部位"
          is-link
          :value="rehabPartName || '请选择'"
          @click="partPickerVisible = true"
        />
        <van-cell v-else title="康复部位" :value="plan?.rehabPartName || plan?.rehabPartCode || '—'" />

        <van-cell
          v-if="editable"
          title="起止日期"
          is-link
          :value="dateRangeText"
          @click="dateRangeVisible = true"
        />
        <van-cell v-else title="起止日期" :value="dateRangeText" />

        <van-field
          v-if="editable"
          v-model="baseForm.remark"
          label="注意事项"
          type="textarea"
          rows="2"
          autosize
          maxlength="500"
          show-word-limit
          placeholder="选填，如：出现疼痛立即停止"
        />
        <van-cell v-else title="注意事项" :label="plan?.remark || '—'" />
      </van-cell-group>

      <!-- 动作项 -->
      <div class="section-title">动作项（{{ displayItems.length }}）</div>
      <van-empty v-if="displayItems.length === 0" description="暂无动作项，请添加" />

      <div v-for="it in displayItems" :key="it.key" class="item-card">
        <div class="item-head">
          <span class="item-seq">{{ it.sequenceNo ?? '—' }}</span>
          <span class="item-name">{{ it.actionName || '训练动作' }}</span>
          <span class="item-version">v{{ it.versionNo ?? '—' }}</span>
        </div>
        <van-cell-group :border="false" class="item-cells">
          <van-cell title="组数 × 次数" :value="`${it.targetSets ?? '—'} × ${it.targetReps ?? '—'}`" />
          <van-cell
            title="目标角度"
            :value="it.targetAmplitude != null ? `${it.targetAmplitude}°` : '—'"
          />
          <van-cell title="单组时长" :value="it.durationSec != null ? `${it.durationSec} 秒` : '—'" />
          <van-cell title="组间休息" :value="it.restSec != null ? `${it.restSec} 秒` : '—'" />
        </van-cell-group>
        <div v-if="editable" class="item-actions">
          <van-button size="small" plain type="primary" @click="openEditItem(it)">编辑</van-button>
          <van-button size="small" plain type="danger" @click="handleDeleteItem(it)">删除</van-button>
        </div>
      </div>

      <van-button
        v-if="editable"
        block
        plain
        type="primary"
        icon="plus"
        class="add-item-btn"
        @click="openAddItem"
      >
        添加动作项
      </van-button>

      <!-- 执行日程 -->
      <div class="section-title">执行日程</div>
      <van-cell-group inset>
        <template v-if="isCreate">
          <van-cell title="频次类型" value="创建计划后可设置" />
        </template>
        <template v-else-if="status === 'DRAFT'">
          <div class="schedule-editor">
            <div class="schedule-label">频次类型</div>
            <van-radio-group v-model="scheduleFrequencyType" direction="horizontal">
              <van-radio name="DAILY">每日</van-radio>
              <van-radio name="WEEKLY">每周</van-radio>
            </van-radio-group>
            <template v-if="scheduleFrequencyType === 'WEEKLY'">
              <div class="schedule-label">执行星期（可多选）</div>
              <van-checkbox-group v-model="scheduleWeekdays" direction="horizontal">
                <van-checkbox v-for="d in WEEKDAY_OPTIONS" :key="d.value" :name="d.value">
                  {{ d.label }}
                </van-checkbox>
              </van-checkbox-group>
            </template>
            <van-button
              size="small"
              type="primary"
              class="schedule-save"
              :loading="scheduleSaving"
              @click="handleSaveSchedule"
            >
              保存日程
            </van-button>
          </div>
        </template>
        <template v-else>
          <van-cell title="频次" :value="frequencyText" />
          <van-cell v-if="weekdaysText" title="执行星期" :value="weekdaysText" />
        </template>
      </van-cell-group>

      <!-- 底部操作按钮（按状态显隐） -->
      <div class="action-bar">
        <template v-if="isCreate || status === 'DRAFT'">
          <van-button block type="primary" :loading="saving" @click="handleSaveBase">
            {{ isCreate ? '保存并创建计划' : '保存基本信息' }}
          </van-button>
        </template>
        <template v-if="!isCreate && status === 'DRAFT'">
          <van-button block type="success" :loading="publishing" @click="handlePublish">
            发布
          </van-button>
          <van-button block plain type="danger" :loading="deleting" @click="handleDeleteDraft">
            删除草稿
          </van-button>
        </template>
        <template v-else-if="!isCreate && status === 'PUBLISHED'">
          <van-button block type="warning" @click="openReasonDialog('pause')">暂停</van-button>
          <van-button block plain type="danger" @click="openReasonDialog('archive')">归档</van-button>
          <van-button block plain type="primary" :loading="adjusting" @click="handleNewVersion">
            调整计划（新版本）
          </van-button>
          <van-button block plain @click="goVersions">查看版本</van-button>
        </template>
        <template v-else-if="!isCreate && status === 'PAUSED'">
          <van-button block type="primary" :loading="resuming" @click="handleResume">恢复</van-button>
          <van-button block plain type="danger" @click="openReasonDialog('archive')">归档</van-button>
          <van-button block plain @click="goVersions">查看版本</van-button>
        </template>
        <template v-else-if="!isCreate && (status === 'COMPLETED' || status === 'ARCHIVED')">
          <van-button block plain @click="goVersions">查看版本</van-button>
        </template>
      </div>
    </template>

    <!-- 康复部位选择 -->
    <van-popup v-model:show="partPickerVisible" position="bottom" round>
      <van-picker
        :columns="partColumns"
        title="选择康复部位"
        @confirm="onPartConfirm"
        @cancel="partPickerVisible = false"
      />
    </van-popup>

    <!-- 计划起止日期选择 -->
    <van-calendar
      v-model:show="dateRangeVisible"
      type="range"
      title="选择计划起止日期"
      :min-date="MIN_DATE"
      :max-date="MAX_DATE"
      :default-date="defaultRange"
      @confirm="onDateRangeConfirm"
    />

    <!-- 动作选择弹层（分页搜索） -->
    <van-popup v-model:show="actionPickerVisible" position="bottom" round :style="{ height: '70%' }">
      <div class="action-picker">
        <van-search
          v-model="actionKeyword"
          placeholder="搜索动作名称"
          @search="reloadActions"
          @clear="reloadActions"
        />
        <div class="action-list">
          <van-list
            v-model:loading="actionLoading"
            :finished="actionFinished"
            finished-text="没有更多了"
            @load="onActionLoad"
          >
            <van-empty
              v-if="actionFinished && actions.length === 0"
              description="未找到可选动作"
            />
            <div
              v-for="a in actions"
              :key="a.actionVersionId"
              class="action-row"
              @click="handleActionSelect(a)"
            >
              <div class="action-name">
                {{ a.name }}
                <span class="action-ver">v{{ a.versionNo ?? '—' }}</span>
              </div>
              <div class="action-meta">
                <span v-if="a.partName">{{ a.partName }}</span>
                <span v-if="a.difficultyName">{{ a.difficultyName }}</span>
                <van-tag v-if="a.actionType" plain type="primary">{{ a.actionType }}</van-tag>
              </div>
              <div v-if="a.description" class="action-desc">{{ a.description }}</div>
            </div>
          </van-list>
        </div>
      </div>
    </van-popup>

    <!-- 动作项参数弹窗 -->
    <van-dialog
      v-model:show="itemDialogVisible"
      :title="editingTarget ? '编辑动作项' : '添加动作项'"
      show-cancel-button
      :confirm-button-loading="itemSubmitting"
      @confirm="handleItemConfirm"
    >
      <div class="item-form">
        <van-cell
          title="动作"
          is-link
          :value="itemForm.actionName || '请选择动作'"
          @click="openActionPicker"
        />
        <van-field v-model="itemForm.targetSets" type="number" label="组数" placeholder="至少 1" />
        <van-field v-model="itemForm.targetReps" type="number" label="次数/组" placeholder="至少 1" />
        <van-field
          v-model="itemForm.targetAmplitude"
          type="number"
          label="目标角度(°)"
          placeholder="选填"
        />
        <van-field
          v-model="itemForm.durationSec"
          type="number"
          label="单组时长(秒)"
          placeholder="选填"
        />
        <van-field v-model="itemForm.restSec" type="number" label="组间休息(秒)" placeholder="选填" />
      </div>
    </van-dialog>

    <!-- 暂停/归档原因弹窗 -->
    <van-dialog
      v-model:show="reasonDialogVisible"
      :title="reasonAction === 'pause' ? '暂停计划' : '归档计划'"
      show-cancel-button
      :confirm-button-loading="reasonSubmitting"
      @confirm="handleReasonSubmit"
    >
      <van-field
        v-model="reasonText"
        type="textarea"
        rows="3"
        maxlength="200"
        show-word-limit
        placeholder="请填写原因（必填，最长 200 字）"
      />
    </van-dialog>
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
  padding: 4px 16px 12px;

  .plan-title {
    font-size: 17px;
    font-weight: 600;
    color: #1f2937;
  }
}

.section-title {
  margin: 16px 16px 8px;
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
}

.item-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 0 16px 12px;

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

  .item-actions {
    margin-top: 8px;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

.add-item-btn {
  margin: 0 16px 4px;
  width: calc(100% - 32px);
}

.schedule-editor {
  padding: 12px;

  .schedule-label {
    margin-bottom: 8px;
    font-size: 13px;
    color: #6b7280;
  }

  .schedule-save {
    margin-top: 12px;
  }
}

.action-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}

.action-picker {
  display: flex;
  flex-direction: column;
  height: 100%;

  .action-list {
    flex: 1;
    overflow-y: auto;
  }

  .action-row {
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;

    .action-name {
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;

      .action-ver {
        margin-left: 6px;
        font-size: 12px;
        font-weight: 400;
        color: #9ca3af;
      }
    }

    .action-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-top: 4px;
      font-size: 12px;
      color: #6b7280;
    }

    .action-desc {
      margin-top: 4px;
      font-size: 12px;
      color: #9ca3af;
      line-height: 1.5;
    }
  }
}

.item-form {
  max-height: 50vh;
  overflow-y: auto;
}
</style>
