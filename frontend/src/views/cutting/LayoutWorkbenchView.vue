<script setup>
import { ref, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { onActivated } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import LayoutToolbar from '@/components/cutting/LayoutToolbar.vue';
import LayoutHistoryPanel from '@/components/cutting/LayoutHistoryPanel.vue';
import LayoutCanvas from '@/components/cutting/LayoutCanvas.vue';
import { useAlgorithmSubmit } from '@/composables/useAlgorithmSubmit';
import { getAlgorithmResult } from '@/api/algorithm';
import { getLayoutResult, createLayoutResult, deleteLayoutResult } from '@/api/layout-results';
import { listOrders, getLayoutInput } from '@/api/orders';

const route = useRoute();
const router = useRouter();

const canvasRef = ref(null);

const { submitting, submit } = useAlgorithmSubmit();

const solutions = ref([]);
const loadingCanvas = ref(false);
const activeResultId = ref(null);
const historyRefreshKey = ref(0);
const layoutZoom = ref(1);
const orderInfo = ref({});
const draftData = ref(null);
const boardResults = ref([]);

const showSettings = ref(false);
const settings = reactive({
  kerfWidth: 3,
  gapDistance: 3,
  safeMargin: 5,
  allowRotation: true,
  units: 'mm'
});

async function loadFromOrder() {
  const orderId = Number(route.query.orderId);
  if (!orderId || route.query.source !== 'cabinet') return false;
  loadingCanvas.value = true;
  try {
    const input = await getLayoutInput(orderId);
    if (!input?.groups?.length) {
      ElMessage.warning('该订单没有待排样明细');
      return false;
    }
    const allSolutions = [];
    const localBoardResults = [];
    for (const group of input.groups) {
      const board = group.board;
      const squareList = [];
      for (const item of group.items) {
        for (let i = 0; i < (item.quantity || 1); i++) {
          squareList.push({ id: `${item.orderItemId}-${i + 1}`, l: item.length, w: item.width });
        }
      }
      const payload = {
        L: board.length, W: board.width,
        isRotateEnable: input.algorithmConfig?.allowRotation ?? false,
        gapDistance: input.algorithmConfig?.gapDistance ?? 3,
        squareList
      };
      const res = await submit(payload);
      const solutions = Array.isArray(res) ? res : (res?.resultJson ? (typeof res.resultJson === 'string' ? JSON.parse(res.resultJson) : res.resultJson) : [res]);
      const flatSolutions = solutions.flatMap(s => ({ ...s, _boardGroup: board }));
      allSolutions.push(...flatSolutions);
      localBoardResults.push({ board, solutions: flatSolutions, bestRate: res?.bestRate, containerCount: res?.containerCount });
    }
    settings.gapDistance = input.algorithmConfig?.gapDistance ?? 3;
    settings.allowRotation = input.algorithmConfig?.allowRotation ?? false;
    solutions.value = allSolutions;
    boardResults.value = localBoardResults;
    const totalRate = localBoardResults.length
      ? localBoardResults.reduce((s, r) => s + (r.bestRate || 0), 0) / localBoardResults.length : 0;
    orderInfo.value = {
      orderId,
      orderName: `订单 #${orderId}`,
      utilizationRate: totalRate,
      containerCount: allSolutions.length
    };
    return true;
  } catch (e) {
    ElMessage.error('自动排版失败：' + (e.message || '未知错误'));
    return false;
  } finally {
    loadingCanvas.value = false;
  }
}

async function loadFromTask() {
  const tid = route.query.taskId;
  if (!tid) return;
  loadingCanvas.value = true;
  try {
    const res = await getAlgorithmResult(tid);
    if (res?.resultJson) {
      const data = typeof res.resultJson === 'string' ? JSON.parse(res.resultJson) : res.resultJson;
      solutions.value = data;
      orderInfo.value = {
        orderName: `任务 #${tid}`,
        orderId: res.orderId || null,
        utilizationRate: res.bestRate,
        containerCount: res.containerCount
      };
    } else {
      ElMessage.warning('该任务没有排版结果数据');
    }
    activeResultId.value = null;
  } catch (e) {
    ElMessage.error('加载排版结果失败');
  } finally {
    loadingCanvas.value = false;
  }
}

function loadFromDraft() {
  const draftId = route.query.draftId;
  if (!draftId) return;
  const raw = sessionStorage.getItem(`layout-draft-${draftId}`);
  if (!raw) {
    ElMessage.warning('草稿已过期或不存在');
    return;
  }
  try {
    const draft = JSON.parse(raw);
    draftData.value = draft;
    boardResults.value = draft.boardResults || [];
    solutions.value = draft.mergedSolutions || [];
    const totalRate = boardResults.value.length
      ? boardResults.value.reduce((s, r) => s + (r.bestRate || 0), 0) / boardResults.value.length
      : 0;
    const boardGroupLabels = boardResults.value.map(br =>
      [br.board?.brand, br.board?.materialType, br.board?.color].filter(Boolean).join(' ') || '未知板材'
    );
    orderInfo.value = {
      ...draft.orderInfo,
      orderName: draft.orderInfo?.orderNo || `草稿 ${draftId}`,
      utilizationRate: totalRate,
      containerCount: solutions.value.length,
      boardGroupLabels
    };
    activeResultId.value = null;
  } catch {
    ElMessage.error('草稿数据解析失败');
  }
}

async function onSelectRecord(record) {
  if (!record?.resultId) return;
  activeResultId.value = record.resultId;
  loadingCanvas.value = true;
  try {
    const detail = await getLayoutResult(record.resultId);
    if (detail?.resultJson) {
      const data = typeof detail.resultJson === 'string' ? JSON.parse(detail.resultJson) : detail.resultJson;
      solutions.value = data;
      orderInfo.value = {
        orderId: detail.orderId || record.orderId || null,
        orderNo: detail.orderNo || record.orderNo || '',
        orderName: record.orderName || `排版 #${record.resultId}`,
        customer: record.customer,
        utilizationRate: detail.usageRate,
        containerCount: detail.containerCount
      };
    } else {
      solutions.value = [];
      ElMessage.warning('该排版记录没有结果数据（resultJson 为空）');
    }
  } catch (e) {
    ElMessage.error('加载排版详情失败');
  } finally {
    loadingCanvas.value = false;
  }
}

async function onDeleteRecord(record) {
  if (!record?.resultId) return;

  const recordName = record.orderNo || record.orderName || `排版 #${record.resultId}`;
  try {
    await ElMessageBox.confirm(
      `确认删除排版记录「${recordName}」？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    );
    await deleteLayoutResult(record.resultId);
    ElMessage.success('排版记录已删除');
    historyRefreshKey.value++;

    if (String(activeResultId.value) === String(record.resultId)) {
      activeResultId.value = null;
      solutions.value = [];
      orderInfo.value = {};
      boardResults.value = [];
      draftData.value = null;
    }
  } catch (e) {
    if (!['cancel', 'close'].includes(e) && !['cancel', 'close'].includes(e?.message)) {
      ElMessage.error('删除失败：' + (e.message || '未知错误'));
    }
  }
}

async function onStartLayout() {
  if (!solutions.value.length) {
    try {
      await ElMessageBox.confirm(
        '尚未导入排单信息或加载排版数据。是否使用演示数据进行排版？',
        '提示',
        { confirmButtonText: '使用演示数据', cancelButtonText: '取消', type: 'info' }
      );
    } catch {
      return;
    }
  }

  loadingCanvas.value = true;
  try {
    const squareList = solutions.value.length
      ? solutions.value.flatMap(s => (s.placeSquareList || []).map(p => ({ id: `${p.x}-${p.y}`, l: p.l, w: p.w })))
      : [
          { id: 'demo-1', l: 600, w: 400 },
          { id: 'demo-2', l: 500, w: 350 },
          { id: 'demo-3', l: 450, w: 300 },
          { id: 'demo-4', l: 700, w: 450 },
          { id: 'demo-5', l: 550, w: 400 },
          { id: 'demo-6', l: 400, w: 300 }
        ];

    const res = await submit({
      L: 2440,
      W: 1220,
      isRotateEnable: settings.allowRotation,
      gapDistance: settings.gapDistance,
      squareList
    });

    // submit() now always returns parsed resultJson
    if (res?.resultJson) {
      solutions.value = Array.isArray(res.resultJson) ? res.resultJson : [res.resultJson];
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

    const { value: orderId } = await ElMessageBox.prompt('请输入要导入的订单ID', '导入排单信息', {
      confirmButtonText: '导入',
      cancelButtonText: '取消',
      inputType: 'text',
      inputPlaceholder: orders.map(o => `${o.orderId}=${o.orderNo}`).join(', ')
    });

    if (orderId) {
      const order = orders.find(o => String(o.orderId) === orderId.trim());
      if (order) {
        orderInfo.value = {
          orderName: order.orderNo || `订单 #${order.orderId}`,
          customer: order.customerName,
          orderId: order.orderId
        };
        ElMessage.success('已导入排单信息');
      } else {
        ElMessage.warning('未找到该订单');
      }
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error('导入失败');
  }
}

function onSaveSettings() {
  ElMessage.success('参数已保存');
  showSettings.value = false;
}

async function resolveSaveOrderId() {
  const existingOrderId = Number(orderInfo.value.orderId || 0);
  if (existingOrderId > 0) return existingOrderId;

  const orderNo = orderInfo.value.orderNo || orderInfo.value.orderName;
  if (!orderNo) return null;

  try {
    const data = await listOrders({ pageNum: 1, pageSize: 100, search: orderNo });
    const orders = Array.isArray(data) ? data : (data?.records ?? []);
    const matched = orders.find(o => o.orderNo === orderNo);
    if (matched?.orderId) {
      orderInfo.value = {
        ...orderInfo.value,
        orderId: matched.orderId,
        orderNo: matched.orderNo,
        customer: matched.customerName || orderInfo.value.customer
      };
      return Number(matched.orderId);
    }
  } catch {
    // Keep the save failure actionable below; the request itself may fail due auth/network.
  }

  return null;
}

function onZoomIn() { canvasRef.value?.zoomIn(); }
function onZoomOut() { canvasRef.value?.zoomOut(); }
function onFitScreen() { canvasRef.value?.fitToScreen(); }
function onZoomChange(value) { layoutZoom.value = value; }

function onExportToolpath() {
  const svg = canvasRef.value?.exportSVG();
  if (!svg) {
    ElMessage.warning('暂无排版结果可导出');
    return;
  }
  const blob = new Blob([svg], { type: 'image/svg+xml' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `刀轨_${new Date().toISOString().slice(0, 10)}.svg`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('刀轨已导出');
}

function onExportFile() {
  const data = solutions.value;
  if (!data?.length) {
    ElMessage.warning('暂无排版结果可导出');
    return;
  }
  const json = JSON.stringify({ solutions: data, settings: { ...settings }, orderInfo: orderInfo.value }, null, 2);
  const blob = new Blob([json], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `排版结果_${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('文件已导出');
}

async function onSaveResult() {
  if (!solutions.value?.length) {
    ElMessage.warning('暂无排版结果可保存');
    return;
  }
  try {
    const orderId = await resolveSaveOrderId();
    if (!orderId) {
      ElMessage.warning('保存排版结果前，请先点击“导入排单”选择一个已存在订单');
      return;
    }

    const totalArea = solutions.value.reduce((sum, s) => {
      const cw = s.containerWidth || s.instance?.W || 0;
      const cl = s.containerLength || s.instance?.L || 0;
      return sum + cw * cl;
    }, 0);
    const usedArea = solutions.value.reduce((sum, s) => {
      return sum + (s.placeSquareList || []).reduce((a, p) => a + p.l * p.w, 0);
    }, 0);
    const rate = totalArea > 0 ? usedArea / totalArea : 0;

    await createLayoutResult({
      orderId,
      usageRate: rate,
      totalArea: totalArea,
      containerCount: solutions.value.length,
      resultJson: JSON.stringify(solutions.value)
    });
    ElMessage.success('排版结果已保存');
    historyRefreshKey.value++;
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'));
  }
}

function onBackToEdit() {
  router.push({ name: 'data-input' });
}

// Initial load: orderId(cabinet) > draftId > taskId
if (route.query.orderId && route.query.source === 'cabinet') {
  loadFromOrder();
} else if (route.query.draftId) {
  loadFromDraft();
} else {
  loadFromTask();
}
onActivated(() => {
  if (route.query.orderId && route.query.source === 'cabinet') loadFromOrder();
  else if (route.query.draftId) loadFromDraft();
  else loadFromTask();
});
watch(() => route.query.orderId, () => { if (route.query.orderId && route.query.source === 'cabinet') loadFromOrder(); });
watch(() => route.query.draftId, () => { if (route.query.draftId) loadFromDraft(); });
watch(() => route.query.taskId, () => { if (route.query.taskId && !route.query.draftId && !route.query.orderId) loadFromTask(); });
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
