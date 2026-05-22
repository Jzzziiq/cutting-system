<script setup>
import { CopyDocument, Delete, Position } from '@element-plus/icons-vue';
import {
  boardTypeLabels,
  grainOptions,
  placementFaceOptions,
  edgeLabels,
  sceneDragModeOptions
} from '@/constants/cabinet';

defineProps({
  activeCabinetJson: { type: Object, default: null },
  activeDraftBoards: { type: Array, default: () => [] },
  selectedBoard: { type: Object, default: null },
  materialSlots: { type: Array, default: () => [] },
  activeSlotMap: { type: Object, default: () => ({}) },
  activeSlotsMapped: { type: Boolean, default: false },
  sceneDragMode: { type: String, default: 'xz' },
  moveStep: { type: Number, default: 50 },
  formatMaterialSlot: { type: Function, required: true },
  getBoardOptionById: { type: Function, required: true }
});

const emit = defineEmits([
  'update:board',
  'update:position',
  'update:size',
  'update:edge',
  'update:grain',
  'update:placementFace',
  'update:displayName',
  'update:sceneDragMode',
  'copy-board',
  'delete-board',
  'snap-board',
  'nudge-board'
]);

function categoryLabel(category) {
  const labels = { wardrobe: '衣柜', 'base-cabinet': '地柜' };
  return labels[category] || category;
}

function formatBoardType(type) {
  return boardTypeLabels[type] || type || '-';
}
</script>

<template>
  <div class="cd-right">
    <section class="props-section">
      <div class="panel-title-row">
        <h4>当前柜体</h4>
        <el-tag v-if="activeCabinetJson" size="small" type="info">
          {{ categoryLabel(activeCabinetJson.cabinet?.category) }}
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
      <div v-if="selectedBoard" class="board-editor">
        <div class="board-tools">
          <el-tooltip content="复制板件" placement="top">
            <el-button size="small" :icon="CopyDocument" circle @click="emit('copy-board')" />
          </el-tooltip>
          <el-tooltip content="吸附对齐" placement="top">
            <el-button size="small" :icon="Position" circle @click="emit('snap-board')" />
          </el-tooltip>
          <el-tooltip content="删除板件" placement="top">
            <el-button size="small" type="danger" :icon="Delete" circle @click="emit('delete-board')" />
          </el-tooltip>
        </div>

        <div class="editor-row">
          <span>名称</span>
          <el-input
            size="small"
            :model-value="selectedBoard.displayName"
            @change="value => emit('update:displayName', value)"
          />
        </div>
        <div class="editor-row">
          <span>类型</span>
          <strong>{{ formatBoardType(selectedBoard.type) }}</strong>
        </div>
        <div class="editor-grid">
          <label>
            <span>长</span>
            <el-input-number
              size="small"
              :model-value="selectedBoard.designLength"
              :min="1"
              :step="10"
              controls-position="right"
              @change="value => emit('update:size', 'designLength', value)"
            />
          </label>
          <label>
            <span>宽</span>
            <el-input-number
              size="small"
              :model-value="selectedBoard.designWidth"
              :min="1"
              :step="10"
              controls-position="right"
              @change="value => emit('update:size', 'designWidth', value)"
            />
          </label>
          <label>
            <span>厚</span>
            <el-input-number
              size="small"
              :model-value="selectedBoard.thickness"
              :min="1"
              :step="1"
              controls-position="right"
              @change="value => emit('update:size', 'thickness', value)"
            />
          </label>
        </div>
        <div class="editor-grid">
          <label>
            <span>X</span>
            <el-input-number
              size="small"
              :model-value="selectedBoard.position?.x || 0"
              :step="moveStep"
              controls-position="right"
              @change="value => emit('update:position', 'x', value)"
            />
          </label>
          <label>
            <span>Y</span>
            <el-input-number
              size="small"
              :model-value="selectedBoard.position?.y || 0"
              :step="moveStep"
              controls-position="right"
              @change="value => emit('update:position', 'y', value)"
            />
          </label>
          <label>
            <span>Z</span>
            <el-input-number
              size="small"
              :model-value="selectedBoard.position?.z || 0"
              :step="moveStep"
              controls-position="right"
              @change="value => emit('update:position', 'z', value)"
            />
          </label>
        </div>
        <div class="nudge-grid">
          <el-button size="small" @click="emit('nudge-board', 'x', -moveStep)">X-</el-button>
          <el-button size="small" @click="emit('nudge-board', 'x', moveStep)">X+</el-button>
          <el-button size="small" @click="emit('nudge-board', 'y', -moveStep)">Y-</el-button>
          <el-button size="small" @click="emit('nudge-board', 'y', moveStep)">Y+</el-button>
          <el-button size="small" @click="emit('nudge-board', 'z', -moveStep)">Z-</el-button>
          <el-button size="small" @click="emit('nudge-board', 'z', moveStep)">Z+</el-button>
        </div>
        <div class="editor-row">
          <span>拖动约束</span>
          <el-segmented
            :model-value="sceneDragMode"
            size="small"
            :options="sceneDragModeOptions"
            block
            @update:model-value="value => emit('update:sceneDragMode', value)"
          />
        </div>
        <div class="editor-row">
          <span>纹理</span>
          <el-select
            size="small"
            :model-value="selectedBoard.grain || 'none'"
            @update:model-value="value => emit('update:grain', value)"
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
            :model-value="selectedBoard.placementFace || 'inner'"
            @update:model-value="value => emit('update:placementFace', value)"
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
              :model-value="Boolean(selectedBoard.edgeBanding?.[edge])"
              @change="value => emit('update:edge', edge, value)"
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
        <el-tag v-if="activeCabinetJson" size="small" :type="activeSlotsMapped ? 'success' : 'warning'">
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
</template>

<style scoped>
.cd-right {
  width: 300px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  padding: 12px;
  overflow-y: auto;
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

@media (max-width: 980px) {
  .cd-right {
    width: 100%;
    max-height: none;
  }

  .editor-grid,
  .nudge-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
