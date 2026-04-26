const api = require('../../services/api');

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onShow() {
    if (wx.getStorageSync('token')) {
      wx.switchTab({ url: '/pages/customers/index' });
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
  },

  async onLogin() {
    const { username, password } = this.data;
    if (!username || !password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    try {
      const info = await api.login(username, password);
      wx.setStorageSync('token', info.token);
      wx.setStorageSync('userInfo', info);
      getApp().globalData.token = info.token;
      getApp().globalData.userInfo = info;
      wx.switchTab({ url: '/pages/customers/index' });
    } finally {
      this.setData({ loading: false });
    }
  }
});
