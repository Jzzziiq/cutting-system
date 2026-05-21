<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import {
  Aim,
  CopyDocument,
  Delete,
  Edit,
  Plus,
  Position,
  Rank,
  RefreshLeft,
  RefreshRight
} from '@element-plus/icons-vue';
import { useThreeScene } from '@/composables/useThreeScene';
import { useCabinetDesignStore } from '@/stores/cabinetDesign';
import { getOrder } from '@/api/orders';
import { listBoards } from '@/api/boards';
import { createCabinetTemplate, updateCabinetTemplate, deleteCabinetTemplate } from '@/api/cabinet-templates';

const route = useRoute();
const router = useRouter();
const store = useCabinetDesignStore();
const {
  canvasRef,
  init,
  buildCabinet,
  highlight,
  onClick,
  getDropPoint,
  resetView,
  resize,
  dispose
} = useThreeScene();

const orderInfo = ref(null);
const sceneReady = ref(false);
const selectedBoard = ref(null);
const showWizard = ref(false);
const showSplitPreview = ref(false);
const showSlotMap = ref(false);
const slotMapsByCabinet = ref({});
const boardOptions = ref([]);
const loading = ref(false);
const draggingPartType = ref(null);
const moveStep = ref(50);

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

const boardTypeLabels = {
  side: '侧板',
  layer: '层板',
  door: '门板',
  back: '背板',
  top: '顶板',
  bottom: '底板'
};

const grainOptions = [
  { value: 'none', label: '无纹理' },
  { value: 'vertical', label: '竖纹' },
  { value: 'horizontal', label: '横纹' }
];

const placementFaceOptions = [
  { value: 'inner', label: '内部' },
  { value: 'left', label: '左侧' },
  { value: 'right', label: '右侧' },
  { value: 'front', label: '正面' },
  { value: 'back', label: '背面' },
  { value: 'top', label: '顶部' },
  { value: 'bottom', label: '底部' }
];

const edgeLabels = {
  left: '左',
  right: '右',
  top: '上',
  bottom: '下'
};

const freeBoardParts = [
  {
    type: 'side',
    label: '侧板',
    materialSlot: 'cabinet_body',
    designLength: 2200,
    designWidth: 600,
    thickness: 18,
    placementFace: 'left',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: true, bottom: true }
  },
  {
    type: 'layer',
    label: '层板',
    materialSlot: 'cabinet_body',
    designLength: 800,
    designWidth: 560,
    thickness: 18,
    placementFace: 'inner',
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: false, bottom: false }
  },
  {
    type: 'door',
    label: '门板',
    materialSlot: 'door',
    designLength: 2150,
    designWidth: 400,
    thickness: 18,
    placementFace: 'front',
    grain: 'vertical',
    edgeBanding: { left: true, right: true, top: true, bottom: true },
    hingeHoles: [{
      edge: 'left',
      count: 3,
      spacing: 'even',
      diameter: 35,
      depth: 12,
      doorGap: 2,
      edgeDistance: 22,
      direction: 'height',
      opening: 'left'
    }]
  },
  {
    type: 'back',
    label: '背板',
    materialSlot: 'back',
    designLength: 800,
    designWidth: 2000,
    thickness: 5,
    placementFace: 'back',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: false, bottom: false }
  },
  {
    type: 'top',
    label: '顶板',
    materialSlot: 'cabinet_body',
    designLength: 800,
    designWidth: 560,
    thickness: 18,
    placementFace: 'top',
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: false, bottom: true }
  },
  {
    type: 'bottom',
    label: '底板',
    materialSlot: 'cabinet_body',
    designLength: 800,
    designWidth: 560,
    thickness: 18,
    placementFace: 'bottom',
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: true, bottom: false }
  }
];

const cabinetPanelThicknessRange = { min: 12, max: 25 };
const materialSlotThicknessRanges = {
  cabinet_body: cabinetPanelThicknessRange,
  door: cabinetPanelThicknessRange,
  back: { min: 3, max: 9 }
};
const boardColorPalette = [
  { keyword: '暖白', color: '#f8fafc' },
  { keyword: '白', color: '#f1f5f9' },
  { keyword: '深灰', color: '#64748b' },
  { keyword: '灰', color: '#94a3b8' },
  { keyword: '黑', color: '#1f2937' },
  { keyword: '胡桃', color: '#8b5a2b' },
  { keyword: '原木', color: '#d6a15f' },
  { keyword: '木', color: '#b7791f' },
  { keyword: '红', color: '#b91c1c' },
  { keyword: '蓝', color: '#2563eb' },
  { keyword: '绿', color: '#0f766e' },
  { keyword: '黄', color: '#ca8a04' }
];

const wizard = ref({ ...defaultWizardByCategory.wardrobe });

const hasCabinet = computed(() => store.hasDrafts);
const activeCabinetId = computed(() => store.activeCabinetId);
const activeCabinetJson = computed(() => store.cabinetJson);
const activeDraftBoards = computed(() => activeCabinetJson.value?.boards ?? []);
const selectedBoardId = computed(() => selectedBoard.value?.id || null);
const selectedDraftBoard = computed(() =>
  activeDraftBoards.value.find(board => board.id === selectedBoardId.value) || null
);
const materialSlots = computed(() => getMaterialSlots(activeDraftBoards.value));
const activeSlotMap = computed(() => slotMapsByCabinet.value[activeCabinetId.value] ?? {});
const activeSlotsMapped = computed(() =>
  materialSlots.value.length > 0 && materialSlots.value.every(slot => Boolean(activeSlotMap.value[slot]))
);
const allCabinetsSlotsMapped = computed(() =>
  store.cabinetDrafts.length > 0 && store.cabinetDrafts.every((draft) => {
    const slots = getMaterialSlots(draft.cabinetJson?.boards ?? []);
    const map = slotMapsByCabinet.value[draft.clientCabinetId] ?? {};
    return slots.length > 0 && slots.every(slot => Boolean(map[slot]));
  })
);
const boardOptionsBySlot = computed(() =>
  materialSlots.value.reduce((optionsBySlot, slot) => {
    optionsBySlot[slot] = getSelectableBoardOptionsForSlot(slot);
    return optionsBySlot;
  }, {})
);
const splitTotalCount = computed(() =>
  store.splitGroups.reduce((total, group) => total + group.items.length, 0)
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
      buildScene(activeCabinetJson.value);
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

function onNewCabinet() {
  store.setSelectedPreset(null);
  wizard.value = { ...defaultWizardByCategory.wardrobe };
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
  const categoryDraftCount = store.cabinetDrafts.filter(draft =>
    draft.cabinetJson?.cabinet?.category === category
  ).length;
  const cabinetName = `${categoryLabels[category]}${categoryDraftCount + 1}`;
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
    designLength: innerWidth,
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
    designLength: innerWidth,
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
      id: `b-${String(20 + index).padStart(3, '0')}`,
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

function buildScene(json, materialSlotBoardMap = activeSlotMap.value) {
  nextTick(() => {
    if (!sceneReady.value) return;
    resize();
    buildCabinet(applyBoardAppearance(json?.boards ?? [], materialSlotBoardMap));
    if (selectedBoardId.value) {
      highlight(selectedBoardId.value);
    }
  });
}

function numberOr(value, fallback) {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : fallback;
}

function formatBoardType(type) {
  return boardTypeLabels[type] || type || '-';
}

function getPartConfig(partType) {
  return freeBoardParts.find(part => part.type === partType) || null;
}

function snapAxis(value, guides, threshold = 32) {
  const numeric = numberOr(value, 0);
  const nearest = guides
    .map(guide => ({ guide, distance: Math.abs(guide - numeric) }))
    .sort((a, b) => a.distance - b.distance)[0];
  if (nearest && nearest.distance <= threshold) return nearest.guide;
  return Math.round(numeric / moveStep.value) * moveStep.value;
}

function getCabinetBounds() {
  const cabinet = activeCabinetJson.value?.cabinet ?? {};
  const width = numberOr(cabinet.width, 1200);
  const height = numberOr(cabinet.height, 2200);
  const depth = numberOr(cabinet.depth, 600);
  return { width, height, depth };
}

function getSnapGuides(thickness = 18) {
  const { width, height, depth } = getCabinetBounds();
  const boardPositions = activeDraftBoards.value.map(board => board.position ?? {});
  return {
    x: [
      -width / 2 + thickness / 2,
      -width / 2,
      0,
      width / 2,
      width / 2 - thickness / 2,
      ...boardPositions.map(position => numberOr(position.x, 0))
    ],
    y: [
      thickness / 2,
      height / 2,
      height - thickness / 2,
      ...boardPositions.map(position => numberOr(position.y, 0))
    ],
    z: [
      -depth / 2 + thickness / 2,
      -depth / 2,
      0,
      depth / 2,
      depth / 2 + thickness / 2,
      ...boardPositions.map(position => numberOr(position.z, 0))
    ]
  };
}

function snapPosition(position, thickness = 18) {
  const guides = getSnapGuides(thickness);
  return {
    x: snapAxis(position?.x, guides.x),
    y: snapAxis(position?.y, guides.y),
    z: snapAxis(position?.z, guides.z)
  };
}

function getDefaultDropY(partType) {
  const { height } = getCabinetBounds();
  const part = getPartConfig(partType);
  const thickness = part?.thickness || 18;
  if (partType === 'top') return Math.max(thickness / 2, height - thickness / 2);
  if (partType === 'bottom') return thickness / 2;
  return height / 2;
}

function createFreeAssemblyBoard(partType, rawPosition = null) {
  const part = getPartConfig(partType);
  if (!part) return null;
  const { width, height, depth } = getCabinetBounds();
  const thickness = part.thickness || 18;
  const innerWidth = Math.max(1, width - thickness * 2);
  const innerHeight = Math.max(1, height - thickness * 2);
  const partCount = activeDraftBoards.value.filter(board => board.type === part.type).length + 1;

  const dimensionByType = {
    side: { designLength: height, designWidth: depth },
    back: { designLength: innerWidth, designWidth: innerHeight },
    door: { designLength: Math.max(1, height - 50), designWidth: Math.max(1, innerWidth / 2) },
    top: { designLength: innerWidth, designWidth: depth },
    bottom: { designLength: innerWidth, designWidth: depth },
    layer: { designLength: innerWidth, designWidth: depth }
  };
  const dimensions = dimensionByType[part.type] ?? part;
  const fallbackPosition = {
    x: 0,
    y: getDefaultDropY(part.type),
    z: part.type === 'door' ? depth / 2 + thickness / 2 : 0
  };
  if (part.type === 'back') fallbackPosition.z = -depth / 2 + thickness / 2;
  if (part.type === 'top') fallbackPosition.y = height - thickness / 2;
  if (part.type === 'bottom') fallbackPosition.y = thickness / 2;

  return createBoard({
    type: part.type,
    displayName: `${part.label}${partCount}`,
    materialSlot: part.materialSlot,
    thickness,
    designLength: dimensions.designLength,
    designWidth: dimensions.designWidth,
    position: snapPosition(rawPosition || fallbackPosition, thickness),
    placementFace: part.placementFace,
    grain: part.grain,
    edgeBanding: { ...(part.edgeBanding ?? {}) },
    hingeHoles: part.hingeHoles ? JSON.parse(JSON.stringify(part.hingeHoles)) : []
  });
}

function syncSelectedBoardFromStore(boardId = selectedBoardId.value) {
  selectedBoard.value = activeDraftBoards.value.find(board => board.id === boardId) || null;
  return selectedBoard.value;
}

function afterCabinetMutation(boardId = selectedBoardId.value) {
  ensureSlotMapForCabinet(activeCabinetId.value, activeDraftBoards.value);
  syncSelectedBoardFromStore(boardId);
  buildScene(activeCabinetJson.value);
}

function addFreeBoard(partType, rawPosition = null) {
  if (!activeCabinetJson.value) {
    ElMessage.warning('请先新增柜体草稿');
    return;
  }
  const board = createFreeAssemblyBoard(partType, rawPosition);
  if (!board) return;
  const created = store.addBoardToActiveCabinet(board);
  selectedBoard.value = created;
  afterCabinetMutation(created?.id);
}

function onPartDragStart(event, part) {
  draggingPartType.value = part.type;
  event.dataTransfer?.setData('application/x-cabinet-part', part.type);
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy';
}

function onPartDragEnd() {
  draggingPartType.value = null;
}

function onCanvasDragOver(event) {
  const types = Array.from(event.dataTransfer?.types ?? []);
  if (!draggingPartType.value && !types.includes('application/x-cabinet-part')) return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy';
}

function onCanvasDrop(event) {
  const partType = event.dataTransfer?.getData('application/x-cabinet-part') || draggingPartType.value;
  if (!partType) return;
  event.preventDefault();
  const point = getDropPoint(event, { y: getDefaultDropY(partType), snapSize: moveStep.value });
  addFreeBoard(partType, point);
  draggingPartType.value = null;
}

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

function onWizardConfirm() {
  const json = generateCabinetJson(wizard.value);
  const draft = store.addCabinetDraft(json);
  store.setWizardParams(wizard.value);
  selectedBoard.value = null;
  ensureSlotMapForCabinet(draft.clientCabinetId, json.boards);
  showWizard.value = false;
  buildScene(json);
}

function onSelectDraft(clientCabinetId) {
  store.setActiveCabinetId(clientCabinetId);
  selectedBoard.value = null;
  ensureSlotMapForCabinet(clientCabinetId, store.cabinetJson?.boards ?? []);
  buildScene(store.cabinetJson);
}

function onCopyDraft(draftOrId) {
  const clientCabinetId = typeof draftOrId === 'string' ? draftOrId : draftOrId?.clientCabinetId;
  const draft = store.copyCabinetDraft(clientCabinetId);
  if (!draft) return;
  selectedBoard.value = null;
  ensureSlotMapForCabinet(draft.clientCabinetId, draft.cabinetJson?.boards ?? []);
  buildScene(draft.cabinetJson);
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
    if (store.cabinetJson) buildScene(store.cabinetJson);
    else buildScene(null);
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '移除柜体失败');
    }
  }
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

function getBoardOptionsForSlot(slot) {
  const range = materialSlotThicknessRanges[slot];
  if (!range) return [];
  return boardOptions.value.filter(option => isThicknessInRange(option.board?.thickness, range));
}

function getSelectableBoardOptionsForSlot(slot) {
  const matchedOptions = getBoardOptionsForSlot(slot);
  return matchedOptions.length ? matchedOptions : boardOptions.value;
}

function getBoardOptionById(boardId) {
  return boardOptions.value.find(option => Number(option.value) === Number(boardId)) || null;
}

function resolveBoardAppearanceColor(board) {
  const text = [board?.color, board?.materialType, board?.brand].filter(Boolean).join(' ');
  const matched = boardColorPalette.find(item => text.includes(item.keyword));
  return matched?.color || null;
}

function applyBoardAppearance(boards, materialSlotBoardMap = {}) {
  return boards.map((board) => {
    const boardId = board.boardId || materialSlotBoardMap[board.materialSlot];
    const mappedBoard = getBoardOptionById(boardId)?.board;
    const appearanceColor = resolveBoardAppearanceColor(mappedBoard);
    return {
      ...board,
      mappedBoard,
      textureUrl: mappedBoard?.textureUrl || undefined,
      appearanceColor: appearanceColor || undefined
    };
  });
}

function isThicknessInRange(thickness, range) {
  const value = Number(thickness);
  return Number.isFinite(value) && value >= range.min && value <= range.max;
}

function createDefaultSlotMap(boards, previousMap = {}) {
  const nextMap = {};
  getMaterialSlots(boards).forEach(slot => {
    const representative = boards.find(board => board.materialSlot === slot);
    const slotOptions = getSelectableBoardOptionsForSlot(slot);
    const current = slotOptions.some(option => option.value === previousMap[slot])
      ? previousMap[slot]
      : null;
    const matched = slotOptions.find(option =>
      Number(option.board?.thickness) === Number(representative?.thickness)
    ) || slotOptions[0];
    nextMap[slot] = current || matched?.value || null;
  });
  return nextMap;
}

function ensureSlotMapForCabinet(clientCabinetId, boards) {
  if (!clientCabinetId) return {};
  const nextMap = createDefaultSlotMap(boards, slotMapsByCabinet.value[clientCabinetId] ?? {});
  slotMapsByCabinet.value = {
    ...slotMapsByCabinet.value,
    [clientCabinetId]: nextMap
  };
  return nextMap;
}

function removeSlotMapForCabinet(clientCabinetId) {
  const nextMaps = { ...slotMapsByCabinet.value };
  delete nextMaps[clientCabinetId];
  slotMapsByCabinet.value = nextMaps;
}

function setActiveSlotMapValue(slot, boardId) {
  if (!activeCabinetId.value) return;
  const nextMap = {
    ...activeSlotMap.value,
    [slot]: boardId
  };
  slotMapsByCabinet.value = {
    ...slotMapsByCabinet.value,
    [activeCabinetId.value]: nextMap
  };
  buildScene(activeCabinetJson.value, nextMap);
}

function findFirstUnmappedCabinetId() {
  const draft = store.cabinetDrafts.find((item) => {
    const slots = getMaterialSlots(item.cabinetJson?.boards ?? []);
    const map = slotMapsByCabinet.value[item.clientCabinetId] ?? {};
    return slots.length === 0 || slots.some(slot => !map[slot]);
  });
  return draft?.clientCabinetId || null;
}

function onOpenSlotMap() {
  if (!store.hasDrafts) {
    ElMessage.warning('请先新增柜体模型');
    return;
  }
  ensureSlotMapForCabinet(activeCabinetId.value, activeDraftBoards.value);
  showSlotMap.value = true;
}

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
    ElMessage.success(
      `统一拆单成功，共 ${results.length} 个柜体，生成 ${totalItems} 条订单明细`
    );
    if (results.some(result => result.nextAction === 'layout-workbench')) {
      router.push({ name: 'layout-workbench', query: { orderId: store.orderId, source: 'cabinet' } });
    }
  } catch (e) {
    if (e?.confirmedResults?.length) {
      ElMessage.error(`确认拆单失败，已写入 ${e.confirmedResults.length} 个柜体，请修正后重试剩余草稿`);
      return;
    }
    ElMessage.error(e?.message || '确认拆单失败');
  }
}

async function onSaveTemplate() {
  const json = store.cabinetJson;
  if (!json) { ElMessage.warning('请先生成柜体模型'); return; }
  try {
    const { value: name } = await ElMessageBox.prompt('请输入模板名称', '保存为模板', {
      confirmButtonText: '保存', cancelButtonText: '取消',
      inputValue: store.selectedPreset?.name || ''
    });
    if (!name?.trim()) return;
    const category = getPresetCategory(store.selectedPreset);
    await createCabinetTemplate({
      name: name.trim(), category,
      cabinetJson: JSON.stringify(json)
    });
    ElMessage.success('模板已保存');
    await store.loadPresets();
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '保存模板失败');
    }
  }
}

async function onEditTemplate(preset) {
  try {
    const { value: name } = await ElMessageBox.prompt('修改模板名称', '编辑模板', {
      confirmButtonText: '保存', cancelButtonText: '取消',
      inputValue: preset.name
    });
    if (!name?.trim()) return;
    await updateCabinetTemplate(preset.id, { name: name.trim() });
    ElMessage.success('模板已更新');
    await store.loadPresets();
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '编辑模板失败');
    }
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
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '删除模板失败');
    }
  }
}

function onCanvasClick(event) {
  onClick(event, (data) => {
    selectedBoard.value = activeDraftBoards.value.find(board => board.id === data.id) || data;
    highlight(data.id);
  });
}

const splitColumns = [
  { prop: 'cabinetName', label: '柜体', minWidth: 90 },
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
      <div class="cd-left">
        <div class="panel-title-row">
          <h4>订单柜体</h4>
          <el-tag size="small" type="info">{{ store.cabinetDrafts.length }} 个</el-tag>
        </div>
        <div v-if="store.cabinetDrafts.length" class="draft-list">
          <div
            v-for="draft in store.cabinetDrafts"
            :key="draft.clientCabinetId"
            class="draft-item"
            :class="{ active: draft.clientCabinetId === store.activeCabinetId }"
            role="button"
            tabindex="0"
            @click="onSelectDraft(draft.clientCabinetId)"
            @keydown.enter="onSelectDraft(draft.clientCabinetId)"
            @keydown.space.prevent="onSelectDraft(draft.clientCabinetId)"
          >
            <span class="draft-main">
              <span class="draft-name">{{ draft.cabinetJson?.cabinet?.name || '未命名柜体' }}</span>
              <span class="draft-meta">
                {{ draft.cabinetJson?.cabinet?.width }} × {{ draft.cabinetJson?.cabinet?.height }} × {{ draft.cabinetJson?.cabinet?.depth }}mm
              </span>
            </span>
            <span class="draft-actions" @click.stop>
              <el-button size="small" text :icon="CopyDocument" @click="onCopyDraft(draft)" />
              <el-button size="small" text type="danger" :icon="Delete" @click="onRemoveDraft(draft)" />
            </span>
          </div>
        </div>
        <div v-else class="preset-hint">
          一个订单可先维护多个柜体，全部完成后再统一拆单。
        </div>

        <el-divider />

        <div class="panel-title-row">
          <h4>新增柜体</h4>
          <el-button size="small" type="primary" plain :icon="Plus" @click="onNewCabinet">空柜体</el-button>
        </div>
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
            <span v-if="preset.isOfficial !== 1" class="preset-tag">我的</span>
            <span
              v-if="preset.isOfficial !== 1"
              class="preset-actions"
              @click.stop
            >
              <el-button size="small" :icon="Edit" text @click="onEditTemplate(preset)" />
              <el-button size="small" :icon="Delete" text type="danger" @click="onDeleteTemplate(preset)" />
            </span>
          </button>
          <div v-if="!loading && store.presets.length === 0" class="preset-hint">
            暂无预设模板，请先执行种子数据脚本
          </div>
          <div v-if="loading" class="preset-hint">加载中...</div>
        </div>

        <el-divider />
        <div class="panel-title-row">
          <h4>板件库</h4>
        </div>
        <div class="part-library">
          <button
            v-for="part in freeBoardParts"
            :key="part.type"
            class="part-card"
            type="button"
            draggable="true"
            :disabled="!activeCabinetJson"
            @dragstart="onPartDragStart($event, part)"
            @dragend="onPartDragEnd"
            @dblclick="addFreeBoard(part.type)"
          >
            <el-icon><Rank /></el-icon>
            <span>
              <strong>{{ part.label }}</strong>
              <small>{{ part.designLength }} × {{ part.designWidth }} × {{ part.thickness }}mm</small>
            </span>
          </button>
        </div>

        <el-divider />
        <div class="preset-hint">
          预设柜体按宽、高、深和层板/门板数量生成。
        </div>
      </div>

      <div class="cd-center">
        <canvas
          ref="canvasRef"
          class="cd-canvas"
          @click="onCanvasClick"
          @dragover="onCanvasDragOver"
          @drop="onCanvasDrop"
        ></canvas>
        <div v-if="!activeCabinetJson" class="cd-canvas-placeholder">
          <p>从左侧新增柜体，完成全屋建模后再统一拆单</p>
        </div>
      </div>

      <div class="cd-right">
        <section class="props-section">
          <div class="panel-title-row">
            <h4>当前柜体</h4>
            <el-tag v-if="activeCabinetJson" size="small" type="info">
              {{ categoryLabels[activeCabinetJson.cabinet?.category] || activeCabinetJson.cabinet?.category }}
            </el-tag>
          </div>
          <div v-if="activeCabinetJson" class="prop-list">
            <div class="prop-row">
              <span>名称</span>
              <strong>{{ activeCabinetJson.cabinet?.name || '未命名柜体' }}</strong>
            </div>
            <div class="prop-row">
              <span>尺寸</span>
              <strong>
                {{ activeCabinetJson.cabinet?.width }} × {{ activeCabinetJson.cabinet?.height }} × {{ activeCabinetJson.cabinet?.depth }}mm
              </strong>
            </div>
            <div class="prop-row">
              <span>板件</span>
              <strong>{{ activeDraftBoards.length }} 块</strong>
            </div>
          </div>
          <div v-else class="empty-props">暂无柜体</div>
        </section>

        <el-divider />

        <section class="props-section">
          <h4 class="props-title">选中板件</h4>
          <div v-if="selectedDraftBoard" class="board-editor">
            <div class="board-tools">
              <el-tooltip content="复制板件" placement="top">
                <el-button size="small" :icon="CopyDocument" circle @click="onCopySelectedBoard" />
              </el-tooltip>
              <el-tooltip content="吸附对齐" placement="top">
                <el-button size="small" :icon="Position" circle @click="onSnapSelectedBoard" />
              </el-tooltip>
              <el-tooltip content="删除板件" placement="top">
                <el-button size="small" type="danger" :icon="Delete" circle @click="onDeleteSelectedBoard" />
              </el-tooltip>
            </div>

            <div class="editor-row">
              <span>名称</span>
              <el-input
                size="small"
                :model-value="selectedDraftBoard.displayName"
                @change="value => updateSelectedBoard({ displayName: value })"
              />
            </div>
            <div class="editor-row">
              <span>类型</span>
              <strong>{{ formatBoardType(selectedDraftBoard.type) }}</strong>
            </div>
            <div class="editor-grid">
              <label>
                <span>长</span>
                <el-input-number
                  size="small"
                  :model-value="selectedDraftBoard.designLength"
                  :min="1"
                  :step="10"
                  controls-position="right"
                  @change="value => updateSelectedSize('designLength', value)"
                />
              </label>
              <label>
                <span>宽</span>
                <el-input-number
                  size="small"
                  :model-value="selectedDraftBoard.designWidth"
                  :min="1"
                  :step="10"
                  controls-position="right"
                  @change="value => updateSelectedSize('designWidth', value)"
                />
              </label>
              <label>
                <span>厚</span>
                <el-input-number
                  size="small"
                  :model-value="selectedDraftBoard.thickness"
                  :min="1"
                  :step="1"
                  controls-position="right"
                  @change="value => updateSelectedSize('thickness', value)"
                />
              </label>
            </div>
            <div class="editor-grid">
              <label>
                <span>X</span>
                <el-input-number
                  size="small"
                  :model-value="selectedDraftBoard.position?.x || 0"
                  :step="moveStep"
                  controls-position="right"
                  @change="value => updateSelectedPosition('x', value)"
                />
              </label>
              <label>
                <span>Y</span>
                <el-input-number
                  size="small"
                  :model-value="selectedDraftBoard.position?.y || 0"
                  :step="moveStep"
                  controls-position="right"
                  @change="value => updateSelectedPosition('y', value)"
                />
              </label>
              <label>
                <span>Z</span>
                <el-input-number
                  size="small"
                  :model-value="selectedDraftBoard.position?.z || 0"
                  :step="moveStep"
                  controls-position="right"
                  @change="value => updateSelectedPosition('z', value)"
                />
              </label>
            </div>
            <div class="nudge-grid">
              <el-button size="small" @click="onNudgeSelectedBoard('x', -moveStep)">X-</el-button>
              <el-button size="small" @click="onNudgeSelectedBoard('x', moveStep)">X+</el-button>
              <el-button size="small" @click="onNudgeSelectedBoard('y', -moveStep)">Y-</el-button>
              <el-button size="small" @click="onNudgeSelectedBoard('y', moveStep)">Y+</el-button>
              <el-button size="small" @click="onNudgeSelectedBoard('z', -moveStep)">Z-</el-button>
              <el-button size="small" @click="onNudgeSelectedBoard('z', moveStep)">Z+</el-button>
            </div>
            <div class="editor-row">
              <span>纹理</span>
              <el-select
                size="small"
                :model-value="selectedDraftBoard.grain || 'none'"
                @update:model-value="value => updateSelectedBoard({ grain: value })"
              >
                <el-option
                  v-for="option in grainOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
            <div class="editor-row">
              <span>放置面</span>
              <el-select
                size="small"
                :model-value="selectedDraftBoard.placementFace || 'inner'"
                @update:model-value="value => updateSelectedBoard({ placementFace: value })"
              >
                <el-option
                  v-for="option in placementFaceOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
            <div class="edge-editor">
              <span>封边</span>
              <div class="edge-checks">
                <el-checkbox
                  v-for="edge in Object.keys(edgeLabels)"
                  :key="edge"
                  :model-value="Boolean(selectedDraftBoard.edgeBanding?.[edge])"
                  @change="value => updateSelectedEdge(edge, value)"
                >
                  {{ edgeLabels[edge] }}
                </el-checkbox>
              </div>
            </div>
          </div>
          <div v-else class="empty-props">未选中板件</div>
        </section>

        <el-divider />

        <section class="props-section">
          <div class="panel-title-row">
            <h4>板材映射</h4>
            <el-tag v-if="hasCabinet" size="small" :type="activeSlotsMapped ? 'success' : 'warning'">
              {{ activeSlotsMapped ? '已完成' : '待选择' }}
            </el-tag>
          </div>
          <div v-if="materialSlots.length" class="slot-status-list">
            <div v-for="slot in materialSlots" :key="slot" class="slot-status-row">
              <span>{{ formatMaterialSlot(slot) }}</span>
              <strong>{{ getBoardOptionById(activeSlotMap[slot])?.label || '未选择' }}</strong>
            </div>
          </div>
          <div v-else class="empty-props">暂无材料角色</div>
        </section>
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
      <p class="slot-hint">
        当前柜体：{{ activeCabinetJson?.cabinet?.name || '未命名柜体' }}。为每类材料角色选择实际板材，未完成映射时不能拆单。
      </p>
      <el-form v-if="hasCabinet" label-width="96px" size="small">
        <el-form-item v-for="slot in materialSlots" :key="slot" :label="formatMaterialSlot(slot)">
          <el-select
            :model-value="activeSlotMap[slot]"
            filterable
            placeholder="选择板材"
            style="width:100%"
            @update:model-value="value => setActiveSlotMapValue(slot, value)"
          >
            <el-option
              v-for="board in boardOptionsBySlot[slot]"
              :key="board.value"
              :label="board.label"
              :value="board.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSlotMap = false">取消</el-button>
        <el-button type="primary" :disabled="!activeSlotsMapped" @click="showSlotMap = false">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSplitPreview" title="统一拆单预览" width="860px" top="5vh">
      <div class="split-summary">
        共 {{ store.splitGroups.length }} 个柜体，{{ splitTotalCount }} 条订单明细，确认后将追加写入当前订单。
      </div>
      <el-table :data="store.splitItems" size="small" border stripe max-height="400">
        <el-table-column v-for="col in splitColumns" :key="col.prop" v-bind="col" />
      </el-table>
      <template #footer>
        <el-button @click="showSplitPreview = false">取消</el-button>
        <el-button type="primary" :loading="store.confirming" @click="onConfirmSplit">确认统一写入订单</el-button>
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

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.panel-title-row h4 {
  margin: 0;
}

.draft-list {
  display: grid;
  gap: 8px;
}

.draft-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.draft-item:hover,
.draft-item.active {
  border-color: #0f766e;
  background: #f0fdfa;
}

.draft-item:focus-visible {
  outline: 2px solid rgba(15, 118, 110, 0.35);
  outline-offset: 2px;
}

.draft-main {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.draft-name {
  font-size: 13px;
  font-weight: 700;
  color: #172033;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.draft-meta {
  font-size: 11px;
  color: #64748b;
}

.draft-actions {
  display: flex;
  flex-shrink: 0;
  gap: 2px;
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

.preset-tag {
  display: inline-block;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  background: #dbeafe;
  color: #1d4ed8;
  margin-top: 2px;
}

.preset-actions {
  display: flex;
  gap: 2px;
  margin-top: 4px;
  justify-content: center;
}

.preset-hint {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 16px;
  grid-column: 1 / -1;
}

.part-library {
  display: grid;
  gap: 8px;
}

.part-card {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 48px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px;
  background: #fff;
  color: #172033;
  cursor: grab;
  text-align: left;
  transition: border-color 0.2s, background 0.2s, transform 0.2s;
}

.part-card:hover:not(:disabled) {
  border-color: #0f766e;
  background: #f8fafc;
  transform: translateY(-1px);
}

.part-card:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.part-card .el-icon {
  flex: 0 0 auto;
  color: #0f766e;
}

.part-card span {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.part-card strong {
  font-size: 13px;
}

.part-card small {
  font-size: 11px;
  color: #64748b;
  overflow-wrap: anywhere;
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

.cd-right {
  width: 300px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 12px;
  overflow-y: auto;
}

.props-section {
  display: grid;
  gap: 10px;
}

.props-title {
  margin: 0;
  font-size: 14px;
  color: #172033;
}

.prop-list,
.slot-status-list {
  display: grid;
  gap: 8px;
}

.prop-row,
.slot-status-row {
  display: grid;
  gap: 3px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
}

.prop-row:last-child,
.slot-status-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.prop-row span,
.slot-status-row span {
  font-size: 12px;
  color: #94a3b8;
}

.prop-row strong,
.slot-status-row strong {
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: #172033;
  overflow-wrap: anywhere;
}

.empty-props {
  font-size: 12px;
  color: #94a3b8;
  padding: 12px 0;
}

.board-editor {
  display: grid;
  gap: 10px;
}

.board-tools {
  display: flex;
  gap: 8px;
}

.editor-row,
.edge-editor {
  display: grid;
  gap: 5px;
}

.editor-row > span,
.edge-editor > span,
.editor-grid label span {
  font-size: 12px;
  color: #94a3b8;
}

.editor-row strong {
  font-size: 13px;
  color: #172033;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.editor-grid label {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.editor-grid :deep(.el-input-number) {
  width: 100%;
}

.nudge-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.nudge-grid :deep(.el-button) {
  margin-left: 0;
}

.edge-checks {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 8px;
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

.split-summary {
  margin-bottom: 10px;
  font-size: 13px;
  color: #475569;
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

  .cd-left,
  .cd-right {
    width: 100%;
    max-height: none;
  }

  .cd-center {
    flex: 0 0 auto;
    min-height: 420px;
  }
}

@media (max-width: 520px) {
  .preset-cards {
    grid-template-columns: 1fr;
  }

  .editor-grid,
  .nudge-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
