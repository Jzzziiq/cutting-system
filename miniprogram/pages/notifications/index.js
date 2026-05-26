const api = require('../../services/api');

Page({
  data: {
    notifications: [],
    pageNum: 1,
    pageSize: 20,
    total: 0,
    loading: false
  },
  onShow() {
    this.loadNotifications();
  },
  async loadNotifications() {
    this.setData({ loading: true });
    try {
      const res = await api.listNotifications(this.data.pageNum, this.data.pageSize);
      const records = res.records || res || [];
      this.setData({
        notifications: records,
        total: res.total || records.length
      });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  },
  async onTapNotification(e) {
    const item = e.currentTarget.dataset.item;
    // 标记已读
    if (!item.isRead) {
      try {
        await api.markNotificationRead(item.id);
        // 更新本地状态
        const notifications = this.data.notifications.map(n => {
          if (n.id === item.id) return { ...n, isRead: 1 };
          return n;
        });
        this.setData({ notifications });
      } catch (err) {
        // 静默失败
      }
    }
    // 跳转到任务详情
    if (item.taskId) {
      wx.navigateTo({ url: `/pages/tasks/detail?id=${item.taskId}` });
    }
  },
  async onMarkAllRead() {
    try {
      await api.markAllNotificationsRead();
      const notifications = this.data.notifications.map(n => ({ ...n, isRead: 1 }));
      this.setData({ notifications });
      wx.showToast({ title: '全部已读' });
    } catch (err) {
      // 错误已在 request.js 中处理
    }
  },
  onPullDownRefresh() {
    this.loadNotifications().finally(() => wx.stopPullDownRefresh());
  }
});
