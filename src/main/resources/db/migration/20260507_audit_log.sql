-- Audit log table for recording user write operations.
-- Run with: USE board_cutting_db; source this file;

USE board_cutting_db;

CREATE TABLE t_audit_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    user_id BIGINT NOT NULL COMMENT 'operator user id',
    username VARCHAR(50) NOT NULL COMMENT 'operator username snapshot',
    module VARCHAR(30) NOT NULL COMMENT 'module name, e.g. customer/board/order',
    action VARCHAR(30) NOT NULL COMMENT 'action type: create/update/delete/assign',
    target_class VARCHAR(200) DEFAULT NULL COMMENT 'controller class name',
    target_method VARCHAR(100) DEFAULT NULL COMMENT 'method name',
    request_params TEXT DEFAULT NULL COMMENT 'request params JSON snapshot',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT 'client IP',
    duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT 'execution duration in ms',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 success, 1 failure',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT 'error message when failed',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    PRIMARY KEY (log_id),
    KEY idx_audit_log_user (user_id),
    KEY idx_audit_log_module (module),
    KEY idx_audit_log_time (create_time),
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='operation audit log';
