/**
 * MoveNet Lightning ONNX Runtime Web 推理层（P3-F1）。
 *
 * 约束（需求 6.3 / 待对齐项 6）：
 * - 模型与 WASM 运行时全部随前端静态资源本地分发（/models、/ort），运行时不访问任何远端地址；
 * - 输入 int32[1,192,192,3]（NHWC，RGB，0~255），输出 float32[1,1,17,3]（y,x,score，归一化 0~1）；
 * - WebGL → WASM 后端自动降级；加载/哈希校验失败抛出明确错误，由 UI 引导而非白屏；
 * - pose_engine_id 来自模型清单（模型 sha256 前 12 位 + ort-web 版本）。
 */
// 使用 WASM 专用入口，避免默认 bundle 同时初始化 WebGL/JSEP 后端。
import * as ort from 'onnxruntime-web/wasm'
import type { Keypoint, PoseFrame } from './types'

/** 模型清单（public/models/manifest.json，由 scripts/prepare-pose-assets.mjs 生成） */
export interface EngineManifest {
  modelFile: string
  modelSizeBytes: number
  sha256: string
  inputSize: number
  inputDtype: string
  inputLayout: string
  colorOrder: string
  valueRange: [number, number]
  outputShape: number[]
  keypointOrder: string[]
  ortWebVersion: string
  engineId: string
  source: string
  note: string
}

/** 归一化矩形（相对原始视频帧，0~1，未镜像的规范坐标系） */
export interface NormRect {
  x: number
  y: number
  w: number
  h: number
}

export interface DetectorOptions {
  /** 前置摄像头镜像（预览与骨架均镜像，解剖学左右命名不变） */
  mirror?: boolean
  /** 执行后端优先级，默认 ['webgl', 'wasm'] 自动降级 */
  executionProviders?: Array<'webgl' | 'wasm'>
  /** 清单地址（默认 /models/manifest.json） */
  manifestUrl?: string
  /** wasm 运行时路径（本地静态分发） */
  wasmPaths?: { wasm: string }
  /** 初始化阶段进度文案（供页面展示“正在尝试某推理后端”等状态） */
  onStatus?: (message: string) => void
}

/** Electron/Trae 内置浏览器可能创建 pthread worker 后不回调；这类环境直接走单线程。 */
/** 推理后端描述 */
export interface DetectorBackend {
  name: 'webgl' | 'wasm' | 'webgpu'
}

/** MoveNet 17 关键点顺序 */
export const KEYPOINT_NAMES: string[] = [
  'nose', 'left_eye', 'right_eye', 'left_ear', 'right_ear',
  'left_shoulder', 'right_shoulder', 'left_elbow', 'right_elbow',
  'left_wrist', 'right_wrist', 'left_hip', 'right_hip',
  'left_knee', 'right_knee', 'left_ankle', 'right_ankle'
]

/** 骨架连线（用于实时叠加渲染） */
export const SKELETON_EDGES: ReadonlyArray<readonly [number, number]> = [
  [0, 1], [0, 2], [1, 3], [2, 4], // 头部
  [5, 6], // 肩-肩
  [5, 7], [7, 9], [6, 8], [8, 10], // 双臂
  [5, 11], [6, 12], // 躯干
  [11, 12], // 髋-髋
  [11, 13], [13, 15], [12, 14], [14, 16] // 双腿
]

let manifestCache: EngineManifest | null = null

type WasmRuntimePaths = { wasm: string }

function isWebglAvailable(): boolean {
  if (typeof document === 'undefined') return false
  try {
    const canvas = document.createElement('canvas')
    return Boolean(canvas.getContext('webgl2') || canvas.getContext('webgl'))
  } catch {
    return false
  }
}

function isEmbeddedBrowser(): boolean {
  return typeof navigator !== 'undefined' && /Electron|Trae/i.test(navigator.userAgent)
}

async function assertAssetAvailable(url: string, label: string): Promise<void> {
  let response: Response
  try {
    response = await fetch(url, { method: 'HEAD', cache: 'no-store' })
  } catch (err) {
    throw new Error(`${label}无法访问：${url}（${err instanceof Error ? err.message : String(err)}）`)
  }
  if (response.status === 404) throw new Error(`${label}不存在（HTTP 404）：${url}`)
  // Some static servers do not implement HEAD. ORT will issue the real GET.
  if (!response.ok && response.status !== 405) {
    throw new Error(`${label}加载失败（HTTP ${response.status}）：${url}`)
  }
}

function resolveAppAsset(path: string): string {
  if (/^https?:\/\//i.test(path)) return path
  const base = typeof document !== 'undefined' ? document.baseURI : `${location.origin}/`
  return new URL(path.replace(/^\//, ''), new URL(import.meta.env.BASE_URL, base)).href
}

/** 加载模型清单（带缓存） */
export async function loadEngineManifest(manifestUrl = '/models/manifest.json'): Promise<EngineManifest> {
  if (manifestCache) return manifestCache
  const url = resolveAppAsset(manifestUrl)
  const resp = await fetch(url)
  if (!resp.ok) {
    throw new Error(`模型清单加载失败（HTTP ${resp.status}），请检查部署目录 /models/manifest.json`)
  }
  manifestCache = (await resp.json()) as EngineManifest
  return manifestCache
}

function toHex(buf: ArrayBuffer): string {
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

/**
 * MoveNet 单姿态检测器。
 *
 * 用法：
 *   const detector = await MoveNetDetector.create({ mirror: true })
 *   const frame = await detector.estimate(video, frameNo)
 */
export class MoveNetDetector {
  private readonly session: ort.InferenceSession
  private readonly manifest: EngineManifest
  private readonly inputSize: number
  private readonly mirror: boolean
  private readonly backend: DetectorBackend
  private readonly canvas: HTMLCanvasElement
  private readonly ctx: CanvasRenderingContext2D
  private readonly tensorData: Int32Array
  private readonly inputName: string
  private readonly outputName: string

  private constructor(
    session: ort.InferenceSession,
    manifest: EngineManifest,
    backend: DetectorBackend,
    mirror: boolean
  ) {
    this.session = session
    this.manifest = manifest
    this.backend = backend
    this.mirror = mirror
    this.inputSize = manifest.inputSize
    this.inputName = session.inputNames[0]
    this.outputName = session.outputNames[0]

    const Ctor = (
      typeof OffscreenCanvas !== 'undefined' ? OffscreenCanvas : HTMLCanvasElement
    ) as unknown as { new (width: number, height: number): HTMLCanvasElement }
    this.canvas = new Ctor(this.inputSize, this.inputSize)
    const ctx = this.canvas.getContext('2d', { willReadFrequently: true })
    if (!ctx) throw new Error('Canvas 2D 上下文初始化失败，设备可能不支持本地姿态推理')
    this.ctx = ctx
    this.tensorData = new Int32Array(this.inputSize * this.inputSize * 3)
  }

  get engineId(): string {
    return this.manifest.engineId
  }

  get backendName(): string {
    return this.backend.name
  }

  get modelSha256(): string {
    return this.manifest.sha256
  }

  /**
   * 加载并校验本地模型，按 webgl → wasm 顺序创建推理会话。
   */
  static async create(options: DetectorOptions = {}): Promise<MoveNetDetector> {
    const manifest = await loadEngineManifest(options.manifestUrl)

    // WASM 运行时固定本地路径；跨域隔离不可用时退化为单线程
    const runtimeBase = new URL(import.meta.env.BASE_URL, document.baseURI)
    const wasmRuntimePaths: WasmRuntimePaths = options.wasmPaths ?? {
      wasm: new URL('ort/ort-wasm-simd-threaded.wasm', runtimeBase).href
    }
    // 直接注入二进制，跳过 ORT 对 .mjs 胶水模块的动态 import。
    // 这是 Vite/Electron 下最稳定的单线程 WASM 初始化方式。
    const wasmResponse = await fetch(wasmRuntimePaths.wasm, { cache: 'force-cache' })
    if (!wasmResponse.ok) {
      throw new Error(`WASM 二进制文件加载失败（HTTP ${wasmResponse.status}）：${wasmRuntimePaths.wasm}`)
    }
    ort.env.wasm.wasmBinary = await wasmResponse.arrayBuffer()
    ort.env.wasm.wasmPaths = undefined
    // 让 ORT 自身的 WASM 初始化也有上限，避免 worker 静默失败时永久挂起。
    ort.env.wasm.initTimeout = 30000
    ort.env.wasm.numThreads = 1
    ort.env.wasm.simd = true
    ort.env.wasm.proxy = false

    const modelUrl = resolveAppAsset(`/models/${manifest.modelFile}`)
    const modelResp = await fetch(modelUrl)
    if (!modelResp.ok) {
      throw new Error(`模型文件加载失败（HTTP ${modelResp.status}），请检查 ${manifest.modelFile} 是否已随站点分发`)
    }
    const modelBuffer = await modelResp.arrayBuffer()
    const actualSha = toHex(await crypto.subtle.digest('SHA-256', modelBuffer))
    if (actualSha !== manifest.sha256) {
      throw new Error(
        `模型完整性校验失败：期望 ${manifest.sha256.slice(0, 12)}…，实际 ${actualSha.slice(0, 12)}…，请重新部署模型文件`
      )
    }

    // 默认只启用单线程 WASM。WebGL 在部分 Electron/驱动环境中会在
    // InferenceSession.create 阶段静默挂起，并且会污染同一页面中的 ORT
    // 全局状态，使后续 WASM 降级也无法启动。WASM 是可预测的基线。
    const requestedProviders = options.executionProviders ?? (['wasm'] as const)
    const providers = requestedProviders.filter((provider) => provider !== 'webgl' || isWebglAvailable())
    if (!providers.includes('wasm')) providers.push('wasm')
    if (isEmbeddedBrowser()) {
      providers.sort((a, b) => (a === 'wasm' ? -1 : b === 'wasm' ? 1 : 0))
    }

    // 组装实际尝试序列：webgl 一次；wasm 在跨域隔离时先多线程，失败/挂起再回落单线程。
    // 背景：部分浏览器/Electron 环境下 pthread module worker 静默不就绪会让
    // InferenceSession.create 永不返回（不 reject），必须用超时竞速避免整页永久卡死。
    interface BackendAttempt {
      provider: 'webgl' | 'wasm'
      threads: number
      timeoutMs: number
      label: string
    }
    const attempts: BackendAttempt[] = []
    for (const provider of providers) {
      if (provider === 'webgl') {
        attempts.push({ provider, threads: 1, timeoutMs: isEmbeddedBrowser() ? 5000 : 10000, label: 'webgl' })
      } else if (provider === 'wasm') {
        attempts.push({ provider, threads: 1, timeoutMs: 30000, label: 'wasm(单线程)' })
      }
    }

    const errors: string[] = []
    for (const attempt of attempts) {
      ort.env.wasm.numThreads = attempt.threads
      ort.env.wasm.initTimeout = attempt.timeoutMs
      try {
        options.onStatus?.(`正在初始化推理后端 ${attempt.label}…`)
        const session = await this.createSessionWithTimeout(
          modelBuffer,
          attempt.provider,
          attempt.timeoutMs,
          manifest.inputSize
        )
        return new MoveNetDetector(session, manifest, { name: attempt.provider }, options.mirror ?? true)
      } catch (err) {
        errors.push(`${attempt.label}: ${err instanceof Error ? err.message : String(err)}`)
        options.onStatus?.(`${attempt.label} 后端不可用（${err instanceof Error ? err.message : String(err)}），尝试下一方案…`)
      }
    }
    throw new Error(
      `姿态推理引擎初始化失败（已尝试 ${attempts.map((a) => a.label).join(' → ')}）。请更新浏览器或更换设备后重试。原始错误：${
        errors.join('；')
      }`
    )
  }

  /**
   * 创建推理会话并加超时竞速。
   * 超时后底层 Promise 无法真正取消；若其稍后才返回，立即 dispose 释放其 worker/WASM 资源。
   */
  private static createSessionWithTimeout(
    modelBuffer: ArrayBuffer,
    provider: 'webgl' | 'wasm',
    timeoutMs: number,
    inputSize: number
  ): Promise<ort.InferenceSession> {
    let timer: ReturnType<typeof setTimeout> | null = null
    let timedOut = false
    const createPromise = ort.InferenceSession.create(modelBuffer.slice(0), {
      executionProviders: [provider],
      graphOptimizationLevel: 'basic'
    }).then(async (session) => {
      try {
        // Session creation alone does not prove that the selected backend can
        // execute. A tiny real inference catches broken WebGL/WASM workers.
        const input = new ort.Tensor('int32', new Int32Array(inputSize * inputSize * 3), [1, inputSize, inputSize, 3])
        await session.run({ [session.inputNames[0]]: input })
        return session
      } catch (err) {
        await session.release().catch(() => undefined)
        throw err
      }
    })
    const timeoutPromise = new Promise<never>((_, reject) => {
      timer = setTimeout(() => {
        timedOut = true
        reject(new Error(`${provider} 后端 ${timeoutMs / 1000}s 内未就绪`))
      }, timeoutMs)
    })
    // 仅在已超时的前提下，若底层稍后才创建成功则释放这个迟到会话（避免悬挂 worker）
    void createPromise.then(
      (late) => {
        if (timedOut) {
          void ((late as ort.InferenceSession & { release?: () => Promise<void> }).release?.())
        }
      },
      () => { /* 已计入错误，无需处理 */ }
    )
    return Promise.race([createPromise, timeoutPromise]).finally(() => {
      if (timer) clearTimeout(timer)
    })
  }

  /**
   * 对单帧视频做推理。
   *
   * @param video 正在播放的视频元素
   * @param frameNo 帧序号（由采集循环维护）
   * @param timestamp 采集时间戳（performance.now()）
   * @param roi 感兴趣区域（规范坐标系）；缺省时中心方裁
   * @returns 关键点坐标映射回原始视频帧的归一化 PoseFrame
   */
  async estimate(
    video: HTMLVideoElement,
    frameNo: number,
    timestamp: number,
    roi?: NormRect
  ): Promise<PoseFrame> {
    const vw = video.videoWidth
    const vh = video.videoHeight
    if (!vw || !vh) {
      throw new Error('视频流尚未就绪（videoWidth/videoHeight 为 0）')
    }

    // 源像素方裁区域（规范坐标系 → 像素）
    let sx: number, sy: number, ss: number
    if (roi) {
      // ROI 外扩 20% 后转正方形并限制在画面内（调用方每 30 帧更新一次）
      const expanded = expandToSquare(roi, 1.2)
      sx = clamp(expanded.x, 0, 1) * vw
      sy = clamp(expanded.y, 0, 1) * vh
      ss = clamp(expanded.w, 0, 1) * Math.min(vw, vh)
      ss = Math.min(ss, vw - sx, vh - sy)
    } else {
      ss = Math.min(vw, vh)
      sx = (vw - ss) / 2
      sy = (vh - ss) / 2
    }

    this.ctx.drawImage(video, sx, sy, ss, ss, 0, 0, this.inputSize, this.inputSize)
    const rgba = this.ctx.getImageData(0, 0, this.inputSize, this.inputSize).data
    const rgb = this.tensorData
    for (let i = 0, j = 0; i < rgba.length; i += 4, j += 3) {
      rgb[j] = rgba[i]
      rgb[j + 1] = rgba[i + 1]
      rgb[j + 2] = rgba[i + 2]
    }

    const tensor = new ort.Tensor('int32', rgb, [1, this.inputSize, this.inputSize, 3])
    const outputs = await this.session.run({ [this.inputName]: tensor })
    const out = outputs[this.outputName]
    const flat = out.data as Float32Array

    const keypoints: Keypoint[] = new Array(17)
    for (let k = 0; k < 17; k++) {
      const my = flat[k * 3]
      const mx = flat[k * 3 + 1]
      const score = flat[k * 3 + 2]
      // 模型输入方裁区域 → 原始帧规范坐标
      let fx = (sx + mx * ss) / vw
      const fy = (sy + my * ss) / vh
      if (this.mirror) fx = 1 - fx
      keypoints[k] = { x: fx, y: fy, score, name: KEYPOINT_NAMES[k] }
    }

    return { frameNo, timestamp, keypoints }
  }

  dispose(): void {
    void this.session.release()
  }
}

/** ROI 外扩并转正方形（以外接矩形中心为锚） */
export function expandToSquare(rect: NormRect, expand = 1.2): NormRect {
  const cx = rect.x + rect.w / 2
  const cy = rect.y + rect.h / 2
  const side = Math.max(rect.w, rect.h) * expand
  const x = clamp(cx - side / 2, 0, 1 - side)
  const y = clamp(cy - side / 2, 0, 1 - side)
  return { x, y, w: side, h: side }
}

function clamp(v: number, min: number, max: number): number {
  if (max < min) return min > max ? 0 : min
  return Math.min(max, Math.max(min, v))
}

/**
 * @deprecated P3 起使用 MoveNetDetector.create()；保留函数名兼容旧调用点。
 */
export async function loadMoveNet(modelType: 'lightning' | 'thunder' = 'lightning'): Promise<MoveNetDetector> {
  if (modelType !== 'lightning') {
    throw new Error('当前仅集成本地 MoveNet Lightning（192×192），Thunder 不在本期分发范围')
  }
  return MoveNetDetector.create()
}
