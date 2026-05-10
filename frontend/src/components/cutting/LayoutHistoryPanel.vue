<script setup>
import { ref, onMounted } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { listLayoutResults } from '@/api/layout-results';

const props = defineProps({
  activeResultId: [Number, String]
});

const emit = defineEmits(['select-record']);

const records = ref([]);
const loading = ref(false);
const searchQuery = ref('');

const statusLabels = {
  'pending': { text: '未开始', type: 'info' },
  'running': { text: '计算中', type: 'warning' },
  'done': { text: '已完成', type: 'success' },
  'failed': { text: '计算失败', type: 'danger' }
};

async function loadRecords() {
  loading.value = true;
  try {
    const params = { pageNum: 1, pageSize: 50 };
    if (searchQuery.value) {
      params.search = searchQuery.value;
    }
    const data = await listLayoutResults(params);
    records.value = Array.isArray(data) ? data : (data?.records ?? []);
  } catch {
    records.value = [];
  } finally {
    loading.value = false;
  }
}

function onSelect(record) {
  emit('select-record', record);
}

function formatTime(ts) {
  if (!ts) return '-';
  const d = new Date(ts);
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
}

onMounted(loadRecords);
</script>

<template>
  <div class="history-panel panel-container">
    <div class="panel-header">
      <span>历史排单记录</span>
    </div>
    <div class="panel-body">
      <div class="search-row">
        <el-input
          v-model="searchQuery"
          placeholder="搜索客户/订单名"
          size="small"
          clearable
          :suffix-icon="Search"
          @keyup.enter="loadRecords"
          @clear="loadRecords"
        />
      </div>

      <div v-if="loading" class="empty-hint">加载中...</div>
      <div v-else-if="!records.length" class="empty-hint">暂无排版记录</div>

      <div
        v-for="rec in records"
        :key="rec.resultId"
        class="history-item"
        :class="{ active: rec.resultId === activeResultId }"
        @click="onSelect(rec)"
      >
        <div class="hi-head">
          <span class="hi-order">{{ rec.orderName || `排版 #${rec.resultId}` }}</span>
          <el-tag size="small" :type="(statusLabels[rec.status] || statusLabels.pending).type">
            {{ (statusLabels[rec.status] || statusLabels.pending).text }}
          </el-tag>
        </div>
        <div class="hi-body">
          <span v-if="rec.customer">客户：{{ rec.customer }}</span>
          <span>利用率：{{ rec.usageRate != null ? (rec.usageRate * 100).toFixed(1) + '%' : '-' }}</span>
        </div>
        <div class="hi-foot">
          <span>{{ formatTime(rec.createTime) }}</span>
          <span v-if="rec.containerCount != null">{{ rec.containerCount }} 张板材</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-row { margin-bottom: 10px; }
.empty-hint {
  font-size: 13px;
  color: #94a3b8;
  padding: 12px 0;
  text-align: center;
}
.history-item {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.history-item:hover {
  border-color: #0f766e;
  box-shadow: 0 0 0 2px rgba(15, 118, 110, 0.1);
}
.history-item.active {
  border-color: #0f766e;
  background: #f0fdfa;
}
.hi-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.hi-order { font-size: 14px; font-weight: 600; color: #172033; }
.hi-body {
  font-size: 13px;
  color: #64748b;
  display: flex;
  gap: 12px;
}
.hi-foot {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  display: flex;
  gap: 12px;
}
</style>
