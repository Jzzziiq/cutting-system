CREATE TABLE t_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收用户ID',
  title VARCHAR(100) NOT NULL COMMENT '通知标题',
  content VARCHAR(500) COMMENT '通知内容',
  task_id BIGINT COMMENT '关联任务ID',
  is_read TINYINT DEFAULT 0 COMMENT '0=未读 1=已读',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_read (user_id, is_read),
  INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
