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

export function registerOrg({ orgName, orgCode, password }) {
  const data = new URLSearchParams();
  data.set('orgName', orgName);
  data.set('orgCode', orgCode);
  data.set('password', password);

  return http.post('/auth/register-org', data, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
}

export function registerUser({ orgCode, username, password, realName, phone }) {
  const data = new URLSearchParams();
  data.set('orgCode', orgCode);
  data.set('username', username);
  data.set('password', password);
  if (realName) data.set('realName', realName);
  if (phone) data.set('phone', phone);

  return http.post('/auth/register-user', data, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  });
}
