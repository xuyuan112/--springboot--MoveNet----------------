import { get } from '../request'
import type { ApiResult } from '../request'

/** 康复部位字典项（与后端 RehabPartDictVO 对齐） */
export interface RehabPartDictVO {
  code: string
  name: string
  groupCode: 'UPPER' | 'LOWER' | 'SPINE' | 'OVERALL'
  sortNo: number
  enabled: boolean
}

/** GET /public/rehab-parts：查询启用中的康复部位字典（匿名可访问） */
export function getEnabledRehabParts(): Promise<ApiResult<RehabPartDictVO[]>> {
  return get<RehabPartDictVO[]>('/public/rehab-parts')
}
