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
    displayName: (state) => state.user?.realName || state.user?.username || '用户'
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
    logout() {
      this.token = '';
      this.user = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }
});

export { TOKEN_KEY, USER_KEY };
