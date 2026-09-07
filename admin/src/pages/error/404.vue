<template>
  <div class="not-found">
    <div class="code">404</div>
    <p>页面不存在或已被移除</p>
    <el-button type="primary" @click="router.replace('/dashboard')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #f0f2f5;

  .code {
    font-size: 72px;
    font-weight: 800;
    color: #c0392b;
    font-family: Georgia, serif;
  }

  p {
    color: #6b7280;
    margin: 0 0 8px;
  }
}
</style>
