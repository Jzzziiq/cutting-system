import http from './http';

export function listCabinetTemplates(params) {
  return http.get('/cabinet-templates', { params });
}

export function getCabinetTemplate(id) {
  return http.get(`/cabinet-templates/${id}`);
}

export function createCabinetTemplate(data) {
  return http.post('/cabinet-templates', data);
}

export function updateCabinetTemplate(id, data) {
  return http.put(`/cabinet-templates/${id}`, data);
}

export function deleteCabinetTemplate(id) {
  return http.delete(`/cabinet-templates/${id}`);
}
