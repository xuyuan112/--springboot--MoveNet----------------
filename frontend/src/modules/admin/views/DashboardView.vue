<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const userName = computed(() => authStore.userInfo?.name ?? '管理员')
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <span>管理端工作台</span>
      </template>
      <h2>欢迎使用 MoveNet 康复训练辅助平台管理后台</h2>
      <p>当前登录管理员：{{ userName }}</p>
      <p class="tip">可通过左侧菜单进入「康复部位字典」等管理模块。</p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
h2 {
  margin: 0 0 12px;
  font-size: 20px;
  color: #1f2937;
}

p {
  margin: 6px 0;
  color: #4b5563;
}

.tip {
  color: #6b7280;
  font-size: 13px;
}
</style>
