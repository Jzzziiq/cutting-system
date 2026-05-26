const api = require('../../services/api');

Page({
  data: {
    id: '',
    detail: null,
    task: null,
    order: null,
    layoutResult: null,
    items: [],
    loading: false,
    transitioning: false
  },
  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id });
      this.loadDetail(options.id);
    }
  },
  async loadDetail(taskId) {
    this.setData({ loading: true });
    try {
      const detail = await api.getMyTaskDetail(taskId);
      const task = detail.task || {};
      const order = detail.order || {};
      const layoutResult = detail.layoutResult || {};
      const items = order.items || [];
      this.setData({ detail, task, order, layoutResult, items });
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ loading: false });
    }
  },
  async onStart() {
    await this.transition(1, '开始生产');
  },
  async onComplete() {
    await this.transition(2, '完成任务');
  },
  async transition(status, label) {
    this.setData({ transitioning: true });
    try {
      await api.transitionTask(this.data.id, status);
      wx.showToast({ title: label + '成功' });
      this.loadDetail(this.data.id);
    } catch (err) {
      // 错误已在 request.js 中处理
    } finally {
      this.setData({ transitioning: false });
    }
  },
  onDownloadNc() {
    const ncFilePath = this.data.layoutResult.ncFilePath;
    if (!ncFilePath) {
      wx.showToast({ title: 'NC文件不存在', icon: 'none' });
      return;
    }
    // ncFilePath 存储的是相对路径，拼接为完整的 HTTP URL
    const url = 'http://localhost:8080/uploads/' + ncFilePath;
    wx.downloadFile({
      url,
      success(res) {
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            fail() {
              wx.showToast({ title: '无法打开文件', icon: 'none' });
            }
          });
        }
      },
      fail() {
        wx.showToast({ title: '下载失败', icon: 'none' });
      }
    });
  }
});
