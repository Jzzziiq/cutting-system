import { ref, computed } from 'vue';
import {
  materialSlotLabels,
  materialSlotThicknessRanges,
  boardColorPalette
} from '@/constants/cabinet';

export function useSlotMapping(boardOptions, activeCabinetId, activeCabinetJson, activeDraftBoards, cabinetDrafts) {
  const slotMapsByCabinet = ref({});

  const activeSlotMap = computed(() => slotMapsByCabinet.value[activeCabinetId.value] ?? {});

  function getMaterialSlots(boards) {
    const slots = new Set();
    boards.forEach(board => {
      if (board.materialSlot) slots.add(board.materialSlot);
    });
    return Array.from(slots);
  }

  function formatMaterialSlot(slot) {
    return materialSlotLabels[slot] || slot;
  }

  function isThicknessInRange(thickness, range) {
    const value = Number(thickness);
    return Number.isFinite(value) && value >= range.min && value <= range.max;
  }

  function getBoardOptionsForSlot(slot) {
    const range = materialSlotThicknessRanges[slot];
    if (!range) return [];
    return boardOptions.value.filter(option => isThicknessInRange(option.board?.thickness, range));
  }

  function getSelectableBoardOptionsForSlot(slot) {
    const matchedOptions = getBoardOptionsForSlot(slot);
    return matchedOptions.length ? matchedOptions : boardOptions.value;
  }

  function getBoardOptionById(boardId) {
    return boardOptions.value.find(option => Number(option.value) === Number(boardId)) || null;
  }

  function resolveBoardAppearanceColor(board) {
    const text = [board?.color, board?.materialType, board?.brand].filter(Boolean).join(' ');
    const matched = boardColorPalette.find(item => text.includes(item.keyword));
    return matched?.color || null;
  }

  function applyBoardAppearance(boards, materialSlotBoardMap = {}) {
    return boards.map((board) => {
      const boardId = board.boardId || materialSlotBoardMap[board.materialSlot];
      const mappedBoard = getBoardOptionById(boardId)?.board;
      const appearanceColor = resolveBoardAppearanceColor(mappedBoard);
      return {
        ...board,
        mappedBoard,
        textureUrl: mappedBoard?.textureUrl || undefined,
        appearanceColor: appearanceColor || undefined
      };
    });
  }

  function createDefaultSlotMap(boards, previousMap = {}) {
    const nextMap = {};
    getMaterialSlots(boards).forEach(slot => {
      const representative = boards.find(board => board.materialSlot === slot);
      const slotOptions = getSelectableBoardOptionsForSlot(slot);
      const current = slotOptions.some(option => option.value === previousMap[slot])
        ? previousMap[slot]
        : null;
      const matched = slotOptions.find(option =>
        Number(option.board?.thickness) === Number(representative?.thickness)
      ) || slotOptions[0];
      nextMap[slot] = current || matched?.value || null;
    });
    return nextMap;
  }

  function ensureSlotMapForCabinet(clientCabinetId, boards) {
    if (!clientCabinetId) return {};
    const nextMap = createDefaultSlotMap(boards, slotMapsByCabinet.value[clientCabinetId] ?? {});
    slotMapsByCabinet.value = {
      ...slotMapsByCabinet.value,
      [clientCabinetId]: nextMap
    };
    return nextMap;
  }

  function removeSlotMapForCabinet(clientCabinetId) {
    const nextMaps = { ...slotMapsByCabinet.value };
    delete nextMaps[clientCabinetId];
    slotMapsByCabinet.value = nextMaps;
  }

  function setActiveSlotMapValue(slot, boardId, onSlotChange) {
    if (!activeCabinetId.value) return;
    const nextMap = {
      ...activeSlotMap.value,
      [slot]: boardId
    };
    slotMapsByCabinet.value = {
      ...slotMapsByCabinet.value,
      [activeCabinetId.value]: nextMap
    };
    if (onSlotChange) onSlotChange(nextMap);
  }

  const materialSlots = computed(() => getMaterialSlots(activeDraftBoards.value));

  const activeSlotsMapped = computed(() =>
    materialSlots.value.length > 0 && materialSlots.value.every(slot => Boolean(activeSlotMap.value[slot]))
  );

  const allCabinetsSlotsMapped = computed(() =>
    cabinetDrafts.value.length > 0 && cabinetDrafts.value.every((draft) => {
      const slots = getMaterialSlots(draft.cabinetJson?.boards ?? []);
      const map = slotMapsByCabinet.value[draft.clientCabinetId] ?? {};
      return slots.length > 0 && slots.every(slot => Boolean(map[slot]));
    })
  );

  const boardOptionsBySlot = computed(() =>
    materialSlots.value.reduce((optionsBySlot, slot) => {
      optionsBySlot[slot] = getSelectableBoardOptionsForSlot(slot);
      return optionsBySlot;
    }, {})
  );

  function findFirstUnmappedCabinetId() {
    const drafts = cabinetDrafts.value;
    const draft = drafts.find((item) => {
      const slots = getMaterialSlots(item.cabinetJson?.boards ?? []);
      const map = slotMapsByCabinet.value[item.clientCabinetId] ?? {};
      return slots.length === 0 || slots.some(slot => !map[slot]);
    });
    return draft?.clientCabinetId || null;
  }

  return {
    slotMapsByCabinet,
    activeSlotMap,
    materialSlots,
    activeSlotsMapped,
    allCabinetsSlotsMapped,
    boardOptionsBySlot,
    getMaterialSlots,
    formatMaterialSlot,
    getBoardOptionsForSlot,
    getSelectableBoardOptionsForSlot,
    getBoardOptionById,
    resolveBoardAppearanceColor,
    applyBoardAppearance,
    createDefaultSlotMap,
    ensureSlotMapForCabinet,
    removeSlotMapForCabinet,
    setActiveSlotMapValue,
    findFirstUnmappedCabinetId
  };
}
