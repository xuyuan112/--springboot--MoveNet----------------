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

/** 各角色登录后的默认首页（本工程仅承载患者端与治疗师端；ADMIN 角色请访问管理端工程） */
const HOME_BY_ROLE: Partial<Record<Role, string>> = {
  PATIENT: '/patient',
  THERAPIST: '/therapist'
}

function homeByRole(role?: Role | null): string {
  return (role && HOME_BY_ROLE[role]) || '/login'
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

  // ===== 患者端 =====
  {
    path: '/patient',
    component: () => import('@/components/RehabLayout.vue'),
    meta: { roles: ['PATIENT'] },
    children: [
      {
        path: '',
        name: 'PatientHome',
        component: () => import('@/modules/patient/views/task/TodayTasksView.vue')
      },
      {
        path: 'plans',
        name: 'PatientPlans',
        component: () => import('@/modules/patient/views/plan/PlanListView.vue')
      },
      {
        path: 'plans/:planId',
        name: 'PatientPlanDetail',
        component: () => import('@/modules/patient/views/plan/PlanDetailView.vue')
      },
      {
        path: 'tasks/calendar',
        name: 'PatientTaskCalendar',
        component: () => import('@/modules/patient/views/task/TaskCalendarView.vue')
      },
      {
        path: 'records',
        name: 'PatientRecords',
        component: () => import('@/modules/patient/views/training/RecordListView.vue')
      },
      {
        path: 'records/:recordId',
        name: 'PatientRecordDetail',
        component: () => import('@/modules/patient/views/training/RecordDetailView.vue')
      },
      {
        path: 'actions',
        name: 'PatientActions',
        component: () => import('@/modules/patient/views/training/ActionListView.vue')
      },
      {
        path: 'progress',
        name: 'PatientProgress',
        component: () => import('@/modules/patient/views/progress/ProgressView.vue')
      },
      {
        path: 'reports',
        name: 'PatientReports',
        component: () => import('@/modules/patient/views/report/ReportListView.vue')
      },
      {
        path: 'reports/:reportId',
        name: 'PatientReportDetail',
        component: () => import('@/modules/patient/views/report/ReportDetailView.vue')
      },
      {
        path: 'contents',
        name: 'PatientContents',
        component: () => import('@/modules/patient/views/content/ContentListView.vue')
      },
      {
        path: 'contents/:contentId',
        name: 'PatientContentDetail',
        component: () => import('@/modules/patient/views/content/ContentDetailView.vue')
      },
      {
        path: 'ai',
        name: 'PatientAiQa',
        component: () => import('@/modules/patient/views/ai/AiQaView.vue')
      },
      {
        path: 'profile',
        name: 'PatientProfile',
        component: () => import('@/modules/patient/views/profile/ProfileView.vue')
      }
    ]
  },

  // 训练执行页：全屏摄像头页，不挂患者底部导航布局
  {
    path: '/patient/training/:taskId',
    name: 'PatientTraining',
    component: () => import('@/modules/patient/views/training/TrainingSessionView.vue'),
    meta: { roles: ['PATIENT'] }
  },

  // ===== 治疗师端 =====
  {
    path: '/therapist',
    component: () => import('@/components/RehabLayout.vue'),
    meta: { roles: ['THERAPIST'] },
    children: [
      {
        path: '',
        name: 'TherapistDashboard',
        component: () => import('@/modules/therapist/views/DashboardView.vue')
      },
      {
        path: 'patients',
        name: 'TherapistPatients',
        component: () => import('@/modules/therapist/views/patient/PatientListView.vue')
      },
      {
        path: 'patients/:patientId',
        name: 'TherapistPatientDetail',
        component: () => import('@/modules/therapist/views/patient/PatientDetailView.vue')
      },
      {
        // 创建模式：planId 参数为 'new'
        path: 'patients/:patientId/plans/new',
        name: 'TherapistPlanCreate',
        component: () => import('@/modules/therapist/views/plan/PlanEditView.vue')
      },
      {
        path: 'plans/:planId',
        name: 'TherapistPlanEdit',
        component: () => import('@/modules/therapist/views/plan/PlanEditView.vue')
      },
      {
        path: 'plans/:planId/versions',
        name: 'TherapistPlanVersions',
        component: () => import('@/modules/therapist/views/plan/PlanVersionsView.vue')
      }
    ]
  },

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

  // 已登录用户访问登录页：直接送回各自首页（ADMIN 无用户端首页，去 403）
  if (to.path === '/login' && authStore.isLoggedIn) {
    const home = homeByRole(authStore.role)
    return home === '/login' ? '/403' : home
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
