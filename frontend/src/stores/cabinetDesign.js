import { defineStore } from 'pinia';
import { executeSplit, confirmSplit } from '@/api/order-split';
import { listCabinetTemplates } from '@/api/cabinet-templates';

export const useCabinetDesignStore = defineStore('cabinetDesign', {
  state: () => ({
    orderId: null,
    presets: [],
    selectedPreset: null,
    wizardParams: { width: 1200, height: 2200, depth: 600, shelfCount: 2, doorCount: 2 },
    cabinetJson: null,
    splitItems: [],
    splitting: false,
    confirming: false,
    splitResult: null
  }),
  actions: {
    setOrderId(id) {
      if (this.orderId !== id) {
        this.reset();
      }
      this.orderId = id;
    },
    setSelectedPreset(p) { this.selectedPreset = p; },
    setCabinetJson(json) { this.cabinetJson = json; },
    setWizardParams(p) { Object.assign(this.wizardParams, p); },
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
    reset() {
      this.selectedPreset = null;
      this.cabinetJson = null;
      this.splitItems = [];
      this.splitResult = null;
    }
  }
});
