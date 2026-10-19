<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import { getTrainingRecordDetail, withdrawTrainingRecord } from '@/api/patient/training'
import type { TrainingRecordDetail } from '@/api/patient/training'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()

const recordId = Number(route.params.recordId)
const loading = ref(true)
const notFound = ref(false)
const detail = ref<TrainingRecordDetail | null>(null)

const fetchDetail = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getTrainingRecordDetail(recordId)
    detail.value = res.data
  } catch {
    // 404（不存在/他人记录不泄露存在性）等：展示空态
    notFound.value = true
  } finally {
    loading.value = false
  }
}

// ===== 展示辅助 =====
const isInsufficient = computed(() => detail.value?.poseQuality === 'INSUFFICIENT')

const scoreDetailMap = computed<Record<string, number>>(() => {
  const raw = detail.value?.scoreDetail
  return (raw ?? {}) as Record<string, number>
})

/** 0~1 比例 → 百分比文案 */
const showPercent = (v?: number | null): string =>
  v === null || v === undefined ? '--' : `${(v * 100).toFixed(1)}%`

/** 角度 → 整数度数文案 */
const showAngle = (v?: number | null): string =>
  v === null || v === undefined ? '--' : `${Math.round(v)}°`

/** 0~100 得分文案 */
const showScore = (v?: number | null): string =>
  v === null || v === undefined ? '--' : String(Math.round(v))

const DIMENSION_NAMES: Record<string, string> = {
  ANGLE: '角度偏差',
  STABILITY: '稳定性',
  SYMMETRY: '对称性',
  HOLD: '保持时长',
  SPEED: '动作速度',
  ACTION_MISMATCH: '动作不匹配'
}

const PHASE_NAMES: Record<string, string> = {
  READY: '预备',
  RAISING: '抬起',
  HOLD: '保持',
  LOWERING: '放下',
  FULL: '全程'
}

const dimensionName = (key: string): string => DIMENSION_NAMES[key] ?? key
const phaseName = (key?: string): string => (key ? (PHASE_NAMES[key] ?? key) : '')

const severityText = (severity: number): string => (severity >= 2 ? '严重' : '提醒')

// ===== 六维雷达图（内联 SVG 手绘，无第三方图表依赖） =====
const RADAR_DIMS = [
  { key: 'amplitude', label: '幅度' },
  { key: 'hold', label: '保持' },
  { key: 'rhythm', label: '节奏' },
  { key: 'stability', label: '稳定' },
  { key: 'symmetry', label: '对称' },
  { key: 'similarity', label: '相似' }
] as const

const CX = 110
const CY = 100
const R = 64

const radarAngle = (i: number): number => ((-90 + i * 60) * Math.PI) / 180
const radarPoint = (i: number, r: number): string =>
  `${(CX + r * Math.cos(radarAngle(i))).toFixed(2)},${(CY + r * Math.sin(radarAngle(i))).toFixed(2)}`

/** 三层网格多边形 */
const gridPolygons = computed(() =>
  [1 / 3, 2 / 3, 1].map((f) => RADAR_DIMS.map((_, i) => radarPoint(i, R * f)).join(' '))
)

/** 各维度得分顶点连成的值多边形 */
const valuePolygon = computed(() =>
  RADAR_DIMS.map((dim, i) => {
    const v = Math.max(0, Math.min(100, Number(scoreDetailMap.value[dim.key] ?? 0)))
    return radarPoint(i, (R * v) / 100)
  }).join(' ')
)

const radarLabels = computed(() =>
  RADAR_DIMS.map((dim, i) => ({
    label: dim.label,
    value: Math.round(Number(scoreDetailMap.value[dim.key] ?? 0)),
    x: CX + (R + 18) * Math.cos(radarAngle(i)),
    y: CY + (R + 18) * Math.sin(radarAngle(i))
  }))
)

const hasScoreDetail = computed(() =>
  RADAR_DIMS.some((dim) => scoreDetailMap.value[dim.key] !== undefined)
)

// ===== 纠错明细展开面板 =====
const activeLogs = ref<number[]>([])

// ===== 撤回（二次确认 + 原因必填） =====
const withdrawPopupVisible = ref(false)
const withdrawReason = ref('')
const withdrawing = ref(false)

const onWithdraw = async (): Promise<void> => {
  try {
    await showConfirmDialog({
      title: '撤回本次训练记录？',
      message: '撤回后记录不可恢复，且不再计入任务进度与趋势；已纳入阶段报告的记录无法撤回。',
      confirmButtonText: '继续撤回',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  withdrawReason.value = ''
  withdrawPopupVisible.value = true
}

const confirmWithdraw = async (): Promise<void> => {
  const reason = withdrawReason.value.trim()
  if (!reason) {
    showToast('请填写撤回原因')
    return
  }
  withdrawing.value = true
  try {
    await withdrawTrainingRecord(recordId, reason)
    showSuccessToast('记录已撤回')
    withdrawPopupVisible.value = false
    router.replace({ name: 'PatientRecords' })
  } catch {
    // 409 已纳入报告等错误由响应拦截器统一提示
  } finally {
    withdrawing.value = false
  }
}

onMounted(fetchDetail)
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="训练详情" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" class="loading-block" vertical>加载中…</van-loading>

    <van-empty v-else-if="notFound || !detail" description="训练记录不存在或已被撤回" />

    <template v-else>
      <!-- 总览卡片 -->
      <div class="summary-card">
        <div class="summary-head">
          <span class="action-name">{{ detail.actionName || '训练动作' }}</span>
          <van-tag :type="isInsufficient ? 'default' : 'success'">
            {{ isInsufficient ? '数据不足' : detail.poseQuality }}
          </van-tag>
        </div>
        <div class="score-line">
          <span class="score-num">{{ detail.totalScore }}</span>
          <span class="score-unit">总分</span>
          <span class="reps">{{ detail.correctReps }} / {{ detail.totalReps }} 次</span>
        </div>
        <van-notice-bar
          v-if="isInsufficient"
          wrapable
          :scrollable="false"
          text="本次训练有效数据不足，成绩不计入任务进度与趋势"
        />
        <div class="meta-lines">
          <div v-if="detail.planName">计划：{{ detail.planName }}</div>
          <div>开始：{{ formatDateTime(detail.startedAt) }}</div>
          <div v-if="detail.endedAt">结束：{{ formatDateTime(detail.endedAt) }}</div>
          <div v-if="detail.poseEngineId">姿态引擎：{{ detail.poseEngineId }}</div>
        </div>
      </div>

      <!-- 六维评分雷达图 -->
      <div class="section-card">
        <div class="section-title">分维度评分</div>
        <div v-if="hasScoreDetail" class="radar-wrap">
          <svg viewBox="0 0 220 200" class="radar-svg" role="img" aria-label="六维评分雷达图">
            <polygon
              v-for="(poly, idx) in gridPolygons"
              :key="`grid-${idx}`"
              :points="poly"
              fill="none"
              stroke="#e5e7eb"
              stroke-width="1"
            />
            <line
              v-for="(_, i) in RADAR_DIMS"
              :key="`axis-${i}`"
              :x1="CX"
              :y1="CY"
              :x2="CX + R * Math.cos(radarAngle(i))"
              :y2="CY + R * Math.sin(radarAngle(i))"
              stroke="#e5e7eb"
              stroke-width="1"
            />
            <polygon :points="valuePolygon" fill="rgba(25, 137, 250, 0.25)" stroke="#1989fa" stroke-width="2" />
            <text
              v-for="(label, i) in radarLabels"
              :key="`label-${i}`"
              :x="label.x"
              :y="label.y"
              text-anchor="middle"
              dominant-baseline="middle"
              class="radar-label"
            >
              {{ label.label }} {{ label.value }}
            </text>
          </svg>
        </div>
        <van-empty v-else image-size="64" description="暂无分项评分" />
      </div>

      <!-- 关键指标 -->
      <div class="section-card">
        <div class="section-title">关键指标</div>
        <div class="metric-grid">
          <div class="metric-item">
            <div class="metric-value">{{ showAngle(detail.avgRom) }}</div>
            <div class="metric-label">平均活动幅度</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showPercent(detail.holdRatio) }}</div>
            <div class="metric-label">保持占比</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showPercent(detail.templateSimilarity) }}</div>
            <div class="metric-label">模板相似度</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showScore(detail.stabilityScore) }}</div>
            <div class="metric-label">稳定性得分</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showScore(detail.symmetryScore) }}</div>
            <div class="metric-label">对称性得分</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showPercent(detail.validFrameRatio) }}</div>
            <div class="metric-label">有效帧占比</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showAngle(detail.peakAngle) }}</div>
            <div class="metric-label">峰值角度</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ showAngle(detail.troughAngle) }}</div>
            <div class="metric-label">谷值角度</div>
          </div>
        </div>
      </div>

      <!-- 纠错摘要与明细 -->
      <div class="section-card">
        <div class="section-title">纠错摘要</div>
        <template v-if="(detail.errorSummary?.length ?? 0) > 0 || (detail.errorLogs?.length ?? 0) > 0">
          <div v-if="(detail.errorSummary?.length ?? 0) > 0" class="summary-chips">
            <van-tag
              v-for="group in detail.errorSummary"
              :key="group.dimension"
              plain
              type="danger"
              size="large"
            >
              {{ dimensionName(group.dimension) }} ×{{ group.count }}
            </van-tag>
          </div>
          <van-collapse v-if="(detail.errorLogs?.length ?? 0) > 0" v-model="activeLogs">
            <van-collapse-item
              v-for="log in detail.errorLogs"
              :key="log.errorId"
              :name="log.errorId"
            >
              <template #title>
                <span class="log-title">
                  {{ dimensionName(log.dimension) }}
                  <span v-if="phaseName(log.phase)"> · {{ phaseName(log.phase) }}</span>
                  <van-tag :type="log.severity >= 2 ? 'danger' : 'warning'" class="log-severity">
                    {{ severityText(log.severity) }}
                  </van-tag>
                </span>
              </template>
              <div class="log-body">
                <div class="log-prompt">{{ log.promptText }}</div>
                <div class="log-meta">
                  <span v-if="log.jointName">部位：{{ log.jointName }}</span>
                  <span v-if="log.deviation !== null && log.deviation !== undefined">
                    偏差：{{ log.deviation }}°
                  </span>
                  <span v-if="log.frameNo !== null && log.frameNo !== undefined">
                    帧：{{ log.frameNo }}
                  </span>
                  <span v-if="log.errorAt">{{ formatDateTime(log.errorAt) }}</span>
                </div>
              </div>
            </van-collapse-item>
          </van-collapse>
        </template>
        <van-empty v-else image-size="64" description="本次训练无纠错提示" />
      </div>

      <!-- 撤回 -->
      <div class="withdraw-area">
        <van-button type="danger" plain block :loading="withdrawing" @click="onWithdraw">
          撤回本次训练记录
        </van-button>
        <div class="withdraw-tip">撤回需填写原因，撤回后记录不可恢复</div>
      </div>
    </template>

    <!-- 撤回原因填写 -->
    <van-popup
      v-model:show="withdrawPopupVisible"
      position="bottom"
      round
      :style="{ paddingBottom: '16px' }"
    >
      <div class="withdraw-popup">
        <div class="popup-title">填写撤回原因</div>
        <van-field
          v-model="withdrawReason"
          type="textarea"
          rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请填写撤回原因（必填，最长 200 字）"
        />
        <van-button type="danger" block :loading="withdrawing" @click="confirmWithdraw">
          确认撤回
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped lang="scss">
.loading-block {
  padding: 48px 0;
}

.summary-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 12px;

  .summary-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .action-name {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .score-line {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin: 10px 0 4px;

    .score-num {
      font-size: 32px;
      font-weight: 700;
      color: #1989fa;
    }

    .score-unit {
      font-size: 12px;
      color: #6b7280;
    }

    .reps {
      margin-left: auto;
      font-size: 13px;
      color: #4b5563;
    }
  }

  .meta-lines {
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    font-size: 12px;
    color: #9ca3af;

    word-break: break-all;
  }
}

.section-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 0 12px 12px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
  }
}

.radar-wrap {
  display: flex;
  justify-content: center;

  .radar-svg {
    width: 100%;
    max-width: 320px;
    height: auto;
  }

  .radar-label {
    font-size: 11px;
    fill: #4b5563;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;

  .metric-item {
    text-align: center;
    background: #f7f8fa;
    border-radius: 6px;
    padding: 8px 2px;

    .metric-value {
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
    }

    .metric-label {
      margin-top: 2px;
      font-size: 11px;
      color: #9ca3af;
    }
  }
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.log-title {
  font-size: 13px;
  color: #1f2937;

  .log-severity {
    margin-left: 6px;
  }
}

.log-body {
  .log-prompt {
    font-size: 13px;
    color: #374151;
  }

  .log-meta {
    margin-top: 6px;
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    font-size: 12px;
    color: #9ca3af;
  }
}

.withdraw-area {
  margin: 4px 12px 24px;

  .withdraw-tip {
    margin-top: 8px;
    text-align: center;
    font-size: 12px;
    color: #9ca3af;
  }
}

.withdraw-popup {
  padding: 16px;

  .popup-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 8px;
    text-align: center;
  }
}
</style>
