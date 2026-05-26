<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { useLayoutCanvas } from '@/composables/useLayoutCanvas';
import { Loading } from '@element-plus/icons-vue';

const props = defineProps({
  solutions: { type: Array, default: () => [] },
  kerfWidth: { type: Number, default: 3 },
  allowRotation: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
  orderInfo: { type: Object, default: () => ({}) }
});

const emit = defineEmits(['zoom-change']);

const canvasRef = ref(null);

const {
  kerfWidth: composableKerfWidth,
  allowRotation: composableAllowRotation,
  zoom,
  boardCount,
  activeBoardIndex,
  currentSolution,
  summary,
  showTooltip,
  tooltipPos,
  tooltipData,
  initCanvas,
  fitToScreen: fitCanvasToScreen,
  onResize,
  onWheel,
  onMouseDown,
  onMouseMove,
  onMouseUp,
  onMouseLeave,
  switchBoard,
  loadSolutions,
  exportSVG,
  drawFrame
} = useLayoutCanvas(canvasRef, {
  kerfWidth: props.kerfWidth,
  allowRotation: props.allowRotation
});

// Sync props → composable refs so tool settings dialog changes take effect
watch(() => props.kerfWidth, (v) => { composableKerfWidth.value = v; });
watch(() => props.allowRotation, (v) => { composableAllowRotation.value = v; });
watch(zoom, (value) => { emit('zoom-change', value); }, { immediate: true });

watch(() => props.solutions, (val) => {
  if (val && val.length) loadSolutions(val);
}, { immediate: true });

onMounted(() => {
  initCanvas();
  window.addEventListener('resize', onResize);
  if (props.solutions?.length) {
    loadSolutions(props.solutions);
    fitCanvasToScreen();
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
});

function zoomIn() { zoom.value = Math.min(5, zoom.value * 1.3); drawFrame(); }
function zoomOut() { zoom.value = Math.max(0.1, zoom.value / 1.3); drawFrame(); }
function fitToScreen() {
  initCanvas();
  fitCanvasToScreen();
  drawFrame();
}

defineExpose({ zoomIn, zoomOut, fitToScreen, exportSVG });

function formatDim(solution) {
  if (!solution) return '-';
  const L = solution.containerLength || solution.instance?.L || '-';
  const W = solution.containerWidth || solution.instance?.W || '-';
  return `${L}×${W}mm`;
}

function boardGroupLabel(solution) {
  if (!solution?._boardGroup) return '';
  const bg = solution._boardGroup;
  return [bg.brand, bg.materialType, bg.color].filter(Boolean).join(' ');
}

function formatNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num.toFixed(0) : '-';
}

function getContainerSize(solution) {
  return {
    L: solution?.containerLength || solution?.instance?.L || 0,
    W: solution?.containerWidth || solution?.instance?.W || 0
  };
}

const tooltipPieceIndex = computed(() => {
  const pieces = currentSolution.value?.placeSquareList || [];
  const index = pieces.indexOf(tooltipData.value);
  return index >= 0 ? index + 1 : null;
});

const tooltipBoardLabel = computed(() => (
  boardGroupLabel(currentSolution.value)
  || props.orderInfo.boardGroupLabels?.[activeBoardIndex.value]
  || '当前板材'
));

const tooltipLayoutBlocks = computed(() => {
  const solution = currentSolution.value;
  const pieces = solution?.placeSquareList || [];
  const { L, W } = getContainerSize(solution);
  if (!L || !W) return [];

  return pieces.map((piece, index) => ({
    key: `${index}-${piece.x}-${piece.y}`,
    active: piece === tooltipData.value,
    style: {
      left: `${Math.max(0, (Number(piece.x || 0) / L) * 100)}%`,
      bottom: `${Math.max(0, (Number(piece.y || 0) / W) * 100)}%`,
      width: `${Math.max(1, (Number(piece.l || 0) / L) * 100)}%`,
      height: `${Math.max(1, (Number(piece.w || 0) / W) * 100)}%`
    }
  }));
});

const tooltipLabelData = computed(() => {
  const piece = tooltipData.value;
  if (!piece) return null;
  const l = formatNumber(piece.l);
  const w = formatNumber(piece.w);
  const t = piece.thickness ?? currentSolution.value?._boardGroup?.thickness ?? '—';
  const coord = `X${formatNumber(piece.x)} Y${formatNumber(piece.y)}`;
  const eb = piece.edgeBanding || {};

  return {
    customer: props.orderInfo.customer || '—',
    cabinetName: piece.cabinetName || extractCabinetName(piece.partName) || '—',
    partName: piece.partName || '',
    dimension: `${l}×${w}×${t}mm`,
    material: tooltipBoardLabel.value,
    edgeTop: eb.top ? 1 : 0,
    edgeBottom: eb.bottom ? 1 : 0,
    edgeLeft: eb.left ? 1 : 0,
    edgeRight: eb.right ? 1 : 0,
    grainDirection: piece.grainDirection || 'none',
    coord,
    timestamp: props.orderInfo.layoutCompletedAt || '—'
  };
});

function extractCabinetName(partName) {
  if (!partName) return '';
  const idx = partName.indexOf('-');
  return idx > 0 ? partName.substring(0, idx) : '';
}

const miniMapStyle = computed(() => {
  const { L, W } = getContainerSize(currentSolution.value);
  if (!L || !W) return {};
  return { aspectRatio: `${L} / ${W}` };
});
</script>

<template>
  <div class="canvas-area">
    <!-- Summary bar -->
    <div class="summary-bar">
      <div class="summary-item">
        <span class="label">订单：</span>
        <span class="value">{{ orderInfo.orderName || '新排版' }}</span>
      </div>
      <div class="summary-item">
        <span class="label">总工件：</span>
        <span class="value">{{ summary.totalPieces }}</span>
      </div>
      <div class="summary-item">
        <span class="label">原板：</span>
        <span class="value" style="color:#0f766e">{{ summary.boardsUsed }} 张</span>
      </div>
      <div class="summary-item">
        <span class="label">余料板：</span>
        <span class="value" style="color:#d97706">{{ summary.offcutsUsed }} 张</span>
      </div>
      <div class="summary-item">
        <span class="label">总利用率：</span>
        <span class="value" style="color:#0f766e;font-size:14px">
          {{ (summary.overallRate * 100).toFixed(1) }}%
        </span>
      </div>
      <div class="summary-item">
        <span class="label">锯路：</span>
        <span class="value">{{ kerfWidth }}mm</span>
      </div>
      <div class="summary-item">
        <span class="label">当前板：</span>
        <span class="value">{{ formatDim(currentSolution) }}</span>
      </div>
      <div v-if="orderInfo.boardGroupLabels?.length" class="summary-item" style="flex-basis:100%">
        <span class="label">板材组：</span>
        <span class="value" style="font-size:12px">
          <el-tag
            v-for="(label, i) in orderInfo.boardGroupLabels"
            :key="i"
            size="small"
            effect="plain"
            style="margin-right:4px;margin-bottom:2px"
          >{{ label }}</el-tag>
        </span>
      </div>
    </div>

    <!-- Board tabs -->
    <div v-if="boardCount > 1" class="board-tabs">
      <button
        v-for="(_, idx) in solutions"
        :key="idx"
        class="board-tab"
        :class="{ active: idx === activeBoardIndex }"
        @click="switchBoard(idx)"
      >
        <template v-if="solutions[idx]?._boardGroup">
          {{ boardGroupLabel(solutions[idx]) }}
        </template>
        <template v-else-if="solutions[idx] && solutions[idx].rate != null && solutions[idx].rate < 0.3">
          余料板 #{{ idx + 1 }}
        </template>
        <template v-else>
          第 {{ idx + 1 }} 张板
        </template>
        <span class="tab-rate" v-if="solutions[idx]">
          {{ (solutions[idx].rate * 100).toFixed(1) }}%
        </span>
      </button>
    </div>

    <!-- Canvas -->
    <div class="canvas-container">
      <canvas
        ref="canvasRef"
        class="layout-canvas"
        @wheel.prevent="onWheel"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseLeave"
      />

      <!-- Loading overlay -->
      <div v-if="loading" class="canvas-overlay">
        <el-icon class="spinner" :size="28"><Loading /></el-icon>
        <span style="margin-left:8px">排版计算中...</span>
      </div>

      <!-- Empty overlay -->
      <div v-if="!loading && !solutions.length" class="canvas-overlay">
        <span>请选择历史排版记录或点击"开始排版"</span>
      </div>

      <!-- Hover tooltip -->
      <div
        v-if="showTooltip && tooltipData"
        class="layout-label-tooltip"
        :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px' }"
      >
        <div class="label-info">
          <div class="label-head">
            <span class="label-title">{{ tooltipLabelData.customer }}</span>
            <span class="label-timestamp">{{ tooltipLabelData.timestamp }}</span>
          </div>
          <div v-if="tooltipLabelData.partName" class="label-part">{{ tooltipLabelData.cabinetName }}-{{ tooltipLabelData.partName }}</div>
          <div class="label-spec-row">
            <span class="label-dimension">{{ tooltipLabelData.dimension }}</span>
            <div class="label-direction-box">
              <span class="edge-tag edge-top">{{ tooltipLabelData.edgeTop }}</span>
              <span class="edge-tag edge-bottom">{{ tooltipLabelData.edgeBottom }}</span>
              <span class="edge-tag edge-left">{{ tooltipLabelData.edgeLeft }}</span>
              <span class="edge-tag edge-right">{{ tooltipLabelData.edgeRight }}</span>
            </div>
          </div>
          <div class="label-material">{{ tooltipLabelData.material }}</div>
        </div>
        <div class="label-map">
          <div class="label-layout-mini" :style="miniMapStyle">
            <span
              v-for="block in tooltipLayoutBlocks"
              :key="block.key"
              :class="{ active: block.active }"
              :style="block.style"
            ></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.canvas-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  overflow: hidden;
}

.summary-bar {
  display: flex;
  gap: 16px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 13px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.summary-item { display: flex; gap: 2px; align-items: center; }
.summary-item .label { color: #64748b; }
.summary-item .value { color: #172033; font-weight: 700; }

.board-tabs {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  flex-shrink: 0;
  padding-bottom: 2px;
}
.board-tab {
  padding: 6px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.board-tab:hover { border-color: #0f766e; }
.board-tab.active { background: #0f766e; color: #fff; border-color: #0f766e; }
.tab-rate { font-size: 12px; opacity: 0.7; }

.canvas-container {
  flex: 1;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  overflow: hidden;
  background: #f1f5f9;
  position: relative;
  cursor: grab;
  min-height: 300px;
}
.layout-canvas {
  width: 100%;
  height: 100%;
  display: block;
}
.canvas-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(241, 245, 249, 0.85);
  color: #64748b;
  font-size: 14px;
  z-index: 5;
  pointer-events: none;
}

.layout-label-tooltip {
  position: absolute;
  width: 372px;
  padding: 16px;
  color: #111;
  background:
    linear-gradient(135deg, rgba(0, 0, 0, 0.045) 0 1px, transparent 1px 14px),
    #fbfbf7;
  border: 2px solid #111;
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.28);
  font-family: SimSun, "宋体", "Microsoft YaHei", sans-serif;
  pointer-events: none;
  z-index: 100;
}

.layout-label-tooltip::before {
  content: "";
  position: absolute;
  inset: 6px;
  border: 1px solid rgba(17, 17, 17, 0.35);
  border-radius: 4px;
}

.label-info {
  position: relative;
  z-index: 1;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.label-title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.label-timestamp {
  font-size: 14px;
  font-weight: 700;
  color: #94a3b8;
  white-space: nowrap;
  flex-shrink: 0;
}

.label-part {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.label-spec-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.label-dimension {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.label-material {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.label-direction-box {
  position: relative;
  width: 60px;
  height: 52px;
  border: 3px solid #111;
  background: #fff;
  flex-shrink: 0;
}

.label-direction-box::before,
.label-direction-box::after {
  content: "";
  position: absolute;
  background: #111;
}

.label-direction-box::before {
  width: 30px;
  height: 4px;
  left: 14px;
  top: 24px;
}

.label-direction-box::after {
  width: 12px;
  height: 12px;
  left: 40px;
  top: 20px;
  clip-path: polygon(0 0, 100% 50%, 0 100%);
}

.edge-tag {
  position: absolute;
  font-size: 12px;
  font-weight: 900;
  color: #111;
  line-height: 1;
}
.edge-tag.edge-top {
  top: -15px;
  left: 50%;
  transform: translateX(-50%);
}
.edge-tag.edge-bottom {
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
}
.edge-tag.edge-left {
  left: -14px;
  top: 50%;
  transform: translateY(-50%);
}
.edge-tag.edge-right {
  right: -14px;
  top: 50%;
  transform: translateY(-50%);
}

.label-map {
  position: relative;
  z-index: 1;
}

.label-layout-mini {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #fff;
  border: 3px solid #111;
}

.label-layout-mini::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(90deg, rgba(17, 17, 17, 0.08) 1px, transparent 1px),
    linear-gradient(rgba(17, 17, 17, 0.08) 1px, transparent 1px);
  background-size: 14px 14px;
}

.label-layout-mini span {
  position: absolute;
  border: 2px solid #111;
  background: rgba(255, 255, 255, 0.94);
}

.label-layout-mini span.active {
  background: #111;
}

.spinner {
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
