import { get, post } from '@/api/request'
import type { ApiResult, PageResult } from '@/api/request'

/** 患者端每日训练任务 */
export interface PatientDailyTask {
  taskId: number
  planItemId: number
  planId?: number
  planName?: string
  actionName?: string
  taskDate: string
  targetSets: number
  targetReps: number
  correctReps: number
  status: string
  statusName?: string
  progressPercent: number
  skipReason?: string
  startedAt?: string
  completedAt?: string
}

/** 动作评估规则 */
export interface ActionRule {
  actionRuleId: number
  jointName: string
  keypointIndices: string
  angleFormula: string
  jointRole: string
  dimension: string
  phase: string
  minAngle?: number
  maxAngle?: number
  standardAngle?: number
  toleranceSoft?: number
  toleranceHard?: number
  holdFramesRequired?: number
  speedMin?: number
  speedMax?: number
  scoreWeight?: number
  severity?: number
  promptText?: string
  symmetryPairRuleId?: number
}

/** 标准动作模板 */
export interface ActionTemplate {
  templateId: number
  sourceType: string
  frameRate?: number
  frameCount?: number
  featureDim?: number
  featureSequence?: string
  keyframeIndex?: string
  sampleCount?: number
  status: string
}

/** 开始训练时返回的动作版本参数快照 */
export interface TaskActionSnapshot {
  taskId: number
  planItemId: number
  actionVersionId: number
  actionName?: string
  targetSets: number
  targetReps: number
  targetAmplitude?: number
  durationSec?: number
  restSec?: number
  instruction?: string
  algoConfig?: string
  motionSignature?: string
  poseEngineId?: string
  rules: ActionRule[]
  template?: ActionTemplate
}

/** 查询今日任务聚合列表 */
export function getTodayTasks() {
  return get<PatientDailyTask[]>('/patient/tasks/today')
}

/** 按日期、状态分页查询每日任务 */
export function getMyTasks(params: {
  page?: number
  size?: number
  from?: string
  to?: string
  status?: string
}) {
  return get<PageResult<PatientDailyTask>>('/patient/tasks', params)
}

/** 查询单条任务详情 */
export function getTaskDetail(taskId: number) {
  return get<PatientDailyTask>(`/patient/tasks/${taskId}`)
}

/**
 * 开始训练，返回动作版本参数快照。
 * 必须上送端侧本地姿态引擎标识（X-Pose-Engine-Id）：
 * 与动作版本固化的 pose_engine_id 不一致时后端返回 422 ENGINE_VERSION_MISMATCH。
 */
export function startTask(taskId: number, poseEngineId: string) {
  return post<TaskActionSnapshot>(
    `/patient/tasks/${taskId}/start`,
    undefined,
    { 'X-Pose-Engine-Id': poseEngineId }
  )
}

/** 暂停训练并记录原因 */
export function pauseTask(taskId: number, reason: string) {
  return post<void>(`/patient/tasks/${taskId}/pause`, { reason })
}

/** 主动结束/跳过任务，必须提交原因 */
export function finishTask(taskId: number, reason: string) {
  return post<void>(`/patient/tasks/${taskId}/finish`, { reason })
}
