/**
 * MoveNet 姿态评估引擎 - 类型契约定义
 *
 * 本文件为算法参数/结果契约的唯一来源（canonical）。
 * 管理端 frontend/src/pose-engine/types.ts 保留同步副本，
 * 修改本文件时必须同步更新副本，避免双端漂移。
 *
 * 供端侧推理后处理管线（质量门控 -> 平滑修复 -> 归一化 -> 特征 -> 计数 -> 评分）使用。
 */

/** 单个关键点（归一化坐标由 MoveNet 输出，x/y 通常为 0~1，score 为置信度 0~1） */
export interface Keypoint {
  x: number
  y: number
  score: number
  /** 关键点名称，如 nose / left_shoulder / right_knee（MoveNet 17 关键点命名） */
  name?: string
}

/** 单帧姿态数据 */
export interface PoseFrame {
  /** 帧序号，从 0 或 1 开始递增 */
  frameNo: number
  /** 该帧的关键点集合（MoveNet 单人为 17 个） */
  keypoints: Keypoint[]
  /** 采集时间戳（毫秒，performance.now() 或视频时间换算） */
  timestamp: number
}

/** 姿态评估算法可调参数 */
export interface AlgoConfig {
  /** 关键点高置信度阈值（高于该值直接采纳） */
  confHigh: number
  /** 关键点低置信度阈值（低于该值判定为遮挡 / 缺失，进入修复） */
  confLow: number
  /** 中值滤波窗口（帧，奇数） */
  medianWindow: number
  /** EMA 指数滑动平均系数（0~1，越大越跟手、越小越平滑） */
  emaAlpha: number
  /** 允许连续补点的最大帧数，超过则判该段轨迹无效 */
  maxGapFrames: number
  /** 骨长一致性容忍比例（相对比例，如 0.15 表示 15%） */
  boneLenTolerance: number
  /** 单段训练允许的补点占比上限 */
  repairRatioLimit: number
  /** 动作状态机状态保持帧数（去抖） */
  stateHoldFrames: number
  /** 状态切换迟滞带（角度） */
  hysteresisBand: number
  /** 最小有效动作周期（秒），短于该时长的周期不计次 */
  minCycleSec: number
  /** 角速度平滑窗口（帧） */
  smoothWindowSpeed: number
  /** 角加速度平滑窗口（帧） */
  smoothWindowAccel: number
  /** 时间权重高斯核 sigma（秒），用于近期帧加权 */
  timeWeightSigma: number
  /** 有效帧占比下限，低于该值整段训练判为数据不足 */
  validFrameRatioMin: number
  /** 幅度维权重（建议六项之和=1） */
  weightAmplitude: number
  /** 保持维权重 */
  weightHold: number
  /** 节奏维权重 */
  weightRhythm: number
  /** 稳定维权重 */
  weightStability: number
  /** 对称维权重 */
  weightSymmetry: number
  /** 相似度维权重 */
  weightSimilarity: number
  /** 算法引擎标识（随评分结果上报，便于后端做版本归因） */
  poseEngineId: string
}

/** 默认算法参数（首版基线，后续随算法迭代调整） */
export const DEFAULT_ALGO_CONFIG: AlgoConfig = {
  confHigh: 0.5,
  confLow: 0.2,
  medianWindow: 3,
  emaAlpha: 0.35,
  maxGapFrames: 5,
  boneLenTolerance: 0.15,
  repairRatioLimit: 0.3,
  stateHoldFrames: 3,
  hysteresisBand: 8,
  minCycleSec: 1.2,
  smoothWindowSpeed: 7,
  smoothWindowAccel: 11,
  timeWeightSigma: 0.2,
  validFrameRatioMin: 0.7,
  weightAmplitude: 0.3,
  weightHold: 0.2,
  weightRhythm: 0.15,
  weightStability: 0.15,
  weightSymmetry: 0.1,
  weightSimilarity: 0.1,
  // P3：真实引擎标识由 public/models/manifest.json 的 engineId 决定
  // （= movenet-lightning-web@{模型sha256前12位}-ortweb{ort-web版本}），运行时以清单为准覆盖
  poseEngineId: 'movenet-lightning-web@1ad4f8d6c2f7-ortweb1.30.0'
}

/**
 * 动作运动签名（与管理端 VersionFormDialog 的 MotionSignatureForm 产出一一对应）。
 * 存储于 action_version.motion_signature（JSON 字符串），训练开始时随参数快照一次性下发。
 */
export interface MotionSignature {
  /** 目标关节（中文名或 MoveNet 关节英文名，如 "左肩" / "left_knee"） */
  targetJoints: string[]
  /** 约束关节（代偿监控，不参与主幅度评分） */
  constraintJoints?: string[]
  /** 起始角度区间 [min,max]（度） */
  startAngleRange?: [number | null, number | null]
  /** 峰值/末端角度区间 [min,max]（度） */
  peakAngleRange?: [number | null, number | null]
  /** 最小关节活动范围 ROM（度） */
  minRom?: number
  /** 期望方向：FLEX 屈曲（角度增大）/ EXTEND 伸展（角度减小）/ MIXED 混合 */
  expectedDirection?: 'FLEX' | 'EXTEND' | 'MIXED'
  /** 阶段顺序，默认 READY→RAISING→HOLD→LOWERING */
  phaseOrder?: string[]
  /** 单周期时长范围（秒）[min,max] */
  durationRangeSec?: [number | null, number | null]
}

/** 六维评分明细（各维度 0~100） */
export interface ScoreDetail {
  /** 幅度分：关节活动度是否达到目标角度范围 */
  amplitude: number
  /** 保持分：末端 / 指定姿态保持时长是否达标 */
  hold: number
  /** 节奏分：动作周期与节拍一致性 */
  rhythm: number
  /** 稳定分：运动过程抖动与代偿控制 */
  stability: number
  /** 对称分：左右侧动作对称性 */
  symmetry: number
  /** 相似度分：与标准动作模板的吻合程度（DTW / 特征距离） */
  similarity: number
}

/** 姿态数据质量等级 */
export type PoseQuality = 'GOOD' | 'FAIR' | 'INSUFFICIENT'

/** 单次训练的汇总评估结果（提交给后端 /training-records 的核心载荷） */
export interface TrainingSummary {
  /** 总分 0~100（六维加权） */
  totalScore: number
  /** 动作完成总次数（状态机计次） */
  totalReps: number
  /** 达标次数（单次周期评分达到合格线） */
  correctReps: number
  /** 有效帧占比（0~1） */
  validFrameRatio: number
  /** 姿态补点帧占比（0~1），反映遮挡严重程度 */
  poseRepairedRatio: number
  /** 数据质量等级：INSUFFICIENT 时不计入趋势与均值 */
  poseQuality: PoseQuality
  /** 六维评分明细 */
  scoreDetail: ScoreDetail
  /** 生成该结果的算法引擎标识 */
  poseEngineId: string
}
