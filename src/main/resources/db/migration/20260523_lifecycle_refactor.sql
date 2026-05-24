-- 订单添加排单日期
ALTER TABLE t_order ADD COLUMN dispatch_date DATE DEFAULT NULL COMMENT '排单日期' AFTER process_name;

-- 排版结果关联的生产任务状态（冗余字段，用于历史面板展示，由任务状态变更时同步更新）
ALTER TABLE t_layout_result ADD COLUMN task_status INT DEFAULT NULL COMMENT '关联生产任务状态 0=待生产 1=生产中 2=已完成' AFTER label_file_path;
