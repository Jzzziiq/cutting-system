import http from './http';

export function getAdminSummary() {
  return http.get('/admin/dashboard');
}
