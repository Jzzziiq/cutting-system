import http from './http';

export function listCustomers(params) {
  return http.get('/customers', { params });
}

export function getCustomer(id) {
  return http.get(`/customers/${id}`);
}

export function createCustomer(data) {
  return http.post('/customers', data);
}

export function updateCustomer(id, data) {
  return http.put(`/customers/${id}`, data);
}

export function deleteCustomer(id) {
  return http.delete(`/customers/${id}`);
}
