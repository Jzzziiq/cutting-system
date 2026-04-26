const api = require('../../services/api');

Page({
  data: {
    id: '',
    saving: false,
    form: {
      brand: '',
      materialType: '',
      color: '',
      sizeType: '',
      width: '',
      length: '',
      thickness: '',
      remark: ''
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id });
      this.loadBoard(options.id);
    }
  },

  async loadBoard(id) {
    const board = await api.getBoard(id);
    this.setData({
      form: {
        brand: board.brand || '',
        materialType: board.materialType || '',
        color: board.color || '',
        sizeType: board.sizeType || '',
        width: board.width || '',
        length: board.length || '',
        thickness: board.thickness || '',
        remark: board.remark || ''
      }
    });
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  normalizeForm() {
    const form = { ...this.data.form };
    form.width = Number(form.width);
    form.length = Number(form.length);
    form.thickness = Number(form.thickness);
    return form;
  },

  validate(form) {
    return form.brand && form.materialType && form.color && form.sizeType &&
      form.width > 0 && form.length > 0 && form.thickness > 0;
  },

  async onSubmit() {
    const { id } = this.data;
    const form = this.normalizeForm();
    if (!this.validate(form)) {
      wx.showToast({ title: '请完整填写板材信息', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    try {
      if (id) {
        await api.updateBoard(id, form);
      } else {
        await api.createBoard(form);
      }
      wx.showToast({ title: '已保存' });
      wx.navigateBack();
    } finally {
      this.setData({ saving: false });
    }
  }
});
