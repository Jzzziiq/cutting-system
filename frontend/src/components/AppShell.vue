<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const navItems = [
  { name: 'dashboard', label: '工作台' },
  { name: 'customers', label: '客户管理' },
  { name: 'boards', label: '板材管理' },
  { name: 'algorithm', label: '算法排样' }
];

const currentTitle = computed(() => route.meta.title || '工作台');

function logout() {
  auth.logout();
  router.replace({ name: 'login' });
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand-title">板材切割系统</div>
        <div class="brand-subtitle">Web Console</div>
      </div>

      <nav class="nav-list">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          class="nav-item"
          :to="{ name: item.name }"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div>
          <h1>{{ currentTitle }}</h1>
          <p>前后端分离管理端</p>
        </div>
        <div class="user-area">
          <span>{{ auth.displayName }}</span>
          <button class="btn ghost" type="button" @click="logout">退出</button>
        </div>
      </header>

      <section class="content-panel">
        <router-view />
      </section>
    </main>
  </div>
</template>
