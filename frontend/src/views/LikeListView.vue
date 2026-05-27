<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, RefreshLeft } from '@element-plus/icons-vue'
import { listLikes, deleteLike } from '../api/likeListApi.js'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const auth = useAuthStore()
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const sortBy = ref('sn')
const sortDir = ref('desc')
const loading = ref(false)

const emptyFilters = () => ({
  productName: '',
  account: '',
  amountMin: null,
  amountMax: null,
  feeRateMin: null,
  feeRateMax: null,
})
const filters = reactive(emptyFilters())

function toQueryParams() {
  return {
    productName: filters.productName,
    account: filters.account,
    amountMin: filters.amountMin,
    amountMax: filters.amountMax,
    // 前端用百分比輸入（例 1.5），送 API 前轉成小數（0.015）
    feeRateMin: filters.feeRateMin == null ? null : Number(filters.feeRateMin) / 100,
    feeRateMax: filters.feeRateMax == null ? null : Number(filters.feeRateMax) / 100,
    sortBy: sortBy.value,
    sortDir: sortDir.value,
    page: page.value,
    pageSize: pageSize.value,
  }
}

async function refresh() {
  loading.value = true
  try {
    const result = await listLikes(toQueryParams())
    items.value = result.items ?? []
    total.value = result.total ?? 0
  } catch (err) {
    ElMessage.error(err.apiMessage || '載入失敗')
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function submitFilters() {
  page.value = 1   // 變更過濾條件後永遠跳回第 1 頁，避免落在不存在的頁碼
  refresh()
}

function resetFilters() {
  Object.assign(filters, emptyFilters())
  page.value = 1
  sortBy.value = 'sn'
  sortDir.value = 'desc'
  refresh()
}

function onSortChange({ prop, order }) {
  // el-table 的 order: 'ascending' | 'descending' | null
  if (!order) {
    sortBy.value = 'sn'
    sortDir.value = 'desc'
  } else {
    sortBy.value = prop
    sortDir.value = order === 'ascending' ? 'asc' : 'desc'
  }
  refresh()
}

function onPageChange(newPage) {
  page.value = newPage
  refresh()
}

function onPageSizeChange(newSize) {
  pageSize.value = newSize
  page.value = 1
  refresh()
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
  router.push({ name: 'create' })
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

onMounted(refresh)
</script>

<template>
  <el-card shadow="never" class="list-card">
    <template #header>
      <div class="card-header">
        <div class="left">
          <span class="card-title">{{ auth.userName }} 的喜好商品清單</span>
          <span class="user-info" v-if="total > 0">
            共 {{ total }} 筆
          </span>
        </div>
        <div class="right">
          <el-button type="primary" :icon="Plus" @click="onCreate">
            新增喜好
          </el-button>
        </div>
      </div>
    </template>

    <el-form :model="filters" inline class="filter-bar" @submit.prevent="submitFilters">
      <el-form-item label="產品名稱">
        <el-input
          v-model="filters.productName"
          placeholder="模糊比對"
          clearable
          style="width: 160px"
          @keyup.enter="submitFilters"
        />
      </el-form-item>
      <el-form-item label="扣款帳號">
        <el-input
          v-model="filters.account"
          placeholder="完全比對"
          clearable
          style="width: 160px"
          @keyup.enter="submitFilters"
        />
      </el-form-item>
      <el-form-item label="預計扣款">
        <el-input-number v-model="filters.amountMin" :min="0" :controls="false" placeholder="下限" style="width: 110px" />
        <span class="range-dash">~</span>
        <el-input-number v-model="filters.amountMax" :min="0" :controls="false" placeholder="上限" style="width: 110px" />
      </el-form-item>
      <el-form-item label="費率(%)">
        <el-input-number v-model="filters.feeRateMin" :min="0" :max="100" :precision="2" :controls="false" placeholder="下限" style="width: 100px" />
        <span class="range-dash">~</span>
        <el-input-number v-model="filters.feeRateMax" :min="0" :max="100" :precision="2" :controls="false" placeholder="上限" style="width: 100px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="submitFilters">查詢</el-button>
        <el-button :icon="RefreshLeft" @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      v-loading="loading"
      :data="items"
      stripe
      empty-text="尚無喜好商品"
      style="width: 100%"
      :default-sort="{ prop: sortBy, order: sortDir === 'asc' ? 'ascending' : 'descending' }"
      @sort-change="onSortChange"
    >
      <el-table-column prop="sn" label="SN" width="80" sortable="custom" />
      <el-table-column prop="productName" label="產品名稱" min-width="160" sortable="custom" />
      <el-table-column prop="price" label="價格" align="right" width="120" sortable="custom">
        <template #default="{ row }">
          <span class="num-cell">{{ formatMoney(row.price) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="feeRate" label="費率" align="right" width="100" sortable="custom">
        <template #default="{ row }">
          <span class="num-cell">{{ formatFeeRate(row.feeRate) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="purchaseQuantity" label="數量" align="right" width="90" sortable="custom" />
      <el-table-column prop="account" label="扣款帳號" width="140" />
      <el-table-column prop="totalFee" label="總手續費" align="right" width="120" sortable="custom">
        <template #default="{ row }">
          <span class="num-cell">{{ formatMoney(row.totalFee) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="預計扣款" align="right" width="140" sortable="custom">
        <template #default="{ row }">
          <span class="amount-strong">{{ formatMoney(row.totalAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="Email" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button size="small" :icon="Edit" @click="onEdit(row)">編輯</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="onDelete(row)">
              刪除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="onPageChange"
      @size-change="onPageSizeChange"
    />
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
.filter-bar {
  padding: 12px 0 4px;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f2f5;
}
.range-dash {
  color: #909399;
  margin: 0 6px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.row-actions {
  display: inline-flex;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}
</style>
