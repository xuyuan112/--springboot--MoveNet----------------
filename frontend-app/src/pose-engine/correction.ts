/**
 * 纠错提示队列（P3-F8，需求 6.3.10）。
 *
 * - 优先级 = 严重度 severity × 规则权重 scoreWeight × 归一化偏差；
 * - 同屏最多 1~2 条；同一规则相邻区间 3 秒内不重复弹出（持续区间常驻不闪烁）；
 * - toleranceSoft 内不提示；soft~hard 为 1 级（黄）；超 hard 为 2 级（红）；
 * - 按“规则 × 连续区间”聚合为 error_summary；
 * - CONSTRAINT 关节同样提示，但不影响幅度分（由稳定维体现）；
 * - ACTION_MISMATCH 作为特殊维度参与提示与汇总。
 */
import type { AlgoConfig } from './types'
import type { ParsedRule } from './snapshot'
import type { Phase } from './state-machine'

export interface CorrectionContext {
  frameIndex: number
  tSec: number
  phase: Phase
  /** 当前帧是否有效（低置信帧冻结判定） */
  valid: boolean
  /** 每个评估器当前角度与角速度（度/秒） */
  angles: Map<string, { value: number; speed: number | null }>
  /** 当前左右对称差（度），无配对为 null */
  symmetryDiff: number | null
  /** 实时抖动（度，残差 RMS） */
  jitterDeg: number
  /** 状态机反方向异常脉冲 */
  mismatchPulse: boolean
}

export interface CorrectionItem {
  key: string
  ruleId?: number
  dimension: string
  joint: string
  level: 1 | 2
  text: string
  priority: number
}

export interface ErrorSegment {
  ruleId?: number
  dimension: string
  joint: string
  phase: string
  level: 1 | 2
  startFrame: number
  endFrame: number
  frames: number
  peakDeviation: number
  promptText: string
}

const DEDUP_SEC = 3
const STABILITY_SOFT = 5
const STABILITY_HARD = 10
const SPEED_MARGIN_SOFT = 0.1
const SPEED_MARGIN_HARD = 0.25

interface ActiveSegment {
  key: string
  rule?: ParsedRule
  dimension: string
  joint: string
  level: 1 | 2
  startFrame: number
  endFrame: number
  peakDeviation: number
  promptText: string
  priority: number
  shown: boolean
}

export class CorrectionQueue {
  private readonly rules: ParsedRule[]
  private readonly conf: AlgoConfig
  private readonly active = new Map<string, ActiveSegment>()
  private readonly segments: ErrorSegment[] = []
  /** 每个规则上一区间结束时间（秒），用于 3 秒去重 */
  private readonly lastEndAt = new Map<string, number>()
  private visibleItems: CorrectionItem[] = []

  constructor(rules: ParsedRule[], conf: AlgoConfig) {
    this.conf = conf
    this.rules = rules
  }

  get items(): CorrectionItem[] {
    return this.visibleItems
  }

  get errorSummary(): ErrorSegment[] {
    return this.segments
  }

  update(ctx: CorrectionContext): CorrectionItem[] {
    if (!ctx.valid) {
      this.freezeAll()
      this.visibleItems = []
      return this.visibleItems
    }

    const fired: Array<{ key: string; rule?: ParsedRule; dimension: string; joint: string; level: 1 | 2; dev: number }> = []

    // ACTION_MISMATCH 特殊维度（READY 期的反方向运动正是它的触发场景，不受相位门控）
    if (ctx.mismatchPulse) {
      fired.push({ key: 'action-mismatch', dimension: 'ACTION_MISMATCH', joint: '', level: 2, dev: 2 })
    }

    // READY=周期间休息位，目标角类规则此刻必然偏离，不产生规则提示（冻结并收区间）
    if (ctx.phase !== 'READY') {
      for (const rule of this.rules) {
        const key = rule.actionRuleId != null ? `rule-${rule.actionRuleId}` : `rule-${rule.jointName}-${rule.dimension}`
        const angle = ctx.angles.get(key)
        // HOLD 规则仅在 HOLD/RAISING 末端相位评估
        if (rule.dimension === 'HOLD' && ctx.phase !== 'HOLD' && ctx.phase !== 'RAISING') continue

        if (rule.dimension === 'ANGLE' || rule.dimension === 'HOLD') {
          if (!angle) continue
          const hit = this.evalAngle(rule, angle.value)
          if (hit) fired.push({ key, rule, dimension: rule.dimension, joint: rule.jointName, ...hit })
        } else if (rule.dimension === 'SPEED') {
          if (!angle || angle.speed === null) continue
          const hit = this.evalSpeed(rule, angle.speed)
          if (hit) fired.push({ key, rule, dimension: 'SPEED', joint: rule.jointName, ...hit })
        } else if (rule.dimension === 'STABILITY') {
          const hit = this.evalJitter(rule, ctx.jitterDeg)
          if (hit) fired.push({ key, rule, dimension: 'STABILITY', joint: rule.jointName, ...hit })
        } else if (rule.dimension === 'SYMMETRY') {
          if (ctx.symmetryDiff === null) continue
          const hit = this.evalSymmetry(rule, ctx.symmetryDiff)
          if (hit) fired.push({ key, rule, dimension: 'SYMMETRY', joint: rule.jointName, ...hit })
        }
      }
    }

    const firedKeys = new Set(fired.map((f) => f.key))
    for (const key of [...this.active.keys()]) {
      if (!firedKeys.has(key)) this.closeSegment(key, ctx.tSec)
    }

    for (const f of fired) {
      const priority = this.priority(f.rule, f.level, f.dev)
      const existing = this.active.get(f.key)
      if (existing) {
        existing.endFrame = ctx.frameIndex
        existing.level = f.level > existing.level ? f.level : existing.level
        existing.peakDeviation = Math.max(existing.peakDeviation, f.dev)
        existing.priority = Math.max(existing.priority, priority)
      } else {
        const lastEnd = this.lastEndAt.get(f.key)
        const suppressed = lastEnd !== undefined && ctx.tSec - lastEnd < DEDUP_SEC
        const text =
          f.dimension === 'ACTION_MISMATCH'
            ? '未检测到当前训练任务对应的有效动作，请调整姿势'
            : f.rule?.promptText || this.defaultText(f.dimension, f.joint)
        this.active.set(f.key, {
          key: f.key,
          rule: f.rule,
          dimension: f.dimension,
          joint: f.joint,
          level: f.level,
          startFrame: ctx.frameIndex,
          endFrame: ctx.frameIndex,
          peakDeviation: f.dev,
          promptText: text,
          priority,
          shown: !suppressed
        })
      }
    }

    // 同屏最多 2 条，按优先级排序
    this.visibleItems = [...this.active.values()]
      .filter((s) => s.shown)
      .sort((a, b) => b.priority - a.priority)
      .slice(0, 2)
      .map((s) => ({
        key: s.key,
        ruleId: s.rule?.actionRuleId,
        dimension: s.dimension,
        joint: s.joint,
        level: s.level,
        text: s.promptText,
        priority: s.priority
      }))
    return this.visibleItems
  }

  /** 结束全部区间（训练结束时调用） */
  finish(tSec: number): ErrorSegment[] {
    for (const key of [...this.active.keys()]) this.closeSegment(key, tSec)
    return this.segments
  }

  private freezeAll(): void {
    this.visibleItems = []
  }

  private closeSegment(key: string, tSec: number): void {
    const seg = this.active.get(key)
    if (!seg) return
    if (seg.shown) {
      this.segments.push({
        ruleId: seg.rule?.actionRuleId,
        dimension: seg.dimension,
        joint: seg.joint,
        phase: seg.rule?.phase ?? 'FULL',
        level: seg.level,
        startFrame: seg.startFrame,
        endFrame: seg.endFrame,
        frames: seg.endFrame - seg.startFrame + 1,
        peakDeviation: Math.round(seg.peakDeviation * 10) / 10,
        promptText: seg.promptText
      })
    }
    this.lastEndAt.set(key, tSec)
    this.active.delete(key)
  }

  private priority(rule: ParsedRule | undefined, level: 1 | 2, normalizedDev: number): number {
    const severity = rule?.severity ?? level
    const weight = rule?.scoreWeight ?? 0.5
    return severity * weight * Math.max(1, normalizedDev)
  }

  private bands(rule: ParsedRule): { soft: number; hard: number; center: number; min?: number; max?: number } {
    const soft = rule.toleranceSoft ?? this.conf.hysteresisBand
    const hard = rule.toleranceHard ?? soft * 2
    return {
      soft,
      hard,
      center: rule.standardAngle ?? (rule.minAngle != null && rule.maxAngle != null ? (rule.minAngle + rule.maxAngle) / 2 : 0),
      min: rule.minAngle ?? undefined,
      max: rule.maxAngle ?? undefined
    }
  }

  private deviation(rule: ParsedRule, value: number): number {
    const b = this.bands(rule)
    // 超出允许区间 [min,max] 的量，否则相对 standard 的偏离量
    if (b.min != null && value < b.min) return b.min - value
    if (b.max != null && value > b.max) return value - b.max
    return Math.abs(value - b.center)
  }

  private evalAngle(rule: ParsedRule, value: number): { level: 1 | 2; dev: number } | null {
    const b = this.bands(rule)
    const dev = this.deviation(rule, value)
    if (dev <= b.soft) return null
    return { level: dev <= b.hard ? 1 : 2, dev }
  }

  private evalSpeed(rule: ParsedRule, speedAbs: number): { level: 1 | 2; dev: number } | null {
    const lo = rule.speedMin
    const hi = rule.speedMax
    let dev = 0
    if (lo != null && speedAbs < lo) dev = (lo - speedAbs) / Math.max(lo, 1)
    if (hi != null && speedAbs > hi) dev = (speedAbs - hi) / Math.max(hi, 1)
    if (dev <= SPEED_MARGIN_SOFT) return null
    return { level: dev <= SPEED_MARGIN_HARD ? 1 : 2, dev }
  }

  private evalJitter(rule: ParsedRule, jitter: number): { level: 1 | 2; dev: number } | null {
    const b = this.bands(rule)
    const soft = b.soft || STABILITY_SOFT
    const hard = b.hard || STABILITY_HARD
    if (jitter <= soft) return null
    return { level: jitter <= hard ? 1 : 2, dev: jitter / Math.max(hard, 1) }
  }

  private evalSymmetry(rule: ParsedRule, diff: number): { level: 1 | 2; dev: number } | null {
    const b = this.bands(rule)
    const soft = b.soft || 10
    const hard = b.hard || 20
    if (diff <= soft) return null
    return { level: diff <= hard ? 1 : 2, dev: diff / Math.max(hard, 1) }
  }

  private defaultText(dimension: string, joint: string): string {
    const who = joint ? `（${joint}）` : ''
    const map: Record<string, string> = {
      ANGLE: `请调整动作角度${who}`,
      HOLD: `请在目标位置保持稳定${who}`,
      SPEED: `动作速度过快/过慢，请注意节奏${who}`,
      STABILITY: `身体晃动过大，请保持稳定`,
      SYMMETRY: `左右侧动作不对称，请均衡发力`
    }
    return map[dimension] ?? '请按指导语调整动作'
  }
}
