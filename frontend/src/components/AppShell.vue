<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();
const sidebarExpanded = ref(true);
let expandTimer = null;
let collapseTimer = null;

const navItems = [
  { name: 'dashboard', label: '工作台', perm: null },
  { name: 'customers', label: '客户管理', perm: 'customer:read' },
  { name: 'boards', label: '板材管理', perm: 'board:read' },
  { name: 'production-board', label: '生产看板', perm: 'order:read' },
  { name: 'data-input', label: '加工数据输入', perm: 'order:write' },
  { name: 'layout-workbench', label: '排版工作台', perm: 'layout:read' },
  { name: 'users', label: '用户管理', perm: 'user:manage' },
  { name: 'audit-logs', label: '审计日志', perm: 'user:manage' }
];

const visibleItems = computed(() =>
  navItems.filter((item) => !item.perm || auth.hasPermission(item.perm))
);

function clearExpandTimer() {
  if (!expandTimer) return;
  window.clearTimeout(expandTimer);
  expandTimer = null;
}

function clearCollapseTimer() {
  if (!collapseTimer) return;
  window.clearTimeout(collapseTimer);
  collapseTimer = null;
}

function clearSidebarTimers() {
  clearExpandTimer();
  clearCollapseTimer();
}

function expandSidebar() {
  clearSidebarTimers();
  sidebarExpanded.value = true;
}

function scheduleExpandSidebar() {
  clearSidebarTimers();
  expandTimer = window.setTimeout(() => {
    sidebarExpanded.value = true;
    expandTimer = null;
  }, 150);
}

function scheduleCollapseSidebar() {
  clearSidebarTimers();
  collapseTimer = window.setTimeout(() => {
    sidebarExpanded.value = false;
    collapseTimer = null;
  }, 120);
}

function logout() {
  auth.logout();
  router.replace({ name: 'login' });
}

onBeforeUnmount(clearSidebarTimers);
</script>

<template>
  <div class="app-shell" :class="{ 'sidebar-collapsed': !sidebarExpanded }">
    <div
      class="sidebar-edge"
      aria-hidden="true"
      @mouseenter="scheduleExpandSidebar"
      @mouseleave="clearExpandTimer"
    ></div>
    <aside
      class="sidebar"
      @mouseenter="expandSidebar"
      @mouseleave="scheduleCollapseSidebar"
      @focusin="expandSidebar"
    >
      <div class="brand-block">
        <div class="brand-title">板材切割系统</div>
        <div class="brand-subtitle">Web Console</div>
      </div>

      <nav class="nav-list">
        <router-link
          v-for="item in visibleItems"
          :key="item.name"
          class="nav-item"
          :to="{ name: item.name }"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="sidebar-user">
        <div>
          <span>当前用户</span>
          <strong>{{ auth.displayName }}</strong>
        </div>
        <button class="sidebar-logout" type="button" @click="logout">退出</button>
      </div>
    </aside>

    <main class="main-panel" @focusin="scheduleCollapseSidebar" @mouseenter="scheduleCollapseSidebar">
      <section class="content-panel">
        <router-view v-slot="{ Component }">
          <keep-alive :max="10">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </section>
    </main>
  </div>
</template>
