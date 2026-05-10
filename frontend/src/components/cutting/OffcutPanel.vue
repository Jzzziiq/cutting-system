<script setup>
import { ref, computed, watch } from 'vue';
import { listRemnants } from '@/api/remnants';

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  selectedBoards: { type: Array, default: () => [] }
});

const emit = defineEmits(['update:modelValue']);

const remnants = ref([]);
const loading = ref(false);

const materialTypes = computed(() => {
  const set = new Set(props.selectedBoards.map(b => b.materialType).filter(Boolean));
  return [...set];
});

const colors = computed(() => {
  const set = new Set(props.selectedBoards.map(b => b.color).filter(Boolean));
  return [...set];
});

watch([materialTypes, colors], async () => {
  if (!materialTypes.value.length && !colors.value.length) {
    remnants.value = [];
    return;
  }
  loading.value = true;
  try {
    const params = { pageNum: 1, pageSize: 200 };
    if (materialTypes.value.length) params.materialType = materialTypes.value[0];
    if (colors.value.length) params.color = colors.value[0];
    const data = await listRemnants(params);
    remnants.value = Array.isArray(data) ? data : (data?.records ?? []);
  } catch {
    remnants.value = [];
  } finally {
    loading.value = false;
  }
}, { immediate: true, deep: true });

function isSelected(offcut) {
  return props.modelValue.some(r => r.offcutId === offcut.offcutId);
}

function toggle(offcut) {
  if (isSelected(offcut)) {
    emit('update:modelValue', props.modelValue.filter(r => r.offcutId !== offcut.offcutId));
  } else {
    emit('update:modelValue', [...props.modelValue, offcut]);
  }
}

function dimLabel(r) {
  return `${r.length || '-'} × ${r.width || '-'} × ${r.thickness || '-'} mm`;
}
</script>

<template>
  <div class="offcut-panel panel-container">
    <div class="panel-header">
      <span>余料选择</span>
      <el-tag v-if="remnants.length" size="small" type="warning">{{ remnants.length }} 块可用</el-tag>
    </div>
    <div class="panel-body">
      <div v-if="!materialTypes.length" class="empty-hint">
        请先在原材料选择中添加板材，系统自动匹配同材质/颜色的余料
      </div>
      <div v-else-if="loading" class="empty-hint">加载余料中...</div>
      <div v-else-if="!remnants.length" class="empty-hint">
        当前材质/颜色暂无可用余料
      </div>
      <el-table
        v-else
        :data="remnants"
        size="small"
        max-height="200"
        stripe
        @row-click="toggle"
        row-style="cursor: pointer"
      >
        <el-table-column width="40">
          <template #default="{ row }">
            <el-checkbox :model-value="isSelected(row)" @click.stop="toggle(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="materialType" label="材质" width="70" />
        <el-table-column prop="color" label="颜色" width="60" />
        <el-table-column label="尺寸(mm)" width="130">
          <template #default="{ row }">{{ dimLabel(row) }}</template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" width="70" />
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.empty-hint {
  font-size: 13px;
  color: #94a3b8;
  padding: 12px 0;
}
</style>
