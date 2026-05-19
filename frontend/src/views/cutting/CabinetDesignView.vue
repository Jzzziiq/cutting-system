<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus/es/components/message/index.mjs';
import { useThreeScene } from '@/composables/useThreeScene';
import { useCabinetDesignStore } from '@/stores/cabinetDesign';
import { getOrder } from '@/api/orders';
import { listBoards } from '@/api/boards';

const route = useRoute();
const router = useRouter();
const store = useCabinetDesignStore();
const { canvasRef, init, buildCabinet, clearScene, highlight, removeHighlight, onClick, resize, dispose } = useThreeScene();

const orderInfo = ref(null);
const sceneReady = ref(false);
const selectedBoard = ref(null);
const showWizard = ref(false);
const showSplitPreview = ref(false);
const showSlotMap = ref(false);
const slotMap = ref({});
const boardOptions = ref([]);

// Wizard params
const wizard = ref({ width: 1200, height: 2200, depth: 600, shelfCount: 2, doorCount: 2 });

onMounted(async () => {
  const oid = Number(route.query.orderId);
  if (!oid) { ElMessage.warning('缺少订单ID，请从加工数据输入页进入'); return; }
  store.setOrderId(oid);
  try { orderInfo.value = await getOrder(oid); } catch { ElMessage.error('加载订单失败'); return; }
  await store.loadPresets();
  nextTick(() => {
    if (canvasRef.value) { init(canvasRef.value); sceneReady.value = true; }
  });
  window.addEventListener('resize', resize);
  await loadBoardOptions();
});

onBeforeUnmount(() => { window.removeEventListener('resize', resize); dispose(); });

async function loadBoardOptions() {
  try {
    const data = await listBoards({ pageNum: 1, pageSize: 100 });
    boardOptions.value = (data?.records ?? (Array.isArray(data) ? data : []))
      .filter(b => b.isEnabled !== 0)
      .map(b => ({ value: b.boardId, label: `${b.materialType || ''} ${b.color || ''} ${b.length}×${b.width}×${b.thickness}mm`, board: b }));
  } catch { boardOptions.value = []; }
}

function onPresetClick(preset) {
  store.setSelectedPreset(preset);
  wizard.value = { width: 1200, height: 2200, depth: 600, shelfCount: 2, doorCount: 2 };
  showWizard.value = true;
}

function generateCabinetJson(params) {
  const { width, height, depth, shelfCount, doorCount } = params;
  const t = 18;
  const boards = [];

  // Left side
  boards.push({
    id: 'b-001', type: 'side', displayName: '左侧板', materialSlot: 'cabinet_body', boardId: null,
    thickness: t, designLength: height, designWidth: depth, grain: 'vertical',
    position: { x: -width / 2 + t / 2, y: height / 2, z: 0 }, rotation: { x: 0, y: 0, z: 0 },
    placementFace: 'left', connectedTo: [],
    edgeBanding: { left: false, right: false, top: true, bottom: true },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }, hingeHoles: []
  });
  // Right side
  boards.push({
    id: 'b-002', type: 'side', displayName: '右侧板', materialSlot: 'cabinet_body', boardId: null,
    thickness: t, designLength: height, designWidth: depth, grain: 'vertical',
    position: { x: width / 2 - t / 2, y: height / 2, z: 0 }, rotation: { x: 0, y: 0, z: 0 },
    placementFace: 'right', connectedTo: [],
    edgeBanding: { left: false, right: false, top: true, bottom: true },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }, hingeHoles: []
  });
  // Top
  boards.push({
    id: 'b-003', type: 'top', displayName: '顶板', materialSlot: 'cabinet_body', boardId: null,
    thickness: t, designLength: width, designWidth: depth, grain: 'horizontal',
    position: { x: 0, y: height - t / 2, z: 0 }, rotation: { x: 0, y: 0, z: 0 },
    placementFace: 'top', connectedTo: ['b-001', 'b-002'],
    edgeBanding: { left: false, right: false, top: false, bottom: true },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }, hingeHoles: []
  });
  // Bottom
  boards.push({
    id: 'b-004', type: 'bottom', displayName: '底板', materialSlot: 'cabinet_body', boardId: null,
    thickness: t, designLength: width, designWidth: depth, grain: 'horizontal',
    position: { x: 0, y: t / 2, z: 0 }, rotation: { x: 0, y: 0, z: 0 },
    placementFace: 'bottom', connectedTo: ['b-001', 'b-002'],
    edgeBanding: { left: false, right: false, top: true, bottom: false },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }, hingeHoles: []
  });
  // Back
  boards.push({
    id: 'b-005', type: 'back', displayName: '背板', materialSlot: 'back', boardId: null,
    thickness: 5, designLength: width - t * 2, designWidth: height - t * 2, grain: 'vertical',
    position: { x: 0, y: height / 2, z: -depth / 2 + 2.5 }, rotation: { x: 0, y: 0, z: 0 },
    placementFace: 'back', connectedTo: [],
    edgeBanding: { left: false, right: false, top: false, bottom: false }, edgeRole: {}, hingeHoles: []
  });
  // Shelves
  const shelfSpacing = (height - t * 2) / (shelfCount + 1);
  for (let i = 0; i < shelfCount; i++) {
    const yPos = t + shelfSpacing * (i + 1);
    boards.push({
      id: `b-0${6 + i}`, type: 'layer', displayName: `层板${i + 1}`, materialSlot: 'cabinet_body', boardId: null,
      thickness: t, designLength: width - t * 2, designWidth: depth, grain: 'horizontal',
      position: { x: 0, y: yPos, z: 0 }, rotation: { x: 0, y: 0, z: 0 },
      placementFace: 'inner', connectedTo: ['b-001', 'b-002'],
      edgeBanding: { left: false, right: false, top: false, bottom: false }, edgeRole: {}, hingeHoles: []
    });
  }
  // Doors
  const doorWidth = (width - t * 2) / doorCount;
  const doorHeight = height - 4;
  for (let i = 0; i < doorCount; i++) {
    const xPos = -width / 2 + t + doorWidth / 2 + doorWidth * i;
    const opening = i === 0 ? 'left' : 'right';
    const hingeEdge = i === 0 ? 'left' : 'right';
    const connId = i === 0 ? 'b-001' : 'b-002';
    boards.push({
      id: `b-${10 + i}`, type: 'door', displayName: `${i === 0 ? '左' : '右'}门板`, materialSlot: 'door', boardId: null,
      thickness: t, designLength: doorHeight, designWidth: doorWidth, grain: 'vertical',
      position: { x: xPos, y: height / 2, z: depth / 2 + 2 }, rotation: { x: 0, y: 0, z: 0 },
      placementFace: 'front', connectedTo: [connId],
      edgeBanding: { left: true, right: true, top: true, bottom: true },
      hingeHoles: [{ edge: hingeEdge, count: doorHeight > 1500 ? 3 : 2, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening }],
      edgeRole: {}
    });
  }

  return {
    cabinet: { name: '衣柜', room: orderInfo.value?.room || '', purpose: '', width, height, depth },
    boards
  };
}

function onWizardConfirm() {
  const json = generateCabinetJson(wizard.value);
  store.setCabinetJson(json);
  store.setWizardParams(wizard.value);
  showWizard.value = false;
  nextTick(() => { if (sceneReady.value) { buildCabinet(json.boards); } });
}

function onOpenSlotMap() {
  slotMap.value = { cabinet_body: null, door: null, back: null };
  showSlotMap.value = true;
}

function getMaterialSlots(boards) {
  const slots = new Set();
  boards.forEach(b => { if (b.materialSlot) slots.add(b.materialSlot); });
  return Array.from(slots);
}

async function onSplit() {
  const json = store.cabinetJson;
  if (!json) { ElMessage.warning('请先生成柜体模型'); return; }
  try {
    store.splitItems = await store.executeSplit(json, slotMap.value);
    showSplitPreview.value = true;
  } catch (e) {
    ElMessage.error(e?.message || '拆单计算失败');
  }
}

async function onConfirmSplit() {
  try {
    const result = await store.confirmSplit(slotMap.value);
    showSplitPreview.value = false;
    ElMessage.success(`拆单成功！批次号：${result.splitBatchCode}，共生成 ${result.createdItemIds.length} 条订单明细`);
  } catch (e) {
    ElMessage.error(e?.message || '确认拆单失败');
  }
}

function onCanvasClick(event) {
  onClick(event, (data) => {
    selectedBoard.value = data;
    highlight(data.id);
  });
}

const hasCabinet = computed(() => !!store.cabinetJson);

const splitColumns = [
  { prop: 'partCode', label: '工件编号', minWidth: 100 },
  { prop: 'partName', label: '名称', minWidth: 80 },
  { prop: 'boardType', label: '类型', minWidth: 60 },
  { prop: 'materialName', label: '材质', minWidth: 100 },
  { prop: 'length', label: '切割长', minWidth: 70 },
  { prop: 'width', label: '切割宽', minWidth: 70 },
  { prop: 'thickness', label: '厚', minWidth: 50 }
];
</script>

<template>
  <div class="cabinet-design-shell">
    <!-- Header bar -->
    <div class="cd-header">
      <span class="cd-title">3D 柜体设计</span>
      <span v-if="orderInfo" class="cd-order">
        订单 #{{ orderInfo.orderId }} | {{ orderInfo.customerName }} | {{ orderInfo.processName }}
      </span>
      <div class="cd-actions">
        <el-button size="small" :disabled="!hasCabinet" @click="onOpenSlotMap">板材映射</el-button>
        <el-button size="small" type="success" :disabled="!hasCabinet || store.splitting" :loading="store.splitting" @click="onSplit">
          一键拆单
        </el-button>
      </div>
    </div>

    <div class="cd-body">
      <!-- Left: Presets -->
      <div class="cd-left">
        <h4>预设柜体</h4>
        <div class="preset-cards">
          <div v-for="p in store.presets" :key="p.id" class="preset-card" @click="onPresetClick(p)">
            <div class="preset-icon">{{ p.category === 'wardrobe' ? '🚪' : '🗄' }}</div>
            <div class="preset-name">{{ p.name }}</div>
            <div class="preset-cat">{{ p.category === 'wardrobe' ? '衣柜' : '地柜' }}</div>
          </div>
          <div v-if="store.presets.length === 0" class="preset-hint">
            暂无预设模板，请先执行种子数据脚本
          </div>
        </div>
        <el-divider />
        <div v-if="selectedBoard" class="board-props">
          <h4>选中板件</h4>
          <p><b>名称：</b>{{ selectedBoard.displayName || selectedBoard.type }}</p>
          <p><b>类型：</b>{{ selectedBoard.type }}</p>
          <p><b>设计尺寸：</b>{{ selectedBoard.designLength }} × {{ selectedBoard.designWidth }} × {{ selectedBoard.thickness }}mm</p>
        </div>
      </div>

      <!-- Center: Canvas -->
      <div class="cd-center">
        <canvas ref="canvasRef" class="cd-canvas" @click="onCanvasClick"></canvas>
        <div v-if="!hasCabinet" class="cd-canvas-placeholder">
          <p>选择左侧预设柜体以开始设计</p>
        </div>
      </div>
    </div>

    <!-- Wizard dialog -->
    <el-dialog v-model="showWizard" title="柜体参数" width="440px">
      <el-form label-width="90px" size="small">
        <el-form-item label="宽度(mm)">
          <el-input-number v-model="wizard.width" :min="400" :max="3000" :step="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="高度(mm)">
          <el-input-number v-model="wizard.height" :min="400" :max="3000" :step="100" style="width:100%" />
        </el-form-item>
        <el-form-item label="深度(mm)">
          <el-input-number v-model="wizard.depth" :min="300" :max="1200" :step="50" style="width:100%" />
        </el-form-item>
        <el-form-item label="层板数">
          <el-input-number v-model="wizard.shelfCount" :min="0" :max="8" style="width:100%" />
        </el-form-item>
        <el-form-item label="门板数">
          <el-input-number v-model="wizard.doorCount" :min="1" :max="6" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWizard = false">取消</el-button>
        <el-button type="primary" @click="onWizardConfirm">生成模型</el-button>
      </template>
    </el-dialog>

    <!-- Slot Map dialog -->
    <el-dialog v-model="showSlotMap" title="板材映射 (Material Slot → Board)" width="480px">
      <p class="slot-hint">为每个材料角色选择实际板材</p>
      <el-form v-if="hasCabinet" label-width="100px" size="small">
        <el-form-item v-for="slot in getMaterialSlots(store.cabinetJson.boards)" :key="slot" :label="slot">
          <el-select v-model="slotMap[slot]" filterable placeholder="选择板材" style="width:100%">
            <el-option v-for="b in boardOptions" :key="b.value" :label="b.label" :value="b.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSlotMap = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- Split Preview -->
    <el-dialog v-model="showSplitPreview" title="拆单预览" width="800px" top="5vh">
      <el-table :data="store.splitItems" size="small" border stripe max-height="400">
        <el-table-column v-for="col in splitColumns" :key="col.prop" v-bind="col" />
      </el-table>
      <template #footer>
        <el-button @click="showSplitPreview = false">取消</el-button>
        <el-button type="primary" :loading="store.confirming" @click="onConfirmSplit">确认写入订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cabinet-design-shell {
  display: flex; flex-direction: column; height: calc(100vh - 60px); gap: 8px; padding: 8px;
}
.cd-header {
  display: flex; align-items: center; gap: 16px;
  padding: 8px 16px; background: #fff; border-radius: 8px; border: 1px solid #e2e8f0;
}
.cd-title { font-size: 18px; font-weight: 700; color: #0f766e; }
.cd-order { font-size: 13px; color: #64748b; flex: 1; }
.cd-actions { display: flex; gap: 8px; }
.cd-body { display: flex; gap: 12px; flex: 1; min-height: 0; }
.cd-left {
  width: 260px; background: #fff; border-radius: 8px; border: 1px solid #e2e8f0;
  padding: 12px; overflow-y: auto;
}
.cd-left h4 { margin: 0 0 8px; font-size: 14px; color: #172033; }
.preset-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.preset-card {
  border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 8px; text-align: center;
  cursor: pointer; transition: border-color 0.2s;
}
.preset-card:hover { border-color: #0f766e; }
.preset-icon { font-size: 28px; margin-bottom: 4px; }
.preset-name { font-size: 13px; font-weight: 600; color: #172033; }
.preset-cat { font-size: 11px; color: #94a3b8; }
.preset-hint { font-size: 12px; color: #94a3b8; text-align: center; padding: 16px; }
.board-props p { margin: 4px 0; font-size: 12px; color: #475569; }
.cd-center {
  flex: 1; position: relative; background: #fff; border-radius: 8px; border: 1px solid #e2e8f0;
  overflow: hidden; min-width: 0;
}
.cd-canvas { width: 100%; height: 100%; display: block; }
.cd-canvas-placeholder {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  color: #94a3b8; font-size: 16px; pointer-events: none;
}
.slot-hint { font-size: 13px; color: #64748b; margin-bottom: 12px; }
</style>
