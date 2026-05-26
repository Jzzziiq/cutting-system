-- 简化订单状态机：移除 待审核(0) 和 已驳回(-1)
-- 待审核(0) → 已创建(1)
-- 已驳回(-1) → 已取消(-2)

UPDATE t_order SET order_status = 1 WHERE order_status = 0;
UPDATE t_order SET order_status = -2 WHERE order_status = -1;
