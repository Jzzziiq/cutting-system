<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();

const navItems = [
  // system admin
  { name: 'admin-dashboard', label: '系统概览', perm: 'account:manage', adminOnly: true },
  { name: 'organizations', label: '组织管理', perm: 'account:manage', adminOnly: true },
  { name: 'users', label: '账号管理', perm: 'account:manage', adminOnly: true },

  // org admin
  { name: 'dashboard', label: '工作台', perm: 'board:read', orgAdminOnly: true, operatorExcluded: true },
  { name: 'production-board', label: '生产看板', perm: 'board:read' },
  { name: 'customers', label: '客户管理', perm: 'customer:read' },
  { name: 'boards', label: '板材管理', perm: 'board:read' },
  { name: 'org-users', label: '成员管理', perm: 'user:manage' },
  { name: 'audit-logs', label: '审计日志', perm: 'audit:read' },

  // operator
  { name: 'data-input', label: '加工数据输入', perm: 'order:write' },
  { name: 'layout-workbench', label: '排版工作台', perm: 'layout:read' },

  // producer
  { name: 'producer-tasks', label: '我的任务', perm: 'production:read', producerOnly: true },

  // common
  { name: 'profile', label: '个人设置', perm: null }
];

const visibleItems = computed(() =>
  navItems.filter((item) => {
    if (item.adminOnly) return auth.isSystemAdmin;
    if (item.producerOnly) return auth.isProducer;
    if (item.orgAdminExcluded && auth.isOrgAdmin) return false;
    if (item.operatorExcluded && (auth.isOperator || auth.isProducer)) return false;
    if (item.orgAdminOnly) return auth.isOrgAdmin;
    if (item.operatorOnly) return auth.isOperator || auth.isProducer;
    if (item.perm) return auth.hasPermission(item.perm);
    return true;
  })
);

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

    <main class="main-panel">
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
