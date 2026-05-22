<script setup>
import { ref } from 'vue';
import { Delete, Plus, ArrowDown, ArrowRight } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import { boardLabel, dimLabel } from '@/utils/boardLabel';

const props = defineProps({
  boardGroups: { type: Array, required: true },
  columns: { type: Array, required: true },
  getGroupStats: { type: Function, required: true }
});

const emit = defineEmits([
  'add-item',
  'delete-item',
  'remove-group',
  'paste',
  'keydown'
]);

const expandedGroups = ref({});

function isExpanded(groupId) {
  return expandedGroups.value[groupId] !== false;
}

function toggleGroup(groupId) {
  expandedGroups.value[groupId] = !isExpanded(groupId);
}

function getCellClass(item, colKey) {
  return item._validation && item._validation[colKey] ? 'cell-error' : '';
}

async function onRemoveGroup(group) {
  const stats = props.getGroupStats(group);
  if (stats.itemCount > 0) {
    try {
      await ElMessageBox.confirm(
        `板材"${boardLabel(group.board)}"下有 ${stats.itemCount} 个工件，确认删除？`,
        '确认删除',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
      );
    } catch {
      return;
    }
  }
  emit('remove-group', group.id);
}

function onPaste(event, groupId, rowIdx, colIdx) {
  emit('paste', groupId, event, rowIdx, colIdx);
}

function onKeydown(event, groupId, rowIdx, colIdx) {
  emit('keydown', groupId, event, rowIdx, colIdx);
}
</script>

<template>
  <div class="board-groups-wrap panel-container">
    <div class="panel-header">
      <span>工件录入（按板材分组）</span>
      <span v-if="boardGroups.length" class="group-summary">
        {{ boardGroups.length }} 组板材 · {{ boardGroups.reduce((s, g) => s + g.items.length, 0) }} 行工件
      </span>
    </div>
    <div class="panel-body groups-body">
      <div v-if="!boardGroups.length" class="empty-hint">
        请先在左侧搜索并添加板材，再录入工件尺寸
      </div>

      <div
        v-for="group in boardGroups"
        :key="group.id"
        class="board-group-table"
        :data-group="group.id"
      >
        <!-- Group header -->
        <div class="group-header" @click="toggleGroup(group.id)">
          <el-icon class="group-toggle" :size="14">
            <ArrowRight v-if="!isExpanded(group.id)" />
            <ArrowDown v-else />
          </el-icon>
          <div class="group-board-info">
            <span class="group-board-name">{{ boardLabel(group.board) }}</span>
            <span class="group-board-dims">{{ dimLabel(group.board) }}</span>
          </div>
          <div class="group-stats">
            <span class="group-stat">工件 {{ getGroupStats(group).itemCount }}</span>
            <span class="group-stat">面积 {{ (getGroupStats(group).area / 1000000).toFixed(2) }} m²</span>
            <span v-if="getGroupStats(group).errors" class="group-stat group-errors">
              错误 {{ getGroupStats(group).errors }}
            </span>
          </div>
          <el-button
            size="small"
            type="danger"
            :icon="Delete"
            circle
            @click.stop="onRemoveGroup(group)"
          />
        </div>

        <!-- Group body -->
        <div v-show="isExpanded(group.id)" class="group-body">
          <el-table
            :data="group.items"
            size="small"
            border
            stripe
            class="group-items-table"
            max-height="400"
          >
            <el-table-column
              v-for="(col, colIdx) in columns"
              :key="col.key"
              :prop="col.key"
              :label="col.label"
              :min-width="col.width"
            >
              <template #default="{ row, $index: rowIdx }">
                <div
                  :class="['cell-inner', getCellClass(row, col.key)]"
                  :data-row="rowIdx"
                  :data-col="colIdx"
                  @keydown="onKeydown($event, group.id, rowIdx, colIdx)"
                >
                  <template v-if="col.type === 'number'">
                    <input
                      v-model.number="row[col.key]"
                      type="number"
                      class="cell-input"
                      @paste="onPaste($event, group.id, rowIdx, colIdx)"
                    />
                  </template>
                  <template v-else>
                    <input
                      v-model="row[col.key]"
                      type="text"
                      class="cell-input"
                      @paste="onPaste($event, group.id, rowIdx, colIdx)"
                    />
                  </template>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="60" fixed="right">
              <template #default="{ $index: rowIdx }">
                <el-button
                  size="small"
                  type="danger"
                  :icon="Delete"
                  circle
                  @click="emit('delete-item', group.id, rowIdx)"
                />
              </template>
            </el-table-column>
          </el-table>

          <div class="group-add-row">
            <el-button size="small" :icon="Plus" @click="emit('add-item', group.id)">
              添加工件行
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.board-groups-wrap {
  min-width: 0;
  flex: 1;
}

.groups-body {
  padding: 8px;
  overflow-y: auto;
}

.group-summary {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.empty-hint {
  font-size: 13px;
  color: #94a3b8;
  padding: 24px 0;
  text-align: center;
}

.board-group-table {
  margin-bottom: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f8fafc;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid #e2e8f0;
}

.group-header:hover {
  background: #f1f5f9;
}

.group-toggle {
  color: #64748b;
  flex-shrink: 0;
}

.group-board-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}

.group-board-name {
  font-size: 14px;
  font-weight: 700;
  color: #172033;
}

.group-board-dims {
  font-size: 12px;
  color: #64748b;
}

.group-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
  flex-shrink: 0;
}

.group-stat {
  white-space: nowrap;
}

.group-errors {
  color: #dc2626;
  font-weight: 700;
}

.group-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.group-items-table {
  min-width: 820px;
}

.group-items-table .cell {
  padding: 0 !important;
}

.group-add-row {
  padding: 6px 10px;
  border-top: 1px solid #e2e8f0;
  background: #fafafa;
}

/* Shared cell styles (mirrored from CuttingTable) */
.cell-inner {
  width: 100%;
  height: 100%;
  min-width: 0;
}
.cell-input {
  width: 100%;
  min-height: 34px;
  padding: 5px 9px;
  border: 2px solid transparent;
  border-radius: 2px;
  background: transparent;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}
.cell-input:focus {
  border-color: #0f766e;
  background: #f0fdfa;
}
.cell-error .cell-input {
  border-color: #dc2626;
  background: #fef2f2;
}
</style>
