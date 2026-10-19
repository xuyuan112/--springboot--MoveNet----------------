/**
 * MoveNet 姿态评估引擎 —— 实时管线编排（P3 落地版）。
 *
 * 实时路径（每帧，因果）：
 *   MoveNet 推理 → 入场检查/ROI → 质量门控 → 在线平滑修复 → 角度特征
 *   → 阶段状态机计数 → 纠错队列 → UI 状态（骨架/计数/评分预览/提示）
 *
 * 结束路径（非因果，确定性复算，结果可复现）：
 *   原始帧缓存（仅本地，绝不上传）→ repairSequence → buildTracks
 *   → 状态机重放 → 签名校验 → 纠错区间聚合 → 六维评分 → TrainingSummary
 *
 * 原始视频与逐帧关键点均不离开浏览器；提交载荷仅含汇总与纠错聚合（P4）。
 */
import type { AlgoConfig, PoseFrame, TrainingSummary } from './types'
import { MoveNetDetector } from './movenet'
import type { NormRect } from './movenet'
import { QualityGate } from './quality'
import { OnlineSmoother, repairSequence } from './smooth-repair'
import type { SmoothedFrame } from './smooth-repair'
import { EntryQualityChecker } from './entry-check'
import type { EntryIssue } from './entry-check'
import { FeatureExtractor, buildEvaluator } from './features'
import type { AngleEvaluator } from './features'
import { RepStateMachine } from './state-machine'
import type { MachineParams, Phase, RawCycle } from './state-machine'
import { SignatureValidator, MISMATCH_ADVICE_CYCLES, MISMATCH_REASON } from './signature'
import { CorrectionQueue } from './correction'
import type { CorrectionItem, ErrorSegment } from './correction'
import { scoreSession } from './scoring'
import type { ScoredCycle } from './scoring'
import { parseSnapshot } from './snapshot'
import type { ParsedRule, ParsedSnapshot } from './snapshot'
import { SYMMETRY_PAIRS, angleAt, centeredMovingAverage, flipRectX, kpPoint, symmetryKey } from './geometry'

export class EngineVersionMismatchError extends Error {
  readonly expected: string
  readonly actual: string
  constructor(expected: string, actual: string) {
    super(`姿态引擎版本不一致：端侧 ${expected}，动作版本 ${actual || '(空)'}，请联系治疗师更新动作版本`)
    this.name = 'EngineVersionMismatchError'
    this.expected = expected
    this.actual = actual
  }
}

export type TrackingMode = 'ENTRY' | 'TRACKING'

export interface RealtimeState {
  mode: TrackingMode
  /** 入场阶段问题（TRACKING 时为 NONE） */
  entryIssue: EntryIssue
  coreVisibleRatio: number
  /** 连续丢失计数达到阈值，提示重新站位 */
  broken: boolean
  phase: Phase
  totalReps: number
  correctReps: number
  targetReps: number
  progressPercent: number
  /** 主关节当前角度（度） */
  mainAngle: number | null
  /** 本帧用于叠加的关键点（TRACKING 为平滑修复后） */
  frame: PoseFrame
  present: boolean[]
  repaired: boolean[]
  corrections: CorrectionItem[]
  /** 连续动作不匹配，建议结束并联系治疗师 */
  mismatchAdvice: boolean
  fps: number
}

export interface SessionResult {
  summary: TrainingSummary
  scoredCycles: ScoredCycle[]
  errorSegments: ErrorSegment[]
  cycles: RawCycle[]
  fps: number
  rawFrameCount: number
  mismatchAdvice: boolean
  mismatchReason: string
}

interface EngineOptions {
  heightCm?: number
  mirror?: boolean
}

export class PoseAssessmentEngine {
  private readonly snapshot: ParsedSnapshot
  private readonly conf: AlgoConfig
  private readonly mirror: boolean
  private readonly evaluators: AngleEvaluator[]
  private readonly main: AngleEvaluator
  private readonly symmetryPair: { left: AngleEvaluator; right: AngleEvaluator } | null
  private readonly machineParams: MachineParams
  private readonly validator: SignatureValidator

  private detector: MoveNetDetector | null = null
  private gate: QualityGate | null = null
  private smoother: OnlineSmoother | null = null
  private entry: EntryQualityChecker | null = null
  private extractor: FeatureExtractor | null = null
  private machine: RepStateMachine | null = null
  private corrections: CorrectionQueue | null = null

  private mode: TrackingMode = 'ENTRY'
  private rawFrames: PoseFrame[] = []
  private brokenRun = 0
  private speedState = new Map<string, { prevAngle: number; prevT: number; ema: number; init: boolean }>()
  private angleRing: number[] = []
  private consecutiveMismatch = 0
  private mismatchAdvice = false

  constructor(rawSnapshot: unknown, options: EngineOptions = {}) {
    this.snapshot = parseSnapshot(rawSnapshot)
    this.conf = { ...this.snapshot.algoConfig }
    this.mirror = options.mirror ?? true

    // 评估器：所有规则解析；主评估器取 PRIMARY 的 ANGLE/HOLD，其次任意角度规则
    this.evaluators = this.snapshot.rules
      .map((rule, i) => buildEvaluator(rule, `joint-${i}`))
      .filter((e): e is AngleEvaluator => e !== null)

    this.main = this.pickMainEvaluator()
    this.symmetryPair = this.buildSymmetryPair()

    const primaryRule = this.findMainRule()
    this.machineParams = RepStateMachine.buildParams(this.conf, {
      direction: this.snapshot.signature?.expectedDirection,
      startRange: this.snapshot.signature?.startAngleRange,
      peakRange: this.snapshot.signature?.peakAngleRange,
      minRom: this.snapshot.signature?.minRom,
      targetAmplitude: this.snapshot.targetAmplitude,
      standardAngle: primaryRule?.standardAngle,
      holdFramesRequired: primaryRule?.holdFramesRequired
    })
    this.validator = new SignatureValidator(this.snapshot.signature)
    this.extractor = new FeatureExtractor(this.conf, options.heightCm)
  }

  get parsedSnapshot(): ParsedSnapshot {
    return this.snapshot
  }

  get engineIdExpected(): string {
    return this.detector?.engineId ?? ''
  }

  /** 加载本地模型（WebGL→WASM 降级 + sha256 校验） */
  async loadModel(onStatus?: (message: string) => void): Promise<{ engineId: string; backend: string }> {
    this.detector = await MoveNetDetector.create({ mirror: this.mirror, onStatus })
    // pose_engine_id 必须与动作版本快照一致，否则拒绝开始（后端 422 兜底）
    if (this.snapshot.snapshotEngineId && this.detector.engineId !== this.snapshot.snapshotEngineId) {
      throw new EngineVersionMismatchError(this.detector.engineId, this.snapshot.snapshotEngineId)
    }
    this.conf.poseEngineId = this.detector.engineId

    this.gate = new QualityGate(this.conf)
    this.smoother = new OnlineSmoother(this.conf)
    this.entry = new EntryQualityChecker(this.conf)
    this.machine = new RepStateMachine(this.conf, this.machineParams)
    this.corrections = new CorrectionQueue(this.snapshot.rules, this.conf)
    return { engineId: this.detector.engineId, backend: this.detector.backendName }
  }

  /** 逐帧处理：页面采集循环调用，返回实时 UI 状态 */
  async handleVideoFrame(video: HTMLVideoElement): Promise<RealtimeState> {
    if (!this.detector || !this.gate || !this.smoother || !this.entry || !this.machine || !this.corrections) {
      throw new Error('引擎尚未初始化，请先 await loadModel()')
    }
    const frameIndex = this.rawFrames.length
    const timestamp = performance.now()
    const roi: NormRect | undefined =
      this.mode === 'TRACKING' && this.entry.currentRoi
        ? this.mirror
          ? flipRectX(this.entry.currentRoi)
          : this.entry.currentRoi
        : undefined
    const raw = await this.detector.estimate(video, frameIndex, timestamp, roi)
    this.rawFrames.push(raw)

    const emptyState = (partial: Partial<RealtimeState>): RealtimeState => ({
      mode: this.mode,
      entryIssue: 'NO_SUBJECT',
      coreVisibleRatio: 0,
      broken: false,
      phase: 'READY',
      totalReps: this.machine?.totalCompleted ?? 0,
      correctReps: 0,
      targetReps: this.snapshot.targetReps,
      progressPercent: 0,
      mainAngle: null,
      frame: raw,
      present: new Array(17).fill(false),
      repaired: new Array(17).fill(false),
      corrections: [],
      mismatchAdvice: this.mismatchAdvice,
      fps: this.liveFps(),
      ...partial
    })

    // ===== 入场阶段 =====
    if (this.mode === 'ENTRY') {
      const r = this.entry.evaluate(raw)
      if (r.passed && r.roi) {
        this.mode = 'TRACKING'
        this.brokenRun = 0
      } else {
        return emptyState({ entryIssue: r.issue, coreVisibleRatio: r.coreVisibleRatio })
      }
    }

    // ===== 训练阶段：连续性监控（失败回到入场） =====
    const track = this.entry.track(raw)
    if (track.break) {
      this.brokenRun++
      if (this.brokenRun >= EntryQualityChecker.BREAK_FRAMES) {
        this.mode = 'ENTRY'
        this.entry.reset()
        return emptyState({ entryIssue: track.issue, coreVisibleRatio: track.ratio, broken: true })
      }
    } else {
      this.brokenRun = 0
    }

    // 管线处理的是工作副本，rawFrames 中的原始帧保持不被污染（供结束复算）
    const work: PoseFrame = { ...raw, keypoints: raw.keypoints.map((k) => ({ ...k })) }
    const gated = this.gate.gate(work)
    const smoothed: SmoothedFrame = this.smoother.process(gated)

    const tSec = timestamp / 1000
    const angles = new Map<string, { value: number; speed: number | null }>()
    for (const ev of this.evaluators) {
      const value = this.extractor!.angle(smoothed.frame, ev, smoothed.present)
      if (value === null) continue
      angles.set(ev.key, { value, speed: this.onlineSpeed(ev.key, value, tSec) })
    }
    const mainValue = angles.get(this.main.key)?.value ?? null
    if (mainValue !== null) {
      this.angleRing.push(mainValue)
      if (this.angleRing.length > 21) this.angleRing.shift()
    }
    const symmetryDiff = this.onlineSymmetry(smoothed)

    const tickRes = this.machine.tick({
      angle: mainValue,
      frameIndex,
      tSec,
      switchable: smoothed.valid && gated.quality !== 'BAD'
    })
    if (tickRes.closedCycle && !tickRes.closedCycle.ignored) {
      const verdict = this.validator.validate(tickRes.closedCycle)
      if (verdict.matched) {
        this.consecutiveMismatch = 0
      } else {
        this.consecutiveMismatch++
        if (this.consecutiveMismatch >= MISMATCH_ADVICE_CYCLES) this.mismatchAdvice = true
      }
    }

    const items = this.corrections.update({
      frameIndex,
      tSec,
      phase: this.machine.currentPhase,
      valid: smoothed.valid,
      angles,
      symmetryDiff,
      jitterDeg: this.onlineJitter(),
      mismatchPulse: tickRes.mismatchPulse
    })

    return {
      mode: 'TRACKING',
      entryIssue: 'NONE',
      coreVisibleRatio: gated.coreHighRatio,
      broken: this.brokenRun > 0,
      phase: this.machine.currentPhase,
      totalReps: this.machine.totalCompleted,
      correctReps: this.liveCorrectReps(),
      targetReps: this.snapshot.targetReps,
      progressPercent: this.snapshot.targetReps > 0
        ? Math.min(100, Math.round((this.liveCorrectReps() / this.snapshot.targetReps) * 100))
        : 0,
      mainAngle: mainValue,
      frame: smoothed.frame,
      present: smoothed.present,
      repaired: smoothed.repaired,
      corrections: items,
      mismatchAdvice: this.mismatchAdvice,
      fps: this.liveFps()
    }
  }

  /** 结束训练：非因果确定性复算并生成汇总 */
  finish(): SessionResult {
    if (!this.corrections) throw new Error('引擎尚未初始化')
    const fps = this.liveFps()
    if (this.rawFrames.length < 15 || !this.detector) {
      return {
        summary: {
          totalScore: 0,
          totalReps: 0,
          correctReps: 0,
          validFrameRatio: 0,
          poseRepairedRatio: this.rawFrames.length === 0 ? 0 : 1,
          poseQuality: 'INSUFFICIENT',
          scoreDetail: { amplitude: 0, hold: 0, rhythm: 0, stability: 0, symmetry: 0, similarity: 0 },
          poseEngineId: this.conf.poseEngineId
        },
        scoredCycles: [],
        errorSegments: [],
        cycles: [],
        fps,
        rawFrameCount: this.rawFrames.length,
        mismatchAdvice: this.mismatchAdvice,
        mismatchReason: MISMATCH_REASON
      }
    }

    // 1) 非因果平滑修复（唯一评分口径）
    const repaired = repairSequence(this.rawFrames, this.conf)

    // 2) 角度轨道（tSec 从 0 开始）
    const tracks = this.extractor!.buildTracks(repaired.frames, repaired.present, repaired.valid, this.evaluators, 0)
    const mainTrack = tracks.get(this.main.key)!

    // 3) 状态机确定性重放
    const machine = new RepStateMachine(this.conf, this.machineParams)
    for (let i = 0; i < repaired.frames.length; i++) {
      machine.tick({
        angle: mainTrack.series[i],
        frameIndex: i,
        tSec: mainTrack.timesSec[i],
        switchable: repaired.valid[i]
      })
    }
    const cycles = machine.getCycles()
    const verdicts = cycles.map((c) => this.validator.validate(c))
    const correctReps = verdicts.filter((v) => v.matched).length
    const mismatchCycles = verdicts.filter((v) => !v.matched).length

    // 4) 左右对称差序列
    const symmetryDiff = this.extractor!.symmetrySeries(
      repaired.frames,
      repaired.present,
      repaired.valid,
      this.main.jointName
    )

    // 5) 纠错区间确定性重放
    const queue = new CorrectionQueue(this.snapshot.rules, this.conf)
    const jitterSeries = this.offlineJitter(mainTrack.series)
    for (let i = 0; i < repaired.frames.length; i++) {
      const angles = new Map<string, { value: number; speed: number | null }>()
      for (const ev of this.evaluators) {
        const tr = tracks.get(ev.key)
        if (!tr || tr.series[i] === null) continue
        angles.set(ev.key, { value: tr.series[i] as number, speed: tr.speed[i] })
      }
      queue.update({
        frameIndex: i,
        tSec: mainTrack.timesSec[i],
        phase: machine.trace[i] ?? 'READY',
        valid: repaired.valid[i],
        angles,
        symmetryDiff: symmetryDiff?.[i] ?? null,
        jitterDeg: jitterSeries[i] ?? 0,
        mismatchPulse: machine.pulseTrace[i] ?? false
      })
    }
    const lastT = mainTrack.timesSec[mainTrack.timesSec.length - 1] ?? 0
    const errorSegments = queue.finish(lastT)

    // 6) 六维评分
    const result = scoreSession({
      cycles,
      verdicts,
      mainTrack,
      symmetryDiff,
      snapshot: this.snapshot,
      validFrameRatio: repaired.validFrameRatio,
      poseRepairedRatio: repaired.poseRepairedRatio,
      totalFrames: repaired.frames.length,
      fps,
      engineId: this.detector.engineId
    })

    // 实时计数以最终复算为准（可复现性）
    this.machine = machine
    this.corrections = queue

    return {
      summary: result.summary,
      scoredCycles: result.scoredCycles,
      errorSegments,
      cycles,
      fps,
      rawFrameCount: this.rawFrames.length,
      mismatchAdvice: mismatchCycles >= MISMATCH_ADVICE_CYCLES || this.mismatchAdvice,
      mismatchReason: MISMATCH_REASON
    }
  }

  dispose(): void {
    this.detector?.dispose()
    this.detector = null
  }

  // ------------------------------------------------------------------
  // 内部工具
  // ------------------------------------------------------------------

  private pickMainEvaluator(): AngleEvaluator {
    const primary = this.evaluators.find((e) => e.rule.jointRole === 'PRIMARY' && (e.rule.dimension === 'ANGLE' || e.rule.dimension === 'HOLD'))
      ?? this.evaluators.find((e) => e.rule.dimension === 'ANGLE')
      ?? this.evaluators[0]
    if (primary) return primary

    // 无可用规则：用签名 targetJoints[0] 合成评估器
    const joint = this.snapshot.signature?.targetJoints?.[0]
    if (joint) {
      const stub: ParsedRule = {
        jointName: joint,
        keypointIndices: '',
        angleFormula: '',
        jointRole: 'PRIMARY',
        dimension: 'ANGLE',
        phase: 'FULL',
        standardAngle: undefined,
        scoreWeight: 1,
        severity: 2,
        promptText: '请按目标轨迹完成动作'
      }
      const ev = buildEvaluator(stub, `joint-${joint}`)
      if (ev) return ev
    }
    throw new Error('参数快照缺少可计算的角度规则，无法启动训练（请联系治疗师检查动作版本配置）')
  }

  private findMainRule(): ParsedRule {
    return this.main.rule
  }

  private buildSymmetryPair(): { left: AngleEvaluator; right: AngleEvaluator } | null {
    const key = symmetryKey(this.main.jointName)
    if (!key) return null
    const pair = SYMMETRY_PAIRS[key]
    const mk = (triplet: readonly number[], side: string): AngleEvaluator => ({
      key: `sym-${side}-${key}`,
      jointName: `${side}${key}`,
      kind: 'innerAngle',
      indices: [...triplet],
      rule: { ...this.main.rule, dimension: 'SYMMETRY' }
    })
    return { left: mk(pair.left, 'left'), right: mk(pair.right, 'right') }
  }

  private onlineSpeed(key: string, value: number, tSec: number): number | null {
    const st = this.speedState.get(key)
    if (!st) {
      this.speedState.set(key, { prevAngle: value, prevT: tSec, ema: 0, init: false })
      return null
    }
    const dt = tSec - st.prevT
    st.prevT = tSec
    if (dt <= 1e-6) return st.init ? st.ema : null
    const raw = (value - st.prevAngle) / dt
    st.prevAngle = value
    if (!st.init) {
      st.ema = raw
      st.init = true
    } else {
      st.ema = 0.7 * st.ema + 0.3 * raw
    }
    return st.ema
  }

  private onlineSymmetry(smoothed: SmoothedFrame): number | null {
    if (!this.symmetryPair) return null
    const a = this.extractor!.angle(smoothed.frame, this.symmetryPair.left, smoothed.present)
    const b = this.extractor!.angle(smoothed.frame, this.symmetryPair.right, smoothed.present)
    if (a === null || b === null) return null
    return Math.abs(a - b)
  }

  private onlineJitter(): number {
    if (this.angleRing.length < 8) return 0
    const w = this.conf.smoothWindowSpeed
    const half = w >> 1
    let acc = 0
    let n = 0
    for (let i = half; i < this.angleRing.length - half; i++) {
      let s = 0
      for (let j = i - half; j <= i + half; j++) s += this.angleRing[j]
      const base = s / (half * 2 + 1)
      acc += (this.angleRing[i] - base) ** 2
      n++
    }
    return n > 0 ? Math.sqrt(acc / n) : 0
  }

  private offlineJitter(series: (number | null)[]): number[] {
    const filled = new Array<number>(series.length).fill(0)
    let last = 0
    series.forEach((v, i) => {
      if (v !== null) last = v
      filled[i] = last
    })
    const base = centeredMovingAverage(filled, this.conf.smoothWindowSpeed)
    return filled.map((v, i) => Math.abs(v - base[i]))
  }

  private liveCorrectReps(): number {
    // 在线口径：通过基础门槛的周期中再经签名校验的计数（与结束复算同源逻辑）
    let correct = 0
    if (!this.machine) return 0
    for (const c of this.machine.getCycles()) {
      if (c.basicValid && this.validator.validate(c).matched) correct++
    }
    return correct
  }

  private liveFps(): number {
    const n = this.rawFrames.length
    if (n < 3) return 0
    const dts: number[] = []
    for (let i = Math.max(1, n - 30); i < n; i++) {
      const d = this.rawFrames[i].timestamp - this.rawFrames[i - 1].timestamp
      if (d > 0) dts.push(d)
    }
    if (dts.length === 0) return 0
    dts.sort((a, b) => a - b)
    const med = dts[dts.length >> 1]
    return Math.round((1000 / med) * 10) / 10
  }
}

// 供训练页绘制骨架/角度复用
export { angleAt, kpPoint }
export { MISMATCH_REASON }
