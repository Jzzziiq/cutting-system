import http from './http';

export function listTasks(params) {
  return http.get('/production-tasks', { params });
}

export function getTask(id) {
  return http.get(`/production-tasks/${id}`);
}

export function createTask(data) {
  return http.post('/production-tasks', data);
}

export function updateTask(id, data) {
  return http.put(`/production-tasks/${id}`, data);
}

export function deleteTask(id) {
  return http.delete(`/production-tasks/${id}`);
}

export function transitionTask(id, targetStatus, remark) {
  return http.put(`/production-tasks/${id}/status`, { targetStatus, remark });
}

export function assignTask(id, assigneeId, assigneeName) {
  return http.put(`/production-tasks/${id}/assign`, { assigneeId, assigneeName });
}

export function assignOrderTask(orderId, assigneeId) {
  return http.put(`/production-tasks/order/${orderId}/assign`, { assigneeId });
}

export function kanbanData() {
  return http.get('/production-tasks/kanban');
}

export function listTasksByOrder(orderId) {
  return http.get(`/production-tasks/order/${orderId}`);
}

