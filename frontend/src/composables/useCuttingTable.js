import { ref, computed, nextTick } from 'vue';

let nextId = 1;

function createEmptyRow() {
  return {
    _id: nextId++,
    orderNo: '',
    customer: '',
    boardType: null,
    length: null,
    width: null,
    materialName: '',
    color: '',
    quantity: null,
    notes: '',
    _validation: {}
  };
}

export function useCuttingTable(boardOptionsRef) {
  const rows = ref([createEmptyRow()]);
  const focusedCell = ref({ row: 0, col: 0 });

  const columns = [
    { key: 'orderNo', label: '订单号', width: 110, type: 'text' },
    { key: 'customer', label: '客户', width: 100, type: 'text' },
    { key: 'boardType', label: '板材类型', width: 150, type: 'select' },
    { key: 'length', label: '长(L)', width: 90, type: 'number' },
    { key: 'width', label: '宽(W)', width: 90, type: 'number' },
    { key: 'materialName', label: '材质', width: 90, type: 'text' },
    { key: 'color', label: '颜色', width: 80, type: 'text' },
    { key: 'quantity', label: '数量', width: 80, type: 'number' },
    { key: 'notes', label: '备注', width: 120, type: 'text' }
  ];

  const errorCount = computed(() => {
    let count = 0;
    for (const row of rows.value) {
      validateRow(row);
      if (Object.keys(row._validation).length > 0) count++;
    }
    return count;
  });

  const totalItems = computed(() => {
    return rows.value.reduce((sum, r) => {
      const qty = Number(r.quantity);
      return sum + (Number.isFinite(qty) && qty > 0 ? qty : 0);
    }, 0);
  });

  const boardTypeCount = computed(() => {
    const types = new Set();
    for (const r of rows.value) {
      if (r.boardType) types.add(r.boardType);
    }
    return types.size;
  });

  const totalArea = computed(() => {
    return rows.value.reduce((sum, r) => {
      const l = Number(r.length);
      const w = Number(r.width);
      const qty = Number(r.quantity);
      if (Number.isFinite(l) && Number.isFinite(w) && Number.isFinite(qty) && l > 0 && w > 0 && qty > 0) {
        return sum + (l * w * qty);
      }
      return sum;
    }, 0);
  });

  function getSelectedBoard(row) {
    if (!row.boardType || !boardOptionsRef?.value) return null;
    return boardOptionsRef.value.find(b => b.boardId === row.boardType) || null;
  }

  function validateRow(row) {
    const errors = {};
    const l = Number(row.length);
    const w = Number(row.width);
    const qty = Number(row.quantity);

    if (row.length === '' || row.length == null || !Number.isFinite(l) || l <= 0) {
      errors.length = '必填';
    } else if (l > 3000) {
      errors.length = '异常(>3000mm)';
    } else {
      const board = getSelectedBoard(row);
      if (board && l > board.length) errors.length = `超出板材(${board.length}mm)`;
    }

    if (row.width === '' || row.width == null || !Number.isFinite(w) || w <= 0) {
      errors.width = '必填';
    } else if (w > 1500) {
      errors.width = '异常(>1500mm)';
    } else {
      const board = getSelectedBoard(row);
      if (board && w > board.width) errors.width = `超出板材(${board.width}mm)`;
    }

    if (row.quantity === '' || row.quantity == null || !Number.isFinite(qty) || qty <= 0) {
      errors.quantity = '必填';
    }

    row._validation = errors;
    return Object.keys(errors).length === 0;
  }

  function addRow(index) {
    const row = createEmptyRow();
    rows.value.splice(index + 1, 0, row);
    return row;
  }

  function deleteRow(index) {
    if (rows.value.length <= 1) {
      rows.value[0] = createEmptyRow();
      return;
    }
    rows.value.splice(index, 1);
  }

  function handlePaste(event, startRow, startCol) {
    const text = event.clipboardData.getData('text/plain');
    if (!text) return;
    const lines = text.split(/\r?\n/).filter(l => l.trim());
    const colKeys = columns.map(c => c.key);

    for (let r = 0; r < lines.length; r++) {
      const cells = lines[r].split('\t');
      const targetRowIdx = startRow + r;
      while (targetRowIdx >= rows.value.length) {
        addRow(rows.value.length - 1);
      }
      const row = rows.value[targetRowIdx];
      for (let c = 0; c < cells.length; c++) {
        const targetCol = startCol + c;
        if (targetCol >= colKeys.length) break;
        const key = colKeys[targetCol];
        const val = cells[c].trim();
        if (key === 'length' || key === 'width' || key === 'quantity') {
          row[key] = val === '' ? null : Number(val);
        } else {
          row[key] = val;
        }
      }
    }
    rows.value.forEach(r => validateRow(r));
  }

  function handleKeydown(event, rowIdx, colIdx) {
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
    if (newRow >= rows.value.length) newRow = rows.value.length - 1;
    if (newCol < 0) newCol = 0;
    if (newCol >= colCount) newCol = colCount - 1;

    focusedCell.value = { row: newRow, col: newCol };
    nextTick(() => {
      focusCellElement(newRow, newCol);
    });
  }

  function focusCellElement(row, col) {
    const el = document.querySelector(
      `.cutting-table [data-row="${row}"][data-col="${col}"] input, .cutting-table [data-row="${row}"][data-col="${col}"] .el-select input`
    );
    if (el) {
      el.focus();
      el.select?.();
    }
  }

  function validateAll() {
    let allValid = true;
    for (const row of rows.value) {
      if (!validateRow(row)) allValid = false;
    }
    return allValid;
  }

  function buildAlgorithmInput() {
    const squareList = [];
    for (const row of rows.value) {
      const l = Number(row.length);
      const w = Number(row.width);
      const qty = Number(row.quantity);
      if (Number.isFinite(l) && Number.isFinite(w) && Number.isFinite(qty) && l > 0 && w > 0 && qty > 0) {
        for (let i = 0; i < qty; i++) {
          squareList.push({
            id: `${row._id}-${i}`,
            l,
            w
          });
        }
      }
    }
    return squareList;
  }

  return {
    rows,
    columns,
    focusedCell,
    errorCount,
    totalItems,
    boardTypeCount,
    totalArea,
    validateRow,
    addRow,
    deleteRow,
    handlePaste,
    handleKeydown,
    validateAll,
    buildAlgorithmInput
  };
}

