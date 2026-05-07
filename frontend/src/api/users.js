import http from './http';

export function listUsers(params) {
  return http.get('/users', { params });
}

export function getUser(id) {
  return http.get(`/users/${id}`);
}

export function updateUserStatus(id, accountStatus) {
  return http.put(`/users/${id}/status`, null, { params: { accountStatus } });
}

export function assignRoles(data) {
  return http.put('/users/roles', data);
}

export function listRoles() {
  return http.get('/users/roles');
}
