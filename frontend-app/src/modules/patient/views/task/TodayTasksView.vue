<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { finishTask, getTodayTasks, pauseTask } from '@/api/patient/task'
import type { PatientDailyTask } from '@/api/patient/task'
import { formatDateTime } from '@/utils/datetime'

const router = useRouter()

const loading = ref(false)
const tasks = ref<PatientDailyTask[]>([])

/** 暂停/结束原因弹窗 */
const reasonDialogVisible = ref(false)
const reasonAction = ref<'pause' | 'finish'>('pause')
const reasonText = ref('')
const reasonTaskId = ref<number | null>(null)
const reasonSaving = ref(false)

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

const fetchToday = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getTodayTasks()
    tasks.value = res.data
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 开始训练：调用 start 拉取动作参数快照并进入全屏姿态训练页 */
const handleStart = (task: PatientDailyTask): void => {
  router.push({ name: 'PatientTraining', params: { taskId: task.taskId } })
}

const openReasonDialog = (task: PatientDailyTask, action: 'pause' | 'finish'): void => {
  reasonTaskId.value = task.taskId
  reasonAction.value = action
  reasonText.value = ''
  reasonDialogVisible.value = true
}

const handleReasonSubmit = async (): Promise<void> => {
  if (!reasonText.value.trim()) {
    showToast('请填写原因')
    return
  }
  if (reasonTaskId.value === null) {
    return
  }
  reasonSaving.value = true
  try {
    if (reasonAction.value === 'pause') {
      await pauseTask(reasonTaskId.value, reasonText.value.trim())
      showSuccessToast('已暂停并记录原因')
    } else {
      await finishTask(reasonTaskId.value, reasonText.value.trim())
      showSuccessToast('任务已结束')
    }
    reasonDialogVisible.value = false
    await fetchToday()
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    reasonSaving.value = false
  }
}

onMounted(fetchToday)
</script>

<template>
  <div class="page-container">
    <van-pull-refresh v-model="loading" @refresh="fetchToday">
      <van-empty v-if="tasks.length === 0" description="今日暂无训练任务" />

      <div v-for="task in tasks" :key="task.taskId" class="task-card">
        <div class="task-head">
          <span class="action-name">{{ task.actionName || '训练动作' }}</span>
          <van-tag :type="statusTagType(task.status)">{{ task.statusName || task.status }}</van-tag>
        </div>
        <div class="plan-name">{{ task.planName }}</div>

        <div class="task-meta">
          <span>目标：{{ task.targetSets }} 组 × {{ task.targetReps }} 次</span>
          <span>已达标：{{ task.correctReps }} / {{ task.targetReps }} 次</span>
        </div>
        <van-progress :percentage="Number(task.progressPercent)" stroke-width="8" />

        <div v-if="task.skipReason" class="skip-reason">跳过原因：{{ task.skipReason }}</div>
        <div class="task-time">
          <span v-if="task.startedAt">开始：{{ formatDateTime(task.startedAt) }}</span>
          <span v-if="task.completedAt">完成：{{ formatDateTime(task.completedAt) }}</span>
        </div>

        <div class="task-actions">
          <van-button
            v-if="task.status === 'PENDING' || task.status === 'IN_PROGRESS'"
            type="primary"
            size="small"
            block
            @click="handleStart(task)"
          >
            {{ task.status === 'PENDING' ? '开始训练' : '继续训练' }}
          </van-button>
          <van-button
            v-if="task.status === 'IN_PROGRESS'"
            size="small"
            block
            @click="openReasonDialog(task, 'pause')"
          >
            暂停
          </van-button>
          <van-button
            v-if="task.status === 'PENDING' || task.status === 'IN_PROGRESS'"
            type="danger"
            plain
            size="small"
            block
            @click="openReasonDialog(task, 'finish')"
          >
            结束/跳过
          </van-button>
        </div>
      </div>
    </van-pull-refresh>

    <!-- 暂停/结束原因弹窗 -->
    <van-dialog
      v-model:show="reasonDialogVisible"
      :title="reasonAction === 'pause' ? '暂停训练' : '结束/跳过任务'"
      show-cancel-button
      :confirm-button-loading="reasonSaving"
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

    .action-name {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .plan-name {
    margin-top: 2px;
    font-size: 12px;
    color: #6b7280;
  }

  .task-meta {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #4b5563;
    margin: 8px 0;
  }

  .skip-reason {
    margin-top: 8px;
    font-size: 12px;
    color: #b91c1c;
  }

  .task-time {
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 12px;
    color: #9ca3af;
  }

  .task-actions {
    margin-top: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
