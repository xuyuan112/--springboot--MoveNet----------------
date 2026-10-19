<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast, showToast } from 'vant'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const visible = ref(false)
const submitting = ref(false)

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 打开弹窗（由父组件调用） */
function open(): void {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  visible.value = true
}

async function submit(): Promise<void> {
  if (!form.oldPassword) {
    showToast('请输入原密码')
    return
  }
  if (form.newPassword.length < 8 || form.newPassword.length > 64) {
    showToast('新密码长度需在 8~64 位之间')
    return
  }
  if (form.confirmPassword !== form.newPassword) {
    showToast('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    visible.value = false
    showSuccessToast('密码修改成功，请重新登录')
    await authStore.logout()
    router.replace('/login')
  } catch {
    // 错误信息由 request 拦截器统一提示（如原密码错误）
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <van-dialog
    v-model:show="visible"
    title="修改密码"
    show-cancel-button
    :confirm-button-loading="submitting"
    :before-close="(action: string) => action === 'cancel' || !submitting"
    @confirm="submit"
  >
    <van-form class="password-form">
      <van-field
        v-model="form.oldPassword"
        type="password"
        label="原密码"
        placeholder="请输入原密码"
        maxlength="64"
      />
      <van-field
        v-model="form.newPassword"
        type="password"
        label="新密码"
        placeholder="8~64 位新密码"
        maxlength="64"
      />
      <van-field
        v-model="form.confirmPassword"
        type="password"
        label="确认密码"
        placeholder="再次输入新密码"
        maxlength="64"
      />
    </van-form>
  </van-dialog>
</template>

<style scoped lang="scss">
.password-form {
  padding: 8px 0;
}
</style>
