const api = require('../../services/api');

Page({
  data: {
    loading: false,
    form: {
      L: 100,
      W: 50,
      rotateEnable: false,
      gapDistance: 0
    },
    squares: [
      { id: 'item1', l: 20, w: 15 },
      { id: 'item2', l: 30, w: 20 }
    ],
    solutions: [],
    displayRate: 0
  },

  onReady() {
    this.drawLayout();
  },

  onFormInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  onRotateChange(e) {
    this.setData({ 'form.rotateEnable': e.detail.value });
  },

  onSquareInput(e) {
    const index = e.currentTarget.dataset.index;
    const field = e.currentTarget.dataset.field;
    this.setData({ [`squares[${index}].${field}`]: e.detail.value });
  },

  addSquare() {
    const nextIndex = this.data.squares.length + 1;
    this.setData({
      squares: this.data.squares.concat([{ id: `item${nextIndex}`, l: '', w: '' }])
    });
  },

  removeSquare(e) {
    const index = e.currentTarget.dataset.index;
    const squares = this.data.squares.filter((_, i) => i !== index);
    this.setData({ squares });
  },

  buildPayload() {
    const form = this.data.form;
    return {
      L: Number(form.L),
      W: Number(form.W),
      rotateEnable: !!form.rotateEnable,
      gapDistance: Number(form.gapDistance || 0),
      squareList: this.data.squares.map((square, index) => ({
        id: square.id || `item${index + 1}`,
        l: Number(square.l),
        w: Number(square.w)
      }))
    };
  },

  validate(payload) {
    if (payload.L <= 0 || payload.W <= 0 || payload.gapDistance < 0) {
      wx.showToast({ title: '请填写正确的容器参数', icon: 'none' });
      return false;
    }
    if (!payload.squareList.length || payload.squareList.some(square => square.l <= 0 || square.w <= 0)) {
      wx.showToast({ title: '请填写正确的矩形尺寸', icon: 'none' });
      return false;
    }
    return true;
  },

  async onSolve() {
    const payload = this.buildPayload();
    if (!this.validate(payload)) return;

    this.setData({ loading: true });
    try {
      const solutions = await api.solveAlgorithm(payload);
      const normalized = (solutions || []).map(solution => ({
        ...solution,
        ratePercent: ((solution.rate || 0) * 100).toFixed(2)
      }));
      this.setData({
        solutions: normalized,
        displayRate: normalized.length ? normalized[0].ratePercent : 0
      });
      this.drawLayout();
    } finally {
      this.setData({ loading: false });
    }
  },

  drawLayout() {
    const solution = this.data.solutions[0];
    const ctx = wx.createCanvasContext('layoutCanvas', this);
    const canvasWidth = 320;
    const canvasHeight = 220;
    ctx.clearRect(0, 0, canvasWidth, canvasHeight);

    if (!solution) {
      ctx.draw();
      return;
    }

    const padding = 16;
    const containerL = Number(solution.containerLength);
    const containerW = Number(solution.containerWidth);
    const scale = Math.min((canvasWidth - padding * 2) / containerL, (canvasHeight - padding * 2) / containerW);
    const drawW = containerL * scale;
    const drawH = containerW * scale;
    const originX = padding;
    const originY = padding;

    ctx.setStrokeStyle('#111827');
    ctx.setLineWidth(1);
    ctx.strokeRect(originX, originY, drawW, drawH);

    const colors = ['#93c5fd', '#86efac', '#fde68a', '#fca5a5', '#c4b5fd', '#67e8f9'];
    (solution.placeSquareList || []).forEach((square, index) => {
      const x = originX + square.x * scale;
      const y = originY + drawH - (square.y + square.w) * scale;
      const w = square.l * scale;
      const h = square.w * scale;
      ctx.setFillStyle(colors[index % colors.length]);
      ctx.fillRect(x, y, w, h);
      ctx.setStrokeStyle('#374151');
      ctx.strokeRect(x, y, w, h);
      ctx.setFillStyle('#111827');
      ctx.setFontSize(10);
      ctx.fillText(String(index + 1), x + 4, y + 12);
    });

    ctx.draw();
  }
});
