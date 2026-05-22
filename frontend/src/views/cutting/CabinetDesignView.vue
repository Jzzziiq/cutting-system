<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import { RefreshLeft, RefreshRight, Aim } from '@element-plus/icons-vue';

import { useThreeScene } from '@/composables/useThreeScene';
import { useCabinetDesignStore } from '@/stores/cabinetDesign';
import { useCabinetGeometry } from '@/composables/useCabinetGeometry';
import { useSlotMapping } from '@/composables/useSlotMapping';
import { useSceneInteraction } from '@/composables/useSceneInteraction';
import { defaultWizardByCategory, categoryLabels } from '@/constants/cabinet';

import { getOrder } from '@/api/orders';
import { listBoards } from '@/api/boards';
import { createCabinetTemplate, updateCabinetTemplate, deleteCabinetTemplate } from '@/api/cabinet-templates';

import CabinetLeftSidebar from '@/components/cabinet/CabinetLeftSidebar.vue';
import BoardInspectorPanel from '@/components/cabinet/BoardInspectorPanel.vue';
import CabinetWizardDialog from '@/components/cabinet/CabinetWizardDialog.vue';
import SlotMapDialog from '@/components/cabinet/SlotMapDialog.vue';
import SplitPreviewDialog from '@/components/cabinet/SplitPreviewDialog.vue';

const route = useRoute();
const router = useRouter();
const store = useCabinetDesignStore();

// --- Three.js scene ---
const {
  canvasRef,
  init,
  buildCabinet,
  highlight,
  onClick,
  getDropPoint,
  getPlanePoint,
  pickBoard,
  setControlsEnabled,
  moveBoardPreview,
  resetView,
  resize,
  dispose
} = useThreeScene();

// --- Geometry helpers ---
const { numberOr, generateCabinetJson, getDefaultDropY, createFreeAssemblyBoard } = useCabinetGeometry();

// --- Local state ---
const orderInfo = ref(null);
const sceneReady = ref(false);
const selectedBoard = ref(null);
const showWizard = ref(false);
const showSplitPreview = ref(false);
const showSlotMap = ref(false);
const boardOptions = ref([]);
const loading = ref(false);
const moveStep = ref(50);

const selectedBoardId = computed(() => selectedBoard.value?.id || null);
const activeCabinetId = computed(() => store.activeCabinetId);
const activeCabinetJson = computed(() => store.cabinetJson);
const activeDraftBoards = computed(() => activeCabinetJson.value?.boards ?? []);
const selectedDraftBoard = computed(() =>
  activeDraftBoards.value.find(board => board.id === selectedBoardId.value) || null
);
const hasCabinet = computed(() => store.hasDrafts);
const cabinetDrafts = computed(() => store.cabinetDrafts);
const splitTotalCount = computed(() =>
  store.splitGroups.reduce((total, group) => total + group.items.length, 0)
);

// --- Slot mapping ---
const {
  slotMapsByCabinet,
  activeSlotMap,
  materialSlots,
  activeSlotsMapped,
  allCabinetsSlotsMapped,
  boardOptionsBySlot,
  formatMaterialSlot,
  getBoardOptionById,
  applyBoardAppearance,
  ensureSlotMapForCabinet,
  removeSlotMapForCabinet,
  setActiveSlotMapValue,
  findFirstUnmappedCabinetId
} = useSlotMapping(boardOptions, activeCabinetId, activeCabinetJson, activeDraftBoards, cabinetDrafts);

// --- Scene interaction ---
const {
  sceneDragMode,
  suppressNextCanvasClick,
  getCabinetBounds,
  snapPosition,
  onPartDragStart,
  onPartDragEnd,
  onCanvasDragOver,
  onCanvasDrop: _onCanvasDrop,
  onCanvasPointerDown,
  onCanvasPointerMove,
  onCanvasPointerUp,
  isSceneDragging,
  sceneDragModeLabel
} = useSceneInteraction({
  activeCabinetJson,
  activeDraftBoards,
  selectedBoard,
  selectedBoardId,
  numberOr,
  getDropPoint,
  getPlanePoint,
  pickBoard,
  setControlsEnabled,
  moveBoardPreview,
  highlight,
  updateBoard: (patch) => updateSelectedBoard(patch),
  moveStep
});

// --- Wizard ---
const wizard = ref({ ...defaultWizardByCategory.wardrobe });

// --- Scene build helper ---
function buildScene(json, materialSlotBoardMap = activeSlotMap.value, resetCamera = false) {
  nextTick(() => {
    if (!sceneReady.value) return;
    resize();
    buildCabinet(applyBoardAppearance(json?.boards ?? [], materialSlotBoardMap), resetCamera);
    if (selectedBoardId.value) {
      highlight(selectedBoardId.value);
    }
  });
}

// --- Board management ---
function updateSelectedBoard(patch) {
  if (!selectedBoardId.value) return;
  const updated = store.updateActiveBoard(selectedBoardId.value, patch);
  if (!updated) return;
  selectedBoard.value = updated;
  afterCabinetMutation(updated.id);
}

function updateSelectedPosition(axis, value) {
  updateSelectedBoard({ position: { [axis]: numberOr(value, 0) } });
}

function updateSelectedSize(field, value) {
  updateSelectedBoard({ [field]: Math.max(1, numberOr(value, 1)) });
}

function updateSelectedEdge(edge, value) {
  updateSelectedBoard({ edgeBanding: { [edge]: value } });
}

function onCopySelectedBoard() {
  if (!selectedBoardId.value) return;
  const copied = store.copyActiveBoard(selectedBoardId.value);
  if (!copied) return;
  selectedBoard.value = copied;
  afterCabinetMutation(copied.id);
}

function onDeleteSelectedBoard() {
  if (!selectedBoardId.value) return;
  const removed = store.removeActiveBoard(selectedBoardId.value);
  if (!removed) return;
  selectedBoard.value = null;
  afterCabinetMutation(null);
}

function onSnapSelectedBoard() {
  if (!selectedDraftBoard.value) return;
  updateSelectedBoard({
    position: snapPosition(selectedDraftBoard.value.position, selectedDraftBoard.value.thickness)
  });
}

function onNudgeSelectedBoard(axis, delta) {
  if (!selectedDraftBoard.value) return;
  const current = selectedDraftBoard.value.position ?? {};
  updateSelectedPosition(axis, numberOr(current[axis], 0) + delta);
}

// --- Undo/Redo ---
function onUndoHistory() {
  const previousSelectedId = selectedBoardId.value;
  store.undo();
  afterCabinetMutation(previousSelectedId);
}

function onRedoHistory() {
  const previousSelectedId = selectedBoardId.value;
  store.redo();
  afterCabinetMutation(previousSelectedId);
}

function onResetView() {
  resetView();
}

// --- Keyboard shortcuts ---
function isEditableTarget(target) {
  const tagName = target?.tagName?.toLowerCase();
  return target?.isContentEditable || ['input', 'textarea', 'select'].includes(tagName);
}

function onKeyboardShortcut(event) {
  if (isEditableTarget(event.target)) return;
  const key = event.key.toLowerCase();
  const hasModifier = event.ctrlKey || event.metaKey;
  if (hasModifier && key === 'z') {
    event.preventDefault();
    if (event.shiftKey) onRedoHistory();
    else onUndoHistory();
    return;
  }
  if (hasModifier && key === 'y') {
    event.preventDefault();
    onRedoHistory();
    return;
  }
  if (hasModifier && key === 'c' && selectedBoardId.value) {
    event.preventDefault();
    onCopySelectedBoard();
    return;
  }
  if ((event.key === 'Delete' || event.key === 'Backspace') && selectedBoardId.value) {
    event.preventDefault();
    onDeleteSelectedBoard();
    return;
  }
  if (key === 'r') {
    event.preventDefault();
    onResetView();
  }
}

// --- Draft management ---
function onWizardConfirm(params) {
  const json = generateCabinetJson(params, store.selectedPreset, store.cabinetDrafts, orderInfo.value);
  json.cabinet.orderId = store.orderId;
  const draft = store.addCabinetDraft(json);
  store.setWizardParams(params);
  selectedBoard.value = null;
  ensureSlotMapForCabinet(draft.clientCabinetId, json.boards);
  showWizard.value = false;
  buildScene(json, activeSlotMap.value, true);
}

function onPresetClick(preset) {
  store.setSelectedPreset(preset);
  wizard.value = getWizardDefaults(preset);
  showWizard.value = true;
}

function onNewCabinet() {
  store.setSelectedPreset(null);
  wizard.value = { ...defaultWizardByCategory.wardrobe };
  showWizard.value = true;
}

function getWizardDefaults(preset) {
  const category = preset?.category === 'base-cabinet' ? 'base-cabinet' : 'wardrobe';
  const template = parseTemplateJson(preset);
  const cabinet = template?.cabinet ?? {};
  const boards = Array.isArray(template?.boards) ? template.boards : [];
  return {
    ...defaultWizardByCategory[category],
    width: Number(cabinet.width) || defaultWizardByCategory[category].width,
    height: Number(cabinet.height) || defaultWizardByCategory[category].height,
    depth: Number(cabinet.depth) || defaultWizardByCategory[category].depth,
    shelfCount: boards.filter(b => b.type === 'layer').length || defaultWizardByCategory[category].shelfCount,
    doorCount: boards.filter(b => b.type === 'door').length || defaultWizardByCategory[category].doorCount
  };
}

function parseTemplateJson(preset) {
  if (!preset?.cabinetJson) return null;
  try {
    return typeof preset.cabinetJson === 'string' ? JSON.parse(preset.cabinetJson) : preset.cabinetJson;
  } catch {
    return null;
  }
}

function onSelectDraft(clientCabinetId) {
  store.setActiveCabinetId(clientCabinetId);
  selectedBoard.value = null;
  ensureSlotMapForCabinet(clientCabinetId, store.cabinetJson?.boards ?? []);
  buildScene(store.cabinetJson, activeSlotMap.value, true);
}

function onCopyDraft(draft) {
  const copied = store.copyCabinetDraft(draft.clientCabinetId);
  if (!copied) return;
  selectedBoard.value = null;
  ensureSlotMapForCabinet(copied.clientCabinetId, copied.cabinetJson?.boards ?? []);
  buildScene(copied.cabinetJson, activeSlotMap.value, true);
}

async function onRemoveDraft(draft) {
  try {
    await ElMessageBox.confirm(`确认移除「${draft.cabinetJson?.cabinet?.name || '未命名柜体'}」草稿？`, '移除柜体', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    store.removeCabinetDraft(draft.clientCabinetId);
    removeSlotMapForCabinet(draft.clientCabinetId);
    selectedBoard.value = null;
    if (store.cabinetJson) buildScene(store.cabinetJson, activeSlotMap.value, true);
    else buildScene(null, activeSlotMap.value, true);
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '移除柜体失败');
    }
  }
}

// --- Free board add ---
function addFreeBoard(partType, rawPosition = null) {
  if (!activeCabinetJson.value) {
    ElMessage.warning('请先新增柜体草稿');
    return;
  }
  const bounds = getCabinetBounds();
  const board = createFreeAssemblyBoard(partType, rawPosition, bounds, activeDraftBoards.value, snapPosition);
  if (!board) return;
  const created = store.addBoardToActiveCabinet(board);
  selectedBoard.value = created;
  afterCabinetMutation(created?.id);
}

function onCanvasDrop(event) {
  _onCanvasDrop(event, (pt) => getDefaultDropY(pt, getCabinetBounds()), addFreeBoard);
}

// --- Mutation helper ---
function syncSelectedBoardFromStore(boardId = selectedBoardId.value) {
  selectedBoard.value = activeDraftBoards.value.find(board => board.id === boardId) || null;
  return selectedBoard.value;
}

function afterCabinetMutation(boardId = selectedBoardId.value) {
  ensureSlotMapForCabinet(activeCabinetId.value, activeDraftBoards.value);
  syncSelectedBoardFromStore(boardId);
  buildScene(activeCabinetJson.value);
}

// --- Canvas click ---
function onCanvasClick(event) {
  if (suppressNextCanvasClick.value) {
    suppressNextCanvasClick.value = false;
    return;
  }
  onClick(event, (data) => {
    selectedBoard.value = activeDraftBoards.value.find(board => board.id === data.id) || data;
    highlight(data.id);
  });
}

// --- Slot map ---
function onOpenSlotMap() {
  if (!store.hasDrafts) {
    ElMessage.warning('请先新增柜体模型');
    return;
  }
  ensureSlotMapForCabinet(activeCabinetId.value, activeDraftBoards.value);
  showSlotMap.value = true;
}

function onSlotMapUpdate(slot, boardId) {
  setActiveSlotMapValue(slot, boardId, (nextMap) => {
    buildScene(activeCabinetJson.value, nextMap);
  });
}

// --- Split ---
async function onSplit() {
  if (!store.hasDrafts) {
    ElMessage.warning('请先新增柜体模型');
    return;
  }
  if (!allCabinetsSlotsMapped.value) {
    const unmappedCabinetId = findFirstUnmappedCabinetId();
    if (unmappedCabinetId) onSelectDraft(unmappedCabinetId);
    onOpenSlotMap();
    ElMessage.warning('请先完成板材映射');
    return;
  }
  try {
    await store.executeAllSplits(slotMapsByCabinet.value);
    showSplitPreview.value = true;
  } catch (e) {
    ElMessage.error(e?.message || '拆单计算失败');
  }
}

async function onConfirmSplit() {
  if (!allCabinetsSlotsMapped.value) {
    const unmappedCabinetId = findFirstUnmappedCabinetId();
    if (unmappedCabinetId) onSelectDraft(unmappedCabinetId);
    onOpenSlotMap();
    ElMessage.warning('请先完成板材映射');
    return;
  }
  try {
    const results = await store.confirmAllSplits(slotMapsByCabinet.value);
    showSplitPreview.value = false;
    const totalItems = results.reduce((total, result) => total + (result.createdItemIds?.length || 0), 0);
    ElMessage.success(`统一拆单成功，共 ${results.length} 个柜体，生成 ${totalItems} 条订单明细`);
    if (results.some(result => result.nextAction === 'data-input')) {
      router.push({ name: 'data-input', query: { orderId: store.orderId } });
    }
  } catch (e) {
    if (e?.confirmedResults?.length) {
      ElMessage.error(`确认拆单失败，已写入 ${e.confirmedResults.length} 个柜体，请修正后重试剩余草稿`);
      return;
    }
    ElMessage.error(e?.message || '确认拆单失败');
  }
}

// --- Template CRUD ---
async function onSaveTemplate() {
  const json = store.cabinetJson;
  if (!json) { ElMessage.warning('请先生成柜体模型'); return; }
  try {
    const { value: name } = await ElMessageBox.prompt('请输入模板名称', '保存为模板', {
      confirmButtonText: '保存', cancelButtonText: '取消',
      inputValue: store.selectedPreset?.name || ''
    });
    if (!name?.trim()) return;
    const category = store.selectedPreset?.category === 'base-cabinet' ? 'base-cabinet' : 'wardrobe';
    await createCabinetTemplate({ name: name.trim(), category, cabinetJson: JSON.stringify(json) });
    ElMessage.success('模板已保存');
    await store.loadPresets();
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error(e?.message || '保存模板失败');
  }
}

async function onEditTemplate(preset) {
  try {
    const { value: name } = await ElMessageBox.prompt('修改模板名称', '编辑模板', {
      confirmButtonText: '保存', cancelButtonText: '取消', inputValue: preset.name
    });
    if (!name?.trim()) return;
    await updateCabinetTemplate(preset.id, { name: name.trim() });
    ElMessage.success('模板已更新');
    await store.loadPresets();
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error(e?.message || '编辑模板失败');
  }
}

async function onDeleteTemplate(preset) {
  try {
    await ElMessageBox.confirm(`确认删除模板「${preset.name}」？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    });
    await deleteCabinetTemplate(preset.id);
    ElMessage.success('模板已删除');
    await store.loadPresets();
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error(e?.message || '删除模板失败');
  }
}

// --- Data loading ---
async function loadBoardOptions() {
  try {
    const data = await listBoards({ pageNum: 1, pageSize: 100 });
    const records = data?.records ?? (Array.isArray(data) ? data : []);
    boardOptions.value = records
      .filter(board => board.isEnabled !== 0)
      .map(board => ({
        value: board.boardId,
        label: [board.materialType, board.color, `${board.length}×${board.width}×${board.thickness}mm`].filter(Boolean).join(' '),
        board
      }));
  } catch {
    boardOptions.value = [];
  }
}

// --- Lifecycle ---
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
  slotMapsByCabinet.value = {};

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
    if (activeCabinetJson.value) {
      ensureSlotMapForCabinet(activeCabinetId.value, activeDraftBoards.value);
      buildScene(activeCabinetJson.value, activeSlotMap.value, true);
    }
  }
  window.addEventListener('resize', resize);
  window.addEventListener('keydown', onKeyboardShortcut);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize);
  window.removeEventListener('keydown', onKeyboardShortcut);
  dispose();
});
</script>

<template>
  <div class="cabinet-design-shell">
    <div class="cd-header">
      <span class="cd-title">3D 柜体设计</span>
      <span v-if="orderInfo" class="cd-order">
        订单 #{{ orderInfo.orderId }} | {{ orderInfo.customerName }} | {{ orderInfo.processName }}
      </span>
      <div class="cd-actions">
        <el-tooltip content="撤销" placement="bottom">
          <el-button size="small" :icon="RefreshLeft" circle :disabled="!store.canUndo" @click="onUndoHistory" />
        </el-tooltip>
        <el-tooltip content="重做" placement="bottom">
          <el-button size="small" :icon="RefreshRight" circle :disabled="!store.canRedo" @click="onRedoHistory" />
        </el-tooltip>
        <el-tooltip content="视角复位" placement="bottom">
          <el-button size="small" :icon="Aim" circle :disabled="!hasCabinet" @click="onResetView" />
        </el-tooltip>
        <el-button size="small" :disabled="!activeCabinetJson" @click="onSaveTemplate">保存当前柜体为模板</el-button>
        <el-button size="small" :disabled="!hasCabinet" @click="onOpenSlotMap">板材映射</el-button>
        <el-button
          size="small"
          type="success"
          :disabled="!hasCabinet || store.splitting"
          :loading="store.splitting"
          @click="onSplit"
        >
          拆单预览
        </el-button>
      </div>
    </div>

    <div class="cd-body">
      <CabinetLeftSidebar
        :drafts="store.cabinetDrafts"
        :active-cabinet-id="activeCabinetId"
        :presets="store.presets"
        :active-cabinet-json="activeCabinetJson"
        :loading="loading"
        @select-draft="onSelectDraft"
        @copy-draft="onCopyDraft"
        @remove-draft="onRemoveDraft"
        @new-cabinet="onNewCabinet"
        @preset-click="onPresetClick"
        @edit-template="onEditTemplate"
        @delete-template="onDeleteTemplate"
        @drag-start="onPartDragStart"
        @drag-end="onPartDragEnd"
        @dblclick-part="addFreeBoard"
      />

      <div class="cd-center" :class="{ 'is-dragging': isSceneDragging }">
        <canvas
          ref="canvasRef"
          class="cd-canvas"
          @click="onCanvasClick"
          @pointerdown="onCanvasPointerDown"
          @pointermove="onCanvasPointerMove"
          @pointerup="onCanvasPointerUp"
          @pointerleave="onCanvasPointerUp"
          @dragover="onCanvasDragOver"
          @drop="onCanvasDrop"
        ></canvas>
        <div v-if="isSceneDragging" class="drag-status">
          {{ sceneDragModeLabel }}
        </div>
        <div v-if="!activeCabinetJson" class="cd-canvas-placeholder">
          <p>从左侧新增柜体，完成全屋建模后再统一拆单</p>
        </div>
      </div>

      <BoardInspectorPanel
        :active-cabinet-json="activeCabinetJson"
        :active-draft-boards="activeDraftBoards"
        :selected-board="selectedDraftBoard"
        :material-slots="materialSlots"
        :active-slot-map="activeSlotMap"
        :active-slots-mapped="activeSlotsMapped"
        :scene-drag-mode="sceneDragMode"
        :move-step="moveStep"
        :format-material-slot="formatMaterialSlot"
        :get-board-option-by-id="getBoardOptionById"
        @update:board="updateSelectedBoard"
        @update:position="updateSelectedPosition"
        @update:size="updateSelectedSize"
        @update:edge="updateSelectedEdge"
        @update:grain="value => updateSelectedBoard({ grain: value })"
        @update:placement-face="value => updateSelectedBoard({ placementFace: value })"
        @update:display-name="value => updateSelectedBoard({ displayName: value })"
        @update:scene-drag-mode="value => sceneDragMode = value"
        @copy-board="onCopySelectedBoard"
        @delete-board="onDeleteSelectedBoard"
        @snap-board="onSnapSelectedBoard"
        @nudge-board="onNudgeSelectedBoard"
      />
    </div>

    <CabinetWizardDialog
      v-model="showWizard"
      :wizard="wizard"
      @confirm="onWizardConfirm"
    />

    <SlotMapDialog
      v-model="showSlotMap"
      :cabinet-name="activeCabinetJson?.cabinet?.name || ''"
      :has-cabinet="hasCabinet"
      :material-slots="materialSlots"
      :active-slot-map="activeSlotMap"
      :active-slots-mapped="activeSlotsMapped"
      :board-options-by-slot="boardOptionsBySlot"
      :format-material-slot="formatMaterialSlot"
      @update:slot="onSlotMapUpdate"
    />

    <SplitPreviewDialog
      v-model="showSplitPreview"
      :split-groups="store.splitGroups"
      :split-items="store.splitItems"
      :split-total-count="splitTotalCount"
      :confirming="store.confirming"
      @confirm="onConfirmSplit"
    />
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
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.cd-body {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
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

.cd-center.is-dragging .cd-canvas {
  cursor: grabbing;
}

.cd-canvas {
  width: 100%;
  height: 100%;
  display: block;
  cursor: grab;
  touch-action: none;
}

.drag-status {
  position: absolute;
  top: 12px;
  left: 12px;
  min-width: 44px;
  padding: 5px 10px;
  border: 1px solid rgba(15, 118, 110, 0.22);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  pointer-events: none;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
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

@media (max-width: 980px) {
  .cabinet-design-shell {
    height: auto;
    min-height: calc(100vh - 60px);
  }

  .cd-header {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .cd-order {
    flex-basis: 100%;
  }

  .cd-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .cd-body {
    flex-direction: column;
    overflow: visible;
  }

  .cd-center {
    flex: 0 0 auto;
    min-height: 420px;
  }
}
</style>
