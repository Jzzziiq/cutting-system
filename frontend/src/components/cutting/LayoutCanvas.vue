<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
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
  fitToScreen,
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

watch(() => props.solutions, (val) => {
  if (val && val.length) loadSolutions(val);
}, { immediate: true });

onMounted(() => {
  initCanvas();
  window.addEventListener('resize', onResize);
  if (props.solutions?.length) {
    loadSolutions(props.solutions);
    fitToScreen();
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
});

function zoomIn() { zoom.value = Math.min(5, zoom.value * 1.3); drawFrame(); }
function zoomOut() { zoom.value = Math.max(0.1, zoom.value / 1.3); drawFrame(); }

defineExpose({ zoomIn, zoomOut, fitToScreen, exportSVG });

function formatDim(solution) {
  if (!solution) return '-';
  const L = solution.containerLength || solution.instance?.L || '-';
  const W = solution.containerWidth || solution.instance?.W || '-';
  return `${L}×${W}mm`;
}
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
        <template v-if="solutions[idx] && solutions[idx].rate != null && solutions[idx].rate < 0.3">
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
        class="layout-tooltip"
        :style="{ left: tooltipPos.x + 'px', top: tooltipPos.y + 'px' }"
      >
        <div class="tt-row"><strong>尺寸：</strong>{{ tooltipData.l?.toFixed(0) }} × {{ tooltipData.w?.toFixed(0) }} mm</div>
        <div class="tt-row"><strong>坐标：</strong>({{ tooltipData.x?.toFixed(1) }}, {{ tooltipData.y?.toFixed(1) }})</div>
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

.layout-tooltip {
  position: absolute;
  padding: 8px 12px;
  background: #1e293b;
  color: #f1f5f9;
  border-radius: 6px;
  font-size: 13px;
  pointer-events: none;
  z-index: 100;
  line-height: 1.6;
  max-width: 240px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
}
.tt-row { white-space: nowrap; }
.tt-row strong { color: #fbbf24; }

.spinner {
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
