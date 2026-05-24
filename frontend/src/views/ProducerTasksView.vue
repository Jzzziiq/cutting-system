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

const activeTask = computed(() => detail.value?.task || tasks.value.find(t => t.taskId === activeTaskId.value) || null);
const orderItems = computed(() => detail.value?.order?.items || []);

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
  } catch {
    tasks.value = [];
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
  } catch {
    detail.value = null;
  } finally {
    detailLoading.value = false;
  }
}

function formatDate(v) {
  return v ? new Date(v).toLocaleString('zh-CN', { hour12: false }) : '-';
}

async function startTask(task) {
  transitioning.value = true;
  errorMessage.value = '';
  try {
    await myTransitionTask(task.taskId, 1, '');
    await loadTasks();
    if (activeTaskId.value) await selectTask({ taskId: activeTaskId.value });
  } catch (e) {
    errorMessage.value = e?.message || '操作失败';
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
    errorMessage.value = e?.message || '操作失败';
  } finally {
    transitioning.value = false;
  }
}

onMounted(loadTasks);
</script>

<template>
  <div class="producer-tasks section-block">
    <div class="section-title">
      <div>
        <h2>我的任务</h2>
        <p>查看分配给你的生产任务</p>
      </div>
      <div class="action-group">
        <button class="btn ghost" type="button" :disabled="loading" @click="loadTasks">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="tasks-layout">
      <aside class="task-list">
        <div v-if="loading" class="empty-state">加载中...</div>
        <div v-else-if="!tasks.length" class="empty-state">暂无分配给你的任务</div>
        <template v-else>
          <button
            v-for="task in tasks"
            :key="task.taskId"
            class="task-item"
            :class="{ active: task.taskId === activeTaskId }"
            type="button"
            @click="selectTask(task)"
          >
            <span class="task-title">{{ task.taskName || `任务 #${task.taskId}` }}</span>
            <span class="task-meta">{{ task.orderNo || '-' }}</span>
            <span class="task-foot">
              <span class="status" :class="{ off: task.status === 0 }">
                {{ task.status === 2 ? '已完成' : task.status === 1 ? '进行中' : '待生产' }}
              </span>
              <span>{{ formatDate(task.createTime) }}</span>
            </span>
          </button>
        </template>
      </aside>

      <section class="detail-panel">
        <div v-if="detailLoading" class="empty-state">加载中...</div>
        <div v-else-if="!activeTask" class="empty-state">请选择一个任务</div>
        <template v-else>
          <div class="detail-head">
            <div>
              <h3>{{ activeTask.taskName || `任务 #${activeTask.taskId}` }}</h3>
              <p>{{ activeTask.orderNo || '-' }}</p>
            </div>
            <div style="display:flex; align-items:center; gap:8px;">
              <span class="status" :class="{ off: activeTask.status === 0 }">
                {{ activeTask.status === 2 ? '已完成' : activeTask.status === 1 ? '进行中' : '待生产' }}
              </span>
              <button
                v-if="activeTask.status === 0"
                class="btn small primary"
                type="button"
                :disabled="transitioning"
                @click="startTask(activeTask)"
              >{{ transitioning ? '处理中...' : '开始生产' }}</button>
              <button
                v-if="activeTask.status === 1"
                class="btn small primary"
                type="button"
                :disabled="transitioning"
                @click="completeTask(activeTask)"
              >{{ transitioning ? '处理中...' : '完成任务' }}</button>
            </div>
          </div>

          <div class="info-grid">
            <div>
              <span>负责人</span>
              <strong>{{ activeTask.assigneeName || '-' }}</strong>
            </div>
            <div>
              <span>预计工时</span>
              <strong>{{ activeTask.estimatedHours ?? '-' }}h</strong>
            </div>
            <div>
              <span>开始时间</span>
              <strong>{{ formatDate(activeTask.startTime) }}</strong>
            </div>
            <div>
              <span>完成时间</span>
              <strong>{{ formatDate(activeTask.completeTime) }}</strong>
            </div>
          </div>

          <h4 style="margin: 16px 0 8px;">板件明细</h4>
          <table class="data-table">
            <thead>
              <tr>
                <th>板件</th>
                <th>尺寸</th>
                <th>厚度</th>
                <th>数量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!orderItems.length">
                <td colspan="4">暂无明细</td>
              </tr>
              <tr v-for="item in orderItems" :key="item.itemId">
                <td>{{ item.partName || item.partCode || '-' }}</td>
                <td>{{ item.length || '-' }} x {{ item.width || '-' }}</td>
                <td>{{ item.thickness || '-' }}</td>
                <td>{{ item.quantity || '-' }}</td>
              </tr>
            </tbody>
          </table>

          <div class="remark-box" style="margin-top: 12px;">
            {{ activeTask.remark || '暂无备注' }}
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.tasks-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  min-height: 500px;
}
.task-list, .detail-panel {
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
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
  background: #fff;
  color: #172033;
  text-align: left;
  cursor: pointer;
}
.task-item.active, .task-item:hover {
  border-color: #0f766e;
  background: #f0fdfa;
}
.task-title { font-weight: 700; }
.task-meta, .task-foot { color: #64748b; font-size: 13px; }
.task-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.detail-panel { padding: 16px; overflow: auto; }
.detail-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.detail-head h3 { margin: 0 0 4px; font-size: 20px; }
.detail-head p { margin: 0; color: #64748b; }
.info-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 16px; }
.info-grid div { display: grid; gap: 4px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc; }
.info-grid span { color: #64748b; font-size: 12px; font-weight: 700; }
.info-grid strong { min-width: 0; overflow-wrap: anywhere; }
.remark-box { min-height: 50px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; background: #f8fafc; color: #334155; }
.empty-state { padding: 28px 12px; color: #94a3b8; text-align: center; }
</style>
