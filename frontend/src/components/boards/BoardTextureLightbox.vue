<script setup>
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';

const props = defineProps({
  board: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['close']);

const infoVisible = shallowRef(false);
const imageFailed = shallowRef(false);
let hideTimer = null;

// 缩放/平移状态
const scale = ref(1);
const translateX = ref(0);
const translateY = ref(0);
const isDragging = ref(false);
let dragStartX = 0;
let dragStartY = 0;
let dragStartTx = 0;
let dragStartTy = 0;

const textureUrl = computed(() => props.board?.textureUrl || '');
const imageAlt = computed(() => `${props.board?.brand || '板材'}纹理大图`);
const dimensionText = computed(() => {
  if (!props.board) return '-';
  const { length, width, thickness } = props.board;
  if (!length || !width || !thickness) return '-';
  return `${length} × ${width} × ${thickness} mm`;
});

function clearHideTimer() {
  if (hideTimer) {
    window.clearTimeout(hideTimer);
    hideTimer = null;
  }
}

function revealInfo() {
  infoVisible.value = true;
  clearHideTimer();
  hideTimer = window.setTimeout(() => {
    infoVisible.value = false;
    hideTimer = null;
  }, 1500);
}

function resetTransform() {
  scale.value = 1;
  translateX.value = 0;
  translateY.value = 0;
}

function close() {
  clearHideTimer();
  resetTransform();
  emit('close');
}

// 滚轮缩放，以鼠标位置为中心
function onWheel(event) {
  const delta = event.deltaY > 0 ? -0.15 : 0.15;
  const newScale = Math.min(5, Math.max(0.5, scale.value + delta * scale.value));
  if (newScale === scale.value) return;

  const rect = event.currentTarget.getBoundingClientRect();
  const mouseX = event.clientX - rect.left;
  const mouseY = event.clientY - rect.top;
  const imgX = (mouseX - translateX.value) / scale.value;
  const imgY = (mouseY - translateY.value) / scale.value;

  scale.value = newScale;
  translateX.value = mouseX - imgX * newScale;
  translateY.value = mouseY - imgY * newScale;
}

// 拖拽平移
function onPointerDown(event) {
  if (scale.value <= 1) return;
  isDragging.value = true;
  dragStartX = event.clientX;
  dragStartY = event.clientY;
  dragStartTx = translateX.value;
  dragStartTy = translateY.value;
  event.currentTarget.setPointerCapture(event.pointerId);
}

function onPointerMove(event) {
  if (!isDragging.value) return;
  translateX.value = dragStartTx + (event.clientX - dragStartX);
  translateY.value = dragStartTy + (event.clientY - dragStartY);
}

function onPointerUp() {
  isDragging.value = false;
}

// 双击重置
function onDblClick() {
  resetTransform();
}

// ESC 键关闭
function onKeyDown(event) {
  if (event.key === 'Escape') {
    event.stopPropagation();
    close();
  }
}

// 计算图片样式
const imageStyle = computed(() => ({
  transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value})`,
  transformOrigin: '0 0',
  cursor: scale.value > 1 ? (isDragging.value ? 'grabbing' : 'grab') : 'zoom-in',
  transition: isDragging.value ? 'none' : 'transform 0.15s ease'
}));

watch(
  () => props.board,
  (board) => {
    clearHideTimer();
    imageFailed.value = false;
    resetTransform();
    infoVisible.value = Boolean(board);
    if (board) {
      revealInfo();
      window.addEventListener('keydown', onKeyDown);
    } else {
      window.removeEventListener('keydown', onKeyDown);
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  clearHideTimer();
  window.removeEventListener('keydown', onKeyDown);
});
</script>

<template>
  <div
    v-if="board"
    class="texture-lightbox"
    role="dialog"
    aria-modal="true"
    aria-label="板材纹理预览"
    @click.self="close"
    @mousemove="revealInfo"
  >
    <button class="lightbox-close" type="button" aria-label="关闭纹理预览" @click="close">×</button>
    <figure class="lightbox-content">
      <div class="image-stage" @wheel.prevent="onWheel">
        <img
          v-if="!imageFailed"
          :src="textureUrl"
          :alt="imageAlt"
          :style="imageStyle"
          @error="imageFailed = true"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @pointerup="onPointerUp"
          @dblclick="onDblClick"
        />
        <div v-else class="image-fallback">纹理图片加载失败</div>
      </div>
      <figcaption class="texture-info" :class="{ visible: infoVisible }">
        <strong>{{ board.brand || '未命名板材' }}</strong>
        <span>材质：{{ board.materialType || '-' }}</span>
        <span>颜色：{{ board.color || '-' }}</span>
        <span>规格：{{ board.sizeType || '-' }}</span>
        <span>尺寸：{{ dimensionText }}</span>
      </figcaption>
    </figure>
  </div>
</template>

<style scoped>
.texture-lightbox {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 32px;
  background: rgba(15, 23, 42, 0.78);
}

.lightbox-close {
  position: fixed;
  top: 24px;
  right: 24px;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(226, 232, 240, 0.72);
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.42);
  color: #ffffff;
  cursor: pointer;
  font-size: 24px;
  line-height: 1;
}

.lightbox-content {
  display: grid;
  gap: 14px;
  width: min(960px, 100%);
  max-height: calc(100vh - 64px);
  margin: 0;
}

.image-stage {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 260px;
  max-height: calc(100vh - 190px);
  border-radius: 8px;
  background: #0f172a;
  overflow: hidden;
}

.image-stage img {
  display: block;
  max-width: 100%;
  max-height: calc(100vh - 190px);
  object-fit: contain;
  user-select: none;
  -webkit-user-drag: none;
}

.image-fallback {
  display: grid;
  place-items: center;
  width: min(520px, 100%);
  min-height: 260px;
  color: #cbd5e1;
  font-weight: 700;
}

.texture-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
  min-height: 50px;
  padding: 11px 16px;
  border: 1px solid rgba(226, 232, 240, 0.18);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.82);
  color: #e2e8f0;
  opacity: 0;
  transform: translateY(8px);
  transition: opacity 0.22s ease, transform 0.22s ease;
  pointer-events: none;
}

.texture-info.visible {
  opacity: 1;
  transform: translateY(0);
}

.texture-info strong {
  color: #ffffff;
}

.texture-info span {
  margin: 0;
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 700px) {
  .texture-lightbox {
    padding: 18px;
  }

  .lightbox-close {
    top: 14px;
    right: 14px;
  }

  .texture-info {
    justify-content: flex-start;
  }
}
</style>
