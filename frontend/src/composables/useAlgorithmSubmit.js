import { ref, onUnmounted } from 'vue';
import { submitAlgorithm, getAlgorithmResult } from '@/api/algorithm';

export function useAlgorithmSubmit() {
  const submitting = ref(false);
  const taskId = ref('');
  const taskStatus = ref('');
  const result = ref(null);
  const errorMsg = ref('');
  let pollTimer = null;

  async function submit(data, algorithm = 'tabu_search') {
    submitting.value = true;
    taskStatus.value = 'running';
    errorMsg.value = '';
    result.value = null;
    try {
      const res = await submitAlgorithm(data, algorithm);
      taskId.value = res?.taskId || '';

      // Already completed with result data
      if (res?.resultJson) {
        taskStatus.value = 'done';
        const parsed = parseResult(res.resultJson);
        result.value = parsed;
        return { ...res, resultJson: parsed };
      }

      // Completed but resultJson not in submit response — fetch separately
      if (res?.status === 2 || res?.bestRate !== undefined) {
        taskStatus.value = 'done';
        const detail = await getAlgorithmResult(taskId.value);
        const parsed = parseResult(detail?.resultJson);
        result.value = parsed;
        return { ...res, ...detail, resultJson: parsed };
      }

      // Failed
      if (res?.status === -1) {
        taskStatus.value = 'failed';
        errorMsg.value = res.errorMsg || '计算失败';
        return res;
      }

      // Still running — poll and wait
      const final = await pollForResult(taskId.value);
      return { ...res, ...final, resultJson: final.resultJson };
    } catch (e) {
      taskStatus.value = 'failed';
      errorMsg.value = e.message || '算法执行失败';
      throw e;
    } finally {
      submitting.value = false;
    }
  }

  function pollForResult(tid) {
    return new Promise((resolve, reject) => {
      stopPolling();
      pollTimer = setInterval(async () => {
        try {
          const res = await getAlgorithmResult(tid);
          if (res?.status === 2) {
            taskStatus.value = 'done';
            const parsed = parseResult(res.resultJson);
            result.value = parsed;
            stopPolling();
            resolve({ ...res, resultJson: parsed });
          } else if (res?.status === -1) {
            taskStatus.value = 'failed';
            errorMsg.value = res.errorMsg || '计算失败';
            stopPolling();
            reject(new Error(res.errorMsg || '计算失败'));
          }
        } catch (e) {
          // Retry silently
        }
      }, 1000);
    });
  }

  function parseResult(raw) {
    if (!raw) return null;
    return typeof raw === 'string' ? JSON.parse(raw) : raw;
  }

  function startPolling() {
    // Legacy: used externally if needed
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }
  }

  onUnmounted(stopPolling);

  return {
    submitting,
    taskId,
    taskStatus,
    result,
    errorMsg,
    submit,
    startPolling,
    stopPolling
  };
}
