import { ElMessage } from 'element-plus/es/components/message/index.mjs';

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
