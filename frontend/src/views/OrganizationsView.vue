<script setup>
import { onMounted, reactive, ref } from 'vue';
import { listOrganizations, createOrganization, updateOrganizationStatus } from '@/api/organizations';

const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
const modalMode = ref('');
const form = reactive({ orgName: '', orgCode: '' });

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const res = await listOrganizations(page);
    records.value = res?.records || [];
    total.value = res?.total ?? records.value.length;
  } catch (e) {
    errorMessage.value = e.message || '加载组织列表失败';
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  form.orgName = '';
  form.orgCode = '';
  modalMode.value = 'create';
}

async function submit() {
  if (!form.orgName || !form.orgCode) {
    errorMessage.value = '请填写完整信息';
    return;
  }
  errorMessage.value = '';
  try {
    await createOrganization({ orgName: form.orgName, orgCode: form.orgCode });
    modalMode.value = '';
    await loadData();
  } catch (e) {
    errorMessage.value = e.message || '创建失败';
  }
}

async function handleToggleStatus(org) {
  const newStatus = org.status === 1 ? 2 : 1;
  const action = newStatus === 2 ? '禁用' : '启用';
  if (!window.confirm(`确定要${action}组织「${org.orgName}」吗？`)) return;
  errorMessage.value = '';
  try {
    await updateOrganizationStatus(org.orgId, newStatus);
    await loadData();
  } catch (e) {
    errorMessage.value = e.message || `${action}失败`;
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

onMounted(loadData);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>组织管理</h2>
        <p>管理组织信息与状态</p>
      </div>
      <div class="action-group">
        <button class="btn primary" type="button" @click="openCreate">创建组织</button>
      </div>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>组织名称</th>
            <th>组织编码</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6">加载中...</td>
          </tr>
          <tr v-else-if="!records.length">
            <td colspan="6">暂无组织数据</td>
          </tr>
          <tr v-for="item in records" v-else :key="item.orgId">
            <td>{{ item.orgId }}</td>
            <td>{{ item.orgName }}</td>
            <td>{{ item.orgCode }}</td>
            <td>
              <span class="status" :class="{ off: item.status !== 1 }">
                {{ item.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td>{{ item.createTime || '-' }}</td>
            <td>
              <div class="row-actions">
                <button
                  class="btn small"
                  :class="item.status === 1 ? 'danger' : 'ghost'"
                  type="button"
                  @click="handleToggleStatus(item)"
                >
                  {{ item.status === 1 ? '禁用' : '启用' }}
                </button>
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
        <h3>创建组织</h3>
        <button class="icon-btn" type="button" @click="modalMode = ''">×</button>
      </div>
      <div class="form-grid">
        <label>
          <span>组织名称</span>
          <input v-model.trim="form.orgName" class="input" placeholder="如：福州某某加工厂" required />
        </label>
        <label>
          <span>组织编码</span>
          <input v-model.trim="form.orgCode" class="input" placeholder="唯一编码，用于员工注册" required />
        </label>
      </div>
      <div class="modal-actions">
        <button class="btn ghost" type="button" @click="modalMode = ''">取消</button>
        <button class="btn primary" type="submit">创建</button>
      </div>
    </form>
  </div>
</template>
