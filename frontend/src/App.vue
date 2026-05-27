<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <div class="logo-mark">FP</div>
      <div>
        <h1>金融商品喜好紀錄系統</h1>
        <div class="subtitle">Financial Preference Management</div>
      </div>
    </div>
  </header>

  <main class="page-container">
    <RouterView />
  </main>
</template>
