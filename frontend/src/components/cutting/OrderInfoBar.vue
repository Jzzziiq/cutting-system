<script setup>
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { listCustomers } from '@/api/customers';
import { listOrders, createOrder, getOrder } from '@/api/orders';

const props = defineProps({
  customer: String,
  orderNo: String,
  orderDate: String,
  operator: String,
  remark: String,
  orderId: { type: Number, default: null }
});

const emit = defineEmits([
  'update:customer',
  'update:orderNo',
  'update:orderDate',
  'update:operator',
  'update:remark',
  'update:orderId'
]);

const customerModel = computed({
  get: () => props.customer,
  set: (val) => emit('update:customer', val)
});
const orderNoModel = computed({
  get: () => props.orderNo,
  set: () => {}
});
const orderDateModel = computed({
  get: () => props.orderDate,
  set: (val) => emit('update:orderDate', val)
});
const operatorModel = computed({
  get: () => props.operator,
  set: () => {}
});
const remarkModel = computed({
  get: () => props.remark,
  set: (val) => emit('update:remark', val)
});

// Customer autocomplete (existing)
const customerOptions = ref([]);
const customerLoading = ref(false);
async function queryCustomers(query) {
  customerLoading.value = true;
  try {
    const data = await listCustomers({ pageNum: 1, pageSize: 20, search: query || '' });
    const list = Array.isArray(data) ? data : (data?.records ?? []);
    customerOptions.value = list.map(c => ({
      value: c.customerName,
      label: c.customerName
    }));
  } catch {
    customerOptions.value = [];
  } finally {
    customerLoading.value = false;
  }
}

// === Order selection ===
const orderOptions = ref([]);
const selectedOrderId = ref(props.orderId);
const orderSearchLoading = ref(false);

watch(() => props.orderId, (val) => {
  selectedOrderId.value = val;
});

async function loadOrders(query) {
  orderSearchLoading.value = true;
  try {
    const data = await listOrders({ pageNum: 1, pageSize: 30, search: query || '' });
    const records = data?.records ?? (Array.isArray(data) ? data : []);
    if (records.length > 0 && records[0].orderId !== undefined) {
      orderOptions.value = records.map(o => ({
        value: o.orderId,
        label: `#${o.orderId} ${o.orderNo || ''} - ${o.customerName || ''}`,
        order: o
      }));
    } else {
      orderOptions.value = [];
    }
  } catch {
    orderOptions.value = [];
  } finally {
    orderSearchLoading.value = false;
  }
}

async function onOrderSelect(orderId) {
  if (!orderId) {
    emit('update:orderId', null);
    emit('update:orderNo', '');
    return;
  }
  try {
    const data = await getOrder(orderId);
    const order = data?.orderId !== undefined ? data : (data?.data ?? data);
    emit('update:customer', order.customerName || '');
    emit('update:orderNo', String(order.orderId || orderId));
    emit('update:orderDate', order.dispatchDate || order.orderDate || order.createTime?.slice(0, 10) || '');
    emit('update:remark', order.remark || '');
    emit('update:orderId', orderId);
  } catch (e) {
    ElMessage.error('加载订单详情失败');
  }
}

// === Create order dialog ===
const createDialogVisible = ref(false);
const newOrderForm = ref({
  customerId: null,
  customerName: '',
  processName: '',
  dispatchDate: null,
  remark: ''
});
const creating = ref(false);

// Customer search for new order dialog (needs customerId)
const dialogCustomerOptions = ref([]);
const dialogCustomerLoading = ref(false);
async function queryDialogCustomers(query) {
  dialogCustomerLoading.value = true;
  try {
    const data = await listCustomers({ pageNum: 1, pageSize: 20, search: query || '' });
    const list = Array.isArray(data) ? data : (data?.records ?? []);
    dialogCustomerOptions.value = list.map(c => ({
      value: c.customerId,
      label: `${c.customerName}${c.phone ? ' - ' + c.phone : ''}`,
      customer: c
    }));
  } catch {
    dialogCustomerOptions.value = [];
  } finally {
    dialogCustomerLoading.value = false;
  }
}

function onCustomerSelect(customerId) {
  const found = dialogCustomerOptions.value.find(c => c.value === customerId);
  if (found) {
    newOrderForm.value.customerName = found.customer.customerName || '';
  }
}

function openCreateDialog() {
  newOrderForm.value = {
    customerId: null,
    customerName: '',
    processName: '',
    dispatchDate: new Date().toISOString().slice(0, 10),
    remark: ''
  };
  createDialogVisible.value = true;
}

async function onCreateOrder() {
  if (!newOrderForm.value.customerId) {
    ElMessage.warning('请选择客户');
    return;
  }
  creating.value = true;
  try {
    const result = await createOrder({
      customerId: newOrderForm.value.customerId,
      customerName: newOrderForm.value.customerName,
      processName: newOrderForm.value.processName.trim() || null,
      dispatchDate: newOrderForm.value.dispatchDate,
      remark: newOrderForm.value.remark.trim(),
      orderStatus: 0
    });
    const orderId = result?.orderId ?? result?.data?.orderId;
    if (orderId) {
      ElMessage.success('订单创建成功');
      createDialogVisible.value = false;
      loadOrders('');
      selectedOrderId.value = orderId;
      await onOrderSelect(orderId);
    } else {
      ElMessage.error('创建订单失败：未返回订单ID');
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '创建订单失败');
  } finally {
    creating.value = false;
  }
}
</script>

<template>
  <div class="order-info-bar">
    <div class="bar-row">
      <!-- Order selection -->
      <div class="order-select-group">
        <span class="group-label">目标订单</span>
        <el-select
          :model-value="selectedOrderId"
          filterable
          remote
          reserve-keyword
          placeholder="选择已有订单"
          size="small"
          style="width:240px"
          clearable
          :remote-method="loadOrders"
          :loading="orderSearchLoading"
          @focus="loadOrders('')"
          @change="onOrderSelect"
        >
          <el-option
            v-for="opt in orderOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button size="small" type="primary" plain @click="openCreateDialog">
          新建订单
        </el-button>
      </div>
    </div>

    <!-- Existing info fields -->
    <div class="bar-row">
      <el-form :inline="true" size="small">
        <el-form-item label="客户名称">
          <el-select
            v-model="customerModel"
            filterable
            allow-create
            remote
            reserve-keyword
            placeholder="输入客户"
            size="small"
            style="width:180px"
            clearable
            :remote-method="queryCustomers"
            :loading="customerLoading"
            @focus="queryCustomers('')"
          >
            <el-option
              v-for="opt in customerOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="orderNoModel" placeholder="创建后自动生成" style="width:140px" readonly />
        </el-form-item>
        <el-form-item label="排单日期">
          <el-date-picker v-model="orderDateModel" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:140px" clearable />
        </el-form-item>
        <el-form-item label="创建者">
          <el-input v-model="operatorModel" placeholder="当前登录用户" style="width:140px" readonly />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="remarkModel" placeholder="备注" style="width:200px" clearable />
        </el-form-item>
      </el-form>
    </div>

    <!-- Create Order Dialog -->
    <el-dialog v-model="createDialogVisible" title="新建订单" width="520px" :close-on-click-modal="false">
      <el-form label-position="top" label-width="90px" size="default">
        <el-form-item label="客户" required>
          <el-select
            v-model="newOrderForm.customerId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索并选择客户"
            style="width:100%"
            :remote-method="queryDialogCustomers"
            :loading="dialogCustomerLoading"
            @focus="queryDialogCustomers('')"
            @change="onCustomerSelect"
          >
            <el-option
              v-for="opt in dialogCustomerOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="加工名称">
          <el-input v-model="newOrderForm.processName" placeholder="如：主卧衣柜板材加工" maxlength="100" />
        </el-form-item>
        <el-form-item label="排单日期">
          <el-date-picker v-model="newOrderForm.dispatchDate" type="date" placeholder="选择排单日期" value-format="YYYY-MM-DD" style="width:100%" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="newOrderForm.remark" type="textarea" placeholder="可选" maxlength="255" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreateOrder">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-info-bar {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 16px;
}

.bar-row {
  margin-bottom: 6px;
}
.bar-row:last-child {
  margin-bottom: 0;
}

.order-select-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.group-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 700;
  white-space: nowrap;
}

.order-info-bar :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}
.order-info-bar :deep(.el-form-item__label) {
  font-size: 13px;
  color: #64748b;
  font-weight: 700;
}
.order-info-bar :deep(.el-input__inner) {
  font-size: 14px;
}
</style>
