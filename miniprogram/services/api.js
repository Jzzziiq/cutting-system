const request = require('../utils/request');

function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data: { username, password },
    auth: false,
    form: true
  });
}

function listMyTasks() {
  return request({ url: '/production-tasks/my' });
}

function getMyTaskDetail(taskId) {
  return request({ url: `/production-tasks/my/${taskId}` });
}

function transitionTask(taskId, status) {
  return request({
    url: `/production-tasks/my/${taskId}/status`,
    method: 'PUT',
    data: { status }
  });
}

function listNotifications(pageNum = 1, pageSize = 20) {
  return request({ url: '/notifications', data: { pageNum, pageSize } });
}

function markNotificationRead(id) {
  return request({ url: `/notifications/${id}/read`, method: 'PUT' });
}

function markAllNotificationsRead() {
  return request({ url: '/notifications/read-all', method: 'PUT' });
}

function getUnreadCount() {
  return request({ url: '/notifications/unread-count' });
}

function getProfile() {
  return request({ url: '/users/me' });
}

function changePassword(oldPassword, newPassword) {
  return request({
    url: '/users/me/password',
    method: 'PUT',
    data: { oldPassword, newPassword }
  });
}

module.exports = {
  login,
  listMyTasks,
  getMyTaskDetail,
  transitionTask,
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  getUnreadCount,
  getProfile,
  changePassword
};
