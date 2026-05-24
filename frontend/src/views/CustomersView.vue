<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  batchDeleteCustomers,
  batchUpdateCustomerStatus,
  createCustomer,
  deleteCustomer,
  downloadCustomerTemplate,
  exportCustomers,
  getCustomer,
  importCustomers,
  listCustomers,
  updateCustomer
} from '@/api/customers';

const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
const selectedIds = ref([]);
const modalMode = ref('');
const currentId = ref(null);
const form = reactive({
  customerName: '',
  phone: '',
  address: '',
  remark: '',
  isEnabled: 1
});

const readonly = computed(() => modalMode.value === 'detail');
const visibleIds = computed(() => records.value.map(item => item.customerId));
const selectedCount = computed(() => selectedIds.value.length);
const allVisibleSelected = computed(() =>
  visibleIds.value.length > 0 && visibleIds.value.every(id => selectedIds.value.includes(id))
);
const modalTitle = computed(() => {
  if (modalMode.value === 'create') return '新增客户';
  if (modalMode.value === 'edit') return '编辑客户';
  return '客户详情';
});

function resetForm(data = {}) {
  form.customerName = data.customerName || '';
  form.phone = data.phone || '';
  form.address = data.address || '';
  form.remark = data.remark || '';
  form.isEnabled = data.isEnabled ?? 1;
}

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const data = await listCustomers(page);
    records.value = data?.records || [];
    total.value = data?.total ?? records.value.length;
    selectedIds.value = selectedIds.value.filter(id => visibleIds.value.includes(id));
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  currentId.value = null;
  resetForm();
  modalMode.value = 'create';
}

async function openDetail(id) {
  const data = await getCustomer(id);
  currentId.value = id;
  resetForm(data);
  modalMode.value = 'detail';
}

async function openEdit(id) {
  const data = await getCustomer(id);
  currentId.value = id;
  resetForm(data);
  modalMode.value = 'edit';
}

async function submit() {
  const payload = {
    customerName: form.customerName,
    phone: form.phone,
    address: form.address,
    remark: form.remark
  };

  if (modalMode.value === 'create') {
    await createCustomer(payload);
  } else {
    await updateCustomer(currentId.value, { ...payload, isEnabled: Number(form.isEnabled) });
  }
  modalMode.value = '';
  await loadData();
}

async function remove(id) {
  if (!window.confirm('确认删除该客户？若已有订单引用，系统会阻止删除以保护订单数据，可改为禁用。')) return;
  errorMessage.value = '';
  try {
    await deleteCustomer(id);
    await loadData();
  } catch (e) {
    errorMessage.value = e.message;
  }
}

function toggleSelect(id) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter(item => item !== id);
  } else {
    selectedIds.value = [...selectedIds.value, id];
  }
}

function toggleVisibleSelection() {
  if (allVisibleSelected.value) {
    selectedIds.value = selectedIds.value.filter(id => !visibleIds.value.includes(id));
    return;
  }
  selectedIds.value = [...new Set([...selectedIds.value, ...visibleIds.value])];
}

async function batchSetEnabled(isEnabled) {
  if (!selectedCount.value) return;
  const label = isEnabled === 1 ? '启用' : '禁用';
  if (!window.confirm(`确认${label}已选 ${selectedCount.value} 个客户？`)) return;
  errorMessage.value = '';
  try {
    await batchUpdateCustomerStatus([...selectedIds.value], isEnabled);
    selectedIds.value = [];
    await loadData();
  } catch (e) {
    errorMessage.value = e.message;
  }
}

async function batchRemove() {
  if (!selectedCount.value) return;
  if (!window.confirm(`确认删除已选 ${selectedCount.value} 个客户？若其中有客户已有订单引用，系统会阻止删除以保护订单数据，可改为禁用。`)) return;
  errorMessage.value = '';
  try {
    await batchDeleteCustomers([...selectedIds.value]);
    selectedIds.value = [];
    await loadData();
  } catch (e) {
    errorMessage.value = e.message;
  }
}

function nextPage() {
  if (page.pageNum * page.pageSize < total.value) {
    page.pageNum += 1;
    loadData();
  }
}

function prevPage() {
  if (page.pageNum > 1) {
    page.pageNum -= 1;
    loadData();
  }
}

function downloadBlob(data, filename) {
  const url = window.URL.createObjectURL(new Blob([data]));
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
}

async function handleExport() {
  try {
    const data = await exportCustomers();
    downloadBlob(data, 'customers.xlsx');
  } catch (e) { errorMessage.value = e.message; }
}

async function handleImport(e) {
  const file = e.target.files[0];
  if (!file) return;
  errorMessage.value = '';
  try {
    const result = await importCustomers(file);
    alert(`导入完成：共 ${result.total} 条，成功 ${result.success}，失败 ${result.fail}`);
    await loadData();
  } catch (e) { errorMessage.value = e.message; }
  e.target.value = '';
}

async function handleDownloadTemplate() {
  try {
    const data = await downloadCustomerTemplate();
    downloadBlob(data, '客户导入模板.xlsx');
  } catch (e) { errorMessage.value = e.message; }
}

onMounted(loadData);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>客户列表</h2>
        <p>维护客户基础资料，供后续订单与排样流程使用</p>
      </div>
      <div class="action-group">
        <button v-permission="'customer:write'" class="btn primary" type="button" @click="openCreate">新增客户</button>
        <button v-permission="'customer:write'" class="btn secondary" type="button" @click="handleExport">导出</button>
        <label v-permission="'customer:write'" class="btn secondary" style="cursor:pointer">
          导入
          <input type="file" accept=".xlsx" hidden @change="handleImport" />
        </label>
        <button class="btn ghost" type="button" @click="handleDownloadTemplate">下载模板</button>
      </div>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="batch-toolbar">
      <label class="batch-selection">
        <input
          type="checkbox"
          :checked="allVisibleSelected"
          :disabled="!records.length"
          @change="toggleVisibleSelection"
        />
        <span>已选 {{ selectedCount }} 条</span>
      </label>
      <div class="batch-actions">
        <button v-permission="'customer:write'" class="btn small ghost" type="button" :disabled="!selectedCount" @click="batchSetEnabled(1)">批量启用</button>
        <button v-permission="'customer:write'" class="btn small ghost" type="button" :disabled="!selectedCount" @click="batchSetEnabled(0)">批量禁用</button>
        <button v-permission="'customer:write'" class="btn small danger" type="button" :disabled="!selectedCount" @click="batchRemove">批量删除</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th class="select-col"></th>
            <th>ID</th>
            <th>客户名称</th>
            <th>电话</th>
            <th>地址</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7">加载中...</td>
          </tr>
          <tr v-else-if="!records.length">
            <td colspan="7">暂无客户数据</td>
          </tr>
          <tr v-for="item in records" v-else :key="item.customerId">
            <td class="select-col">
              <input
                class="row-check"
                type="checkbox"
                :checked="selectedIds.includes(item.customerId)"
                @change="toggleSelect(item.customerId)"
              />
            </td>
            <td>{{ item.customerId }}</td>
            <td>{{ item.customerName }}</td>
            <td>{{ item.phone }}</td>
            <td>{{ item.address || '-' }}</td>
            <td>
              <span class="status" :class="{ off: item.isEnabled === 0 }">
                {{ item.isEnabled === 0 ? '禁用' : '启用' }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="btn small ghost" type="button" @click="openDetail(item.customerId)">查看</button>
                <button v-permission="'customer:write'" class="btn small secondary" type="button" @click="openEdit(item.customerId)">编辑</button>
                <button v-permission="'customer:write'" class="btn small danger" type="button" @click="remove(item.customerId)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <span>共 {{ total }} 条，第 {{ page.pageNum }} 页</span>
      <button class="btn ghost" type="button" :disabled="page.pageNum <= 1" @click="prevPage">上一页</button>
      <button class="btn ghost" type="button" :disabled="page.pageNum * page.pageSize >= total" @click="nextPage">下一页</button>
    </div>
  </div>

  <div v-if="modalMode" class="modal-backdrop">
    <form class="modal" @submit.prevent="submit">
      <div class="modal-header">
        <h3>{{ modalTitle }}</h3>
        <button class="icon-btn" type="button" @click="modalMode = ''">×</button>
      </div>
      <div class="form-grid">
        <label>
          <span>客户名称</span>
          <input v-model.trim="form.customerName" class="input" :readonly="readonly" required />
        </label>
        <label>
          <span>联系电话</span>
          <input v-model.trim="form.phone" class="input" :readonly="readonly" required />
        </label>
        <label class="wide">
          <span>地址</span>
          <input v-model.trim="form.address" class="input" :readonly="readonly" />
        </label>
        <label>
          <span>状态</span>
          <select v-model="form.isEnabled" class="input" :disabled="readonly || modalMode === 'create'">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </label>
        <label class="wide">
          <span>备注</span>
          <textarea v-model.trim="form.remark" class="input" rows="3" :readonly="readonly"></textarea>
        </label>
      </div>
      <div class="modal-actions">
        <button class="btn ghost" type="button" @click="modalMode = ''">取消</button>
        <button v-if="!readonly" v-permission="'customer:write'" class="btn primary" type="submit">保存</button>
        <button v-else v-permission="'customer:write'" class="btn secondary" type="button" @click="modalMode = 'edit'">编辑</button>
      </div>
    </form>
  </div>
</template>
