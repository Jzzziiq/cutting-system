<script setup>
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue';
import { kanbanData, deleteTask, assignTask } from '@/api/production-tasks';
import { listUsers } from '@/api/users';

const loading = ref(false);
const errorMessage = ref('');
const searchQuery = ref('');

function isToday(dateStr) {
  if (!dateStr) return false;
  const d = new Date(dateStr);
  const now = new Date();
  return d.getFullYear() === now.getFullYear()
    && d.getMonth() === now.getMonth()
    && d.getDate() === now.getDate();
}
const columns = reactive([
  { status: 0, label: '待生产', tasks: [] },
  { status: 1, label: '生产中', tasks: [] },
  { status: 2, label: '已完成', tasks: [] }
]);

const filteredColumns = computed(() => {
  let result = columns;
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase();
    result = columns.map(col => ({
      ...col,
      tasks: col.tasks.filter(t =>
        (t.orderNo || '').toLowerCase().includes(q) ||
        (t.taskName || '').toLowerCase().includes(q) ||
        (t.assigneeName || '').toLowerCase().includes(q)
      )
    }));
  }
  return result.map(col =>
    col.status === 2
      ? { ...col, tasks: col.tasks.filter(t => isToday(t.completeTime)) }
      : col
  );
});

const modalMode = ref('');
const currentId = ref(null);
const form = reactive({
  assigneeId: '', assigneeName: ''
});

const userOptions = ref([]);
const userLoading = ref(false);

async function queryUsers(query) {
  userLoading.value = true;
  try {
    const data = await listUsers({ pageNum: 1, pageSize: 30, search: query || '' });
    const list = Array.isArray(data) ? data : (data?.records ?? []);
    userOptions.value = list.map(u => ({
      userId: u.userId,
      realName: u.realName || u.username,
      username: u.username
    }));
  } catch {
    userOptions.value = [];
  } finally {
    userLoading.value = false;
  }
}

function onUserSelect(userId) {
  const u = userOptions.value.find(x => x.userId === userId);
  form.assigneeName = u ? u.realName : '';
}

const modalTitle = computed(() => {
  if (modalMode.value === 'assign') return '分配工人';
  return '';
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const data = await kanbanData();
    columns[0].tasks = data[0] || [];
    columns[1].tasks = data[1] || [];
    columns[2].tasks = data[2] || [];
  } catch (e) {
    errorMessage.value = e?.message || e || '加载失败';
  } finally {
    loading.value = false;
  }
}

function openAssign(task) {
  modalMode.value = 'assign';
  currentId.value = task.taskId;
  form.assigneeId = task.assigneeId || '';
  form.assigneeName = task.assigneeName || '';
  queryUsers('');
}

async function submitAssign() {
  errorMessage.value = '';
  try {
    await assignTask(currentId.value,
      form.assigneeId ? Number(form.assigneeId) : null,
      form.assigneeName);
    closeModal();
    loadData();
  } catch (e) { errorMessage.value = e?.message || e || '分配失败'; }
}

async function doDelete(taskId) {
  if (!window.confirm('确认删除该任务？删除后不可恢复。')) return;
  errorMessage.value = '';
  try {
    await deleteTask(taskId);
    loadData();
  } catch (e) {
    errorMessage.value = e?.message || e || '删除失败';
  }
}

function closeModal() {
  modalMode.value = '';
  currentId.value = null;
  errorMessage.value = '';
}

const isFullscreen = ref(false);

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {});
  } else {
    document.exitFullscreen().catch(() => {});
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement;
  document.documentElement.classList.toggle('kanban-fullscreen', isFullscreen.value);
}

onMounted(() => {
  loadData();
  document.addEventListener('fullscreenchange', onFullscreenChange);
});

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', onFullscreenChange);
});
</script>

<template>
  <div class="section-block section-block--kanban">
    <div class="section-title">
      <div>
        <h2>生产看板</h2>
        <p>查看和管理生产加工任务</p>
      </div>
      <div class="action-group">
        <button class="btn ghost" type="button" @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏显示' }}
        </button>
        <input
          v-model="searchQuery"
          class="input"
          placeholder="搜索订单号/任务名/负责人"
          style="width: 240px; min-height: 32px; padding: 4px 10px; font-size: 13px;"
        />
        <button class="btn ghost" type="button" :disabled="loading" @click="loadData">
          {{ loading ? '刷新中...' : '刷新' }}
        </button>
      </div>
    </div>
    <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

    <div class="kanban-board">
      <div
        v-for="col in filteredColumns" :key="col.status"
        class="kanban-column"
        :class="{
          'kanban-column--pending': col.status === 0,
          'kanban-column--active': col.status === 1,
          'kanban-column--done': col.status === 2
        }"
      >
        <div class="kanban-col-header">
          <strong>{{ col.label }}</strong>
          <span class="status" :class="{ off: col.status === 0 }">
            {{ col.tasks.length }}
          </span>
        </div>
        <div v-if="col.tasks.length === 0" class="kanban-empty">暂无任务</div>
        <div
          v-for="task in col.tasks" :key="task.taskId"
          class="kanban-card"
        >
          <div class="kanban-card-title">{{ task.taskName }}</div>
          <div class="kanban-card-body">
            <div class="kanban-card-row">
              <span v-if="task.orderNo" class="kanban-label">订单</span>
              <span v-if="task.orderNo">{{ task.orderNo }}</span>
              <span v-if="task.assigneeName" class="kanban-label" style="margin-left:8px;">负责人</span>
              <span v-if="task.assigneeName">{{ task.assigneeName }}</span>
            </div>
            <div class="kanban-card-row">
              <span class="kanban-label">工时</span>
              <span>
                <template v-if="task.estimatedHours">预估 {{ task.estimatedHours }}h</template>
                <template v-if="task.estimatedHours && task.actualHours"> / </template>
                <template v-if="task.actualHours">实际 {{ task.actualHours }}h</template>
                <template v-if="!task.estimatedHours && !task.actualHours">-</template>
              </span>
            </div>
          </div>
          <div class="kanban-card-actions" @click.stop>
            <button v-permission="'order:write'" class="btn small ghost" type="button" @click="openAssign(task)">分配</button>
            <button v-permission="'order:write'" class="btn small danger" type="button" @click="doDelete(task.taskId)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Assign Modal -->
    <div v-if="modalMode" class="modal-backdrop">
      <form class="modal" @submit.prevent="submitAssign">
        <div class="modal-header">
          <h3>{{ modalTitle }}</h3>
          <button class="icon-btn" type="button" @click="closeModal">×</button>
        </div>
        <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
        <div class="form-grid">
          <label>
            <span>工人</span>
            <select
              v-model="form.assigneeId"
              class="input"
              @change="onUserSelect(Number(form.assigneeId))"
            >
              <option value="">请选择工人</option>
              <option v-for="u in userOptions" :key="u.userId" :value="u.userId">
                {{ u.realName }}
              </option>
            </select>
          </label>
        </div>
        <div class="modal-actions">
          <button class="btn ghost" type="button" @click="closeModal">取消</button>
          <button v-permission="'order:write'" class="btn primary" type="submit">确认分配</button>
        </div>
      </form>
    </div>
  </div>
</template>
