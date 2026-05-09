<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  kanbanData, createTask, updateTask, deleteTask,
  transitionTask, assignTask
} from '@/api/production-tasks';

const loading = ref(false);
const errorMessage = ref('');
const columns = reactive([
  { status: 0, label: '待生产', tasks: [] },
  { status: 1, label: '生产中', tasks: [] },
  { status: 2, label: '已完成', tasks: [] }
]);

const modalMode = ref('');
const currentId = ref(null);
const form = reactive({
  taskName: '', orderId: '', layoutResultId: '',
  assigneeId: '', assigneeName: '', estimatedHours: '', actualHours: '', remark: ''
});
const statusForm = reactive({ targetStatus: 0, remark: '' });

const readonly = computed(() => modalMode.value === 'detail');
const modalTitle = computed(() => {
  if (modalMode.value === 'create') return '新建任务';
  if (modalMode.value === 'edit') return '编辑任务';
  if (modalMode.value === 'assign') return '分配工人';
  return '任务详情';
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

function openCreate() {
  modalMode.value = 'create';
  currentId.value = null;
  resetForm();
}

function openEdit(task) {
  modalMode.value = 'edit';
  currentId.value = task.taskId;
  form.taskName = task.taskName || '';
  form.orderId = task.orderId || '';
  form.layoutResultId = task.layoutResultId || '';
  form.assigneeId = task.assigneeId || '';
  form.assigneeName = task.assigneeName || '';
  form.estimatedHours = task.estimatedHours || '';
  form.actualHours = task.actualHours || '';
  form.remark = task.remark || '';
}

function openAssign(task) {
  modalMode.value = 'assign';
  currentId.value = task.taskId;
  form.assigneeId = task.assigneeId || '';
  form.assigneeName = task.assigneeName || '';
}

function resetForm() {
  form.taskName = '';
  form.orderId = '';
  form.layoutResultId = '';
  form.assigneeId = '';
  form.assigneeName = '';
  form.estimatedHours = '';
  form.actualHours = '';
  form.remark = '';
}

async function submitCreate() {
  if (!form.taskName) { errorMessage.value = '任务名称不能为空'; return; }
  errorMessage.value = '';
  try {
    await createTask({
      taskName: form.taskName,
      orderId: form.orderId ? Number(form.orderId) : null,
      layoutResultId: form.layoutResultId ? Number(form.layoutResultId) : null,
      assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
      assigneeName: form.assigneeName,
      estimatedHours: form.estimatedHours ? Number(form.estimatedHours) : null,
      remark: form.remark
    });
    closeModal();
    loadData();
  } catch (e) { errorMessage.value = e?.message || e || '创建失败'; }
}

async function submitEdit() {
  errorMessage.value = '';
  try {
    await updateTask(currentId.value, {
      taskName: form.taskName,
      orderId: form.orderId ? Number(form.orderId) : null,
      estimatedHours: form.estimatedHours ? Number(form.estimatedHours) : null,
      actualHours: form.actualHours ? Number(form.actualHours) : null,
      assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
      assigneeName: form.assigneeName,
      remark: form.remark
    });
    closeModal();
    loadData();
  } catch (e) { errorMessage.value = e?.message || e || '更新失败'; }
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

async function doTransition(taskId, targetStatus) {
  try {
    await transitionTask(taskId, targetStatus, '');
    loadData();
  } catch (e) { errorMessage.value = e?.message || e || '状态变更失败'; }
}

async function doDelete(taskId) {
  if (!confirm('确认删除该任务？')) return;
  try {
    await deleteTask(taskId);
    loadData();
  } catch (e) { errorMessage.value = e?.message || e || '删除失败'; }
}

function closeModal() {
  modalMode.value = '';
  currentId.value = null;
  errorMessage.value = '';
}

/* drag-and-drop */
function onDragStart(e, task) {
  e.dataTransfer.setData('text/plain', JSON.stringify({ taskId: task.taskId, status: task.status }));
  e.dataTransfer.effectAllowed = 'move';
}

function onDragOver(e, status) {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'move';
}

function onDrop(e, targetStatus) {
  e.preventDefault();
  const raw = e.dataTransfer.getData('text/plain');
  if (!raw) return;
  const { taskId, status: fromStatus } = JSON.parse(raw);
  if (fromStatus === targetStatus) return;
  doTransition(taskId, targetStatus);
}

function startTask(task) {
  doTransition(task.taskId, 1);
}

function completeTask(task) {
  statusForm.targetStatus = 2;
  doTransition(task.taskId, 2);
}

onMounted(loadData);
</script>

<template>
  <div>
    <div class="section-title">
      <span>生产看板</span>
      <button class="btn primary small" @click="openCreate">+ 新建任务</button>
    </div>
    <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

    <div class="kanban-board">
      <div
        v-for="col in columns" :key="col.status"
        class="kanban-column"
        @dragover="onDragOver($event, col.status)"
        @drop="onDrop($event, col.status)"
      >
        <div class="kanban-col-header">
          <strong>{{ col.label }}</strong>
          <span class="kanban-count">{{ col.tasks.length }}</span>
        </div>
        <div v-if="col.tasks.length === 0" class="kanban-empty">暂无任务</div>
        <div
          v-for="task in col.tasks" :key="task.taskId"
          class="kanban-card"
          draggable="true"
          @dragstart="onDragStart($event, task)"
          @click="openEdit(task)"
        >
          <div class="kanban-card-title">{{ task.taskName }}</div>
          <div class="kanban-card-meta">
            <span v-if="task.orderNo">订单: {{ task.orderNo }}</span>
            <span v-if="task.assigneeName">{{ task.assigneeName }}</span>
          </div>
          <div class="kanban-card-meta">
            <span v-if="task.estimatedHours">预估 {{ task.estimatedHours }}h</span>
            <span v-if="task.actualHours">实际 {{ task.actualHours }}h</span>
          </div>
          <div class="kanban-card-actions" @click.stop>
            <template v-if="task.status === 0">
              <button class="btn primary small" @click="startTask(task)">开始</button>
            </template>
            <template v-if="task.status === 1">
              <button class="btn primary small" @click="completeTask(task)">完成</button>
            </template>
            <button class="btn ghost small" @click="openAssign(task)">分配</button>
            <button class="btn ghost small" @click="doDelete(task.taskId)" style="color:#dc2626">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="modalMode" class="modal-backdrop" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ modalTitle }}</h2>
        </div>
        <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
        <div v-if="modalMode === 'assign'" class="form-grid">
          <label>工人ID <input v-model.number="form.assigneeId" class="input" /></label>
          <label>工人姓名 <input v-model="form.assigneeName" class="input" /></label>
        </div>
        <div v-else-if="modalMode === 'create' || modalMode === 'edit'" class="form-grid">
          <label>任务名称 <input v-model="form.taskName" class="input" :disabled="readonly" /></label>
          <label>订单ID <input v-model.number="form.orderId" class="input" :disabled="readonly || modalMode==='edit'" /></label>
          <label>排样结果ID <input v-model.number="form.layoutResultId" class="input" :disabled="readonly" /></label>
          <label>预估工时(h) <input v-model.number="form.estimatedHours" class="input" :disabled="readonly" /></label>
          <label>实际工时(h) <input v-model.number="form.actualHours" class="input" :disabled="readonly || modalMode==='create'" /></label>
          <label>备注 <input v-model="form.remark" class="input" :disabled="readonly" /></label>
        </div>
        <div class="modal-actions">
          <button v-if="modalMode === 'create'" class="btn primary" @click="submitCreate">创建</button>
          <button v-if="modalMode === 'edit'" class="btn primary" @click="submitEdit">保存</button>
          <button v-if="modalMode === 'assign'" class="btn primary" @click="submitAssign">确认分配</button>
          <button class="btn secondary" @click="closeModal">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>
