import http from './http';

export function login({ username, password }) {
  const data = new URLSearchParams();
  data.set('username', username);
  data.set('password', password);

  return http.post('/auth/login', data, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
}
