<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api/authApi.js'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  userId: '',
  password: '',
})

const rules = {
  userId: [{ required: true, message: '請輸入使用者 ID', trigger: 'blur' }],
  password: [{ required: true, message: '請輸入密碼', trigger: 'blur' }],
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = await login(form.userId, form.password)
    auth.setSession({
      token: data.token,
      userId: data.userId,
      userName: data.userName,
      account: data.account,
    })
    ElMessage.success(`歡迎回來，${data.userName}`)
    const redirect = route.query.redirect && typeof route.query.redirect === 'string'
      ? route.query.redirect
      : '/'
    router.replace(redirect)
  } catch (err) {
    ElMessage.error(err.apiMessage || '登入失敗')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-wrapper">
    <el-card shadow="never" class="login-card">
      <template #header>
        <div class="login-title">登入</div>
        <div class="login-subtitle">金融商品喜好紀錄系統</div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item label="使用者 ID" prop="userId">
          <el-input v-model="form.userId" :prefix-icon="User" placeholder="例：A1236456789" maxlength="20" />
        </el-form-item>

        <el-form-item label="密碼" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :prefix-icon="Lock"
            placeholder="密碼"
            show-password
            maxlength="100"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit" style="width: 100%">
            登入
          </el-button>
        </el-form-item>
      </el-form>

      <div class="hint">
        <div>預設測試帳號：<code>A1236456789</code> / <code>B9876543210</code></div>
        <div>密碼：<code>Test@1234</code></div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrapper {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 420px;
  border-radius: 8px;
}
.login-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2d3d;
}
.login-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 2px;
}
.hint {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 8px;
  line-height: 1.6;
}
.hint code {
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 0.75rem;
}
</style>
