<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <van-empty image="error" description="抱歉，你无权访问该页面">
      <template #bottom>
        <van-button type="primary" round @click="router.push('/')">返回首页</van-button>
      </template>
    </van-empty>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
}
</style>
