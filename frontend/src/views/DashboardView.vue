<script setup>
import { onMounted, ref } from 'vue';
import { listBoards } from '@/api/boards';
import { listCustomers } from '@/api/customers';

const loading = ref(true);
const metrics = ref({
  customers: '-',
  boards: '-',
  enabledBoards: '-'
});

function totalOf(page) {
  return page?.total ?? page?.records?.length ?? 0;
}

onMounted(async () => {
  loading.value = true;
  try {
    const [customers, boards] = await Promise.all([
      listCustomers({ pageNum: 1, pageSize: 1 }),
      listBoards({ pageNum: 1, pageSize: 50 })
    ]);
    metrics.value.customers = totalOf(customers);
    metrics.value.boards = totalOf(boards);
    metrics.value.enabledBoards = (boards?.records || []).filter((item) => item.isEnabled !== 0).length;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="dashboard-grid">
    <article class="metric-card">
      <span>客户总数</span>
      <strong>{{ loading ? '...' : metrics.customers }}</strong>
    </article>
    <article class="metric-card">
      <span>板材总数</span>
      <strong>{{ loading ? '...' : metrics.boards }}</strong>
    </article>
    <article class="metric-card">
      <span>启用板材</span>
      <strong>{{ loading ? '...' : metrics.enabledBoards }}</strong>
    </article>
  </div>

  <div class="section-block">
    <div class="section-title">
      <h2>常用入口</h2>
    </div>
    <div class="quick-actions">
      <router-link class="btn secondary" to="/customers">客户管理</router-link>
      <router-link class="btn secondary" to="/boards">板材管理</router-link>
      <router-link class="btn primary" to="/algorithm">算法排样</router-link>
    </div>
  </div>
</template>
