<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  kanbanData, createTask, updateTask, deleteTask,
  transitionTask, assignTask
} from '@/api/production-tasks';
import { listUsers } from '@/api/users';
import { listOrders } from '@/api/orders';
import { listLayoutResults } from '@/api/layout-results';

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

const orderOptions = ref([]);
const orderLoading = ref(false);

async function queryOrders(query) {
  orderLoading.value = true;
  try {
    const data = await listOrders({ pageNum: 1, pageSize: 30, search: query || '' });
    const list = Array.isArray(data) ? data : (data?.records ?? []);
    orderOptions.value = list.map(o => ({
      orderId: o.orderId,
      orderNo: o.orderNo || `订单 #${o.orderId}`,
      customerName: o.customerName || ''
    }));
  } catch {
    orderOptions.value = [];
  } finally {
    orderLoading.value = false;
  }
}

const layoutOptions = ref([]);
const layoutLoading = ref(false);

async function queryLayouts(query) {
  layoutLoading.value = true;
  try {
    const data = await listLayoutResults({ pageNum: 1, pageSize: 30, search: query || '' });
    const list = Array.isArray(data) ? data : (data?.records ?? []);
    layoutOptions.value = list.map(r => ({
      resultId: r.resultId,
      label: `${r.orderName || r.orderNo || `排版 #${r.resultId}`}${r.customer ? ' - ' + r.customer : ''}`
    }));
  } catch {
    layoutOptions.value = [];
  } finally {
    layoutLoading.value = false;
  }
}

function onUserSelect(userId) {
  const u = userOptions.value.find(x => x.userId === userId);
  form.assigneeName = u ? u.realName : '';
}

function onOrderSelect(orderId) {
  const o = orderOptions.value.find(x => x.orderId === orderId);
  if (o && !form.taskName) {
    form.taskName = o.orderNo || '';
  }
}

const dialogVisible = computed({
  get: () => !!modalMode.value,
  set: (val) => { if (!val) closeModal(); }
});
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
  queryUsers('');
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
  try {
    await ElMessageBox.confirm('确认删除该任务？删除后不可恢复。', '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteTask(taskId);
    loadData();
    ElMessage.success('任务已删除');
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      errorMessage.value = e?.message || e || '删除失败';
    }
  }
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
  const validTransitions = { 0: [1], 1: [2], 2: [] };
  if (!validTransitions[fromStatus]?.includes(targetStatus)) {
    ElMessage.warning('该状态转换不被允许');
    return;
  }
  doTransition(taskId, targetStatus);
}

function startTask(task) {
  doTransition(task.taskId, 1);
}

function completeTask(task) {
  doTransition(task.taskId, 2);
}

onMounted(loadData);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>生产看板</h2>
        <p>管理生产加工任务，拖拽卡片切换状态</p>
      </div>
      <el-button type="primary" @click="openCreate">+ 新建任务</el-button>
    </div>
    <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>

    <div class="kanban-board">
      <div
        v-for="col in columns" :key="col.status"
        class="kanban-column"
        :class="{
          'kanban-column--pending': col.status === 0,
          'kanban-column--active': col.status === 1,
          'kanban-column--done': col.status === 2
        }"
        @dragover="onDragOver($event, col.status)"
        @drop="onDrop($event, col.status)"
      >
        <div class="kanban-col-header">
          <strong>{{ col.label }}</strong>
          <el-tag
            size="small"
            :type="col.status === 0 ? 'info' : col.status === 1 ? 'warning' : 'success'"
            round
          >
            {{ col.tasks.length }}
          </el-tag>
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
          <div class="kanban-card-body">
            <div v-if="task.orderNo" class="kanban-card-row">
              <span class="kanban-label">订单</span>
              <span>{{ task.orderNo }}</span>
            </div>
            <div v-if="task.assigneeName" class="kanban-card-row">
              <span class="kanban-label">负责人</span>
              <span>{{ task.assigneeName }}</span>
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
            <template v-if="task.status === 0">
              <el-button size="small" type="primary" @click="startTask(task)">开始</el-button>
            </template>
            <template v-if="task.status === 1">
              <el-button size="small" type="success" @click="completeTask(task)">完成</el-button>
            </template>
            <el-button size="small" @click="openAssign(task)">分配</el-button>
            <el-button size="small" type="danger" plain @click="doDelete(task.taskId)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <el-dialog
      v-model="dialogVisible"
      :title="modalTitle"
      width="480px"
      :close-on-click-modal="false"
      @closed="closeModal"
    >
      <div v-if="errorMessage" class="form-error">{{ errorMessage }}</div>
      <el-form v-if="modalMode === 'assign'" label-width="80px" size="small">
        <el-form-item label="工人">
          <el-select
            v-model="form.assigneeId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索工人姓名"
            style="width:100%"
            clearable
            :remote-method="queryUsers"
            :loading="userLoading"
            @focus="queryUsers('')"
            @change="onUserSelect"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.userId"
              :label="u.realName"
              :value="u.userId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <el-form v-else-if="modalMode === 'create' || modalMode === 'edit'" label-width="90px" size="small">
        <el-form-item label="任务名称">
          <el-input v-model="form.taskName" :disabled="readonly" />
        </el-form-item>
        <el-form-item label="订单">
          <el-select
            v-model="form.orderId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索订单号"
            style="width:100%"
            clearable
            :disabled="readonly || modalMode === 'edit'"
            :remote-method="queryOrders"
            :loading="orderLoading"
            @focus="queryOrders('')"
            @change="onOrderSelect"
          >
            <el-option
              v-for="o in orderOptions"
              :key="o.orderId"
              :label="`${o.orderNo}${o.customerName ? ' - ' + o.customerName : ''}`"
              :value="o.orderId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排样结果">
          <el-select
            v-model="form.layoutResultId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索排版记录"
            style="width:100%"
            clearable
            :disabled="readonly"
            :remote-method="queryLayouts"
            :loading="layoutLoading"
            @focus="queryLayouts('')"
          >
            <el-option
              v-for="r in layoutOptions"
              :key="r.resultId"
              :label="r.label"
              :value="r.resultId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预估工时(h)">
          <el-input-number v-model="form.estimatedHours" :min="0" :max="999" :disabled="readonly" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="modalMode === 'edit'" label="实际工时(h)">
          <el-input-number v-model="form.actualHours" :min="0" :max="999" :disabled="readonly" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" :disabled="readonly" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeModal">取消</el-button>
        <el-button v-if="modalMode === 'create'" type="primary" @click="submitCreate">创建</el-button>
        <el-button v-if="modalMode === 'edit'" type="primary" @click="submitEdit">保存</el-button>
        <el-button v-if="modalMode === 'assign'" type="primary" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>
