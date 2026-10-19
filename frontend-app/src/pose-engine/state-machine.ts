/**
 * 阶段状态机与计数（P3-F6，需求 6.3.8）。
 *
 * READY → RAISING → HOLD → LOWERING →（回到 READY 计一个周期）
 * - 双阈值滞后（tolerance 区间 + hysteresisBand 默认 8° 滞回带）；
 * - 连续 stateHoldFrames 帧确认才切换（去抖）；
 * - 低置信/无效帧不参与切换；
 * - 完整周期且幅度/保持达最低门槛才计 correct_reps；
 * - 周期短于 minCycleSec 视为甩动，直接丢弃不计次；
 * - 反方向大幅持续运动输出 mismatch 计数（交签名校验/纠错模块聚合）。
 */
import type { AlgoConfig } from './types'
import { clamp } from './geometry'

export type Phase = 'READY' | 'RAISING' | 'HOLD' | 'LOWERING'

export interface MachineParams {
  /** 期望方向；MIXED 时按 READY 期首次越界方向自动定向 */
  direction: 'FLEX' | 'EXTEND' | 'MIXED'
  /** READY 角度带 */
  startLow: number
  startHigh: number
  /** 峰值角度带（FLEX 取下沿，EXTEND 取上沿） */
  peakLow: number
  peakHigh: number
  /** 最小 ROM（度） */
  minRom: number
  /** 保持帧数要求（0 表示不要求） */
  holdFramesRequired: number
}

/** 状态机切出的原始周期（签名校验前） */
export interface RawCycle {
  startFrame: number
  peakFrame: number
  endFrame: number
  startAngle: number
  peakAngle: number
  rom: number
  maxHoldFrames: number
  hadHold: boolean
  durationSec: number
  /** 周期内帧索引（评分窗口） */
  frameRange: [number, number]
  direction: 'FLEX' | 'EXTEND'
  /** 基础门槛（时长/幅度/保持）是否通过；签名校验在后续环节 */
  basicValid: boolean
  /** 短于 minCycleSec 的甩动周期，丢弃不计次 */
  ignored: boolean
}

export interface MachineTickInput {
  angle: number | null
  frameIndex: number
  tSec: number
  /** 帧质量是否允许参与状态切换（低置信帧为 false） */
  switchable: boolean
}

interface CycleSeed {
  frame: number
  tSec: number
  angle: number
}

export class RepStateMachine {
  private readonly conf: AlgoConfig
  /** MIXED 标定基线后会就地收敛起始带，故不可 readonly */
  private params: MachineParams
  private polarity: 1 | -1
  private phase: Phase = 'READY'
  private pending: Phase | null = null
  private pendingCount = 0
  private cycleStart: CycleSeed | null = null
  private peakProj = -Infinity
  private peakFrame = -1
  private maxHoldFrames = 0
  private holdRun = 0
  private baseline: number | null = null
  private baselineSamples: number[] = []
  /** READY 带内最近基线角（周期起点角取它，而非越界瞬间角度，否则 ROM 被腰斩） */
  private readyAngle: number | null = null
  /** 本轮 READY 休息窗内最放松位的投影（FLEX 取最小角、EXTEND 投影后同为最小投影） */
  private readyMinProj = Infinity
  private mismatchedFrames = 0
  private mismatchStreak = 0
  private cycles: RawCycle[] = []
  /** 已完成周期数（不含被丢弃的甩动） */
  private completedCount = 0
  /** 每次 tick 后的相位轨迹（含无效帧，相位冻结） */
  readonly trace: Phase[] = []
  /** 每次 tick 是否产生反方向异常脉冲 */
  readonly pulseTrace: boolean[] = []

  constructor(conf: AlgoConfig, params: MachineParams) {
    this.conf = conf
    this.params = params
    this.polarity = params.direction === 'EXTEND' ? -1 : 1
  }

  get currentPhase(): Phase {
    return this.phase
  }

  /** 本周期已观测到的峰值投影角度 */
  get currentPeakAngle(): number | null {
    return this.peakProj === -Infinity ? null : this.polarity * this.peakProj
  }

  getCycles(): RawCycle[] {
    return this.cycles
  }

  get totalCompleted(): number {
    return this.completedCount
  }

  /** 连续反方向异常帧（实时纠错用） */
  get mismatchFrameStreak(): number {
    return this.mismatchStreak
  }

  /** 签名未给起始带时，用前 15 个有效帧做 READY 基线标定（MIXED 同步刷新起始带） */
  private calibrate(angle: number): void {
    if (this.baseline !== null) return
    if (this.baselineSamples.length < 15) this.baselineSamples.push(angle)
    if (this.baselineSamples.length >= 15) {
      this.baseline = this.baselineSamples.reduce((a, b) => a + b, 0) / this.baselineSamples.length
      if (this.params.direction === 'MIXED') {
        this.params = {
          ...this.params,
          startLow: this.baseline - this.conf.hysteresisBand,
          startHigh: this.baseline + this.conf.hysteresisBand
        }
      }
    }
  }

  private request(target: Phase): boolean {
    if (this.pending === target) {
      this.pendingCount++
    } else {
      this.pending = target
      this.pendingCount = 1
    }
    if (this.pendingCount >= this.conf.stateHoldFrames) {
      this.pending = null
      this.pendingCount = 0
      return true
    }
    return false
  }

  tick(input: MachineTickInput): { phaseChanged: boolean; closedCycle: RawCycle | null; mismatchPulse: boolean } {
    let mismatchPulse = false
    if (!input.switchable || input.angle === null) {
      // 低置信帧：冻结状态切换，保持当前相位
      this.trace.push(this.phase)
      this.pulseTrace.push(false)
      return { phaseChanged: false, closedCycle: null, mismatchPulse }
    }
    const angle = input.angle
    this.calibrate(angle)

    const hyst = this.conf.hysteresisBand
    const p = this.polarity
    const proj = p * angle
    const startHighProj = p * (p === 1 ? this.params.startHigh : this.params.startLow)
    const startLowProj = p * (p === 1 ? this.params.startLow : this.params.startHigh)
    const peakEnterProj = p * (p === 1 ? this.params.peakLow : this.params.peakHigh)

    const prev = this.phase
    let closedCycle: RawCycle | null = null

    switch (this.phase) {
      case 'READY': {
        // MIXED：按越界方向定向
        if (this.params.direction === 'MIXED' && this.baseline !== null) {
          if (angle > this.params.startHigh + hyst && this.polarity === -1) this.flipPolarity()
          else if (angle < this.params.startLow - hyst && this.polarity === 1) this.flipPolarity()
        }
        // 起始带内持续刷新基线角与最放松位，供新周期起点归因（ROM 从 READY 位算起）
        if (proj <= startHighProj) {
          this.readyAngle = angle
          if (proj < this.readyMinProj) this.readyMinProj = proj
        }
        if (proj > startHighProj + hyst) {
          if (this.request('RAISING')) {
            this.phase = 'RAISING'
            const startAngle =
              this.readyMinProj === Infinity ? (this.readyAngle ?? angle) : p * this.readyMinProj
            this.cycleStart = {
              frame: input.frameIndex,
              tSec: input.tSec,
              angle: startAngle
            }
            this.peakProj = proj
            this.peakFrame = input.frameIndex
            this.maxHoldFrames = 0
            this.holdRun = 0
          }
        } else if (proj < startLowProj - hyst - 10) {
          // READY 期出现反方向显著运动
          this.mismatchedFrames++
          if (this.mismatchedFrames >= this.conf.stateHoldFrames * 2) {
            this.mismatchStreak++
            this.mismatchedFrames = 0
            mismatchPulse = true
          }
        } else {
          this.mismatchedFrames = 0
        }
        break
      }
      case 'RAISING': {
        if (proj > this.peakProj) {
          this.peakProj = proj
          this.peakFrame = input.frameIndex
        }
        const inPeak = proj >= peakEnterProj
        if (inPeak) {
          this.holdRun++
          this.maxHoldFrames = Math.max(this.maxHoldFrames, this.holdRun)
          if (this.request('HOLD')) this.phase = 'HOLD'
        } else {
          this.holdRun = 0
          if (proj < startHighProj) {
            // 未达峰值即回落 → 本次不闭合有效周期，回 READY
            if (this.request('READY')) this.resetCycle()
          }
        }
        // 反方向大幅运动
        if (proj < startLowProj - hyst - 10) {
          this.mismatchStreak++
          mismatchPulse = true
        }
        break
      }
      case 'HOLD': {
        // HOLD 期间继续刷新峰值（峰值常出现在保持段，不能只记进入峰值带那一刻）
        if (proj > this.peakProj) {
          this.peakProj = proj
          this.peakFrame = input.frameIndex
        }
        if (proj >= peakEnterProj - hyst) {
          this.holdRun++
          this.maxHoldFrames = Math.max(this.maxHoldFrames, this.holdRun)
        }
        if (proj < peakEnterProj - hyst) {
          this.holdRun = 0
          if (this.request('LOWERING')) this.phase = 'LOWERING'
        }
        break
      }
      case 'LOWERING': {
        if (proj <= startHighProj + hyst) {
          if (this.request('READY')) {
            closedCycle = this.closeCycle(input)
            this.phase = 'READY'
          }
        }
        break
      }
    }

    this.trace.push(this.phase)
    this.pulseTrace.push(mismatchPulse)
    return { phaseChanged: prev !== this.phase, closedCycle, mismatchPulse }
  }

  private flipPolarity(): void {
    this.polarity = (this.polarity * -1) as 1 | -1
  }

  private resetCycle(): void {
    this.phase = 'READY'
    this.cycleStart = null
    this.peakProj = -Infinity
    this.peakFrame = -1
    this.maxHoldFrames = 0
    this.holdRun = 0
    this.readyMinProj = Infinity
    this.mismatchStreak = 0
  }

  private closeCycle(end: MachineTickInput): RawCycle {
    const start = this.cycleStart as CycleSeed
    const p = this.polarity
    const startAngle = start.angle
    const peakAngle = p * this.peakProj
    const rom = Math.abs(peakAngle - startAngle)
    const durationSec = Math.max(0, end.tSec - start.tSec)
    const holdRequired = this.params.holdFramesRequired
    const hadHold = this.maxHoldFrames >= Math.max(holdRequired, this.conf.stateHoldFrames)
    const ignored = durationSec < this.conf.minCycleSec
    const basicValid =
      !ignored &&
      rom >= this.params.minRom &&
      (holdRequired === 0 || this.maxHoldFrames >= holdRequired)

    const cycle: RawCycle = {
      startFrame: start.frame,
      peakFrame: this.peakFrame,
      endFrame: end.frameIndex,
      startAngle,
      peakAngle,
      rom,
      maxHoldFrames: this.maxHoldFrames,
      hadHold,
      durationSec,
      frameRange: [start.frame, end.frameIndex],
      direction: p === 1 ? 'FLEX' : 'EXTEND',
      basicValid,
      ignored
    }
    if (!ignored) {
      this.completedCount++
      this.cycles.push(cycle)
    }
    this.mismatchStreak = 0
    this.cycleStart = null
    this.peakProj = -Infinity
    this.peakFrame = -1
    this.maxHoldFrames = 0
    this.holdRun = 0
    this.readyMinProj = Infinity
    return cycle
  }

  /** 由签名/快照构建状态机参数 */
  static buildParams(
    conf: AlgoConfig,
    opts: {
      direction?: 'FLEX' | 'EXTEND' | 'MIXED'
      startRange?: [number | null, number | null]
      peakRange?: [number | null, number | null]
      minRom?: number
      targetAmplitude?: number
      standardAngle?: number
      holdFramesRequired?: number
    }
  ): MachineParams {
    const hyst = conf.hysteresisBand
    const standard = opts.standardAngle ?? 90
    const startLow = opts.startRange?.[0] ?? standard - hyst
    const startHigh = opts.startRange?.[1] ?? standard + hyst
    const minRom = opts.minRom ?? opts.targetAmplitude ?? 30
    const peakLow = opts.peakRange?.[0] ?? startHigh + minRom
    const peakHigh = opts.peakRange?.[1] ?? clamp(startHigh + minRom + 60, 0, 180)

    // 方向一致性矫正：起止区间完备时，方向由“峰值中位相对起始中位的升降”决定。
    // 端侧角度口径为屈曲角（伸直 0°、屈曲增大）；若治疗师把“抬臂”误标为 EXTEND
    // 但区间仍是 [0,30]→[70,110] 这种上升形态，硬按 EXTEND 投影会导致永远无法触发。
    // MIXED（双向动作）不做矫正。
    let direction = opts.direction ?? 'FLEX'
    if (direction !== 'MIXED' && opts.startRange && opts.peakRange) {
      const startMid = (startLow + startHigh) / 2
      const peakMid = (peakLow + peakHigh) / 2
      if (Math.abs(peakMid - startMid) >= hyst) {
        direction = peakMid > startMid ? 'FLEX' : 'EXTEND'
      }
    }
    return {
      direction,
      startLow,
      startHigh,
      peakLow,
      peakHigh,
      minRom,
      holdFramesRequired: opts.holdFramesRequired ?? 0
    }
  }
}
