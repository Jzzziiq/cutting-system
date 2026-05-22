<script setup>
import { Plus, CopyDocument, Delete, Edit, Rank } from '@element-plus/icons-vue';
import { freeBoardParts, categoryLabels } from '@/constants/cabinet';

defineProps({
  drafts: { type: Array, default: () => [] },
  activeCabinetId: { type: String, default: null },
  presets: { type: Array, default: () => [] },
  activeCabinetJson: { type: Object, default: null },
  loading: { type: Boolean, default: false }
});

const emit = defineEmits([
  'select-draft',
  'copy-draft',
  'remove-draft',
  'new-cabinet',
  'preset-click',
  'edit-template',
  'delete-template',
  'drag-start',
  'drag-end',
  'dblclick-part'
]);

function getCategoryLabel(category) {
  return categoryLabels[category] || category;
}
</script>

<template>
  <div class="cd-left">
    <div class="panel-title-row">
      <h4>订单柜体</h4>
      <el-tag size="small" type="info">{{ drafts.length }} 个</el-tag>
    </div>
    <div v-if="drafts.length" class="draft-list">
      <div
        v-for="draft in drafts"
        :key="draft.clientCabinetId"
        class="draft-item"
        :class="{ active: draft.clientCabinetId === activeCabinetId }"
        role="button"
        tabindex="0"
        @click="emit('select-draft', draft.clientCabinetId)"
        @keydown.enter="emit('select-draft', draft.clientCabinetId)"
        @keydown.space.prevent="emit('select-draft', draft.clientCabinetId)"
      >
        <span class="draft-main">
          <span class="draft-name">{{ draft.cabinetJson?.cabinet?.name || '未命名柜体' }}</span>
          <span class="draft-meta">
            {{ draft.cabinetJson?.cabinet?.width }} × {{ draft.cabinetJson?.cabinet?.height }} × {{ draft.cabinetJson?.cabinet?.depth }}mm
          </span>
        </span>
        <span class="draft-actions" @click.stop>
          <el-button size="small" text :icon="CopyDocument" @click="emit('copy-draft', draft)" />
          <el-button size="small" text type="danger" :icon="Delete" @click="emit('remove-draft', draft)" />
        </span>
      </div>
    </div>
    <div v-else class="preset-hint">
      一个订单可先维护多个柜体，全部完成后再统一拆单。
    </div>

    <el-divider />

    <div class="panel-title-row">
      <h4>新增柜体</h4>
      <el-button size="small" type="primary" plain :icon="Plus" @click="emit('new-cabinet')">空柜体</el-button>
    </div>
    <div class="preset-cards">
      <button
        v-for="preset in presets"
        :key="preset.id"
        class="preset-card"
        type="button"
        @click="emit('preset-click', preset)"
      >
        <span class="preset-icon">{{ preset.category === 'wardrobe' ? '衣' : '柜' }}</span>
        <span class="preset-name">{{ preset.name }}</span>
        <span class="preset-cat">{{ getCategoryLabel(preset.category) }}</span>
        <span v-if="preset.isOfficial !== 1" class="preset-tag">我的</span>
        <span
          v-if="preset.isOfficial !== 1"
          class="preset-actions"
          @click.stop
        >
          <el-button size="small" :icon="Edit" text @click="emit('edit-template', preset)" />
          <el-button size="small" :icon="Delete" text type="danger" @click="emit('delete-template', preset)" />
        </span>
      </button>
      <div v-if="!loading && presets.length === 0" class="preset-hint">
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
        @dragstart="emit('drag-start', $event, part)"
        @dragend="emit('drag-end')"
        @dblclick="emit('dblclick-part', part.type)"
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
</template>

<style scoped>
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

@media (max-width: 520px) {
  .preset-cards {
    grid-template-columns: 1fr;
  }
}
</style>
