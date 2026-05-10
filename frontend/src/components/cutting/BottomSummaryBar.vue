<script setup>
defineProps({
  itemCount: { type: Number, default: 0 },
  boardTypeCount: { type: Number, default: 0 },
  totalArea: { type: Number, default: 0 },
  errorCount: { type: Number, default: 0 },
  canConfirm: { type: Boolean, default: false }
});

defineEmits(['confirm']);
</script>

<template>
  <div class="bottom-bar">
    <div class="stats">
      <span class="stat-item">工件数量：<strong>{{ itemCount }}</strong></span>
      <span class="stat-item">板材类型：<strong>{{ boardTypeCount }}</strong></span>
      <span class="stat-item">总面积：<strong>{{ totalArea.toLocaleString() }}</strong> mm²</span>
      <span v-if="errorCount" class="stat-item" style="color:#dc2626">
        错误行数：<strong>{{ errorCount }}</strong>
      </span>
    </div>
    <el-button type="primary" size="large" :disabled="!canConfirm" @click="$emit('confirm')">
      确认并进入排版
    </el-button>
  </div>
</template>

<style scoped>
.bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.stats {
  display: flex;
  gap: 20px;
  font-size: 14px;
}
.stat-item {
  color: #475569;
}
.stat-item strong {
  color: #172033;
}
</style>
