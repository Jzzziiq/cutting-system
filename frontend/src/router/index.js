import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import AppShell from '@/components/AppShell.vue';
import LoginView from '@/views/LoginView.vue';
import DashboardView from '@/views/DashboardView.vue';
import CustomersView from '@/views/CustomersView.vue';
import BoardsView from '@/views/BoardsView.vue';
import AlgorithmView from '@/views/AlgorithmView.vue';

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
        path: 'algorithm',
        name: 'algorithm',
        component: AlgorithmView,
        meta: { title: '算法排样' }
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
