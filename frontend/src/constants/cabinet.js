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
