<script setup>
import { ZoomIn, ZoomOut, FullScreen, Download, Setting, Upload, Back } from '@element-plus/icons-vue';

defineProps({
  zoom: { type: Number, default: 1 },
  taskRunning: { type: Boolean, default: false }
});

defineEmits([
  'import-order',
  'start-layout',
  'open-settings',
  'zoom-in',
  'zoom-out',
  'fit-screen',
  'export-toolpath',
  'export-file',
  'save-result',
  'back-to-edit'
]);
</script>

<template>
  <div class="layout-toolbar">
    <div class="toolbar-group">
      <el-button size="small" :icon="Upload" @click="$emit('import-order')">导入排单</el-button>
      <el-button size="small" type="primary" :icon="Setting" :loading="taskRunning" @click="$emit('start-layout')">
        开始排版
      </el-button>
    </div>

    <div class="toolbar-group">
      <el-button size="small" :icon="Setting" @click="$emit('open-settings')">刀具参数</el-button>
    </div>

    <div class="toolbar-group">
      <el-button size="small" :icon="ZoomOut" @click="$emit('zoom-out')" />
      <span class="zoom-label">{{ Math.round(zoom * 100) }}%</span>
      <el-button size="small" :icon="ZoomIn" @click="$emit('zoom-in')" />
      <el-button size="small" :icon="FullScreen" @click="$emit('fit-screen')">适应</el-button>
    </div>

    <div class="toolbar-group">
      <el-button size="small" :icon="Download" @click="$emit('export-toolpath')">输出刀轨</el-button>
      <el-button size="small" :icon="Download" @click="$emit('export-file')">导出文件</el-button>
      <el-button size="small" :icon="Download" @click="$emit('save-result')">保存结果</el-button>
    </div>

    <div class="toolbar-spacer" />

    <div class="toolbar-group">
      <el-button size="small" :icon="Back" @click="$emit('back-to-edit')">返回编辑</el-button>
    </div>
  </div>
</template>

<style scoped>
.layout-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.toolbar-group {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 8px;
  border-right: 1px solid #e2e8f0;
}
.toolbar-group:first-child { padding-left: 0; }
.toolbar-group:last-child { border-right: none; padding-right: 0; }
.toolbar-spacer { flex: 1; }
.zoom-label {
  font-size: 13px;
  color: #64748b;
  min-width: 40px;
  text-align: center;
  font-weight: 600;
}
</style>
