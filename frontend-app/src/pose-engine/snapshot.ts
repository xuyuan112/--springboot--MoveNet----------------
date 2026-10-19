/**
 * 动作版本参数快照解析（P3-B1/B2 下发、P3-F10 全程不变）。
 *
 * start 接口一次性下发：algo_config + motion_signature + 规则 + 模板特征序列。
 * 本模块仅负责宽容解析（字段缺失/JSON 异常时回退默认值），不做业务校验。
 */
import type { AlgoConfig, MotionSignature } from './types'
import { DEFAULT_ALGO_CONFIG } from './types'

/** 规则（与 /patient/tasks/{id}/start 响应中的 rules 元素对应，数值字段宽容转 number） */
export interface ParsedRule {
  actionRuleId?: number
  jointName: string
  keypointIndices: string
  angleFormula: string
  jointRole: string // PRIMARY / AUXILIARY
  dimension: string // ANGLE / STABILITY / SYMMETRY / HOLD / SPEED
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

export interface ParsedTemplate {
  templateId: number
  frameRate: number
  frameCount: number
  featureDim: number
  /** 标准特征序列：帧 × 维度（解析后统一为 number[][]） */
  featureSequence: number[][] | null
  keyframeIndex: number[]
  sampleCount: number
  status: string
}

export interface ParsedSnapshot {
  taskId: number
  planItemId: number
  actionVersionId: number
  actionName: string
  targetSets: number
  targetReps: number
  targetAmplitude?: number
  durationSec?: number
  restSec?: number
  instruction?: string
  algoConfig: AlgoConfig
  /** 快照中携带的引擎标识（需与端侧清单比对） */
  snapshotEngineId: string
  signature: MotionSignature | null
  rules: ParsedRule[]
  template: ParsedTemplate | null
}

function num(v: unknown): number | undefined {
  if (v === null || v === undefined || v === '') return undefined
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
}

function safeJson(raw: unknown): any | null {
  if (raw == null) return null
  if (typeof raw === 'object') return raw
  if (typeof raw !== 'string') return null
  const s = raw.trim()
  if (!s || s === '{}') return null
  try {
    return JSON.parse(s)
  } catch {
    return null
  }
}

/** 合并算法参数：缺失字段回退 DEFAULT_ALGO_CONFIG，并对越界值做收敛 */
export function parseAlgoConfig(raw: unknown): AlgoConfig {
  const p = safeJson(raw) ?? {}
  const merged: AlgoConfig = { ...DEFAULT_ALGO_CONFIG, ...p }
  // 数值字段防御性收敛（NaN/非法回退默认）
  const guards: Array<[keyof AlgoConfig, number, number]> = [
    ['confHigh', 0, 1],
    ['confLow', 0, 1],
    ['medianWindow', 1, 11],
    ['emaAlpha', 0.01, 1],
    ['maxGapFrames', 0, 30],
    ['boneLenTolerance', 0.01, 1],
    ['repairRatioLimit', 0.01, 1],
    ['stateHoldFrames', 1, 30],
    ['hysteresisBand', 0, 45],
    ['minCycleSec', 0.1, 30],
    ['smoothWindowSpeed', 1, 61],
    ['smoothWindowAccel', 1, 61],
    ['timeWeightSigma', 0.01, 5],
    ['validFrameRatioMin', 0.1, 1],
    ['weightAmplitude', 0, 1],
    ['weightHold', 0, 1],
    ['weightRhythm', 0, 1],
    ['weightStability', 0, 1],
    ['weightSymmetry', 0, 1],
    ['weightSimilarity', 0, 1]
  ]
  for (const [key, min, max] of guards) {
    const v = merged[key]
    const writable = merged as unknown as Record<string, number>
    if (typeof v !== 'number' || !Number.isFinite(v)) {
      writable[key] = DEFAULT_ALGO_CONFIG[key] as number
    } else {
      writable[key] = Math.min(max, Math.max(min, v))
    }
  }
  if (merged.medianWindow % 2 === 0) merged.medianWindow += 1 // 中值窗口取奇数
  return merged
}

export function parseMotionSignature(raw: unknown): MotionSignature | null {
  const p = safeJson(raw)
  if (!p) return null
  const toRange = (v: unknown): [number | null, number | null] | undefined => {
    if (!Array.isArray(v) || v.length !== 2) return undefined
    return [v[0] == null ? null : Number(v[0]) || 0, v[1] == null ? null : Number(v[1]) || 0]
  }
  const sig: MotionSignature = {
    targetJoints: Array.isArray(p.targetJoints) ? p.targetJoints.map(String) : [],
    constraintJoints: Array.isArray(p.constraintJoints) ? p.constraintJoints.map(String) : undefined,
    startAngleRange: toRange(p.startAngleRange),
    peakAngleRange: toRange(p.peakAngleRange),
    minRom: num(p.minRom),
    expectedDirection:
      p.expectedDirection === 'FLEX' || p.expectedDirection === 'EXTEND' || p.expectedDirection === 'MIXED'
        ? p.expectedDirection
        : undefined,
    phaseOrder: Array.isArray(p.phaseOrder) ? p.phaseOrder.map(String) : undefined,
    durationRangeSec: toRange(p.durationRangeSec)
  }
  if (sig.targetJoints.length === 0 && sig.minRom === undefined && !sig.peakAngleRange) return null
  return sig
}

function parseTemplate(raw: any): ParsedTemplate | null {
  if (!raw) return null
  let seq: number[][] | null = null
  const fs = safeJson(raw.featureSequence)
  if (Array.isArray(fs)) {
    if (fs.length > 0 && Array.isArray(fs[0])) {
      seq = fs.map((row: unknown[]) => row.map((v) => Number(v) || 0))
    } else if (fs.length > 0 && typeof fs[0] === 'number') {
      seq = [(fs as number[]).slice()]
    }
  }
  let keyframes: number[] = []
  const ki = safeJson(raw.keyframeIndex)
  if (Array.isArray(ki)) keyframes = ki.map((v) => Number(v) || 0).filter(Number.isFinite)

  return {
    templateId: Number(raw.templateId) || 0,
    frameRate: num(raw.frameRate) ?? 30,
    frameCount: num(raw.frameCount) ?? 0,
    featureDim: num(raw.featureDim) ?? 0,
    featureSequence: seq,
    keyframeIndex: keyframes,
    sampleCount: num(raw.sampleCount) ?? 0,
    status: String(raw.status ?? '')
  }
}

/**
 * 解析 start 接口快照。
 * @param raw API 响应 data（TaskActionSnapshot）
 */
export function parseSnapshot(raw: any): ParsedSnapshot {
  const rules: ParsedRule[] = Array.isArray(raw.rules)
    ? raw.rules.map((r: any) => ({
        actionRuleId: num(r.actionRuleId),
        jointName: String(r.jointName ?? ''),
        keypointIndices: String(r.keypointIndices ?? ''),
        angleFormula: String(r.angleFormula ?? ''),
        jointRole: String(r.jointRole ?? 'AUXILIARY'),
        dimension: String(r.dimension ?? 'ANGLE'),
        phase: String(r.phase ?? 'FULL'),
        minAngle: num(r.minAngle),
        maxAngle: num(r.maxAngle),
        standardAngle: num(r.standardAngle),
        toleranceSoft: num(r.toleranceSoft),
        toleranceHard: num(r.toleranceHard),
        holdFramesRequired: num(r.holdFramesRequired),
        speedMin: num(r.speedMin),
        speedMax: num(r.speedMax),
        scoreWeight: num(r.scoreWeight),
        severity: num(r.severity),
        promptText: r.promptText == null ? undefined : String(r.promptText),
        symmetryPairRuleId: num(r.symmetryPairRuleId)
      }))
    : []

  return {
    taskId: Number(raw.taskId),
    planItemId: Number(raw.planItemId),
    actionVersionId: Number(raw.actionVersionId),
    actionName: String(raw.actionName ?? ''),
    targetSets: num(raw.targetSets) ?? 1,
    targetReps: num(raw.targetReps) ?? 0,
    targetAmplitude: num(raw.targetAmplitude),
    durationSec: num(raw.durationSec),
    restSec: num(raw.restSec),
    instruction: raw.instruction == null ? undefined : String(raw.instruction),
    algoConfig: parseAlgoConfig(raw.algoConfig),
    snapshotEngineId: String(raw.poseEngineId ?? ''),
    signature: parseMotionSignature(raw.motionSignature),
    rules,
    template: parseTemplate(raw.template)
  }
}
