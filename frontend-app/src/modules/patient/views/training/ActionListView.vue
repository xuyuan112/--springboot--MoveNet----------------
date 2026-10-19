<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getEnabledRehabParts } from '@/api/common/dict'
import type { RehabPartDictVO } from '@/api/common/dict'
import { getActionDetail, listActions } from '@/api/patient/action'
import type { PatientActionDetail, PatientActionItem } from '@/api/patient/action'

const SIZE = 10

// ===== 筛选 =====
const keyword = ref('')
const partCode = ref<string>('')
const difficulty = ref<string>('')
const parts = ref<RehabPartDictVO[]>([])

const partOptions = computed(() => [
  { text: '全部部位', value: '' },
  ...parts.value.map((p) => ({ text: p.name, value: p.code }))
])

const difficultyOptions = [
  { text: '全部难度', value: '' },
  { text: '简单', value: 'EASY' },
  { text: '中等', value: 'MEDIUM' },
  { text: '困难', value: 'HARD' }
]

const difficultyTagType = (d?: string): 'success' | 'warning' | 'danger' | 'default' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'default'> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return map[d ?? ''] ?? 'default'
}

// ===== 列表分页 =====
const actions = ref<PatientActionItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

const fetchPage = async (): Promise<void> => {
  const res = await listActions({
    page: page.value,
    size: SIZE,
    partCode: partCode.value || undefined,
    difficulty: difficulty.value || undefined,
    keyword: keyword.value.trim() || undefined
  })
  const list = res.data.records ?? []
  if (page.value === 1) {
    actions.value = list
  } else {
    actions.value.push(...list)
  }
  total.value = res.data.total ?? 0
  page.value += 1
  if (recordsExhausted(list)) {
    finished.value = true
  }
}

const recordsExhausted = (list: PatientActionItem[]): boolean =>
  list.length < SIZE || actions.value.length >= total.value

/** 首屏/筛选变化：重置后拉第一页 */
const reload = async (): Promise<void> => {
  loading.value = true
  actions.value = []
  page.value = 1
  finished.value = false
  try {
    await fetchPage()
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

/** van-list 滚动到底触发翻页 */
const onLoad = async (): Promise<void> => {
  if (finished.value) {
    loading.value = false
    return
  }
  try {
    await fetchPage()
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

// ===== 版本快照详情（只读弹层） =====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<PatientActionDetail | null>(null)
const jsonActiveNames = ref<string[]>([])

/** 美化 JSON 展示；解析失败回退原文 */
const prettyJson = (raw?: string): string => {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

const openDetail = async (item: PatientActionItem): Promise<void> => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  jsonActiveNames.value = []
  try {
    const res = await getActionDetail(item.actionVersionId)
    detail.value = res.data
  } catch {
    // 错误提示由响应拦截器统一处理
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  reload()
  try {
    const res = await getEnabledRehabParts()
    parts.value = res.data ?? []
  } catch {
    // 部位字典加载失败不阻塞列表，仅缺少下拉选项
  }
})
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="动作库" />

    <van-search
      v-model="keyword"
      placeholder="搜索动作名称 / 描述"
      @search="reload"
      @clear="reload"
    />

    <van-dropdown-menu>
      <van-dropdown-item v-model="partCode" :options="partOptions" @change="reload" />
      <van-dropdown-item v-model="difficulty" :options="difficultyOptions" @change="reload" />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="reload">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-empty v-if="!loading && actions.length === 0" description="暂无已发布动作" />

        <div
          v-for="action in actions"
          :key="action.actionVersionId"
          class="action-card"
          @click="openDetail(action)"
        >
          <div class="action-head">
            <span class="action-name">{{ action.name }}</span>
            <van-tag :type="difficultyTagType(action.difficulty)">
              {{ action.difficultyName || action.difficulty || '未知难度' }}
            </van-tag>
          </div>
          <div class="action-desc">{{ action.description || '暂无描述' }}</div>
          <div class="action-meta">
            <span v-if="action.partName">{{ action.partName }}</span>
            <span v-if="action.actionType">{{ action.actionType }}</span>
            <span>V{{ action.versionNo ?? '-' }}</span>
          </div>
        </div>
      </van-list>
    </van-pull-refresh>

    <!-- 版本快照详情（只读） -->
    <van-popup
      v-model:show="detailVisible"
      position="bottom"
      round
      :style="{ height: '80%' }"
    >
      <div class="detail-popup">
        <van-nav-bar title="动作详情" left-arrow @click-left="detailVisible = false" />
        <van-loading v-if="detailLoading" class="loading-block" vertical>加载中…</van-loading>
        <template v-else-if="detail">
          <div class="detail-scroll">
            <div class="detail-head">
              <div class="detail-name">{{ detail.name }}</div>
              <div class="detail-tags">
                <van-tag v-if="detail.partName" type="primary" plain>{{ detail.partName }}</van-tag>
                <van-tag :type="difficultyTagType(detail.difficulty)" plain>
                  {{ detail.difficultyName || detail.difficulty }}
                </van-tag>
                <van-tag plain>V{{ detail.versionNo ?? '-' }}</van-tag>
                <van-tag v-if="detail.actionType" plain>{{ detail.actionType }}</van-tag>
              </div>
              <div class="detail-desc">{{ detail.description || '暂无描述' }}</div>
            </div>

            <!-- 评估规则（只读快照） -->
            <div class="detail-section">
              <div class="section-title">评估规则（{{ detail.rules?.length ?? 0 }} 条）</div>
              <div
                v-for="rule in detail.rules ?? []"
                :key="rule.actionRuleId"
                class="rule-card"
              >
                <div class="rule-head">
                  <span>{{ rule.jointName || rule.dimension || '规则' }}</span>
                  <span class="rule-weight">权重 {{ rule.scoreWeight ?? '-' }}</span>
                </div>
                <div class="rule-rows">
                  <div v-if="rule.dimension"><label>维度</label>{{ rule.dimension }}</div>
                  <div v-if="rule.phase"><label>阶段</label>{{ rule.phase }}</div>
                  <div v-if="rule.minAngle !== null && rule.minAngle !== undefined || rule.maxAngle !== null && rule.maxAngle !== undefined">
                    <label>标准区间</label>{{ rule.minAngle ?? '-' }}° ~ {{ rule.maxAngle ?? '-' }}°
                  </div>
                  <div v-if="rule.standardAngle !== null && rule.standardAngle !== undefined">
                    <label>标准角度</label>{{ rule.standardAngle }}°
                  </div>
                  <div v-if="rule.toleranceSoft !== null && rule.toleranceSoft !== undefined">
                    <label>软容差</label>±{{ rule.toleranceSoft }}°
                  </div>
                  <div v-if="rule.toleranceHard !== null && rule.toleranceHard !== undefined">
                    <label>硬容差</label>±{{ rule.toleranceHard }}°
                  </div>
                  <div v-if="rule.holdFramesRequired"><label>保持帧数</label>{{ rule.holdFramesRequired }}</div>
                  <div v-if="rule.severity"><label>严重度</label>{{ rule.severity === 2 ? '严重' : '提醒' }}</div>
                </div>
                <div v-if="rule.promptText" class="rule-prompt">{{ rule.promptText }}</div>
              </div>
              <van-empty
                v-if="(detail.rules?.length ?? 0) === 0"
                image-size="64"
                description="该版本未配置评估规则"
              />
            </div>

            <!-- 算法参数 / 运动签名（只读 JSON） -->
            <van-collapse v-model="jsonActiveNames" class="json-collapse">
              <van-collapse-item v-if="detail.algoConfig" title="算法参数（algo_config）" name="algo">
                <pre class="json-pre">{{ prettyJson(detail.algoConfig) }}</pre>
              </van-collapse-item>
              <van-collapse-item v-if="detail.motionSignature" title="运动签名（motion_signature）" name="signature">
                <pre class="json-pre">{{ prettyJson(detail.motionSignature) }}</pre>
              </van-collapse-item>
            </van-collapse>

            <div v-if="detail.poseEngineId" class="engine-line">
              姿态引擎：{{ detail.poseEngineId }}
            </div>
          </div>
        </template>
      </div>
    </van-popup>
  </div>
</template>

<style scoped lang="scss">
.action-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin: 0 12px 12px;

  .action-head {
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

  .action-desc {
    margin-top: 4px;
    font-size: 13px;
    color: #6b7280;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .action-meta {
    margin-top: 8px;
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #9ca3af;
  }
}

.detail-popup {
  display: flex;
  flex-direction: column;
  height: 100%;

  .loading-block {
    padding: 48px 0;
  }

  .detail-scroll {
    flex: 1;
    overflow-y: auto;
    padding-bottom: 16px;
  }
}

.detail-head {
  padding: 12px;

  .detail-name {
    font-size: 17px;
    font-weight: 600;
    color: #1f2937;
  }

  .detail-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
  }

  .detail-desc {
    margin-top: 8px;
    font-size: 13px;
    color: #6b7280;
  }
}

.detail-section {
  padding: 0 12px;

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin: 8px 0;
  }
}

.rule-card {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 8px;

  .rule-head {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 600;
    color: #1f2937;

    .rule-weight {
      font-weight: 400;
      color: #1989fa;
    }
  }

  .rule-rows {
    margin-top: 6px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 4px 12px;
    font-size: 12px;
    color: #4b5563;

    label {
      color: #9ca3af;
      margin-right: 4px;
    }
  }

  .rule-prompt {
    margin-top: 6px;
    font-size: 12px;
    color: #b45309;
  }
}

.json-collapse {
  margin: 8px 12px 0;
}

.json-pre {
  margin: 0;
  max-height: 240px;
  overflow: auto;
  font-size: 11px;
  line-height: 1.5;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-all;
}

.engine-line {
  padding: 8px 12px 0;
  font-size: 12px;
  color: #9ca3af;
  word-break: break-all;
}
</style>
