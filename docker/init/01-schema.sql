mysqldump: [Warning] Using a password on the command line interface can be insecure.
CREATE TABLE `t_audit_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `user_id` bigint NOT NULL COMMENT 'operator user id',
  `username` varchar(50) NOT NULL COMMENT 'operator username snapshot',
  `module` varchar(30) NOT NULL COMMENT 'module name, e.g. customer/board/order',
  `action` varchar(30) NOT NULL COMMENT 'action type: create/update/delete/assign',
  `target_class` varchar(200) DEFAULT NULL COMMENT 'controller class name',
  `target_method` varchar(100) DEFAULT NULL COMMENT 'method name',
  `request_params` text COMMENT 'request params JSON snapshot',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'client IP',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT 'execution duration in ms',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0 success, 1 failure',
  `error_msg` varchar(500) DEFAULT NULL COMMENT 'error message when failed',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`log_id`),
  KEY `idx_audit_log_user` (`user_id`),
  KEY `idx_audit_log_module` (`module`),
  KEY `idx_audit_log_time` (`create_time`),
  CONSTRAINT `fk_audit_log_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='operation audit log';
CREATE TABLE `t_board` (
  `board_id` bigint NOT NULL AUTO_INCREMENT COMMENT '板材唯一ID（原材序号）',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户 ID',
  `brand` varchar(50) NOT NULL COMMENT '板材品牌',
  `material_type` varchar(50) NOT NULL COMMENT '板材材质',
  `color` varchar(50) NOT NULL COMMENT '板材颜色',
  `size_type` varchar(50) NOT NULL COMMENT '尺寸类型（标准板，加长板，自定义）',
  `width` int NOT NULL DEFAULT '1220' COMMENT '板材宽度(mm)',
  `length` int NOT NULL DEFAULT '2440' COMMENT '板材长度(mm)',
  `thickness` int NOT NULL DEFAULT '18' COMMENT '板材厚度(mm)',
  `use_count` int DEFAULT '0' COMMENT '使用频次',
  `last_use_time` datetime DEFAULT NULL COMMENT '最后调用时间',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：1=启用，0=禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`board_id`),
  KEY `idx_material` (`brand`,`material_type`,`color`),
  KEY `idx_sort` (`last_use_time`,`use_count`),
  KEY `idx_board_user` (`user_id`),
  CONSTRAINT `fk_board_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='板材表';
CREATE TABLE `t_customer` (
  `customer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户唯一ID',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户 ID',
  `customer_name` varchar(100) NOT NULL COMMENT '客户姓名/公司名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '客户地址',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：1=启用，0=禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`customer_id`),
  KEY `idx_name` (`customer_name`),
  KEY `idx_customer_user` (`user_id`),
  KEY `idx_customer_phone` (`phone`),
  CONSTRAINT `fk_customer_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户表';
CREATE TABLE `t_layout_result` (
  `result_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'layout result id',
  `order_id` bigint NOT NULL COMMENT 'order id',
  `user_id` bigint NOT NULL COMMENT 'owner user id',
  `usage_rate` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT 'material usage rate, 0 to 1',
  `total_area` decimal(16,2) DEFAULT NULL COMMENT 'total cutting area in square millimeters',
  `container_count` int NOT NULL DEFAULT '0' COMMENT 'used board/container count',
  `result_json` json NOT NULL COMMENT 'layout result json',
  `image_path` varchar(500) DEFAULT NULL COMMENT 'exported layout image path',
  `nc_file_path` varchar(500) DEFAULT NULL COMMENT 'exported NC file path',
  `label_file_path` varchar(500) DEFAULT NULL COMMENT 'exported label file path',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`result_id`),
  KEY `idx_layout_result_order` (`order_id`),
  KEY `idx_layout_result_user_time` (`user_id`,`create_time`),
  CONSTRAINT `fk_layout_result_order` FOREIGN KEY (`order_id`) REFERENCES `t_order` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_layout_result_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='layout result table';
CREATE TABLE `t_offcut` (
  `offcut_id` bigint NOT NULL AUTO_INCREMENT COMMENT '余料唯一ID',
  `user_id` bigint NOT NULL COMMENT 'owner user id',
  `board_id` bigint NOT NULL COMMENT '关联板材ID',
  `source_order_id` bigint DEFAULT NULL COMMENT '来源订单ID',
  `width` int NOT NULL COMMENT '余料宽度(mm)',
  `length` int NOT NULL COMMENT '余料长度(mm)',
  `thickness` int NOT NULL COMMENT '余料厚度(mm)',
  `material_type` varchar(50) DEFAULT NULL COMMENT 'material type snapshot',
  `brand` varchar(50) DEFAULT NULL COMMENT 'brand snapshot',
  `color` varchar(50) DEFAULT NULL COMMENT 'color snapshot',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '余料状态：1=可用，2=已用完，3=作废禁用',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1 enabled, 0 disabled',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`offcut_id`),
  KEY `idx_board` (`board_id`,`status`),
  KEY `idx_order` (`source_order_id`),
  KEY `idx_offcut_user_status` (`user_id`,`is_enabled`,`status`),
  KEY `idx_offcut_material` (`brand`,`material_type`,`color`),
  CONSTRAINT `fk_offcut_board` FOREIGN KEY (`board_id`) REFERENCES `t_board` (`board_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_offcut_source_order` FOREIGN KEY (`source_order_id`) REFERENCES `t_order` (`order_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_offcut_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='余料表';
CREATE TABLE `t_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单唯一ID',
  `user_id` bigint NOT NULL COMMENT 'owner user id',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `customer_name` varchar(100) DEFAULT NULL COMMENT 'customer name snapshot',
  `customer_address` varchar(255) DEFAULT NULL COMMENT 'customer address snapshot',
  `process_name` varchar(100) DEFAULT NULL COMMENT '加工单名称',
  `order_status` tinyint NOT NULL DEFAULT '1' COMMENT '订单状态：1=待加工，2=加工中，3=已完成，4=已取消',
  `raw_material_json` json DEFAULT NULL COMMENT 'raw material config snapshot',
  `remnant_json` json DEFAULT NULL COMMENT 'remnant config snapshot',
  `config_json` json DEFAULT NULL COMMENT 'layout config json',
  `layout_result_id` bigint DEFAULT NULL COMMENT 'current layout result id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `finish_time` datetime DEFAULT NULL COMMENT '订单完成时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_status` (`order_status`,`create_time`),
  KEY `idx_order_user_status` (`user_id`,`order_status`,`create_time`),
  KEY `idx_order_layout_result` (`layout_result_id`),
  CONSTRAINT `fk_order_customer` FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`customer_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_layout_result` FOREIGN KEY (`layout_result_id`) REFERENCES `t_layout_result` (`result_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';
CREATE TABLE `t_order_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '下料条目唯一ID',
  `user_id` bigint NOT NULL COMMENT 'owner user id',
  `order_id` bigint NOT NULL COMMENT '所属订单ID',
  `part_name` varchar(100) DEFAULT NULL COMMENT '部件名称',
  `part_code` varchar(50) DEFAULT NULL COMMENT '部件编号',
  `board_id` bigint NOT NULL COMMENT '所用板材ID',
  `offcut_id` bigint DEFAULT NULL COMMENT '所用余料ID',
  `width` int NOT NULL COMMENT '下料宽度(mm)',
  `length` int NOT NULL COMMENT '下料长度(mm)',
  `thickness` int NOT NULL COMMENT '下料厚度(mm)',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '加工数量',
  `material_name` varchar(100) DEFAULT NULL COMMENT 'material name snapshot',
  `color` varchar(50) DEFAULT NULL COMMENT 'color snapshot',
  `edge_left` tinyint NOT NULL DEFAULT '0' COMMENT '左封边：1=封边，0=不封边',
  `edge_right` tinyint NOT NULL DEFAULT '0' COMMENT '右封边：1=封边，0=不封边',
  `edge_front` tinyint NOT NULL DEFAULT '0' COMMENT '前封边：1=封边，0=不封边',
  `edge_back` tinyint NOT NULL DEFAULT '0' COMMENT '后封边：1=封边，0=不封边',
  `edge_top` tinyint NOT NULL DEFAULT '0' COMMENT 'top edge banding flag',
  `edge_bottom` tinyint NOT NULL DEFAULT '0' COMMENT 'bottom edge banding flag',
  `is_texture` tinyint NOT NULL DEFAULT '0' COMMENT '纹理匹配：1=是，0=否',
  `allow_rotation` tinyint NOT NULL DEFAULT '0' COMMENT '1 allow rotation, 0 not allow',
  `label` varchar(100) DEFAULT NULL COMMENT 'part label',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`item_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_board` (`board_id`),
  KEY `idx_offcut` (`offcut_id`),
  KEY `idx_order_item_user_order` (`user_id`,`order_id`),
  KEY `idx_order_item_material` (`order_id`,`material_name`,`color`),
  CONSTRAINT `fk_order_item_board` FOREIGN KEY (`board_id`) REFERENCES `t_board` (`board_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_order_item_offcut` FOREIGN KEY (`offcut_id`) REFERENCES `t_offcut` (`offcut_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `t_order` (`order_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_order_item_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单下料明细表';
CREATE TABLE `t_permission` (
  `perm_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `perm_code` varchar(50) NOT NULL COMMENT 'permission code, e.g. customer:write',
  `perm_name` varchar(50) NOT NULL COMMENT 'display name',
  `description` varchar(200) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`perm_id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='permission';
CREATE TABLE `t_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `role_code` varchar(30) NOT NULL COMMENT 'role code, e.g. admin/operator/viewer',
  `role_name` varchar(50) NOT NULL COMMENT 'display name',
  `description` varchar(200) DEFAULT NULL COMMENT 'role description',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='role';
CREATE TABLE `t_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `perm_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`perm_id`),
  KEY `fk_rp_perm` (`perm_id`),
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`perm_id`) REFERENCES `t_permission` (`perm_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='role-permission mapping';
CREATE TABLE `t_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户唯一 ID',
  `username` varchar(50) NOT NULL COMMENT '登录用户名，全局唯一',
  `password` char(32) NOT NULL COMMENT '登录密码，MD5 加密存储',
  `real_name` varchar(50) DEFAULT NULL COMMENT '用户真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `role_type` tinyint NOT NULL DEFAULT '2' COMMENT '角色类型：1 = 系统管理员，2 = 生产人员；注册账号默认为生产人员',
  `account_status` tinyint NOT NULL DEFAULT '3' COMMENT '账号状态：1 = 正常启用，2 = 禁用，3 = 待审批；注册后默认为待审批',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后一次登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注信息',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_status` (`role_type`,`account_status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储系统所有用户账号信息，区分管理员与生产人员双角色，实现账号权限管控与注册审批流程';
CREATE TABLE `t_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `fk_user_role_role` (`role_id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`role_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user-role mapping';

INSERT INTO t_role (role_code, role_name, description) VALUES
('admin', '超级管理员', '系统全部权限'),
('operator', '操作员', '业务操作与排样执行'),
('viewer', '观察员', '只读访问');

INSERT INTO t_permission (perm_code, perm_name, description) VALUES
('user:manage', '用户管理', '查看、编辑用户与分配角色'),
('customer:write', '客户编辑', '新增、修改、删除客户'),
('customer:read', '客户查看', '查看客户信息'),
('board:write', '板材编辑', '新增、修改、删除板材'),
('board:read', '板材查看', '查看板材信息'),
('order:write', '订单编辑', '新增、修改、删除订单'),
('order:read', '订单查看', '查看订单信息'),
('algorithm:execute', '算法执行', '提交排样计算'),
('layout:read', '排样查看', '查看排样结果');

INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='admin'), perm_id FROM t_permission;

INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='operator'), perm_id
FROM t_permission WHERE perm_code <> 'user:manage';

INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='viewer'), perm_id
FROM t_permission WHERE perm_code IN ('customer:read', 'board:read', 'order:read', 'layout:read');

INSERT INTO t_user (username, password, real_name, role_type, account_status) VALUES
('admin', '827ccb0eea8a706c4c34a16891f84e7b', '系统管理员', 1, 1);

INSERT INTO t_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'admin';
