const api = require('../../services/api');

Page({
  data: {
    pageNum: 1,
    pageSize: 20,
    boards: [],
    total: 0
  },

  onShow() {
    this.loadBoards();
  },

  async loadBoards() {
    const { pageNum, pageSize } = this.data;
    const page = await api.listBoards(pageNum, pageSize);
    this.setData({
      boards: page.records || [],
      total: page.total || 0
    });
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/boards/edit' });
  },

  goEdit(e) {
    wx.navigateTo({ url: `/pages/boards/edit?id=${e.currentTarget.dataset.id}` });
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/boards/detail?id=${e.currentTarget.dataset.id}` });
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除板材',
      content: '确认删除该板材？',
      success: async res => {
        if (!res.confirm) return;
        await api.deleteBoard(id);
        wx.showToast({ title: '已删除' });
        this.loadBoards();
      }
    });
  },

  onPullDownRefresh() {
    this.loadBoards().finally(() => wx.stopPullDownRefresh());
  }
});
