<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  cabinetName: { type: String, default: '' },
  hasCabinet: { type: Boolean, default: false },
  materialSlots: { type: Array, default: () => [] },
  activeSlotMap: { type: Object, default: () => ({}) },
  activeSlotsMapped: { type: Boolean, default: false },
  boardOptionsBySlot: { type: Object, default: () => ({}) },
  formatMaterialSlot: { type: Function, required: true }
});

const emit = defineEmits(['update:modelValue', 'update:slot']);

function onClose() {
  emit('update:modelValue', false);
}

function onSlotChange(slot, value) {
  emit('update:slot', slot, value);
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="板材映射" width="520px" @update:model-value="onClose">
    <p class="slot-hint">
      当前柜体：{{ cabinetName || '未命名柜体' }}。为每类材料角色选择实际板材，未完成映射时不能拆单。
    </p>
    <el-form v-if="hasCabinet" label-width="96px" size="small">
      <el-form-item v-for="slot in materialSlots" :key="slot" :label="formatMaterialSlot(slot)">
        <el-select
          :model-value="activeSlotMap[slot]"
          filterable
          placeholder="选择板材"
          style="width:100%"
          @update:model-value="value => onSlotChange(slot, value)"
        >
          <el-option
            v-for="board in boardOptionsBySlot[slot]"
            :key="board.value"
            :label="board.label"
            :value="board.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="onClose">取消</el-button>
      <el-button type="primary" :disabled="!activeSlotsMapped" @click="onClose">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.slot-hint {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
}
</style>
