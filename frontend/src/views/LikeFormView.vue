<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createLike, updateLike, SEED_USERS } from '../api/likeListApi.js'

const props = defineProps({
  mode: { type: String, required: true },   // 'create' | 'edit'
  sn:   { type: Number, default: null },
})

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const error = ref('')

const form = reactive({
  userId: SEED_USERS[0].id,
  productName: '',
  price: '',
  feeRate: '',
  purchaseQuantity: 1,
  account: '',
})

onMounted(() => {
  if (props.mode === 'create') {
    if (route.query.userId) form.userId = route.query.userId
    return
  }
  // edit mode：從 router state 帶過來的 item
  const stateItem = window.history.state?.item
  if (stateItem) {
    form.userId = stateItem.userId
    form.productName = stateItem.productName
    form.price = stateItem.price
    form.feeRate = stateItem.feeRate
    form.purchaseQuantity = stateItem.purchaseQuantity
    form.account = stateItem.account
  } else {
    error.value = '找不到原始資料，請從列表進入編輯。'
  }
})

async function onSubmit() {
  submitting.value = true
  error.value = ''
  try {
    if (props.mode === 'create') {
      await createLike({
        userId: form.userId,
        productName: form.productName,
        price: Number(form.price),
        feeRate: Number(form.feeRate),
        purchaseQuantity: Number(form.purchaseQuantity),
        account: form.account,
      })
    } else {
      await updateLike(props.sn, {
        productName: form.productName,
        price: Number(form.price),
        feeRate: Number(form.feeRate),
        purchaseQuantity: Number(form.purchaseQuantity),
        account: form.account,
      })
    }
    router.push({ name: 'list' })
  } catch (err) {
    error.value = err.apiMessage || '送出失敗'
  } finally {
    submitting.value = false
  }
}

function onCancel() { router.push({ name: 'list' }) }
</script>

<template>
  <section class="like-form">
    <h1>{{ mode === 'create' ? '新增喜好商品' : `編輯喜好商品 #${sn}` }}</h1>

    <p v-if="error" class="error">{{ error }}</p>

    <form @submit.prevent="onSubmit">
      <label v-if="mode === 'create'">
        使用者 ID
        <select v-model="form.userId">
          <option v-for="u in SEED_USERS" :key="u.id" :value="u.id">
            {{ u.id }}（{{ u.name }}）
          </option>
        </select>
      </label>

      <label>
        產品名稱
        <input v-model="form.productName" required maxlength="100" />
      </label>

      <label>
        產品價格
        <input v-model="form.price" type="number" step="0.01" min="0" required />
      </label>

      <label>
        手續費率（0–1，例：0.01 = 1%）
        <input v-model="form.feeRate" type="number" step="0.0001" min="0" max="1" required />
      </label>

      <label>
        購買數量
        <input v-model="form.purchaseQuantity" type="number" step="1" min="1" required />
      </label>

      <label>
        扣款帳號
        <input v-model="form.account" required maxlength="20" />
      </label>

      <div class="actions">
        <button type="submit" :disabled="submitting" class="primary">
          {{ submitting ? '送出中…' : '送出' }}
        </button>
        <button type="button" @click="onCancel">取消</button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.like-form { max-width: 600px; margin: 0 auto; padding: 24px; }
h1 { font-size: 1.5rem; margin-bottom: 16px; }
form { display: flex; flex-direction: column; gap: 12px; }
label { display: flex; flex-direction: column; gap: 4px; font-weight: 500; }
input, select { padding: 8px 10px; font: inherit; border: 1px solid #d1d5db; border-radius: 4px; }
.actions { display: flex; gap: 10px; margin-top: 12px; }
button { padding: 8px 16px; cursor: pointer; }
button.primary { background: #0a7; color: white; border: none; border-radius: 4px; }
.error { color: #c00; padding: 8px 12px; background: #fee; border-radius: 4px; margin-bottom: 12px; }
</style>
