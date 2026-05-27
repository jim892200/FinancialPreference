<script setup>
import { RouterView, useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth.js'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

function onLogout() {
  auth.clear()
  ElMessage.success('已登出')
  if (route.name !== 'login') {
    router.replace({ name: 'login' })
  }
}
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <div class="brand">
        <div class="logo-mark">FP</div>
        <div>
          <h1>金融商品喜好紀錄系統</h1>
          <div class="subtitle">Financial Preference Management</div>
        </div>
      </div>

      <div class="user-area" v-if="auth.isAuthenticated">
        <span class="user-info">您好，{{ auth.userName }}（{{ auth.userId }}）</span>
        <el-button :icon="SwitchButton" size="small" link @click="onLogout">登出</el-button>
      </div>
    </div>
  </header>

  <main class="page-container">
    <RouterView />
  </main>
</template>

<style scoped>
.app-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}
.user-info {
  font-size: 0.875rem;
  color: #6b7280;
}
</style>
