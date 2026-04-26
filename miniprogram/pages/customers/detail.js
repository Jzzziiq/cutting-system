const api = require('../../services/api');

Page({
  data: {
    id: '',
    customer: {}
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadCustomer(options.id);
  },

  async loadCustomer(id) {
    const customer = await api.getCustomer(id);
    this.setData({ customer });
  },

  goEdit() {
    wx.navigateTo({ url: `/pages/customers/edit?id=${this.data.id}` });
  }
});
