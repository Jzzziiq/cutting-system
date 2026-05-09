<script setup>
import { computed } from 'vue';

const props = defineProps({
  customer: String,
  orderNo: String,
  orderDate: String,
  operator: String,
  remark: String
});

const emit = defineEmits([
  'update:customer',
  'update:orderNo',
  'update:orderDate',
  'update:operator',
  'update:remark'
]);

const customerModel = computed({
  get: () => props.customer,
  set: (val) => emit('update:customer', val)
});
const orderNoModel = computed({
  get: () => props.orderNo,
  set: (val) => emit('update:orderNo', val)
});
const orderDateModel = computed({
  get: () => props.orderDate,
  set: (val) => emit('update:orderDate', val)
});
const operatorModel = computed({
  get: () => props.operator,
  set: (val) => emit('update:operator', val)
});
const remarkModel = computed({
  get: () => props.remark,
  set: (val) => emit('update:remark', val)
});
</script>

<template>
  <div class="order-info-bar">
    <el-form :inline="true" size="small">
      <el-form-item label="客户名称">
        <el-input v-model="customerModel" placeholder="输入客户" style="width:140px" clearable />
      </el-form-item>
      <el-form-item label="订单号">
        <el-input v-model="orderNoModel" placeholder="订单号" style="width:140px" clearable />
      </el-form-item>
      <el-form-item label="排单日期">
        <el-input v-model="orderDateModel" placeholder="日期" style="width:130px" clearable />
      </el-form-item>
      <el-form-item label="操作人员">
        <el-input v-model="operatorModel" placeholder="操作员" style="width:120px" clearable />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="remarkModel" placeholder="备注" style="width:200px" clearable />
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.order-info-bar {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 12px;
}
.order-info-bar :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}
.order-info-bar :deep(.el-form-item__label) {
  font-size: 12px;
  color: #64748b;
  font-weight: 700;
}
</style>
