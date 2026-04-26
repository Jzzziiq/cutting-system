const api = require('../../services/api');

Page({
  data: {
    id: '',
    saving: false,
    form: {
      customerName: '',
      phone: '',
      address: '',
      remark: ''
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id });
      this.loadCustomer(options.id);
    }
  },

  async loadCustomer(id) {
    const customer = await api.getCustomer(id);
    this.setData({
      form: {
        customerName: customer.customerName || '',
        phone: customer.phone || '',
        address: customer.address || '',
        remark: customer.remark || ''
      }
    });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  async onSubmit() {
    const { id, form } = this.data;
    if (!form.customerName || !form.phone) {
      wx.showToast({ title: '请填写客户名称和电话', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    try {
      if (id) {
        await api.updateCustomer(id, form);
      } else {
        await api.createCustomer(form);
      }
      wx.showToast({ title: '已保存' });
      wx.navigateBack();
    } finally {
      this.setData({ saving: false });
    }
  }
});
