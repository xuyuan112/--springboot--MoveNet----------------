/**
 * 六维加权评分 + DTW 相似度（P3-F7，需求 6.3.9/6.3.12）。
 *
 * 六维：幅度 amplitude / 保持 hold / 节奏 rhythm / 稳定 stability / 对称 symmetry / 相似度 similarity。
 * - 仅对通过基础门槛 + 动作签名校验的周期打分；
 * - 关键帧高斯时间窗加权（timeWeightSigma，秒）；模板缺失时相似度自动降权为 0，不阻塞；
 * - 总分 = Σ(w·S)/Σw（权重不归一时自动归一）；
 * - INSUFFICIENT（有效帧占比不足或修复比例超限）记录但不计分（六维与总分置 0）。
 */
import type { AlgoConfig, PoseQuality, ScoreDetail, TrainingSummary } from './types'
import type { AngleTrack } from './features'
import type { ParsedSnapshot } from './snapshot'
import type { RawCycle } from './state-machine'
import { clamp, mean, resample, zNormalize } from './geometry'

/** 已通过签名校验的周期（带校验结论） */
export interface ScoredCycle {
  cycle: RawCycle
  /** 签名未通过原因（空=通过） */
  mismatchReasons: string[]
  scores: {
    amplitude: number
    hold: number
    rhythm: number
    stability: number
    symmetry: number
    similarity: number | null
  }
  /** 周期内逐帧主角度（用于详情回放） */
  angleSlice: (number | null)[]
  timeSlice: number[]
}

export interface ScoreInput {
  cycles: RawCycle[]
  /** 与 cycles 同序的签名结论：reasons 为空表示通过 */
  verdicts: Array<{ matched: boolean; reasons: string[] }>
  mainTrack: AngleTrack
  symmetryDiff: (number | null)[] | null
  snapshot: ParsedSnapshot
  validFrameRatio: number
  poseRepairedRatio: number
  totalFrames: number
  /** 实际采集帧率（帧/秒） */
  fps: number
  engineId: string
}

export interface ScoreResult {
  summary: TrainingSummary
  scoredCycles: ScoredCycle[]
  totalReps: number
  correctReps: number
}

const ZERO: ScoreDetail = { amplitude: 0, hold: 0, rhythm: 0, stability: 0, symmetry: 0, similarity: 0 }

/** 经典 DTW 距离（窗口约束后按路径长度归一） */
export function dtwDistance(a: number[], b: number[], windowRatio = 0.2): number {
  const n = a.length
  const m = b.length
  if (n === 0 || m === 0) return 1
  const w = Math.max(1, Math.floor(Math.max(n, m) * windowRatio))
  const INF = Number.POSITIVE_INFINITY
  const cost = Array.from({ length: n + 1 }, () => new Float64Array(m + 1).fill(INF))
  cost[0][0] = 0
  for (let i = 1; i <= n; i++) {
    const lo = Math.max(1, i - w)
    const hi = Math.min(m, i + w)
    for (let j = lo; j <= hi; j++) {
      const d = Math.abs(a[i - 1] - b[j - 1])
      cost[i][j] = d + Math.min(cost[i - 1][j], cost[i][j - 1], cost[i - 1][j - 1])
    }
  }
  return cost[n][m] / (n + m)
}

/** 相似度得分 0~100（z 归一化序列 DTW 距离映射） */
function similarityScore(cycleSlice: (number | null)[], snapshot: ParsedSnapshot): number | null {
  const seq = snapshot.template?.featureSequence
  if (!seq || seq.length === 0) return null
  const observed = resample(
    cycleSlice.filter((v): v is number => v !== null),
    48
  )
  if (observed.length < 8) return null
  // 模板为多维时取第 0 维（主关节角），整体为一维时直接使用
  const template1d = seq[0].length > 1 ? seq.map((row) => row[0]) : seq.map((row) => row[0])
  const template = resample(template1d, 48)
  const d = dtwDistance(zNormalize(observed), zNormalize(template))
  // z 归一距离 0→100 分，~1 距离≈50 分
  return clamp(100 / (1 + d), 0, 100)
}

function sliceRange<T>(arr: T[], lo: number, hi: number): T[] {
  return arr.slice(clamp(lo, 0, arr.length - 1), clamp(hi + 1, 0, arr.length))
}

export function scoreSession(input: ScoreInput): ScoreResult {
  const conf = input.snapshot.algoConfig
  const sig = input.snapshot.signature ?? null

  // 期望单周期时长（秒）：签名区间中点，缺失时取已观测有效周期中位数
  const scored: ScoredCycle[] = []
  const validCycles: RawCycle[] = []
  input.cycles.forEach((cycle, i) => {
    const verdict = input.verdicts[i] ?? { matched: true, reasons: [] as string[] }
    if (!cycle.ignored && cycle.basicValid && verdict.matched) validCycles.push(cycle)
    scored.push({
      cycle,
      mismatchReasons: verdict.matched ? [] : verdict.reasons,
      scores: { amplitude: 0, hold: 0, rhythm: 0, stability: 0, symmetry: 0, similarity: null },
      angleSlice: [],
      timeSlice: []
    })
  })

  const validDurations = validCycles.map((c) => c.durationSec).filter((d) => d > 0)
  validDurations.sort((a, b) => a - b)
  const medianDuration = validDurations.length
    ? validDurations[validDurations.length >> 1]
    : (input.snapshot.durationSec ?? 2)
  const expectedDuration =
    sig?.durationRangeSec && sig.durationRangeSec[0] != null && sig.durationRangeSec[1] != null
      ? (sig.durationRangeSec[0] + sig.durationRangeSec[1]) / 2
      : medianDuration
  const rhythmSigma = Math.max(0.25 * expectedDuration, 0.4)

  // 周期间节拍稳定性：有效周期时长的变异系数 CV（标准差/均值）。
  // 与“对期望时长的吻合度”各占节奏分 40%/60%；周期不足 2 个时不评稳定性。
  let rhythmRegularity: number | null = null
  if (validDurations.length >= 2) {
    const m = mean(validDurations)
    if (m > 0) {
      const cv = Math.sqrt(mean(validDurations.map((d) => (d - m) ** 2))) / m
      rhythmRegularity = 100 * Math.exp(-((cv / 0.2) ** 2))
    }
  }

  // 幅度目标：计划目标幅度 > 签名峰值 ROM > 签名 minRom > 周期中位数
  const amplitudeTarget =
    input.snapshot.targetAmplitude ??
    (sig?.peakAngleRange && sig?.startAngleRange
      ? Math.abs(((sig.peakAngleRange[0] ?? 90) + (sig.peakAngleRange[1] ?? 90)) / 2 -
          ((sig.startAngleRange[0] ?? 0) + (sig.startAngleRange[1] ?? 0)) / 2)
      : sig?.minRom) ??
    (validCycles.length ? median(validCycles.map((c) => c.rom)) : 30)

  const holdRequiredSec = (primaryHoldFrames(input) ?? 0) / input.fps

  // 关键帧高斯时间窗（映射模板关键帧到本段时长）
  const sessionSec = input.mainTrack.timesSec[input.mainTrack.timesSec.length - 1] ?? 0
  const keyTimes = resolveKeyTimes(input.snapshot, sessionSec)

  validCycles.forEach((cycle) => {
    const idx = input.cycles.indexOf(cycle)
    const item = scored[idx]
    const angleSlice = sliceRange(input.mainTrack.series, cycle.startFrame, cycle.endFrame)
    const timeSlice = sliceRange(input.mainTrack.timesSec, cycle.startFrame, cycle.endFrame)
    item.angleSlice = angleSlice
    item.timeSlice = timeSlice

    // 1) 幅度（达标即满分，超出不额外加分；峰值落入签名目标区间直接满分）
    const ampRatio = amplitudeTarget > 0 ? cycle.rom / amplitudeTarget : 1
    let amplitude = clamp(ampRatio, 0, 1) * 100
    if (sig?.peakAngleRange) {
      const [lo, hi] = sig.peakAngleRange
      if ((lo == null || cycle.peakAngle >= lo) && (hi == null || cycle.peakAngle <= hi)) amplitude = 100
    }
    item.scores.amplitude = round1(amplitude)

    // 2) 保持
    const holdSec = cycle.maxHoldFrames / input.fps
    item.scores.hold = round1(
      holdRequiredSec > 0 ? clamp(holdSec / holdRequiredSec, 0, 1) * 100 : cycle.hadHold ? 100 : 85
    )

    // 3) 节奏 = 60% 对期望时长的高斯吻合 + 40% 周期间节拍一致性（CV）
    const rhythmFit = 100 * Math.exp(-((cycle.durationSec - expectedDuration) ** 2) / (2 * rhythmSigma * rhythmSigma))
    const rhythm = rhythmRegularity === null ? rhythmFit : 0.6 * rhythmFit + 0.4 * rhythmRegularity
    item.scores.rhythm = round1(rhythm)

    // 4) 稳定：周期内高频抖动 + 末端掉幅
    const nums = angleSlice.filter((v): v is number => v !== null)
    const stability = nums.length >= 4
      ? 100 * Math.exp(-jitterRms(nums) / 8)
      : 80
    item.scores.stability = round1(clamp(stability, 0, 100))

    // 5) 对称
    if (input.symmetryDiff) {
      const symSlice = sliceRange(input.symmetryDiff, cycle.startFrame, cycle.endFrame).filter(
        (v): v is number => v !== null
      )
      if (symSlice.length >= 4) {
        item.scores.symmetry = round1(clamp(100 * Math.exp(-mean(symSlice) / 15), 0, 100))
      }
    }

    // 6) 相似度（模板缺失自动 null → 权重置 0）
    item.scores.similarity = similarityScore(angleSlice, input.snapshot)
  })

  // ===== 聚合（关键帧高斯窗 + 六维权重） =====
  const quality = classifyQuality(conf, input.validFrameRatio, input.poseRepairedRatio)
  const insufficient = quality === 'INSUFFICIENT'

  const weights = {
    amplitude: conf.weightAmplitude,
    hold: conf.weightHold,
    rhythm: conf.weightRhythm,
    stability: conf.weightStability,
    symmetry: conf.weightSymmetry,
    similarity: validCycles.some((c) => scored[input.cycles.indexOf(c)].scores.similarity !== null)
      ? conf.weightSimilarity
      : 0,
    hasSymmetry: input.symmetryDiff !== null
  }
  if (!weights.hasSymmetry) weights.symmetry = 0

  // 每个周期的关键帧高斯时间权重（按周期中点绝对时间）
  const weightByIdx = input.cycles.map((cycle) => {
    const times = input.mainTrack.timesSec
    const midFrame = (cycle.startFrame + cycle.endFrame) / 2
    const lo = Math.floor(midFrame)
    const hi = Math.ceil(midFrame)
    const midSec =
      times[lo] != null && times[hi] != null
        ? (times[lo] + times[hi]) / 2
        : (times[clamp(lo, 0, times.length - 1)] ?? 0)
    return cycleTimeWeight(midSec, keyTimes, conf.timeWeightSigma)
  })

  const dimAvg = (pick: (s: ScoredCycle['scores']) => number | null): number | null => {
    let acc = 0
    let wsum = 0
    input.cycles.forEach((cycle, ci) => {
      if (!validCycles.includes(cycle)) return
      const v = pick(scored[ci].scores)
      if (v === null) return
      const w = weightByIdx[ci]
      acc += v * w
      wsum += w
    })
    return wsum > 0 ? acc / wsum : null
  }

  const detail: ScoreDetail = { ...ZERO }
  if (!insufficient && validCycles.length > 0) {
    const amp = dimAvg((s) => s.amplitude)
    const hold = dimAvg((s) => s.hold)
    const rhythm = dimAvg((s) => s.rhythm)
    const stability = dimAvg((s) => s.stability)
    const symmetry = dimAvg((s) => s.symmetry)
    const similarity = dimAvg((s) => s.similarity)
    detail.amplitude = round1(amp ?? 0)
    detail.hold = round1(hold ?? 0)
    detail.rhythm = round1(rhythm ?? 0)
    detail.stability = round1(stability ?? 0)
    detail.symmetry = round1(symmetry ?? 0)
    detail.similarity = round1(similarity ?? 0)
  }

  const effectiveWeightSum =
    weights.amplitude + weights.hold + weights.rhythm + weights.stability + weights.symmetry + weights.similarity
  let total = 0
  if (!insufficient && effectiveWeightSum > 0 && validCycles.length > 0) {
    total =
      (detail.amplitude * weights.amplitude +
        detail.hold * weights.hold +
        detail.rhythm * weights.rhythm +
        detail.stability * weights.stability +
        detail.symmetry * weights.symmetry +
        detail.similarity * weights.similarity) /
      effectiveWeightSum
  }

  const totalReps = input.cycles.filter((c) => !c.ignored).length
  const correctReps = validCycles.length

  const summary: TrainingSummary = {
    totalScore: insufficient ? 0 : round1(clamp(total, 0, 100)),
    totalReps,
    correctReps,
    validFrameRatio: round3(input.validFrameRatio),
    poseRepairedRatio: round3(input.poseRepairedRatio),
    poseQuality: insufficient ? 'INSUFFICIENT' : quality,
    scoreDetail: insufficient ? { ...ZERO } : detail,
    poseEngineId: input.engineId
  }

  return { summary, scoredCycles: scored, totalReps, correctReps }
}

function primaryHoldFrames(input: ScoreInput): number | undefined {
  const primary = input.snapshot.rules.find(
    (r) => r.jointRole === 'PRIMARY' && (r.dimension === 'ANGLE' || r.dimension === 'HOLD')
  )
  return primary?.holdFramesRequired
}

function resolveKeyTimes(snapshot: ParsedSnapshot, sessionSec: number): number[] | null {
  const tpl = snapshot.template
  if (!tpl || tpl.keyframeIndex.length === 0 || !(tpl.frameRate > 0) || !(sessionSec > 0)) return null
  const tplDuration = tpl.frameCount > 0 ? tpl.frameCount / tpl.frameRate : 0
  if (!(tplDuration > 0)) return null
  return tpl.keyframeIndex.map((k) => (k / tpl.frameRate / tplDuration) * sessionSec)
}

function cycleTimeWeight(midSec: number, keyTimes: number[] | null, sigma: number): number {
  if (!keyTimes || keyTimes.length === 0) return 1
  let w = 0
  for (const kt of keyTimes) {
    w = Math.max(w, Math.exp(-((midSec - kt) ** 2) / (2 * sigma * sigma)))
  }
  return Math.max(w, 0.2)
}

function classifyQuality(conf: AlgoConfig, validFrameRatio: number, repairedRatio: number): PoseQuality {
  if (validFrameRatio < conf.validFrameRatioMin || repairedRatio > conf.repairRatioLimit) return 'INSUFFICIENT'
  if (validFrameRatio >= 0.9 && repairedRatio <= 0.1) return 'GOOD'
  return 'FAIR'
}

function jitterRms(vals: number[]): number {
  if (vals.length < 3) return 0
  // 三点滑动平均作为基线，残差 RMS
  const base = vals.map((_, i) => {
    const lo = Math.max(0, i - 1)
    const hi = Math.min(vals.length - 1, i + 1)
    let s = 0
    let n = 0
    for (let j = lo; j <= hi; j++) {
      s += vals[j]
      n++
    }
    return s / n
  })
  return Math.sqrt(mean(vals.map((v, i) => (v - base[i]) ** 2)))
}

function median(xs: number[]): number {
  if (xs.length === 0) return 0
  const s = [...xs].sort((a, b) => a - b)
  const mid = s.length >> 1
  return s.length % 2 ? s[mid] : (s[mid - 1] + s[mid]) / 2
}

function round1(v: number): number {
  return Math.round(v * 10) / 10
}

function round3(v: number): number {
  return Math.round(v * 1000) / 1000
}
