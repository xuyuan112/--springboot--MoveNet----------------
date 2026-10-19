import { get } from '@/api/request'
import type { PageResult } from '@/api/request'

/** 治疗师端动作库列表项（仅 PUBLISHED 动作及已发布版本，与患者端 PatientActionVO 口径一致） */
export interface TherapistActionItem {
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

/** 分页浏览已发布动作（partCode/difficulty/keyword 筛选） */
export function listTherapistActions(params: {
  page?: number
  size?: number
  partCode?: string
  difficulty?: string
  keyword?: string
}) {
  return get<PageResult<TherapistActionItem>>('/therapist/actions', params)
}
