import http from './http';

export function listLayoutResults(params) {
  return http.get('/layout-results', { params });
}

export function getLayoutResult(id) {
  return http.get(`/layout-results/${id}`);
}

export function getLayoutResultsByOrder(orderId) {
  return http.get(`/layout-results/order/${orderId}`);
}

export function createLayoutResult(data) {
  return http.post('/layout-results', data);
}

export function updateLayoutResult(id, data) {
  return http.put(`/layout-results/${id}`, data);
}

export function deleteLayoutResult(id) {
  return http.delete(`/layout-results/${id}`);
}
