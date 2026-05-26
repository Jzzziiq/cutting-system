const api = require('../../services/api');
const app = getApp();

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },
  onShow() {
    const token = wx.getStorageSync('token');
    if (token) {
      wx.switchTab({ url: '/pages/tasks/index' });
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail });
  },
  async onLogin() {
    const { username, password } = this.data;
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const res = await api.login(username, password);
      wx.setStorageSync('token', res.token);
      wx.setStorageSync('userInfo', res.userInfo || res);
      app.globalData.token = res.token;
      app.globalData.userInfo = res.userInfo || res;

      // 角色校验：只允许 viewer 角色
      const userInfo = res.userInfo || res;
      const roles = userInfo.roles || [];
      if (!roles.includes('viewer')) {
        wx.showToast({ title: '请使用电脑端管理系统', icon: 'none', duration: 3000 });
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        app.globalData.token = '';
        app.globalData.userInfo = null;
        return;
      }

      wx.switchTab({ url: '/pages/tasks/index' });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  }
});
