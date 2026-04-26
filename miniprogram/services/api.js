const request = require('../utils/request');

function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'POST',
    auth: false,
    form: true,
    data: { username, password }
  });
}

function listCustomers(pageNum = 1, pageSize = 20) {
  return request({ url: '/customers', data: { pageNum, pageSize } });
}

function getCustomer(id) {
  return request({ url: `/customers/${id}` });
}

function createCustomer(data) {
  return request({ url: '/customers', method: 'POST', data });
}

function updateCustomer(id, data) {
  return request({ url: `/customers/${id}`, method: 'PUT', data });
}

function deleteCustomer(id) {
  return request({ url: `/customers/${id}`, method: 'DELETE' });
}

function listBoards(pageNum = 1, pageSize = 20) {
  return request({ url: '/boards', data: { pageNum, pageSize } });
}

function getBoard(id) {
  return request({ url: `/boards/${id}` });
}

function createBoard(data) {
  return request({ url: '/boards', method: 'POST', data });
}

function updateBoard(id, data) {
  return request({ url: `/boards/${id}`, method: 'PUT', data });
}

function deleteBoard(id) {
  return request({ url: `/boards/${id}`, method: 'DELETE' });
}

function solveAlgorithm(data) {
  return request({ url: '/algorithm/answer', method: 'POST', data, raw: true });
}

module.exports = {
  login,
  listCustomers,
  getCustomer,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  listBoards,
  getBoard,
  createBoard,
  updateBoard,
  deleteBoard,
  solveAlgorithm
};
