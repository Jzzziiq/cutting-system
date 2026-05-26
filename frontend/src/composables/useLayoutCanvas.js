import { ref, computed, watch, nextTick } from 'vue';

const PIECE_COLORS = [
  '#6f8f8c', '#a47f7f', '#7f8fb3', '#b39a6a', '#8a7fb0',
  '#7ea078', '#b07a99', '#6f9ba8', '#ad8370', '#9fa56e',
  '#7188a4', '#a283aa', '#8b9672', '#8c7f72', '#77958f'
];

const BOARD_FILL = '#ffffff';
const OFFCUT_BOARD_FILL = '#f8fafc';
const BOARD_STROKE = '#94a3b8';
const PIECE_STROKE = '#f8fafc';
const DARK_TEXT = '#172033';
const LIGHT_TEXT = '#ffffff';
const TOOLTIP_WIDTH = 372;
const TOOLTIP_HEIGHT = 232;
const TOOLTIP_GAP = 14;
const TOOLTIP_MARGIN = 12;

function hexToRgb(hex) {
  const value = hex.replace('#', '');
  return {
    r: parseInt(value.slice(0, 2), 16),
    g: parseInt(value.slice(2, 4), 16),
    b: parseInt(value.slice(4, 6), 16)
  };
}

function getRelativeLuminance(hex) {
  const { r, g, b } = hexToRgb(hex);
  const channels = [r, g, b].map((channel) => {
    const normalized = channel / 255;
    return normalized <= 0.03928
      ? normalized / 12.92
      : ((normalized + 0.055) / 1.055) ** 2.4;
  });
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

function getContrastRatio(luminanceA, luminanceB) {
  const lighter = Math.max(luminanceA, luminanceB);
  const darker = Math.min(luminanceA, luminanceB);
  return (lighter + 0.05) / (darker + 0.05);
}

function getReadableTextColor(backgroundColor) {
  const backgroundLuminance = getRelativeLuminance(backgroundColor);
  const darkContrast = getContrastRatio(backgroundLuminance, getRelativeLuminance(DARK_TEXT));
  const lightContrast = getContrastRatio(backgroundLuminance, getRelativeLuminance(LIGHT_TEXT));
  return darkContrast >= lightContrast ? DARK_TEXT : LIGHT_TEXT;
}

function colorDistance(colorA, colorB) {
  const a = hexToRgb(colorA);
  const b = hexToRgb(colorB);
  return Math.hypot(a.r - b.r, a.g - b.g, a.b - b.b);
}

function stableHash(value) {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = ((hash << 5) - hash + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

function getPieceSizeKey(piece) {
  const l = Number(piece?.l || 0).toFixed(1);
  const w = Number(piece?.w || 0).toFixed(1);
  return `${l}x${w}`;
}

function getPieceRect(piece) {
  const x = Number(piece?.x || 0);
  const y = Number(piece?.y || 0);
  const l = Number(piece?.l || 0);
  const w = Number(piece?.w || 0);
  return { x, y, l, w, right: x + l, top: y + w };
}

function rangesOverlap(startA, endA, startB, endB) {
  return Math.min(endA, endB) > Math.max(startA, startB);
}

function arePiecesAdjacent(pieceA, pieceB, tolerance) {
  const a = getPieceRect(pieceA);
  const b = getPieceRect(pieceB);
  const touchesVertically = Math.abs(a.top - b.y) <= tolerance || Math.abs(b.top - a.y) <= tolerance;
  const touchesHorizontally = Math.abs(a.right - b.x) <= tolerance || Math.abs(b.right - a.x) <= tolerance;
  return (touchesVertically && rangesOverlap(a.x, a.right, b.x, b.right))
    || (touchesHorizontally && rangesOverlap(a.y, a.top, b.y, b.top));
}

function buildPieceColorMap(solutions, tolerance) {
  const entries = [];
  const firstSeenIndex = new Map();
  const adjacency = new Map();

  for (let boardIndex = 0; boardIndex < solutions.length; boardIndex++) {
    const solution = solutions[boardIndex];
    for (const piece of solution.placeSquareList || []) {
      const key = getPieceSizeKey(piece);
      if (!firstSeenIndex.has(key)) {
        firstSeenIndex.set(key, entries.length);
        adjacency.set(key, new Set());
      }
      entries.push({ key, piece, boardIndex });
    }
  }

  for (let i = 0; i < entries.length; i++) {
    for (let j = i + 1; j < entries.length; j++) {
      const a = entries[i];
      const b = entries[j];
      if (a.boardIndex !== b.boardIndex || a.key === b.key || !arePiecesAdjacent(a.piece, b.piece, tolerance)) continue;
      adjacency.get(a.key).add(b.key);
      adjacency.get(b.key).add(a.key);
    }
  }

  const assignedColors = new Map();
  const colorUseCount = new Map();
  const sizeKeys = [...firstSeenIndex.keys()].sort((a, b) => {
    const adjacencyDelta = (adjacency.get(b)?.size || 0) - (adjacency.get(a)?.size || 0);
    return adjacencyDelta || firstSeenIndex.get(a) - firstSeenIndex.get(b);
  });

  for (const key of sizeKeys) {
    const neighborColors = [...(adjacency.get(key) || [])]
      .map(neighborKey => assignedColors.get(neighborKey))
      .filter(Boolean);
    const preferredIndex = stableHash(key) % PIECE_COLORS.length;

    let bestColor = PIECE_COLORS[preferredIndex];
    let bestScore = Number.NEGATIVE_INFINITY;

    PIECE_COLORS.forEach((color, index) => {
      const nearestNeighborDistance = neighborColors.length
        ? Math.min(...neighborColors.map(neighborColor => colorDistance(color, neighborColor)))
        : 180;
      const reusePenalty = (colorUseCount.get(color) || 0) * 18;
      const stableBias = index === preferredIndex ? 6 : 0;
      const score = nearestNeighborDistance - reusePenalty + stableBias;
      if (score > bestScore) {
        bestScore = score;
        bestColor = color;
      }
    });

    assignedColors.set(key, bestColor);
    colorUseCount.set(bestColor, (colorUseCount.get(bestColor) || 0) + 1);
  }

  return new Map(
    [...firstSeenIndex.keys()].map((key) => {
      const fill = assignedColors.get(key) || PIECE_COLORS[stableHash(key) % PIECE_COLORS.length];
      return [key, { fill, text: getReadableTextColor(fill) }];
    })
  );
}

export function useLayoutCanvas(canvasRef, options = {}) {
  const kerfWidth = ref(options.kerfWidth ?? 3);
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

  const pieceColorMap = computed(() => buildPieceColorMap(
    solutions.value,
    kerfWidth.value + 0.5
  ));

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

  function getPieceVisual(piece) {
    const fallbackFill = PIECE_COLORS[stableHash(getPieceSizeKey(piece)) % PIECE_COLORS.length];
    return pieceColorMap.value.get(getPieceSizeKey(piece)) || {
      fill: fallbackFill,
      text: getReadableTextColor(fallbackFill)
    };
  }

  function getPieceDimensionLabel(piece) {
    const l = Number(piece?.l || 0);
    const w = Number(piece?.w || 0);
    return `${l.toFixed(0)}×${w.toFixed(0)}`;
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
    const availW = Math.max(1, canvasSize.value.w - padding * 2);
    const availH = Math.max(1, canvasSize.value.h - padding * 2);
    const s = Math.min(availW / L, availH / W);
    if (!Number.isFinite(s) || s <= 0) return;
    zoom.value = s;
    panOffset.value = {
      x: (canvasSize.value.w - L * s) / 2,
      y: (canvasSize.value.h - W * s) / 2
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
    const boardColor = rate < 0.3 ? OFFCUT_BOARD_FILL : BOARD_FILL;
    ctx.fillStyle = boardColor;
    ctx.strokeStyle = BOARD_STROKE;
    ctx.lineWidth = 2;
    const tl = worldToScreen(0, W, W);
    const br = worldToScreen(L, 0, W);
    const boardScreenW = br.x - tl.x;
    const boardScreenH = br.y - tl.y;
    ctx.fillRect(tl.x, tl.y, boardScreenW, boardScreenH);
    ctx.strokeRect(tl.x, tl.y, boardScreenW, boardScreenH);

    // Board info label
    ctx.fillStyle = '#172033';
    ctx.font = 'bold 13px "Microsoft YaHei", sans-serif';
    ctx.textAlign = 'left';
    const bgLabel = solution._boardGroup
      ? [solution._boardGroup.brand, solution._boardGroup.materialType, solution._boardGroup.color].filter(Boolean).join(' ')
      : '';
    const infoText = bgLabel
      ? `${bgLabel}  板材 #${activeBoardIndex.value + 1}  ${L}×${W}mm  利用率 ${(rate * 100).toFixed(1)}%  工件 ${pieces.length} 个`
      : `板材 #${activeBoardIndex.value + 1}  ${L}×${W}mm  利用率 ${(rate * 100).toFixed(1)}%  工件 ${pieces.length} 个`;
    ctx.fillText(infoText, tl.x + 8, Math.max(16, tl.y - 8));

    // Pieces
    pieces.forEach((piece) => {
      const px = piece.x;
      const py = piece.y;
      const pl = piece.l;
      const pw = piece.w;
      if (pl <= 0 || pw <= 0) return;

      const pTl = worldToScreen(px, py + pw, W);
      const pBr = worldToScreen(px + pl, py, W);
      const pScreenW = pBr.x - pTl.x;
      const pScreenH = pBr.y - pTl.y;

      // Fill
      const pieceVisual = getPieceVisual(piece);
      ctx.fillStyle = pieceVisual.fill;
      ctx.fillRect(pTl.x, pTl.y, pScreenW, pScreenH);

      // Border
      ctx.strokeStyle = PIECE_STROKE;
      ctx.lineWidth = Math.max(0.5, 1 / zoom.value);
      ctx.strokeRect(pTl.x, pTl.y, pScreenW, pScreenH);

      // Label
      const fontSize = Math.min(12 / zoom.value, Math.min(pScreenW, pScreenH) * 0.45);
      if (fontSize > 5 && pScreenW > 20 && pScreenH > 15) {
        ctx.fillStyle = pieceVisual.text;
        ctx.font = `${Math.max(8, fontSize)}px "Microsoft YaHei", sans-serif`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        const label = getPieceDimensionLabel(piece);
        const cx = pTl.x + pScreenW / 2;
        const cy = pTl.y + pScreenH / 2;
        if (pScreenW > ctx.measureText(label).width + 6) {
          ctx.fillText(label, cx, cy);
        }
      }
    });

    // Highlight hovered piece
    if (hoveredPiece.value) {
      const hp = hoveredPiece.value;
      const px = hp.x;
      const py = hp.y;
      const pl = hp.l;
      const pw = hp.w;
      const pTl = worldToScreen(px, py + pw, W);
      const pBr = worldToScreen(px + pl, py, W);

      ctx.strokeStyle = '#fbbf24';
      ctx.lineWidth = Math.max(2, 3 / zoom.value);
      ctx.strokeRect(pTl.x - 2, pTl.y - 2, pBr.x - pTl.x + 4, pBr.y - pTl.y + 4);
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
    if (currentSolution.value) {
      fitToScreen();
    }
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

    let found = null;
    for (let i = pieces.length - 1; i >= 0; i--) {
      const p = pieces[i];
      const px = p.x;
      const py = p.y;
      const pl = p.l;
      const pw = p.w;
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
      let tooltipX = event.offsetX + TOOLTIP_GAP;
      let tooltipY = event.offsetY + TOOLTIP_GAP;

      if (tooltipX + TOOLTIP_WIDTH + TOOLTIP_MARGIN > canvasSize.value.w) {
        tooltipX = event.offsetX - TOOLTIP_WIDTH - TOOLTIP_GAP;
      }
      if (tooltipY + TOOLTIP_HEIGHT + TOOLTIP_MARGIN > canvasSize.value.h) {
        tooltipY = event.offsetY - TOOLTIP_HEIGHT - TOOLTIP_GAP;
      }

      const canvasRect = canvasRef.value?.getBoundingClientRect();
      if (canvasRect) {
        const rightOverflow = canvasRect.left + tooltipX + TOOLTIP_WIDTH + TOOLTIP_MARGIN - window.innerWidth;
        const bottomOverflow = canvasRect.top + tooltipY + TOOLTIP_HEIGHT + TOOLTIP_MARGIN - window.innerHeight;
        if (rightOverflow > 0) {
          tooltipX -= rightOverflow;
        }
        if (bottomOverflow > 0) {
          tooltipY -= bottomOverflow;
        }
      }

      tooltipPos.value = {
        x: Math.max(TOOLTIP_MARGIN, tooltipX),
        y: Math.max(TOOLTIP_MARGIN, tooltipY)
      };
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
    const boardFill = (solution.rate || 0) < 0.3 ? OFFCUT_BOARD_FILL : BOARD_FILL;

    let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${L} ${W}" width="${L}" height="${W}">`;
    svg += `<rect x="0" y="0" width="${L}" height="${W}" fill="${boardFill}" stroke="${BOARD_STROKE}" stroke-width="2"/>`;
    pieces.forEach((p) => {
      const px = p.x;
      const py = p.y;
      const pl = p.l;
      const pw = p.w;
      const transformY = W - py - pw;
      const pieceVisual = getPieceVisual(p);
      svg += `<rect x="${px}" y="${transformY}" width="${pl}" height="${pw}" fill="${pieceVisual.fill}" stroke="${PIECE_STROKE}" stroke-width="0.5"/>`;
      svg += `<text x="${px + pl / 2}" y="${transformY + pw / 2}" text-anchor="middle" dominant-baseline="central" fill="${pieceVisual.text}" font-size="10">${getPieceDimensionLabel(p)}</text>`;
    });
    svg += '</svg>';
    return svg;
  }

  return {
    kerfWidth,
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
