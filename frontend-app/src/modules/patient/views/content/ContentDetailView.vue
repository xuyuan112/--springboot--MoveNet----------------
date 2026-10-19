<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showSuccessToast } from 'vant'
import {
  collectContent,
  getContentDetail,
  listCollections,
  uncollectContent
} from '@/api/patient/content'
import type { ContentAttachment, ContentDetail } from '@/api/patient/content'
import { toMinioProxyUrl } from '@/utils/minio'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const contentId = Number(route.params.contentId)

const loading = ref(true)
const loadFailed = ref(false)
const detail = ref<ContentDetail | null>(null)

/** 收藏态：详情 VO 不含收藏标志，通过本人收藏列表判断 */
const collected = ref(false)
const collectLoading = ref(false)

/** 封面图取附件中 type=COVER 的预签名地址 */
const coverUrl = computed(() => {
  const cover = detail.value?.attachments?.find((a) => a.type === 'COVER')
  return cover ? toMinioProxyUrl(cover.url) : ''
})

/** 正文区展示的附件（排除封面） */
const bodyAttachments = computed<ContentAttachment[]>(() =>
  (detail.value?.attachments ?? []).filter((a) => a.type !== 'COVER')
)

const formatSize = (size?: number): string => {
  if (!size) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const loadDetail = async (): Promise<void> => {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getContentDetail(contentId)
    detail.value = res.data
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

const loadCollectedState = async (): Promise<void> => {
  try {
    const res = await listCollections({ page: 1, size: 200 })
    collected.value = (res.data.records ?? []).some((c) => c.contentId === contentId)
  } catch {
    // 收藏态获取失败不阻塞浏览，按钮仍可交互
  }
}

const toggleCollect = async (): Promise<void> => {
  if (collectLoading.value) return
  collectLoading.value = true
  try {
    if (collected.value) {
      await uncollectContent(contentId)
      collected.value = false
      showSuccessToast('已取消收藏')
    } else {
      await collectContent(contentId)
      collected.value = true
      showSuccessToast('已收藏')
    }
  } catch {
    // 错误提示由响应拦截器统一处理
  } finally {
    collectLoading.value = false
  }
}

const openAttachment = (att: ContentAttachment): void => {
  window.open(toMinioProxyUrl(att.url), '_blank')
}

onMounted(() => {
  loadDetail()
  loadCollectedState()
})
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="内容详情" left-arrow @click-left="$router.back()" />

    <van-loading v-if="loading" class="page-loading" vertical>加载中…</van-loading>

    <van-empty v-else-if="loadFailed || !detail" description="内容不存在或已下架">
      <van-button round type="primary" size="small" @click="$router.back()">返回</van-button>
    </van-empty>

    <template v-else>
      <img v-if="coverUrl" class="cover-img" :src="coverUrl" alt="" />

      <div class="detail-body">
        <h1 class="detail-title">{{ detail.title }}</h1>
        <div class="detail-meta">
          <van-tag type="primary" plain>{{ detail.contentTypeName }}</van-tag>
          <van-tag v-if="detail.targetPartName" plain>{{ detail.targetPartName }}</van-tag>
          <span v-if="detail.publishedAt" class="meta-time">
            发布于 {{ formatDateTime(detail.publishedAt) }}
          </span>
        </div>

        <!-- 正文 -->
        <div class="detail-content">{{ detail.contentBody }}</div>

        <!-- 附件 -->
        <template v-if="bodyAttachments.length > 0">
          <div class="attach-title">附件（{{ bodyAttachments.length }}）</div>
          <div
            v-for="att in bodyAttachments"
            :key="att.id"
            class="attach-item"
            @click="openAttachment(att)"
          >
            <!-- 图片附件直接预览 -->
            <template v-if="att.contentType?.startsWith('image/')">
              <img class="attach-image" :src="toMinioProxyUrl(att.url)" alt="" />
              <div class="attach-name">{{ att.name }}</div>
            </template>

            <!-- 视频附件内联播放 -->
            <template v-else-if="att.contentType?.startsWith('video/')">
              <video
                class="attach-video"
                :src="toMinioProxyUrl(att.url)"
                controls
                preload="metadata"
                playsinline
              />
              <div class="attach-name">{{ att.name }}</div>
            </template>

            <!-- 其他（文档等）提供下载入口 -->
            <template v-else>
              <div class="attach-row">
                <van-icon name="description" class="attach-icon" />
                <div class="attach-info">
                  <div class="attach-name">{{ att.name }}</div>
                  <div class="attach-size">
                    {{ formatSize(att.size) }}<span v-if="att.urlExpiresAt"> · 链接有效期至 {{ formatDateTime(att.urlExpiresAt) }}</span>
                  </div>
                </div>
                <van-icon name="arrow" class="attach-arrow" />
              </div>
            </template>
          </div>
        </template>
      </div>

      <!-- 收藏按钮（幂等，可反复切换） -->
      <div class="collect-bar">
        <van-button
          round
          block
          :type="collected ? 'default' : 'primary'"
          :loading="collectLoading"
          :icon="collected ? 'star' : 'star-o'"
          @click="toggleCollect"
        >
          {{ collected ? '取消收藏' : '收藏' }}
        </van-button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.page-loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.cover-img {
  width: 100%;
  display: block;
  object-fit: cover;
  max-height: 220px;
}

.detail-body {
  background: #fff;
  padding: 16px;
  margin-top: -12px;
  border-radius: 12px 12px 0 0;
  position: relative;
}

.detail-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #323233;
  line-height: 1.4;
}

.detail-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.meta-time {
  font-size: 12px;
  color: #969799;
}

.detail-content {
  margin-top: 16px;
  font-size: 15px;
  line-height: 1.8;
  color: #323233;
  white-space: pre-wrap;
  word-break: break-word;
}

.attach-title {
  margin-top: 24px;
  padding-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #323233;
  border-bottom: 1px solid #f0f0f0;
}

.attach-item {
  margin-top: 12px;
}

.attach-image {
  width: 100%;
  border-radius: 6px;
  display: block;
}

.attach-video {
  width: 100%;
  border-radius: 6px;
  display: block;
  background: #000;
}

.attach-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 6px;

  &:active {
    background: #f2f3f5;
  }
}

.attach-icon {
  font-size: 22px;
  color: #1989fa;
}

.attach-info {
  flex: 1;
  min-width: 0;
}

.attach-name {
  margin-top: 4px;
  font-size: 13px;
  color: #646566;
}

.attach-size {
  font-size: 11px;
  color: #969799;
}

.attach-arrow {
  color: #c8c9cc;
}

.collect-bar {
  padding: 12px 16px calc(16px + constant(safe-area-inset-bottom));
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
  background: #fff;
  position: sticky;
  bottom: 0;
}
</style>
