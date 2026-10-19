<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, Picture, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getCaptcha } from '@/api/auth'
import type { Role } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

/** 验证码图片与标识（captchaId 由后端生成并在登录时原样回传） */
const captchaImage = ref('')
const captchaId = ref('')
const captchaLoading = ref(false)

/** 登录表单 */
const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/** 各角色登录成功后的落地页（本工程为管理端；患者/治疗师端在 frontend-app 工程） */
const HOME_BY_ROLE: Record<Role, string> = {
  ADMIN: '/admin',
  PATIENT: '/403',
  THERAPIST: '/403'
}

/** 获取图形验证码；每次刷新都会生成新的 captchaId，旧验证码在后端 2 分钟后自动过期 */
const refreshCaptcha = async (): Promise<void> => {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captchaId.value = res.data.captchaId
    captchaImage.value = res.data.image
    form.captchaCode = ''
  } catch {
    // 错误提示已由响应拦截器统一弹出
    captchaImage.value = ''
  } finally {
    captchaLoading.value = false
  }
}

const handleLogin = async (): Promise<void> => {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const user = await authStore.login({
      username: form.username,
      password: form.password,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode
    })
    ElMessage.success(`欢迎回来，${user.name}`)
    router.replace(HOME_BY_ROLE[user.role] ?? '/')
  } catch {
    // 验证码为一次性消费，无论失败原因都需刷新，避免用户用已失效验证码反复提交
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <h2 class="login-title">MoveNet 康复训练平台</h2>
        <p class="login-subtitle">基于姿态评估的康复训练辅助系统</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="请输入验证码"
              :prefix-icon="Picture"
              maxlength="6"
              clearable
              class="captcha-input"
            />
            <el-tooltip v-if="captchaImage" content="点击刷新验证码" placement="top">
              <img
                :src="captchaImage"
                alt="验证码"
                title="点击刷新验证码"
                class="captcha-img"
                @click="refreshCaptcha"
              >
            </el-tooltip>
            <div v-else class="captcha-img captcha-placeholder" @click="refreshCaptcha">
              {{ captchaLoading ? '加载中…' : '点击获取' }}
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb 0%, #1e3a8a 100%);

  .login-card {
    width: 400px;
    max-width: calc(100vw - 32px);
    border-radius: 8px;

    :deep(.el-card__body) {
      padding: 32px 28px 20px;
    }
  }

  .login-header {
    text-align: center;
    margin-bottom: 24px;

    .login-title {
      margin: 0 0 8px;
      font-size: 22px;
      color: #1f2937;
    }

    .login-subtitle {
      margin: 0;
      font-size: 13px;
      color: #6b7280;
    }
  }

  .captcha-row {
    display: flex;
    gap: 10px;
    width: 100%;
    align-items: center;

    .captcha-input {
      flex: 1;
    }

    .captcha-img {
      width: 130px;
      height: 40px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      object-fit: cover;
      user-select: none;
      flex-shrink: 0;
    }

    .captcha-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      color: #909399;
      background: #f5f7fa;
    }
  }

  .login-btn {
    width: 100%;
  }
}
</style>
