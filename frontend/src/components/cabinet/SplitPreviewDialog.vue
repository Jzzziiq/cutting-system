<script setup>
import { splitColumns } from '@/constants/cabinet';

defineProps({
  modelValue: { type: Boolean, default: false },
  splitGroups: { type: Array, default: () => [] },
  splitItems: { type: Array, default: () => [] },
  splitTotalCount: { type: Number, default: 0 },
  confirming: { type: Boolean, default: false }
});

const emit = defineEmits(['update:modelValue', 'confirm']);

function onClose() {
  emit('update:modelValue', false);
}

function onConfirm() {
  emit('confirm');
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="统一拆单预览" width="860px" top="5vh" @update:model-value="onClose">
    <div class="split-summary">
      共 {{ splitGroups.length }} 个柜体，{{ splitTotalCount }} 条订单明细，确认后将追加写入当前订单。
    </div>
    <el-table :data="splitItems" size="small" border stripe max-height="400">
      <el-table-column v-for="col in splitColumns" :key="col.prop" v-bind="col" />
    </el-table>
    <template #footer>
      <el-button @click="onClose">取消</el-button>
      <el-button type="primary" :loading="confirming" @click="onConfirm">确认统一写入订单</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.split-summary {
  margin-bottom: 10px;
  font-size: 13px;
  color: #475569;
}
</style>
