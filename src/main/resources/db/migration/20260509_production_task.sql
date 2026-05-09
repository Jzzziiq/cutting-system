USE board_cutting_db;

CREATE TABLE IF NOT EXISTS t_production_task (
    task_id         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         BIGINT         NOT NULL COMMENT '创建用户ID',
    order_id        BIGINT         NOT NULL COMMENT '关联订单ID',
    layout_result_id BIGINT        DEFAULT NULL COMMENT '关联排样结果ID',
    task_name       VARCHAR(100)   NOT NULL COMMENT '任务名称',
    assignee_id     BIGINT         DEFAULT NULL COMMENT '指派人ID(t_user)',
    assignee_name   VARCHAR(50)    DEFAULT NULL COMMENT '指派人姓名(快照)',
    estimated_hours DECIMAL(5,1)   DEFAULT NULL COMMENT '预估工时(小时)',
    actual_hours    DECIMAL(5,1)   DEFAULT NULL COMMENT '实际工时(小时)',
    status          INT            NOT NULL DEFAULT 0 COMMENT '状态:0-待生产,1-生产中,2-已完成',
    start_time      DATETIME       DEFAULT NULL COMMENT '开始时间',
    complete_time   DATETIME       DEFAULT NULL COMMENT '完成时间',
    remark          VARCHAR(500)   DEFAULT NULL COMMENT '备注',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (task_id),
    INDEX idx_pt_order (order_id),
    INDEX idx_pt_assignee (assignee_id),
    INDEX idx_pt_status (status),
    CONSTRAINT fk_pt_order FOREIGN KEY (order_id) REFERENCES t_order(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_user FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产任务表';
