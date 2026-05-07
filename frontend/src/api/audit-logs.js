import http from './http';

export function listAuditLogs(params) {
  return http.get('/audit-logs', { params });
}
