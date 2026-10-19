/**
 * 姿态引擎本地资产准备脚本（P3-F1）
 *
 * 职责：
 * 1. 将 onnxruntime-web 的 WASM 运行时从 node_modules 拷贝到 public/ort/（随静态资源本地分发，禁用远端地址）；
 * 2. 校验 public/models/movenet-lightning.onnx 的 SHA-256，生成 public/models/manifest.json；
 * 3. 由 模型sha256前12位 + ort-web 版本 拼出 pose_engine_id（与 action_version.pose_engine_id 对齐）。
 *
 * 模型来源（构建期一次性获取，运行时仅读本地文件，不访问任何远端地址）：
 *   https://huggingface.co/Xenova/movenet-singlepose-lightning/resolve/main/onnx/model.onnx
 *   国内镜像：https://hf-mirror.com/Xenova/movenet-singlepose-lightning/resolve/main/onnx/model.onnx
 * 模型契约：input int32[1,192,192,3]（NHWC，RGB，0~255）；output float32[1,1,17,3]（y,x,score，均归一化 0~1）。
 *
 * 用法：node scripts/prepare-pose-assets.mjs
 */
import { createHash } from 'node:crypto'
import { existsSync, mkdirSync, readFileSync, writeFileSync, copyFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const ortDist = join(root, 'node_modules', 'onnxruntime-web', 'dist')
const ortVersion = JSON.parse(readFileSync(join(ortDist, '..', 'package.json'), 'utf8')).version

const publicOrt = join(root, 'public', 'ort')
const modelsDir = join(root, 'public', 'models')
const modelFile = 'movenet-lightning.onnx'
const modelPath = join(modelsDir, modelFile)

// ===== 1) 拷贝 WASM 运行时（默认 ESM bundle 使用 jsep 版，支持 wasm/webgl/webgpu） =====
mkdirSync(publicOrt, { recursive: true })
const wasmAssets = ['ort-wasm-simd-threaded.jsep.mjs', 'ort-wasm-simd-threaded.jsep.wasm']
for (const f of wasmAssets) {
  const src = join(ortDist, f)
  if (!existsSync(src)) {
    console.error(`[prepare] 缺少 ${src}，请先执行 npm install`)
    process.exit(1)
  }
  copyFileSync(src, join(publicOrt, f))
  console.log(`[prepare] copied public/ort/${f}`)
}

// ===== 2) 校验模型并计算 SHA-256 =====
if (!existsSync(modelPath)) {
  console.error(`[prepare] 缺少模型文件 ${modelPath}`)
  console.error('[prepare] 请从以下地址下载后放到 public/models/ （构建期获取，运行时仅本地加载）：')
  console.error('[prepare]   https://hf-mirror.com/Xenova/movenet-singlepose-lightning/resolve/main/onnx/model.onnx')
  process.exit(1)
}
const buf = readFileSync(modelPath)
const sha256 = createHash('sha256').update(buf).digest('hex')

// ===== 3) 写模型清单 =====
const KEYPOINT_NAMES = [
  'nose', 'left_eye', 'right_eye', 'left_ear', 'right_ear',
  'left_shoulder', 'right_shoulder', 'left_elbow', 'right_elbow',
  'left_wrist', 'right_wrist', 'left_hip', 'right_hip',
  'left_knee', 'right_knee', 'left_ankle', 'right_ankle'
]
const manifest = {
  modelFile,
  modelSizeBytes: buf.length,
  sha256,
  inputSize: 192,
  inputDtype: 'int32',
  inputLayout: 'NHWC',
  colorOrder: 'RGB',
  valueRange: [0, 255],
  outputShape: [1, 1, 17, 3],
  keypointOrder: KEYPOINT_NAMES,
  ortWebVersion: ortVersion,
  engineId: `movenet-lightning-web@${sha256.slice(0, 12)}-ortweb${ortVersion}`,
  source: 'Xenova/movenet-singlepose-lightning onnx/model.onnx (Google MoveNet SinglePose Lightning ONNX export)',
  note: '运行时仅从 /models/ 本地加载；模型与 wasm 均不使用任何远端地址'
}
mkdirSync(modelsDir, { recursive: true })
writeFileSync(join(modelsDir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n', 'utf8')
console.log(`[prepare] manifest.json written`)
console.log(`[prepare] sha256 = ${sha256}`)
console.log(`[prepare] engineId = ${manifest.engineId}`)
