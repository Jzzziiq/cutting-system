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

export function deleteBoard(id) {
  return http.delete(`/boards/${id}`);
}
