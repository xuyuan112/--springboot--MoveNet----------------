import { del, get, post, put } from './request'
import type { PageResult } from './request'

/** 康复部位分组编码（与数据字典 groupCode 约定一致） */
export type PartGroupCode = 'UPPER' | 'LOWER' | 'SPINE' | 'OVERALL'

/** 康复部位字典项 */
export interface RehabPart {
  /** 主键，新增时可不传 */
  id?: number
  /** 部位编码，业务唯一键，如 KNEE */
  code: string
  /** 部位名称，如 膝关节 */
  name: string
  /** 分组编码：UPPER 上肢 / LOWER 下肢 / SPINE 脊柱 / OVERALL 全身综合 */
  groupCode: PartGroupCode | string
  /** 排序号，升序 */
  sortNo: number
  /** 是否启用 */
  enabled: boolean
}

/** 管理端字典分页查询参数 */
export interface PartPageParams {
  page?: number
  size?: number
  /** 按分组筛选 */
  groupCode?: string
  /** 名称 / 编码关键词 */
  keyword?: string
}

/**
 * 查询全部启用中的康复部位（公共接口，供业务下拉选择）
 * GET /public/rehab-parts
 */
export function getEnabledParts() {
  return get<RehabPart[]>('/public/rehab-parts')
}

/**
 * 管理端 - 分页查询康复部位字典
 * GET /admin/dictionaries/rehab-parts
 */
export function pageParts(params: PartPageParams) {
  return get<PageResult<RehabPart>>('/admin/dictionaries/rehab-parts', params)
}

/**
 * 管理端 - 新增康复部位字典项
 * POST /admin/dictionaries/rehab-parts
 */
export function createPart(data: RehabPart) {
  return post<RehabPart>('/admin/dictionaries/rehab-parts', data)
}

/**
 * 管理端 - 修改康复部位字典项（名称、排序、启用状态等，编码 code 为业务主键不可变）
 * PUT /admin/dictionaries/rehab-parts/{code}
 */
export function updatePart(data: RehabPart) {
  return put<RehabPart>(`/admin/dictionaries/rehab-parts/${data.code}`, data)
}

/**
 * 管理端 - 删除康复部位字典项（仅允许删除未被业务数据引用的项）
 * DELETE /admin/dictionaries/rehab-parts/{code}
 */
export function deletePart(code: string) {
  return del<null>(`/admin/dictionaries/rehab-parts/${code}`)
}
