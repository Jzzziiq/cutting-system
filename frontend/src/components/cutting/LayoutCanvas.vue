<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { useLayoutCanvas } from '@/composables/useLayoutCanvas';
import { Loading } from '@element-plus/icons-vue';

const props = defineProps({
  solutions: { type: Array, default: () => [] },
  kerfWidth: { type: Number, default: 3 },
  gapDistance: { type: Number, default: 3 },
  allowRotation: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
  orderInfo: { type: Object, default: () => ({}) }
});

const emit = defineEmits(['zoom-change']);

const canvasRef = ref(null);

const {
  kerfWidth: composableKerfWidth,
  gapDistance: composableGapDistance,
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
  gapDistance: props.gapDistance,
  allowRotation: props.allowRotation
});

// Sync props → composable refs so tool settings dialog changes take effect
watch(() => props.kerfWidth, (v) => { composableKerfWidth.value = v; });
watch(() => props.gapDistance, (v) => { composableGapDistance.value = v; });
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

const labelQrCells = Array.from({ length: 49 }, (_, index) => ({
  index,
  dark: index % 6 === 0 || index % 10 === 0 || [1, 2, 7, 14, 34, 41, 47, 48].includes(index)
}));

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
  const orderName = props.orderInfo.orderNo || props.orderInfo.orderName || `第 ${activeBoardIndex.value + 1} 张板`;
  const pieceName = piece.partName || piece.label || `工件 ${tooltipPieceIndex.value || ''}`.trim();
  const processName = props.orderInfo.processName || props.orderInfo.process || '排版切割';
  const coord = `X${formatNumber(piece.x)} Y${formatNumber(piece.y)}`;

  return {
    title: `${orderName}-${pieceName}`,
    pieceName,
    dimension: `${formatNumber(piece.l)}*${formatNumber(piece.w)}`,
    processName,
    material: tooltipBoardLabel.value,
    coord,
    code: `${orderName}-${activeBoardIndex.value + 1}-${tooltipPieceIndex.value || 0}`,
    page: `板${activeBoardIndex.value + 1}`
  };
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
        <div class="label-title">{{ tooltipLabelData.title }}</div>
        <div class="label-grid">
          <div class="label-left">
            <div class="label-part">{{ tooltipLabelData.pieceName }}</div>
            <div class="label-size">{{ tooltipLabelData.dimension }}</div>
            <div class="label-direction-row">
              <div class="label-direction-box"></div>
              <div class="label-qr">
                <i
                  v-for="cell in labelQrCells"
                  :key="cell.index"
                  :class="{ 'is-dark': cell.dark }"
                ></i>
              </div>
            </div>
          </div>
          <div class="label-right">
            <div class="label-process">工艺：{{ tooltipLabelData.processName }}</div>
            <div class="label-layout-mini">
              <span
                v-for="block in tooltipLayoutBlocks"
                :key="block.key"
                :class="{ active: block.active }"
                :style="block.style"
              ></span>
            </div>
            <div class="label-code">{{ tooltipLabelData.code }}</div>
          </div>
        </div>
        <div class="label-footer">
          <span>{{ tooltipLabelData.material }}</span>
          <span>坐标 {{ tooltipLabelData.coord }}</span>
        </div>
        <div class="label-page">{{ tooltipLabelData.page }}</div>
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
  min-height: 232px;
  padding: 12px 14px 14px;
  color: #111;
  background:
    linear-gradient(135deg, rgba(0, 0, 0, 0.045) 0 1px, transparent 1px 14px),
    #fbfbf7;
  border: 2px solid #111;
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.28);
  font-family: KaiTi, STKaiti, "Microsoft YaHei", sans-serif;
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

.label-title,
.label-grid,
.label-footer {
  position: relative;
  z-index: 1;
}

.label-title {
  max-width: 280px;
  margin-bottom: 8px;
  overflow: hidden;
  color: #111;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.08;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-grid {
  display: grid;
  grid-template-columns: 142px minmax(0, 1fr);
  gap: 12px;
}

.label-part {
  overflow: hidden;
  font-size: 21px;
  font-weight: 900;
  line-height: 1.08;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-size {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 900;
  line-height: 1;
}

.label-direction-row {
  display: grid;
  grid-template-columns: 60px 60px;
  gap: 13px;
  align-items: end;
  margin-top: 17px;
}

.label-direction-box {
  position: relative;
  width: 60px;
  height: 52px;
  border: 3px solid #111;
  background: #fff;
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

.label-qr {
  display: grid;
  width: 60px;
  height: 60px;
  padding: 4px;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: repeat(7, 1fr);
  background: #fff;
  border: 3px solid #111;
}

.label-qr i {
  display: block;
  background: transparent;
}

.label-qr i.is-dark {
  background: #111;
}

.label-process {
  margin-top: 4px;
  overflow: hidden;
  font-size: 19px;
  font-weight: 900;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-layout-mini {
  position: relative;
  height: 76px;
  margin-top: 9px;
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

.label-code {
  margin-top: 7px;
  overflow: hidden;
  font-family: "Courier New", Consolas, monospace;
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  padding-right: 62px;
  font-size: 17px;
  font-weight: 900;
  line-height: 1.08;
}

.label-footer span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.label-page {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 1;
  width: 58px;
  height: 54px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 7px;
  color: #fff;
  font-size: 15px;
  font-weight: 900;
  clip-path: polygon(50% 0, 100% 100%, 0 100%);
  background: #111;
}

.spinner {
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
