import http from './http';

export function listOrganizations(params = {}) {
  return http.get('/organizations', { params });
}

export function getOrganization(id) {
  return http.get(`/organizations/${id}`);
}

export function createOrganization(data) {
  return http.post('/organizations', data);
}

export function updateOrganizationStatus(id, status) {
  return http.put(`/organizations/${id}/status`, null, { params: { status } });
}
