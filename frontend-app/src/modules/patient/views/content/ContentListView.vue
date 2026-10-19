<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  listCollections,
  listContents
} from '@/api/patient/content'
import type { CollectionItem, ContentItem } from '@/api/patient/content'
import { getEnabledRehabParts } from '@/api/common/dict'
import type { RehabPartDictVO } from '@/api/common/dict'
import { toMinioProxyUrl } from '@/utils/minio'
import { formatDateTime } from '@/utils/datetime'

const router = useRouter()
const SIZE = 10

// ===== Tab：全部内容 / 我的收藏 =====
const activeTab = ref<'all' | 'collect'>('all')

// ===== 全部内容：筛选 =====
const keyword = ref('')
const partCode = ref('')
const category = ref('')
const parts = ref<RehabPartDictVO[]>([])

const partOptions = computed(() => [
  { text: '全部部位', value: '' },
  ...parts.value.map((p) => ({ text: p.name, value: p.code }))
])

const categoryOptions = [
  { text: '全部类型', value: '' },
  { text: '图文文章', value: 'ARTICLE' },
  { text: '视频', value: 'VIDEO' },
  { text: '指南', value: 'GUIDE' }
]

// ===== 全部内容：分页 =====
const contents = ref<ContentItem[]>([])
const allPage = ref(1)
const allLoading = ref(false)
const allFinished = ref(false)
const allError = ref(false)
const allRefreshing = ref(false)

const fetchContentPage = async (): Promise<void> => {
  const res = await listContents({
    page: allPage.value,
    size: SIZE,
    partCode: partCode.value || undefined,
    category: category.value || undefined,
    keyword: keyword.value.trim() || undefined
  })
  const list = res.data.records ?? []
  if (allPage.value === 1) {
    contents.value = list
  } else {
    contents.value.push(...list)
  }
  allPage.value += 1
  if (list.length < SIZE) {
    allFinished.value = true
  }
}

const reloadContents = async (): Promise<void> => {
  allLoading.value = true
  contents.value = []
  allPage.value = 1
  allFinished.value = false
  allError.value = false
  try {
    await fetchContentPage()
  } catch {
    allError.value = true
  } finally {
    allLoading.value = false
    allRefreshing.value = false
  }
}

const onContentLoad = async (): Promise<void> => {
  if (allFinished.value) {
    allLoading.value = false
    return
  }
  try {
    await fetchContentPage()
  } catch {
    allError.value = true
  } finally {
    allLoading.value = false
  }
}

// ===== 我的收藏：分页 =====
const collections = ref<CollectionItem[]>([])
const colPage = ref(1)
const colLoading = ref(false)
const colFinished = ref(false)
const colError = ref(false)
const colRefreshing = ref(false)
const colLoaded = ref(false)

const fetchCollectionPage = async (): Promise<void> => {
  const res = await listCollections({ page: colPage.value, size: SIZE })
  const list = res.data.records ?? []
  if (colPage.value === 1) {
    collections.value = list
  } else {
    collections.value.push(...list)
  }
  colPage.value += 1
  if (list.length < SIZE) {
    colFinished.value = true
  }
}

const reloadCollections = async (): Promise<void> => {
  colLoading.value = true
  collections.value = []
  colPage.value = 1
  colFinished.value = false
  colError.value = false
  try {
    await fetchCollectionPage()
    colLoaded.value = true
  } catch {
    colError.value = true
  } finally {
    colLoading.value = false
    colRefreshing.value = false
  }
}

const onCollectionLoad = async (): Promise<void> => {
  if (colFinished.value) {
    colLoading.value = false
    return
  }
  try {
    await fetchCollectionPage()
  } catch {
    colError.value = true
  } finally {
    colLoading.value = false
  }
}

// ===== Tab 切换：收藏页懒加载 =====
const onTabChange = (name: string): void => {
  if (name === 'collect' && !colLoaded.value) {
    reloadCollections()
  }
}

// ===== 跳转 =====
const goDetail = (item: ContentItem): void => {
  router.push({ name: 'PatientContentDetail', params: { contentId: item.contentId } })
}

const goCollection = (item: CollectionItem): void => {
  if (!item.available) {
    showToast('该内容已下架或过期，暂无法查看')
    return
  }
  router.push({ name: 'PatientContentDetail', params: { contentId: item.contentId } })
}

onMounted(() => {
  reloadContents()
  getEnabledRehabParts()
    .then((res) => {
      parts.value = res.data ?? []
    })
    .catch(() => {
      // 部位字典加载失败不阻塞列表，仅缺少筛选选项
    })
})
</script>

<template>
  <div class="page-container">
    <van-nav-bar title="健康内容" />

    <van-tabs v-model:active="activeTab" shrink @change="onTabChange">
      <van-tab title="全部内容" name="all" />
      <van-tab title="我的收藏" name="collect" />
    </van-tabs>

    <!-- 全部内容 -->
    <template v-if="activeTab === 'all'">
      <van-search
        v-model="keyword"
        placeholder="搜索标题 / 摘要"
        @search="reloadContents"
        @clear="reloadContents"
      />
      <van-dropdown-menu>
        <van-dropdown-item v-model="partCode" :options="partOptions" @change="reloadContents" />
        <van-dropdown-item v-model="category" :options="categoryOptions" @change="reloadContents" />
      </van-dropdown-menu>

      <van-pull-refresh v-model="allRefreshing" @refresh="reloadContents">
        <van-list
          v-model:loading="allLoading"
          v-model:error="allError"
          :finished="allFinished"
          error-text="加载失败，点击重试"
          finished-text="没有更多了"
          @load="onContentLoad"
        >
          <van-empty v-if="!allLoading && contents.length === 0" description="暂无已发布内容" />

          <div
            v-for="item in contents"
            :key="item.contentId"
            class="content-card"
            @click="goDetail(item)"
          >
            <img
              v-if="item.coverUrl"
              class="cover"
              :src="toMinioProxyUrl(item.coverUrl)"
              alt=""
            />
            <div class="card-main">
              <div class="card-title">{{ item.title }}</div>
              <div v-if="item.summary" class="card-summary">{{ item.summary }}</div>
              <div class="card-meta">
                <van-tag type="primary" plain>{{ item.contentTypeName }}</van-tag>
                <van-tag v-if="item.targetPartName" plain>{{ item.targetPartName }}</van-tag>
                <span v-if="item.publishedAt" class="card-time">
                  {{ formatDateTime(item.publishedAt) }}
                </span>
              </div>
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </template>

    <!-- 我的收藏 -->
    <template v-else>
      <van-pull-refresh v-model="colRefreshing" @refresh="reloadCollections">
        <van-list
          v-model:loading="colLoading"
          v-model:error="colError"
          :finished="colFinished"
          error-text="加载失败，点击重试"
          finished-text="没有更多了"
          @load="onCollectionLoad"
        >
          <van-empty
            v-if="!colLoading && collections.length === 0"
            description="暂无收藏，去「全部内容」看看吧"
          />

          <div
            v-for="item in collections"
            :key="item.collectionId"
            class="content-card"
            :class="{ disabled: !item.available }"
            @click="goCollection(item)"
          >
            <img
              v-if="item.coverUrl && item.available"
              class="cover"
              :src="toMinioProxyUrl(item.coverUrl)"
              alt=""
            />
            <div class="card-main">
              <div class="card-title">{{ item.title }}</div>
              <div v-if="item.summary && item.available" class="card-summary">
                {{ item.summary }}
              </div>
              <div class="card-meta">
                <van-tag type="primary" plain>{{ item.contentTypeName }}</van-tag>
                <van-tag v-if="item.targetPartName" plain>{{ item.targetPartName }}</van-tag>
                <van-tag v-if="!item.available" type="danger" plain>已失效</van-tag>
                <span class="card-time">收藏于 {{ formatDateTime(item.collectedAt) }}</span>
              </div>
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </template>
  </div>
</template>

<style scoped lang="scss">
.content-card {
  display: flex;
  gap: 12px;
  margin: 12px 16px 0;
  padding: 12px;
  background: #fff;
  border-radius: 8px;

  &:active {
    background: #f2f3f5;
  }

  &.disabled {
    opacity: 0.55;
  }
}

.cover {
  width: 96px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 6px;
  object-fit: cover;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #323233;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-summary {
  margin-top: 4px;
  font-size: 12px;
  color: #969799;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.card-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.card-time {
  font-size: 11px;
  color: #c8c9cc;
}
</style>
