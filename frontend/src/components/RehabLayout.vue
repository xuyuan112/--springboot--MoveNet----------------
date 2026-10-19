<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Aim,
  ArrowDown,
  Bell,
  Collection,
  Connection,
  Document,
  Expand,
  Files,
  Fold,
  Monitor,
  Reading,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/api/auth'

interface MenuItem {
  index: string
  title: string
  icon: Component
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

/** 侧边栏折叠状态 */
const collapsed = ref(false)

/** 按角色生成侧边菜单（本工程仅管理端；患者/治疗师端在 frontend-app 工程） */
const menus = computed<MenuItem[]>(() => {
  const role: Role | null = authStore.role
  if (role === 'ADMIN') {
    return [
      { index: '/admin', title: '工作台', icon: Monitor },
      { index: '/admin/users', title: '账号管理', icon: User },
      { index: '/admin/bindings', title: '绑定关系', icon: Connection },
      { index: '/admin/actions', title: '动作库', icon: Aim },
      { index: '/admin/contents', title: '内容管理', icon: Reading },
      { index: '/admin/announcements', title: '公告管理', icon: Bell },
      { index: '/admin/dict', title: '康复部位字典', icon: Collection },
      { index: '/admin/operation-logs', title: '操作日志', icon: Document },
      { index: '/admin/backups', title: '备份恢复', icon: Files }
    ]
  }
  return []
})

const activeMenu = computed(() => route.path)
const userName = computed(() => authStore.userInfo?.name ?? '未登录用户')

/** 头部下拉操作 */
const handleCommand = async (command: string): Promise<void> => {
  if (command === 'password') {
    // TODO: 后续接入 PUT /auth/password 修改密码弹窗
    ElMessage.info('修改密码功能开发中')
    return
  }

  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}
</script>

<template>
  <el-container class="rehab-layout">
    <el-aside :width="collapsed ? '64px' : '210px'" class="layout-aside">
      <div class="logo-bar">
        <span v-show="!collapsed" class="logo-text">MoveNet 康复</span>
        <span v-show="collapsed" class="logo-text-mini">MN</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        router
        background-color="#102a43"
        text-color="#bcccdc"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="layout-body">
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <span class="sys-title">MoveNet 康复训练平台</span>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-trigger">
            <el-icon><UserFilled /></el-icon>
            <span class="user-name">{{ userName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.rehab-layout {
  height: 100%;
}

.layout-aside {
  background-color: #102a43;
  transition: width 0.25s ease;
  overflow-x: hidden;

  .logo-bar {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #0b2236;

    .logo-text {
      color: #ffffff;
      font-size: 18px;
      font-weight: 600;
      letter-spacing: 1px;
      white-space: nowrap;
    }

    .logo-text-mini {
      color: #ffffff;
      font-size: 20px;
      font-weight: 700;
    }
  }

  :deep(.el-menu) {
    border-right: none;
  }
}

.layout-body {
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #475569;
    }

    .sys-title {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .user-trigger {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    outline: none;
    cursor: pointer;
    color: #334155;

    .user-name {
      font-size: 14px;
    }
  }
}

.layout-main {
  background-color: #f5f7fa;
  padding: 16px;
}
</style>
