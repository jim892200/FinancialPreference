<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import { createLike, updateLike } from '../api/likeListApi.js'
import { useAuthStore } from '../stores/auth.js'

const props = defineProps({
  mode: { type: String, required: true },   // 'create' | 'edit'
  sn:   { type: Number, default: null },
})

const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  productName: '',
  price: null,
  feeRate: null,
  purchaseQuantity: 1,
  account: auth.account,
})

const rules = {
  productName: [
    { required: true, message: '請輸入產品名稱', trigger: 'blur' },
    { max: 100, message: '產品名稱不可超過 100 字', trigger: 'blur' },
  ],
  price: [
    { required: true, message: '請輸入產品價格', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (value === null || value === undefined || value === '') return cb(new Error('請輸入產品價格'))
        if (Number(value) < 0) return cb(new Error('價格不可為負'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  feeRate: [
    { required: true, message: '請輸入手續費率', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (value === null || value === undefined || value === '') return cb(new Error('請輸入手續費率'))
        const num = Number(value)
        if (num < 0 || num > 1) return cb(new Error('費率必須介於 0 ~ 1（例如 0.01 = 1%）'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  purchaseQuantity: [
    { required: true, message: '請輸入購買數量', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (!Number.isInteger(Number(value)) || Number(value) < 1)
          return cb(new Error('購買數量需為正整數'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  account: [
    { required: true, message: '請輸入扣款帳號', trigger: 'blur' },
    { max: 20, message: '帳號不可超過 20 字', trigger: 'blur' },
  ],
}

onMounted(() => {
  // 不論 create / edit，account 一律強制使用登入者的 USER.ACCOUNT（後端會驗證一致性）
  form.account = auth.account

  if (props.mode === 'create') return
  const stateItem = window.history.state?.item
  if (stateItem) {
    form.productName = stateItem.productName
    form.price = Number(stateItem.price)
    form.feeRate = Number(stateItem.feeRate)
    form.purchaseQuantity = Number(stateItem.purchaseQuantity)
  } else {
    ElMessage.warning('找不到原始資料，請從列表進入編輯。')
  }
})

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      productName: form.productName,
      price: Number(form.price),
      feeRate: Number(form.feeRate),
      purchaseQuantity: Number(form.purchaseQuantity),
      account: form.account,
    }
    if (props.mode === 'create') {
      await createLike(payload)
      ElMessage.success('新增成功')
    } else {
      await updateLike(props.sn, payload)
      ElMessage.success('更新成功')
    }
    router.push({ name: 'list' })
  } catch (err) {
    ElMessage.error(err.apiMessage || '送出失敗')
  } finally {
    submitting.value = false
  }
}

function onCancel() {
  router.push({ name: 'list' })
}
</script>

<template>
  <el-card shadow="never" class="form-card">
    <template #header>
      <div class="card-header">
        <el-button :icon="ArrowLeft" link @click="onCancel">返回</el-button>
        <span class="card-title">
          {{ mode === 'create' ? '新增喜好商品' : `編輯喜好商品 #${sn}` }}
        </span>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      label-position="right"
    >
      <el-form-item label="產品名稱" prop="productName">
        <el-input v-model="form.productName" maxlength="100" show-word-limit placeholder="例：美元定存" />
      </el-form-item>

      <el-form-item label="產品價格" prop="price">
        <el-input-number
          v-model="form.price"
          :min="0"
          :step="0.01"
          :precision="2"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="手續費率" prop="feeRate">
        <el-input-number
          v-model="form.feeRate"
          :min="0"
          :max="1"
          :step="0.0001"
          :precision="4"
          controls-position="right"
          style="width: 100%"
        />
        <div class="hint-text">0 ~ 1 之間（例：0.01 = 1%）</div>
      </el-form-item>

      <el-form-item label="購買數量" prop="purchaseQuantity">
        <el-input-number
          v-model="form.purchaseQuantity"
          :min="1"
          :step="1"
          :precision="0"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="扣款帳號" prop="account">
        <el-input v-model="form.account" maxlength="20" readonly />
        <div class="hint-text">登入帳戶的扣款帳號，不可修改</div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :icon="Check" :loading="submitting" @click="onSubmit">
          {{ mode === 'create' ? '送出' : '更新' }}
        </el-button>
        <el-button @click="onCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style scoped>
.form-card {
  max-width: 720px;
  margin: 0 auto;
  border-radius: 8px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2d3d;
}
.hint-text {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 4px;
}
</style>
