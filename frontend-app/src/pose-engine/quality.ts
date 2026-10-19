/**
 * 单帧质量门控（P3-F3，需求 6.3.3）。
 *
 * 三级置信度：
 * - HIGH（>= confHigh）：直接采纳；
 * - MID（confLow ~ confHigh）：采纳但计入中等质量；
 * - LOW（< confLow）：判遮挡/缺失，交平滑修复模块处理，不直接参与角度计算。
 */
import type { AlgoConfig, PoseFrame } from './types'
import { CORE_INDICES } from './geometry'

export type GateLevel = 'HIGH' | 'MID' | 'LOW'

/** 帧质量等级（区别于汇总 PoseQuality，这里只描述单帧可用性） */
export type FrameQuality = 'GOOD' | 'FAIR' | 'BAD'

export interface GatedFrame {
  frame: PoseFrame
  /** 每个关键点的置信等级 */
  levels: GateLevel[]
  /** 每个关键点是否缺失（LOW） */
  missing: boolean[]
  /** 核心关键点中 MID 及以上占比 */
  coreUsableRatio: number
  /** 核心关键点 HIGH 占比 */
  coreHighRatio: number
  /** 帧质量：BAD 帧不参与状态切换与评分 */
  quality: FrameQuality
}

export class QualityGate {
  private readonly conf: AlgoConfig

  constructor(conf: AlgoConfig) {
    this.conf = conf
  }

  gate(frame: PoseFrame): GatedFrame {
    const levels: GateLevel[] = new Array(17)
    const missing: boolean[] = new Array(17)
    let usable = 0
    let high = 0
    for (let i = 0; i < 17; i++) {
      const s = frame.keypoints[i].score
      let level: GateLevel
      if (s >= this.conf.confHigh) {
        level = 'HIGH'
        high++
        usable++
      } else if (s >= this.conf.confLow) {
        level = 'MID'
        usable++
      } else {
        level = 'LOW'
      }
      levels[i] = level
      missing[i] = level === 'LOW'
    }

    let coreUsable = 0
    let coreHigh = 0
    for (const i of CORE_INDICES) {
      if (levels[i] !== 'LOW') coreUsable++
      if (levels[i] === 'HIGH') coreHigh++
    }
    const coreUsableRatio = coreUsable / CORE_INDICES.length
    const coreHighRatio = coreHigh / CORE_INDICES.length

    const quality: FrameQuality =
      coreUsableRatio < this.conf.validFrameRatioMin
        ? 'BAD'
        : coreHighRatio >= 0.85
          ? 'GOOD'
          : 'FAIR'

    return { frame, levels, missing, coreUsableRatio, coreHighRatio, quality }
  }
}
