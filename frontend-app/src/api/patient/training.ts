import { del, get, post } from '@/api/request'
import type { ApiResult, PageResult } from '@/api/request'

/** 六维评分明细（各维度 0~100） */
export interface ScoreDetailMap {
  amplitude: number
  hold: number
  rhythm: number
  stability: number
  symmetry: number
  similarity: number
}

/** 纠错摘要明细项（按"规则×连续区间"聚合，对应 training_error_log 一行） */
export interface ErrorLogItem {
  ruleId?: number
  dimension: string
  phase: string
  jointName?: string
  deviation?: number
  severity: 1 | 2
  frameNo?: number
  promptText: string
  /** 相对训练开始时间的偏移秒数 */
  offsetSec?: number
}

/** 训练汇总提交载荷（POST /patient/training-records） */
export interface TrainingSubmitPayload {
  clientRequestId: string
  dailyTaskId: number
  planItemId: number
  actionVersionId: number
  /** 本地时间 ISO 字符串（不含时区后缀），如 2026-10-07T10:00:00 */
  startedAt: string
  endedAt: string
  totalReps: number
  correctReps: number
  totalScore: number
  scoreDetail: ScoreDetailMap
  poseQuality: 'GOOD' | 'FAIR' | 'POOR' | 'INSUFFICIENT'
  validFrameRatio: number
  poseRepairedRatio?: number
  avgRom?: number
  peakAngle?: number
  troughAngle?: number
  peakFrameNo?: number
  troughFrameNo?: number
  holdRatio?: number
  templateSimilarity?: number
  stabilityScore?: number
  symmetryScore?: number
  errorSummary?: ErrorLogItem[]
  poseEngineId: string
}

/** 提交响应：记录ID + 任务新状态 + 是否重复提交 */
export interface TrainingRecordSubmitResult {
  trainingRecordId: number
  dailyTaskId: number
  taskStatus: string
  targetReps: number
  correctReps: number
  progressPercent: number
  duplicate: boolean
  insufficient: boolean
}

/** 训练记录列表项 */
export interface TrainingRecordItem {
  recordId: number
  dailyTaskId: number
  planId?: number
  planName?: string
  planItemId?: number
  actionVersionId: number
  actionName?: string
  startedAt?: string
  endedAt?: string
  totalReps: number
  correctReps: number
  totalScore: number
  poseQuality: string
  validFrameRatio?: number
  poseRepairedRatio?: number
  createdAt?: string
}

/** 纠错日志（详情页） */
export interface TrainingErrorLog {
  errorId: number
  ruleId?: number
  dimension: string
  phase: string
  errorType?: string
  jointName?: string
  deviation?: number
  severity: number
  frameNo?: number
  promptText: string
  errorAt?: string
}

/** 纠错维度分布项 */
export interface ErrorGroup {
  dimension: string
  count: number
}

/** 训练记录详情 */
export interface TrainingRecordDetail extends TrainingRecordItem {
  scoreDetail?: Partial<ScoreDetailMap>
  avgRom?: number
  peakAngle?: number
  troughAngle?: number
  peakFrameNo?: number
  troughFrameNo?: number
  holdRatio?: number
  templateSimilarity?: number
  stabilityScore?: number
  symmetryScore?: number
  poseEngineId?: string
  remark?: string
  errorLogs?: TrainingErrorLog[]
  errorSummary?: ErrorGroup[]
}

/**
 * 提交训练汇总。
 * 断网/超时由调用方捕获后调用 enqueuePendingSubmission 缓存，
 * 恢复后经 flushPendingSubmissions 以原 clientRequestId 重试（幂等）。
 */
export function submitTrainingRecord(payload: TrainingSubmitPayload) {
  return post<TrainingRecordSubmitResult>('/patient/training-records', payload)
}

/** 分页查询本人训练记录（按日期/计划/动作版本筛选） */
export function listTrainingRecords(params: {
  page?: number
  size?: number
  from?: string
  to?: string
  planId?: number
  actionVersionId?: number
}) {
  return get<PageResult<TrainingRecordItem>>('/patient/training-records', params)
}

/** 查询单次训练详情 */
export function getTrainingRecordDetail(recordId: number) {
  return get<TrainingRecordDetail>(`/patient/training-records/${recordId}`)
}

/** 撤回训练记录（软删除，原因必填；已纳入报告返回 409） */
export function withdrawTrainingRecord(recordId: number, reason: string) {
  return del<void>(`/patient/training-records/${recordId}`, undefined, { reason })
}

// ==================== 断网重试队列（P3-F9 / P4 衔接） ====================

const PENDING_KEY = 'rehab_pending_training_submissions'

interface PendingSubmission {
  payload: TrainingSubmitPayload
  savedAt: number
}

function readPending(): PendingSubmission[] {
  try {
    const raw = localStorage.getItem(PENDING_KEY)
    return raw ? (JSON.parse(raw) as PendingSubmission[]) : []
  } catch {
    return []
  }
}

function writePending(list: PendingSubmission[]): void {
  localStorage.setItem(PENDING_KEY, JSON.stringify(list))
}

/** 断网时缓存提交载荷（含原 clientRequestId），恢复后原键重试保证幂等 */
export function enqueuePendingSubmission(payload: TrainingSubmitPayload): void {
  const list = readPending()
  list.push({ payload, savedAt: Date.now() })
  writePending(list)
}

export function hasPendingSubmissions(): boolean {
  return readPending().length > 0
}

/**
 * 冲刷待重试的提交队列：逐条原键重试，成功即移除，失败保留下次再试。
 * 静默执行（不抛错），适合在训练页/今日任务页挂载时调用。
 */
export async function flushPendingSubmissions(): Promise<number> {
  const list = readPending()
  if (list.length === 0) return 0
  const remaining: PendingSubmission[] = []
  let flushed = 0
  for (const item of list) {
    try {
      await submitTrainingRecord(item.payload)
      flushed++
    } catch {
      remaining.push(item)
    }
  }
  writePending(remaining)
  return flushed
}

/** 生成本地时间 ISO 字符串（不含时区后缀，匹配后端 LocalDateTime 反序列化） */
export function localIsoTime(date: Date = new Date()): string {
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
