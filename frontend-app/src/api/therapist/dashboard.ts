import { get } from '@/api/request'

/** 治疗师工作台聚合数据（GET /therapist/dashboard，与后端 TherapistDashboardVO 对齐） */
export interface TherapistDashboard {
  /** 负责患者数（ACTIVE 绑定去重） */
  patientCount: number
  /** 今日任务总数 */
  todayTaskTotal: number
  /** 今日已完成任务数 */
  todayTaskCompleted: number
  /** 今日任务完成率（0-100，保留一位小数；无任务为 null） */
  todayCompletionRate: number | null
  /** 待审核报告数（未生效报告） */
  pendingReportCount: number
  /** 待回复咨询数（存在患者未读消息的 OPEN 会话） */
  pendingConsultCount: number
  /** 今日低分训练数（total_score<60，仅提示） */
  lowScoreTodayCount: number
}

/** 查询治疗师工作台聚合指标 */
export function getTherapistDashboard() {
  return get<TherapistDashboard>('/therapist/dashboard')
}
