import http from './http';

export function listOrderItems(params) {
  return http.get('/order-items', { params });
}

export function getOrderItem(id) {
  return http.get(`/order-items/${id}`);
}

export function createOrderItem(data) {
  return http.post('/order-items', data);
}

export function updateOrderItem(id, data) {
  return http.put(`/order-items/${id}`, data);
}

export function deleteOrderItem(id) {
  return http.delete(`/order-items/${id}`);
}
