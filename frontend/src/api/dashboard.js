import http from './http';

export function getSummary() {
  return http.get('/dashboard/summary');
}

export function getOrderTrend(days = 7) {
  return http.get('/dashboard/order-trend', { params: { days } });
}

export function getOrderStatusDist() {
  return http.get('/dashboard/order-status-dist');
}
