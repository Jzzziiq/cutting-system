USE board_cutting_db;

CREATE TABLE IF NOT EXISTS t_cabinet_template (
    id              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(100)   NOT NULL COMMENT '模板名称',
    category        VARCHAR(50)    NOT NULL COMMENT '品类枚举：wardrobe/base-cabinet',
    thumbnail       VARCHAR(255)   DEFAULT NULL COMMENT '缩略图URL',
    cabinet_json    JSON           NOT NULL COMMENT '柜体结构JSON',
    is_official     TINYINT        NOT NULL DEFAULT 0 COMMENT '官方预置=1，用户保存=0',
    created_by      BIGINT         DEFAULT NULL COMMENT '创建人ID，关联t_user',
    create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_ct_category (category),
    INDEX idx_ct_official (is_official),
    CONSTRAINT fk_ct_user FOREIGN KEY (created_by) REFERENCES t_user(user_id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='柜体设计预设模板表';
