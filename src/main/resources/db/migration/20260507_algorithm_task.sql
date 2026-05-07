USE board_cutting_db;

CREATE TABLE t_algorithm_task (
    task_id VARCHAR(36) NOT NULL COMMENT 'UUID task id',
    user_id BIGINT NOT NULL COMMENT 'submitter',
    algorithm VARCHAR(30) NOT NULL COMMENT 'algorithm name: tabu_search, genetic_algorithm',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 completed, -1 failed',
    input_json JSON NOT NULL COMMENT 'InstanceDTO input snapshot',
    result_json JSON DEFAULT NULL COMMENT 'List<SolutionResponseDTO> output',
    best_rate DECIMAL(10, 4) DEFAULT NULL COMMENT 'best utilization rate',
    container_count INT DEFAULT NULL COMMENT 'number of containers used',
    duration_ms BIGINT DEFAULT NULL COMMENT 'execution duration',
    error_msg VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time DATETIME DEFAULT NULL,
    PRIMARY KEY (task_id),
    KEY idx_task_user (user_id),
    KEY idx_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='algorithm task queue';
