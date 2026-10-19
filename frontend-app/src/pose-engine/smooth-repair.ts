/**
 * 时序平滑与遮挡修复（P3-F3，需求 6.3.3/6.3.4）。
 *
 * - 中值滤波（medianWindow=3）+ EMA（α=emaAlpha=0.35）；
 * - 缺失 ≤ maxGapFrames：在线路径保持上一值（因果），汇总路径邻域线性插值；
 * - 末端全遮挡：骨长中位数 + 父关节方向几何约束纠正，
 *   仅当几何预测与朴素保持值偏差超过 boneLenTolerance×L 时才替换；
 * - 所有补点均计入 poseRepairedRatio；核心关键点补不齐的帧标记为无效帧。
 */
import type { AlgoConfig, PoseFrame } from './types'
import { KP, clamp, dist, median, midpoint } from './geometry'
import { QualityGate } from './quality'
import type { GatedFrame } from './quality'

/** 关键点 → 父关键点索引（基于运动链）；鼻子父节点为双肩中点（虚拟） */
const PARENT: ReadonlyArray<number | null> = [
  -1, // 0 nose：虚拟双肩中点
  0, // 1 left_eye
  0, // 2 right_eye
  1, // 3 left_ear
  2, // 4 right_ear
  11, // 5 left_shoulder -> left_hip（躯干侧骨）
  12, // 6 right_shoulder -> right_hip
  5, // 7 left_elbow
  6, // 8 right_elbow
  7, // 9 left_wrist
  8, // 10 right_wrist
  5, // 11 left_hip -> left_shoulder
  6, // 12 right_hip -> right_shoulder
  11, // 13 left_knee
  12, // 14 right_knee
  13, // 15 left_ankle
  14 // 16 right_ankle
]

/** 核心关键点（补不齐 → 无效帧） */
const CORE = [
  KP.L_SHOULDER, KP.R_SHOULDER, KP.L_ELBOW, KP.R_ELBOW,
  KP.L_HIP, KP.R_HIP, KP.L_KNEE, KP.R_KNEE
]

export interface SmoothedFrame {
  frame: PoseFrame
  /** 关键点是否有坐标（false=该点彻底缺失） */
  present: boolean[]
  /** 是否为补点（插值/几何纠正） */
  repaired: boolean[]
  /** 帧是否可用于角度计算与状态机 */
  valid: boolean
}

interface OnlineKpState {
  obsX: number[]
  obsY: number[]
  emaX: number
  emaY: number
  init: boolean
  gap: number
  bones: number[]
  dirX: number
  dirY: number
  dirInit: boolean
}

/** 在线（因果）平滑修复器，供实时计数/纠错使用 */
export class OnlineSmoother {
  private readonly conf: AlgoConfig
  private readonly states: OnlineKpState[]

  constructor(conf: AlgoConfig) {
    this.conf = conf
    this.states = Array.from({ length: 17 }, () => ({
      obsX: [],
      obsY: [],
      emaX: 0,
      emaY: 0,
      init: false,
      gap: 0,
      bones: [],
      dirX: 0,
      dirY: 0,
      dirInit: false
    }))
  }

  /**
   * 处理一帧门控后的姿态。
   * 注意：本方法会用平滑后坐标就地替换 gated.frame 的关键点坐标。
   */
  process(gated: GatedFrame): SmoothedFrame {
    const present = new Array<boolean>(17).fill(false)
    const repaired = new Array<boolean>(17).fill(false)
    const kps = gated.frame.keypoints

    // 鼻子依赖双肩中点，放在肩之后处理；其余均满足父索引 < 子索引
    const order = [1, 2, 3, 4, 5, 6, 11, 12, 7, 8, 13, 14, 9, 10, 15, 16, 0]

    for (const idx of order) {
      const st = this.states[idx]
      const level = gated.levels[idx]
      const raw = kps[idx]

      if (level !== 'LOW') {
        // 观测帧：中值滤波窗口取最近观测，再 EMA
        st.obsX.push(raw.x)
        st.obsY.push(raw.y)
        if (st.obsX.length > this.conf.medianWindow) {
          st.obsX.shift()
          st.obsY.shift()
        }
        const mx = median(st.obsX)
        const my = median(st.obsY)
        if (!st.init) {
          st.emaX = mx
          st.emaY = my
          st.init = true
        } else {
          st.emaX = (1 - this.conf.emaAlpha) * st.emaX + this.conf.emaAlpha * mx
          st.emaY = (1 - this.conf.emaAlpha) * st.emaY + this.conf.emaAlpha * my
        }
        st.gap = 0
        present[idx] = true

        // 更新骨长与父→子方向
        const parent = this.resolveParent(idx, present)
        if (parent) {
          const L = dist({ x: st.emaX, y: st.emaY }, parent)
          if (L > 1e-6) {
            st.bones.push(L)
            if (st.bones.length > 90) st.bones.shift()
            st.dirX = (st.emaX - parent.x) / L
            st.dirY = (st.emaY - parent.y) / L
            st.dirInit = true
          }
        }
      } else {
        // 缺失帧：缺口内先尝试几何纠正，再退化为保持
        st.gap++
        if (st.gap <= this.conf.maxGapFrames && st.init) {
          const hold = { x: st.emaX, y: st.emaY }
          const geo = this.geometricEstimate(idx, present)
          let x = hold.x
          let y = hold.y
          if (geo) {
            const L = median(st.bones)
            const dev = dist(hold, geo)
            if (L > 1e-6 && dev > this.conf.boneLenTolerance * L) {
              x = geo.x
              y = geo.y
            }
          }
          st.emaX = x
          st.emaY = y
          present[idx] = true
          repaired[idx] = true
        } else if (st.gap > this.conf.maxGapFrames && st.init) {
          // 超长缺口仍尝试几何纠正一次，失败才彻底判缺
          const geo = this.geometricEstimate(idx, present)
          if (geo) {
            st.emaX = geo.x
            st.emaY = geo.y
            present[idx] = true
            repaired[idx] = true
          }
        }
      }

      if (present[idx]) {
        kps[idx] = { x: st.emaX, y: st.emaY, score: repaired[idx] ? 0 : raw.score, name: raw.name }
      }
    }

    let valid = true
    for (const c of CORE) {
      if (!present[c]) {
        valid = false
        break
      }
    }
    return { frame: gated.frame, present, repaired, valid }
  }

  private resolveParent(idx: number, present: boolean[]): { x: number; y: number } | null {
    const p = PARENT[idx]
    if (p === null || p === undefined) return null
    if (p === -1) {
      const ls = this.states[KP.L_SHOULDER]
      const rs = this.states[KP.R_SHOULDER]
      if (!ls.init || !rs.init) return null
      // 本帧双肩缺失则使用上一帧 EMA（缺口期保持）
      if (!present[KP.L_SHOULDER] || !present[KP.R_SHOULDER]) return null
      return midpoint({ x: ls.emaX, y: ls.emaY }, { x: rs.emaX, y: rs.emaY })
    }
    // 父节点本帧缺失时回退到上一帧 EMA 位置（其本身就是保持值）
    const ps = this.states[p]
    return ps.init ? { x: ps.emaX, y: ps.emaY } : null
  }

  private geometricEstimate(idx: number, present: boolean[]): { x: number; y: number } | null {
    const st = this.states[idx]
    const parent = this.resolveParent(idx, present)
    if (!parent || !st.dirInit || st.bones.length < 3) return null
    const L = median(st.bones)
    if (!(L > 1e-6)) return null
    return { x: parent.x + st.dirX * L, y: parent.y + st.dirY * L }
  }
}

// ---------------------------------------------------------------------------
// 非因果汇总复算（最终评分用，结果确定可复现）
// ---------------------------------------------------------------------------

export interface RepairedSequence {
  frames: PoseFrame[]
  /** 每帧关键点是否存在 */
  present: boolean[][]
  /** 每帧关键点是否为补点 */
  repaired: boolean[][]
  /** 有效帧标记 */
  valid: boolean[]
  validFrameRatio: number
  /** 含补点的帧占比（姿态修复比例） */
  poseRepairedRatio: number
}

interface FullKpTrack {
  x: (number | null)[]
  y: (number | null)[]
  bones: number[]
  dirX: number
  dirY: number
  dirInit: boolean
}

/**
 * 对整段原始帧做非因果平滑修复（最终评分的唯一口径，保证可复现）。
 */
export function repairSequence(rawFrames: PoseFrame[], conf: AlgoConfig): RepairedSequence {
  const n = rawFrames.length
  if (n === 0) {
    return {
      frames: [],
      present: [],
      repaired: [],
      valid: [],
      validFrameRatio: 0,
      poseRepairedRatio: 0
    }
  }

  const gate = new QualityGate(conf)
  const gated = rawFrames.map((f) => gate.gate(f))

  // 每个关键点一条轨迹
  const tracks: FullKpTrack[] = Array.from({ length: 17 }, () => ({
    x: new Array(n).fill(null),
    y: new Array(n).fill(null),
    bones: [],
    dirX: 0,
    dirY: 0,
    dirInit: false
  }))

  // 1) 中值滤波（窗口 3，居中，仅取窗口内观测值）
  for (let k = 0; k < 17; k++) {
    for (let t = 0; t < n; t++) {
      if (gated[t].missing[k]) continue
      const xs: number[] = []
      const ys: number[] = []
      for (let d = -1; d <= 1; d++) {
        const j = t + d
        if (j >= 0 && j < n && !gated[j].missing[k]) {
          xs.push(rawFrames[j].keypoints[k].x)
          ys.push(rawFrames[j].keypoints[k].y)
        }
      }
      tracks[k].x[t] = median(xs)
      tracks[k].y[t] = median(ys)
    }
  }

  // 2) 邻域线性插值（内缺口 ≤ maxGapFrames）
  for (let k = 0; k < 17; k++) {
    let t = 0
    while (t < n) {
      if (tracks[k].x[t] !== null) {
        t++
        continue
      }
      const start = t
      while (t < n && tracks[k].x[t] === null) t++
      const end = t // 缺口后第一个非空
      const gapLen = end - start
      if (start > 0 && end < n && gapLen <= conf.maxGapFrames) {
        const x0 = tracks[k].x[start - 1] as number
        const y0 = tracks[k].y[start - 1] as number
        const x1 = tracks[k].x[end] as number
        const y1 = tracks[k].y[end] as number
        for (let j = start; j < end; j++) {
          const r = (j - start + 1) / (gapLen + 1)
          tracks[k].x[j] = x0 + (x1 - x0) * r
          tracks[k].y[j] = y0 + (y1 - y0) * r
        }
      }
    }
  }

  // 3) 骨长中位数/方向统计（用完整观测帧的父子距离，全序列口径更稳）
  const parentOf = (k: number, t: number): { x: number; y: number } | null => {
    const p = PARENT[k]
    if (p === null || p === undefined) return null
    if (p === -1) {
      const lx = tracks[KP.L_SHOULDER].x[t]
      const ly = tracks[KP.L_SHOULDER].y[t]
      const rx = tracks[KP.R_SHOULDER].x[t]
      const ry = tracks[KP.R_SHOULDER].y[t]
      if (lx === null || ly === null || rx === null || ry === null) return null
      return { x: (lx + rx) / 2, y: (ly + ry) / 2 }
    }
    const px = tracks[p].x[t]
    const py = tracks[p].y[t]
    if (px === null || py === null) return null
    return { x: px, y: py }
  }
  for (let k = 0; k < 17; k++) {
    for (let t = 0; t < n; t++) {
      const x = tracks[k].x[t]
      const y = tracks[k].y[t]
      const parent = parentOf(k, t)
      if (x === null || y === null || !parent) continue
      const L = dist({ x, y }, parent)
      if (L > 1e-6) {
        tracks[k].bones.push(L)
        tracks[k].dirX = (x - parent.x) / L
        tracks[k].dirY = (y - parent.y) / L
        tracks[k].dirInit = true
      }
    }
  }

  // 4) 几何纠正剩余缺失（父节点先于子节点处理）
  const fullOrder = [5, 6, 11, 12, 1, 2, 3, 4, 7, 8, 13, 14, 0, 9, 10, 15, 16]
  const repairedFlags: boolean[][] = Array.from({ length: n }, () => new Array(17).fill(false))
  for (const k of fullOrder) {
    const L = median(tracks[k].bones)
    for (let t = 0; t < n; t++) {
      if (tracks[k].x[t] !== null) continue
      const parent = parentOf(k, t)
      if (parent && tracks[k].dirInit && L > 1e-6) {
        // 朴素保持值（最近邻），偏差超容忍才用几何值替换
        let hold: { x: number; y: number } | null = null
        for (let j = t - 1; j >= 0; j--) {
          if (tracks[k].x[j] !== null) {
            hold = { x: tracks[k].x[j] as number, y: tracks[k].y[j] as number }
            break
          }
        }
        if (!hold) {
          for (let j = t + 1; j < n; j++) {
            if (tracks[k].x[j] !== null) {
              hold = { x: tracks[k].x[j] as number, y: tracks[k].y[j] as number }
              break
            }
          }
        }
        const geo = { x: parent.x + tracks[k].dirX * L, y: parent.y + tracks[k].dirY * L }
        if (hold && dist(hold, geo) <= conf.boneLenTolerance * L) {
          tracks[k].x[t] = hold.x
          tracks[k].y[t] = hold.y
        } else {
          tracks[k].x[t] = geo.x
          tracks[k].y[t] = geo.y
        }
        repairedFlags[t][k] = gated[t].missing[k]
      } else if (gated[t].missing[k]) {
        // 无法修复：插值已经处理过的内缺口这里坐标非空，不会进入
      }
      if (gated[t].missing[k] && tracks[k].x[t] !== null) repairedFlags[t][k] = true
    }
  }

  // 5) 前向 EMA（在中值+插值+几何修复之后）。
  //    当帧平滑值逐帧落盘 smX/smY，组装时按帧取——切勿直接读最终标量 emaX（曾导致全序列塌缩为末端位置）。
  const emaX = new Array<number>(17)
  const emaY = new Array<number>(17)
  const emaInit = new Array<boolean>(17).fill(false)
  const smX: (number | null)[][] = Array.from({ length: 17 }, () => new Array(n).fill(null))
  const smY: (number | null)[][] = Array.from({ length: 17 }, () => new Array(n).fill(null))
  const present: boolean[][] = Array.from({ length: n }, () => new Array<boolean>(17).fill(false))

  for (let t = 0; t < n; t++) {
    for (let k = 0; k < 17; k++) {
      const x = tracks[k].x[t]
      const y = tracks[k].y[t]
      if (x === null || y === null) continue
      if (!emaInit[k]) {
        emaX[k] = x
        emaY[k] = y
        emaInit[k] = true
      } else {
        emaX[k] = (1 - conf.emaAlpha) * emaX[k] + conf.emaAlpha * x
        emaY[k] = (1 - conf.emaAlpha) * emaY[k] + conf.emaAlpha * y
      }
      smX[k][t] = emaX[k]
      smY[k][t] = emaY[k]
      present[t][k] = true
    }
  }

  // 6) 组装输出帧与统计
  const frames: PoseFrame[] = new Array(n)
  const valid = new Array<boolean>(n).fill(false)
  let validCount = 0
  let repairedFrameCount = 0
  for (let t = 0; t < n; t++) {
    const keypoints = rawFrames[t].keypoints.map((raw, k) => ({
      x: present[t][k] ? (smX[k][t] as number) : raw.x,
      y: present[t][k] ? (smY[k][t] as number) : raw.y,
      score: repairedFlags[t][k] ? 0 : raw.score,
      name: raw.name
    }))
    frames[t] = { frameNo: rawFrames[t].frameNo, timestamp: rawFrames[t].timestamp, keypoints }

    let frameValid = true
    for (const c of CORE) {
      if (!present[t][c]) {
        frameValid = false
        break
      }
    }
    valid[t] = frameValid
    if (frameValid) validCount++
    if (repairedFlags[t].some(Boolean)) repairedFrameCount++
  }

  return {
    frames,
    present,
    repaired: repairedFlags,
    valid,
    validFrameRatio: n > 0 ? validCount / n : 0,
    poseRepairedRatio: n > 0 ? repairedFrameCount / n : 0
  }
}

/** 将比例限制在 0~1 */
export function ratio01(v: number): number {
  return clamp(v, 0, 1)
}
