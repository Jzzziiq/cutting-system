export function boardLabel(board) {
  return [board?.brand, board?.materialType, board?.color, board?.sizeType]
    .filter(Boolean).join(' ') || '未知板材';
}

export function dimLabel(board) {
  return `${board?.length || '-'} × ${board?.width || '-'} × ${board?.thickness || '-'} mm`;
}
