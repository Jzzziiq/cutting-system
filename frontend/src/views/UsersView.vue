<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { listUsers, getUser, updateUserStatus, assignRoles, listRoles } from '@/api/users';

const auth = useAuthStore();
const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
const modalMode = ref('');
const currentId = ref(null);
const allRoles = ref([]);
const form = reactive({
  username: '',
  realName: '',
  phone: '',
  roleType: null,
  accountStatus: 1,
  remark: '',
  roles: [],
  permissions: []
});
const roleForm = ref([]);

const readonly = computed(() => modalMode.value === 'detail');
const modalTitle = computed(() => {
  if (modalMode.value === 'edit') return '编辑用户';
  return '用户详情';
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const data = await listUsers(page);
    records.value = data?.records || [];
    total.value = data?.total ?? records.value.length;
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
}

async function openDetail(id) {
  const data = await getUser(id);
  currentId.value = id;
  form.username = data.username || '';
  form.realName = data.realName || '';
  form.phone = data.phone || '';
  form.roleType = data.roleType;
  form.accountStatus = data.accountStatus;
  form.remark = data.remark || '';
  form.roles = data.roles || [];
  form.permissions = data.permissions || [];
  roleForm.value = allRoles.value.filter((r) => (data.roles || []).includes(r.roleCode)).map((r) => r.roleId);
  modalMode.value = 'detail';
}

async function openEdit(id) {
  await openDetail(id);
  modalMode.value = 'edit';
}

async function submit() {
  if (modalMode.value === 'edit') {
    await updateUserStatus(currentId.value, form.accountStatus);
    await assignRoles({ userId: currentId.value, roleIds: roleForm.value });
  }
  modalMode.value = '';
  await loadData();
}

async function toggleStatus(item) {
  const newStatus = item.accountStatus === 1 ? 2 : 1;
  await updateUserStatus(item.userId, newStatus);
  await loadData();
}

async function fetchRoles() {
  try {
    allRoles.value = await listRoles();
  } catch (e) {
    // ignore
  }
}

function toggleRole(roleId) {
  const idx = roleForm.value.indexOf(roleId);
  if (idx >= 0) {
    roleForm.value.splice(idx, 1);
  } else {
    roleForm.value.push(roleId);
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

const statusLabel = (s) => (s === 1 ? '启用' : s === 2 ? '禁用' : '待审批');

onMounted(() => {
  fetchRoles();
  loadData();
});
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>用户管理</h2>
        <p>管理系统用户、查看角色与权限</p>
      </div>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>角色</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7">加载中...</td>
          </tr>
          <tr v-else-if="!records.length">
            <td colspan="7">暂无用户数据</td>
          </tr>
          <tr v-for="item in records" v-else :key="item.userId">
            <td>{{ item.userId }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName || '-' }}</td>
            <td>{{ (item.roles || []).join(', ') || '-' }}</td>
            <td>
              <span class="status" :class="{ off: item.accountStatus !== 1 }">
                {{ statusLabel(item.accountStatus) }}
              </span>
            </td>
            <td>{{ item.createTime || '-' }}</td>
            <td>
              <div class="row-actions">
                <button class="btn small ghost" type="button" @click="openDetail(item.userId)">查看</button>
                <button class="btn small secondary" type="button" @click="openEdit(item.userId)">编辑</button>
                <button class="btn small danger" type="button" @click="toggleStatus(item)">
                  {{ item.accountStatus === 1 ? '禁用' : '启用' }}
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
        <h3>{{ modalTitle }}</h3>
        <button class="icon-btn" type="button" @click="modalMode = ''">×</button>
      </div>
      <div class="form-grid">
        <label>
          <span>用户名</span>
          <input class="input" :value="form.username" readonly />
        </label>
        <label>
          <span>真实姓名</span>
          <input class="input" :value="form.realName" readonly />
        </label>
        <label>
          <span>联系电话</span>
          <input class="input" :value="form.phone" readonly />
        </label>
        <label>
          <span>状态</span>
          <select v-model="form.accountStatus" class="input" :disabled="readonly">
            <option :value="1">启用</option>
            <option :value="2">禁用</option>
          </select>
        </label>
        <label class="wide">
          <span>角色分配</span>
          <div class="role-checkboxes" v-if="allRoles.length">
            <label v-for="role in allRoles" :key="role.roleId" class="role-chip" :class="{ active: roleForm.includes(role.roleId) }">
              <input type="checkbox" :value="role.roleId" :disabled="readonly" :checked="roleForm.includes(role.roleId)" @change="toggleRole(role.roleId)" />
              {{ role.roleName }}
            </label>
          </div>
          <span v-else>加载中...</span>
        </label>
        <label class="wide">
          <span>当前权限</span>
          <div class="perm-tags">
            <span v-for="p in form.permissions" :key="p" class="tag">{{ p }}</span>
            <span v-if="!form.permissions.length">无</span>
          </div>
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
