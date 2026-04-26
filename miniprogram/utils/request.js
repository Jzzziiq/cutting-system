const { baseUrl } = require('./config');

function redirectToLogin() {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1];
  if (!current || current.route !== 'pages/login/login') {
    wx.reLaunch({ url: '/pages/login/login' });
  }
}

function request(options) {
  const {
    url,
    method = 'GET',
    data = {},
    auth = true,
    raw = false,
    form = false
  } = options;

  const token = wx.getStorageSync('token');
  const header = {
    'Content-Type': form ? 'application/x-www-form-urlencoded' : 'application/json',
    ...(options.header || {})
  };

  if (auth) {
    if (!token) {
      redirectToLogin();
      return Promise.reject(new Error('未登录'));
    }
    header.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}${url}`,
      method,
      data,
      header,
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          wx.showToast({ title: '登录已失效', icon: 'none' });
          redirectToLogin();
          reject(new Error('未授权'));
          return;
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          wx.showToast({ title: `请求失败 ${res.statusCode}`, icon: 'none' });
          reject(res);
          return;
        }

        if (raw) {
          if (res.data && typeof res.data.code === 'number' && res.data.code !== 200) {
            wx.showToast({ title: res.data.msg || '请求失败', icon: 'none' });
            reject(res.data);
            return;
          }
          resolve(res.data);
          return;
        }

        if (res.data && res.data.code === 200) {
          resolve(res.data.data);
          return;
        }

        wx.showToast({ title: (res.data && res.data.msg) || '请求失败', icon: 'none' });
        reject(res.data);
      },
      fail(err) {
        wx.showToast({ title: '网络不可用', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = request;
