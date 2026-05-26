<script setup>
import { computed, onMounted, onActivated, watch, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import OrderInfoBar from '@/components/cutting/OrderInfoBar.vue';
import RawMaterialPanel from '@/components/cutting/RawMaterialPanel.vue';
import OffcutPanel from '@/components/cutting/OffcutPanel.vue';
import BoardGroupTable from '@/components/cutting/BoardGroupTable.vue';
import BottomSummaryBar from '@/components/cutting/BottomSummaryBar.vue';
import { useBoardWorkpieceGroups } from '@/composables/useBoardWorkpieceGroups';
import { getOrder, getLayoutInput, saveLayoutInput } from '@/api/orders';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// Order info
const currentOrderId = ref(null);
const customer = ref('');
const orderNo = ref('');
const orderDate = ref(new Date().toISOString().slice(0, 10));
const operator = ref('');
const remark = ref('');
const currentOperatorName = computed(() => authStore.user?.realName || authStore.user?.username || '');

// Board groups
const {
  boardGroups,
  columns,
  totalItems,
  groupCount,
  totalArea,
  totalErrors,
  addBoardGroup,
  removeBoardGroup,
  addItem,
  deleteItem,
  handlePaste,
  handleKeydown,
  validateAll,
  getGroupStats,
  loadFromLayoutInput,
  buildSavePayload
} = useBoardWorkpieceGroups();

const selectedOffcuts = ref([]);
const newlyCreatedOrderIds = ref(new Set());
const canConfirm = computed(() => totalErrors.value === 0 && totalItems.value > 0 && boardGroups.value.length > 0);

function formatDate(val) {
  if (!val) return '';
  if (typeof val === 'number' || val instanceof Date) {
    const d = new Date(val);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
  if (typeof val === 'string' && val.length >= 10) return val.slice(0, 10);
  return '';
}

function displayOrderNo(order, fallbackId) {
  const id = order?.orderId ?? fallbackId;
  return id ? String(id) : '';
}

function applyDefaultOperator() {
  if (!operator.value && currentOperatorName.value) {
    operator.value = currentOperatorName.value;
  }
}

function onAddBoard(board) {
  addBoardGroup(board);
}

function onOrderCreated(orderId) {
  newlyCreatedOrderIds.value.add(orderId);
}

async function loadOrderContext(orderId) {
  const id = Number(orderId);
  if (!Number.isFinite(id) || id <= 0) return;
  try {
    const order = await getOrder(id);
    currentOrderId.value = id;
    customer.value = order?.customerName || '';
    orderNo.value = displayOrderNo(order, id);
    orderDate.value = formatDate(order?.dispatchDate || order?.orderDate || order?.createTime) || orderDate.value;
    remark.value = order?.remark || '';
    applyDefaultOperator();

    const layoutInput = await getLayoutInput(id);
    loadFromLayoutInput(layoutInput);
    if (!layoutInput?.groups?.length && !newlyCreatedOrderIds.value.has(id)) {
      ElMessage.info('该订单暂无已保存的工件明细');
    }
    newlyCreatedOrderIds.value.delete(id);
  } catch (e) {
    ElMessage.error(e?.message || '加载订单失败');
  }
}

async function onRemoveBoardGroup(groupId) {
  const group = boardGroups.value.find(g => g.id === groupId);
  if (!group) return;
  const stats = getGroupStats(group);
  if (stats.itemCount > 0) {
    try {
      await ElMessageBox.confirm(
        `该板材组下有 ${stats.itemCount} 个工件，确认删除？`,
        '确认删除',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
      );
    } catch {
      return;
    }
  }
  removeBoardGroup(groupId);
}

async function onSave() {
  if (!currentOrderId.value) {
    ElMessage.warning('请先选择或创建一个订单');
    return;
  }
  try {
    await saveLayoutInput(currentOrderId.value, buildSavePayload());
    ElMessage.success('保存成功');
  } catch (e) {
    ElMessage.error(e?.message || '保存失败');
  }
}

async function goToCabinetDesign() {
  if (!currentOrderId.value) {
    ElMessage.warning('请先选择或创建一个订单');
    return;
  }
  try {
    await saveLayoutInput(currentOrderId.value, buildSavePayload());
  } catch (e) {
    ElMessage.error(e?.message || '保存失败');
    return;
  }
  router.push({
    name: 'cabinet-design',
    query: { orderId: currentOrderId.value }
  });
}

async function onConfirm() {
  if (!currentOrderId.value) {
    ElMessage.warning('请先选择或创建一个订单');
    return;
  }
  if (!validateAll()) {
    ElMessage.error('存在数据错误，请检查红色标记的行');
    return;
  }
  if (boardGroups.value.length === 0) {
    ElMessage.warning('请至少添加一种原材料板材');
    return;
  }
  if (totalItems.value === 0) {
    ElMessage.warning('请至少录入一条工件尺寸');
    return;
  }

  try {
    await saveLayoutInput(currentOrderId.value, buildSavePayload());
    ElMessage.success('排版输入已保存，进入排版工作台');
  } catch (e) {
    ElMessage.error('保存排版输入失败：' + (e.message || '未知错误'));
    return;
  }

  router.push({ name: 'layout-workbench', query: { orderId: currentOrderId.value } });
}

onActivated(() => {
  applyDefaultOperator();
  const oid = route.query.orderId;
  if (oid) {
    loadOrderContext(oid);
  }
});

watch(
  () => route.query.orderId,
  (newId) => {
    applyDefaultOperator();
    if (newId) {
      loadOrderContext(newId);
    }
  }
);

// Load order context when order is selected from history dialog or created
watch(currentOrderId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadOrderContext(newId);
  }
});

onMounted(() => {
  applyDefaultOperator();
  if (route.query.orderId) {
    loadOrderContext(route.query.orderId);
  }
});
</script>

<template>
  <div class="cutting-shell">
    <div class="cutting-view">
      <!-- Top: Order info bar -->
      <OrderInfoBar
        v-model:customer="customer"
        v-model:order-no="orderNo"
        v-model:order-date="orderDate"
        v-model:operator="operator"
        v-model:remark="remark"
        v-model:order-id="currentOrderId"
        @order-created="onOrderCreated"
      />

      <!-- Main: Left (boards + offcuts) | Right (board group tables) -->
      <div class="data-input-layout">
        <div class="left-panel">
          <RawMaterialPanel
            :board-groups="boardGroups"
            @add-board="onAddBoard"
            @remove-board="onRemoveBoardGroup"
          />
          <OffcutPanel
            v-model="selectedOffcuts"
            :selected-boards="boardGroups.map(g => g.board)"
          />
        </div>

        <div class="right-panel">
          <BoardGroupTable
            :board-groups="boardGroups"
            :columns="columns"
            :get-group-stats="getGroupStats"
            @add-item="addItem"
            @delete-item="deleteItem"
            @remove-group="onRemoveBoardGroup"
            @paste="handlePaste"
            @keydown="(groupId, event, rowIdx, colIdx) => handleKeydown(groupId, event, rowIdx, colIdx)"
          />
        </div>
      </div>

      <!-- Bottom: Summary + actions -->
      <div class="bottom-actions">
        <BottomSummaryBar
          :item-count="totalItems"
          :board-type-count="groupCount"
          :total-area="totalArea"
          :error-count="totalErrors"
          :can-confirm="canConfirm"
          :can-save="!!currentOrderId"
          @confirm="onConfirm"
          @save="onSave"
        />
        <el-button
          v-if="currentOrderId"
          v-permission="'order:write'"
          type="success"
          size="large"
          @click="goToCabinetDesign"
        >
          3D 柜体设计
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bottom-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
.bottom-actions > :first-child {
  flex: 1;
}
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
