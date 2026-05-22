const MAX_LENGTH = 3000;
const MAX_WIDTH = 1500;

export function validateWorkpieceDimensions(item, board = null) {
  const errors = {};
  const l = Number(item.length);
  const w = Number(item.width);
  const qty = Number(item.quantity);

  if (item.length === '' || item.length == null || !Number.isFinite(l) || l <= 0) {
    errors.length = '必填';
  } else if (l > MAX_LENGTH) {
    errors.length = `异常(>${MAX_LENGTH}mm)`;
  } else if (board && l > board.length) {
    errors.length = `超出板材(${board.length}mm)`;
  }

  if (item.width === '' || item.width == null || !Number.isFinite(w) || w <= 0) {
    errors.width = '必填';
  } else if (w > MAX_WIDTH) {
    errors.width = `异常(>${MAX_WIDTH}mm)`;
  } else if (board && w > board.width) {
    errors.width = `超出板材(${board.width}mm)`;
  }

  if (item.quantity === '' || item.quantity == null || !Number.isFinite(qty) || qty <= 0) {
    errors.quantity = '必填';
  }

  return errors;
}
