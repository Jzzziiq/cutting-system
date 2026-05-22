import { freeBoardParts, categoryLabels } from '@/constants/cabinet';

export function useCabinetGeometry() {
  function numberOr(value, fallback) {
    const numeric = Number(value);
    return Number.isFinite(numeric) ? numeric : fallback;
  }

  function createBoard({
    id,
    type,
    displayName,
    materialSlot,
    thickness,
    designLength,
    designWidth,
    position,
    placementFace,
    connectedTo = [],
    grain = 'none',
    edgeBanding = {},
    edgeRole = {},
    hingeHoles = []
  }) {
    return {
      id,
      type,
      displayName,
      materialSlot,
      boardId: null,
      thickness,
      designLength,
      designWidth,
      grain,
      position,
      rotation: { x: 0, y: 0, z: 0 },
      placementFace,
      connectedTo,
      edgeBanding,
      edgeRole,
      hingeHoles
    };
  }

  function getPartConfig(partType) {
    return freeBoardParts.find(part => part.type === partType) || null;
  }

  function generateCabinetJson(params, preset, existingDrafts, orderInfo) {
    const category = preset?.category === 'base-cabinet' ? 'base-cabinet' : 'wardrobe';
    const width = Number(params.width);
    const height = Number(params.height);
    const depth = Number(params.depth);
    const shelfCount = Math.max(0, Number(params.shelfCount) || 0);
    const doorCount = Math.max(1, Number(params.doorCount) || 1);
    const thickness = 18;
    const innerWidth = Math.max(1, width - thickness * 2);
    const innerHeight = Math.max(1, height - thickness * 2);
    const categoryDraftCount = existingDrafts.filter(draft =>
      draft.cabinetJson?.cabinet?.category === category
    ).length;
    const cabinetName = `${categoryLabels[category]}${categoryDraftCount + 1}`;
    const boards = [];

    boards.push(createBoard({
      id: 'b-001',
      type: 'side',
      displayName: '左侧板',
      materialSlot: 'cabinet_body',
      thickness,
      designLength: height,
      designWidth: depth,
      position: { x: -width / 2 + thickness / 2, y: height / 2, z: 0 },
      placementFace: 'left',
      grain: 'vertical',
      edgeBanding: { left: false, right: false, top: true, bottom: true },
      edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
    }));

    boards.push(createBoard({
      id: 'b-002',
      type: 'side',
      displayName: '右侧板',
      materialSlot: 'cabinet_body',
      thickness,
      designLength: height,
      designWidth: depth,
      position: { x: width / 2 - thickness / 2, y: height / 2, z: 0 },
      placementFace: 'right',
      grain: 'vertical',
      edgeBanding: { left: false, right: false, top: true, bottom: true },
      edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
    }));

    boards.push(createBoard({
      id: 'b-003',
      type: 'top',
      displayName: '顶板',
      materialSlot: 'cabinet_body',
      thickness,
      designLength: innerWidth,
      designWidth: depth,
      position: { x: 0, y: height - thickness / 2, z: 0 },
      placementFace: 'top',
      connectedTo: ['b-001', 'b-002'],
      grain: 'horizontal',
      edgeBanding: { left: false, right: false, top: false, bottom: true },
      edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
    }));

    boards.push(createBoard({
      id: 'b-004',
      type: 'bottom',
      displayName: '底板',
      materialSlot: 'cabinet_body',
      thickness,
      designLength: innerWidth,
      designWidth: depth,
      position: { x: 0, y: thickness / 2, z: 0 },
      placementFace: 'bottom',
      connectedTo: ['b-001', 'b-002'],
      grain: 'horizontal',
      edgeBanding: { left: false, right: false, top: true, bottom: false },
      edgeRole: { left: '靠墙侧', right: '前口', top: '上端', bottom: '下端' }
    }));

    boards.push(createBoard({
      id: 'b-005',
      type: 'back',
      displayName: '背板',
      materialSlot: 'back',
      thickness: 5,
      designLength: innerWidth,
      designWidth: innerHeight,
      position: { x: 0, y: height / 2, z: -depth / 2 + 2.5 },
      placementFace: 'back',
      grain: 'vertical',
      edgeBanding: { left: false, right: false, top: false, bottom: false }
    }));

    const shelfSpacing = innerHeight / (shelfCount + 1);
    for (let index = 0; index < shelfCount; index += 1) {
      boards.push(createBoard({
        id: `b-${String(6 + index).padStart(3, '0')}`,
        type: 'layer',
        displayName: `层板${index + 1}`,
        materialSlot: 'cabinet_body',
        thickness,
        designLength: innerWidth,
        designWidth: depth,
        position: { x: 0, y: thickness + shelfSpacing * (index + 1), z: 0 },
        placementFace: 'inner',
        connectedTo: ['b-001', 'b-002'],
        grain: 'horizontal',
        edgeBanding: { left: false, right: false, top: false, bottom: false }
      }));
    }

    const doorWidth = innerWidth / doorCount;
    const doorHeight = Math.max(1, height - (category === 'base-cabinet' ? 50 : 50));
    for (let index = 0; index < doorCount; index += 1) {
      const isLeftDoor = index === 0;
      const isRightDoor = index === doorCount - 1;
      const hingeEdge = isLeftDoor ? 'left' : 'right';
      const opening = isLeftDoor ? 'left' : 'right';
      boards.push(createBoard({
        id: `b-${String(20 + index).padStart(3, '0')}`,
        type: 'door',
        displayName: `${index + 1}号门板`,
        materialSlot: 'door',
        thickness,
        designLength: doorHeight,
        designWidth: doorWidth,
        position: {
          x: -innerWidth / 2 + doorWidth / 2 + doorWidth * index,
          y: doorHeight / 2 + (height - doorHeight) / 2,
          z: depth / 2 + thickness / 2
        },
        placementFace: 'front',
        connectedTo: [isLeftDoor ? 'b-001' : (isRightDoor ? 'b-002' : 'b-001')],
        grain: 'vertical',
        edgeBanding: { left: true, right: true, top: true, bottom: true },
        hingeHoles: [{
          edge: hingeEdge,
          count: doorHeight > 1500 ? 3 : 2,
          spacing: 'even',
          diameter: 35,
          depth: 12,
          doorGap: 2,
          edgeDistance: 22,
          direction: 'height',
          opening
        }]
      }));
    }

    return {
      cabinet: {
        name: cabinetName,
        orderId: null,
        room: orderInfo?.room || '',
        purpose: '',
        width,
        height,
        depth,
        category
      },
      boards
    };
  }

  function getDefaultDropY(partType, cabinetBounds) {
    const { height } = cabinetBounds;
    const part = getPartConfig(partType);
    const thickness = part?.thickness || 18;
    if (partType === 'top') return Math.max(thickness / 2, height - thickness / 2);
    if (partType === 'bottom') return thickness / 2;
    return height / 2;
  }

  function createFreeAssemblyBoard(partType, rawPosition, cabinetBounds, existingBoards, snapFn) {
    const part = getPartConfig(partType);
    if (!part) return null;
    const { width, height, depth } = cabinetBounds;
    const thickness = part.thickness || 18;
    const innerWidth = Math.max(1, width - thickness * 2);
    const innerHeight = Math.max(1, height - thickness * 2);
    const partCount = existingBoards.filter(board => board.type === part.type).length + 1;

    const dimensionByType = {
      side: { designLength: height, designWidth: depth },
      back: { designLength: innerWidth, designWidth: innerHeight },
      door: { designLength: Math.max(1, height - 50), designWidth: Math.max(1, innerWidth / 2) },
      top: { designLength: innerWidth, designWidth: depth },
      bottom: { designLength: innerWidth, designWidth: depth },
      layer: { designLength: innerWidth, designWidth: depth }
    };
    const dimensions = dimensionByType[part.type] ?? part;
    const fallbackPosition = {
      x: 0,
      y: getDefaultDropY(part.type, cabinetBounds),
      z: part.type === 'door' ? depth / 2 + thickness / 2 : 0
    };
    if (part.type === 'back') fallbackPosition.z = -depth / 2 + thickness / 2;
    if (part.type === 'top') fallbackPosition.y = height - thickness / 2;
    if (part.type === 'bottom') fallbackPosition.y = thickness / 2;

    return createBoard({
      type: part.type,
      displayName: `${part.label}${partCount}`,
      materialSlot: part.materialSlot,
      thickness,
      designLength: dimensions.designLength,
      designWidth: dimensions.designWidth,
      position: snapFn(rawPosition || fallbackPosition, thickness),
      placementFace: part.placementFace,
      grain: part.grain,
      edgeBanding: { ...(part.edgeBanding ?? {}) },
      hingeHoles: part.hingeHoles ? JSON.parse(JSON.stringify(part.hingeHoles)) : []
    });
  }

  return {
    numberOr,
    createBoard,
    getPartConfig,
    generateCabinetJson,
    getDefaultDropY,
    createFreeAssemblyBoard
  };
}
