<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createCustomer,
  deleteCustomer,
  getCustomer,
  listCustomers,
  updateCustomer
} from '@/api/customers';

const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
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
  if (!window.confirm('确认删除该客户？')) return;
  await deleteCustomer(id);
  await loadData();
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

onMounted(loadData);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>客户列表</h2>
        <p>维护客户基础资料，供后续订单与排样流程使用</p>
      </div>
      <button class="btn primary" type="button" @click="openCreate">新增客户</button>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
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
            <td colspan="6">加载中...</td>
          </tr>
          <tr v-else-if="!records.length">
            <td colspan="6">暂无客户数据</td>
          </tr>
          <tr v-for="item in records" v-else :key="item.customerId">
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
                <button class="btn small secondary" type="button" @click="openEdit(item.customerId)">编辑</button>
                <button class="btn small danger" type="button" @click="remove(item.customerId)">删除</button>
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
        <button v-if="!readonly" class="btn primary" type="submit">保存</button>
        <button v-else class="btn secondary" type="button" @click="modalMode = 'edit'">编辑</button>
      </div>
    </form>
  </div>
</template>
