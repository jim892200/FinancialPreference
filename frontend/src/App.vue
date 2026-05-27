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
        <el-button class="logout-btn" :icon="SwitchButton" size="small" @click="onLogout">登出</el-button>
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
  color: #ffffff;
  font-weight: 500;
}
.logout-btn {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.4);
  color: #ffffff;
  font-weight: 500;
}
.logout-btn:hover,
.logout-btn:focus {
  background: rgba(255, 255, 255, 0.28);
  border-color: #ffffff;
  color: #ffffff;
}
</style>
