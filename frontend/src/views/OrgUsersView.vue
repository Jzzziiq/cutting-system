<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import http from '@/api/http';

const auth = useAuthStore();
const loading = ref(false);
const errorMessage = ref('');
const users = ref([]);
const pendingUsers = ref([]);
const activeTab = ref('members');

const orgRoleOptions = [
  { value: 'org_admin', label: '组织管理员' },
  { value: 'operator', label: '操作员' },
  { value: 'viewer', label: '生产员' }
];

const orgRoleLabel = (role) => orgRoleOptions.find(o => o.value === role)?.label || role;

async function loadUsers() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [membersRes, pendingRes] = await Promise.all([
      http.get('/users', { params: { pageNum: 1, pageSize: 100 } }),
      http.get('/users/pending', { params: { pageNum: 1, pageSize: 100 } })
    ]);
    users.value = membersRes?.records || [];
    pendingUsers.value = pendingRes?.records || [];
  } catch (e) {
    errorMessage.value = e.message || '加载用户列表失败';
  } finally {
    loading.value = false;
  }
}

async function handleApprove(user) {
  errorMessage.value = '';
  try {
    await http.put(`/users/${user.userId}/status`, null, { params: { accountStatus: 1 } });
    loadUsers();
  } catch (e) {
    errorMessage.value = e.message || '审批失败';
  }
}

async function handleReject(user) {
  errorMessage.value = '';
  try {
    await http.put(`/users/${user.userId}/status`, null, { params: { accountStatus: 2 } });
    loadUsers();
  } catch (e) {
    errorMessage.value = e.message || '操作失败';
  }
}

async function handleRoleChange(user, newRole) {
  errorMessage.value = '';
  try {
    await http.put(`/users/${user.userId}/org-role`, null, { params: { orgRole: newRole } });
    loadUsers();
  } catch (e) {
    errorMessage.value = e.message || '更新失败';
  }
}

onMounted(loadUsers);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>成员管理</h2>
        <p>管理组织成员与审批</p>
      </div>
    </div>

    <div class="tab-bar">
      <button :class="['tab-btn', { active: activeTab === 'members' }]" @click="activeTab = 'members'">
        组织成员 ({{ users.length }})
      </button>
      <button :class="['tab-btn', { active: activeTab === 'pending' }]" @click="activeTab = 'pending'">
        待审批 ({{ pendingUsers.length }})
      </button>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <!-- Members Tab -->
    <div v-if="activeTab === 'members'" class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>手机号</th>
            <th>组织角色</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6">加载中...</td>
          </tr>
          <tr v-else-if="!users.length">
            <td colspan="6">暂无成员数据</td>
          </tr>
          <tr v-for="item in users" v-else :key="item.userId">
            <td>{{ item.userId }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName || '-' }}</td>
            <td>{{ item.phone || '-' }}</td>
            <td>
              <select
                class="input"
                :value="item.orgRole"
                @change="(e) => handleRoleChange(item, e.target.value)"
                style="min-height: 32px; padding: 4px 8px; font-size: 13px;"
              >
                <option v-for="opt in orgRoleOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </td>
            <td>
              <span class="status" :class="{ off: item.accountStatus !== 1 }">
                {{ item.accountStatus === 1 ? '正常' : item.accountStatus === 3 ? '待审批' : '禁用' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pending Tab -->
    <div v-if="activeTab === 'pending'" class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>手机号</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6">加载中...</td>
          </tr>
          <tr v-else-if="!pendingUsers.length">
            <td colspan="6">暂无待审批用户</td>
          </tr>
          <tr v-for="item in pendingUsers" v-else :key="item.userId">
            <td>{{ item.userId }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.realName || '-' }}</td>
            <td>{{ item.phone || '-' }}</td>
            <td>{{ item.createTime || '-' }}</td>
            <td>
              <div class="row-actions">
                <button class="btn small primary" type="button" @click="handleApprove(item)">通过</button>
                <button class="btn small danger" type="button" @click="handleReject(item)">拒绝</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.tab-bar {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  width: fit-content;
}
.tab-btn {
  padding: 8px 20px;
  border: none;
  background: #f8fafc;
  cursor: pointer;
  font-size: 14px;
  color: #64748b;
  transition: all 0.2s;
}
.tab-btn.active {
  background: #0d9488;
  color: #fff;
  font-weight: 600;
}
</style>
