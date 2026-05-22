<script setup>
import { defaultWizardByCategory } from '@/constants/cabinet';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  wizard: { type: Object, required: true }
});

const emit = defineEmits(['update:modelValue', 'confirm']);

function onClose() {
  emit('update:modelValue', false);
}

function onConfirm() {
  emit('confirm', { ...props.wizard });
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="柜体参数" width="440px" @update:model-value="onClose">
    <el-form label-width="96px" size="small">
      <el-form-item label="宽度(mm)">
        <el-input-number v-model="wizard.width" :min="400" :max="3000" :step="100" style="width:100%" />
      </el-form-item>
      <el-form-item label="高度(mm)">
        <el-input-number v-model="wizard.height" :min="400" :max="3000" :step="100" style="width:100%" />
      </el-form-item>
      <el-form-item label="深度(mm)">
        <el-input-number v-model="wizard.depth" :min="300" :max="1200" :step="50" style="width:100%" />
      </el-form-item>
      <el-form-item label="层板数">
        <el-input-number v-model="wizard.shelfCount" :min="0" :max="8" style="width:100%" />
      </el-form-item>
      <el-form-item label="门板数">
        <el-input-number v-model="wizard.doorCount" :min="1" :max="6" style="width:100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="onClose">取消</el-button>
      <el-button type="primary" @click="onConfirm">生成模型</el-button>
    </template>
  </el-dialog>
</template>
