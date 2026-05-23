<script setup>
import { reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { onActivated } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import LayoutToolbar from '@/components/cutting/LayoutToolbar.vue';
import LayoutHistoryPanel from '@/components/cutting/LayoutHistoryPanel.vue';
import LayoutCanvas from '@/components/cutting/LayoutCanvas.vue';
import { useLayoutRunner } from '@/composables/useLayoutRunner';
import { useLayoutDataLoader } from '@/composables/useLayoutDataLoader';
import { exportToolpathSVG, exportResultJSON } from '@/utils/exportUtils';
import { listOrders } from '@/api/orders';
import { assignOrderTask } from '@/api/production-tasks';
import { listUsers } from '@/api/users';

const route = useRoute();
const router = useRouter();
const canvasRef = ref(null);

const layoutZoom = ref(1);
const showSettings = ref(false);
const settings = reactive({
  kerfWidth: 3,
  gapDistance: 3,
  safeMargin: 5,
  allowRotation: true,
  units: 'mm'
});

// --- Runner ---
const { submitting, runLayoutForGroups, parseResultJson, boardLabel } = useLayoutRunner();

// --- Data loader ---
const {
  solutions,
  loadingCanvas,
  activeResultId,
  historyRefreshKey,
  orderInfo,
  boardResults,
  currentLayoutInput,
  loadFromOrder,
  loadFromRoute,
  onSelectRecord,
  onDeleteRecord,
  onSaveResult,
  resetRouteKey
} = useLayoutDataLoader({ runLayoutForGroups, parseResultJson, boardLabel });

// --- Order import ---
const activeOrderId = ref(0);
const assignDialogVisible = ref(false);
const assignSubmitting = ref(false);
const assignRecord = ref(null);
const assignAssigneeId = ref('');
const userOptions = ref([]);
const userLoading = ref(false);

watch(() => orderInfo.value?.orderId, (id) => {
  if (id) activeOrderId.value = Number(id);
}, { immediate: true });

async function onStartLayout() {
  const orderId = Number(orderInfo.value?.orderId || route.query.orderId || 0);
  if (currentLayoutInput.value?.groups?.length) {
    const ok = await loadFromOrder(orderId, settings);
    if (ok !== false) {
      ElMessage.success('排版计算完成');
    }
    return;
  }
  if (orderId > 0) {
    await loadFromOrder(orderId, settings);
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

async function onImportOrder() {
  try {
    const data = await listOrders({ pageNum: 1, pageSize: 20 });
    const orders = Array.isArray(data) ? data : (data?.records ?? []);
    if (!orders.length) {
      ElMessage.info('暂无可导入的订单');
      return;
    }

    const { value: orderId } = await ElMessageBox.prompt('请输入要排版的订单ID', '选择订单', {
      confirmButtonText: '加载',
      cancelButtonText: '取消',
      inputType: 'text',
      inputPlaceholder: orders.map(o => `${o.orderId}=${o.orderNo}`).join(', ')
    });

    if (orderId) {
      const order = orders.find(o => String(o.orderId) === orderId.trim());
      if (order) {
        await router.push({ name: 'layout-workbench', query: { orderId: order.orderId } });
        ElMessage.success('已加载订单排版输入');
      } else {
        ElMessage.warning('未找到该订单');
      }
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error('导入失败');
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

function onExportToolpath() { exportToolpathSVG(canvasRef); }
function onExportFile() { exportResultJSON(solutions.value, settings, orderInfo.value); }

function onBackToEdit() {
  const orderId = Number(orderInfo.value?.orderId || route.query.orderId || 0);
  router.push({ name: 'data-input', query: orderId > 0 ? { orderId } : {} });
}

// --- Route loading ---
onActivated(() => {
  resetRouteKey();
  loadFromRoute(settings);
});
watch(
  () => [route.query.draftId, route.query.orderId, route.query.taskId],
  () => loadFromRoute(settings),
  { immediate: true }
);
</script>

<template>
  <div class="cutting-shell">
    <div class="cutting-view">
      <LayoutToolbar
        :zoom="layoutZoom"
        :task-running="submitting"
        @import-order="onImportOrder"
        @start-layout="onStartLayout"
        @open-settings="showSettings = true"
        @zoom-in="onZoomIn"
        @zoom-out="onZoomOut"
        @fit-screen="onFitScreen"
        @export-toolpath="onExportToolpath"
        @export-file="onExportFile"
        @save-result="onSaveResult"
        @back-to-edit="onBackToEdit"
      />

      <div class="workbench-layout">
        <LayoutHistoryPanel
          :active-result-id="activeResultId"
          :refresh-key="historyRefreshKey"
          @select-record="onSelectRecord"
          @delete-record="onDeleteRecord"
          @assign-record="onAssignRecord"
        />

        <LayoutCanvas
          ref="canvasRef"
          :solutions="solutions"
          :kerf-width="settings.kerfWidth"
          :gap-distance="settings.gapDistance"
          :allow-rotation="settings.allowRotation"
          :loading="loadingCanvas || submitting"
          :order-info="orderInfo"
          @zoom-change="onZoomChange"
        />
      </div>

      <el-dialog v-model="showSettings" title="刀具参数设置" width="480px">
        <el-form :model="settings" label-width="110px" size="small">
          <el-form-item label="锯路宽度(mm)">
            <el-input-number v-model="settings.kerfWidth" :min="0" :max="20" :step="0.5" style="width:160px" />
          </el-form-item>
          <el-form-item label="刀轨间隙(mm)">
            <el-input-number v-model="settings.gapDistance" :min="0" :max="50" :step="0.5" style="width:160px" />
          </el-form-item>
          <el-form-item label="安全边距(mm)">
            <el-input-number v-model="settings.safeMargin" :min="0" :max="50" :step="1" style="width:160px" />
          </el-form-item>
          <el-form-item label="允许旋转">
            <el-switch v-model="settings.allowRotation" />
          </el-form-item>
          <el-form-item label="默认单位">
            <el-select v-model="settings.units" style="width:120px">
              <el-option label="mm" value="mm" />
              <el-option label="cm" value="cm" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showSettings = false">取消</el-button>
          <el-button type="primary" @click="onSaveSettings">确认</el-button>
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
          <el-button type="primary" :loading="assignSubmitting" @click="submitAssignOrder">确认分配</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
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
