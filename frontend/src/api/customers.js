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

export function exportCustomers() {
  return http.get('/customers/export', { responseType: 'blob' });
}

export function importCustomers(file) {
  const form = new FormData();
  form.append('file', file);
  return http.post('/customers/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function downloadCustomerTemplate() {
  return http.get('/customers/template', { responseType: 'blob' });
}
