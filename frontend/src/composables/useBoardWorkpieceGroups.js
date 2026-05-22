import { ref, computed, nextTick } from 'vue';
import { validateWorkpieceDimensions } from '@/utils/validation';

let nextGroupId = 1;
let nextItemId = 1;

function createEmptyItem() {
  return {
    _id: nextItemId++,
    itemName: '',
    length: null,
    width: null,
    quantity: null,
    notes: '',
    _validation: {}
  };
}

export function useBoardWorkpieceGroups() {
  const boardGroups = ref([]);

  const columns = [
    { key: 'itemName', label: '工件名称', width: 120, type: 'text' },
    { key: 'length', label: '长(L)', width: 100, type: 'number' },
    { key: 'width', label: '宽(W)', width: 100, type: 'number' },
    { key: 'quantity', label: '数量', width: 90, type: 'number' },
    { key: 'notes', label: '备注', width: 150, type: 'text' }
  ];

  const totalItems = computed(() => {
    let sum = 0;
    for (const g of boardGroups.value) {
      for (const item of g.items) {
        const qty = Number(item.quantity);
        if (Number.isFinite(qty) && qty > 0) sum += qty;
      }
    }
    return sum;
  });

  const groupCount = computed(() => boardGroups.value.length);

  const totalArea = computed(() => {
    let sum = 0;
    for (const g of boardGroups.value) {
      for (const item of g.items) {
        const l = Number(item.length);
        const w = Number(item.width);
        const qty = Number(item.quantity);
        if (Number.isFinite(l) && Number.isFinite(w) && Number.isFinite(qty) && l > 0 && w > 0 && qty > 0) {
          sum += l * w * qty;
        }
      }
    }
    return sum;
  });

  const totalErrors = computed(() => {
    let count = 0;
    for (const g of boardGroups.value) {
      for (const item of g.items) {
        if (Object.keys(getItemValidationErrors(item, g.board)).length > 0) count++;
      }
    }
    return count;
  });

  function getItemValidationErrors(item, board) {
    return validateWorkpieceDimensions(item, board);
  }

  function validateItem(item, board) {
    const errors = getItemValidationErrors(item, board);
    item._validation = errors;
    return Object.keys(errors).length === 0;
  }

  function findGroupByBoardId(boardId) {
    return boardGroups.value.find(g => g.board.boardId === boardId);
  }

  function addBoardGroup(board) {
    const existing = findGroupByBoardId(board.boardId);
    if (existing) {
      existing.expanded = true;
      return existing;
    }
    const group = {
      id: `g${nextGroupId++}`,
      board: { ...board },
      items: [createEmptyItem()],
      expanded: true
    };
    boardGroups.value.push(group);
    return group;
  }

  function removeBoardGroup(groupId) {
    boardGroups.value = boardGroups.value.filter(g => g.id !== groupId);
  }

  function addItem(groupId) {
    const group = boardGroups.value.find(g => g.id === groupId);
    if (!group) return;
    group.items.push(createEmptyItem());
    group.expanded = true;
  }

  function deleteItem(groupId, rowIndex) {
    const group = boardGroups.value.find(g => g.id === groupId);
    if (!group) return;
    if (group.items.length <= 1) {
      group.items[0] = createEmptyItem();
      return;
    }
    group.items.splice(rowIndex, 1);
  }

  function handlePaste(groupId, event, startRow, startCol) {
    const group = boardGroups.value.find(g => g.id === groupId);
    if (!group) return;

    const text = event.clipboardData.getData('text/plain');
    if (!text) return;
    const lines = text.split(/\r?\n/).filter(l => l.trim());
    const colKeys = columns.map(c => c.key);

    for (let r = 0; r < lines.length; r++) {
      const cells = lines[r].split('\t');
      const targetRowIdx = startRow + r;
      while (targetRowIdx >= group.items.length) {
        group.items.push(createEmptyItem());
      }
      const item = group.items[targetRowIdx];
      for (let c = 0; c < cells.length; c++) {
        const targetCol = startCol + c;
        if (targetCol >= colKeys.length) break;
        const key = colKeys[targetCol];
        const val = cells[c].trim();
        if (key === 'length' || key === 'width' || key === 'quantity') {
          item[key] = val === '' ? null : Number(val);
        } else {
          item[key] = val;
        }
      }
    }
    group.items.forEach(item => validateItem(item, group.board));
  }

  function handleKeydown(groupId, event, rowIdx, colIdx, tableRef) {
    const group = boardGroups.value.find(g => g.id === groupId);
    if (!group) return;

    const colCount = columns.length;
    let newRow = rowIdx;
    let newCol = colIdx;

    switch (event.key) {
      case 'Tab':
        event.preventDefault();
        newCol = colIdx + (event.shiftKey ? -1 : 1);
        break;
      case 'Enter':
        event.preventDefault();
        if (!event.shiftKey) {
          newRow = rowIdx + 1;
        } else {
          newRow = rowIdx - 1;
        }
        break;
      case 'ArrowUp':
        event.preventDefault();
        newRow = rowIdx - 1;
        break;
      case 'ArrowDown':
        event.preventDefault();
        newRow = rowIdx + 1;
        break;
      case 'ArrowLeft':
        if (event.target.selectionStart === 0) {
          event.preventDefault();
          newCol = colIdx - 1;
        }
        break;
      case 'ArrowRight':
        if (event.target.selectionStart === (event.target.value || '').length) {
          event.preventDefault();
          newCol = colIdx + 1;
        }
        break;
      default:
        return;
    }

    if (newRow < 0) newRow = 0;
    if (newRow >= group.items.length) newRow = group.items.length - 1;
    if (newCol < 0) newCol = 0;
    if (newCol >= colCount) newCol = colCount - 1;

    nextTick(() => {
      focusCellElement(groupId, newRow, newCol);
    });
  }

  function focusCellElement(groupId, row, col) {
    const el = document.querySelector(
      `.board-group-table[data-group="${groupId}"] [data-row="${row}"][data-col="${col}"] input`
    );
    if (el) {
      el.focus();
      el.select?.();
    }
  }

  function validateAll() {
    let allValid = true;
    for (const g of boardGroups.value) {
      for (const item of g.items) {
        if (!validateItem(item, g.board)) allValid = false;
      }
    }
    return allValid;
  }

  function buildAlgorithmJobs() {
    return boardGroups.value.map(g => {
      const squareList = [];
      for (const item of g.items) {
        const l = Number(item.length);
        const w = Number(item.width);
        const qty = Number(item.quantity);
        if (Number.isFinite(l) && Number.isFinite(w) && Number.isFinite(qty) && l > 0 && w > 0 && qty > 0) {
          for (let i = 0; i < qty; i++) {
            squareList.push({ id: `${item._id}-${i}`, l, w });
          }
        }
      }
      return {
        groupId: g.id,
        board: g.board,
        squareList
      };
    }).filter(job => job.squareList.length > 0);
  }

  function getGroupStats(group) {
    let itemCount = 0;
    let area = 0;
    let errors = 0;
    for (const item of group.items) {
      const errs = getItemValidationErrors(item, group.board);
      if (Object.keys(errs).length > 0) errors++;
      const qty = Number(item.quantity);
      if (Number.isFinite(qty) && qty > 0) itemCount += qty;
      const l = Number(item.length);
      const w = Number(item.width);
      if (Number.isFinite(l) && Number.isFinite(w) && l > 0 && w > 0 && qty > 0) {
        area += l * w * qty;
      }
    }
    return { itemCount, area, errors };
  }

  function loadFromLayoutInput(layoutInput) {
    if (!layoutInput?.groups?.length) {
      boardGroups.value = [];
      return;
    }
    boardGroups.value = layoutInput.groups.map((group, gi) => ({
      id: `g${gi + 1}`,
      board: { ...group.board },
      items: group.items.map((item, ii) => ({
        _id: ii + 1,
        itemName: item.partName || '',
        length: item.length,
        width: item.width,
        quantity: item.quantity,
        notes: '',
        _validation: {}
      })),
      expanded: true
    }));
    nextGroupId = layoutInput.groups.length + 1;
    nextItemId = Math.max(...boardGroups.value.flatMap(g => g.items.map(i => i._id))) + 1;
    validateAll();
  }

  function buildSavePayload() {
    return {
      groups: boardGroups.value.map(g => ({
        boardId: g.board.boardId,
        items: g.items
          .filter(item => {
            const l = Number(item.length);
            const w = Number(item.width);
            const qty = Number(item.quantity);
            return Number.isFinite(l) && l > 0 && Number.isFinite(w) && w > 0 && Number.isFinite(qty) && qty > 0;
          })
          .map(item => ({
            partName: item.itemName || '',
            length: Number(item.length),
            width: Number(item.width),
            quantity: Number(item.quantity),
            remark: item.notes || ''
        }))
      })).filter(g => g.items.length > 0)
    };
  }

  return {
    boardGroups,
    columns,
    totalItems,
    groupCount,
    totalArea,
    totalErrors,
    addBoardGroup,
    removeBoardGroup,
    addItem,
    deleteItem,
    handlePaste,
    handleKeydown,
    validateAll,
    buildAlgorithmJobs,
    getGroupStats,
    focusCellElement,
    loadFromLayoutInput,
    buildSavePayload
  };
}
