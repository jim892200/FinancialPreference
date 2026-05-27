<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listLikes, deleteLike, SEED_USERS } from '../api/likeListApi.js'

const router = useRouter()
const userId = ref(SEED_USERS[0].id)
const items = ref([])
const loading = ref(false)
const error = ref('')

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    items.value = await listLikes(userId.value)
  } catch (err) {
    error.value = err.apiMessage || '載入失敗'
    items.value = []
  } finally {
    loading.value = false
  }
}

async function onDelete(item) {
  if (!confirm(`確定刪除「${item.productName}」？`)) return
  try {
    await deleteLike(item.sn)
    await refresh()
  } catch (err) {
    error.value = err.apiMessage || '刪除失敗'
  }
}

function onCreate() {
  router.push({ name: 'create', query: { userId: userId.value } })
}

function onEdit(item) {
  router.push({
    name: 'edit',
    params: { sn: item.sn },
    state: { item: JSON.parse(JSON.stringify(item)) },
  })
}

watch(userId, refresh)
onMounted(refresh)
</script>

<template>
  <section class="like-list">
    <header>
      <h1>金融商品喜好清單</h1>
      <div class="controls">
        <label>
          使用者
          <select v-model="userId">
            <option v-for="u in SEED_USERS" :key="u.id" :value="u.id">
              {{ u.id }}（{{ u.name }}）
            </option>
          </select>
        </label>
        <button class="primary" @click="onCreate">＋ 新增喜好</button>
      </div>
    </header>

    <p v-if="loading" class="hint">載入中…</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <p v-else-if="items.length === 0" class="hint">尚無喜好商品。</p>

    <table v-else>
      <thead>
        <tr>
          <th>SN</th>
          <th>產品名稱</th>
          <th class="num">價格</th>
          <th class="num">費率</th>
          <th class="num">數量</th>
          <th>扣款帳號</th>
          <th class="num">總手續費</th>
          <th class="num">預計扣款</th>
          <th>Email</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="it in items" :key="it.sn">
          <td>{{ it.sn }}</td>
          <td>{{ it.productName }}</td>
          <td class="num">{{ it.price }}</td>
          <td class="num">{{ it.feeRate }}</td>
          <td class="num">{{ it.purchaseQuantity }}</td>
          <td>{{ it.account }}</td>
          <td class="num">{{ it.totalFee }}</td>
          <td class="num strong">{{ it.totalAmount }}</td>
          <td>{{ it.email }}</td>
          <td class="actions">
            <button @click="onEdit(it)">編輯</button>
            <button class="danger" @click="onDelete(it)">刪除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.like-list { max-width: 1200px; margin: 0 auto; padding: 24px; }
header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 16px; flex-wrap: wrap; }
h1 { margin: 0; font-size: 1.6rem; }
.controls { display: flex; gap: 12px; align-items: center; }
select { padding: 6px 8px; }
button { padding: 6px 12px; cursor: pointer; }
button.primary { background: #0a7; color: white; border: none; border-radius: 4px; }
button.danger { background: #e74; color: white; border: none; border-radius: 4px; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 8px 10px; border-bottom: 1px solid #e5e7eb; text-align: left; }
.num { text-align: right; font-variant-numeric: tabular-nums; }
.strong { font-weight: 600; }
.actions { display: flex; gap: 6px; }
.hint { color: #6b7280; padding: 24px 0; }
.error { color: #c00; padding: 8px 12px; background: #fee; border-radius: 4px; }
</style>
