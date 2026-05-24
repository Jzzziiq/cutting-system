import { ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs';
import { getAlgorithmResult } from '@/api/algorithm';
import { getLayoutResult, createLayoutResult, deleteLayoutResult } from '@/api/layout-results';
import { getLayoutInput, getOrder } from '@/api/orders';

export function useLayoutDataLoader({ runLayoutForGroups, parseResultJson, boardLabel }) {
  const route = useRoute();

  const solutions = ref([]);
  const loadingCanvas = ref(false);
  const activeResultId = ref(null);
  const historyRefreshKey = ref(0);
  const orderInfo = ref({});
  const draftData = ref(null);
  const boardResults = ref([]);
  const currentLayoutInput = ref(null);
  const lastRouteLoadKey = ref('');

  async function buildOrderInfo(orderId) {
    try {
      const order = await getOrder(orderId);
      return {
        orderId,
        orderNo: order?.orderNo || '',
        orderName: order?.orderNo || `订单 #${orderId}`,
        customer: order?.customerName || '',
        processName: order?.processName || ''
      };
    } catch {
      return { orderId, orderName: `订单 #${orderId}` };
    }
  }

  async function loadFromOrder(orderId, settings = {}, algorithmConfig = {}) {
    const numericOrderId = Number(orderId);
    if (!Number.isFinite(numericOrderId) || numericOrderId <= 0) return false;
    loadingCanvas.value = true;
    try {
      const input = await getLayoutInput(numericOrderId);
      if (!input?.groups?.length) {
        ElMessage.warning('该订单没有待排样明细');
        orderInfo.value = await buildOrderInfo(numericOrderId);
        return false;
      }
      currentLayoutInput.value = input;
      draftData.value = null;
      activeResultId.value = null;
      const effectiveConfig = {
        gapDistance: input.algorithmConfig?.gapDistance ?? settings.gapDistance ?? 3,
        allowRotation: input.algorithmConfig?.allowRotation ?? settings.allowRotation ?? false
      };
      const baseOrderInfo = await buildOrderInfo(numericOrderId);
      const result = await runLayoutForGroups(input.groups, settings, effectiveConfig);
      solutions.value = result.solutions;
      boardResults.value = result.boardResults;
      orderInfo.value = {
        ...baseOrderInfo,
        utilizationRate: result.totalRate,
        containerCount: result.solutionCount,
        boardGroupLabels: input.groups.map(group => boardLabel(group.board))
      };
      return true;
    } catch (e) {
      ElMessage.error('自动排版失败：' + (e.message || '未知错误'));
      return false;
    } finally {
      loadingCanvas.value = false;
    }
  }

  async function loadOrderMeta(orderId) {
    const numericOrderId = Number(orderId);
    if (!Number.isFinite(numericOrderId) || numericOrderId <= 0) return false;
    loadingCanvas.value = true;
    try {
      const input = await getLayoutInput(numericOrderId);
      currentLayoutInput.value = input;
      const baseOrderInfo = await buildOrderInfo(numericOrderId);
      const totalPieces = input?.groups?.reduce((sum, g) =>
        sum + (g.items?.reduce((s, i) => s + (i.quantity || 1), 0) || 0), 0) || 0;
      orderInfo.value = {
        ...baseOrderInfo,
        totalPieces,
        boardGroupCount: input?.groups?.length || 0
      };
      solutions.value = [];
      boardResults.value = [];
      activeResultId.value = null;
      return true;
    } catch {
      return false;
    } finally {
      loadingCanvas.value = false;
    }
  }

  async function loadFromTask(taskId) {
    if (!taskId) return;
    loadingCanvas.value = true;
    try {
      const res = await getAlgorithmResult(taskId);
      if (res?.resultJson) {
        solutions.value = parseResultJson(res.resultJson);
        orderInfo.value = {
          orderName: `任务 #${taskId}`,
          orderId: res.orderId || null,
          utilizationRate: res.bestRate,
          containerCount: res.containerCount
        };
      } else {
        ElMessage.warning('该任务没有排版结果数据');
      }
      currentLayoutInput.value = null;
      draftData.value = null;
      activeResultId.value = null;
    } catch {
      ElMessage.error('加载排版结果失败');
    } finally {
      loadingCanvas.value = false;
    }
  }

  function loadFromDraft(draftId, queryOrderId) {
    if (!draftId) return;
    const raw = sessionStorage.getItem(`layout-draft-${draftId}`);
    if (!raw) {
      ElMessage.warning('草稿已过期或不存在');
      return;
    }
    try {
      const draft = JSON.parse(raw);
      draftData.value = draft;
      currentLayoutInput.value = null;
      boardResults.value = draft.boardResults || [];
      solutions.value = draft.mergedSolutions || [];
      let totalRate = 0;
      if (boardResults.value.length) {
        let weightedSum = 0;
        let totalArea = 0;
        for (const r of boardResults.value) {
          const board = r.board || {};
          const area = (board.length || 0) * (board.width || 0);
          weightedSum += (r.bestRate || 0) * area;
          totalArea += area;
        }
        totalRate = totalArea > 0 ? weightedSum / totalArea : 0;
      }
      const boardGroupLabels = boardResults.value.map(br =>
        [br.board?.brand, br.board?.materialType, br.board?.color].filter(Boolean).join(' ') || '未知板材'
      );
      orderInfo.value = {
        ...draft.orderInfo,
        orderId: draft.orderInfo?.orderId || Number(queryOrderId) || null,
        orderName: draft.orderInfo?.orderNo || `草稿 ${draftId}`,
        utilizationRate: totalRate,
        containerCount: solutions.value.length,
        boardGroupLabels
      };
      activeResultId.value = null;
    } catch {
      ElMessage.error('草稿数据解析失败');
    }
  }

  async function loadFromRoute(settings = {}) {
    const key = JSON.stringify({
      draftId: route.query.draftId || '',
      orderId: route.query.orderId || '',
      taskId: route.query.taskId || ''
    });
    if (key === lastRouteLoadKey.value) return;
    lastRouteLoadKey.value = key;

    if (route.query.draftId) {
      loadFromDraft(route.query.draftId, route.query.orderId);
    } else if (route.query.orderId) {
      await loadFromOrder(Number(route.query.orderId), settings);
    } else if (route.query.taskId) {
      await loadFromTask(route.query.taskId);
    } else {
      currentLayoutInput.value = null;
    }
  }

  async function onSelectRecord(record) {
    if (!record?.resultId) return;
    activeResultId.value = record.resultId;
    loadingCanvas.value = true;
    try {
      const detail = await getLayoutResult(record.resultId);
      if (detail?.resultJson) {
        const data = typeof detail.resultJson === 'string' ? JSON.parse(detail.resultJson) : detail.resultJson;
        solutions.value = parseResultJson(data);
        orderInfo.value = {
          orderId: detail.orderId || record.orderId || null,
          orderNo: detail.orderNo || record.orderNo || '',
          orderName: record.orderName || `排版 #${record.resultId}`,
          customer: record.customer,
          utilizationRate: detail.usageRate,
          containerCount: detail.containerCount
        };
        currentLayoutInput.value = null;
        draftData.value = null;
      } else {
        solutions.value = [];
        ElMessage.warning('该排版记录没有结果数据（resultJson 为空）');
      }
    } catch {
      ElMessage.error('加载排版详情失败');
    } finally {
      loadingCanvas.value = false;
    }
  }

  async function onDeleteRecord(record) {
    if (!record?.resultId) return;
    const recordName = record.orderNo || record.orderName || `排版 #${record.resultId}`;
    try {
      await ElMessageBox.confirm(
        `确认删除排版记录「${recordName}」？删除后不可恢复。`,
        '删除确认',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
      );
      await deleteLayoutResult(record.resultId);
      ElMessage.success('排版记录已删除');
      historyRefreshKey.value++;

      if (String(activeResultId.value) === String(record.resultId)) {
        activeResultId.value = null;
        solutions.value = [];
        orderInfo.value = {};
        boardResults.value = [];
        draftData.value = null;
      }
    } catch (e) {
      if (!['cancel', 'close'].includes(e) && !['cancel', 'close'].includes(e?.message)) {
        ElMessage.error('删除失败：' + (e.message || '未知错误'));
      }
    }
  }

  async function onSaveResult() {
    if (!solutions.value?.length) {
      ElMessage.warning('暂无排版结果可保存');
      return;
    }
    try {
      const existingOrderId = Number(orderInfo.value.orderId || 0);
      if (!(existingOrderId > 0)) {
        ElMessage.warning('保存排版结果前，请先选择一个已存在订单');
        return;
      }

      const totalArea = solutions.value.reduce((sum, s) => {
        const cw = s.containerWidth || s.instance?.W || 0;
        const cl = s.containerLength || s.instance?.L || 0;
        return sum + cw * cl;
      }, 0);
      const usedArea = solutions.value.reduce((sum, s) => {
        return sum + (s.placeSquareList || []).reduce((a, p) => a + p.l * p.w, 0);
      }, 0);
      const rate = totalArea > 0 ? usedArea / totalArea : 0;

      await createLayoutResult({
        orderId: existingOrderId,
        usageRate: rate,
        totalArea,
        containerCount: solutions.value.length,
        resultJson: JSON.stringify(solutions.value)
      });
      ElMessage.success('排版结果已保存');
      historyRefreshKey.value++;
    } catch (e) {
      ElMessage.error('保存失败：' + (e.message || '未知错误'));
    }
  }

  function resetRouteKey() {
    lastRouteLoadKey.value = '';
  }

  return {
    solutions,
    loadingCanvas,
    activeResultId,
    historyRefreshKey,
    orderInfo,
    draftData,
    boardResults,
    currentLayoutInput,
    resetRouteKey,
    loadOrderMeta,
    loadFromOrder,
    loadFromTask,
    loadFromDraft,
    loadFromRoute,
    onSelectRecord,
    onDeleteRecord,
    onSaveResult
  };
}
