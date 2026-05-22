const api = require('../../services/api');

Page({
  data: {
    pageNum: 1,
    pageSize: 20,
    customers: [],
    total: 0
  },

  onShow() {
    this.loadCustomers();
  },

  async loadCustomers() {
    const { pageNum, pageSize } = this.data;
    const page = await api.listCustomers(pageNum, pageSize);
    this.setData({
      customers: page.records || [],
      total: page.total || 0
    });
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/customers/edit' });
  },

  goEdit(e) {
    wx.navigateTo({ url: `/pages/customers/edit?id=${e.currentTarget.dataset.id}` });
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/customers/detail?id=${e.currentTarget.dataset.id}` });
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除客户',
      content: '确认删除该客户？若已有订单引用，系统会阻止删除以保护订单数据，可改为禁用。',
      success: async res => {
        if (!res.confirm) return;
        try {
          await api.deleteCustomer(id);
          wx.showToast({ title: '已删除' });
          this.loadCustomers();
        } catch (e) {
          // request.js 已展示错误 toast。
        }
      }
    });
  },

  onPullDownRefresh() {
    this.loadCustomers().finally(() => wx.stopPullDownRefresh());
  }
});
