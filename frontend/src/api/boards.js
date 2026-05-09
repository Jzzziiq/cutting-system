import http from './http';

export function listBoards(params) {
  return http.get('/boards', { params });
}

export function getBoard(id) {
  return http.get(`/boards/${id}`);
}

export function createBoard(data) {
  return http.post('/boards', data);
}

export function updateBoard(id, data) {
  return http.put(`/boards/${id}`, data);
}

export function batchUpdateBoardStatus(ids, isEnabled) {
  return http.put('/boards/batch/status', { ids, isEnabled });
}

export function deleteBoard(id) {
  return http.delete(`/boards/${id}`);
}

export function batchDeleteBoards(ids) {
  return http.delete('/boards/batch', { data: { ids } });
}

export function exportBoards() {
  return http.get('/boards/export', { responseType: 'blob' });
}

export function importBoards(file) {
  const form = new FormData();
  form.append('file', file);
  return http.post('/boards/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function downloadBoardTemplate() {
  return http.get('/boards/template', { responseType: 'blob' });
}
