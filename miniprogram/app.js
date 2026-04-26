App({
  globalData: {
    token: '',
    userInfo: null
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || '';
    this.globalData.userInfo = wx.getStorageSync('userInfo') || null;
  }
});
