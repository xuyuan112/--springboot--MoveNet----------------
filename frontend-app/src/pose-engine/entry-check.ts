/**
 * 入场质量检查与 ROI 跟踪（P3-F2，需求 6.3.2）。
 *
 * - 单人入镜检查：MoveNet 为单姿态模型，无法枚举第二人；采用工程启发式——
 *   主体丢失、人体外接框/髋中心突变（双人交替主导时典型表现）判为 MULTI_OR_JUMP；
 * - 通过后以首个稳定骨架的髋中心/肩宽/外接框为锚点；
 * - 训练中每 30 帧按外接框外扩 20% 更新 ROI；
 * - 连续性不满足（连续若干帧丢失或跳变）时通知上层暂停评分并重新入场检查。
 */
import type { AlgoConfig, PoseFrame } from './types'
import { CORE_INDICES, KP, clamp, dist, keypointBox, midpoint } from './geometry'
import type { Box } from './geometry'
import type { NormRect } from './movenet'

export type EntryIssue =
  | 'NONE' // 通过/稳定
  | 'NO_SUBJECT' // 主体丢失或关键点不足
  | 'TOO_FAR' // 人体占画面过小
  | 'TOO_CLOSE' // 人体超出画面过多
  | 'MULTI_OR_JUMP' // 疑似第二人或主体跳变
  | 'WARMUP' // 已有人体但稳定帧未攒够

export interface EntryResult {
  passed: boolean
  issue: EntryIssue
  /** 核心关键点高置信占比 0~1 */
  coreVisibleRatio: number
  /** 人体外接框（相对全帧归一化） */
  box: Box | null
  /** 连续稳定帧数 */
  stableFrames: number
  /** 通过后输出的初始 ROI */
  roi: NormRect | null
}

/** 入场判定参数（与 AlgoConfig 解耦，属视觉采集约束） */
const ENTRY = {
  /** 需要连续稳定的帧数（约 0.5~1s） */
  REQUIRED_STABLE_FRAMES: 12,
  /** 人体外接框高度占画面下限（站位引导要求 60%~90%，算法侧放宽至 45%） */
  BOX_HEIGHT_MIN: 0.45,
  /** 人体外接框高度占画面上限（允许略超，关键关节不能出画） */
  BOX_HEIGHT_MAX: 1.05,
  /** 髋中心帧间跳变阈值（相对全帧） */
  HIP_JUMP: 0.18,
  /** 外接框尺寸/位置突变比例 */
  BOX_JUMP: 0.35,
  /** ROI 更新间隔（帧） */
  ROI_UPDATE_INTERVAL: 30,
  /** ROI 外扩比例 */
  ROI_EXPAND: 1.2,
  /** 训练中连续异常帧数，超过则触发重新入场 */
  BREAK_FRAMES: 8
} as const

function boxToRect(box: Box, expand: number): NormRect {
  const cx = box.x + box.w / 2
  const cy = box.y + box.h / 2
  const side = Math.max(box.w, box.h) * expand
  const x = clamp(cx - side / 2, 0, 1)
  const y = clamp(cy - side / 2, 0, 1)
  const w = clamp(side, 0, 1 - x)
  const h = clamp(side, 0, 1 - y)
  return { x, y, w, h }
}

export class EntryQualityChecker {
  private readonly conf: AlgoConfig
  private stableRun = 0
  private lastHip: { x: number; y: number } | null = null
  private lastBox: Box | null = null
  private anchorBox: Box | null = null
  private roi: NormRect | null = null
  private framesSinceRoi = 0

  constructor(conf: AlgoConfig) {
    this.conf = conf
  }

  reset(): void {
    this.stableRun = 0
    this.lastHip = null
    this.lastBox = null
    this.anchorBox = null
    this.roi = null
    this.framesSinceRoi = 0
  }

  get currentRoi(): NormRect | null {
    return this.roi
  }

  private coreRatio(frame: PoseFrame): number {
    let ok = 0
    for (const i of CORE_INDICES) {
      if (frame.keypoints[i].score >= this.conf.confHigh) ok++
    }
    return ok / CORE_INDICES.length
  }

  /**
   * 入场阶段逐帧检查。
   * 以未使用 ROI 的全帧推理结果调用；攒够连续稳定帧后 passed=true。
   */
  evaluate(frame: PoseFrame): EntryResult {
    const ratio = this.coreRatio(frame)
    const box = keypointBox(frame, CORE_INDICES, this.conf.confHigh)

    if (!box || ratio < this.conf.validFrameRatioMin) {
      this.stableRun = 0
      this.lastHip = null
      this.lastBox = null
      return { passed: false, issue: 'NO_SUBJECT', coreVisibleRatio: ratio, box, stableFrames: 0, roi: null }
    }

    if (box.h < ENTRY.BOX_HEIGHT_MIN) {
      this.stableRun = 0
      return { passed: false, issue: 'TOO_FAR', coreVisibleRatio: ratio, box, stableFrames: this.stableRun, roi: null }
    }
    if (box.h > ENTRY.BOX_HEIGHT_MAX || box.y < 0.005 || box.y + box.h > 0.995) {
      // 框顶/框底贴边且超高，大概率脚或头出画
      this.stableRun = 0
      return { passed: false, issue: 'TOO_CLOSE', coreVisibleRatio: ratio, box, stableFrames: this.stableRun, roi: null }
    }

    const hip = midpoint(frame.keypoints[KP.L_HIP], frame.keypoints[KP.R_HIP])
    if (this.lastHip && this.lastBox) {
      const hipJump = dist(hip, this.lastHip)
      const sizeJump =
        Math.abs(box.w - this.lastBox.w) / Math.max(this.lastBox.w, 1e-6) +
        Math.abs(box.h - this.lastBox.h) / Math.max(this.lastBox.h, 1e-6)
      const centerJump =
        Math.abs(box.x + box.w / 2 - (this.lastBox.x + this.lastBox.w / 2)) +
        Math.abs(box.y + box.h / 2 - (this.lastBox.y + this.lastBox.h / 2))
      if (hipJump > ENTRY.HIP_JUMP || sizeJump > ENTRY.BOX_JUMP || centerJump > ENTRY.BOX_JUMP) {
        this.stableRun = 0
        this.lastHip = hip
        this.lastBox = box
        return { passed: false, issue: 'MULTI_OR_JUMP', coreVisibleRatio: ratio, box, stableFrames: 0, roi: null }
      }
    }

    this.stableRun++
    this.lastHip = hip
    this.lastBox = box

    if (this.stableRun < ENTRY.REQUIRED_STABLE_FRAMES) {
      return { passed: false, issue: 'WARMUP', coreVisibleRatio: ratio, box, stableFrames: this.stableRun, roi: null }
    }

    if (!this.anchorBox) this.anchorBox = box
    if (!this.roi) this.roi = boxToRect(this.anchorBox, ENTRY.ROI_EXPAND)
    return { passed: true, issue: 'NONE', coreVisibleRatio: ratio, box, stableFrames: this.stableRun, roi: this.roi }
  }

  /**
   * 训练中连续性监控 + ROI 周期更新。
   * @returns break=true 时上层应暂停评分并重新入场；updated 表示 ROI 本帧被刷新
   */
  track(frame: PoseFrame): { break: boolean; issue: EntryIssue; updated: boolean; ratio: number } {
    const ratio = this.coreRatio(frame)
    const box = keypointBox(frame, CORE_INDICES, this.conf.confHigh)
    let updated = false

    if (!box || ratio < this.conf.validFrameRatioMin) {
      return { break: true, issue: 'NO_SUBJECT', updated, ratio }
    }
    const hip = midpoint(frame.keypoints[KP.L_HIP], frame.keypoints[KP.R_HIP])
    if (this.lastHip) {
      const jump = dist(hip, this.lastHip)
      if (jump > ENTRY.HIP_JUMP) {
        return { break: true, issue: 'MULTI_OR_JUMP', updated, ratio }
      }
    }

    this.lastHip = hip
    this.framesSinceRoi++
    if (this.framesSinceRoi >= ENTRY.ROI_UPDATE_INTERVAL) {
      // 指数平滑外接框，避免 ROI 抖动
      const prev = this.anchorBox ?? box
      const smooth: Box = {
        x: prev.x * 0.6 + box.x * 0.4,
        y: prev.y * 0.6 + box.y * 0.4,
        w: prev.w * 0.6 + box.w * 0.4,
        h: prev.h * 0.6 + box.h * 0.4
      }
      this.anchorBox = smooth
      this.roi = boxToRect(smooth, ENTRY.ROI_EXPAND)
      this.framesSinceRoi = 0
      updated = true
    }
    return { break: false, issue: 'NONE', updated, ratio }
  }

  /** 连续异常计数由上层维护，暴露阈值供其判断 */
  static get BREAK_FRAMES(): number {
    return ENTRY.BREAK_FRAMES
  }
}
