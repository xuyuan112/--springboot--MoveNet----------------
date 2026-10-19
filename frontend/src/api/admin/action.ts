import { get, post, put, del } from '../request'
import type { PageResult } from '../request'

/** 动作难度 */
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

/** 动作状态 */
export type ActionStatus = 'DRAFT' | 'PUBLISHED' | 'DISABLED'

/** 版本状态 */
export type VersionStatus = 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'

/** 动作类型 */
export type ActionType = 'ROM' | 'STRENGTH' | 'BALANCE'

/** 模板来源类型 */
export type TemplateSource = 'THERAPIST' | 'ALGO'

/** 模板状态 */
export type TemplateStatus = 'ACTIVE' | 'DEPRECATED'

/** 关节角色 */
export type JointRole = 'PRIMARY' | 'AUXILIARY'

/** 评估维度 */
export type AssessmentDimension = 'ANGLE' | 'STABILITY' | 'SYMMETRY' | 'HOLD' | 'SPEED'

/** 动作阶段 */
export type ActionPhase = 'START' | 'CONCENTRIC' | 'ECCENTRIC' | 'HOLD' | 'FULL'

/** 动作分页查询参数 */
export interface ActionPageParams {
  page?: number
  size?: number
  targetPart?: string
  status?: ActionStatus
  keyword?: string
}

/** 动作列表项 */
export interface AdminAction {
  actionId: number
  name: string
  targetPartCode: string
  targetPartName: string
  difficulty: Difficulty | string
  difficultyName: string
  status: ActionStatus | string
  statusName: string
  createdAt?: string
}

/** 动作详情（含版本列表） */
export interface AdminActionDetail extends AdminAction {
  description?: string
  createdBy?: number
  updatedAt?: string
  versionList?: AdminActionVersion[]
}

/** 版本分页查询参数 */
export interface ActionVersionPageParams {
  page?: number
  size?: number
  actionId?: number
  status?: VersionStatus
}

/** 动作版本列表项 */
export interface AdminActionVersion {
  versionId: number
  actionId: number
  versionNo: number
  actionType: ActionType | string
  actionTypeName: string
  poseEngineId?: string
  status: VersionStatus | string
  statusName: string
  createdAt?: string
}

/** 动作版本详情（含规则与模板列表） */
export interface AdminActionVersionDetail extends AdminActionVersion {
  algoConfig?: string
  motionSignature?: string
  createdBy?: number
  updatedAt?: string
  ruleList?: AdminActionRule[]
  templateList?: AdminActionTemplate[]
}

/** 动作规则项 */
export interface AdminActionRule {
  ruleId: number
  jointName: string
  keypointIndices?: string
  angleFormula?: string
  jointRole: JointRole | string
  dimension: AssessmentDimension | string
  phase: ActionPhase | string
  minAngle?: number
  maxAngle?: number
  standardAngle?: number
  toleranceSoft?: number
  toleranceHard?: number
  holdFramesRequired?: number
  speedMin?: number
  speedMax?: number
  scoreWeight: number
  severity?: string
  promptText?: string
  symmetryPairRuleId?: number
  createdAt?: string
  updatedAt?: string
}

/** 动作模板项 */
export interface AdminActionTemplate {
  templateId: number
  actionVersionId: number
  sourceType: TemplateSource | string
  sourceTypeName: string
  frameRate?: number
  frameCount?: number
  featureDim?: number
  /** 标准特征序列 JSON（仅详情/预览使用，列表可能不回传） */
  featureSequence?: string
  /** 关键帧索引 JSON */
  keyframeIndex?: string
  sampleCount?: number
  status: TemplateStatus | string
  statusName: string
  createdAt?: string
}

/** 创建/修改动作请求 */
export interface ActionSavePayload {
  name: string
  targetPartCode: string
  difficulty: Difficulty
  description?: string
}

/** 创建/修改版本请求 */
export interface ActionVersionSavePayload {
  actionType: ActionType
  poseEngineId?: string
  algoConfig?: string
  motionSignature?: string
}

/** 创建/修改规则请求 */
export interface ActionRulePayload {
  jointName: string
  keypointIndices?: string
  angleFormula?: string
  jointRole: JointRole
  dimension: AssessmentDimension
  phase: ActionPhase
  minAngle?: number
  maxAngle?: number
  standardAngle?: number
  toleranceSoft?: number
  toleranceHard?: number
  holdFramesRequired?: number
  speedMin?: number
  speedMax?: number
  scoreWeight: number
  severity?: string
  promptText?: string
  symmetryPairRuleId?: number
}

/** 创建模板请求 */
export interface ActionTemplateSavePayload {
  sourceType: TemplateSource
  frameRate?: number
  frameCount?: number
  featureDim?: number
  featureSequence?: string
  keyframeIndex?: string
  sampleCount?: number
}

// ==================== 动作管理 ====================

/**
 * 管理端 - 分页查询动作
 * GET /admin/actions
 */
export function pageActions(params: ActionPageParams) {
  return get<PageResult<AdminAction>>('/admin/actions', params)
}

/**
 * 管理端 - 创建动作
 * POST /admin/actions
 */
export function createAction(data: ActionSavePayload) {
  return post<number>('/admin/actions', data)
}

/**
 * 管理端 - 查询动作详情及版本列表
 * GET /admin/actions/{actionId}
 */
export function getActionDetail(actionId: number) {
  return get<AdminActionDetail>(`/admin/actions/${actionId}`)
}

/**
 * 管理端 - 修改动作基础信息（仅草稿可编辑）
 * PUT /admin/actions/{actionId}
 */
export function updateAction(actionId: number, data: ActionSavePayload) {
  return put<null>(`/admin/actions/${actionId}`, data)
}

/**
 * 管理端 - 动作下线
 * POST /admin/actions/{actionId}/offline
 */
export function offlineAction(actionId: number) {
  return post<null>(`/admin/actions/${actionId}/offline`)
}

// ==================== 版本管理 ====================

/**
 * 管理端 - 分页查询动作版本
 * GET /admin/action-versions
 */
export function pageActionVersions(params: ActionVersionPageParams) {
  return get<PageResult<AdminActionVersion>>('/admin/action-versions', params)
}

/**
 * 管理端 - 创建动作版本
 * POST /admin/actions/{actionId}/versions
 */
export function createActionVersion(actionId: number, data: ActionVersionSavePayload) {
  return post<number>(`/admin/actions/${actionId}/versions`, data)
}

/**
 * 管理端 - 查询版本详情（含规则与模板列表）
 * GET /admin/action-versions/{versionId}
 */
export function getActionVersionDetail(versionId: number) {
  return get<AdminActionVersionDetail>(`/admin/action-versions/${versionId}`)
}

/**
 * 管理端 - 修改版本（仅草稿可编辑）
 * PUT /admin/action-versions/{versionId}
 */
export function updateActionVersion(versionId: number, data: ActionVersionSavePayload) {
  return put<null>(`/admin/action-versions/${versionId}`, data)
}

/**
 * 管理端 - 发布版本（校验权重和=1）
 * POST /admin/action-versions/{versionId}/publish
 */
export function publishActionVersion(versionId: number) {
  return post<null>(`/admin/action-versions/${versionId}/publish`)
}

// ==================== 规则管理 ====================

/**
 * 管理端 - 查询指定版本下的规则列表
 * GET /admin/action-versions/{versionId}/rules
 */
export function listActionRules(versionId: number) {
  return get<AdminActionRule[]>(`/admin/action-versions/${versionId}/rules`)
}

/**
 * 管理端 - 创建规则
 * POST /admin/action-versions/{versionId}/rules
 */
export function createActionRule(versionId: number, data: ActionRulePayload) {
  return post<number>(`/admin/action-versions/${versionId}/rules`, data)
}

/**
 * 管理端 - 修改规则
 * PUT /admin/action-versions/{versionId}/rules/{ruleId}
 */
export function updateActionRule(versionId: number, ruleId: number, data: ActionRulePayload) {
  return put<null>(`/admin/action-versions/${versionId}/rules/${ruleId}`, data)
}

/**
 * 管理端 - 删除规则
 * DELETE /admin/action-versions/{versionId}/rules/{ruleId}
 */
export function deleteActionRule(versionId: number, ruleId: number) {
  return del<null>(`/admin/action-versions/${versionId}/rules/${ruleId}`)
}

// ==================== 模板管理 ====================

/**
 * 管理端 - 分页查询指定版本下的模板
 * GET /admin/action-versions/{versionId}/templates
 */
export function pageActionTemplates(versionId: number, params: { page?: number; size?: number }) {
  return get<PageResult<AdminActionTemplate>>(`/admin/action-versions/${versionId}/templates`, params)
}

/**
 * 管理端 - 创建模板
 * POST /admin/action-versions/{versionId}/templates
 */
export function createActionTemplate(versionId: number, data: ActionTemplateSavePayload) {
  return post<number>(`/admin/action-versions/${versionId}/templates`, data)
}

/**
 * 管理端 - 发布模板
 * POST /admin/templates/{templateId}/publish
 */
export function publishActionTemplate(templateId: number) {
  return post<null>(`/admin/templates/${templateId}/publish`)
}

/**
 * 管理端 - 删除模板
 * DELETE /admin/templates/{templateId}
 */
export function deleteActionTemplate(templateId: number) {
  return del<null>(`/admin/templates/${templateId}`)
}
