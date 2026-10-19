/**
 * 动作签名合规校验（P3-F5，需求 6.3.7）。
 *
 * 对状态机切出的每个周期按 motion_signature 校验
 * “起始（startAngleRange）→ 峰值（peakAngleRange）→ 结束 + 最小 ROM + 方向 + 时长 + 阶段顺序”。
 * 未通过周期记 ACTION_MISMATCH：不计次、不评分，仅提示
 * “未检测到当前训练任务对应的有效动作”；连续失败建议结束并联系治疗师。
 *
 * signature 缺失时不做合规拦截（降级为仅依赖状态机阈值）。
 */
import type { MotionSignature } from './types'
import type { RawCycle } from './state-machine'

export interface SignatureVerdict {
  matched: boolean
  reasons: string[]
}

export const MISMATCH_REASON = '未检测到当前训练任务对应的有效动作'

/** 连续 MISMATCH 周期数达到该值时，建议结束训练并联系治疗师 */
export const MISMATCH_ADVICE_CYCLES = 3

export class SignatureValidator {
  private readonly sig: MotionSignature | null

  constructor(signature: MotionSignature | null) {
    this.sig = signature
  }

  get available(): boolean {
    return this.sig !== null
  }

  validate(cycle: RawCycle): SignatureVerdict {
    if (!this.sig) return { matched: true, reasons: [] }
    const reasons: string[] = []
    const sig = this.sig

    const inRange = (v: number, range: [number | null, number | null] | undefined): boolean => {
      if (!range) return true
      const [lo, hi] = range
      if (lo != null && v < lo) return false
      if (hi != null && v > hi) return false
      return true
    }

    if (!inRange(cycle.startAngle, sig.startAngleRange)) {
      reasons.push(`起始角度 ${cycle.startAngle.toFixed(0)}° 不在规定区间`)
    }
    if (!inRange(cycle.peakAngle, sig.peakAngleRange)) {
      reasons.push(`峰值角度 ${cycle.peakAngle.toFixed(0)}° 不在目标区间`)
    }
    if (sig.minRom != null && cycle.rom < sig.minRom) {
      reasons.push(`活动幅度 ${cycle.rom.toFixed(0)}° 未达最小要求 ${sig.minRom}°`)
    }

    // 方向以区间形态推导为准（与状态机 buildParams 同一口径）：
    // 起止区间完备且与声明方向冲突时（治疗师误标），按峰值相对起始的升降判定。
    let direction = sig.expectedDirection
    if (direction && direction !== 'MIXED' && sig.startAngleRange && sig.peakAngleRange) {
      const [slo, shi] = sig.startAngleRange
      const [plo, phi] = sig.peakAngleRange
      if (slo != null && shi != null && plo != null && phi != null) {
        direction = (plo + phi) / 2 > (slo + shi) / 2 ? 'FLEX' : 'EXTEND'
      }
    }
    if (direction === 'FLEX' && cycle.peakAngle <= cycle.startAngle + 1e-6) {
      reasons.push('动作方向与签名不符（应为屈曲方向）')
    }
    if (direction === 'EXTEND' && cycle.peakAngle >= cycle.startAngle - 1e-6) {
      reasons.push('动作方向与签名不符（应为伸展方向）')
    }

    if (sig.durationRangeSec) {
      const [lo, hi] = sig.durationRangeSec
      if (lo != null && cycle.durationSec < lo) reasons.push('周期时长短于签名下限')
      if (hi != null && cycle.durationSec > hi) reasons.push('周期时长超过签名上限')
    }

    // 阶段顺序：状态机保证 READY→RAISING→(HOLD)→LOWERING→READY，
    // 缺 HOLD 仅在签名显式声明包含 HOLD 时判失败
    const order = sig.phaseOrder
    if (Array.isArray(order) && order.length > 0) {
      if (order.includes('HOLD') && !cycle.hadHold) {
        reasons.push('缺少 HOLD 保持阶段')
      }
    }

    return { matched: reasons.length === 0, reasons }
  }
}
