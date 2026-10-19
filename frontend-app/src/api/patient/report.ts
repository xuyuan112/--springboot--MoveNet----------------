import { get } from '@/api/request'
import type { ApiResult, PageResult } from '@/api/request'

/** 患者评估报告列表项（仅 PUBLISHED/EFFECTIVE 可见） */
export interface ReportItem {
  reportId: number
  planId: number
  planName: string
  planVersion: number
  periodStart: string
  periodEnd: string
  status: string
  statusName: string
  createdAt: string
}

/** 分项评分（各维度 0-100） */
export type ScoreAverages = Partial<{
  amplitude: number
  hold: number
  rhythm: number
  stability: number
  symmetry: number
  similarity: number
}>

/** 患者评估报告详情（只读，含免责声明） */
export interface ReportDetail extends ReportItem {
  totalTasks: number
  completedTasks: number
  completionRate: number
  recordCount: number
  /** 无效数据（INSUFFICIENT）记录数 */
  insufficientCount: number
  totalReps: number
  correctReps: number
  avgTotalScore: number
  scoreAverages?: ScoreAverages
  /** 姿态质量分布（GOOD/FAIR/POOR/INSUFFICIENT -> 条数） */
  qualityDistribution?: Record<string, number>
  /** 纠错维度分布（维度 -> 次数） */
  errorDimensionDistribution?: Record<string, number>
  therapistConclusion?: string
  reviewedAt?: string
  disclaimer: string
}

/**
 * 分页查询本人已生效报告。
 * status 仅支持 EFFECTIVE（对应后端 PUBLISHED），其余值返回 400。
 */
export function listReports(params: { page?: number; size?: number; status?: 'EFFECTIVE' }) {
  return get<PageResult<ReportItem>>('/patient/reports', params)
}

/** 查询报告详情；非 PUBLISHED 或非本人报告返回 404 */
export function getReportDetail(reportId: number) {
  return get<ReportDetail>(`/patient/reports/${reportId}`)
}
