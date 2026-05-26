export const sceneDragModeOptions = [
  { value: 'xz', label: '平面' },
  { value: 'x', label: 'X' },
  { value: 'y', label: 'Y' },
  { value: 'z', label: 'Z' }
];

export const SNAP_THRESHOLD = 5;

export const defaultWizardByCategory = {
  wardrobe: { width: 1200, height: 2200, depth: 600, shelfCount: 2, doorCount: 2 },
  'base-cabinet': { width: 800, height: 800, depth: 500, shelfCount: 1, doorCount: 2 }
};

export const categoryLabels = {
  wardrobe: '衣柜',
  'base-cabinet': '地柜'
};

export const materialSlotLabels = {
  cabinet_body: '柜体板',
  door: '门板',
  back: '背板'
};

export const boardTypeLabels = {
  side: '侧板',
  layer: '层板',
  door: '门板',
  back: '背板',
  top: '顶板',
  bottom: '底板'
};

export const grainOptions = [
  { value: 'none', label: '无纹理' },
  { value: 'vertical', label: '竖纹' },
  { value: 'horizontal', label: '横纹' }
];

export const placementFaceOptions = [
  { value: 'inner', label: '内部' },
  { value: 'left', label: '左侧' },
  { value: 'right', label: '右侧' },
  { value: 'front', label: '正面' },
  { value: 'back', label: '背面' },
  { value: 'top', label: '顶部' },
  { value: 'bottom', label: '底部' }
];

export const edgeLabels = {
  left: '左',
  right: '右',
  top: '上',
  bottom: '下'
};

export const freeBoardParts = [
  {
    type: 'side',
    label: '侧板',
    materialSlot: 'cabinet_body',
    designLength: 2200,
    designWidth: 600,
    thickness: 18,
    placementFace: 'left',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: true, bottom: true }
  },
  {
    type: 'layer',
    label: '层板',
    materialSlot: 'cabinet_body',
    designLength: 800,
    designWidth: 560,
    thickness: 18,
    placementFace: 'inner',
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: false, bottom: false }
  },
  {
    type: 'door',
    label: '门板',
    materialSlot: 'door',
    designLength: 2150,
    designWidth: 400,
    thickness: 18,
    placementFace: 'front',
    grain: 'vertical',
    edgeBanding: { left: true, right: true, top: true, bottom: true },
    hingeHoles: [{
      edge: 'left',
      count: 3,
      spacing: 'even',
      diameter: 35,
      depth: 12,
      doorGap: 2,
      edgeDistance: 22,
      direction: 'height',
      opening: 'left'
    }]
  },
  {
    type: 'back',
    label: '背板',
    materialSlot: 'back',
    designLength: 800,
    designWidth: 2000,
    thickness: 5,
    placementFace: 'back',
    grain: 'vertical',
    edgeBanding: { left: false, right: false, top: false, bottom: false }
  },
  {
    type: 'top',
    label: '顶板',
    materialSlot: 'cabinet_body',
    designLength: 800,
    designWidth: 560,
    thickness: 18,
    placementFace: 'top',
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: false, bottom: true }
  },
  {
    type: 'bottom',
    label: '底板',
    materialSlot: 'cabinet_body',
    designLength: 800,
    designWidth: 560,
    thickness: 18,
    placementFace: 'bottom',
    grain: 'horizontal',
    edgeBanding: { left: false, right: false, top: true, bottom: false }
  }
];

export const cabinetPanelThicknessRange = { min: 12, max: 25 };

export const materialSlotThicknessRanges = {
  cabinet_body: cabinetPanelThicknessRange,
  door: cabinetPanelThicknessRange,
  back: { min: 3, max: 9 }
};

export const boardColorPalette = [
  { keyword: '暖白', color: '#f8fafc' },
  { keyword: '白', color: '#f1f5f9' },
  { keyword: '深灰', color: '#64748b' },
  { keyword: '灰', color: '#94a3b8' },
  { keyword: '黑', color: '#1f2937' },
  { keyword: '胡桃', color: '#8b5a2b' },
  { keyword: '原木', color: '#d6a15f' },
  { keyword: '木', color: '#b7791f' },
  { keyword: '红', color: '#b91c1c' },
  { keyword: '蓝', color: '#2563eb' },
  { keyword: '绿', color: '#0f766e' },
  { keyword: '黄', color: '#ca8a04' }
];

export const splitColumns = [
  { prop: 'cabinetName', label: '柜体', minWidth: 90 },
  { prop: 'partCode', label: '工件编号', minWidth: 100 },
  { prop: 'partName', label: '名称', minWidth: 80 },
  { prop: 'boardType', label: '类型', minWidth: 60 },
  { prop: 'materialName', label: '材质', minWidth: 100 },
  { prop: 'length', label: '切割长', minWidth: 70 },
  { prop: 'width', label: '切割宽', minWidth: 70 },
  { prop: 'thickness', label: '厚度', minWidth: 50 }
];

// 预设柜体模板
// T=18mm 柜体板, T_b=5mm 背板
// 坐标系: 原点在柜体底面中心, Y 向上, Z 向前
const T = 18;
const T_B = 5;

function b(id, type, displayName, materialSlot, designLength, designWidth, thickness, position, placementFace, opts = {}) {
  return {
    id, type, displayName, materialSlot, boardId: null,
    thickness, designLength, designWidth,
    grain: opts.grain || 'none',
    position, rotation: { x: 0, y: 0, z: 0 },
    placementFace,
    connectedTo: opts.connectedTo || [],
    edgeBanding: opts.edgeBanding || {},
    edgeRole: opts.edgeRole || {},
    hingeHoles: opts.hingeHoles || []
  };
}

// 抽屉体生成器：5 件（左侧板、右侧板、底板、后板、前横板）
// cx,cy,cz = 面板中心坐标, containerW = 格子内宽, fh = 面板高, D = 柜深
let _did = 0;
function drawerBody(cx, cy, cz, containerW, fh, D, prefix) {
  _did++;
  const bodyW = containerW - 40; // 每边 20mm 导轨空间
  const bodyD = Math.round(D * 0.75); // 柜深的 75%（导轨行程）
  const faceBackZ = cz - T / 2; // 面板背面 z
  const bodyFrontZ = faceBackZ - 2; // 体前缘（面板后 2mm 间隙）
  const bodyZ = bodyFrontZ - bodyD / 2; // 体中心 z
  const backZ = bodyFrontZ - bodyD + T_B / 2; // 后板中心 z
  return [
    b(`${prefix}-L${_did}`, 'side', `抽屉左侧板${_did}`, 'cabinet_body', fh, bodyD, T,
      { x: cx - bodyW / 2 + T / 2, y: cy, z: bodyZ }, 'inner', { grain: 'vertical' }),
    b(`${prefix}-R${_did}`, 'side', `抽屉右侧板${_did}`, 'cabinet_body', fh, bodyD, T,
      { x: cx + bodyW / 2 - T / 2, y: cy, z: bodyZ }, 'inner', { grain: 'vertical' }),
    b(`${prefix}-Bm${_did}`, 'layer', `抽屉底板${_did}`, 'cabinet_body', bodyW, bodyD, T,
      { x: cx, y: cy - fh / 2 + T / 2, z: bodyZ }, 'inner', { grain: 'horizontal' }),
    b(`${prefix}-Bk${_did}`, 'back', `抽屉后板${_did}`, 'back', bodyW - 2, fh - 2, T_B,
      { x: cx, y: cy, z: backZ }, 'back'),
    b(`${prefix}-Fr${_did}`, 'back', `抽屉前面板${_did}`, 'back', bodyW - 2, fh - 2, T_B,
      { x: cx, y: cy, z: bodyFrontZ + T_B / 2 }, 'back')
  ];
}

export const presetCabinets = [
  // ── 1. 衣柜 A: 标准两门衣柜 (挂衣+层板) ──
  {
    name: '标准两门衣柜',
    category: 'wardrobe',
    width: 1200, height: 2200, depth: 600,
    boards: (() => {
      const W = 1200, H = 2200, D = 600;
      const iW = W - 2 * T;
      const iH = H - 2 * T;
      const doorW = (iW - 2) / 2;
      return [
        b('b-001', 'side', '左侧板', 'cabinet_body', H, D, T, { x: -(W / 2 - T / 2), y: H / 2, z: 0 }, 'left', { grain: 'vertical', edgeBanding: { top: true, bottom: true }, edgeRole: { top: '上端', bottom: '下端', right: '内侧' } }),
        b('b-002', 'side', '右侧板', 'cabinet_body', H, D, T, { x: W / 2 - T / 2, y: H / 2, z: 0 }, 'right', { grain: 'vertical', edgeBanding: { top: true, bottom: true }, edgeRole: { top: '上端', bottom: '下端', left: '内侧' } }),
        b('b-003', 'top', '顶板', 'cabinet_body', iW, D, T, { x: 0, y: H - T / 2, z: 0 }, 'top', { grain: 'horizontal', edgeBanding: { bottom: true }, edgeRole: { bottom: '前口' } }),
        b('b-004', 'bottom', '底板', 'cabinet_body', iW, D, T, { x: 0, y: T / 2, z: 0 }, 'bottom', { grain: 'horizontal', edgeBanding: { top: true }, edgeRole: { top: '前口' } }),
        b('b-005', 'layer', '层板1', 'cabinet_body', iW, D, T, { x: 0, y: 722, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true }, edgeRole: { top: '前口' } }),
        b('b-006', 'layer', '层板2', 'cabinet_body', iW, D, T, { x: 0, y: 1444, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true }, edgeRole: { top: '前口' } }),
        b('b-007', 'door', '左门板', 'door', H - 50, doorW, T, { x: -(doorW / 2 + 1), y: H / 2, z: D / 2 + T / 2 }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, edgeRole: { left: '左侧', right: '中缝', top: '上端', bottom: '下端' }, hingeHoles: [{ edge: 'left', count: 3, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'left' }] }),
        b('b-008', 'door', '右门板', 'door', H - 50, doorW, T, { x: doorW / 2 + 1, y: H / 2, z: D / 2 + T / 2 }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, edgeRole: { left: '中缝', right: '右侧', top: '上端', bottom: '下端' }, hingeHoles: [{ edge: 'right', count: 3, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'right' }] }),
        b('b-009', 'back', '背板', 'back', iW, iH, T_B, { x: 0, y: H / 2, z: -(D / 2 - T_B / 2) }, 'back')
      ];
    })()
  },

  // ── 2. 衣柜 B: 三门衣柜 免拉手 (挂衣+抽屉+层板) ──
  {
    name: '三门衣柜·免拉手',
    category: 'wardrobe',
    width: 1800, height: 2200, depth: 600,
    boards: (() => {
      _did = 0;
      const W = 1800, H = 2200, D = 600;
      const iW = W - 2 * T;
      const iH = H - 2 * T;
      const compW = (iW - 2 * T) / 3;
      const sideDoorW = compW - 2;
      const centerDoorW = compW + T - 2;
      const centerCompW = compW + T; // 中格内宽
      const dGap = 20;
      const dZone = iH / 3;
      const dh = (dZone - 2 * dGap) / 3;
      const drawBase = T + 2;
      const dFaceZ = D / 2 + T / 2;

      return [
        b('b-001', 'side', '左侧板', 'cabinet_body', H, D, T, { x: -(W / 2 - T / 2), y: H / 2, z: 0 }, 'left', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-002', 'side', '右侧板', 'cabinet_body', H, D, T, { x: W / 2 - T / 2, y: H / 2, z: 0 }, 'right', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-003', 'top', '顶板', 'cabinet_body', iW, D, T, { x: 0, y: H - T / 2, z: 0 }, 'top', { grain: 'horizontal' }),
        b('b-004', 'bottom', '底板', 'cabinet_body', iW, D, T, { x: 0, y: T / 2, z: 0 }, 'bottom', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-005', 'side', '立板1', 'cabinet_body', iH, D, T, { x: -(compW / 2 + T / 2), y: H / 2, z: 0 }, 'inner', { grain: 'vertical' }),
        b('b-006', 'side', '立板2', 'cabinet_body', iH, D, T, { x: compW / 2 + T / 2, y: H / 2, z: 0 }, 'inner', { grain: 'vertical' }),
        b('b-007', 'layer', '层板1', 'cabinet_body', compW, D, T, { x: -(W / 2 - T - compW / 2), y: T + dZone + T / 2, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-008', 'layer', '层板2', 'cabinet_body', compW, D, T, { x: W / 2 - T - compW / 2, y: T + dZone + T / 2, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-020', 'layer', '层板3', 'cabinet_body', centerCompW, D, T, { x: 0, y: T / 2 + dZone, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-009', 'door', '左门板·免拉手', 'door', H - 50, sideDoorW, T, { x: -(W / 2 - T - compW / 2), y: H / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, hingeHoles: [{ edge: 'left', count: 3, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'left' }] }),
        b('b-010', 'door', '中门板·免拉手', 'door', H - dZone - T - 25, centerDoorW, T, { x: 0, y: (H + T + dZone + T) / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('b-011', 'door', '右门板·免拉手', 'door', H - 50, sideDoorW, T, { x: W / 2 - T - compW / 2, y: H / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, hingeHoles: [{ edge: 'right', count: 3, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'right' }] }),
        // 抽屉 (中格3个)
        b('d-001', 'door', '抽屉面板1·免拉手', 'door', dh, centerDoorW, T, { x: 0, y: drawBase + dh / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('d-002', 'door', '抽屉面板2·免拉手', 'door', dh, centerDoorW, T, { x: 0, y: drawBase + dh + dGap + dh / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('d-003', 'door', '抽屉面板3·免拉手', 'door', dh, centerDoorW, T, { x: 0, y: drawBase + 2 * (dh + dGap) + dh / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        ...drawerBody(0, drawBase + dh / 2, dFaceZ, centerCompW, dh, D, 'db'),
        ...drawerBody(0, drawBase + dh + dGap + dh / 2, dFaceZ, centerCompW, dh, D, 'db'),
        ...drawerBody(0, drawBase + 2 * (dh + dGap) + dh / 2, dFaceZ, centerCompW, dh, D, 'db'),
        b('b-015', 'back', '背板', 'back', iW, iH, T_B, { x: 0, y: H / 2, z: -(D / 2 - T_B / 2) }, 'back')
      ];
    })()
  },

  // ── 3. 衣柜 C: 大型三格衣柜 免拉手 (挂衣+多抽屉+层板) ──
  {
    name: '大型三格衣柜·免拉手',
    category: 'wardrobe',
    width: 2400, height: 2200, depth: 600,
    boards: (() => {
      _did = 0;
      const W = 2400, H = 2200, D = 600;
      const iW = W - 2 * T;
      const iH = H - 2 * T;
      const compW = (iW - 2 * T) / 3;
      const sideDoorW = compW - 2;
      const centerDoorW = compW + T - 2;
      const centerCompW = compW + T;
      const dGap = 20;
      const dZone = iH / 3;
      const dh = (dZone - 2 * dGap) / 3;
      const drawBase = T + 2;
      const dFaceZ = D / 2 + T / 2;

      return [
        b('b-001', 'side', '左侧板', 'cabinet_body', H, D, T, { x: -(W / 2 - T / 2), y: H / 2, z: 0 }, 'left', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-002', 'side', '右侧板', 'cabinet_body', H, D, T, { x: W / 2 - T / 2, y: H / 2, z: 0 }, 'right', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-003', 'top', '顶板', 'cabinet_body', iW, D, T, { x: 0, y: H - T / 2, z: 0 }, 'top', { grain: 'horizontal' }),
        b('b-004', 'bottom', '底板', 'cabinet_body', iW, D, T, { x: 0, y: T / 2, z: 0 }, 'bottom', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-005', 'side', '立板1', 'cabinet_body', iH, D, T, { x: -(compW / 2 + T / 2), y: H / 2, z: 0 }, 'inner', { grain: 'vertical' }),
        b('b-006', 'side', '立板2', 'cabinet_body', iH, D, T, { x: compW / 2 + T / 2, y: H / 2, z: 0 }, 'inner', { grain: 'vertical' }),
        b('b-007', 'layer', '层板1', 'cabinet_body', compW, D, T, { x: W / 2 - T - compW / 2, y: T + dZone + T / 2, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-008', 'layer', '层板2', 'cabinet_body', compW, D, T, { x: W / 2 - T - compW / 2, y: T + 2 * dZone + T / 2, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-020', 'layer', '层板3', 'cabinet_body', centerCompW, D, T, { x: 0, y: T / 2 + dZone, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-009', 'door', '左门板·免拉手', 'door', H - 50, sideDoorW, T, { x: -(W / 2 - T - compW / 2), y: H / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, hingeHoles: [{ edge: 'left', count: 3, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'left' }] }),
        b('b-010', 'door', '中门板·免拉手', 'door', H - dZone - T - 25, centerDoorW, T, { x: 0, y: (H + T + dZone + T) / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('b-011', 'door', '右门板·免拉手', 'door', H - 50, sideDoorW, T, { x: W / 2 - T - compW / 2, y: H / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, hingeHoles: [{ edge: 'right', count: 3, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'right' }] }),
        // 抽屉 (中格3个)
        b('d-001', 'door', '抽屉面板1·免拉手', 'door', dh, centerDoorW, T, { x: 0, y: drawBase + dh / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('d-002', 'door', '抽屉面板2·免拉手', 'door', dh, centerDoorW, T, { x: 0, y: drawBase + dh + dGap + dh / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('d-003', 'door', '抽屉面板3·免拉手', 'door', dh, centerDoorW, T, { x: 0, y: drawBase + 2 * (dh + dGap) + dh / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        ...drawerBody(0, drawBase + dh / 2, dFaceZ, centerCompW, dh, D, 'dc'),
        ...drawerBody(0, drawBase + dh + dGap + dh / 2, dFaceZ, centerCompW, dh, D, 'dc'),
        ...drawerBody(0, drawBase + 2 * (dh + dGap) + dh / 2, dFaceZ, centerCompW, dh, D, 'dc'),
        b('b-015', 'back', '背板', 'back', iW, iH, T_B, { x: 0, y: H / 2, z: -(D / 2 - T_B / 2) }, 'back')
      ];
    })()
  },

  // ── 4. 鞋柜: 多层板+底部抽屉 ──
  {
    name: '鞋柜',
    category: 'base-cabinet',
    width: 1200, height: 900, depth: 350,
    boards: (() => {
      _did = 0;
      const W = 1200, H = 900, D = 350;
      const iW = W - 2 * T;
      const iH = H - 2 * T;
      const drawerH = 170;
      const drawerGap = 20;
      const drawerZone = 2 * drawerH + drawerGap;
      const shelfZone = iH - drawerZone;
      const shelfGap = shelfZone / 5;
      const doorW = (iW - 2) / 2;
      const dFaceW = iW - 2;
      const dFaceZ = D / 2 + T / 2;

      return [
        b('b-001', 'side', '左侧板', 'cabinet_body', H, D, T, { x: -(W / 2 - T / 2), y: H / 2, z: 0 }, 'left', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-002', 'side', '右侧板', 'cabinet_body', H, D, T, { x: W / 2 - T / 2, y: H / 2, z: 0 }, 'right', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-003', 'top', '顶板', 'cabinet_body', iW, D, T, { x: 0, y: H - T / 2, z: 0 }, 'top', { grain: 'horizontal' }),
        b('b-004', 'bottom', '底板', 'cabinet_body', iW, D, T, { x: 0, y: T / 2, z: 0 }, 'bottom', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-005', 'layer', '层板1', 'cabinet_body', iW, D, T, { x: 0, y: T + drawerZone + shelfGap, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-006', 'layer', '层板2', 'cabinet_body', iW, D, T, { x: 0, y: T + drawerZone + 2 * shelfGap, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-007', 'layer', '层板3', 'cabinet_body', iW, D, T, { x: 0, y: T + drawerZone + 3 * shelfGap, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-008', 'layer', '层板4', 'cabinet_body', iW, D, T, { x: 0, y: T + drawerZone + 4 * shelfGap, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-009', 'layer', '层板5', 'cabinet_body', iW, D, T, { x: 0, y: T + drawerZone + 5 * shelfGap, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-010', 'door', '左门板', 'door', shelfZone - 50, doorW, T, { x: -(doorW / 2 + 1), y: T + drawerZone + shelfZone / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, hingeHoles: [{ edge: 'left', count: 2, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'left' }] }),
        b('b-011', 'door', '右门板', 'door', shelfZone - 50, doorW, T, { x: doorW / 2 + 1, y: T + drawerZone + shelfZone / 2, z: dFaceZ }, 'front', { grain: 'vertical', edgeBanding: { left: true, right: true, top: true, bottom: true }, hingeHoles: [{ edge: 'right', count: 2, spacing: 'even', diameter: 35, depth: 12, doorGap: 2, edgeDistance: 22, direction: 'height', opening: 'right' }] }),
        // 抽屉
        b('d-001', 'door', '抽屉面板1', 'door', drawerH, dFaceW, T, { x: 0, y: T + 2 + drawerH / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('d-002', 'door', '抽屉面板2', 'door', drawerH, dFaceW, T, { x: 0, y: T + 2 + drawerH + drawerGap + drawerH / 2, z: dFaceZ }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        ...drawerBody(0, T + 2 + drawerH / 2, dFaceZ, iW, drawerH, D, 'ds'),
        ...drawerBody(0, T + 2 + drawerH + drawerGap + drawerH / 2, dFaceZ, iW, drawerH, D, 'ds'),
        b('b-014', 'back', '背板', 'back', iW, iH, T_B, { x: 0, y: H / 2, z: -(D / 2 - T_B / 2) }, 'back')
      ];
    })()
  },

  // ── 5. 吊柜: 上翻门 ──
  {
    name: '吊柜',
    category: 'base-cabinet',
    width: 1200, height: 600, depth: 350,
    boards: (() => {
      const W = 1200, H = 600, D = 350;
      const iW = W - 2 * T; // 1164
      const iH = H - 2 * T; // 564
      const doorH = (iH - 4) / 2; // 280 per door

      return [
        b('b-001', 'side', '左侧板', 'cabinet_body', H, D, T, { x: -(W / 2 - T / 2), y: H / 2, z: 0 }, 'left', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-002', 'side', '右侧板', 'cabinet_body', H, D, T, { x: W / 2 - T / 2, y: H / 2, z: 0 }, 'right', { grain: 'vertical', edgeBanding: { top: true, bottom: true } }),
        b('b-003', 'top', '顶板', 'cabinet_body', iW, D, T, { x: 0, y: H - T / 2, z: 0 }, 'top', { grain: 'horizontal' }),
        b('b-004', 'bottom', '底板', 'cabinet_body', iW, D, T, { x: 0, y: T / 2, z: 0 }, 'bottom', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-005', 'layer', '层板', 'cabinet_body', iW, D, T, { x: 0, y: H / 2, z: 0 }, 'inner', { grain: 'horizontal', edgeBanding: { top: true } }),
        b('b-006', 'door', '上门板', 'door', doorH, iW - 4, T, { x: 0, y: H - T - doorH / 2 - 2, z: D / 2 + T / 2 }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('b-007', 'door', '下门板', 'door', doorH, iW - 4, T, { x: 0, y: T + doorH / 2 + 2, z: D / 2 + T / 2 }, 'front', { grain: 'horizontal', edgeBanding: { left: true, right: true, top: true, bottom: true } }),
        b('b-008', 'back', '背板', 'back', iW, iH, T_B, { x: 0, y: H / 2, z: -(D / 2 - T_B / 2) }, 'back')
      ];
    })()
  }
];
