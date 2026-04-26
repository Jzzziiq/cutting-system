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
      content: '确认删除该客户？',
      success: async res => {
        if (!res.confirm) return;
        await api.deleteCustomer(id);
        wx.showToast({ title: '已删除' });
        this.loadCustomers();
      }
    });
  },

  onPullDownRefresh() {
    this.loadCustomers().finally(() => wx.stopPullDownRefresh());
  }
});
