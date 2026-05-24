import { defineStore } from 'pinia';
import { login as loginApi } from '@/api/auth';

const TOKEN_KEY = 'cutting_system_token';
const USER_KEY = 'cutting_system_user';

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null');
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '用户',
    permissions: (state) => state.user?.permissions || [],
    roles: (state) => state.user?.roles || [],
    orgId: (state) => state.user?.orgId || null,
    orgRole: (state) => state.user?.orgRole || null,
    isOrgAdmin: (state) => state.user?.orgRole === 'org_admin',
    isAdmin: (state) => (state.user?.roles || []).includes('admin'),
    isSystemAdmin: (state) => (state.user?.roles || []).includes('admin') && !state.user?.orgId,
    isProducer: (state) => (state.user?.roles || []).includes('viewer'),
    isOperator: (state) => (state.user?.roles || []).includes('operator'),
    hasPermission: (state) => (code) => (state.user?.permissions || []).includes(code),
    hasRole: (state) => (code) => (state.user?.roles || []).includes(code)
  },
  actions: {
    async login(credentials) {
      const user = await loginApi(credentials);
      this.token = user?.token || '';
      this.user = user || null;
      localStorage.setItem(TOKEN_KEY, this.token);
      localStorage.setItem(USER_KEY, JSON.stringify(this.user));
      return user;
    },
    setUser(user) {
      this.token = user?.token || '';
      this.user = user || null;
      localStorage.setItem(TOKEN_KEY, this.token);
      localStorage.setItem(USER_KEY, JSON.stringify(this.user));
    },
    logout() {
      this.token = '';
      this.user = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }
});

export { TOKEN_KEY, USER_KEY };
