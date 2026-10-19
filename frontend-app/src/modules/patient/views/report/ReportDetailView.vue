<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getReportDetail } from '@/api/patient/report'
import type { ReportDetail, ScoreAverages } from '@/api/patient/report'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const reportId = Number(route.params.reportId)

const loading = ref(true)
const loadFailed = ref(false)
const report = ref<ReportDetail | null>(null)

/** 六维分项评分（与后端 score_averages 键一致） */
const DIMENSIONS: { key: keyof ScoreAverages; label: string }[] = [
  { key: 'amplitude', label: '动作幅度' },
  { key: 'hold', label: '姿态保持' },
  { key: 'rhythm', label: '动作节奏' },
  { key: 'stability', label: '动作稳定性' },
  { key: 'symmetry', label: '左右对称性' },
  { key: 'similarity', label: '标准相似度' }
]

/** 姿态质量标签映射 */
const QUALITY_LABELS: Record<string, string> = {
  GOOD: '优',
  FAIR: '良',
  POOR: '差',
  INSUFFICIENT: '数据不足'
}

const QUALITY_TAG_TYPES: Record<string, 'success' | 'primary' | 'danger' | 'default'> = {
  GOOD: 'success',
  FAIR: 'primary',
  POOR: 'danger',
  INSUFFICIENT: 'default'
}

/** 纠错维度标签映射（AssessmentDimension） */
const DIMENSION_LABELS: Record<string, string> = {
  ANGLE: '角度',
  STABILITY: '稳定性',
  SYMMETRY: '对称性',
  HOLD: '保持',
  SPEED: '速度'
}

const dimLabel = (code: string): string => DIMENSION_LABELS[code] ?? code

const loadDetail = async (): Promise<void> => {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getReportDetail(reportId)
    report.value = res.data
  } catch {
    // DRAFT/AI_GENERATED 等未生效报告后端返回 404，此处统一兜底展示
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

const qualityEntries = (): [string, number][] => Object.entries(report.value?.qualityDistribution ?? {})
const dimensionEntries = (): [string, number][] =>
  Object.entries(report.value?.errorDimensionDistribution ?? {})

onMounted(loadDetail)
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="报告详情" left-arrow @click-left="$router.back()" />

    <van-loading v-if="loading" class="page-loading" vertical>加载中…</van-loading>

    <van-empty
      v-else-if="loadFailed || !report"
      description="报告不存在或尚未对您生效"
    >
      <van-button round type="primary" size="small" @click="$router.back()">返回</van-button>
    </van-empty>

    <template v-else>
      <!-- 报告头 -->
      <div class="report-hero">
        <div class="hero-plan">{{ report.planName }}（V{{ report.planVersion ?? '-' }}）</div>
        <div class="hero-period">{{ report.periodStart }} ~ {{ report.periodEnd }}</div>
        <div class="hero-score">
          <span class="score-num">{{ report.avgTotalScore ?? '-' }}</span>
          <span class="score-unit">平均总分</span>
        </div>
      </div>

      <!-- 免责声明：显著展示 -->
      <van-notice-bar
        wrapable
        :scrollable="false"
        left-icon="warning-o"
        color="#ed6a0c"
        background="#fffbe8"
      >
        {{ report.disclaimer }}
      </van-notice-bar>

      <!-- 总体统计 -->
      <van-cell-group inset title="训练统计" class="block">
        <van-grid :column-num="2" :border="false">
          <van-grid-item text-center>
            <template #default>
              <div class="stat-num">{{ report.completedTasks ?? 0 }}/{{ report.totalTasks ?? 0 }}</div>
              <div class="stat-label">任务完成（{{ report.completionRate ?? 0 }}%）</div>
            </template>
          </van-grid-item>
          <van-grid-item text-center>
            <template #default>
              <div class="stat-num">{{ report.recordCount ?? 0 }}</div>
              <div class="stat-label">训练记录（含无效 {{ report.insufficientCount ?? 0 }} 次）</div>
            </template>
          </van-grid-item>
          <van-grid-item text-center>
            <template #default>
              <div class="stat-num">{{ report.correctReps ?? 0 }}/{{ report.totalReps ?? 0 }}</div>
              <div class="stat-label">有效达标 / 总次数</div>
            </template>
          </van-grid-item>
          <van-grid-item text-center>
            <template #default>
              <div class="stat-num">{{ report.statusName || '已生效' }}</div>
              <div class="stat-label">报告状态</div>
            </template>
          </van-grid-item>
        </van-grid>
      </van-cell-group>

      <!-- 六维分项评分 -->
      <van-cell-group inset title="分项评分" class="block">
        <div class="score-list">
          <div v-for="dim in DIMENSIONS" :key="dim.key" class="score-row">
            <span class="score-label">{{ dim.label }}</span>
            <van-progress
              class="score-bar"
              :percentage="report.scoreAverages?.[dim.key] ?? 0"
              stroke-width="8"
              :show-pivot="false"
            />
            <span class="score-value">{{ report.scoreAverages?.[dim.key] ?? '-' }}</span>
          </div>
        </div>
      </van-cell-group>

      <!-- 姿态质量分布 -->
      <van-cell-group inset title="姿态质量分布" class="block">
        <div v-if="qualityEntries().length" class="tag-wrap">
          <van-tag
            v-for="[quality, count] in qualityEntries()"
            :key="quality"
            size="large"
            :type="QUALITY_TAG_TYPES[quality] ?? 'default'"
            class="quality-tag"
          >
            {{ QUALITY_LABELS[quality] ?? quality }} × {{ count }}
          </van-tag>
        </div>
        <van-empty v-else description="周期内暂无训练记录" image-size="64" />
      </van-cell-group>

      <!-- 纠错维度分布 -->
      <van-cell-group inset title="常见纠错维度" class="block">
        <div v-if="dimensionEntries().length" class="tag-wrap">
          <van-tag
            v-for="[dim, count] in dimensionEntries()"
            :key="dim"
            size="large"
            type="warning"
            plain
            class="quality-tag"
          >
            {{ dimLabel(dim) }} × {{ count }}
          </van-tag>
        </div>
        <van-empty v-else description="周期内无纠错记录，继续保持" image-size="64" />
      </van-cell-group>

      <!-- 治疗师审核意见（只读） -->
      <van-cell-group inset title="治疗师审核意见" class="block">
        <template v-if="report.therapistConclusion">
          <div class="conclusion">{{ report.therapistConclusion }}</div>
          <div class="conclusion-time">审核时间：{{ formatDateTime(report.reviewedAt) }}</div>
        </template>
        <van-empty v-else description="治疗师尚未填写审核意见" image-size="64" />
      </van-cell-group>

      <div class="page-foot-hint">本报告由系统基于训练数据自动统计，仅供参考</div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.block {
  margin-top: 12px;
}

.report-hero {
  background: linear-gradient(135deg, #1989fa, #39a9ed);
  color: #fff;
  padding: 20px 16px;
}

.hero-plan {
  font-size: 16px;
  font-weight: 600;
}

.hero-period {
  margin-top: 4px;
  font-size: 13px;
  opacity: 0.9;
}

.hero-score {
  margin-top: 12px;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.score-num {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
}

.score-unit {
  font-size: 13px;
  opacity: 0.9;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #323233;
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: #969799;
}

.score-list {
  padding: 8px 16px 12px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;

  &:first-child {
    margin-top: 4px;
  }
}

.score-label {
  width: 76px;
  flex-shrink: 0;
  font-size: 13px;
  color: #646566;
}

.score-bar {
  flex: 1;
}

.score-value {
  width: 36px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #1989fa;
}

.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
}

.quality-tag {
  margin: 0;
}

.conclusion {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #323233;
  white-space: pre-wrap;
}

.conclusion-time {
  padding: 0 16px 12px;
  font-size: 12px;
  color: #969799;
}

.page-foot-hint {
  padding: 16px 0 24px;
  text-align: center;
  font-size: 12px;
  color: #c8c9cc;
}
</style>
