<script setup>
import { ref, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { onActivated } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import LayoutToolbar from '@/components/cutting/LayoutToolbar.vue';
import LayoutHistoryPanel from '@/components/cutting/LayoutHistoryPanel.vue';
import LayoutCanvas from '@/components/cutting/LayoutCanvas.vue';
import { useAlgorithmSubmit } from '@/composables/useAlgorithmSubmit';
import { getAlgorithmResult } from '@/api/algorithm';
import { getLayoutResult } from '@/api/layout-results';
import { createLayoutResult } from '@/api/layout-results';
import { listOrders } from '@/api/orders';

const route = useRoute();
const router = useRouter();

const canvasRef = ref(null);

const { submitting, result, submit } = useAlgorithmSubmit();

const solutions = ref([]);
const loadingCanvas = ref(false);
const activeResultId = ref(null);
const orderInfo = ref({});

const showSettings = ref(false);
const settings = reactive({
  kerfWidth: 3,
  gapDistance: 3,
  safeMargin: 5,
  allowRotation: true,
  units: 'mm'
});

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
        utilizationRate: res.bestRate,
        containerCount: res.containerCount
      };
    }
    activeResultId.value = null;
  } catch (e) {
    ElMessage.error('加载排版结果失败');
  } finally {
    loadingCanvas.value = false;
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
        orderName: record.orderName || `排版 #${record.resultId}`,
        customer: record.customer,
        utilizationRate: detail.usageRate,
        containerCount: detail.containerCount
      };
    }
  } catch (e) {
    ElMessage.error('加载排版详情失败');
  } finally {
    loadingCanvas.value = false;
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

    if (res?.resultJson) {
      const data = typeof res.resultJson === 'string' ? JSON.parse(res.resultJson) : res.resultJson;
      solutions.value = data;
    } else if (result.value) {
      solutions.value = result.value;
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

function onZoomIn() { canvasRef.value?.zoomIn(); }
function onZoomOut() { canvasRef.value?.zoomOut(); }
function onFitScreen() { canvasRef.value?.fitToScreen(); }

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
      orderId: orderInfo.value.orderId || null,
      usageRate: rate,
      totalArea: totalArea,
      containerCount: solutions.value.length,
      resultJson: JSON.stringify(solutions.value)
    });
    ElMessage.success('排版结果已保存');
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || '未知错误'));
  }
}

function onBackToEdit() {
  router.push({ name: 'data-input' });
}

// Initial load + reload when keep-alive reactivates or taskId changes
loadFromTask();
onActivated(() => loadFromTask());
watch(() => route.query.taskId, () => { if (route.query.taskId) loadFromTask(); });
</script>

<template>
  <div class="cutting-view">
    <LayoutToolbar
      :zoom="canvasRef?.zoom || 1"
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
        @select-record="onSelectRecord"
      />

      <LayoutCanvas
        ref="canvasRef"
        :solutions="solutions"
        :kerf-width="settings.kerfWidth"
        :gap-distance="settings.gapDistance"
        :allow-rotation="settings.allowRotation"
        :loading="loadingCanvas || submitting"
        :order-info="orderInfo"
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
</template>

<style scoped>
.workbench-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 12px;
  min-height: 0;
}
@media (max-width: 1024px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }
}
</style>
