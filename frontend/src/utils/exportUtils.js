import { ElMessage } from 'element-plus/es/components/message/index.mjs';
import { TOKEN_KEY } from '@/stores/auth';

export async function exportToolpathNC(resultId, toolRadius = 1.5, cutParams = {}) {
  if (!resultId) {
    ElMessage.warning('请先保存排版结果');
    return;
  }
  const baseURL = import.meta.env.VITE_API_BASE || '/api';
  const params = new URLSearchParams({ toolRadius: String(toolRadius) });
  if (cutParams.cutDepth != null) params.set('cutDepth', cutParams.cutDepth);
  if (cutParams.cutFeed != null) params.set('cutFeed', cutParams.cutFeed);
  if (cutParams.spindleSpeed != null) params.set('spindleSpeed', cutParams.spindleSpeed);
  const url = `${baseURL}/layout-results/${resultId}/nc?${params}`;
  const token = localStorage.getItem(TOKEN_KEY);

  const response = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || 'NC文件导出失败');
  }

  const blob = await response.blob();
  const a = document.createElement('a');
  const objectUrl = URL.createObjectURL(blob);
  a.href = objectUrl;
  a.download = `刀轨_${new Date().toISOString().slice(0, 10)}.nc`;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(objectUrl);
}

export function exportToolpathSVG(canvasRef) {
  const svg = canvasRef?.value?.exportSVG?.();
  if (!svg) {
    ElMessage.warning('暂无排版结果可导出');
    return;
  }
  const blob = new Blob([svg], { type: 'image/svg+xml' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `刀轨_${new Date().toISOString().slice(0, 10)}.svg`;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  ElMessage.success('刀轨已导出');
}

export function exportResultJSON(solutions, settings, orderInfo) {
  if (!solutions?.length) {
    ElMessage.warning('暂无排版结果可导出');
    return;
  }
  const json = JSON.stringify({ solutions, settings: { ...settings }, orderInfo }, null, 2);
  const blob = new Blob([json], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `排版结果_${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  ElMessage.success('文件已导出');
}
