import http from './http';

export function listRemnants(params) {
  return http.get('/remnants', { params });
}

export function getRemnant(id) {
  return http.get(`/remnants/${id}`);
}

export function createRemnant(data) {
  return http.post('/remnants', data);
}

export function updateRemnant(id, data) {
  return http.put(`/remnants/${id}`, data);
}

export function deleteRemnant(id) {
  return http.delete(`/remnants/${id}`);
}
