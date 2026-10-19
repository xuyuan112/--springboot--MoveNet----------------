/**
 * 归一化与特征提取（P3-F4，需求 6.3.5/6.3.6）。
 *
 * 归一化：髋中心平移 → 像素身高/真实身高（缺身高档案时按肩宽兜底）尺度归一 → 躯干轴向对齐。
 * 角度约定（端侧统一口径）：
 * - angle(a,b,c)      屈曲角 = 180 - 三点内角（伸直=0°，屈曲增大），符合康复 ROM 习惯；
 * - innerAngle(a,b,c) 三点内角（伸直=180°）；
 * - inclination(a,b)  相对竖直方向的倾斜角（0~180，躯干直立=0°）。
 */
import type { AlgoConfig, PoseFrame } from './types'
import type { ParsedRule } from './snapshot'
import {
  KP,
  angleAt,
  centeredMovingAverage,
  clamp,
  derivative,
  dist,
  inclination,
  jointTriplet,
  kpPoint,
  mean,
  midpoint,
  SYMMETRY_PAIRS,
  symmetryKey
} from './geometry'
import type { Point } from './geometry'

/** 默认肩宽（厘米），无身高档案时的尺度兜底 */
const DEFAULT_SHOULDER_WIDTH_CM = 40

export type EvaluatorKind = 'angle' | 'innerAngle' | 'inclination'

export interface AngleEvaluator {
  /** 唯一键：优先规则 ID，目标关节兜底 jointName */
  key: string
  ruleId?: number
  jointName: string
  kind: EvaluatorKind
  /** 三点索引（顶点居中，inclination 时取前两个） */
  indices: number[]
  /** 规则原始字段（评分/纠错用） */
  rule: ParsedRule
}

export interface NormalizedFrame {
  frame: PoseFrame
  /** 厘米尺度下、躯干对齐后的坐标（索引同 17 关键点） */
  points: (Point | null)[]
  /** 当帧像素/厘米 换算率（无效帧为 null） */
  scale: number | null
}

export interface AngleTrack {
  evaluator: AngleEvaluator
  /** 逐帧角度（无效帧为 null） */
  series: (number | null)[]
  /** 平滑后角速度（度/秒） */
  speed: (number | null)[]
  /** 平滑后角加速度（度/秒²） */
  accel: (number | null)[]
  timesSec: number[]
  valid: boolean[]
}

export interface TrackStats {
  /** 活动范围 ROM（度） */
  rom: number
  minAngle: number
  maxAngle: number
  minFrame: number
  maxFrame: number
  /** 峰值角速度绝对值 */
  peakSpeed: number
  /** 高频抖动能量（RMS） */
  jitter: number
  /** 有效帧角度均值 */
  meanAngle: number
}

/** 解析 angle_formula，失败时按关节名词典兜底 */
export function buildEvaluator(rule: ParsedRule, fallbackKey: string): AngleEvaluator | null {
  const formula = (rule.angleFormula || '').trim()
  let kind: EvaluatorKind = 'angle'
  let indices: number[] = []

  const flex = /^angle\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/i.exec(formula)
  const inner = /^innerAngle\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/i.exec(formula)
  const incl = /^inclination\s*\(\s*(\d+)\s*,\s*(\d+)\s*\)$/i.exec(formula)
  if (flex) {
    kind = 'angle'
    indices = [Number(flex[1]), Number(flex[2]), Number(flex[3])]
  } else if (inner) {
    kind = 'innerAngle'
    indices = [Number(inner[1]), Number(inner[2]), Number(inner[3])]
  } else if (incl) {
    kind = 'inclination'
    indices = [Number(incl[1]), Number(incl[2])]
  } else {
    // keypoint_indices JSON 兜底
    const kps = parseKeypointIndices(rule.keypointIndices)
    if (kps.length >= 3) {
      kind = 'angle'
      indices = kps.slice(0, 3)
    } else {
      const triplet = jointTriplet(rule.jointName || '')
      if (triplet) {
        kind = 'angle'
        indices = [...triplet]
      }
    }
  }

  if (indices.length < 2) return null
  if (indices.some((i) => i < 0 || i > 16)) return null
  return {
    key: rule.actionRuleId != null ? `rule-${rule.actionRuleId}` : fallbackKey,
    ruleId: rule.actionRuleId ?? undefined,
    jointName: rule.jointName || fallbackKey,
    kind,
    indices,
    rule
  }
}

/** 解析 keypoint_indices（后端 JSON 字符串，如 "[6,8,10]"） */
export function parseKeypointIndices(raw: string | undefined | null): number[] {
  if (!raw) return []
  try {
    const v = JSON.parse(raw)
    if (Array.isArray(v)) return v.map(Number).filter((n) => Number.isInteger(n) && n >= 0 && n <= 16)
  } catch {
    // 忽略非法 JSON
  }
  return []
}

/**
 * 特征提取器（无状态可复用；逐帧计算 + 整段统计）。
 */
export class FeatureExtractor {
  private readonly conf: AlgoConfig
  private readonly heightCm?: number

  constructor(conf: AlgoConfig, heightCm?: number) {
    this.conf = conf
    this.heightCm = heightCm && heightCm > 50 && heightCm < 250 ? heightCm : undefined
  }

  /** 单帧归一化 */
  normalize(frame: PoseFrame, present: boolean[] = new Array(17).fill(true)): NormalizedFrame {
    const pts: (Point | null)[] = frame.keypoints.map((k, i) => (present[i] ? { x: k.x, y: k.y } : null))
    const lHip = pts[KP.L_HIP]
    const rHip = pts[KP.R_HIP]
    const lShoulder = pts[KP.L_SHOULDER]
    const rShoulder = pts[KP.R_SHOULDER]
    if (!lHip || !rHip || !lShoulder || !rShoulder) {
      return { frame, points: pts, scale: null }
    }

    const hipCenter = midpoint(lHip, rHip)
    const shoulderCenter = midpoint(lShoulder, rShoulder)

    // 尺度：画面内身高（头/肩中点→踝中点），踝缺失退化为髋→肩
    let personPx: number | null = null
    const lAnkle = pts[KP.L_ANKLE]
    const rAnkle = pts[KP.R_ANKLE]
    if (lAnkle && rAnkle) {
      personPx = dist(shoulderCenter, midpoint(lAnkle, rAnkle))
    } else {
      personPx = dist(shoulderCenter, hipCenter) * 3.2 // 躯干约占身高 1/3 强
    }
    let pxPerCm: number
    if (this.heightCm) {
      pxPerCm = personPx / this.heightCm
      if (!(pxPerCm > 1e-6)) pxPerCm = dist(lShoulder, rShoulder) / DEFAULT_SHOULDER_WIDTH_CM
    } else {
      pxPerCm = dist(lShoulder, rShoulder) / DEFAULT_SHOULDER_WIDTH_CM
    }
    if (!(pxPerCm > 1e-6)) return { frame, points: pts, scale: null }

    // 躯干轴向：肩中心→髋中心相对竖直的偏角，旋转抵消
    const torsoVec = { x: hipCenter.x - shoulderCenter.x, y: hipCenter.y - shoulderCenter.y }
    const theta = Math.atan2(torsoVec.x, torsoVec.y) // 竖直向下为 0
    const cos = Math.cos(-theta)
    const sin = Math.sin(-theta)

    const normalized = pts.map((p) => {
      if (!p) return null
      // 平移到髋中心 → 缩放为厘米 → 躯干旋转对齐
      const dx = (p.x - hipCenter.x) / pxPerCm
      const dy = (p.y - hipCenter.y) / pxPerCm
      return { x: dx * cos - dy * sin, y: dx * sin + dy * cos }
    })
    return { frame, points: normalized, scale: pxPerCm }
  }

  /** 计算单个评估器在某帧的角度（度）；关键点不足返回 null */
  angle(frame: PoseFrame, ev: AngleEvaluator, present: boolean[] = new Array(17).fill(true)): number | null {
    const idx = ev.indices
    if (ev.kind === 'inclination') {
      if (!present[idx[0]] || !present[idx[1]]) return null
      return inclination(kpPoint(frame, idx[0]), kpPoint(frame, idx[1]))
    }
    if (idx.length < 3 || !present[idx[0]] || !present[idx[1]] || !present[idx[2]]) return null
    const inner = angleAt(kpPoint(frame, idx[0]), kpPoint(frame, idx[1]), kpPoint(frame, idx[2]))
    return ev.kind === 'innerAngle' ? inner : clamp(180 - inner, 0, 180)
  }

  /** 对修复后的整段序列构建角度轨道（含速度/加速度平滑） */
  buildTracks(
    frames: PoseFrame[],
    present: boolean[][],
    valid: boolean[],
    evaluators: AngleEvaluator[],
    startTimestamp: number
  ): Map<string, AngleTrack> {
    const timesSec = frames.map((f) => (f.timestamp - startTimestamp) / 1000)
    const tracks = new Map<string, AngleTrack>()
    for (const ev of evaluators) {
      const series = frames.map((f, t) => (valid[t] ? this.angle(f, ev, present[t]) : null))

      // 平滑导数：速度窗 7、加速度窗 11（需求 6.3.4）；空帧邻接填充后再差分
      const filled = fillNullsForDerivative(series)
      const speedRaw = derivative(filled, timesSec)
      const accelRaw = derivative(centeredMovingAverage(speedRaw, this.conf.smoothWindowSpeed), timesSec)
      const speedSmooth = centeredMovingAverage(speedRaw, this.conf.smoothWindowSpeed)
      const accelSmooth = centeredMovingAverage(accelRaw, this.conf.smoothWindowAccel)

      tracks.set(ev.key, {
        evaluator: ev,
        series,
        speed: series.map((v, t) => (v === null ? null : speedSmooth[t])),
        accel: series.map((v, t) => (v === null ? null : accelSmooth[t])),
        timesSec,
        valid
      })
    }
    return tracks
  }

  /** 单条轨道统计（ROM/极值帧/峰值速度/抖动） */
  trackStats(track: AngleTrack): TrackStats | null {
    const vals = track.series.filter((v): v is number => v !== null)
    if (vals.length < 3) return null
    let min = Infinity
    let max = -Infinity
    let minFrame = -1
    let maxFrame = -1
    track.series.forEach((v, i) => {
      if (v === null) return
      if (v < min) {
        min = v
        minFrame = i
      }
      if (v > max) {
        max = v
        maxFrame = i
      }
    })
    const speeds = track.speed.filter((v): v is number => v !== null)
    const baseline = centeredMovingAverage(vals, this.conf.smoothWindowSpeed)
    const jitter = Math.sqrt(mean(vals.map((v, i) => (v - baseline[i]) ** 2)))
    return {
      rom: max - min,
      minAngle: min,
      maxAngle: max,
      minFrame,
      maxFrame,
      peakSpeed: speeds.reduce((m, v) => Math.max(m, Math.abs(v)), 0),
      jitter,
      meanAngle: mean(vals)
    }
  }

  /** 左右对称差序列（度），无配对返回 null */
  symmetrySeries(
    frames: PoseFrame[],
    present: boolean[][],
    valid: boolean[],
    jointName: string
  ): (number | null)[] | null {
    const key = symmetryKey(jointName)
    if (!key) return null
    const pair = SYMMETRY_PAIRS[key]
    return frames.map((f, t) => {
      if (!valid[t]) return null
      const p = present[t]
      const tripOK = (triplet: readonly number[]) => triplet.every((i) => p[i])
      if (!tripOK(pair.left) || !tripOK(pair.right)) return null
      const a = angleAt(kpPoint(f, pair.left[0]), kpPoint(f, pair.left[1]), kpPoint(f, pair.left[2]))
      const b = angleAt(kpPoint(f, pair.right[0]), kpPoint(f, pair.right[1]), kpPoint(f, pair.right[2]))
      return Math.abs(a - b)
    })
  }
}

/** 导数值计算前对 null 段做保持填充（仅用于求导，不回写原序列） */
function fillNullsForDerivative(series: (number | null)[]): number[] {
  const out = new Array<number>(series.length).fill(0)
  let last = 0
  for (let i = 0; i < series.length; i++) {
    if (series[i] !== null) last = series[i] as number
    out[i] = last
  }
  return out
}
