<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const errorMessage = ref('');

const form = reactive({
  username: '',
  password: ''
});

async function submit() {
  errorMessage.value = '';
  loading.value = true;
  try {
    await auth.login(form);
    router.replace(route.query.redirect || '/dashboard');
  } catch (error) {
    errorMessage.value = error.message || '登录失败';
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
        <h1>板材切割系统</h1>
        <p>请输入账号密码进入网页管理端</p>
      </div>

      <form class="form-stack" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <input v-model.trim="form.username" class="input" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" class="input" type="password" autocomplete="current-password" required />
        </label>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="btn primary full" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>
