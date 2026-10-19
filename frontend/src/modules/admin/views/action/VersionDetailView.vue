<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getActionVersionDetail, listActionRules, createActionRule, updateActionRule, deleteActionRule, pageActionTemplates, createActionTemplate, publishActionTemplate, deleteActionTemplate } from '@/api/admin/action'
import type { AdminActionVersionDetail, AdminActionRule, AdminActionTemplate, ActionRulePayload, ActionTemplateSavePayload } from '@/api/admin/action'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const versionId = Number(route.params.versionId)

// ===== 规则权重和（实时，与后端发布校验口径一致：必须 = 1） =====
const ruleWeightSum = computed(() => {
  const s = ruleList.value.reduce((acc, r) => acc + Number(r.scoreWeight ?? 0), 0)
  return Math.round(s * 1000) / 1000
})
const ruleWeightOk = computed(() => Math.abs(ruleWeightSum.value - 1) < 0.001)

/** JSON 美化展示，非法 JSON 原样返回 */
const prettyJson = (raw?: string): string => {
  if (!raw) return '-'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

// ===== 版本详情 =====
const loading = ref(false)
const version = ref<AdminActionVersionDetail>()

const fetchDetail = async (): Promise<void> => {
  loading.value = true
  try {
    const res = await getActionVersionDetail(versionId)
    version.value = res.data
    // 规则数据跟随版本详情加载
    ruleList.value = res.data.ruleList ?? []
    templateList.value = res.data.templateList ?? []
  } catch {
    // 拦截器统一提示
  } finally {
    loading.value = false
  }
}

// ===== 规则管理 =====
const ruleList = ref<AdminActionRule[]>([])
const ruleDialogVisible = ref(false)
const ruleSubmitting = ref(false)
const editingRuleId = ref<number | null>(null)

const ruleForm = reactive<ActionRulePayload>({
  jointName: '',
  keypointIndices: '',
  angleFormula: '',
  jointRole: 'PRIMARY',
  dimension: 'ANGLE',
  phase: 'FULL',
  minAngle: undefined,
  maxAngle: undefined,
  standardAngle: undefined,
  toleranceSoft: undefined,
  toleranceHard: undefined,
  holdFramesRequired: undefined,
  speedMin: undefined,
  speedMax: undefined,
  scoreWeight: 0,
  severity: '',
  promptText: '',
  symmetryPairRuleId: undefined
})

const resetRuleForm = (): void => {
  ruleForm.jointName = ''
  ruleForm.keypointIndices = ''
  ruleForm.angleFormula = ''
  ruleForm.jointRole = 'PRIMARY'
  ruleForm.dimension = 'ANGLE'
  ruleForm.phase = 'FULL'
  ruleForm.minAngle = undefined
  ruleForm.maxAngle = undefined
  ruleForm.standardAngle = undefined
  ruleForm.toleranceSoft = undefined
  ruleForm.toleranceHard = undefined
  ruleForm.holdFramesRequired = undefined
  ruleForm.speedMin = undefined
  ruleForm.speedMax = undefined
  ruleForm.scoreWeight = 0
  ruleForm.severity = ''
  ruleForm.promptText = ''
  ruleForm.symmetryPairRuleId = undefined
}

const openRuleDialog = (row?: AdminActionRule): void => {
  editingRuleId.value = row ? row.ruleId : null
  resetRuleForm()
  if (row) {
    ruleForm.jointName = row.jointName
    ruleForm.keypointIndices = row.keypointIndices || ''
    ruleForm.angleFormula = row.angleFormula || ''
    ruleForm.jointRole = row.jointRole as ActionRulePayload['jointRole']
    ruleForm.dimension = row.dimension as ActionRulePayload['dimension']
    ruleForm.phase = row.phase as ActionRulePayload['phase']
    ruleForm.minAngle = row.minAngle
    ruleForm.maxAngle = row.maxAngle
    ruleForm.standardAngle = row.standardAngle
    ruleForm.toleranceSoft = row.toleranceSoft
    ruleForm.toleranceHard = row.toleranceHard
    ruleForm.holdFramesRequired = row.holdFramesRequired
    ruleForm.speedMin = row.speedMin
    ruleForm.speedMax = row.speedMax
    ruleForm.scoreWeight = row.scoreWeight
    ruleForm.severity = row.severity || ''
    ruleForm.promptText = row.promptText || ''
    ruleForm.symmetryPairRuleId = row.symmetryPairRuleId
  }
  ruleDialogVisible.value = true
}

const handleSaveRule = async (): Promise<void> => {
  ruleSubmitting.value = true
  try {
    const payload: ActionRulePayload = {
      ...ruleForm,
      keypointIndices: ruleForm.keypointIndices || undefined,
      angleFormula: ruleForm.angleFormula || undefined,
      severity: ruleForm.severity || undefined,
      promptText: ruleForm.promptText || undefined,
      symmetryPairRuleId: ruleForm.symmetryPairRuleId || undefined
    }
    if (editingRuleId.value == null) {
      await createActionRule(versionId, payload)
      ElMessage.success('规则创建成功')
    } else {
      await updateActionRule(versionId, editingRuleId.value, payload)
      ElMessage.success('规则更新成功')
    }
    ruleDialogVisible.value = false
    fetchDetail()
  } catch {
    // 拦截器统一提示
  } finally {
    ruleSubmitting.value = false
  }
}

const handleDeleteRule = (row: AdminActionRule): void => {
  ElMessageBox.confirm(`确认删除规则「${row.jointName}」？`, '删除规则', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteActionRule(versionId, row.ruleId)
      ElMessage.success('规则已删除')
      fetchDetail()
    })
    .catch(() => {
      // 用户取消
    })
}

// ===== 模板管理 =====
const templateList = ref<AdminActionTemplate[]>([])
const tplDialogVisible = ref(false)
const tplSubmitting = ref(false)
const tplForm = reactive<ActionTemplateSavePayload>({
  sourceType: 'THERAPIST',
  frameRate: undefined,
  frameCount: undefined,
  featureDim: undefined,
  featureSequence: '',
  keyframeIndex: '',
  sampleCount: undefined
})

const resetTplForm = (): void => {
  tplForm.sourceType = 'THERAPIST'
  tplForm.frameRate = undefined
  tplForm.frameCount = undefined
  tplForm.featureDim = undefined
  tplForm.featureSequence = ''
  tplForm.keyframeIndex = ''
  tplForm.sampleCount = undefined
}

const openTplDialog = (): void => {
  resetTplForm()
  tplDialogVisible.value = true
}

const handleSaveTpl = async (): Promise<void> => {
  tplSubmitting.value = true
  try {
    const payload: ActionTemplateSavePayload = {
      ...tplForm,
      featureSequence: tplForm.featureSequence || undefined,
      keyframeIndex: tplForm.keyframeIndex || undefined
    }
    await createActionTemplate(versionId, payload)
    ElMessage.success('模板创建成功')
    tplDialogVisible.value = false
    fetchDetail()
  } catch {
    // 拦截器统一提示
  } finally {
    tplSubmitting.value = false
  }
}

const handlePublishTpl = (row: AdminActionTemplate): void => {
  ElMessageBox.confirm(`确认发布模板 #${row.templateId}？`, '发布模板', {
    confirmButtonText: '确认发布',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await publishActionTemplate(row.templateId)
      ElMessage.success('模板已发布')
      fetchDetail()
    })
    .catch(() => {
      // 用户取消
    })
}

const handleDeleteTpl = (row: AdminActionTemplate): void => {
  ElMessageBox.confirm(`确认删除模板 #${row.templateId}？`, '删除模板', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteActionTemplate(row.templateId)
      ElMessage.success('模板已删除')
      fetchDetail()
    })
    .catch(() => {
      // 用户取消
    })
}

const isDraft = () => version.value?.status === 'DRAFT'

// ===== 模板特征只读预览 =====
const previewVisible = ref(false)
const previewTpl = ref<AdminActionTemplate>()

const openTplPreview = (row: AdminActionTemplate): void => {
  previewTpl.value = row
  previewVisible.value = true
}

const handleCopyFeature = async (raw?: string): Promise<void> => {
  if (!raw) return
  try {
    await navigator.clipboard.writeText(raw)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="page-container">
    <!-- 返回 + 基本信息 -->
    <el-card shadow="never" class="info-card">
      <div class="header-info">
        <el-button link type="primary" @click="router.go(-1)">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <span class="divider">|</span>
        <span class="title">版本详情</span>
      </div>

      <el-descriptions v-loading="loading" :column="3" border class="mt-12">
        <el-descriptions-item label="版本号">V{{ version?.versionNo ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="动作类型">{{ version?.actionTypeName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="version?.status === 'PUBLISHED' ? 'success' : version?.status === 'DRAFT' ? 'warning' : 'info'"
            size="small"
          >
            {{ version?.statusName ?? '-' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="姿态引擎">{{ version?.poseEngineId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(version?.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(version?.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="算法配置" :span="3">
          <pre class="json-box">{{ prettyJson(version?.algoConfig) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="运动签名" :span="3">
          <pre class="json-box">{{ prettyJson(version?.motionSignature) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 规则管理 -->
    <el-card shadow="never" class="section-card">
      <div class="page-toolbar">
        <span class="table-title">规则列表</span>
        <div class="toolbar-right">
          <el-tag :type="ruleWeightOk ? 'success' : 'danger'" size="default" effect="light">
            规则权重和：{{ ruleWeightSum }} {{ ruleWeightOk ? '✓ 可发布' : `（差额 ${Math.round((1 - ruleWeightSum) * 1000) / 1000}，发布要求 = 1）` }}
          </el-tag>
          <el-button type="primary" :disabled="!isDraft()" @click="openRuleDialog()">新增规则</el-button>
        </div>
      </div>

      <el-table :data="ruleList" border stripe style="width: 100%">
        <el-table-column prop="ruleId" label="ID" width="70" />
        <el-table-column prop="jointName" label="关节名称" min-width="120" />
        <el-table-column prop="jointRole" label="角色" min-width="100" />
        <el-table-column prop="dimension" label="评估维度" min-width="100" />
        <el-table-column prop="phase" label="阶段" min-width="100" />
        <el-table-column prop="scoreWeight" label="权重" width="80" />
        <el-table-column prop="standardAngle" label="标准角" width="80" />
        <el-table-column prop="minAngle" label="最小角" width="80" />
        <el-table-column prop="maxAngle" label="最大角" width="80" />
        <el-table-column prop="toleranceSoft" label="软容差" width="80" />
        <el-table-column prop="toleranceHard" label="硬容差" width="80" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="!isDraft()"
              @click="openRuleDialog(row as AdminActionRule)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="!isDraft()"
              @click="handleDeleteRule(row as AdminActionRule)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="ruleList.length === 0" class="empty-tip">暂无规则，请点击「新增规则」添加</div>
    </el-card>

    <!-- 模板管理 -->
    <el-card shadow="never" class="section-card">
      <div class="page-toolbar">
        <span class="table-title">模板列表</span>
        <el-button type="primary" :disabled="!isDraft()" @click="openTplDialog()">新增模板</el-button>
      </div>

      <el-table :data="templateList" border stripe style="width: 100%">
        <el-table-column prop="templateId" label="ID" width="80" />
        <el-table-column prop="sourceTypeName" label="来源" min-width="110" />
        <el-table-column prop="frameRate" label="帧率" width="80" />
        <el-table-column prop="frameCount" label="帧数" width="80" />
        <el-table-column prop="featureDim" label="特征维度" width="90" />
        <el-table-column prop="sampleCount" label="样本数" width="80" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="openTplPreview(row as AdminActionTemplate)"
            >
              查看特征
            </el-button>
            <el-button
              link
              type="success"
              size="small"
              :disabled="row.status === 'ACTIVE'"
              @click="handlePublishTpl(row as AdminActionTemplate)"
            >
              发布
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDeleteTpl(row as AdminActionTemplate)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="templateList.length === 0" class="empty-tip">暂无模板，请点击「新增模板」添加</div>
    </el-card>

    <!-- 规则弹窗 -->
    <el-dialog v-model="ruleDialogVisible" :title="editingRuleId ? '编辑规则' : '新增规则'" width="640px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="ruleForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关节名称" required>
              <el-input v-model="ruleForm.jointName" placeholder="如 左膝关节" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关节角色" required>
              <el-radio-group v-model="ruleForm.jointRole">
                <el-radio value="PRIMARY">主评估</el-radio>
                <el-radio value="AUXILIARY">辅助</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="评估维度" required>
              <el-select v-model="ruleForm.dimension" style="width: 100%">
                <el-option label="角度" value="ANGLE" />
                <el-option label="稳定性" value="STABILITY" />
                <el-option label="对称性" value="SYMMETRY" />
                <el-option label="保持" value="HOLD" />
                <el-option label="速度" value="SPEED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阶段" required>
              <el-select v-model="ruleForm.phase" style="width: 100%">
                <el-option label="准备阶段" value="START" />
                <el-option label="向心阶段" value="CONCENTRIC" />
                <el-option label="离心阶段" value="ECCENTRIC" />
                <el-option label="保持阶段" value="HOLD" />
                <el-option label="全程" value="FULL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="标准角">
              <el-input-number v-model="ruleForm.standardAngle" :min="0" :max="180" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最小角">
              <el-input-number v-model="ruleForm.minAngle" :min="0" :max="180" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大角">
              <el-input-number v-model="ruleForm.maxAngle" :min="0" :max="180" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="软容差">
              <el-input-number v-model="ruleForm.toleranceSoft" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="硬容差">
              <el-input-number v-model="ruleForm.toleranceHard" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="评分权重" required>
              <el-input-number v-model="ruleForm.scoreWeight" :min="0" :max="1" :step="0.1" :precision="3" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="最小速度">
              <el-input-number v-model="ruleForm.speedMin" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大速度">
              <el-input-number v-model="ruleForm.speedMax" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保持帧数">
              <el-input-number v-model="ruleForm.holdFramesRequired" :min="0" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="关键点索引">
          <el-input v-model="ruleForm.keypointIndices" placeholder="如 11,13,15" clearable />
        </el-form-item>
        <el-form-item label="角度公式">
          <el-input v-model="ruleForm.angleFormula" placeholder="如 angle(11,13,15)" clearable />
        </el-form-item>
        <el-form-item label="严重级别">
          <el-input v-model="ruleForm.severity" placeholder="如 HIGH / MEDIUM / LOW" clearable />
        </el-form-item>
        <el-form-item label="提示文本">
          <el-input v-model="ruleForm.promptText" type="textarea" :rows="2" placeholder="提示用户纠正的文本" />
        </el-form-item>
        <el-form-item label="对称规则ID">
          <el-input-number v-model="ruleForm.symmetryPairRuleId" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSubmitting" @click="handleSaveRule">确定</el-button>
      </template>
    </el-dialog>

    <!-- 模板弹窗 -->
    <el-dialog v-model="tplDialogVisible" title="新增模板" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form :model="tplForm" label-width="100px">
        <el-form-item label="来源类型" required>
          <el-radio-group v-model="tplForm.sourceType">
            <el-radio value="THERAPIST">治疗师录制</el-radio>
            <el-radio value="ALGO">算法生成</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="帧率">
              <el-input-number v-model="tplForm.frameRate" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="帧数">
              <el-input-number v-model="tplForm.frameCount" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="特征维度">
              <el-input-number v-model="tplForm.featureDim" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="样本数">
              <el-input-number v-model="tplForm.sampleCount" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="特征序列">
          <el-input v-model="tplForm.featureSequence" type="textarea" :rows="3" placeholder="JSON 格式特征序列" />
        </el-form-item>
        <el-form-item label="关键帧索引">
          <el-input v-model="tplForm.keyframeIndex" placeholder="如 [0, 30, 60]" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="tplDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tplSubmitting" @click="handleSaveTpl">确定</el-button>
      </template>
    </el-dialog>
    <!-- 模板特征只读预览弹窗 -->
    <el-dialog v-model="previewVisible" title="标准动作模板特征预览" width="720px" :close-on-click-modal="false">
      <el-descriptions :column="3" border size="small" v-if="previewTpl">
        <el-descriptions-item label="模板ID">{{ previewTpl.templateId }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ previewTpl.sourceTypeName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ previewTpl.statusName }}</el-descriptions-item>
        <el-descriptions-item label="帧率">{{ previewTpl.frameRate ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="帧数">{{ previewTpl.frameCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="特征维度">{{ previewTpl.featureDim ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="样本数">{{ previewTpl.sampleCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(previewTpl.createdAt) }}</el-descriptions-item>
      </el-descriptions>

      <div class="preview-section">
        <div class="preview-head">
          <span class="preview-label">归一化特征序列（feature_sequence，只读）</span>
          <el-button link type="primary" size="small" @click="handleCopyFeature(previewTpl?.featureSequence)">复制</el-button>
        </div>
        <pre class="json-box large">{{ prettyJson(previewTpl?.featureSequence) }}</pre>
      </div>
      <div class="preview-section">
        <div class="preview-head">
          <span class="preview-label">关键帧索引（keyframe_index，只读）</span>
          <el-button link type="primary" size="small" @click="handleCopyFeature(previewTpl?.keyframeIndex)">复制</el-button>
        </div>
        <pre class="json-box">{{ prettyJson(previewTpl?.keyframeIndex) }}</pre>
      </div>

      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.info-card {
  margin-bottom: 16px;

  .header-info {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;

    .divider {
      color: #dcdfe6;
    }

    .title {
      font-weight: 600;
      color: #1f2937;
    }
  }

  .mt-12 {
    margin-top: 12px;
  }
}

.section-card {
  margin-bottom: 16px;

  .table-title {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .empty-tip {
    padding: 24px 0;
    text-align: center;
    color: #909399;
    font-size: 14px;
  }
}

.json-box {
  margin: 0;
  max-height: 180px;
  overflow: auto;
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;

  &.large {
    max-height: 320px;
  }
}

.preview-section {
  margin-top: 14px;

  .preview-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;

    .preview-label {
      font-size: 13px;
      font-weight: 600;
      color: #1f2937;
    }
  }
}
</style>
