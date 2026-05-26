const api = require('../../services/api');

Page({
  data: {
    tasks: [],
    loading: false,
    unreadCount: 0
  },
  onShow() {
    this.loadTasks();
    this.loadUnreadCount();
  },
  async loadTasks() {
    this.setData({ loading: true });
    try {
      const tasks = await api.listMyTasks();
      this.setData({ tasks: Array.isArray(tasks) ? tasks : (tasks.records || []) });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  },
  async loadUnreadCount() {
    try {
      const count = await api.getUnreadCount();
      this.setData({ unreadCount: count || 0 });
    } catch (err) {
      // 静默失败
    }
  },
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/tasks/detail?id=${id}` });
  },
  goNotifications() {
    wx.navigateTo({ url: '/pages/notifications/index' });
  },
  onPullDownRefresh() {
    this.loadTasks().finally(() => wx.stopPullDownRefresh());
  }
});
