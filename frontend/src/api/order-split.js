import http from './http';

export function executeSplit(data) {
  return http.post('/order-split/execute', data);
}

export function confirmSplit(data) {
  return http.post('/order-split/confirm', data);
}
