import http from './http';

export function submitAlgorithm(data, algorithm = 'tabu_search') {
  return http.post('/algorithm/submit', data, { params: { algorithm } });
}

export function getAlgorithmResult(taskId) {
  return http.get(`/algorithm/result/${taskId}`);
}

export function getAlgorithms() {
  return http.get('/algorithm/algorithms');
}

export function compareAlgorithms(data, algorithms = 'tabu_search,genetic_algorithm') {
  return http.post('/algorithm/compare', data, { params: { algorithms } });
}
