const api = require('../../services/api');
const app = getApp();

Page({
  data: {
    userInfo: null,
    showPasswordDialog: false,
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    saving: false
  },
  onShow() {
    this.loadProfile();
  },
  async loadProfile() {
    try {
      const userInfo = await api.getProfile();
      this.setData({ userInfo });
    } catch (err) {
      // 如果 /users/me 不可用，从 storage 读取
      const userInfo = wx.getStorageSync('userInfo');
      if (userInfo) this.setData({ userInfo });
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail });
  },
  showChangePassword() {
    this.setData({ showPasswordDialog: true, oldPassword: '', newPassword: '', confirmPassword: '' });
  },
  hideChangePassword() {
    this.setData({ showPasswordDialog: false });
  },
  async onChangePassword() {
    const { oldPassword, newPassword, confirmPassword } = this.data;
    if (!oldPassword || !newPassword) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      this.setData({ saving: false });
      return;
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      this.setData({ saving: false });
      return;
    }
    this.setData({ saving: true });
    try {
      await api.changePassword(oldPassword, newPassword);
      wx.showToast({ title: '密码修改成功' });
      this.hideChangePassword();
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ saving: false });
    }
  },
  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success(res) {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          app.globalData.token = '';
          app.globalData.userInfo = null;
          wx.reLaunch({ url: '/pages/login/login' });
        }
      }
    });
  }
});
