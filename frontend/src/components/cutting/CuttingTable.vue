<script setup>
import { Delete, Plus } from '@element-plus/icons-vue';

const props = defineProps({
  rows: { type: Array, required: true },
  columns: { type: Array, required: true },
  boardOptions: { type: Array, default: () => [] },
  focusedCell: { type: Object, default: () => ({ row: 0, col: 0 }) }
});

const emit = defineEmits([
  'add-row',
  'delete-row',
  'paste',
  'keydown',
  'focus-cell'
]);

function onCellClick(rowIdx, colIdx) {
  emit('focus-cell', { row: rowIdx, col: colIdx });
}

function onPaste(event, rowIdx, colIdx) {
  emit('paste', event, rowIdx, colIdx);
}

function onKeydown(event, rowIdx, colIdx) {
  emit('keydown', event, rowIdx, colIdx);
}

function boardLabel(boardId) {
  if (!boardId) return '';
  const b = props.boardOptions.find(x => x.boardId === boardId);
  if (!b) return '';
  return [b.brand, b.materialType, b.color, b.sizeType].filter(Boolean).join(' ');
}

function getCellClass(row, colKey) {
  return row._validation && row._validation[colKey] ? 'cell-error' : '';
}
</script>

<template>
  <div class="cutting-table-wrap panel-container">
    <div class="panel-header">
      <span>下料尺寸输入</span>
      <el-button size="small" :icon="Plus" @click="$emit('add-row', rows.length - 1)">添加行</el-button>
    </div>
    <div class="panel-body cutting-table-body">
      <el-table
        :data="rows"
        size="small"
        border
        stripe
        class="cutting-table"
        row-class-name="cutting-row"
        height="100%"
      >
        <el-table-column
          v-for="(col, colIdx) in columns"
          :key="col.key"
          :prop="col.key"
          :label="col.label"
          :width="col.width"
        >
          <template #default="{ row, $index: rowIdx }">
            <div
              :class="['cell-inner', getCellClass(row, col.key)]"
              :data-row="rowIdx"
              :data-col="colIdx"
              @click="onCellClick(rowIdx, colIdx)"
              @keydown="onKeydown($event, rowIdx, colIdx)"
            >
              <!-- Number input -->
              <template v-if="col.type === 'number'">
                <input
                  v-model.number="row[col.key]"
                  type="number"
                  class="cell-input"
                  @paste="onPaste($event, rowIdx, colIdx)"
                />
              </template>
              <!-- Board type select -->
              <template v-else-if="col.key === 'boardType'">
                <el-select
                  v-model="row.boardType"
                  size="small"
                  clearable
                  filterable
                  placeholder="选择板材"
                  style="width:100%"
                  @paste="onPaste($event, rowIdx, colIdx)"
                >
                  <el-option
                    v-for="b in boardOptions"
                    :key="b.boardId"
                    :label="boardLabel(b.boardId) || b.materialType"
                    :value="b.boardId"
                  >
                    <span>{{ b.brand }} {{ b.materialType }} {{ b.color }}</span>
                    <span style="color:#94a3b8;font-size:11px;margin-left:4px">
                      {{ b.length }}×{{ b.width }}×{{ b.thickness }}
                    </span>
                  </el-option>
                </el-select>
              </template>
              <!-- Text input -->
              <template v-else>
                <input
                  v-model="row[col.key]"
                  type="text"
                  class="cell-input"
                  @paste="onPaste($event, rowIdx, colIdx)"
                />
              </template>
            </div>
          </template>
        </el-table-column>

        <!-- Action column -->
        <el-table-column label="操作" width="60" fixed="right">
          <template #default="{ $index: rowIdx }">
            <el-button
              size="small"
              type="danger"
              :icon="Delete"
              circle
              @click="$emit('delete-row', rowIdx)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style>
.cutting-table .el-table__body-wrapper {
  overflow-y: auto;
}
.cutting-table-wrap {
  min-width: 0;
}
.cutting-table-body {
  padding: 0;
  overflow: auto;
  flex: 1;
  min-width: 0;
}
.cutting-table {
  min-width: 1040px;
}
.cutting-table .cell {
  padding: 0 !important;
}
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
.cell-inner .el-select {
  width: 100%;
}
.cell-inner .el-select .el-input__inner {
  font-size: 14px;
}
.cell-inner .el-select .el-input__wrapper {
  border-radius: 0;
  box-shadow: none !important;
  border: 2px solid transparent;
}
.cell-inner .el-select .el-input__wrapper:hover {
  border-color: #cbd5e1;
}
.cell-inner .el-select.is-focus .el-input__wrapper {
  border-color: #0f766e;
  background: #f0fdfa;
}
.cell-error .el-select .el-input__wrapper {
  border-color: #dc2626 !important;
  background: #fef2f2;
}
</style>
