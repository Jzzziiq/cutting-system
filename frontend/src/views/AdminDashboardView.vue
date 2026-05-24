<script setup>
import { onMounted, ref } from 'vue';
import { getAdminSummary } from '@/api/admin';

const loading = ref(true);
const summary = ref({ orgCount: 0, activeOrgCount: 0, userCount: 0, recentOrgs: [] });

onMounted(async () => {
  try {
    summary.value = await getAdminSummary();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div>
    <div class="dashboard-grid">
      <article class="metric-card">
        <span>组织总数</span>
        <strong>{{ loading ? '...' : summary.orgCount }}</strong>
      </article>
      <article class="metric-card">
        <span>活跃组织</span>
        <strong>{{ loading ? '...' : summary.activeOrgCount }}</strong>
      </article>
      <article class="metric-card">
        <span>用户总数</span>
        <strong>{{ loading ? '...' : summary.userCount }}</strong>
      </article>
    </div>

    <section style="margin-top: 24px;">
      <h3>最近创建的组织</h3>
      <table class="data-table" v-if="summary.recentOrgs.length">
        <thead>
          <tr>
            <th>组织名称</th>
            <th>组织编码</th>
            <th>状态</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="org in summary.recentOrgs" :key="org.orgId">
            <td>{{ org.orgName }}</td>
            <td>{{ org.orgCode }}</td>
            <td>{{ org.status === 1 ? '启用' : '禁用' }}</td>
            <td>{{ org.createTime }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else style="color: #999; margin-top: 12px;">暂无组织数据</p>
    </section>
  </div>
</template>
