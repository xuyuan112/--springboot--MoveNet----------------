<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { startTask } from '@/api/patient/task'
import type { TaskActionSnapshot } from '@/api/patient/task'
import {
  enqueuePendingSubmission,
  flushPendingSubmissions,
  localIsoTime,
  submitTrainingRecord
} from '@/api/patient/training'
import type { TrainingSubmitPayload } from '@/api/patient/training'
import { PoseAssessmentEngine, EngineVersionMismatchError } from '@/pose-engine'
import type { RealtimeState, SessionResult } from '@/pose-engine'
import { SKELETON_EDGES, loadEngineManifest } from '@/pose-engine/movenet'
import type { EntryIssue } from '@/pose-engine/entry-check'
import type { Phase } from '@/pose-engine/state-machine'
import type { PoseQuality } from '@/pose-engine/types'

type Stage = 'loading' | 'ready' | 'result' | 'error'
type SubmitStatus = 'submitting' | 'ok' | 'queued' | 'failed'

const route = useRoute()
const router = useRouter()

const stage = ref<Stage>('loading')
const loadingText = ref('正在下发动作参数并加载本地姿态模型…')
const errorText = ref('')
const mismatchInfo = ref<{ expected: string; actual: string } | null>(null)

const actionName = ref('训练动作')
const targetReps = ref(0)
const targetSets = ref(1)
const instruction = ref('')
const backendName = ref('')

const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const state = ref<RealtimeState | null>(null)
const result = ref<SessionResult | null>(null)
const ending = ref(false)

// P4：提交状态与快照（planItemId/actionVersionId 随 start 快照下发）
const snapshot = ref<TaskActionSnapshot | null>(null)
const submitStatus = ref<SubmitStatus | null>(null)
const submitNote = ref('')
let lastPayload: TrainingSubmitPayload | null = null
let sessionStartedAt: Date | null = null

let engine: PoseAssessmentEngine | null = null
let stream: MediaStream | null = null
let rafId = 0
let processing = false
let disposed = false
let inferenceErrorRun = 0

const taskId = Number(route.params.taskId)

const formatModelError = (err: unknown): string => {
  const raw = err instanceof Error ? err.message : String(err ?? '')
  const text = raw.toLowerCase()
  if (text.includes('http 404') || text.includes('http 500') || text.includes('not found')) {
    return `姿态模型资源加载失败（${raw}）。请确认访问的是用户端 frontend-app，并重新执行构建后完整发布 dist/models 和 dist/ort 目录。`
  }
  if (text.includes('timeout') || text.includes('超时') || text.includes('未就绪')) {
    return `姿态模型初始化超时。系统当前使用单线程 WASM，未依赖 WebGL。原始错误：${raw}`
  }
  if (text.includes('webassembly') || text.includes('wasm')) {
    return '当前浏览器的 WASM 推理环境不可用，请更新 Chrome/Edge，或关闭 Trae 内置预览后在系统浏览器中打开。'
  }
  if (text.includes('webgl')) {
    return '当前设备不支持 WebGL，系统会自动切换 WASM；若仍失败，请更新浏览器或更换设备。'
  }
  return raw || '姿态模型初始化失败，请刷新页面后重试'
}

// ---------------- 初始化 ----------------

const init = async (): Promise<void> => {
  if (!Number.isFinite(taskId) || taskId <= 0) {
    stage.value = 'error'
    errorText.value = '任务参数无效，请返回重新进入'
    return
  }
  try {
    // 先读取本地模型清单（静态资源、带缓存），取端侧引擎标识随 start 上送做服务端兜底校验
    loadingText.value = '正在校验本地姿态引擎…'
    const manifest = await loadEngineManifest()

    loadingText.value = '正在下发动作参数…'
    const res = await startTask(taskId, manifest.engineId)
    const snap = res.data
    snapshot.value = snap
    actionName.value = snap.actionName || '训练动作'
    targetReps.value = snap.targetReps
    targetSets.value = snap.targetSets || 1
    instruction.value = snap.instruction || ''

    loadingText.value = '正在加载本地姿态模型（首次加载约 10MB，请稍候）…'
    engine = new PoseAssessmentEngine(snap, { mirror: true })
    const runtime = await engine.loadModel((msg) => {
      loadingText.value = msg
    })
    backendName.value = runtime.backend

    // video 只在 ready 分支渲染。先切换并等待 DOM 更新，再请求摄像头，
    // 否则 videoRef 为空会被误报为“模型初始化超时”。
    stage.value = 'ready'
    sessionStartedAt = new Date()
    await nextTick()
    loadingText.value = '正在请求摄像头权限…'
    await openCamera()
    loop()
  } catch (e) {
    if (e instanceof EngineVersionMismatchError) {
      mismatchInfo.value = { expected: e.expected, actual: e.actual }
      errorText.value = e.message
    } else {
      // 服务端业务错误（如 422 ENGINE_VERSION_MISMATCH）优先展示后端消息
      const bizMessage = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      errorText.value = bizMessage || formatModelError(e)
    }
    stage.value = 'error'
  }
}

/** getUserMedia 错误码 → 患者可读的中文引导（错误码见 WebRTC 规范） */
const mapCameraError = (err: unknown): Error => {
  const name = err instanceof DOMException ? err.name : ''
  const map: Record<string, string> = {
    NotAllowedError: '摄像头权限被拒绝：请点击浏览器地址栏的摄像头图标，选择“始终允许”后重试',
    PermissionDismissedError: '未授权摄像头：请在弹窗中点击“允许”',
    NotFoundError: '未检测到摄像头设备，请确认设备已连接且未被物理遮挡/禁用',
    DevicesNotFoundError: '未检测到摄像头设备，请确认设备已连接',
    NotReadableError: '摄像头被其他应用占用（如视频会议软件），请关闭后重试',
    TrackStartError: '摄像头启动失败，可能被其他应用占用，请关闭后重试',
    OverconstrainedError: '当前摄像头不支持所需分辨率，请更换设备或浏览器重试',
    SecurityError: '当前页面环境禁止使用摄像头（需 HTTPS 或 localhost 访问）',
    AbortError: '摄像头启动中断，请重试'
  }
  return new Error(map[name] ?? (err instanceof Error ? err.message : '无法访问摄像头，请检查设备与权限设置'))
}

const openCamera = async (): Promise<void> => {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error('当前浏览器不支持摄像头采集，请使用最新版 Chrome / Edge / Safari')
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }
    })
  } catch (err) {
    throw mapCameraError(err)
  }
  const video = videoRef.value
  if (!video) throw new Error('视频组件未就绪')
  video.srcObject = stream
  video.muted = true
  video.playsInline = true
  await Promise.race([
    video.play(),
    new Promise<never>((_, reject) => setTimeout(() => reject(new Error('摄像头视频启动超时，请检查浏览器权限和设备占用')), 8000))
  ])
  await Promise.race([
    new Promise<void>((resolve) => {
      if (video.readyState >= 2) resolve()
      else video.addEventListener('loadeddata', () => resolve(), { once: true })
    }),
    new Promise<never>((_, reject) => setTimeout(() => reject(new Error('摄像头没有输出视频帧，请关闭占用摄像头的其他应用后重试')), 8000))
  ])
}

// ---------------- 采集与渲染循环 ----------------

const loop = (): void => {
  if (disposed) return
  rafId = requestAnimationFrame(loop)
  const video = videoRef.value
  if (!video || !engine || processing || stage.value !== 'ready') return
  if (video.readyState < 2 || video.videoWidth === 0) return
  processing = true
  engine
    .handleVideoFrame(video)
    .then((s) => {
      inferenceErrorRun = 0
      state.value = s
      drawSkeleton(s)
    })
    .catch((err) => {
      inferenceErrorRun++
      // 单帧失败允许恢复；连续失败说明后端会话或视频帧已失效，必须显式提示。
      if (inferenceErrorRun >= 3 && stage.value === 'ready') {
        errorText.value = `摄像头姿态推理中断：${err instanceof Error ? err.message : String(err)}`
        stage.value = 'error'
        stopCamera()
      }
    })
    .finally(() => {
      processing = false
    })
}

const drawSkeleton = (s: RealtimeState): void => {
  const video = videoRef.value
  const canvas = canvasRef.value
  if (!video || !canvas) return
  const w = video.videoWidth
  const h = video.videoHeight
  if (w === 0 || h === 0) return
  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w
    canvas.height = h
  }
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)
  ctx.lineWidth = Math.max(2, w / 240)

  const kps = s.frame.keypoints
  // 骨架边
  ctx.strokeStyle = 'rgba(38, 222, 129, 0.85)'
  for (const [a, b] of SKELETON_EDGES) {
    if (!s.present[a] || !s.present[b]) continue
    ctx.beginPath()
    ctx.moveTo(kps[a].x * w, kps[a].y * h)
    ctx.lineTo(kps[b].x * w, kps[b].y * h)
    ctx.stroke()
  }
  // 关键点：正常绿色，补点橙色
  const r = Math.max(3, w / 160)
  for (let i = 0; i < kps.length; i++) {
    if (!s.present[i]) continue
    ctx.beginPath()
    ctx.fillStyle = s.repaired[i] ? '#ff976a' : '#27e07f'
    ctx.arc(kps[i].x * w, kps[i].y * h, r, 0, Math.PI * 2)
    ctx.fill()
  }
}

// ---------------- 结束与结果 ----------------

const handleFinish = async (): Promise<void> => {
  try {
    await showConfirmDialog({
      title: '结束本组训练？',
      message: '结束后将在本机完成姿态复算与评分（视频与关键点不会上传）。'
    })
  } catch {
    return
  }
  if (!engine) return
  ending.value = true
  // 让最后一帧推理完成
  await new Promise((r) => setTimeout(r, 120))
  result.value = engine.finish()
  stage.value = 'result'
  ending.value = false
  stopCamera()
  if (result.value.summary.poseQuality === 'INSUFFICIENT') {
    showFailToast('有效数据不足，本次成绩不计入趋势')
  } else {
    showSuccessToast('评分完成')
  }
  // P4：复算完成后提交训练汇总（INSUFFICIENT 同样落库但不计分）
  await submitSessionResult()
}

// ---------------- 训练记录提交（P4） ----------------

/** 生成客户端请求唯一标识（幂等键；randomUUID 在非安全上下文不可用时退化为随机串） */
const genClientRequestId = (): string => {
  const rand =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID().slice(0, 8)
      : Math.random().toString(36).slice(2, 10)
  return `web-${Date.now()}-${rand}`
}

/** 由引擎复算结果组装提交载荷（不含视频与逐帧关键点） */
const buildSubmitPayload = (): TrainingSubmitPayload | null => {
  const res = result.value
  const snap = snapshot.value
  if (!res || !snap) return null
  const summary = res.summary
  const cycles = res.cycles.filter((c) => !c.ignored && c.basicValid)
  // 关键指标：平均 ROM / 峰谷角与对应帧号 / 保持帧占比
  const avgRom = cycles.length
    ? cycles.reduce((sum, c) => sum + c.rom, 0) / cycles.length
    : undefined
  let peakCycle = cycles[0]
  let troughCycle = cycles[0]
  for (const c of cycles) {
    if (!peakCycle || c.peakAngle > peakCycle.peakAngle) peakCycle = c
    if (!troughCycle || c.startAngle < troughCycle.startAngle) troughCycle = c
  }
  const holdRatio = cycles.length
    ? cycles.reduce((sum, c) => {
        const len = c.endFrame - c.startFrame + 1
        return sum + (len > 0 ? Math.min(1, c.maxHoldFrames / len) : 0)
      }, 0) / cycles.length
    : undefined
  const fps = res.fps > 0 ? res.fps : 1

  return {
    clientRequestId: genClientRequestId(),
    dailyTaskId: taskId,
    planItemId: snap.planItemId,
    actionVersionId: snap.actionVersionId,
    startedAt: localIsoTime(sessionStartedAt ?? new Date()),
    endedAt: localIsoTime(),
    totalReps: summary.totalReps,
    correctReps: summary.correctReps,
    totalScore: Math.round(summary.totalScore * 10) / 10,
    scoreDetail: {
      amplitude: Math.round(summary.scoreDetail.amplitude * 10) / 10,
      hold: Math.round(summary.scoreDetail.hold * 10) / 10,
      rhythm: Math.round(summary.scoreDetail.rhythm * 10) / 10,
      stability: Math.round(summary.scoreDetail.stability * 10) / 10,
      symmetry: Math.round(summary.scoreDetail.symmetry * 10) / 10,
      similarity: Math.round((summary.scoreDetail.similarity ?? 0) * 10) / 10
    },
    poseQuality: summary.poseQuality,
    validFrameRatio: Math.round(summary.validFrameRatio * 10000) / 10000,
    poseRepairedRatio: Math.round(summary.poseRepairedRatio * 10000) / 10000,
    avgRom: avgRom != null ? Math.round(avgRom * 100) / 100 : undefined,
    peakAngle: peakCycle ? Math.round(peakCycle.peakAngle * 100) / 100 : undefined,
    troughAngle: troughCycle ? Math.round(troughCycle.startAngle * 100) / 100 : undefined,
    peakFrameNo: peakCycle?.peakFrame,
    troughFrameNo: troughCycle?.startFrame,
    holdRatio: holdRatio != null ? Math.round(holdRatio * 10000) / 10000 : undefined,
    templateSimilarity:
      summary.scoreDetail.similarity != null
        ? Math.round((summary.scoreDetail.similarity / 100) * 10000) / 10000
        : undefined,
    stabilityScore: Math.round(summary.scoreDetail.stability * 10) / 10,
    symmetryScore: Math.round(summary.scoreDetail.symmetry * 10) / 10,
    errorSummary: res.errorSegments.map((seg) => ({
      ruleId: seg.ruleId,
      dimension: seg.dimension,
      phase: seg.phase,
      jointName: seg.joint || undefined,
      deviation: Math.round(seg.peakDeviation * 100) / 100,
      severity: seg.level,
      frameNo: seg.startFrame,
      promptText: seg.promptText,
      offsetSec: Math.round((seg.startFrame / fps) * 10) / 10
    })),
    poseEngineId: summary.poseEngineId
  }
}

const submitSessionResult = async (): Promise<void> => {
  const payload = buildSubmitPayload()
  if (!payload) {
    submitStatus.value = 'failed'
    submitNote.value = '本机复算结果缺失，未能提交训练记录'
    return
  }
  lastPayload = payload
  submitStatus.value = 'submitting'
  submitNote.value = '正在提交训练记录…'
  try {
    const res = await submitTrainingRecord(payload)
    submitStatus.value = 'ok'
    submitNote.value = res.data.duplicate
      ? '该成绩此前已提交，已保留首次记录'
      : res.data.insufficient
        ? '已提交：有效数据不足，本次不计入完成度与趋势'
        : '训练记录已提交'
  } catch {
    // 断网/超时：缓存原 clientRequestId 与载荷，恢复后原键重试（幂等）
    enqueuePendingSubmission(payload)
    submitStatus.value = 'queued'
    submitNote.value = '网络不可用，成绩已保存在本机，恢复后将自动重试'
  }
}

/** 手动重试（结果页按钮）：复用原 clientRequestId 原键重试，保证幂等不重复落库 */
const retrySubmit = async (): Promise<void> => {
  if (lastPayload) {
    submitStatus.value = 'submitting'
    submitNote.value = '正在重试提交…'
    try {
      const res = await submitTrainingRecord(lastPayload)
      submitStatus.value = 'ok'
      submitNote.value = res.data.duplicate
        ? '该成绩此前已提交，已保留首次记录'
        : '训练记录已提交'
    } catch {
      submitStatus.value = 'queued'
      submitNote.value = '仍未提交成功，已保留在本机，恢复后自动重试'
    }
    return
  }
  await flushPendingSubmissions()
}

const stopCamera = (): void => {
  cancelAnimationFrame(rafId)
  stream?.getTracks().forEach((t) => t.stop())
  stream = null
  const video = videoRef.value
  if (video) video.srcObject = null
}

const goBack = (): void => {
  router.replace('/patient')
}

onMounted(() => {
  // P4：进入训练页时静默冲刷断网期间缓存的历史提交（原 clientRequestId 幂等重试）
  flushPendingSubmissions().catch(() => undefined)
  init()
})
onBeforeUnmount(() => {
  disposed = true
  cancelAnimationFrame(rafId)
  stopCamera()
  engine?.dispose()
  engine = null
})

// ---------------- 展示文案 ----------------

const issueText = (issue: EntryIssue): string => {
  switch (issue) {
    case 'NO_SUBJECT':
      return '未检测到人体，请站入画面中央'
    case 'TOO_FAR':
      return '离镜头太远，请向前走一步（身体占画面约 60%~90%）'
    case 'TOO_CLOSE':
      return '离镜头太近，请后退并确保头脚都在画面内'
    case 'MULTI_OR_JUMP':
      return '画面内疑似多人或移动过快，请保持单人、站稳'
    case 'WARMUP':
      return '请保持姿势稳定，正在校准…'
    default:
      return ''
  }
}

const phaseText = (phase: Phase): string => {
  switch (phase) {
    case 'READY':
      return '准备'
    case 'RAISING':
      return '动作进行中'
    case 'HOLD':
      return '末端保持'
    case 'LOWERING':
      return '缓慢还原'
    default:
      return ''
  }
}

const qualityText = (q: PoseQuality): string => {
  switch (q) {
    case 'GOOD':
      return '数据质量良好'
    case 'FAIR':
      return '数据质量一般'
    case 'INSUFFICIENT':
      return '有效数据不足'
  }
}

const qualityColor = (q: PoseQuality): string => {
  switch (q) {
    case 'GOOD':
      return '#07c160'
    case 'FAIR':
      return '#ff976a'
    case 'INSUFFICIENT':
      return '#ee0a24'
  }
}

const inEntry = computed(() => state.value?.mode === 'ENTRY')
const guideText = computed(() => {
  const s = state.value
  if (!s) return ''
  if (s.mode === 'ENTRY') return issueText(s.entryIssue)
  if (s.broken) return '检测中断，请回到刚才的站位，保持全身入镜'
  return ''
})

const scoreRows = computed(() => {
  const d = result.value?.summary.scoreDetail
  if (!d) return []
  return [
    { label: '幅度', value: d.amplitude },
    { label: '保持', value: d.hold },
    { label: '节奏', value: d.rhythm },
    { label: '稳定', value: d.stability },
    { label: '对称', value: d.symmetry },
    { label: '相似度', value: d.similarity }
  ]
})

const topErrors = computed(() => result.value?.errorSegments.slice(0, 5) ?? [])
</script>

<template>
  <div class="training-page">
    <!-- 加载 / 错误态 -->
    <div v-if="stage === 'loading'" class="center-panel">
      <van-loading size="36px" vertical>{{ loadingText }}</van-loading>
      <p class="privacy-note">所有姿态数据仅在本机处理，视频与关键点不会上传</p>
    </div>

    <div v-else-if="stage === 'error'" class="center-panel">
      <van-icon name="warning-o" size="48" color="#ee0a24" />
      <div class="error-title">无法开始训练</div>
      <div class="error-text">{{ errorText }}</div>
      <div v-if="mismatchInfo" class="mismatch-box">
        <div>端侧引擎：{{ mismatchInfo.expected }}</div>
        <div>动作版本要求：{{ mismatchInfo.actual || '（空）' }}</div>
      </div>
      <van-button type="primary" class="back-btn" @click="goBack">返回今日任务</van-button>
    </div>

    <!-- 训练态 -->
    <template v-else-if="stage === 'ready'">
      <div class="camera-wrap">
        <video ref="videoRef" class="camera" autoplay muted playsinline />
        <canvas ref="canvasRef" class="overlay" />

        <!-- 站位引导 -->
        <div v-if="inEntry" class="guide-mask">
          <div class="guide-title">请按以下要求站位</div>
          <ul class="guide-list">
            <li>全身入镜：从头到脚完整可见</li>
            <li>身体占画面高度约 60%~90%</li>
            <li>画面内仅一人，正对手机</li>
            <li>光线均匀，避免逆光</li>
          </ul>
          <div v-if="guideText" class="guide-live">{{ guideText }}</div>
        </div>

        <!-- 中断提示 -->
        <div v-else-if="guideText" class="break-banner">
          <van-icon name="warning-o" />
          <span>{{ guideText }}</span>
        </div>

        <!-- 顶部计数 HUD -->
        <div v-if="!inEntry" class="hud">
          <div class="hud-row">
            <span class="hud-set">第 1 / {{ targetSets }} 组</span>
            <van-tag plain type="primary">{{ phaseText(state?.phase ?? 'READY') }}</van-tag>
          </div>
          <div class="hud-reps">
            达标 <b>{{ state?.correctReps ?? 0 }}</b> / {{ targetReps }} 次
            <span class="hud-total">（完成 {{ state?.totalReps ?? 0 }} 次）</span>
          </div>
          <van-progress
            :percentage="state?.progressPercent ?? 0"
            stroke-width="6"
            color="#27e07f"
            track-color="rgba(255,255,255,0.35)"
          />
          <div v-if="state?.mainAngle != null" class="hud-angle">
            当前关节角度 {{ state.mainAngle.toFixed(0) }}°
          </div>
        </div>

        <!-- 动作不匹配建议 -->
        <div v-if="state?.mismatchAdvice" class="mismatch-banner">
          连续多次未识别到目标动作，请参照指导语调整；若仍不匹配，建议结束并联系治疗师
        </div>

        <!-- 纠错提示 -->
        <div v-if="!inEntry && state?.corrections?.length" class="correction-stack">
          <div
            v-for="item in state.corrections"
            :key="item.key"
            class="correction-item"
            :class="item.level === 2 ? 'level-2' : 'level-1'"
          >
            <van-icon :name="item.level === 2 ? 'close' : 'info-o'" />
            <span>{{ item.text }}</span>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="bottom-bar">
          <div class="action-name">{{ actionName }}</div>
          <van-button
            type="success"
            round
            block
            :loading="ending"
            loading-text="正在复算评分…"
            @click="handleFinish"
          >
            结束并评分
          </van-button>
          <div class="engine-foot">
            本地推理 · {{ state?.fps ? `${state.fps} FPS` : '—' }}
          </div>
        </div>
      </div>
    </template>

    <!-- 结果态 -->
    <div v-else class="result-panel">
      <div class="result-head">
        <div class="result-action">{{ actionName }}</div>
        <div
          class="result-quality"
          :style="{ color: qualityColor(result?.summary.poseQuality ?? 'INSUFFICIENT') }"
        >
          {{ qualityText(result?.summary.poseQuality ?? 'INSUFFICIENT') }}
        </div>
      </div>

      <div class="score-ring">
        <div class="score-num">{{ Math.round(result?.summary.totalScore ?? 0) }}</div>
        <div class="score-label">综合评分</div>
      </div>

      <van-cell-group inset class="result-group">
        <van-cell title="达标次数" :value="`${result?.summary.correctReps ?? 0} / ${result?.summary.totalReps ?? 0}`" />
        <van-cell
          title="有效帧占比"
          :value="`${Math.round((result?.summary.validFrameRatio ?? 0) * 100)}%`"
        />
        <van-cell
          title="姿态补点占比"
          :value="`${Math.round((result?.summary.poseRepairedRatio ?? 0) * 100)}%`"
        />
        <van-cell title="采集帧率" :value="result?.fps ? `${result.fps} FPS` : '—'" />
      </van-cell-group>

      <div class="dim-grid">
        <div v-for="row in scoreRows" :key="row.label" class="dim-item">
          <div class="dim-value">{{ row.value == null ? '—' : Math.round(row.value) }}</div>
          <div class="dim-label">{{ row.label }}</div>
        </div>
      </div>

      <div v-if="topErrors.length" class="error-block">
        <div class="error-block-title">主要纠错（{{ result?.errorSegments.length }} 段）</div>
        <div v-for="(seg, i) in topErrors" :key="i" class="error-line">
          <van-tag :type="seg.level === 2 ? 'danger' : 'warning'">{{ seg.level }} 级</van-tag>
          <span>{{ seg.promptText }}</span>
        </div>
      </div>

      <!-- 提交状态（P4） -->
      <div v-if="submitStatus" class="submit-box" :class="`submit-${submitStatus}`">
        <template v-if="submitStatus === 'submitting'">
          <van-loading size="14" />
          <span>{{ submitNote }}</span>
        </template>
        <template v-else>
          <van-icon :name="submitStatus === 'ok' ? 'checked' : 'under-way-o'" />
          <span>{{ submitNote }}</span>
          <van-button
            v-if="submitStatus === 'queued' || submitStatus === 'failed'"
            size="small"
            plain
            type="warning"
            @click="retrySubmit"
          >
            重试提交
          </van-button>
        </template>
      </div>

      <p class="local-note">
        仅汇总成绩与纠错摘要会上传；原始视频与逐帧关键点始终保留在本机，不会上传。
      </p>

      <div class="result-actions">
        <van-button block round @click="goBack">返回今日任务</van-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.training-page {
  position: fixed;
  inset: 0;
  background: #0f1115;
  color: #fff;
  overflow: hidden;
}

.center-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 32px;
  text-align: center;

  .privacy-note {
    margin-top: 8px;
    font-size: 12px;
    color: #9ca3af;
    line-height: 1.6;
  }

  .error-title {
    font-size: 18px;
    font-weight: 600;
  }

  .error-text {
    font-size: 14px;
    color: #d1d5db;
    line-height: 1.7;
  }

  .mismatch-box {
    width: 100%;
    max-width: 320px;
    background: rgba(238, 10, 36, 0.12);
    border: 1px solid rgba(238, 10, 36, 0.4);
    border-radius: 8px;
    padding: 10px 12px;
    font-size: 12px;
    color: #fca5a5;
    line-height: 1.8;
    word-break: break-all;
  }

  .back-btn {
    margin-top: 8px;
    width: 220px;
  }
}

.camera-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

.overlay {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.guide-mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 17, 21, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 24px;

  .guide-title {
    font-size: 20px;
    font-weight: 600;
  }

  .guide-list {
    margin: 0;
    padding-left: 20px;
    font-size: 15px;
    line-height: 2;
    color: #e5e7eb;
  }

  .guide-live {
    margin-top: 8px;
    padding: 10px 18px;
    background: rgba(255, 151, 106, 0.18);
    border: 1px solid rgba(255, 151, 106, 0.5);
    border-radius: 20px;
    font-size: 14px;
    color: #ffd9c2;
  }
}

.break-banner {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(238, 10, 36, 0.85);
  border-radius: 18px;
  font-size: 13px;
  white-space: nowrap;
}

.hud {
  position: absolute;
  top: 12px;
  left: 12px;
  right: 12px;
  padding: 10px 12px;
  background: rgba(15, 17, 21, 0.55);
  border-radius: 10px;
  backdrop-filter: blur(4px);

  .hud-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
  }

  .hud-reps {
    margin: 6px 0;
    font-size: 17px;

    b {
      font-size: 24px;
      color: #27e07f;
    }

    .hud-total {
      font-size: 12px;
      color: #cbd5e1;
    }
  }

  .hud-angle {
    margin-top: 4px;
    font-size: 12px;
    color: #cbd5e1;
  }
}

.mismatch-banner {
  position: absolute;
  top: 132px;
  left: 12px;
  right: 12px;
  padding: 8px 12px;
  background: rgba(238, 10, 36, 0.85);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.correction-stack {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 118px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.correction-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.4;

  &.level-1 {
    background: rgba(255, 151, 106, 0.92);
    color: #4a240d;
  }

  &.level-2 {
    background: rgba(238, 10, 36, 0.92);
    color: #fff;
  }
}

.bottom-bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 16px calc(14px + env(safe-area-inset-bottom));
  background: linear-gradient(transparent, rgba(15, 17, 21, 0.85) 30%);

  .action-name {
    text-align: center;
    font-size: 13px;
    color: #d1d5db;
    margin-bottom: 8px;
  }

  .engine-foot {
    margin-top: 6px;
    text-align: center;
    font-size: 11px;
    color: #9ca3af;
  }
}

.result-panel {
  height: 100%;
  overflow-y: auto;
  padding: 20px 0 calc(24px + env(safe-area-inset-bottom));

  .result-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;

    .result-action {
      font-size: 18px;
      font-weight: 600;
    }

    .result-quality {
      font-size: 13px;
      font-weight: 600;
    }
  }

  .score-ring {
    margin: 20px auto 16px;
    width: 148px;
    height: 148px;
    border-radius: 50%;
    border: 8px solid #27e07f;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;

    .score-num {
      font-size: 42px;
      font-weight: 700;
      line-height: 1;
    }

    .score-label {
      margin-top: 6px;
      font-size: 13px;
      color: #9ca3af;
    }
  }

  .result-group {
    margin: 0 16px;
    background: #1a1d24;

    :deep(.van-cell) {
      background: #1a1d24;
      color: #e5e7eb;

      .van-cell__title {
        color: #9ca3af;
      }

      .van-cell__value {
        color: #f3f4f6;
      }
    }
  }

  .dim-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    padding: 16px;

    .dim-item {
      background: #1a1d24;
      border-radius: 10px;
      padding: 12px 4px;
      text-align: center;

      .dim-value {
        font-size: 22px;
        font-weight: 700;
        color: #27e07f;
      }

      .dim-label {
        margin-top: 2px;
        font-size: 12px;
        color: #9ca3af;
      }
    }
  }

  .error-block {
    margin: 0 16px;
    padding: 12px 14px;
    background: #1a1d24;
    border-radius: 10px;

    .error-block-title {
      font-size: 13px;
      color: #9ca3af;
      margin-bottom: 8px;
    }

    .error-line {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      line-height: 1.5;
      color: #e5e7eb;
      margin-bottom: 6px;
    }
  }

  .submit-box {
    margin: 14px 16px 0;
    padding: 10px 14px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    background: #1a1d24;

    &.submit-ok {
      color: #07c160;
    }

    &.submit-queued,
    &.submit-failed {
      color: #ffd21e;
    }

    &.submit-submitting {
      color: #9ca3af;
    }
  }

  .local-note {
    margin: 14px 20px 0;
    font-size: 12px;
    color: #9ca3af;
    line-height: 1.7;
  }

  .result-actions {
    padding: 16px 20px 0;
  }
}
</style>
