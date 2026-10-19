<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getCaptcha } from '@/api/auth'
import type { Role } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

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

/** 各角色登录成功后的落地页（本工程仅患者端/治疗师端；ADMIN 角色应使用管理端工程） */
const HOME_BY_ROLE: Partial<Record<Role, string>> = {
  PATIENT: '/patient',
  THERAPIST: '/therapist'
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

/** van-form submit 事件自带校验，校验通过才会进入 */
const handleLogin = async (): Promise<void> => {
  loading.value = true
  try {
    const user = await authStore.login({
      username: form.username,
      password: form.password,
      captchaId: captchaId.value,
      captchaCode: form.captchaCode
    })
    showSuccessToast(`欢迎回来，${user.name}`)
    const home = HOME_BY_ROLE[user.role]
    // ADMIN 角色不属于本工程，提示去管理端
    router.replace(home ?? '/403')
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
    <div class="login-header">
      <h2 class="login-title">MoveNet 康复训练平台</h2>
      <p class="login-subtitle">基于姿态评估的康复训练辅助系统</p>
    </div>

    <van-form class="login-form" @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          left-icon="user-o"
          placeholder="请输入用户名"
          clearable
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          left-icon="lock"
          placeholder="请输入密码"
          clearable
          :rules="[{ required: true, message: '请输入密码' }]"
        />
        <van-field
          v-model="form.captchaCode"
          name="captchaCode"
          left-icon="shield-o"
          placeholder="请输入验证码"
          maxlength="6"
          clearable
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <template #button>
            <img
              v-if="captchaImage"
              :src="captchaImage"
              alt="验证码"
              class="captcha-img"
              @click="refreshCaptcha"
            />
            <span v-else class="captcha-placeholder" @click="refreshCaptcha">
              {{ captchaLoading ? '加载中…' : '点击获取' }}
            </span>
          </template>
        </van-field>
      </van-cell-group>

      <div class="login-btn-wrap">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登 录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100%;
  padding: 64px 0 40px;
  background: linear-gradient(135deg, #2563eb 0%, #1e3a8a 100%);

  .login-header {
    text-align: center;
    margin-bottom: 28px;
    padding: 0 24px;

    .login-title {
      margin: 0 0 8px;
      font-size: 22px;
      color: #ffffff;
    }

    .login-subtitle {
      margin: 0;
      font-size: 13px;
      color: rgba(255, 255, 255, 0.75);
    }
  }

  .captcha-img {
    width: 96px;
    height: 32px;
    border-radius: 4px;
    object-fit: cover;
    user-select: none;
    display: block;
  }

  .captcha-placeholder {
    font-size: 13px;
    color: #969799;
  }

  .login-btn-wrap {
    margin: 24px 16px 0;
  }
}
</style>
