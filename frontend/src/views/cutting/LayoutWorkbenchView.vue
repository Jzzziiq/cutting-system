<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { onActivated } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import LayoutToolbar from '@/components/cutting/LayoutToolbar.vue';
import LayoutHistoryPanel from '@/components/cutting/LayoutHistoryPanel.vue';
import LayoutCanvas from '@/components/cutting/LayoutCanvas.vue';
import { useLayoutRunner } from '@/composables/useLayoutRunner';
import { useLayoutDataLoader } from '@/composables/useLayoutDataLoader';
import { exportToolpathNC } from '@/utils/exportUtils';
import { assignOrderTask } from '@/api/production-tasks';
import { listUsers } from '@/api/users';

const route = useRoute();
const router = useRouter();
const canvasRef = ref(null);

const layoutZoom = ref(1);
const showSettings = ref(false);
const settings = reactive({
  kerfWidth: 3,
  safeMargin: 5,
  allowRotation: true,
  cutDepth: 3,
  cutFeed: 3000,
  spindleSpeed: 18000
});

// --- Runner ---
const { submitting, runLayoutForGroups, parseResultJson, decorateSolutions, boardLabel } = useLayoutRunner();

// --- Data loader ---
const {
  solutions,
  loadingCanvas,
  activeResultId,
  historyRefreshKey,
  orderInfo,
  boardResults,
  currentLayoutInput,
  loadOrderMeta,
  loadFromOrder,
  loadFromRoute,
  onSelectRecord,
  onDeleteRecord,
  onSaveResult,
  resetRouteKey
} = useLayoutDataLoader({ runLayoutForGroups, parseResultJson, decorateSolutions, boardLabel });

// --- Order import ---
const activeOrderId = ref(0);
const assignDialogVisible = ref(false);
const assignSubmitting = ref(false);
const assignRecord = ref(null);
const assignAssigneeId = ref('');
const userOptions = ref([]);
const userLoading = ref(false);
const hasLoadedHistoryRecord = ref(false);
const enteredFromSidebar = computed(() => !route.query.orderId && !route.query.draftId && !route.query.taskId);

watch(() => orderInfo.value?.orderId, (id) => {
  if (id) activeOrderId.value = Number(id);
}, { immediate: true });

async function onStartLayout() {
  if (enteredFromSidebar.value && hasLoadedHistoryRecord.value) {
    try {
      await ElMessageBox.confirm(
        '当前查看的是历史排版结果，重新排版将覆盖当前显示。是否继续？',
        '重新排版确认',
        { confirmButtonText: '继续排版', cancelButtonText: '取消', type: 'warning' }
      );
    } catch {
      return;
    }
  }

  const orderId = Number(orderInfo.value?.orderId || route.query.orderId || 0);
  if (currentLayoutInput.value?.groups?.length) {
    const ok = await loadFromOrder(orderId, settings);
    if (ok !== false) {
      await onSaveResult();
      ElMessage.success('排版计算完成');
    }
    return;
  }
  if (orderId > 0) {
    const ok = await loadFromOrder(orderId, settings);
    if (ok !== false) {
      await onSaveResult();
      ElMessage.success('排版计算完成');
    }
    return;
  }

  if (!solutions.value.length) {
    try {
      await ElMessageBox.confirm(
        '尚未选择订单或加载排版数据。是否使用演示数据进行排版？',
        '提示',
        { confirmButtonText: '使用演示数据', cancelButtonText: '取消', type: 'info' }
      );
    } catch {
      return;
    }
  }

  loadingCanvas.value = true;
  try {
    const squareList = [
      { id: 'demo-1', l: 600, w: 400 },
      { id: 'demo-2', l: 500, w: 350 },
      { id: 'demo-3', l: 450, w: 300 },
      { id: 'demo-4', l: 700, w: 450 },
      { id: 'demo-5', l: 550, w: 400 },
      { id: 'demo-6', l: 400, w: 300 }
    ];

    const { submit } = useLayoutRunner();
    const res = await submit({
      L: 2440, W: 1220,
      isRotateEnable: settings.allowRotation,
      gapDistance: settings.gapDistance,
      squareList
    });

    if (res?.resultJson) {
      solutions.value = parseResultJson(res.resultJson);
    }
    orderInfo.value = {
      ...orderInfo.value,
      utilizationRate: res?.bestRate,
      containerCount: res?.containerCount
    };
    ElMessage.success('排版计算完成');
  } catch (e) {
    ElMessage.error(e.message || '排版计算失败');
  } finally {
    loadingCanvas.value = false;
  }
}

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

function onAssignRecord(record) {
  if (!record?.orderId) {
    ElMessage.warning('该排版记录未关联订单，无法分配生产');
    return;
  }
  assignRecord.value = record;
  assignDialogVisible.value = true;
  assignAssigneeId.value = record.assigneeId || '';
  queryUsers('').then(() => {
    if (record.assigneeId && !userOptions.value.some(u => u.userId === record.assigneeId)) {
      userOptions.value.unshift({
        userId: record.assigneeId,
        realName: record.assigneeName || `员工 #${record.assigneeId}`,
        username: ''
      });
    }
  });
}

async function submitAssignOrder() {
  if (!assignRecord.value?.orderId || !assignAssigneeId.value) {
    ElMessage.warning('请选择接收生产任务的员工');
    return;
  }
  assignSubmitting.value = true;
  try {
    await assignOrderTask(assignRecord.value.orderId, Number(assignAssigneeId.value));
    ElMessage.success('生产任务已分配');
    assignDialogVisible.value = false;
    // 重新加载 orderInfo 以同步 taskStatus
    if (orderInfo.value?.orderId) {
      await loadOrderMeta(Number(orderInfo.value.orderId));
    }
    historyRefreshKey.value++;
  } catch (e) {
    ElMessage.error(e?.message || '分配生产失败');
  } finally {
    assignSubmitting.value = false;
  }
}

function onSaveSettings() {
  ElMessage.success('参数已保存');
  showSettings.value = false;
}

function onZoomIn() { canvasRef.value?.zoomIn(); }
function onZoomOut() { canvasRef.value?.zoomOut(); }
function onFitScreen() { canvasRef.value?.fitToScreen(); }
function onZoomChange(value) { layoutZoom.value = value; }

async function onExportToolpath() {
  if (!activeResultId.value) {
    ElMessage.warning('请先保存排版结果后再导出NC文件');
    return;
  }
  try {
    const toolRadius = settings.kerfWidth / 2;
    const cutParams = {
      cutDepth: settings.cutDepth,
      cutFeed: settings.cutFeed,
      spindleSpeed: settings.spindleSpeed
    };
    await exportToolpathNC(activeResultId.value, toolRadius, cutParams);
    ElMessage.success('NC文件已导出');
  } catch (e) {
    ElMessage.error(e?.message || 'NC文件导出失败');
  }
}
function onBackToEdit() {
  const orderId = Number(orderInfo.value?.orderId || route.query.orderId || 0);
  router.push({ name: 'data-input', query: orderId > 0 ? { orderId } : {} });
}

// --- Route loading ---
onActivated(() => {
  resetRouteKey();
  hasLoadedHistoryRecord.value = false;
  const orderId = route.query.orderId;
  const draftId = route.query.draftId;
  const taskId = route.query.taskId;
  if (draftId) {
    loadFromRoute(settings);
  } else if (orderId) {
    loadOrderMeta(Number(orderId));
  } else if (taskId) {
    loadFromRoute(settings);
  } else {
    currentLayoutInput.value = null;
    solutions.value = [];
  }
});

function onSelectHistoryRecord(record) {
  hasLoadedHistoryRecord.value = true;
  onSelectRecord(record);
}
watch(
  () => route.query.orderId,
  (id) => {
    if (id) loadOrderMeta(Number(id));
  }
);
</script>

<template>
  <div class="cutting-shell">
    <div class="cutting-view">
      <LayoutToolbar
        :zoom="layoutZoom"
        :task-running="submitting"
        :disabled="enteredFromSidebar && !hasLoadedHistoryRecord"
        @start-layout="onStartLayout"
        @open-settings="showSettings = true"
        @zoom-in="onZoomIn"
        @zoom-out="onZoomOut"
        @fit-screen="onFitScreen"
        @export-toolpath="onExportToolpath"
        @back-to-edit="onBackToEdit"
      />

      <div v-if="orderInfo?.orderId" class="order-header">
        <div class="order-header-info">
          <span class="order-header-no">{{ orderInfo.orderNo || `订单 #${orderInfo.orderId}` }}</span>
          <span v-if="orderInfo.customer" class="order-header-customer">{{ orderInfo.customer }}</span>
          <span v-if="orderInfo.totalPieces" class="order-header-meta">共 {{ orderInfo.totalPieces }} 件</span>
          <span v-if="orderInfo.boardGroupCount" class="order-header-meta">{{ orderInfo.boardGroupCount }} 种板材</span>
        </div>
        <el-button v-permission="'order:write'" size="small" type="primary" @click="onAssignRecord(orderInfo)">分配生产</el-button>
      </div>

      <div class="workbench-layout">
        <LayoutHistoryPanel
          :active-result-id="activeResultId"
          :refresh-key="historyRefreshKey"
          @select-record="onSelectHistoryRecord"
          @delete-record="onDeleteRecord"
        />

        <LayoutCanvas
          ref="canvasRef"
          :solutions="solutions"
          :kerf-width="settings.kerfWidth"
          :allow-rotation="settings.allowRotation"
          :loading="loadingCanvas || submitting"
          :order-info="orderInfo"
          @zoom-change="onZoomChange"
        />
      </div>

      <el-dialog v-model="showSettings" title="刀具参数设置" width="520px">
        <el-divider content-position="left">排版参数</el-divider>
        <el-form :model="settings" label-width="110px" size="small">
          <el-form-item label="刀路宽度(mm)">
            <el-input-number v-model="settings.kerfWidth" :min="0" :max="20" :step="0.5" style="width:160px" />
          </el-form-item>
          <el-form-item label="安全边距(mm)">
            <el-input-number v-model="settings.safeMargin" :min="0" :max="50" :step="1" style="width:160px" />
          </el-form-item>
          <el-form-item label="允许旋转">
            <el-switch v-model="settings.allowRotation" />
          </el-form-item>
        </el-form>
        <el-divider content-position="left">加工参数</el-divider>
        <el-form :model="settings" label-width="110px" size="small">
          <el-form-item label="切割深度(mm)">
            <el-input-number v-model="settings.cutDepth" :min="0.1" :max="100" :step="0.5" style="width:160px" />
          </el-form-item>
          <el-form-item label="切削进给(mm/min)">
            <el-input-number v-model="settings.cutFeed" :min="100" :max="20000" :step="100" style="width:160px" />
          </el-form-item>
          <el-form-item label="主轴转速(RPM)">
            <el-input-number v-model="settings.spindleSpeed" :min="1000" :max="30000" :step="1000" style="width:160px" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showSettings = false">取消</el-button>
          <el-button v-permission="'order:write'" type="primary" @click="onSaveSettings">确认</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="assignDialogVisible"
        title="分配生产"
        width="420px"
        :close-on-click-modal="false"
      >
        <el-form label-width="76px" size="small">
          <el-form-item label="订单">
            <el-input
              :model-value="assignRecord?.orderNo || assignRecord?.orderName || (assignRecord ? `订单 #${assignRecord.orderId}` : '')"
              readonly
            />
          </el-form-item>
          <el-form-item label="员工">
            <el-select
              v-model="assignAssigneeId"
              filterable
              remote
              reserve-keyword
              placeholder="搜索员工姓名"
              style="width:100%"
              clearable
              :remote-method="queryUsers"
              :loading="userLoading"
              @focus="queryUsers('')"
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
        <template #footer>
          <el-button @click="assignDialogVisible = false">取消</el-button>
          <el-button v-permission="'order:write'" type="primary" :loading="assignSubmitting" @click="submitAssignOrder">确认分配</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f0fdfa;
  border: 1px solid #99f6e4;
  border-radius: 6px;
  margin-bottom: 4px;
}
.order-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}
.order-header-no {
  font-weight: 700;
  color: #0f766e;
}
.order-header-customer {
  color: #64748b;
}
.order-header-meta {
  color: #94a3b8;
}
.workbench-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 12px;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}
@media (max-width: 1024px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }
}
</style>
