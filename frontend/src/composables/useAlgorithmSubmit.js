import { ref, onUnmounted } from 'vue';
import { submitAlgorithm, getAlgorithmResult } from '@/api/algorithm';

export function useAlgorithmSubmit() {
  const submitting = ref(false);
  const taskId = ref('');
  const taskStatus = ref(''); // 'running' | 'done' | 'failed'
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
      // Algorithm is synchronous in current backend, so result may be immediate
      if (res?.status === 2 || res?.bestRate !== undefined) {
        // Already completed
        taskStatus.value = 'done';
        if (res.resultJson) {
          result.value = typeof res.resultJson === 'string' ? JSON.parse(res.resultJson) : res.resultJson;
        }
      } else if (res?.status === -1) {
        taskStatus.value = 'failed';
        errorMsg.value = res.errorMsg || '计算失败';
      } else {
        // Poll for result
        startPolling();
      }
      return res;
    } catch (e) {
      taskStatus.value = 'failed';
      errorMsg.value = e.message || '算法执行失败';
      throw e;
    } finally {
      submitting.value = false;
    }
  }

  function startPolling() {
    stopPolling();
    pollTimer = setInterval(async () => {
      try {
        const res = await getAlgorithmResult(taskId.value);
        if (res?.status === 2) {
          taskStatus.value = 'done';
          if (res.resultJson) {
            result.value = typeof res.resultJson === 'string' ? JSON.parse(res.resultJson) : res.resultJson;
          }
          stopPolling();
        } else if (res?.status === -1) {
          taskStatus.value = 'failed';
          errorMsg.value = res.errorMsg || '计算失败';
          stopPolling();
        }
      } catch (e) {
        // Silently retry
      }
    }, 1000);
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
