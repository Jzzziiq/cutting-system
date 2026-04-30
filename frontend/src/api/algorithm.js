import http from './http';

export function solveAlgorithm(data) {
  return http.post('/algorithm/answer', data);
}
