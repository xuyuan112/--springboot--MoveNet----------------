<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { askAi, MEDICAL_DISCLAIMER } from '@/api/patient/ai'

interface ChatMessage {
  role: 'user' | 'ai'
  text: string
}

const router = useRouter()

const messages = ref<ChatMessage[]>([])
const input = ref('')
const sending = ref(false)
const scrollEl = ref<HTMLDivElement>()

/** 常驻免责声明（与后端 Constants.MEDICAL_DISCLAIMER 对齐） */
const noticeText = `AI 回复${MEDICAL_DISCLAIMER}，如有不适请及时就医。`

const scrollToBottom = (): void => {
  nextTick(() => {
    const el = scrollEl.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

const send = async (): Promise<void> => {
  const question = input.value.trim()
  if (!question || sending.value) return

  messages.value.push({ role: 'user', text: question })
  input.value = ''
  sending.value = true
  scrollToBottom()
  try {
    // 会话不持久化：每次独立问答，仅存于当前页面内存
    const res = await askAi(question)
    messages.value.push({ role: 'ai', text: res.data.answer })
  } catch {
    // 错误提示（含 429 限流）由响应拦截器统一 toast，这里补一条占位保持对话连贯
    messages.value.push({ role: 'ai', text: '本次回答生成失败，请稍后重试。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const clearChat = (): void => {
  if (!messages.value.length) return
  messages.value = []
}

onMounted(scrollToBottom)
</script>

<template>
  <div class="ai-page">
    <van-nav-bar title="AI 健康问答" right-text="清空" @click-right="clearChat" />

    <!-- 常驻免责声明 -->
    <van-notice-bar
      wrapable
      :scrollable="false"
      left-icon="warning-o"
      color="#ed6a0c"
      background="#fffbe8"
    >
      {{ noticeText }}
    </van-notice-bar>

    <!-- 正式报告入口提示 -->
    <div class="report-entry" @click="router.push({ name: 'PatientReports' })">
      <van-icon name="records" class="entry-icon" />
      <div class="entry-text">
        <div class="entry-title">查看正式评估报告</div>
        <div class="entry-sub">AI 生成小结仅为辅助摘要，不替代治疗师审核的正式报告</div>
      </div>
      <van-icon name="arrow" class="entry-arrow" />
    </div>

    <!-- 对话区 -->
    <div ref="scrollEl" class="chat-area">
      <van-empty
        v-if="messages.length === 0"
        image-size="90"
        description="输入您的问题开始咨询，例如：训练后膝盖轻微酸痛正常吗？"
      />

      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="msg-row"
        :class="msg.role === 'user' ? 'from-user' : 'from-ai'"
      >
        <div class="bubble">{{ msg.text }}</div>
      </div>

      <div v-if="sending" class="msg-row from-ai">
        <div class="bubble thinking">
          <van-loading size="14" /> 思考中…
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-bar">
      <van-field
        v-model="input"
        type="textarea"
        rows="1"
        autosize
        maxlength="500"
        placeholder="请输入您的问题（每日提问次数有限）"
        :disabled="sending"
        @keydown.enter.exact.prevent="send"
      />
      <van-button type="primary" size="small" :loading="sending" :disabled="!input.trim()" @click="send">
        发送
      </van-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 50px);
}

.report-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 12px 0;
  padding: 10px 12px;
  background: #e8f4ff;
  border-radius: 8px;

  &:active {
    opacity: 0.85;
  }
}

.entry-icon {
  font-size: 22px;
  color: #1989fa;
}

.entry-text {
  flex: 1;
  min-width: 0;
}

.entry-title {
  font-size: 13px;
  font-weight: 600;
  color: #1989fa;
}

.entry-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #969799;
}

.entry-arrow {
  color: #c8c9cc;
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  display: flex;

  &.from-user {
    justify-content: flex-end;

    .bubble {
      background: #1989fa;
      color: #fff;
    }
  }

  &.from-ai {
    justify-content: flex-start;

    .bubble {
      background: #fff;
      color: #323233;
    }
  }
}

.bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;

  &.thinking {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #969799;
  }
}

.input-bar {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 12px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid #f0f0f0;

  .van-field {
    flex: 1;
    background: #f7f8fa;
    border-radius: 8px;
  }

  .van-button {
    flex-shrink: 0;
  }
}
</style>
