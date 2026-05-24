<script setup>
import { computed, onMounted, ref } from 'vue';
import { getMyTaskDetail, listMyTasks, myTransitionTask } from '@/api/production-tasks';

const loading = ref(false);
const detailLoading = ref(false);
const transitioning = ref(false);
const errorMessage = ref('');
const tasks = ref([]);
const activeTaskId = ref(null);
const detail = ref(null);

const activeTask = computed(() => detail.value?.task || tasks.value.find(task => task.taskId === activeTaskId.value) || null);
const orderItems = computed(() => detail.value?.order?.items || []);
const layoutResult = computed(() => detail.value?.layoutResult || null);

async function loadTasks() {
  loading.value = true;
  try {
    const data = await listMyTasks();
    tasks.value = Array.isArray(data) ? data : [];
    if (!activeTaskId.value && tasks.value.length) {
      await selectTask(tasks.value[0]);
    } else if (!tasks.value.length) {
      activeTaskId.value = null;
      detail.value = null;
    }
  } catch (e) {
    tasks.value = [];
    errorMessage.value = e?.message || '加载我的生产订单失败';
  } finally {
    loading.value = false;
  }
}

async function selectTask(task) {
  if (!task?.taskId) return;
  activeTaskId.value = task.taskId;
  detailLoading.value = true;
  try {
    detail.value = await getMyTaskDetail(task.taskId);
  } catch (e) {
    detail.value = null;
    errorMessage.value = e?.message || '加载生产订单详情失败';
  } finally {
    detailLoading.value = false;
  }
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function formatPercent(value) {
  if (value == null) return '-';
  return `${(Number(value) * 100).toFixed(1)}%`;
}

async function startProduction(task) {
  transitioning.value = true;
  errorMessage.value = '';
  try {
    await myTransitionTask(task.taskId, 1, '');
    await loadTasks();
    if (activeTaskId.value) await selectTask({ taskId: activeTaskId.value });
  } catch (e) {
    errorMessage.value = e?.message || '状态变更失败';
  } finally {
    transitioning.value = false;
  }
}

async function completeTask(task) {
  transitioning.value = true;
  errorMessage.value = '';
  try {
    await myTransitionTask(task.taskId, 2, '');
    await loadTasks();
    if (activeTaskId.value) await selectTask({ taskId: activeTaskId.value });
  } catch (e) {
    errorMessage.value = e?.message || '状态变更失败';
  } finally {
    transitioning.value = false;
  }
}

onMounted(loadTasks);
</script>

<template>
  <div class="my-orders section-block">
    <div class="section-title">
      <div>
        <h2>我的生产订单</h2>
        <p>查看已分配给当前账号的生产任务和订单明细</p>
      </div>
      <div class="action-group">
        <button class="btn ghost" type="button" :disabled="loading" @click="loadTasks">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="my-orders-layout">
      <aside class="task-list">
        <div v-if="loading" class="empty-state">加载中...</div>
        <div v-else-if="!tasks.length" class="empty-state">暂无分配给你的生产订单</div>
        <template v-else>
          <button
            v-for="task in tasks"
            :key="task.taskId"
            class="task-item"
            :class="{ active: task.taskId === activeTaskId }"
            type="button"
            @click="selectTask(task)"
          >
            <span class="task-title">{{ task.orderNo || `订单 #${task.orderId}` }}</span>
            <span class="task-meta">{{ task.taskName || '生产任务' }}</span>
            <span class="task-foot">
              <span class="status" :class="{ off: task.status === 0 }">
                {{ task.statusLabel || '待生产' }}
              </span>
              <span>{{ formatDate(task.createTime) }}</span>
            </span>
          </button>
        </template>
      </aside>

      <section class="detail-panel">
        <div v-if="detailLoading" class="empty-state">加载详情中...</div>
        <div v-else-if="!activeTask" class="empty-state">请选择一条生产订单</div>
        <template v-else>
          <div class="detail-head">
            <div>
              <h3>{{ detail?.order?.orderNo || activeTask.orderNo || `订单 #${activeTask.orderId}` }}</h3>
              <p>{{ detail?.order?.customerName || '-' }} · {{ detail?.order?.processName || activeTask.taskName || '-' }}</p>
            </div>
            <div style="display:flex; align-items:center; gap:8px;">
              <span class="status" :class="{ off: activeTask.status === 0 }">
                {{ activeTask.statusLabel || '待生产' }}
              </span>
              <button
                v-if="activeTask.status === 0"
                v-permission="'order:write'"
                class="btn small primary"
                type="button"
                :disabled="transitioning"
                @click="startProduction(activeTask)"
              >{{ transitioning ? '处理中...' : '开始生产' }}</button>
              <button
                v-if="activeTask.status === 1"
                v-permission="'order:write'"
                class="btn small primary"
                type="button"
                :disabled="transitioning"
                @click="completeTask(activeTask)"
              >{{ transitioning ? '处理中...' : '完成任务' }}</button>
            </div>
          </div>

          <div class="info-grid">
            <div>
              <span>客户地址</span>
              <strong>{{ detail?.order?.customerAddress || '-' }}</strong>
            </div>
            <div>
              <span>负责人</span>
              <strong>{{ activeTask.assigneeName || '-' }}</strong>
            </div>
            <div>
              <span>排版利用率</span>
              <strong>{{ formatPercent(layoutResult?.usageRate) }}</strong>
            </div>
            <div>
              <span>板材张数</span>
              <strong>{{ layoutResult?.containerCount ?? '-' }}</strong>
            </div>
          </div>

          <div class="subsection-title">
            <h3>板件明细</h3>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>板件</th>
                  <th>尺寸</th>
                  <th>厚度</th>
                  <th>数量</th>
                  <th>备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!orderItems.length">
                  <td colspan="5">暂无板件明细</td>
                </tr>
                <tr v-for="item in orderItems" :key="item.itemId">
                  <td>{{ item.partName || item.partCode || '-' }}</td>
                  <td>{{ item.length || '-' }} × {{ item.width || '-' }}</td>
                  <td>{{ item.thickness || '-' }}</td>
                  <td>{{ item.quantity || '-' }}</td>
                  <td>{{ item.remark || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="subsection-title">
            <h3>生产要求</h3>
          </div>
          <div class="remark-box">
            {{ activeTask.remark || detail?.order?.remark || '暂无备注' }}
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.my-orders {
  min-height: 0;
}

.my-orders-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 560px;
}

.task-list,
.detail-panel {
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.task-list {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 10px;
  overflow-y: auto;
}

.task-item {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
  color: #172033;
  text-align: left;
  cursor: pointer;
}

.task-item.active,
.task-item:hover {
  border-color: #0f766e;
  background: #f0fdfa;
}

.task-title {
  font-weight: 700;
}

.task-meta,
.task-foot {
  color: #64748b;
  font-size: 13px;
}

.task-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.detail-panel {
  padding: 16px;
  overflow: auto;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-head h3 {
  margin: 0 0 4px;
  font-size: 20px;
}

.detail-head p {
  margin: 0;
  color: #64748b;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.info-grid div,
.remark-box {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
}

.info-grid div {
  display: grid;
  gap: 4px;
  padding: 12px;
}

.info-grid span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.info-grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.remark-box {
  min-height: 76px;
  padding: 12px;
  color: #334155;
}

.empty-state {
  padding: 28px 12px;
  color: #94a3b8;
  text-align: center;
}

@media (max-width: 980px) {
  .my-orders-layout {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
