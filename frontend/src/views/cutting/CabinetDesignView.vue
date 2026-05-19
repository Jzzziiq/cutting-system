<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { useThreeScene } from '@/composables/useThreeScene';
import { useCabinetDesignStore } from '@/stores/cabinetDesign';
import { getOrder } from '@/api/orders';
import { listBoards } from '@/api/boards';

const route = useRoute();
const router = useRouter();
const store = useCabinetDesignStore();
const { canvasRef, init, buildCabinet, highlight, onClick, resize, dispose } = useThreeScene();

const orderInfo = ref(null);
const sceneReady = ref(false);
const selectedBoard = ref(null);
const showWizard = ref(false);
const showSplitPreview = ref(false);
const showSlotMap = ref(false);
const slotMap = ref({});
const boardOptions = ref([]);
const loading = ref(false);

const defaultWizardByCategory = {
  wardrobe: { width: 1200, height: 2200, depth: 600, shelfCount: 2, doorCount: 2 },
  'base-cabinet': { width: 800, height: 800, depth: 500, shelfCount: 1, doorCount: 2 }
};

const categoryLabels = {
  wardrobe: '衣柜',
  'base-cabinet': '地柜'
};

const materialSlotLabels = {
  cabinet_body: '柜体板',
  door: '门板',
  back: '背板'
};

const wizard = ref({ ...defaultWizardByCategory.wardrobe });

const hasCabinet = computed(() => !!store.cabinetJson);
const materialSlots = computed(() => getMaterialSlots(store.cabinetJson?.boards ?? []));
const allSlotsMapped = computed(() =>
  materialSlots.value.length > 0 && materialSlots.value.every(slot => Boolean(slotMap.value[slot]))
);

onMounted(async () => {
  const oid = Number(route.query.orderId);
  if (!Number.isFinite(oid) || oid <= 0) {
    ElMessage.warning('缺少订单ID，请从加工数据输入页进入');
    router.replace({ name: 'data-input' });
    return;
  }

  loading.value = true;
  store.setOrderId(oid);
  selectedBoard.value = null;
  slotMap.value = {};

  try {
    orderInfo.value = await getOrder(oid);
    await Promise.all([store.loadPresets(), loadBoardOptions()]);
  } catch (e) {
    ElMessage.error(e?.message || '加载3D建模页面失败');
  } finally {
    loading.value = false;
  }

  await nextTick();
  if (canvasRef.value) {
    init(canvasRef.value);
    sceneReady.value = true;
    resize();
  }
  window.addEventListener('resize', resize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize);
  dispose();
});

async function loadBoardOptions() {
  try {
    const data = await listBoards({ pageNum: 1, pageSize: 100 });
    const records = data?.records ?? (Array.isArray(data) ? data : []);
    boardOptions.value = records
      .filter(board => board.isEnabled !== 0)
      .map(board => ({
        value: board.boardId,
        label: [
          board.materialType,
          board.color,
          `${board.length}×${board.width}×${board.thickness}mm`
        ].filter(Boolean).join(' '),
        board
      }));
  } catch {
    boardOptions.value = [];
  }
}

function parseTemplateJson(preset) {
  if (!preset?.cabinetJson) return null;
  try {
    return typeof preset.cabinetJson === 'string'
      ? JSON.parse(preset.cabinetJson)
      : preset.cabinetJson;
  } catch {
    return null;
  }
}

function getPresetCategory(preset) {
  return preset?.category === 'base-cabinet' ? 'base-cabinet' : 'wardrobe';
}

function getWizardDefaults(preset) {
  const category = getPresetCategory(preset);
  const template = parseTemplateJson(preset);
  const cabinet = template?.cabinet ?? {};
  const boards = Array.isArray(template?.boards) ? template.boards : [];

  return {
    ...defaultWizardByCategory[category],
    width: Number(cabinet.width) || defaultWizardByCategory[category].width,
    height: Number(cabinet.height) || defaultWizardByCategory[category].height,
    depth: Number(cabinet.depth) || defaultWizardByCategory[category].depth,
    shelfCount: boards.filter(board => board.type === 'layer').length || defaultWizardByCategory[category].shelfCount,
    doorCount: boards.filter(board => board.type === 'door').length || defaultWizardByCategory[category].doorCount
  };
}

function onPresetClick(preset) {
  store.setSelectedPreset(preset);
  wizard.value = getWizardDefaults(preset);
  showWizard.value = true;
}

function createBoard({
  id,
  type,
  displayName,
  materialSlot,
  thickness,
  designLength,
  designWidth,
  position,
  placementFace,
  connectedTo = [],
  grain = 'none',
  edgeBanding = {},
  edgeRole = {},
  hingeHoles = []
}) {
  return {
    id,
    type,
    displayName,
    materialSlot,
    boardId: null,
    thickness,
    designLength,
    designWidth,
    grain,
    position,
    rotation: { x: 0, y: 0, z: 0 },
    placementFace,
    connectedTo,
    edgeBanding,
    edgeRole,
    hingeHoles
  };
}

function generateCabinetJson(params) {
  const category = getPresetCategory(store.selectedPreset);
  const width = Number(params.width);
  const height = Number(params.height);
  const depth = Number(params.depth);
  const shelfCount = Math.max(0, Number(params.shelfCount) || 0);
  const doorCount = Math.max(1, Number(params.doorCount) || 1);
  const thickness = 18;
  const innerWidth = Math.max(1, width - thickness * 2);
  const innerHeight = Math.max(1, height - thickness * 2);
  const cabinetName = categoryLabels[category];
  const boards = [];

  boards.push(createBoard({
    id: 'b-001',
    type: 'side',
    displayName: '左侧板',
    materialSlot: 'cabinet_body',
    thickness,
    designLength: height,
    designWidth: depth,
    position: { x: -width / 2 + thickness / 2, y: height / 2, z: 0 },
    placementFace: 'left',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: true, bottom: true },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
  }));

  boards.push(createBoard({
    id: 'b-002',
    type: 'side',
    displayName: '右侧板',
    materialSlot: 'cabinet_body',
    thickness,
    designLength: height,
    designWidth: depth,
    position: { x: width / 2 - thickness / 2, y: height / 2, z: 0 },
    placementFace: 'right',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: true, bottom: true },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
  }));

  boards.push(createBoard({
    id: 'b-003',
    type: 'top',
    displayName: '顶板',
    materialSlot: 'cabinet_body',
    thickness,
    designLength: width,
    designWidth: depth,
    position: { x: 0, y: height - thickness / 2, z: 0 },
    placementFace: 'top',
    connectedTo: ['b-001', 'b-002'],
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: false, bottom: true },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
  }));

  boards.push(createBoard({
    id: 'b-004',
    type: 'bottom',
    displayName: '底板',
    materialSlot: 'cabinet_body',
    thickness,
    designLength: width,
    designWidth: depth,
    position: { x: 0, y: thickness / 2, z: 0 },
    placementFace: 'bottom',
    connectedTo: ['b-001', 'b-002'],
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: true, bottom: false },
    edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
  }));

  boards.push(createBoard({
    id: 'b-005',
    type: 'back',
    displayName: '背板',
    materialSlot: 'back',
    thickness: 5,
    designLength: innerWidth,
    designWidth: innerHeight,
    position: { x: 0, y: height / 2, z: -depth / 2 + 2.5 },
    placementFace: 'back',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: false, bottom: false }
  }));

  const shelfSpacing = innerHeight / (shelfCount + 1);
  for (let index = 0; index < shelfCount; index += 1) {
    boards.push(createBoard({
      id: `b-${String(6 + index).padStart(3, '0')}`,
      type: 'layer',
      displayName: `层板${index + 1}`,
      materialSlot: 'cabinet_body',
      thickness,
      designLength: innerWidth,
      designWidth: depth,
      position: { x: 0, y: thickness + shelfSpacing * (index + 1), z: 0 },
      placementFace: 'inner',
      connectedTo: ['b-001', 'b-002'],
      grain: 'horizontal',
      edgeBanding: { left: false, right: false, top: false, bottom: false }
    }));
  }

  const doorWidth = innerWidth / doorCount;
  const doorHeight = Math.max(1, height - (category === 'base-cabinet' ? 50 : 50));
  for (let index = 0; index < doorCount; index += 1) {
    const isLeftDoor = index === 0;
    const isRightDoor = index === doorCount - 1;
    const hingeEdge = isLeftDoor ? 'left' : 'right';
    const opening = isLeftDoor ? 'left' : 'right';
    boards.push(createBoard({
      id: `b-${String(10 + index).padStart(3, '0')}`,
      type: 'door',
      displayName: `${index + 1}号门板`,
      materialSlot: 'door',
      thickness,
      designLength: doorHeight,
      designWidth: doorWidth,
      position: {
        x: -innerWidth / 2 + doorWidth / 2 + doorWidth * index,
        y: doorHeight / 2 + (height - doorHeight) / 2,
        z: depth / 2 + thickness / 2
      },
      placementFace: 'front',
      connectedTo: [isLeftDoor ? 'b-001' : (isRightDoor ? 'b-002' : 'b-001')],
      grain: 'vertical',
      edgeBanding: { left: true, right: true, top: true, bottom: true },
      hingeHoles: [{
        edge: hingeEdge,
        count: doorHeight > 1500 ? 3 : 2,
        spacing: 'even',
        diameter: 35,
        depth: 12,
        doorGap: 2,
        edgeDistance: 22,
        direction: 'height',
        opening
      }]
    }));
  }

  return {
    cabinet: {
      name: cabinetName,
      orderId: store.orderId,
      room: orderInfo.value?.room || '',
      purpose: '',
      width,
      height,
      depth,
      category
    },
    boards
  };
}

function buildScene(json) {
  nextTick(() => {
    if (!sceneReady.value) return;
    resize();
    buildCabinet(json.boards);
  });
}

function onWizardConfirm() {
  const json = generateCabinetJson(wizard.value);
  store.setCabinetJson(json);
  store.setWizardParams(wizard.value);
  selectedBoard.value = null;
  slotMap.value = createDefaultSlotMap(json.boards);
  showWizard.value = false;
  buildScene(json);
}

function getMaterialSlots(boards) {
  const slots = new Set();
  boards.forEach(board => {
    if (board.materialSlot) slots.add(board.materialSlot);
  });
  return Array.from(slots);
}

function formatMaterialSlot(slot) {
  return materialSlotLabels[slot] || slot;
}

function createDefaultSlotMap(boards) {
  const nextMap = {};
  getMaterialSlots(boards).forEach(slot => {
    const representative = boards.find(board => board.materialSlot === slot);
    const matched = boardOptions.value.find(option =>
      Number(option.board?.thickness) === Number(representative?.thickness)
    );
    nextMap[slot] = slotMap.value[slot] || matched?.value || null;
  });
  return nextMap;
}

function onOpenSlotMap() {
  slotMap.value = createDefaultSlotMap(store.cabinetJson?.boards ?? []);
  showSlotMap.value = true;
}

async function onSplit() {
  if (!store.cabinetJson) {
    ElMessage.warning('请先生成柜体模型');
    return;
  }
  if (!allSlotsMapped.value) {
    onOpenSlotMap();
    ElMessage.warning('请先完成板材映射');
    return;
  }

  try {
    store.splitItems = await store.executeSplit(store.cabinetJson, slotMap.value);
    showSplitPreview.value = true;
  } catch (e) {
    ElMessage.error(e?.message || '拆单计算失败');
  }
}

async function onConfirmSplit() {
  if (!allSlotsMapped.value) {
    onOpenSlotMap();
    ElMessage.warning('请先完成板材映射');
    return;
  }

  try {
    const result = await store.confirmSplit(slotMap.value);
    showSplitPreview.value = false;
    ElMessage.success(
      `拆单成功，批次号：${result.splitBatchCode}，共生成 ${result.createdItemIds?.length || 0} 条订单明细`
    );
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

const splitColumns = [
  { prop: 'partCode', label: '工件编号', minWidth: 100 },
  { prop: 'partName', label: '名称', minWidth: 80 },
  { prop: 'boardType', label: '类型', minWidth: 60 },
  { prop: 'materialName', label: '材质', minWidth: 100 },
  { prop: 'length', label: '切割长', minWidth: 70 },
  { prop: 'width', label: '切割宽', minWidth: 70 },
  { prop: 'thickness', label: '厚度', minWidth: 50 }
];
</script>

<template>
  <div class="cabinet-design-shell">
    <div class="cd-header">
      <span class="cd-title">3D 柜体设计</span>
      <span v-if="orderInfo" class="cd-order">
        订单 #{{ orderInfo.orderId }} | {{ orderInfo.customerName }} | {{ orderInfo.processName }}
      </span>
      <div class="cd-actions">
        <el-button size="small" :disabled="!hasCabinet" @click="onOpenSlotMap">板材映射</el-button>
        <el-button
          size="small"
          type="success"
          :disabled="!hasCabinet || store.splitting"
          :loading="store.splitting"
          @click="onSplit"
        >
          一键拆单
        </el-button>
      </div>
    </div>

    <div class="cd-body">
      <div class="cd-left">
        <h4>预设柜体</h4>
        <div class="preset-cards">
          <button
            v-for="preset in store.presets"
            :key="preset.id"
            class="preset-card"
            type="button"
            @click="onPresetClick(preset)"
          >
            <span class="preset-icon">{{ preset.category === 'wardrobe' ? '衣' : '柜' }}</span>
            <span class="preset-name">{{ preset.name }}</span>
            <span class="preset-cat">{{ categoryLabels[preset.category] || preset.category }}</span>
          </button>
          <div v-if="!loading && store.presets.length === 0" class="preset-hint">
            暂无预设模板，请先执行种子数据脚本
          </div>
          <div v-if="loading" class="preset-hint">加载中...</div>
        </div>

        <el-divider />

        <div v-if="selectedBoard" class="board-props">
          <h4>选中板件</h4>
          <p><b>名称：</b>{{ selectedBoard.displayName || selectedBoard.type }}</p>
          <p><b>类型：</b>{{ selectedBoard.type }}</p>
          <p><b>设计尺寸：</b>{{ selectedBoard.designLength }} × {{ selectedBoard.designWidth }} × {{ selectedBoard.thickness }}mm</p>
        </div>
        <div v-else class="preset-hint">
          点击模型中的板件查看尺寸和工艺信息
        </div>
      </div>

      <div class="cd-center">
        <canvas ref="canvasRef" class="cd-canvas" @click="onCanvasClick"></canvas>
        <div v-if="!hasCabinet" class="cd-canvas-placeholder">
          <p>选择左侧预设柜体以开始设计</p>
        </div>
      </div>
    </div>

    <el-dialog v-model="showWizard" title="柜体参数" width="440px">
      <el-form label-width="96px" size="small">
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

    <el-dialog v-model="showSlotMap" title="板材映射" width="520px">
      <p class="slot-hint">为每类材料角色选择实际板材，未完成映射时不能拆单。</p>
      <el-form v-if="hasCabinet" label-width="96px" size="small">
        <el-form-item v-for="slot in materialSlots" :key="slot" :label="formatMaterialSlot(slot)">
          <el-select v-model="slotMap[slot]" filterable placeholder="选择板材" style="width:100%">
            <el-option v-for="board in boardOptions" :key="board.value" :label="board.label" :value="board.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSlotMap = false">取消</el-button>
        <el-button type="primary" :disabled="!allSlotsMapped" @click="showSlotMap = false">确定</el-button>
      </template>
    </el-dialog>

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
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  gap: 8px;
  padding: 8px;
}

.cd-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.cd-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f766e;
}

.cd-order {
  font-size: 13px;
  color: #64748b;
  flex: 1;
}

.cd-actions {
  display: flex;
  gap: 8px;
}

.cd-body {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.cd-left {
  width: 260px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 12px;
  overflow-y: auto;
}

.cd-left h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #172033;
}

.preset-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.preset-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
  color: inherit;
}

.preset-card:hover {
  border-color: #0f766e;
  box-shadow: 0 0 0 2px rgba(15, 118, 110, 0.1);
}

.preset-icon,
.preset-name,
.preset-cat {
  display: block;
}

.preset-icon {
  width: 28px;
  height: 28px;
  line-height: 28px;
  margin: 0 auto 4px;
  border-radius: 6px;
  background: #ecfdf5;
  color: #0f766e;
  font-weight: 700;
}

.preset-name {
  font-size: 13px;
  font-weight: 600;
  color: #172033;
}

.preset-cat {
  font-size: 11px;
  color: #94a3b8;
}

.preset-hint {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 16px;
  grid-column: 1 / -1;
}

.board-props p {
  margin: 4px 0;
  font-size: 12px;
  color: #475569;
}

.cd-center {
  flex: 1;
  position: relative;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  min-width: 0;
}

.cd-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.cd-canvas-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #94a3b8;
  font-size: 16px;
  pointer-events: none;
}

.slot-hint {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
}
</style>
