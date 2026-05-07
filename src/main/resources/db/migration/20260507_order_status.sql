USE board_cutting_db;

-- Add status history column for tracking state transitions
ALTER TABLE t_order
    ADD COLUMN status_history JSON DEFAULT NULL COMMENT 'state transition log' AFTER order_status;
