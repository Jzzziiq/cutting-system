<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import http from '@/api/http';

const auth = useAuthStore();

const user = computed(() => auth.user || {});
const orgRoleLabel = computed(() => {
  const map = { org_admin: '组织管理员', operator: '操作员', viewer: '生产员' };
  return map[user.value.orgRole] || user.value.orgRole || '-';
});

const form = ref({ oldPassword: '', newPassword: '', confirmPassword: '' });
const saving = ref(false);
const errorMessage = ref('');
const showPasswordModal = ref(false);

function openPasswordModal() {
  errorMessage.value = '';
  form.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
  showPasswordModal.value = true;
}

function closePasswordModal() {
  showPasswordModal.value = false;
  errorMessage.value = '';
  form.value = { oldPassword: '', newPassword: '', confirmPassword: '' };
}

async function changePassword() {
  errorMessage.value = '';
  if (!form.value.oldPassword || !form.value.newPassword) {
    errorMessage.value = '请填写旧密码和新密码';
    return;
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的新密码不一致';
    return;
  }
  if (form.value.newPassword.length < 6) {
    errorMessage.value = '新密码至少6位';
    return;
  }
  saving.value = true;
  try {
    await http.put('/users/me/password', {
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword
    });
    closePasswordModal();
    alert('密码修改成功');
  } catch (e) {
    errorMessage.value = e.response?.data?.message || '修改失败';
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div v-if="auth.isProducer" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4 text-yellow-800">
    生产员请使用微信小程序处理生产任务
  </div>

  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>个人设置</h2>
        <p>查看账号信息与修改密码</p>
      </div>
    </div>

    <h3 style="margin: 0 0 12px; font-size: 15px; color: #475569;">账号信息</h3>
    <table class="data-table">
      <tbody>
        <tr><td class="label" style="width: 120px; color: #64748b;">用户名</td><td>{{ user.username }}</td></tr>
        <tr><td class="label" style="width: 120px; color: #64748b;">真实姓名</td><td>{{ user.realName || '-' }}</td></tr>
        <tr><td class="label" style="width: 120px; color: #64748b;">所属组织</td><td>{{ user.orgName || '无（系统管理员）' }}</td></tr>
        <tr><td class="label" style="width: 120px; color: #64748b;">组织角色</td><td>{{ orgRoleLabel }}</td></tr>
      </tbody>
    </table>

    <div style="margin-top: 24px;">
      <button class="btn primary" type="button" @click="openPasswordModal">修改密码</button>
    </div>
  </div>

  <div v-if="showPasswordModal" class="modal-backdrop">
    <form class="modal" @submit.prevent="changePassword">
      <div class="modal-header">
        <h3>修改密码</h3>
        <button class="icon-btn" type="button" @click="closePasswordModal">×</button>
      </div>

      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <div class="form-grid">
        <label>
          <span>旧密码</span>
          <input v-model="form.oldPassword" class="input" type="password" required />
        </label>
        <label>
          <span>新密码</span>
          <input v-model="form.newPassword" class="input" type="password" required />
        </label>
        <label>
          <span>确认新密码</span>
          <input v-model="form.confirmPassword" class="input" type="password" required />
        </label>
      </div>

      <div class="modal-actions">
        <button class="btn ghost" type="button" @click="closePasswordModal">取消</button>
        <button class="btn primary" type="submit" :disabled="saving">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </form>
  </div>
</template>
