import { get } from '@/api/request'
import type { PageResult } from '@/api/request'

/** 动作库列表项（仅 PUBLISHED 动作 + PUBLISHED 版本） */
export interface PatientActionItem {
  actionId: number
  actionVersionId: number
  name: string
  description?: string
  targetPartCode?: string
  partName?: string
  difficulty?: string
  difficultyName?: string
  actionType?: string
  versionNo?: number
  updatedAt?: string
}

/** 评估规则（版本快照只读） */
export interface ActionRuleSnapshot {
  actionRuleId: number
  jointName?: string
  keypointIndices?: string
  angleFormula?: string
  jointRole?: string
  dimension?: string
  phase?: string
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

/** 动作版本快照详情（算法参数/运动签名/规则/引擎标识，只读） */
export interface PatientActionDetail extends PatientActionItem {
  status?: string
  algoConfig?: string
  motionSignature?: string
  poseEngineId?: string
  rules?: ActionRuleSnapshot[]
}

/** 分页浏览已发布动作（部位/难度/关键词筛选） */
export function listActions(params: {
  page?: number
  size?: number
  partCode?: string
  difficulty?: string
  keyword?: string
}) {
  return get<PageResult<PatientActionItem>>('/patient/actions', params)
}

/** 查询动作版本快照详情（只读） */
export function getActionDetail(actionVersionId: number) {
  return get<PatientActionDetail>(`/patient/actions/${actionVersionId}`)
}
