import { useAlgorithmSubmit } from '@/composables/useAlgorithmSubmit';
import { boardLabel } from '@/utils/boardLabel';

export function useLayoutRunner() {
  const { submitting, submit } = useAlgorithmSubmit();

  function parseResultJson(raw) {
    if (!raw) return [];
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return Array.isArray(parsed) ? parsed : [parsed];
  }

  function buildSquareList(items = []) {
    const squareList = [];
    for (const item of items) {
      const quantity = Math.max(1, Number(item.quantity) || 1);
      for (let index = 0; index < quantity; index += 1) {
        const id = item.partCode
          ? `${item.partCode}-${index + 1}`
          : `${item.orderItemId || 'item'}-${index + 1}`;
        squareList.push({
          id,
          l: item.length,
          w: item.width,
          partCode: item.partCode,
          partName: item.partName,
          orderItemId: item.orderItemId
        });
      }
    }
    return squareList;
  }

  function decorateSolutions(rawSolutions, board, items = []) {
    const itemByCode = new Map();
    const itemById = new Map();
    for (const item of items) {
      if (item.partCode) itemByCode.set(String(item.partCode), item);
      if (item.orderItemId) itemById.set(String(item.orderItemId), item);
    }

    return parseResultJson(rawSolutions).map(solution => ({
      ...solution,
      _boardGroup: board,
      placeSquareList: (solution.placeSquareList || []).map(piece => {
        const pieceId = String(piece.id || '');
        const code = pieceId.split('-').slice(0, -1).join('-');
        const orderItemId = pieceId.split('-')[0];
        const sourceItem = itemByCode.get(code) || itemById.get(orderItemId);
        return {
          ...piece,
          partCode: sourceItem?.partCode || piece.partCode,
          partName: sourceItem?.partName || piece.partName
        };
      })
    }));
  }

  function summarizeBoardResults(results) {
    let weightedSum = 0;
    let totalArea = 0;
    let solutionCount = 0;
    for (const result of results) {
      const board = result.board || {};
      const area = (board.length || 0) * (board.width || 0);
      weightedSum += (result.bestRate || 0) * area;
      totalArea += area;
      solutionCount += result.solutions.length;
    }
    const totalRate = totalArea > 0 ? weightedSum / totalArea : 0;
    return { totalRate, solutionCount };
  }

  async function runLayoutForGroups(groups, settings = {}, algorithmConfig = {}) {
    const allSolutions = [];
    const nextBoardResults = [];

    for (const group of groups) {
      const board = group.board;
      const squareList = buildSquareList(group.items);
      if (!squareList.length) continue;

      const result = await submit({
        L: board.length,
        W: board.width,
        isRotateEnable: algorithmConfig.allowRotation ?? settings.allowRotation ?? true,
        gapDistance: algorithmConfig.gapDistance ?? settings.gapDistance ?? 3,
        squareList
      });

      if (result?.status === -1) {
        throw new Error(result.errorMsg || '算法任务失败');
      }

      const groupSolutions = decorateSolutions(result?.resultJson, board, group.items);
      if (!groupSolutions.length) {
        throw new Error(`板材"${boardLabel(board)}"未返回排版结果`);
      }
      allSolutions.push(...groupSolutions);
      nextBoardResults.push({
        board,
        solutions: groupSolutions,
        bestRate: result?.bestRate,
        containerCount: result?.containerCount
      });
    }

    return {
      solutions: allSolutions,
      boardResults: nextBoardResults,
      ...summarizeBoardResults(nextBoardResults)
    };
  }

  return {
    submitting,
    submit,
    parseResultJson,
    boardLabel,
    buildSquareList,
    decorateSolutions,
    summarizeBoardResults,
    runLayoutForGroups
  };
}
