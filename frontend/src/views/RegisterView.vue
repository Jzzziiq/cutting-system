<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { registerOrg, registerUser } from '@/api/auth';

const router = useRouter();
const auth = useAuthStore();
const activeTab = ref('org');
const loading = ref(false);
const successMessage = ref('');
const errorMessage = ref('');

const orgForm = reactive({
  orgName: '',
  orgCode: '',
  password: '',
  confirmPassword: ''
});

const userForm = reactive({
  orgCode: '',
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: ''
});

async function submitOrg() {
  errorMessage.value = '';
  successMessage.value = '';
  if (orgForm.password !== orgForm.confirmPassword) {
    errorMessage.value = '两次密码不一致';
    return;
  }
  loading.value = true;
  try {
    const data = await registerOrg({
      orgName: orgForm.orgName,
      orgCode: orgForm.orgCode,
      password: orgForm.password
    });
    auth.setUser(data);
    router.replace('/dashboard');
  } catch (error) {
    errorMessage.value = error.message || '注册失败';
  } finally {
    loading.value = false;
  }
}

async function submitUser() {
  errorMessage.value = '';
  successMessage.value = '';
  if (userForm.password !== userForm.confirmPassword) {
    errorMessage.value = '两次密码不一致';
    return;
  }
  loading.value = true;
  try {
    await registerUser({
      orgCode: userForm.orgCode,
      username: userForm.username,
      password: userForm.password,
      realName: userForm.realName,
      phone: userForm.phone
    });
    successMessage.value = '注册成功，请等待管理员审批后登录';
  } catch (error) {
    errorMessage.value = error.message || '注册失败';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="login-heading">
        <span>Cutting System</span>
        <h1>注册账号</h1>
      </div>

      <div class="tab-bar">
        <button :class="['tab-btn', { active: activeTab === 'org' }]" @click="activeTab = 'org'">注册组织</button>
        <button :class="['tab-btn', { active: activeTab === 'user' }]" @click="activeTab = 'user'">加入组织</button>
      </div>

      <!-- Register Org Form -->
      <form v-if="activeTab === 'org'" class="form-stack" @submit.prevent="submitOrg">
        <label>
          <span>组织名称</span>
          <input v-model.trim="orgForm.orgName" class="input" required placeholder="如：福州某某加工厂" />
        </label>
        <label>
          <span>组织编码</span>
          <input v-model.trim="orgForm.orgCode" class="input" required placeholder="唯一编码，用于员工注册" />
        </label>
        <label>
          <span>管理员密码</span>
          <input v-model="orgForm.password" class="input" type="password" required minlength="6" />
        </label>
        <label>
          <span>确认密码</span>
          <input v-model="orgForm.confirmPassword" class="input" type="password" required />
        </label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="btn primary full" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册组织' }}
        </button>
      </form>

      <!-- Register User Form -->
      <form v-if="activeTab === 'user'" class="form-stack" @submit.prevent="submitUser">
        <label>
          <span>组织编码</span>
          <input v-model.trim="userForm.orgCode" class="input" required placeholder="向管理员获取" />
        </label>
        <label>
          <span>用户名</span>
          <input v-model.trim="userForm.username" class="input" required />
        </label>
        <label>
          <span>真实姓名</span>
          <input v-model.trim="userForm.realName" class="input" />
        </label>
        <label>
          <span>手机号</span>
          <input v-model.trim="userForm.phone" class="input" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="userForm.password" class="input" type="password" required minlength="6" />
        </label>
        <label>
          <span>确认密码</span>
          <input v-model="userForm.confirmPassword" class="input" type="password" required />
        </label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <p v-if="successMessage" class="form-success">{{ successMessage }}</p>
        <button class="btn primary full" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '申请加入' }}
        </button>
      </form>

      <div class="register-link">
        <router-link to="/login">已有账号？去登录</router-link>
      </div>
    </section>
  </main>
</template>

<style scoped>
.tab-bar {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}
.tab-btn {
  flex: 1;
  padding: 10px;
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
.form-success {
  color: #059669;
  font-size: 13px;
  margin: 0;
}
.register-link {
  text-align: center;
  margin-top: 16px;
}
.register-link a {
  color: #0d9488;
  font-size: 14px;
}
</style>
