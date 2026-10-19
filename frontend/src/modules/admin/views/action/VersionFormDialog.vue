<script setup lang="ts">
import { computed, reactive, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createActionVersion, updateActionVersion, getActionVersionDetail } from '@/api/admin/action'
import type { ActionVersionSavePayload, AdminActionVersion } from '@/api/admin/action'
import { DEFAULT_ALGO_CONFIG } from '@/pose-engine/types'
import type { AlgoConfig } from '@/pose-engine/types'

interface Props {
  actionId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

/** 运动签名结构化表单（与需求书 6.3.7 motion_signature 对应） */
interface MotionSignatureForm {
  targetJoints: string
  constraintJoints: string
  startAngleMin: number | undefined
  startAngleMax: number | undefined
  peakAngleMin: number | undefined
  peakAngleMax: number | undefined
  minRom: number | undefined
  expectedDirection: 'FLEX' | 'EXTEND' | 'MIXED'
  phaseOrder: string
  durationMin: number | undefined
  durationMax: number | undefined
}

const visible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const parseWarned = ref(false)

const actionType = ref<ActionVersionSavePayload['actionType']>('ROM')
const poseEngineId = ref(DEFAULT_ALGO_CONFIG.poseEngineId)

// 算法参数：与 pose-engine AlgoConfig 契约同源（DEFAULT_ALGO_CONFIG）
const algoForm = reactive<AlgoConfig>({ ...DEFAULT_ALGO_CONFIG })

// 运动签名结构化表单
const signatureForm = reactive<MotionSignatureForm>({
  targetJoints: '',
  constraintJoints: '',
  startAngleMin: undefined,
  startAngleMax: undefined,
  peakAngleMin: undefined,
  peakAngleMax: undefined,
  minRom: undefined,
  expectedDirection: 'FLEX',
  phaseOrder: 'READY,RAISING,HOLD,LOWERING',
  durationMin: undefined,
  durationMax: undefined
})

const rules: FormRules = {
  poseEngineId: [
    { required: true, message: '请输入姿态引擎标识', trigger: 'blur' },
    { max: 100, message: '引擎标识最多 100 个字符', trigger: 'blur' }
  ]
}

// 折叠面板默认展开前四个配置组
const activeGroups = ref<string[]>(['gate', 'smooth', 'state', 'weight'])

// ===== 六维权重合计（建议=1，评分公式会按 Σw 归一化，故只提示不拦截） =====
const weightSum = computed(() => {
  const s =
    algoForm.weightAmplitude +
    algoForm.weightHold +
    algoForm.weightRhythm +
    algoForm.weightStability +
    algoForm.weightSymmetry +
    algoForm.weightSimilarity
  return Math.round(s * 1000) / 1000
})
const weightOk = computed(() => Math.abs(weightSum.value - 1) < 0.001)

// ===== JSON 预览 =====
const algoJsonPreview = computed(() => JSON.stringify({ ...algoForm, poseEngineId: poseEngineId.value }, null, 2))

const csvToArray = (s: string): string[] =>
  s
    .split(/[,，]/)
    .map((x) => x.trim())
    .filter(Boolean)

const signatureJsonPreview = computed(() => {
  const obj: Record<string, unknown> = {
    targetJoints: csvToArray(signatureForm.targetJoints),
    constraintJoints: csvToArray(signatureForm.constraintJoints),
    startAngleRange:
      signatureForm.startAngleMin != null || signatureForm.startAngleMax != null
        ? [signatureForm.startAngleMin ?? null, signatureForm.startAngleMax ?? null]
        : undefined,
    peakAngleRange:
      signatureForm.peakAngleMin != null || signatureForm.peakAngleMax != null
        ? [signatureForm.peakAngleMin ?? null, signatureForm.peakAngleMax ?? null]
        : undefined,
    minRom: signatureForm.minRom,
    expectedDirection: signatureForm.expectedDirection,
    phaseOrder: csvToArray(signatureForm.phaseOrder),
    durationRangeSec:
      signatureForm.durationMin != null || signatureForm.durationMax != null
        ? [signatureForm.durationMin ?? null, signatureForm.durationMax ?? null]
        : undefined
  }
  return JSON.stringify(obj, null, 2)
})

const dialogTitle = (): string => (editingId.value == null ? '新增版本' : '编辑版本')

const resetAlgoForm = (): void => {
  Object.assign(algoForm, DEFAULT_ALGO_CONFIG)
}

const resetSignatureForm = (): void => {
  signatureForm.targetJoints = ''
  signatureForm.constraintJoints = ''
  signatureForm.startAngleMin = undefined
  signatureForm.startAngleMax = undefined
  signatureForm.peakAngleMin = undefined
  signatureForm.peakAngleMax = undefined
  signatureForm.minRom = undefined
  signatureForm.expectedDirection = 'FLEX'
  signatureForm.phaseOrder = 'READY,RAISING,HOLD,LOWERING'
  signatureForm.durationMin = undefined
  signatureForm.durationMax = undefined
}

/** 解析库存 JSON 到结构化表单，缺字段以默认值补齐；非法 JSON 回退默认并提示 */
const loadAlgoConfig = (raw?: string): void => {
  resetAlgoForm()
  if (!raw || raw.trim() === '' || raw.trim() === '{}') return
  try {
    const parsed = JSON.parse(raw) as Partial<AlgoConfig>
    Object.assign(algoForm, DEFAULT_ALGO_CONFIG, parsed)
    if (parsed.poseEngineId) poseEngineId.value = parsed.poseEngineId
  } catch {
    parseWarned.value = true
  }
}

const loadSignature = (raw?: string): void => {
  resetSignatureForm()
  if (!raw || raw.trim() === '' || raw.trim() === '{}') return
  try {
    const p = JSON.parse(raw) as Record<string, unknown>
    if (Array.isArray(p.targetJoints)) signatureForm.targetJoints = (p.targetJoints as string[]).join(',')
    if (Array.isArray(p.constraintJoints)) signatureForm.constraintJoints = (p.constraintJoints as string[]).join(',')
    if (Array.isArray(p.startAngleRange)) {
      signatureForm.startAngleMin = (p.startAngleRange[0] as number) ?? undefined
      signatureForm.startAngleMax = (p.startAngleRange[1] as number) ?? undefined
    }
    if (Array.isArray(p.peakAngleRange)) {
      signatureForm.peakAngleMin = (p.peakAngleRange[0] as number) ?? undefined
      signatureForm.peakAngleMax = (p.peakAngleRange[1] as number) ?? undefined
    }
    if (typeof p.minRom === 'number') signatureForm.minRom = p.minRom
    if (p.expectedDirection === 'FLEX' || p.expectedDirection === 'EXTEND' || p.expectedDirection === 'MIXED') {
      signatureForm.expectedDirection = p.expectedDirection
    }
    if (Array.isArray(p.phaseOrder)) signatureForm.phaseOrder = (p.phaseOrder as string[]).join(',')
    if (Array.isArray(p.durationRangeSec)) {
      signatureForm.durationMin = (p.durationRangeSec[0] as number) ?? undefined
      signatureForm.durationMax = (p.durationRangeSec[1] as number) ?? undefined
    }
  } catch {
    parseWarned.value = true
  }
}

const open = (row?: AdminActionVersion): void => {
  editingId.value = row ? row.versionId : null
  parseWarned.value = false
  actionType.value = 'ROM'
  poseEngineId.value = DEFAULT_ALGO_CONFIG.poseEngineId
  resetAlgoForm()
  resetSignatureForm()

  visible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })

  // 编辑时拉取版本详情（列表行不含 algoConfig / motionSignature）
  if (row) {
    actionType.value = row.actionType as ActionVersionSavePayload['actionType']
    if (row.poseEngineId) poseEngineId.value = row.poseEngineId
    void getActionVersionDetail(row.versionId).then((res) => {
      const d = res.data
      loadAlgoConfig(d.algoConfig)
      loadSignature(d.motionSignature)
      if (parseWarned.value) {
        ElMessage.warning('原配置包含非法 JSON，已回退为默认参数，请重新核对后保存')
      }
    })
  }
}

defineExpose({ open })

const handleResetDefaults = (): void => {
  Object.assign(algoForm, DEFAULT_ALGO_CONFIG)
  poseEngineId.value = DEFAULT_ALGO_CONFIG.poseEngineId
  ElMessage.success('已恢复算法参数基线默认值')
}

const handleSubmit = async (): Promise<void> => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload: ActionVersionSavePayload = {
      actionType: actionType.value,
      poseEngineId: poseEngineId.value,
      algoConfig: JSON.stringify({ ...algoForm, poseEngineId: poseEngineId.value }),
      motionSignature: signatureJsonPreview.value
    }
    if (editingId.value == null) {
      await createActionVersion(props.actionId, payload)
      ElMessage.success('版本创建成功')
    } else {
      await updateActionVersion(editingId.value, payload)
      ElMessage.success('版本更新成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 拦截器统一提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="dialogTitle()" width="780px" top="6vh" :close-on-click-modal="false" destroy-on-close>
    <el-form ref="formRef" :model="{ actionType, poseEngineId }" :rules="rules" label-width="130px">
      <el-form-item label="动作类型" prop="actionType">
        <el-radio-group v-model="actionType">
          <el-radio value="ROM">关节活动度</el-radio>
          <el-radio value="STRENGTH">力量</el-radio>
          <el-radio value="BALANCE">平衡</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="姿态引擎标识" prop="poseEngineId">
        <el-input v-model="poseEngineId" placeholder="如 movenet-lightning@sha256:..." clearable maxlength="100" />
      </el-form-item>

      <el-collapse v-model="activeGroups" class="config-collapse">
        <!-- 质量门控 -->
        <el-collapse-item title="质量门控（关键点置信度 / 有效帧）" name="gate">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="高置信阈值">
                <el-input-number v-model="algoForm.confHigh" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="低置信阈值">
                <el-input-number v-model="algoForm.confLow" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="有效帧占比下限">
                <el-input-number v-model="algoForm.validFrameRatioMin" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="修复帧占比上限">
                <el-input-number v-model="algoForm.repairRatioLimit" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 平滑与修复 -->
        <el-collapse-item title="时序平滑与遮挡修复" name="smooth">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="中值滤波窗口">
                <el-input-number v-model="algoForm.medianWindow" :min="1" :max="11" :step="2" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="EMA 平滑系数">
                <el-input-number v-model="algoForm.emaAlpha" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="最大补点帧数">
                <el-input-number v-model="algoForm.maxGapFrames" :min="0" :max="60" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="骨长容差比例">
                <el-input-number v-model="algoForm.boneLenTolerance" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="角速度窗口">
                <el-input-number v-model="algoForm.smoothWindowSpeed" :min="1" :max="31" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="角加速度窗口">
                <el-input-number v-model="algoForm.smoothWindowAccel" :min="1" :max="31" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 阶段判定 -->
        <el-collapse-item title="阶段状态机（去抖 / 滞回 / 周期）" name="state">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="状态保持帧数">
                <el-input-number v-model="algoForm.stateHoldFrames" :min="1" :max="30" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="滞回带宽(°)">
                <el-input-number v-model="algoForm.hysteresisBand" :min="0" :max="45" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="最小周期(秒)">
                <el-input-number v-model="algoForm.minCycleSec" :min="0" :max="30" :step="0.1" :precision="1" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="时间权重σ(秒)">
                <el-input-number v-model="algoForm.timeWeightSigma" :min="0" :max="5" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- 六维评分权重 -->
        <el-collapse-item title="六维评分权重" name="weight">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="幅度">
                <el-input-number v-model="algoForm.weightAmplitude" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="保持">
                <el-input-number v-model="algoForm.weightHold" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="节奏">
                <el-input-number v-model="algoForm.weightRhythm" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="稳定">
                <el-input-number v-model="algoForm.weightStability" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="对称">
                <el-input-number v-model="algoForm.weightSymmetry" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="相似度">
                <el-input-number v-model="algoForm.weightSimilarity" :min="0" :max="1" :step="0.05" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-alert
            :type="weightOk ? 'success' : 'warning'"
            :closable="false"
            show-icon
            :title="weightOk ? `六维权重合计 = ${weightSum}（符合建议值 1）` : `六维权重合计 = ${weightSum}，建议调整为 1（评分时会按权重和归一化，但不规范的配置会导致维度间口径不一致）`"
          />
        </el-collapse-item>

        <!-- 运动签名 -->
        <el-collapse-item title="运动签名（动作合规校验：起始/峰值/阶段顺序）" name="signature">
          <el-form-item label="目标关节">
            <el-input v-model="signatureForm.targetJoints" placeholder="逗号分隔，如 左肩,左肘" clearable />
          </el-form-item>
          <el-form-item label="约束关节">
            <el-input v-model="signatureForm.constraintJoints" placeholder="逗号分隔，如 躯干,右肩" clearable />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="起始角区间(°)">
                <div class="range-input">
                  <el-input-number v-model="signatureForm.startAngleMin" :min="0" :max="360" :precision="0" placeholder="min" />
                  <span class="range-sep">~</span>
                  <el-input-number v-model="signatureForm.startAngleMax" :min="0" :max="360" :precision="0" placeholder="max" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="峰值角区间(°)">
                <div class="range-input">
                  <el-input-number v-model="signatureForm.peakAngleMin" :min="0" :max="360" :precision="0" placeholder="min" />
                  <span class="range-sep">~</span>
                  <el-input-number v-model="signatureForm.peakAngleMax" :min="0" :max="360" :precision="0" placeholder="max" />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="最小 ROM(°)">
                <el-input-number v-model="signatureForm.minRom" :min="0" :max="360" :precision="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="变化方向">
                <el-select v-model="signatureForm.expectedDirection" style="width: 100%">
                  <el-option label="屈曲（角度减小）" value="FLEX" />
                  <el-option label="伸展（角度增大）" value="EXTEND" />
                  <el-option label="双向/混合" value="MIXED" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="阶段顺序">
            <el-input v-model="signatureForm.phaseOrder" placeholder="逗号分隔，如 READY,RAISING,HOLD,LOWERING" clearable />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="允许时长(秒)">
                <div class="range-input">
                  <el-input-number v-model="signatureForm.durationMin" :min="0" :precision="1" placeholder="min" />
                  <span class="range-sep">~</span>
                  <el-input-number v-model="signatureForm.durationMax" :min="0" :precision="1" placeholder="max" />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>

        <!-- JSON 预览 -->
        <el-collapse-item title="提交 JSON 预览（algo_config / motion_signature）" name="preview">
          <div class="json-label">algo_config：</div>
          <pre class="json-preview">{{ algoJsonPreview }}</pre>
          <div class="json-label mt">motion_signature：</div>
          <pre class="json-preview">{{ signatureJsonPreview }}</pre>
        </el-collapse-item>
      </el-collapse>

      <div class="reset-bar">
        <el-button link type="primary" @click="handleResetDefaults">恢复算法参数基线默认值</el-button>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts">
export default { name: 'VersionFormDialog' }
</script>

<style scoped lang="scss">
.config-collapse {
  margin-top: 4px;
  border-top: none;

  :deep(.el-collapse-item__header) {
    font-weight: 600;
    color: #1f2937;
  }
}

.range-input {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;

  .el-input-number {
    flex: 1;
  }

  .range-sep {
    color: #909399;
  }
}

.json-label {
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;

  &.mt {
    margin-top: 10px;
  }
}

.json-preview {
  margin: 0;
  max-height: 220px;
  overflow: auto;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #303133;
}

.reset-bar {
  margin-top: 8px;
  text-align: right;
}
</style>
