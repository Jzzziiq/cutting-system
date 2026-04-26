const api = require('../../services/api');

Page({
  data: {
    id: '',
    board: {}
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadBoard(options.id);
  },

  async loadBoard(id) {
    const board = await api.getBoard(id);
    this.setData({ board });
  },

  goEdit() {
    wx.navigateTo({ url: `/pages/boards/edit?id=${this.data.id}` });
  }
});
