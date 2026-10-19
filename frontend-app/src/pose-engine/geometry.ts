/**
 * 姿态几何与数值工具（P3-F3/F4 共用）。
 *
 * 关键点索引遵循 MoveNet 17 点约定：
 *  0 nose        1 left_eye    2 right_eye   3 left_ear   4 right_ear
 *  5 left_shoulder  6 right_shoulder  7 left_elbow  8 right_elbow
 *  9 left_wrist    10 right_wrist   11 left_hip   12 right_hip
 * 13 left_knee    14 right_knee    15 left_ankle 16 right_ankle
 */
import type { Keypoint, PoseFrame } from './types'

export interface Point {
  x: number
  y: number
}

export interface Box {
  x: number
  y: number
  w: number
  h: number
}

export const KP = {
  NOSE: 0,
  L_SHOULDER: 5,
  R_SHOULDER: 6,
  L_ELBOW: 7,
  R_ELBOW: 8,
  L_WRIST: 9,
  R_WRIST: 10,
  L_HIP: 11,
  R_HIP: 12,
  L_KNEE: 13,
  R_KNEE: 14,
  L_ANKLE: 15,
  R_ANKLE: 16
} as const

/** 核心躯干/下肢关键点（入场检查与有效帧判定使用） */
export const CORE_INDICES = [
  KP.L_SHOULDER,
  KP.R_SHOULDER,
  KP.L_ELBOW,
  KP.R_ELBOW,
  KP.L_HIP,
  KP.R_HIP,
  KP.L_KNEE,
  KP.R_KNEE
]

export function clamp(v: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, v))
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t
}

export function mean(xs: number[]): number {
  if (xs.length === 0) return 0
  let s = 0
  for (const x of xs) s += x
  return s / xs.length
}

export function median(xs: number[]): number {
  if (xs.length === 0) return 0
  const sorted = [...xs].sort((a, b) => a - b)
  const mid = sorted.length >> 1
  return sorted.length % 2 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2
}

export function dist(a: Point, b: Point): number {
  return Math.hypot(a.x - b.x, a.y - b.y)
}

export function midpoint(a: Point, b: Point): Point {
  return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
}

/**
 * 三点夹角（顶点 b），0~180 度。
 * 约定：完全伸直=180°，完全屈曲靠近=小角度（与康复 ROM 习惯相反时由调用方取 180-angle）。
 */
export function angleAt(a: Point, b: Point, c: Point): number {
  const v1x = a.x - b.x
  const v1y = a.y - b.y
  const v2x = c.x - b.x
  const v2y = c.y - b.y
  const dot = v1x * v2x + v1y * v2y
  const n = Math.hypot(v1x, v1y) * Math.hypot(v2x, v2y)
  if (n < 1e-9) return 0
  return (Math.acos(clamp(dot / n, -1, 1)) * 180) / Math.PI
}

/**
 * 向量 a→b 与竖直向下方向（屏幕 +y）的夹角（0~180），用于躯干倾斜度。
 * 屏幕坐标系 y 向下，直立时肩→髋向量与竖直方向夹角≈0。
 */
export function inclination(a: Point, b: Point): number {
  const vx = b.x - a.x
  const vy = b.y - a.y
  const n = Math.hypot(vx, vy)
  if (n < 1e-9) return 0
  return (Math.acos(clamp(vy / n, -1, 1)) * 180) / Math.PI
}

export function kp(frame: PoseFrame, idx: number): Keypoint {
  return frame.keypoints[idx]
}

export function kpPoint(frame: PoseFrame, idx: number): Point {
  const k = frame.keypoints[idx]
  return { x: k.x, y: k.y }
}

/**
 * 关键点外接矩形（仅纳入置信度达标的关键点）。
 * @param minScore 纳入判定的置信度下限
 */
export function keypointBox(frame: PoseFrame, indices: number[], minScore: number): Box | null {
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  let used = 0
  for (const i of indices) {
    const k = frame.keypoints[i]
    if (k.score < minScore) continue
    used++
    if (k.x < minX) minX = k.x
    if (k.y < minY) minY = k.y
    if (k.x > maxX) maxX = k.x
    if (k.y > maxY) maxY = k.y
  }
  if (used < 3) return null
  return { x: minX, y: minY, w: Math.max(1e-6, maxX - minX), h: Math.max(1e-6, maxY - minY) }
}

/**
 * 滑动平均（因果，仅用当前及历史窗口）。
 */
export function movingAverage(series: number[], window: number): number[] {
  const half = Math.max(1, Math.floor(window / 2))
  const out = new Array<number>(series.length)
  let acc = 0
  const q: number[] = []
  for (let i = 0; i < series.length; i++) {
    q.push(series[i])
    acc += series[i]
    if (q.length > half * 2 + 1) acc -= q.shift() as number
    out[i] = acc / q.length
  }
  return out
}

/** 居中滑动平均（非因果，供最终汇总复算使用） */
export function centeredMovingAverage(series: number[], window: number): number[] {
  const half = Math.max(0, Math.floor(window / 2))
  const out = new Array<number>(series.length)
  for (let i = 0; i < series.length; i++) {
    let s = 0
    let n = 0
    for (let j = i - half; j <= i + half; j++) {
      if (j >= 0 && j < series.length) {
        s += series[j]
        n++
      }
    }
    out[i] = s / n
  }
  return out
}

/** 数值差分（按 dt 秒），首位以前向差分填充 */
export function derivative(series: number[], timesSec: number[]): number[] {
  const out = new Array<number>(series.length).fill(0)
  for (let i = 1; i < series.length; i++) {
    const dt = timesSec[i] - timesSec[i - 1]
    out[i] = dt > 1e-6 ? (series[i] - series[i - 1]) / dt : 0
  }
  if (out.length > 1) out[0] = out[1]
  return out
}

/** 序列等长重采样（线性插值，用于 DTW 对齐模板） */
export function resample(series: number[], targetLen: number): number[] {
  if (series.length === 0 || targetLen <= 1) return series.slice(0, targetLen)
  const out = new Array<number>(targetLen)
  const ratio = (series.length - 1) / (targetLen - 1)
  for (let i = 0; i < targetLen; i++) {
    const pos = i * ratio
    const lo = Math.floor(pos)
    const hi = Math.min(series.length - 1, lo + 1)
    const t = pos - lo
    out[i] = lerp(series[lo], series[hi], t)
  }
  return out
}

/** z-score 归一化（方差为 0 时返回全 0） */
export function zNormalize(series: number[]): number[] {
  if (series.length === 0) return []
  const m = mean(series)
  const sd = Math.sqrt(mean(series.map((x) => (x - m) * (x - m))))
  if (sd < 1e-9) return series.map(() => 0)
  return series.map((x) => (x - m) / sd)
}

/** 水平翻转归一化矩形（镜像坐标 → 摄像头规范坐标） */
export function flipRectX(rect: { x: number; y: number; w: number; h: number }) {
  return { x: 1 - rect.x - rect.w, y: rect.y, w: rect.w, h: rect.h }
}

/**
 * 关节名 → 角度三点索引（顶点在中间）。
 * 同时兼容 MoveNet 英文名与管理端常用中文名。
 */
const JOINT_TRIPLETS: Record<string, readonly [number, number, number]> = {
  left_elbow: [KP.L_SHOULDER, KP.L_ELBOW, KP.L_WRIST],
  right_elbow: [KP.R_SHOULDER, KP.R_ELBOW, KP.R_WRIST],
  elbow: [KP.L_SHOULDER, KP.L_ELBOW, KP.L_WRIST],
  左肘: [KP.L_SHOULDER, KP.L_ELBOW, KP.L_WRIST],
  右肘: [KP.R_SHOULDER, KP.R_ELBOW, KP.R_WRIST],
  left_shoulder: [KP.L_HIP, KP.L_SHOULDER, KP.L_ELBOW],
  right_shoulder: [KP.R_HIP, KP.R_SHOULDER, KP.R_ELBOW],
  shoulder: [KP.L_HIP, KP.L_SHOULDER, KP.L_ELBOW],
  左肩: [KP.L_HIP, KP.L_SHOULDER, KP.L_ELBOW],
  右肩: [KP.R_HIP, KP.R_SHOULDER, KP.R_ELBOW],
  left_hip: [KP.L_SHOULDER, KP.L_HIP, KP.L_KNEE],
  right_hip: [KP.R_SHOULDER, KP.R_HIP, KP.R_KNEE],
  hip: [KP.L_SHOULDER, KP.L_HIP, KP.L_KNEE],
  左髋: [KP.L_SHOULDER, KP.L_HIP, KP.L_KNEE],
  右髋: [KP.R_SHOULDER, KP.R_HIP, KP.R_KNEE],
  left_knee: [KP.L_HIP, KP.L_KNEE, KP.L_ANKLE],
  right_knee: [KP.R_HIP, KP.R_KNEE, KP.R_ANKLE],
  knee: [KP.L_HIP, KP.L_KNEE, KP.L_ANKLE],
  左膝: [KP.L_HIP, KP.L_KNEE, KP.L_ANKLE],
  右膝: [KP.R_HIP, KP.R_KNEE, KP.R_ANKLE]
}

/** 关节中文名/英文名 → 左右角度三元组配对（用于对称维度） */
export const SYMMETRY_PAIRS: Record<string, { left: readonly [number, number, number]; right: readonly [number, number, number] }> = {
  shoulder: { left: JOINT_TRIPLETS.left_shoulder, right: JOINT_TRIPLETS.right_shoulder },
  肩: { left: JOINT_TRIPLETS.left_shoulder, right: JOINT_TRIPLETS.right_shoulder },
  elbow: { left: JOINT_TRIPLETS.left_elbow, right: JOINT_TRIPLETS.right_elbow },
  肘: { left: JOINT_TRIPLETS.left_elbow, right: JOINT_TRIPLETS.right_elbow },
  hip: { left: JOINT_TRIPLETS.left_hip, right: JOINT_TRIPLETS.right_hip },
  髋: { left: JOINT_TRIPLETS.left_hip, right: JOINT_TRIPLETS.right_hip },
  knee: { left: JOINT_TRIPLETS.left_knee, right: JOINT_TRIPLETS.right_knee },
  膝: { left: JOINT_TRIPLETS.left_knee, right: JOINT_TRIPLETS.right_knee }
}

/** 取关节所属的左右配对类别（如 left_knee/左膝 → knee） */
export function symmetryKey(jointName: string): string | null {
  const n = jointName.trim().toLowerCase()
  const stripped = n.replace(/^(left|right)[-_]/, '').replace(/^左|^右/, '')
  return SYMMETRY_PAIRS[stripped] ? stripped : null
}

export function jointTriplet(name: string): readonly [number, number, number] | null {
  const trimmed = name.trim()
  return JOINT_TRIPLETS[trimmed.toLowerCase()] ?? JOINT_TRIPLETS[trimmed] ?? null
}
