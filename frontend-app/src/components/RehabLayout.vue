<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/api/auth'
import ChangePasswordDialog from './ChangePasswordDialog.vue'

interface TabItem {
  path: string
  title: string
  icon: string
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

/** 各角色底部 TabBar 配置（本工程仅患者端与治疗师端） */
const TABS_BY_ROLE: Partial<Record<Role, TabItem[]>> = {
  PATIENT: [
    { path: '/patient', title: '今日训练', icon: 'play-circle-o' },
    { path: '/patient/plans', title: '康复计划', icon: 'notes-o' },
    { path: '/patient/tasks/calendar', title: '任务日历', icon: 'calendar-o' },
    { path: '/patient/profile', title: '我的', icon: 'user-o' }
  ],
  THERAPIST: [{ path: '/therapist', title: '工作台', icon: 'desktop-o' }]
}

const tabs = computed<TabItem[]>(() => {
  const role: Role | null = authStore.role
  return (role && TABS_BY_ROLE[role]) || []
})

/** 当前激活的 Tab：详情页等子路由高亮其所属 Tab */
const activeTab = computed<string>(() => {
  const path = route.path
  // 按路径前缀最长匹配，保证 /patient/plans/123 高亮「康复计划」
  const matched = tabs.value
    .filter((t) => path === t.path || path.startsWith(t.path + '/'))
    .sort((a, b) => b.path.length - a.path.length)[0]
  return matched?.path ?? path
})

/** 非 Tab 子页面标题映射（患者端/治疗师端详情页等） */
const PAGE_TITLES: Record<string, string> = {
  PatientPlanDetail: '计划详情',
  TherapistPatients: '患者管理',
  TherapistPatientDetail: '患者详情',
  TherapistPlanCreate: '新建计划',
  TherapistPlanEdit: '康复计划',
  TherapistPlanVersions: '计划版本'
}

/** 顶部导航标题：详情页用标题映射，Tab 页用 Tab 标题 */
const navTitle = computed<string>(() => {
  const pageTitle = PAGE_TITLES[String(route.name ?? '')]
  if (pageTitle) {
    return pageTitle
  }
  return tabs.value.find((t) => t.path === activeTab.value)?.title ?? 'MoveNet 康复'
})

/** 非 Tab 首页（如计划详情）显示返回箭头 */
const showBack = computed<boolean>(() => !tabs.value.some((t) => t.path === route.path))

/** 修改密码弹窗引用 */
const passwordDialog = ref<InstanceType<typeof ChangePasswordDialog>>()

/** 右上角更多菜单 */
const moreSheetVisible = ref(false)
const moreActions = [{ name: '修改密码' }, { name: '退出登录', color: '#ee0a24' }]

const onMoreSelect = async (action: { name: string }): Promise<void> => {
  moreSheetVisible.value = false
  if (action.name === '修改密码') {
    passwordDialog.value?.open()
    return
  }
  if (action.name === '退出登录') {
    try {
      await showConfirmDialog({ title: '退出提示', message: '确定要退出登录吗？' })
    } catch {
      return
    }
    await authStore.logout()
    showToast('已退出登录')
    router.replace('/login')
  }
}
</script>

<template>
  <div class="rehab-layout">
    <van-nav-bar
      :title="navTitle"
      :left-arrow="showBack"
      fixed
      placeholder
      @click-left="showBack ? router.back() : undefined"
    >
      <template #right>
        <van-icon name="ellipsis" size="20" @click="moreSheetVisible = true" />
      </template>
    </van-nav-bar>

    <main class="layout-main">
      <router-view />
    </main>

    <van-tabbar :model-value="activeTab" fixed placeholder safe-area-inset-bottom>
      <van-tabbar-item
        v-for="tab in tabs"
        :key="tab.path"
        :name="tab.path"
        :icon="tab.icon"
        @click="router.push(tab.path)"
      >
        {{ tab.title }}
      </van-tabbar-item>
    </van-tabbar>

    <van-action-sheet v-model:show="moreSheetVisible" :actions="moreActions" cancel-text="取消" @select="onMoreSelect" />
    <ChangePasswordDialog ref="passwordDialog" />
  </div>
</template>

<style scoped lang="scss">
.rehab-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.layout-main {
  flex: 1;
  background-color: #f5f7fa;
}
</style>
