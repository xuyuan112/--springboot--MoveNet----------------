<script setup lang="ts">
import { reactive, ref } from 'vue'
import { showFailToast } from 'vant'
import { getMyLoginLogs } from '@/api/common/user'
import type { LoginLogVO } from '@/api/common/user'
import { formatDateTime } from '@/utils/datetime'

/** 弹层可见性 */
const visible = defineModel<boolean>('visible', { required: true })

const logs = ref<LoginLogVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20 })

/** van-list 加载状态 */
const listLoading = ref(false)
const listFinished = ref(false)

const onLoad = async (): Promise<void> => {
  try {
    const res = await getMyLoginLogs({ page: query.page, size: query.size })
    logs.value.push(...res.data.records)
    total.value = Number(res.data.total)
    if (logs.value.length >= total.value || res.data.records.length === 0) {
      listFinished.value = true
    } else {
      query.page += 1
    }
  } catch {
    showFailToast('登录日志加载失败')
    listFinished.value = true
  } finally {
    listLoading.value = false
  }
}

/** 每次打开时重置并重新加载 */
const handleOpen = (): void => {
  logs.value = []
  query.page = 1
  listFinished.value = false
  listLoading.value = true
  onLoad()
}
</script>

<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    round
    :style="{ height: '80%' }"
    @open="handleOpen"
  >
    <div class="log-panel">
      <div class="log-title">登录日志</div>
      <van-list
        v-model:loading="listLoading"
        :finished="listFinished"
        finished-text="没有更多了"
        class="log-list"
        @load="onLoad"
      >
        <van-cell v-for="log in logs" :key="log.logId" :title="formatDateTime(log.createdAt)">
          <template #label>
            <div class="log-line">
              <van-tag :type="log.action === 'LOGIN' ? 'primary' : 'default'">
                {{ log.action === 'LOGIN' ? '登录' : '登出' }}
              </van-tag>
              <van-tag :type="log.status === 'SUCCESS' ? 'success' : 'danger'">
                {{ log.status === 'SUCCESS' ? '成功' : '失败' }}
              </van-tag>
              <span class="log-ip">{{ log.ip || '—' }}</span>
            </div>
            <div v-if="log.message" class="log-msg">{{ log.message }}</div>
          </template>
        </van-cell>
      </van-list>
    </div>
  </van-popup>
</template>

<style scoped lang="scss">
.log-panel {
  height: 100%;
  display: flex;
  flex-direction: column;

  .log-title {
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    padding: 16px 0 8px;
    flex-shrink: 0;
  }

  .log-list {
    flex: 1;
    overflow-y: auto;
  }

  .log-line {
    display: flex;
    align-items: center;
    gap: 6px;

    .log-ip {
      font-size: 12px;
      color: #9ca3af;
    }
  }

  .log-msg {
    margin-top: 4px;
    font-size: 12px;
    color: #6b7280;
  }
}
</style>
