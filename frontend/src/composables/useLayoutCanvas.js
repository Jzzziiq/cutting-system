import { ref, computed, watch, nextTick } from 'vue';

const PIECE_COLORS = [
  '#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6',
  '#ec4899', '#06b6d4', '#f97316', '#14b8a6', '#6366f1',
  '#84cc16', '#d946ef', '#0ea5e9', '#e11d48', '#65a30d'
];

export function useLayoutCanvas(canvasRef, options = {}) {
  const kerfWidth = ref(options.kerfWidth ?? 3);
  const gapDistance = ref(options.gapDistance ?? 3);
  const allowRotation = ref(options.allowRotation ?? true);

  const solutions = ref([]);
  const activeBoardIndex = ref(0);
  const zoom = ref(1);
  const panOffset = ref({ x: 0, y: 0 });
  const hoveredPiece = ref(null);
  const tooltipPos = ref({ x: 0, y: 0 });
  const tooltipData = ref(null);
  const showTooltip = ref(false);
  const canvasSize = ref({ w: 800, h: 500 });

  let isDragging = false;
  let dragStart = { x: 0, y: 0 };
  let panStart = { x: 0, y: 0 };
  let dpr = 1;
  let rafId = null;

  const currentSolution = computed(() => solutions.value[activeBoardIndex.value] || null);

  const boardCount = computed(() => solutions.value.length);

  const summary = computed(() => {
    if (!solutions.value.length) return { totalPieces: 0, boardsUsed: 0, offcutsUsed: 0, overallRate: 0, totalOffcutArea: 0 };
    let totalSquareArea = 0;
    let totalContainerArea = 0;
    let totalPieces = 0;
    for (const s of solutions.value) {
      totalPieces += (s.placeSquareList || []).length;
      totalSquareArea += (s.placeSquareList || []).reduce((sum, p) => sum + p.l * p.w, 0);
      const cw = s.containerWidth || s.instance?.W || 0;
      const cl = s.containerLength || s.instance?.L || 0;
      totalContainerArea += cw * cl;
    }
    const overallRate = totalContainerArea > 0 ? totalSquareArea / totalContainerArea : 0;
    const offcuts = solutions.value.filter(s => (s.rate || 0) < 0.3);
    const offcutArea = offcuts.reduce((sum, s) => {
      const cw = s.containerWidth || s.instance?.W || 0;
      const cl = s.containerLength || s.instance?.L || 0;
      const used = (s.placeSquareList || []).reduce((a, p) => a + p.l * p.w, 0);
      return sum + (cw * cl - used);
    }, 0);
    return {
      totalPieces,
      boardsUsed: solutions.value.length - offcuts.length,
      offcutsUsed: offcuts.length,
      overallRate,
      totalOffcutArea: offcutArea
    };
  });

  function getContainerDim(solution) {
    return {
      L: solution.containerLength || solution.instance?.L || 2440,
      W: solution.containerWidth || solution.instance?.W || 1220
    };
  }

  function initCanvas() {
    const canvas = canvasRef.value;
    if (!canvas) return;
    dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvasSize.value = { w: rect.width, h: rect.height };
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    canvas.style.width = rect.width + 'px';
    canvas.style.height = rect.height + 'px';
  }

  function worldToScreen(wx, wy, containerW) {
    return {
      x: wx * zoom.value + panOffset.value.x,
      y: (containerW - wy) * zoom.value + panOffset.value.y
    };
  }

  function screenToWorld(sx, sy, containerW) {
    return {
      x: (sx - panOffset.value.x) / zoom.value,
      y: containerW - (sy - panOffset.value.y) / zoom.value
    };
  }

  function fitToScreen() {
    const solution = currentSolution.value;
    if (!solution) return;
    const { L, W } = getContainerDim(solution);
    const padding = 60;
    const availW = canvasSize.value.w - padding * 2;
    const availH = canvasSize.value.h - padding * 2;
    const s = Math.min(availW / L, availH / W);
    zoom.value = s;
    panOffset.value = {
      x: (canvasSize.value.w - L * s) / 2,
      y: (canvasSize.value.h - W * s) / 2 + W * s
    };
  }

  function drawFrame() {
    const canvas = canvasRef.value;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const cw = canvasSize.value.w;
    const ch = canvasSize.value.h;

    ctx.save();
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, cw, ch);

    // Background
    ctx.fillStyle = '#f1f5f9';
    ctx.fillRect(0, 0, cw, ch);

    const solution = currentSolution.value;
    if (!solution) {
      ctx.fillStyle = '#94a3b8';
      ctx.font = '15px "Microsoft YaHei", sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('未加载排版结果', cw / 2, ch / 2);
      ctx.restore();
      return;
    }

    const { L, W } = getContainerDim(solution);
    const pieces = solution.placeSquareList || [];
    const rate = solution.rate || 0;

    // Board rectangle
    const boardColor = rate < 0.3 ? '#fef3c7' : '#ffffff';
    ctx.fillStyle = boardColor;
    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 2;
    const tl = worldToScreen(0, W, W);
    const br = worldToScreen(L, 0, W);
    const boardScreenW = br.x - tl.x;
    const boardScreenH = tl.y - br.y;
    ctx.fillRect(tl.x, br.y, boardScreenW, boardScreenH);
    ctx.strokeRect(tl.x, br.y, boardScreenW, boardScreenH);

    // Board info label
    ctx.fillStyle = '#172033';
    ctx.font = 'bold 13px "Microsoft YaHei", sans-serif';
    ctx.textAlign = 'left';
    const infoText = `板材 #${activeBoardIndex.value + 1}  ${L}×${W}mm  利用率 ${(rate * 100).toFixed(1)}%  工件 ${pieces.length} 个`;
    ctx.fillText(infoText, tl.x + 8, br.y - 8);

    // Pieces
    pieces.forEach((piece, i) => {
      const gap = kerfWidth.value / 2;
      const px = piece.x + gap;
      const py = piece.y + gap;
      const pl = piece.l - gap * 2;
      const pw = piece.w - gap * 2;
      if (pl <= 0 || pw <= 0) return;

      const pTl = worldToScreen(px, py + pw, W);
      const pBr = worldToScreen(px + pl, py, W);
      const pScreenW = pBr.x - pTl.x;
      const pScreenH = pTl.y - pBr.y;

      // Fill
      ctx.fillStyle = PIECE_COLORS[i % PIECE_COLORS.length];
      ctx.fillRect(pTl.x, pBr.y, pScreenW, pScreenH);

      // Border
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = Math.max(0.5, 1 / zoom.value);
      ctx.strokeRect(pTl.x, pBr.y, pScreenW, pScreenH);

      // Label
      const fontSize = Math.min(12 / zoom.value, Math.min(pScreenW, pScreenH) * 0.45);
      if (fontSize > 5 && pScreenW > 20 && pScreenH > 15) {
        ctx.fillStyle = '#fff';
        ctx.font = `${Math.max(8, fontSize)}px "Microsoft YaHei", sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        const label = `${pl.toFixed(0)}×${pw.toFixed(0)}`;
        const cx = pTl.x + pScreenW / 2;
        const cy = pBr.y + pScreenH / 2;
        if (pScreenW > ctx.measureText(label).width + 6) {
          ctx.fillText(label, cx, cy);
        }
      }
    });

    // Highlight hovered piece
    if (hoveredPiece.value) {
      const hp = hoveredPiece.value;
      const gap = kerfWidth.value / 2;
      const px = hp.x + gap;
      const py = hp.y + gap;
      const pl = hp.l - gap * 2;
      const pw = hp.w - gap * 2;
      const pTl = worldToScreen(px, py + pw, W);
      const pBr = worldToScreen(px + pl, py, W);

      ctx.strokeStyle = '#fbbf24';
      ctx.lineWidth = Math.max(2, 3 / zoom.value);
      ctx.strokeRect(pTl.x - 2, pBr.y - 2, pBr.x - pTl.x + 4, pTl.y - pBr.y + 4);
    }

    ctx.restore();
  }

  function requestDraw() {
    if (rafId) return;
    rafId = requestAnimationFrame(() => {
      rafId = null;
      drawFrame();
    });
  }

  function onResize() {
    initCanvas();
    requestDraw();
  }

  function onWheel(event) {
    const factor = event.deltaY < 0 ? 1.15 : 0.87;
    const newZoom = Math.min(5, Math.max(0.1, zoom.value * factor));
    const mx = event.offsetX;
    const my = event.offsetY;
    panOffset.value = {
      x: mx - (mx - panOffset.value.x) * (newZoom / zoom.value),
      y: my - (my - panOffset.value.y) * (newZoom / zoom.value)
    };
    zoom.value = newZoom;
    requestDraw();
  }

  function onMouseDown(event) {
    if (event.button !== 0) return;
    isDragging = true;
    dragStart = { x: event.offsetX, y: event.offsetY };
    panStart = { ...panOffset.value };
  }

  function onMouseMove(event) {
    if (isDragging) {
      const dx = event.offsetX - dragStart.x;
      const dy = event.offsetY - dragStart.y;
      panOffset.value = {
        x: panStart.x + dx,
        y: panStart.y + dy
      };
      requestDraw();
      return;
    }

    // Hover detection
    const solution = currentSolution.value;
    if (!solution) return;
    const { W } = getContainerDim(solution);
    const world = screenToWorld(event.offsetX, event.offsetY, W);
    const pieces = solution.placeSquareList || [];
    const gap = kerfWidth.value / 2;

    let found = null;
    for (let i = pieces.length - 1; i >= 0; i--) {
      const p = pieces[i];
      const px = p.x + gap;
      const py = p.y + gap;
      const pl = p.l - gap * 2;
      const pw = p.w - gap * 2;
      if (world.x >= px && world.x <= px + pl && world.y >= py && world.y <= py + pw) {
        found = p;
        break;
      }
    }

    if (found !== hoveredPiece.value) {
      hoveredPiece.value = found;
      requestDraw();
    }

    if (found) {
      tooltipPos.value = { x: event.offsetX + 12, y: event.offsetY + 12 };
      tooltipData.value = found;
      showTooltip.value = true;
    } else {
      showTooltip.value = false;
    }
  }

  function onMouseUp() {
    isDragging = false;
  }

  function onMouseLeave() {
    isDragging = false;
    hoveredPiece.value = null;
    showTooltip.value = false;
    requestDraw();
  }

  function switchBoard(index) {
    if (index >= 0 && index < solutions.value.length) {
      activeBoardIndex.value = index;
      fitToScreen();
      requestDraw();
    }
  }

  function loadSolutions(data) {
    solutions.value = Array.isArray(data) ? data : [];
    activeBoardIndex.value = 0;
    if (solutions.value.length > 0) {
      nextTick(() => {
        initCanvas();
        fitToScreen();
        requestDraw();
      });
    }
  }

  function exportSVG() {
    const solution = currentSolution.value;
    if (!solution) return;
    const { L, W } = getContainerDim(solution);
    const pieces = solution.placeSquareList || [];
    const gap = kerfWidth.value / 2;

    let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${L} ${W}" width="${L}" height="${W}">`;
    svg += `<rect x="0" y="0" width="${L}" height="${W}" fill="#fff" stroke="#000" stroke-width="2"/>`;
    pieces.forEach((p, i) => {
      const px = p.x + gap;
      const py = p.y + gap;
      const pl = p.l - gap * 2;
      const pw = p.w - gap * 2;
      const transformY = W - py - pw;
      svg += `<rect x="${px}" y="${transformY}" width="${pl}" height="${pw}" fill="${PIECE_COLORS[i % PIECE_COLORS.length]}" stroke="#fff" stroke-width="0.5"/>`;
      svg += `<text x="${px + pl / 2}" y="${transformY + pw / 2}" text-anchor="middle" dominant-baseline="central" fill="#fff" font-size="10">${pl.toFixed(0)}×${pw.toFixed(0)}</text>`;
    });
    svg += '</svg>';
    return svg;
  }

  return {
    kerfWidth,
    gapDistance,
    allowRotation,
    solutions,
    activeBoardIndex,
    zoom,
    panOffset,
    hoveredPiece,
    showTooltip,
    tooltipPos,
    tooltipData,
    canvasSize,
    currentSolution,
    boardCount,
    summary,
    initCanvas,
    fitToScreen,
    drawFrame: requestDraw,
    onResize,
    onWheel,
    onMouseDown,
    onMouseMove,
    onMouseUp,
    onMouseLeave,
    switchBoard,
    loadSolutions,
    exportSVG
  };
}
