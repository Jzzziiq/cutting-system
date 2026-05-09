import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import AppShell from '@/components/AppShell.vue';
import LoginView from '@/views/LoginView.vue';
import DashboardView from '@/views/DashboardView.vue';
import CustomersView from '@/views/CustomersView.vue';
import BoardsView from '@/views/BoardsView.vue';
import UsersView from '@/views/UsersView.vue';
import AuditLogView from '@/views/AuditLogView.vue';
import ProductionKanbanView from '@/views/ProductionKanbanView.vue';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AppShell,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: '工作台' }
      },
      {
        path: 'customers',
        name: 'customers',
        component: CustomersView,
        meta: { title: '客户管理' }
      },
      {
        path: 'boards',
        name: 'boards',
        component: BoardsView,
        meta: { title: '板材管理' }
      },
      {
        path: 'users',
        name: 'users',
        component: UsersView,
        meta: { title: '用户管理' }
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: AuditLogView,
        meta: { title: '审计日志' }
      },
      {
        path: 'production-board',
        name: 'production-board',
        component: ProductionKanbanView,
        meta: { title: '生产看板' }
      },
      {
        path: 'cutting/data-input',
        name: 'data-input',
        component: () => import('@/views/cutting/DataInputView.vue'),
        meta: { title: '加工数据输入' }
      },
      {
        path: 'cutting/layout-workbench',
        name: 'layout-workbench',
        component: () => import('@/views/cutting/LayoutWorkbenchView.vue'),
        meta: { title: '排版工作台' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  document.title = `${to.meta.title || '系统'} - 板材切割系统`;

  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' };
  }

  return true;
});

export default router;
