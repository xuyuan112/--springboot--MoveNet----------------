import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { Role } from '@/api/auth'

declare module 'vue-router' {
  interface RouteMeta {
    /** 公开路由，无需登录即可访问 */
    public?: boolean
    /** 允许访问的角色列表；不配置表示任意已登录角色可访问 */
    roles?: Role[]
  }
}

/** 各角色登录后的默认首页（本工程为管理端；患者/治疗师端在 frontend-app 工程） */
const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: '/admin',
  PATIENT: '/403',
  THERAPIST: '/403'
}

function homeByRole(role?: Role | null): string {
  return role ? HOME_BY_ROLE[role] : '/login'
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/modules/common/login/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Root',
    // 根路由按当前角色重定向到对应端首页
    redirect: () => {
      const authStore = useAuthStore()
      return homeByRole(authStore.role)
    }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/modules/common/error/ForbiddenView.vue'),
    meta: { public: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/modules/common/error/NotFoundView.vue'),
    meta: { public: true }
  },

  // ===== 管理端 =====
  {
    path: '/admin',
    component: () => import('@/components/RehabLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/modules/admin/views/DashboardView.vue')
      },
      {
        path: 'dict',
        name: 'AdminDict',
        component: () => import('@/modules/admin/views/DictView.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/modules/admin/views/user/UserListView.vue')
      },
      {
        path: 'bindings',
        name: 'AdminBindings',
        component: () => import('@/modules/admin/views/binding/BindingListView.vue')
      },
      {
        path: 'actions',
        name: 'AdminActions',
        component: () => import('@/modules/admin/views/action/ActionListView.vue')
      },
      {
        path: 'contents',
        name: 'AdminContents',
        component: () => import('@/modules/admin/views/content/ContentListView.vue')
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/modules/admin/views/announcement/AnnouncementListView.vue')
      },
      {
        path: 'operation-logs',
        name: 'AdminOperationLogs',
        component: () => import('@/modules/admin/views/log/OperationLogView.vue')
      },
      {
        path: 'backups',
        name: 'AdminBackups',
        component: () => import('@/modules/admin/views/backup/BackupListView.vue')
      },
      {
        path: 'actions/:actionId/versions',
        name: 'AdminActionVersions',
        component: () => import('@/modules/admin/views/action/VersionListView.vue')
      },
      {
        path: 'action-versions/:versionId',
        name: 'AdminActionVersionDetail',
        component: () => import('@/modules/admin/views/action/VersionDetailView.vue')
      }
    ]
  },

  // ===== 患者端 / 治疗师端已拆分至 frontend-app 工程，本工程不再承载 =====

  // 兜底：未匹配路由统一进 404
  {
    path: '/:pathMatch(.*)*',
    name: 'CatchAll',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// ===== 全局前置守卫：登录校验 + 角色权限 =====
router.beforeEach((to) => {
  const authStore = useAuthStore()

  // 已登录用户访问登录页：直接送回各自首页
  if (to.path === '/login' && authStore.isLoggedIn) {
    return homeByRole(authStore.role)
  }

  // 未登录访问受保护路由：跳登录页并携带 redirect
  if (!to.meta.public && !authStore.isLoggedIn) {
    return {
      path: '/login',
      query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined
    }
  }

  // 已登录但角色不匹配：403
  const allowedRoles = to.meta.roles
  if (allowedRoles && allowedRoles.length > 0) {
    const currentRole = authStore.role
    if (!currentRole || !allowedRoles.includes(currentRole)) {
      return '/403'
    }
  }

  return true
})

export default router
