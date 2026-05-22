import { ref, computed } from 'vue';
import { SNAP_THRESHOLD, sceneDragModeOptions } from '@/constants/cabinet';

export function useSceneInteraction({
  activeCabinetJson,
  activeDraftBoards,
  selectedBoard,
  selectedBoardId,
  numberOr,
  getDropPoint,
  getPlanePoint,
  pickBoard,
  setControlsEnabled,
  moveBoardPreview,
  highlight,
  updateBoard,
  moveStep
}) {
  const draggingPartType = ref(null);
  const sceneDragMode = ref('xz');
  const sceneDragState = ref(null);
  const suppressNextCanvasClick = ref(false);

  function getCabinetBounds() {
    const cabinet = activeCabinetJson.value?.cabinet ?? {};
    const width = numberOr(cabinet.width, 1200);
    const height = numberOr(cabinet.height, 2200);
    const depth = numberOr(cabinet.depth, 600);
    return { width, height, depth };
  }

  function snapAxis(value, guides, threshold = SNAP_THRESHOLD) {
    const numeric = numberOr(value, 0);
    const nearest = guides
      .map(guide => ({ guide, distance: Math.abs(guide - numeric) }))
      .sort((a, b) => a.distance - b.distance)[0];
    if (nearest && nearest.distance <= threshold) return nearest.guide;
    return Math.round(numeric / moveStep.value) * moveStep.value;
  }

  function getSnapGuides(thickness = 18) {
    const { width, height, depth } = getCabinetBounds();
    const boardPositions = activeDraftBoards.value.map(board => board.position ?? {});
    return {
      x: [
        -width / 2 + thickness / 2,
        -width / 2,
        0,
        width / 2,
        width / 2 - thickness / 2,
        ...boardPositions.map(position => numberOr(position.x, 0))
      ],
      y: [
        thickness / 2,
        height / 2,
        height - thickness / 2,
        ...boardPositions.map(position => numberOr(position.y, 0))
      ],
      z: [
        -depth / 2 + thickness / 2,
        -depth / 2,
        0,
        depth / 2,
        depth / 2 + thickness / 2,
        ...boardPositions.map(position => numberOr(position.z, 0))
      ]
    };
  }

  function snapPosition(position, thickness = 18) {
    const guides = getSnapGuides(thickness);
    return {
      x: snapAxis(position?.x, guides.x),
      y: snapAxis(position?.y, guides.y),
      z: snapAxis(position?.z, guides.z)
    };
  }

  // --- Drag & Drop from parts library ---

  function onPartDragStart(event, part) {
    draggingPartType.value = part.type;
    event.dataTransfer?.setData('application/x-cabinet-part', part.type);
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy';
  }

  function onPartDragEnd() {
    draggingPartType.value = null;
  }

  function onCanvasDragOver(event) {
    const types = Array.from(event.dataTransfer?.types ?? []);
    if (!draggingPartType.value && !types.includes('application/x-cabinet-part')) return;
    event.preventDefault();
    if (event.dataTransfer) event.dataTransfer.dropEffect = 'copy';
  }

  function onCanvasDrop(event, getDefaultDropY, addFreeBoard) {
    const partType = event.dataTransfer?.getData('application/x-cabinet-part') || draggingPartType.value;
    if (!partType) return;
    event.preventDefault();
    const point = getDropPoint(event, { y: getDefaultDropY(partType), snapSize: moveStep.value });
    addFreeBoard(partType, point);
    draggingPartType.value = null;
  }

  // --- Pointer-based 3D board dragging ---

  function clearSceneDragState() {
    sceneDragState.value = null;
    setControlsEnabled(true);
  }

  function getSceneDragPosition(event) {
    const state = sceneDragState.value;
    if (!state) return null;
    const basePosition = state.startPosition;
    if (sceneDragMode.value === 'y') {
      const deltaY = (state.startClientY - event.clientY) * 2;
      return {
        ...basePosition,
        y: snapAxis(basePosition.y + deltaY, getSnapGuides(state.thickness).y)
      };
    }

    const point = getPlanePoint(event, { y: basePosition.y });
    if (!point) return null;
    const guides = getSnapGuides(state.thickness);
    const next = { ...basePosition };

    if (sceneDragMode.value === 'xz' || sceneDragMode.value === 'x') {
      next.x = snapAxis(point.x, guides.x);
    }
    if (sceneDragMode.value === 'xz' || sceneDragMode.value === 'z') {
      next.z = snapAxis(point.z, guides.z);
    }
    return next;
  }

  function onCanvasPointerDown(event) {
    if (event.button !== 0 || !activeCabinetJson.value) return;
    const hit = pickBoard(event);
    if (!hit?.board?.id) return;
    const draftBoard = activeDraftBoards.value.find(board => board.id === hit.board.id) || hit.board;
    const startPosition = {
      x: numberOr(draftBoard.position?.x, 0),
      y: numberOr(draftBoard.position?.y, 0),
      z: numberOr(draftBoard.position?.z, 0)
    };

    selectedBoard.value = draftBoard;
    highlight(draftBoard.id);
    sceneDragState.value = {
      boardId: draftBoard.id,
      thickness: numberOr(draftBoard.thickness, 18),
      startPosition,
      previewPosition: startPosition,
      startClientY: event.clientY,
      pointerId: event.pointerId,
      moved: false
    };
    setControlsEnabled(false);
    event.currentTarget?.setPointerCapture?.(event.pointerId);
    event.preventDefault();
  }

  function onCanvasPointerMove(event) {
    const state = sceneDragState.value;
    if (!state) return;
    const position = getSceneDragPosition(event);
    if (!position) return;
    const moved = ['x', 'y', 'z'].some(axis => Math.abs(position[axis] - state.startPosition[axis]) >= 1);
    sceneDragState.value = {
      ...state,
      previewPosition: position,
      moved: state.moved || moved
    };
    moveBoardPreview(state.boardId, position);
    event.preventDefault();
  }

  function onCanvasPointerUp(event) {
    const state = sceneDragState.value;
    if (!state) return;
    if (event.currentTarget?.hasPointerCapture?.(state.pointerId)) {
      event.currentTarget.releasePointerCapture(state.pointerId);
    }
    const finalPosition = state.previewPosition || state.startPosition;
    const shouldCommit = state.moved
      && ['x', 'y', 'z'].some(axis => Math.abs(finalPosition[axis] - state.startPosition[axis]) >= 1);
    clearSceneDragState();
    suppressNextCanvasClick.value = true;

    if (shouldCommit) {
      updateBoard({ position: finalPosition });
      return;
    }

    moveBoardPreview(state.boardId, state.startPosition);
    highlight(state.boardId);
  }

  return {
    draggingPartType,
    sceneDragMode,
    sceneDragState,
    suppressNextCanvasClick,
    getCabinetBounds,
    snapAxis,
    getSnapGuides,
    snapPosition,
    onPartDragStart,
    onPartDragEnd,
    onCanvasDragOver,
    onCanvasDrop,
    onCanvasPointerDown,
    onCanvasPointerMove,
    onCanvasPointerUp,
    isSceneDragging: computed(() => Boolean(sceneDragState.value)),
    sceneDragModeLabel: computed(() =>
      sceneDragModeOptions.find(option => option.value === sceneDragMode.value)?.label || '平面'
    )
  };
}
