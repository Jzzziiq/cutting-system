<script setup>
import { computed, shallowRef, watch } from 'vue';

const props = defineProps({
  board: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['preview']);

const imageFailed = shallowRef(false);
const textureUrl = computed(() => props.board?.textureUrl || '');
const hasTexture = computed(() => Boolean(textureUrl.value));
const imageAlt = computed(() => `${props.board?.brand || '板材'}纹理缩略图`);

watch(textureUrl, () => {
  imageFailed.value = false;
});

function openPreview() {
  if (!hasTexture.value || imageFailed.value) return;
  emit('preview', props.board);
}
</script>

<template>
  <button
    v-if="hasTexture"
    class="texture-thumb"
    type="button"
    :disabled="imageFailed"
    :title="imageFailed ? '纹理图片加载失败' : '查看纹理大图'"
    @click="openPreview"
  >
    <img
      v-if="!imageFailed"
      :src="textureUrl"
      :alt="imageAlt"
      loading="lazy"
      @error="imageFailed = true"
    />
    <span v-else>加载失败</span>
  </button>
  <span v-else class="texture-empty">暂无</span>
</template>

<style scoped>
.texture-thumb {
  width: 72px;
  height: 48px;
  padding: 0;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  cursor: zoom-in;
  overflow: hidden;
  font-size: 12px;
  font-weight: 700;
}

.texture-thumb:disabled {
  cursor: default;
  opacity: 0.72;
}

.texture-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.texture-empty {
  color: #94a3b8;
  font-size: 13px;
}
</style>
