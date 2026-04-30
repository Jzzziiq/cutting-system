-- Align schema with the graduation-design documents.
-- Target database: board_cutting_db, MySQL 8.x.
-- Run a database backup before executing this file.

USE board_cutting_db;

-- 1. Offcut/remnant table: add ownership and material snapshot fields.
ALTER TABLE t_offcut
    ADD COLUMN user_id BIGINT NOT NULL COMMENT 'owner user id' AFTER offcut_id,
    ADD COLUMN material_type VARCHAR(50) DEFAULT NULL COMMENT 'material type snapshot' AFTER thickness,
    ADD COLUMN brand VARCHAR(50) DEFAULT NULL COMMENT 'brand snapshot' AFTER material_type,
    ADD COLUMN color VARCHAR(50) DEFAULT NULL COMMENT 'color snapshot' AFTER brand,
    ADD COLUMN is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled' AFTER status;

-- 2. Order table: add ownership, customer snapshot, algorithm input snapshots, and current layout result reference.
ALTER TABLE t_order
    ADD COLUMN user_id BIGINT NOT NULL COMMENT 'owner user id' AFTER order_id,
    ADD COLUMN customer_name VARCHAR(100) DEFAULT NULL COMMENT 'customer name snapshot' AFTER customer_id,
    ADD COLUMN customer_address VARCHAR(255) DEFAULT NULL COMMENT 'customer address snapshot' AFTER customer_name,
    ADD COLUMN raw_material_json JSON DEFAULT NULL COMMENT 'raw material config snapshot' AFTER order_status,
    ADD COLUMN remnant_json JSON DEFAULT NULL COMMENT 'remnant config snapshot' AFTER raw_material_json,
    ADD COLUMN config_json JSON DEFAULT NULL COMMENT 'layout config json' AFTER remnant_json,
    ADD COLUMN layout_result_id BIGINT DEFAULT NULL COMMENT 'current layout result id' AFTER config_json;

-- 3. Order item table: add ownership, material snapshot, edge aliases, rotation flag, and label.
ALTER TABLE t_order_item
    ADD COLUMN user_id BIGINT NOT NULL COMMENT 'owner user id' AFTER item_id,
    ADD COLUMN material_name VARCHAR(100) DEFAULT NULL COMMENT 'material name snapshot' AFTER quantity,
    ADD COLUMN color VARCHAR(50) DEFAULT NULL COMMENT 'color snapshot' AFTER material_name,
    ADD COLUMN edge_top TINYINT NOT NULL DEFAULT 0 COMMENT 'top edge banding flag' AFTER edge_back,
    ADD COLUMN edge_bottom TINYINT NOT NULL DEFAULT 0 COMMENT 'bottom edge banding flag' AFTER edge_top,
    ADD COLUMN allow_rotation TINYINT NOT NULL DEFAULT 0 COMMENT '1 allow rotation, 0 not allow' AFTER is_texture,
    ADD COLUMN label VARCHAR(100) DEFAULT NULL COMMENT 'part label' AFTER allow_rotation;

-- 4. Layout result table: store algorithm output and generated export file paths.
CREATE TABLE t_layout_result (
    result_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'layout result id',
    order_id BIGINT NOT NULL COMMENT 'order id',
    user_id BIGINT NOT NULL COMMENT 'owner user id',
    usage_rate DECIMAL(10, 4) NOT NULL DEFAULT 0.0000 COMMENT 'material usage rate, 0 to 1',
    total_area DECIMAL(16, 2) DEFAULT NULL COMMENT 'total cutting area in square millimeters',
    container_count INT NOT NULL DEFAULT 0 COMMENT 'used board/container count',
    result_json JSON NOT NULL COMMENT 'layout result json',
    image_path VARCHAR(500) DEFAULT NULL COMMENT 'exported layout image path',
    nc_file_path VARCHAR(500) DEFAULT NULL COMMENT 'exported NC file path',
    label_file_path VARCHAR(500) DEFAULT NULL COMMENT 'exported label file path',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    PRIMARY KEY (result_id),
    KEY idx_layout_result_order (order_id),
    KEY idx_layout_result_user_time (user_id, create_time),
    CONSTRAINT fk_layout_result_order
        FOREIGN KEY (order_id) REFERENCES t_order(order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_layout_result_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='layout result table';

-- 5. Add indexes.
ALTER TABLE t_board
    ADD KEY idx_board_user (user_id);

ALTER TABLE t_customer
    ADD KEY idx_customer_user (user_id),
    ADD KEY idx_customer_phone (phone);

ALTER TABLE t_offcut
    ADD KEY idx_offcut_user_status (user_id, is_enabled, status),
    ADD KEY idx_offcut_material (brand, material_type, color);

ALTER TABLE t_order
    ADD KEY idx_order_user_status (user_id, order_status, create_time),
    ADD KEY idx_order_layout_result (layout_result_id);

ALTER TABLE t_order_item
    ADD KEY idx_order_item_user_order (user_id, order_id),
    ADD KEY idx_order_item_material (order_id, material_name, color);

-- 6. Add foreign keys.
ALTER TABLE t_board
    ADD CONSTRAINT fk_board_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE t_customer
    ADD CONSTRAINT fk_customer_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE t_offcut
    ADD CONSTRAINT fk_offcut_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_offcut_board
        FOREIGN KEY (board_id) REFERENCES t_board(board_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_offcut_source_order
        FOREIGN KEY (source_order_id) REFERENCES t_order(order_id)
        ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE t_order
    ADD CONSTRAINT fk_order_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id) REFERENCES t_customer(customer_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_order_layout_result
        FOREIGN KEY (layout_result_id) REFERENCES t_layout_result(result_id)
        ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE t_order_item
    ADD CONSTRAINT fk_order_item_user
        FOREIGN KEY (user_id) REFERENCES t_user(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id) REFERENCES t_order(order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    ADD CONSTRAINT fk_order_item_board
        FOREIGN KEY (board_id) REFERENCES t_board(board_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_order_item_offcut
        FOREIGN KEY (offcut_id) REFERENCES t_offcut(offcut_id)
        ON DELETE SET NULL ON UPDATE CASCADE;
