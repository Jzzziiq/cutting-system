import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import AppShell from '@/components/AppShell.vue';

const LoginView = () => import('@/views/LoginView.vue');
const RegisterView = () => import('@/views/RegisterView.vue');
const DashboardView = () => import('@/views/DashboardView.vue');
const AdminDashboardView = () => import('@/views/AdminDashboardView.vue');
const CustomersView = () => import('@/views/CustomersView.vue');
const BoardsView = () => import('@/views/BoardsView.vue');
const UsersView = () => import('@/views/UsersView.vue');
const OrganizationsView = () => import('@/views/OrganizationsView.vue');
const OrgUsersView = () => import('@/views/OrgUsersView.vue');
const AuditLogView = () => import('@/views/AuditLogView.vue');
const ProductionKanbanView = () => import('@/views/ProductionKanbanView.vue');
const ProfileView = () => import('@/views/ProfileView.vue');

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { public: true, title: '注册' }
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
        path: 'admin/dashboard',
        name: 'admin-dashboard',
        component: AdminDashboardView,
        meta: { title: '系统概览', perm: 'account:manage' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: ProfileView,
        meta: { title: '个人设置' }
      },
      {
        path: 'customers',
        name: 'customers',
        component: CustomersView,
        meta: { title: '客户管理', perm: 'customer:read' }
      },
      {
        path: 'boards',
        name: 'boards',
        component: BoardsView,
        meta: { title: '板材管理', perm: 'board:read' }
      },
      {
        path: 'users',
        name: 'users',
        component: UsersView,
        meta: { title: '账号管理', perm: 'account:manage' }
      },
      {
        path: 'organizations',
        name: 'organizations',
        component: OrganizationsView,
        meta: { title: '组织管理', perm: 'account:manage' }
      },
      {
        path: 'org-users',
        name: 'org-users',
        component: OrgUsersView,
        meta: { title: '成员管理', perm: 'user:manage' }
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: AuditLogView,
        meta: { title: '审计日志', perm: 'audit:read' }
      },
      {
        path: 'production-board',
        name: 'production-board',
        component: ProductionKanbanView,
        meta: { title: '生产看板', perm: 'board:read' }
      },
      {
        path: 'cutting/data-input',
        name: 'data-input',
        component: () => import('@/views/cutting/DataInputView.vue'),
        meta: { title: '订单录入', perm: 'order:write' }
      },
      {
        path: 'cutting/layout-workbench',
        name: 'layout-workbench',
        component: () => import('@/views/cutting/LayoutWorkbenchView.vue'),
        meta: { title: '排版工作台', perm: 'layout:read' }
      },
      {
        path: 'cutting/cabinet-design',
        name: 'cabinet-design',
        component: () => import('@/views/cutting/CabinetDesignView.vue'),
        meta: { title: '3D 柜体设计', perm: 'order:write' }
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
    // role-based default landing page
    if (auth.isSystemAdmin) return { name: 'admin-dashboard' };
    if (auth.isProducer) return { name: 'profile' };
    return { name: 'dashboard' };
  }

  if (to.meta.operatorOnly && !auth.isOperator && !auth.isProducer) {
    if (auth.isSystemAdmin) return { name: 'admin-dashboard' };
    return { name: 'dashboard' };
  }

  if (to.meta.perm && !auth.hasPermission(to.meta.perm)) {
    // redirect to appropriate default page
    if (auth.isSystemAdmin) return { name: 'admin-dashboard' };
    if (auth.isProducer) return { name: 'profile' };
    return { name: 'dashboard' };
  }

  return true;
});

export default router;
