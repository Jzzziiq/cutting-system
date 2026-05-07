import http from './http';

export function listAuditLogs(params) {
  return http.get('/audit-logs', { params });
}

export function exportAuditLogs() {
  return http.get('/audit-logs/export', { responseType: 'blob' });
}
