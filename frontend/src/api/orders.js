import http from './http';

export function listOrders(params) {
  return http.get('/orders', { params });
}

export function getOrder(id) {
  return http.get(`/orders/${id}`);
}

export function createOrder(data) {
  return http.post('/orders', data);
}

export function updateOrder(id, data) {
  return http.put(`/orders/${id}`, data);
}

export function deleteOrder(id) {
  return http.delete(`/orders/${id}`);
}

export function transitionOrderStatus(id, targetStatus, remark) {
  return http.put(`/orders/${id}/status`, { targetStatus, remark });
}

export function getOrderStatusLabels() {
  return http.get('/orders/status-labels');
}
