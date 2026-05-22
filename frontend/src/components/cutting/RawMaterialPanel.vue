<script setup>
import { ref } from 'vue';
import { listBoards } from '@/api/boards';
import { Search, Plus, Delete } from '@element-plus/icons-vue';

const props = defineProps({
  boardGroups: { type: Array, default: () => [] }
});

const emit = defineEmits(['add-board', 'remove-board']);

const searchQuery = ref('');
const searchResults = ref([]);
const searching = ref(false);
const searchFocused = ref(false);
let blurTimer = null;

async function doSearch() {
  if (!searchQuery.value.trim()) {
    searchResults.value = [];
    return;
  }
  searching.value = true;
  try {
    const data = await listBoards({ brand: searchQuery.value, materialType: searchQuery.value, color: searchQuery.value, pageNum: 1, pageSize: 50 });
    searchResults.value = Array.isArray(data) ? data : (data?.records ?? []);
  } catch {
    searchResults.value = [];
  } finally {
    searching.value = false;
  }
}

async function onFocus() {
  searchFocused.value = true;
  clearTimeout(blurTimer);
  if (searchResults.value.length > 0) return;
  searching.value = true;
  try {
    const data = await listBoards({ pageNum: 1, pageSize: 50 });
    searchResults.value = Array.isArray(data) ? data : (data?.records ?? []);
  } catch {
    searchResults.value = [];
  } finally {
    searching.value = false;
  }
}

function onBlur() {
  blurTimer = setTimeout(() => {
    searchFocused.value = false;
    if (!searchQuery.value.trim()) {
      searchResults.value = [];
    }
  }, 200);
}

function isBoardAdded(boardId) {
  return props.boardGroups.some(g => g.board.boardId === boardId);
}

function addBoard(board) {
  emit('add-board', board);
}

function removeBoard(boardId) {
  emit('remove-board', boardId);
}

function boardLabel(board) {
  return [board.brand, board.materialType, board.color, board.sizeType]
    .filter(Boolean).join(' ');
}

function dimLabel(board) {
  return `${board.length || '-'} × ${board.width || '-'} × ${board.thickness || '-'} mm`;
}
</script>

<template>
  <div class="raw-material-panel panel-container">
    <div class="panel-header">
      <span>原材料选择</span>
    </div>
    <div class="panel-body">
      <!-- Search -->
      <div class="search-row">
        <el-input
          v-model="searchQuery"
          placeholder="搜索板材（品牌/材质/颜色）"
          size="small"
          clearable
          @focus="onFocus"
          @blur="onBlur"
          @keyup.enter="doSearch"
        >
          <template #append>
            <el-button :icon="Search" :loading="searching" @click="doSearch" />
          </template>
        </el-input>
      </div>

      <!-- Search results -->
      <div v-if="searchResults.length && (searchFocused || searchQuery.trim())" class="result-section">
        <div class="section-label">搜索结果</div>
        <el-table :data="searchResults" size="small" max-height="180" stripe>
          <el-table-column prop="materialType" label="材质" width="80" />
          <el-table-column prop="color" label="颜色" width="70" />
          <el-table-column label="尺寸(mm)" width="150">
            <template #default="{ row }">{{ dimLabel(row) }}</template>
          </el-table-column>
          <el-table-column prop="brand" label="品牌" width="80" />
          <el-table-column label="操作" width="60">
            <template #default="{ row }">
              <el-button
                v-if="!isBoardAdded(row.boardId)"
                size="small"
                type="primary"
                :icon="Plus"
                circle
                @click="addBoard(row)"
              />
              <el-tag v-else size="small" type="success">已添加</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- Empty search state -->
      <div v-if="!searchResults.length && searchQuery" class="empty-hint">
        <template v-if="searching">搜索中...</template>
        <template v-else>未找到匹配板材</template>
      </div>

      <!-- Selected boards -->
      <div class="selected-section">
        <div class="section-label">
          已选板材
          <el-tag v-if="boardGroups.length" size="small" type="info">{{ boardGroups.length }} 种</el-tag>
        </div>
        <div v-if="!boardGroups.length" class="empty-hint">暂未添加板材，请搜索并添加</div>
        <div v-for="group in boardGroups" :key="group.id" class="board-card">
          <div class="board-info">
            <div class="board-name">{{ boardLabel(group.board) }}</div>
            <div class="board-dims">{{ dimLabel(group.board) }}</div>
          </div>
          <el-button size="small" type="danger" :icon="Delete" circle @click="removeBoard(group.id)" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-row { margin-bottom: 10px; }
.section-label {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.result-section { margin-bottom: 12px; }
.selected-section { margin-top: 4px; }
.empty-hint {
  font-size: 13px;
  color: #94a3b8;
  padding: 8px 0;
}
.board-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  margin-bottom: 6px;
  background: #f8fafc;
}
.board-name { font-size: 14px; font-weight: 600; color: #172033; }
.board-dims { font-size: 13px; color: #64748b; margin-top: 2px; }
</style>
