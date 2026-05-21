import { defineStore } from 'pinia';
import { executeSplit, confirmSplit } from '@/api/order-split';
import { listCabinetTemplates } from '@/api/cabinet-templates';

function cloneJson(json) {
  return json ? JSON.parse(JSON.stringify(json)) : null;
}

function createDraftId() {
  return `cab-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function createBoardId(boards) {
  const maxSequence = boards.reduce((max, board) => {
    const match = /^b-(\d+)$/.exec(board?.id || '');
    return match ? Math.max(max, Number(match[1])) : max;
  }, 0);
  return `b-${String(maxSequence + 1).padStart(3, '0')}`;
}

function cabinetName(cabinetJson, fallback = '未命名柜体') {
  return cabinetJson?.cabinet?.name || fallback;
}

function parseWorkpieceCode(code) {
  const match = /^(.+)-(\d+)$/.exec(code || '');
  if (!match) return null;
  return {
    prefix: match[1],
    sequence: Number(match[2]),
    width: match[2].length
  };
}

function formatWorkpieceCode(prefix, sequence, width) {
  return `${prefix}-${String(sequence).padStart(width, '0')}`;
}

function renumberPreviewItems(items, prefixMaxSequences) {
  return items.map((item) => {
    const parsed = parseWorkpieceCode(item.partCode);
    if (!parsed) return item;

    const currentMax = prefixMaxSequences.get(parsed.prefix) || 0;
    const sequence = parsed.sequence <= currentMax ? currentMax + 1 : parsed.sequence;
    prefixMaxSequences.set(parsed.prefix, sequence);

    const partCode = formatWorkpieceCode(parsed.prefix, sequence, parsed.width);
    const holeOperations = Array.isArray(item.holeOperations)
      ? item.holeOperations.map(operation => ({ ...operation, workpieceCode: partCode }))
      : item.holeOperations;
    return { ...item, partCode, holeOperations };
  });
}

function materialSlotBoardMapForDraft(draft, materialSlotBoardMaps) {
  return materialSlotBoardMaps?.[draft.clientCabinetId] ?? materialSlotBoardMaps ?? {};
}

function historySnapshot(cabinetDrafts, activeCabinetId) {
  return {
    cabinetDrafts: cloneJson(cabinetDrafts) ?? [],
    activeCabinetId
  };
}

function mergeObject(target, patch) {
  Object.entries(patch || {}).forEach(([key, value]) => {
    if (
      value
      && typeof value === 'object'
      && !Array.isArray(value)
      && target[key]
      && typeof target[key] === 'object'
      && !Array.isArray(target[key])
    ) {
      target[key] = { ...target[key], ...value };
      return;
    }
    target[key] = value;
  });
}

const HISTORY_LIMIT = 40;

export const useCabinetDesignStore = defineStore('cabinetDesign', {
  state: () => ({
    orderId: null,
    presets: [],
    selectedPreset: null,
    wizardParams: { width: 1200, height: 2200, depth: 600, shelfCount: 2, doorCount: 2 },
    cabinetDrafts: [],
    activeCabinetId: null,
    splitItems: [],
    splitGroups: [],
    splitting: false,
    confirming: false,
    splitResult: null,
    splitResults: [],
    undoStack: [],
    redoStack: []
  }),
  getters: {
    activeDraft(state) {
      return state.cabinetDrafts.find(draft => draft.clientCabinetId === state.activeCabinetId) || null;
    },
    cabinetJson() {
      return this.activeDraft?.cabinetJson || null;
    },
    hasDrafts(state) {
      return state.cabinetDrafts.length > 0;
    },
    canUndo(state) {
      return state.undoStack.length > 0;
    },
    canRedo(state) {
      return state.redoStack.length > 0;
    }
  },
  actions: {
    captureHistory() {
      this.undoStack.push(historySnapshot(this.cabinetDrafts, this.activeCabinetId));
      if (this.undoStack.length > HISTORY_LIMIT) {
        this.undoStack.shift();
      }
      this.redoStack = [];
    },
    restoreHistory(snapshot) {
      this.cabinetDrafts = cloneJson(snapshot?.cabinetDrafts) ?? [];
      this.activeCabinetId = snapshot?.activeCabinetId || this.cabinetDrafts[0]?.clientCabinetId || null;
      if (!this.cabinetDrafts.some(draft => draft.clientCabinetId === this.activeCabinetId)) {
        this.activeCabinetId = this.cabinetDrafts[0]?.clientCabinetId || null;
      }
      this.clearSplitPreview();
    },
    undo() {
      const snapshot = this.undoStack.pop();
      if (!snapshot) return null;
      this.redoStack.push(historySnapshot(this.cabinetDrafts, this.activeCabinetId));
      this.restoreHistory(snapshot);
      return this.cabinetJson;
    },
    redo() {
      const snapshot = this.redoStack.pop();
      if (!snapshot) return null;
      this.undoStack.push(historySnapshot(this.cabinetDrafts, this.activeCabinetId));
      this.restoreHistory(snapshot);
      return this.cabinetJson;
    },
    setOrderId(id) {
      if (this.orderId !== id) {
        this.reset();
      }
      this.orderId = id;
    },
    setSelectedPreset(p) { this.selectedPreset = p; },
    setCabinetJson(json) {
      if (!json) return;
      if (!this.activeCabinetId) {
        this.addCabinetDraft(json);
        return;
      }
      this.updateCabinetDraft(this.activeCabinetId, json);
    },
    setWizardParams(p) { Object.assign(this.wizardParams, p); },
    addCabinetDraft(cabinetJson, options = {}) {
      if (options.recordHistory !== false) this.captureHistory();
      const draft = {
        clientCabinetId: createDraftId(),
        cabinetJson: cloneJson(cabinetJson),
        createdAt: Date.now()
      };
      this.cabinetDrafts.push(draft);
      this.activeCabinetId = draft.clientCabinetId;
      this.clearSplitPreview();
      return draft;
    },
    updateCabinetDraft(clientCabinetId, cabinetJson, options = {}) {
      const draft = this.cabinetDrafts.find(item => item.clientCabinetId === clientCabinetId);
      if (!draft) {
        return this.addCabinetDraft(cabinetJson);
      }
      if (options.recordHistory !== false) this.captureHistory();
      draft.cabinetJson = cloneJson(cabinetJson);
      this.activeCabinetId = clientCabinetId;
      this.clearSplitPreview();
      return draft;
    },
    setActiveCabinetId(clientCabinetId) {
      if (this.cabinetDrafts.some(draft => draft.clientCabinetId === clientCabinetId)) {
        this.activeCabinetId = clientCabinetId;
      }
    },
    copyCabinetDraft(clientCabinetId) {
      const source = this.cabinetDrafts.find(draft => draft.clientCabinetId === clientCabinetId);
      if (!source) return null;
      const nextJson = cloneJson(source.cabinetJson);
      if (nextJson?.cabinet) {
        nextJson.cabinet.name = `${cabinetName(nextJson)} 副本`;
      }
      return this.addCabinetDraft(nextJson);
    },
    removeCabinetDraft(clientCabinetId) {
      const index = this.cabinetDrafts.findIndex(draft => draft.clientCabinetId === clientCabinetId);
      if (index < 0) return;
      this.captureHistory();
      this.cabinetDrafts.splice(index, 1);
      if (this.activeCabinetId === clientCabinetId) {
        const next = this.cabinetDrafts[Math.min(index, this.cabinetDrafts.length - 1)];
        this.activeCabinetId = next?.clientCabinetId || null;
      }
      this.clearSplitPreview();
    },
    addBoardToActiveCabinet(board) {
      const draft = this.activeDraft;
      if (!draft?.cabinetJson) return null;
      this.captureHistory();
      const boards = draft.cabinetJson.boards ?? [];
      draft.cabinetJson.boards = boards;
      const nextBoard = {
        ...cloneJson(board),
        id: board.id || createBoardId(boards)
      };
      draft.cabinetJson.boards.push(nextBoard);
      this.clearSplitPreview();
      return nextBoard;
    },
    updateActiveBoard(boardId, patch) {
      const draft = this.activeDraft;
      const board = draft?.cabinetJson?.boards?.find(item => item.id === boardId);
      if (!board) return null;
      this.captureHistory();
      mergeObject(board, patch);
      this.clearSplitPreview();
      return cloneJson(board);
    },
    copyActiveBoard(boardId) {
      const draft = this.activeDraft;
      const source = draft?.cabinetJson?.boards?.find(item => item.id === boardId);
      if (!source) return null;
      this.captureHistory();
      const boards = draft.cabinetJson.boards ?? [];
      draft.cabinetJson.boards = boards;
      const nextBoard = cloneJson(source);
      nextBoard.id = createBoardId(boards);
      nextBoard.displayName = `${source.displayName || source.type || '板件'} 副本`;
      nextBoard.position = {
        ...(source.position ?? { x: 0, y: 0, z: 0 }),
        x: Number(source.position?.x || 0) + 40,
        z: Number(source.position?.z || 0) + 40
      };
      boards.push(nextBoard);
      this.clearSplitPreview();
      return nextBoard;
    },
    removeActiveBoard(boardId) {
      const draft = this.activeDraft;
      const boards = draft?.cabinetJson?.boards;
      const index = boards?.findIndex(item => item.id === boardId) ?? -1;
      if (index < 0) return false;
      this.captureHistory();
      boards.splice(index, 1);
      this.clearSplitPreview();
      return true;
    },
    async loadPresets() {
      try {
        const data = await listCabinetTemplates({ pageNum: 1, pageSize: 50 });
        this.presets = data?.records ?? (Array.isArray(data) ? data : []);
      } catch { this.presets = []; }
    },
    async executeSplit(cabinetJson, materialSlotBoardMap) {
      this.splitting = true;
      try {
        this.splitItems = await executeSplit({ cabinetJson, materialSlotBoardMap });
        return this.splitItems;
      } finally { this.splitting = false; }
    },
    async executeAllSplits(materialSlotBoardMaps) {
      if (this.cabinetDrafts.length === 0) {
        throw new Error('请先新增柜体模型');
      }
      this.splitting = true;
      try {
        const groups = [];
        const prefixMaxSequences = new Map();
        for (const draft of this.cabinetDrafts) {
          const rawItems = await executeSplit({
            cabinetJson: draft.cabinetJson,
            materialSlotBoardMap: materialSlotBoardMapForDraft(draft, materialSlotBoardMaps)
          });
          const items = renumberPreviewItems(rawItems, prefixMaxSequences);
          groups.push({
            clientCabinetId: draft.clientCabinetId,
            cabinetName: cabinetName(draft.cabinetJson),
            items
          });
        }
        this.splitGroups = groups;
        this.splitItems = groups.flatMap(group =>
          group.items.map(item => ({ ...item, cabinetName: group.cabinetName }))
        );
        return groups;
      } finally { this.splitting = false; }
    },
    async confirmSplit(materialSlotBoardMap) {
      this.confirming = true;
      try {
        this.splitResult = await confirmSplit({
          orderId: this.orderId, confirmMode: 'append',
          cabinetJson: this.cabinetJson, materialSlotBoardMap
        });
        return this.splitResult;
      } finally { this.confirming = false; }
    },
    async confirmAllSplits(materialSlotBoardMaps) {
      if (this.cabinetDrafts.length === 0) {
        throw new Error('请先新增柜体模型');
      }
      this.confirming = true;
      const results = [];
      try {
        for (const draft of this.cabinetDrafts) {
          const result = await confirmSplit({
            orderId: this.orderId,
            confirmMode: 'append',
            cabinetJson: draft.cabinetJson,
            materialSlotBoardMap: materialSlotBoardMapForDraft(draft, materialSlotBoardMaps)
          });
          results.push({
            ...result,
            clientCabinetId: draft.clientCabinetId,
            cabinetName: cabinetName(draft.cabinetJson)
          });
        }
        this.splitResults = results;
        this.splitResult = results[results.length - 1] || null;
        return results;
      } catch (error) {
        error.confirmedResults = results;
        this.splitResults = results;
        if (results.length > 0) {
          const confirmedIds = new Set(results.map(result => result.clientCabinetId));
          this.cabinetDrafts = this.cabinetDrafts.filter(draft => !confirmedIds.has(draft.clientCabinetId));
          if (!this.cabinetDrafts.some(draft => draft.clientCabinetId === this.activeCabinetId)) {
            this.activeCabinetId = this.cabinetDrafts[0]?.clientCabinetId || null;
          }
        }
        throw error;
      } finally { this.confirming = false; }
    },
    clearSplitPreview() {
      this.splitItems = [];
      this.splitGroups = [];
      this.splitResult = null;
      this.splitResults = [];
    },
    reset() {
      this.selectedPreset = null;
      this.cabinetDrafts = [];
      this.activeCabinetId = null;
      this.undoStack = [];
      this.redoStack = [];
      this.clearSplitPreview();
    }
  }
});
