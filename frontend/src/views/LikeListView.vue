<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listLikes, deleteLike, SEED_USERS } from '../api/likeListApi.js'

const router = useRouter()
const userId = ref(SEED_USERS[0].id)
const items = ref([])
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    items.value = await listLikes(userId.value)
  } catch (err) {
    ElMessage.error(err.apiMessage || '載入失敗')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function onDelete(item) {
  try {
    await ElMessageBox.confirm(
      `確定要刪除「${item.productName}」嗎？此動作無法復原。`,
      '確認刪除',
      {
        confirmButtonText: '刪除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  try {
    await deleteLike(item.sn)
    ElMessage.success('刪除成功')
    await refresh()
  } catch (err) {
    ElMessage.error(err.apiMessage || '刪除失敗')
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

function formatFeeRate(rate) {
  const num = Number(rate)
  if (!Number.isFinite(num)) return rate
  return `${(num * 100).toFixed(2)}%`
}

function formatMoney(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return value
  return num.toLocaleString('zh-TW', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

watch(userId, refresh)
onMounted(refresh)
</script>

<template>
  <el-card shadow="never" class="list-card">
    <template #header>
      <div class="card-header">
        <div class="left">
          <span class="card-title">喜好商品清單</span>
          <span class="user-info" v-if="items.length > 0">
            共 {{ items.length }} 筆
          </span>
        </div>
        <div class="right">
          <el-select
            v-model="userId"
            placeholder="選擇使用者"
            style="width: 240px"
          >
            <el-option
              v-for="u in SEED_USERS"
              :key="u.id"
              :label="`${u.id}（${u.name}）`"
              :value="u.id"
            />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="onCreate">
            新增喜好
          </el-button>
        </div>
      </div>
    </template>

    <el-table
      v-loading="loading"
      :data="items"
      stripe
      empty-text="尚無喜好商品"
      style="width: 100%"
    >
      <el-table-column prop="sn" label="SN" width="70" />
      <el-table-column prop="productName" label="產品名稱" min-width="160" />
      <el-table-column label="價格" align="right" width="120">
        <template #default="{ row }">
          <span class="num-cell">{{ formatMoney(row.price) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="費率" align="right" width="90">
        <template #default="{ row }">
          <span class="num-cell">{{ formatFeeRate(row.feeRate) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseQuantity" label="數量" align="right" width="80" />
      <el-table-column prop="account" label="扣款帳號" width="140" />
      <el-table-column label="總手續費" align="right" width="120">
        <template #default="{ row }">
          <span class="num-cell">{{ formatMoney(row.totalFee) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="預計扣款" align="right" width="140">
        <template #default="{ row }">
          <span class="amount-strong">{{ formatMoney(row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="Email" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click="onEdit(row)">編輯</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="onDelete(row)">
            刪除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.list-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2d3d;
}
.user-info {
  font-size: 0.875rem;
  color: #6b7280;
}
.right {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
