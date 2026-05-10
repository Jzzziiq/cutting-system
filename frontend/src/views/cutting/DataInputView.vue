<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import OrderInfoBar from '@/components/cutting/OrderInfoBar.vue';
import RawMaterialPanel from '@/components/cutting/RawMaterialPanel.vue';
import OffcutPanel from '@/components/cutting/OffcutPanel.vue';
import CuttingTable from '@/components/cutting/CuttingTable.vue';
import BottomSummaryBar from '@/components/cutting/BottomSummaryBar.vue';
import { useCuttingTable } from '@/composables/useCuttingTable';
import { submitAlgorithm } from '@/api/algorithm';

const router = useRouter();

// Order info
const customer = ref('');
const orderNo = ref('');
const orderDate = ref(new Date().toISOString().slice(0, 10));
const operator = ref('');
const remark = ref('');

// Board selection
const selectedBoards = ref([]);

// Offcut selection
const selectedOffcuts = ref([]);

// Cutting table
const boardOptionsRef = computed(() => selectedBoards.value);
const {
  rows,
  columns,
  focusedCell,
  errorCount,
  totalItems,
  boardTypeCount,
  totalArea,
  addRow,
  deleteRow,
  handlePaste,
  handleKeydown,
  validateAll,
  buildAlgorithmInput
} = useCuttingTable(boardOptionsRef);

const canConfirm = computed(() => errorCount.value === 0 && totalItems.value > 0);

const confirming = ref(false);

async function onConfirm() {
  if (!validateAll()) {
    ElMessage.error('存在数据错误，请检查红色标记的行');
    return;
  }
  if (selectedBoards.value.length === 0) {
    ElMessage.warning('请至少选择一种原材料板材');
    return;
  }
  if (totalItems.value === 0) {
    ElMessage.warning('请至少录入一条下料尺寸');
    return;
  }

  confirming.value = true;
  try {
    // Use first selected board dimensions as container for algorithm
    const board = selectedBoards.value[0];
    const squareList = buildAlgorithmInput();
    const payload = {
      L: board.length,
      W: board.width,
      isRotateEnable: true,
      gapDistance: 3,
      squareList
    };

    const result = await submitAlgorithm(payload, 'tabu_search');
    router.push({
      name: 'layout-workbench',
      query: { taskId: result?.taskId || '' }
    });
  } catch (e) {
    ElMessage.error(e.message || '算法提交失败');
  } finally {
    confirming.value = false;
  }
}

function onFocusCell({ row, col }) {
  focusedCell.value = { row, col };
}
</script>

<template>
  <div class="cutting-view">
    <!-- Top: Order info bar -->
    <OrderInfoBar
      v-model:customer="customer"
      v-model:order-no="orderNo"
      v-model:order-date="orderDate"
      v-model:operator="operator"
      v-model:remark="remark"
    />

    <!-- Main: Left (boards + offcuts) | Right (cutting table) -->
    <div class="data-input-layout">
      <div class="left-panel">
        <RawMaterialPanel v-model="selectedBoards" />
        <OffcutPanel
          v-model="selectedOffcuts"
          :selected-boards="selectedBoards"
        />
      </div>

      <div class="right-panel">
        <CuttingTable
          :rows="rows"
          :columns="columns"
          :board-options="selectedBoards"
          :focused-cell="focusedCell"
          @add-row="addRow"
          @delete-row="deleteRow"
          @paste="handlePaste"
          @keydown="handleKeydown"
          @focus-cell="onFocusCell"
        />
      </div>
    </div>

    <!-- Bottom: Summary + confirm -->
    <BottomSummaryBar
      :item-count="totalItems"
      :board-type-count="boardTypeCount"
      :total-area="totalArea"
      :error-count="errorCount"
      :can-confirm="canConfirm && !confirming"
      @confirm="onConfirm"
    />
  </div>
</template>

<style scoped>
.data-input-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.95fr) minmax(0, 2.05fr);
  gap: 12px;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.right-panel {
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  display: flex;
}

@media (max-width: 1024px) {
  .data-input-layout {
    grid-template-columns: 1fr;
  }
}
</style>
