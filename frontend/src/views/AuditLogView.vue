<script setup>
import { onMounted, reactive, ref } from 'vue';
import { listAuditLogs } from '@/api/audit-logs';

const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
const filters = reactive({ module: '', username: '', status: '' });

const moduleOptions = ['', '客户管理', '板材管理', '订单管理', '订单明细', '余料管理', '排样结果', '用户管理'];
const statusOptions = [
  { value: '', label: '全部' },
  { value: '0', label: '成功' },
  { value: '1', label: '失败' }
];

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const params = { pageNum: page.pageNum, pageSize: page.pageSize };
    for (const [k, v] of Object.entries(filters)) {
      if (v !== '') params[k] = v;
    }
    const data = await listAuditLogs(params);
    records.value = data?.records || [];
    total.value = data?.total ?? records.value.length;
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.pageNum = 1;
  loadData();
}

function nextPage() {
  if (page.pageNum * page.pageSize < total.value) {
    page.pageNum += 1;
    loadData();
  }
}

function prevPage() {
  if (page.pageNum > 1) {
    page.pageNum -= 1;
    loadData();
  }
}

function statusLabel(s) {
  return s === 1 ? '失败' : '成功';
}

onMounted(loadData);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>操作审计日志</h2>
        <p>追溯用户的增/删/改操作记录</p>
      </div>
    </div>

    <div class="filter-row">
      <select v-model="filters.module" class="input" @change="search">
        <option v-for="m in moduleOptions" :key="m" :value="m">{{ m || '全部模块' }}</option>
      </select>
      <input v-model="filters.username" class="input" placeholder="操作人" @keyup.enter="search" />
      <select v-model="filters.status" class="input" @change="search">
        <option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
      <button class="btn secondary" type="button" @click="search">查询</button>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>模块</th>
            <th>操作</th>
            <th>操作人</th>
            <th>IP</th>
            <th>耗时(ms)</th>
            <th>状态</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8">加载中...</td>
          </tr>
          <tr v-else-if="!records.length">
            <td colspan="8">暂无审计日志</td>
          </tr>
          <tr v-for="item in records" v-else :key="item.logId">
            <td>{{ item.logId }}</td>
            <td>{{ item.module }}</td>
            <td>{{ item.action }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.ipAddress || '-' }}</td>
            <td>{{ item.durationMs }}</td>
            <td>
              <span class="status" :class="{ off: item.status === 1 }">
                {{ statusLabel(item.status) }}
              </span>
            </td>
            <td>{{ item.createTime || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <span>共 {{ total }} 条，第 {{ page.pageNum }} 页</span>
      <button class="btn ghost" type="button" :disabled="page.pageNum <= 1" @click="prevPage">上一页</button>
      <button class="btn ghost" type="button" :disabled="page.pageNum * page.pageSize >= total" @click="nextPage">下一页</button>
    </div>
  </div>
</template>
