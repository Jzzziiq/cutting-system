import axios from 'axios';
import { TOKEN_KEY, USER_KEY } from '@/stores/auth';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && Object.prototype.hasOwnProperty.call(body, 'code')) {
      if (body.code === 200) {
        return body.data ?? null;
      }
      return Promise.reject(new Error(body.msg || '请求失败'));
    }
    return body;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(new Error(error.response?.data?.msg || error.message || '网络请求失败'));
  }
);

export default http;
