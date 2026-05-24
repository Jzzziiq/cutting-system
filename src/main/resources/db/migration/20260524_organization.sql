-- Organization feature: data isolation from user_id to org_id
-- Run with: USE board_cutting_db; source this file;

USE board_cutting_db;

-- ============================================================
-- 1. Organization table
-- ============================================================

CREATE TABLE t_organization (
    org_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '组织ID',
    org_name     VARCHAR(100) NOT NULL COMMENT '组织名称',
    org_code     VARCHAR(50)  NOT NULL COMMENT '组织编码，用于注册时输入',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 2=禁用',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (org_id),
    UNIQUE KEY uk_org_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织';

-- ============================================================
-- 2. Add org_id and org_role to t_user
-- ============================================================

ALTER TABLE t_user ADD COLUMN org_id BIGINT DEFAULT NULL COMMENT '所属组织ID';
ALTER TABLE t_user ADD COLUMN org_role VARCHAR(30) DEFAULT NULL COMMENT '组织内角色: org_admin/operator/viewer';
ALTER TABLE t_user ADD INDEX idx_user_org (org_id);

-- ============================================================
-- 3. Add org_id to business tables
-- ============================================================

ALTER TABLE t_customer ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_customer_org ON t_customer(org_id);

ALTER TABLE t_board ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_board_org ON t_board(org_id);

ALTER TABLE t_order ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_order_org ON t_order(org_id);

ALTER TABLE t_order_item ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_order_item_org ON t_order_item(org_id);

ALTER TABLE t_offcut ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_offcut_org ON t_offcut(org_id);

ALTER TABLE t_layout_result ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_layout_result_org ON t_layout_result(org_id);

ALTER TABLE t_production_task ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_production_task_org ON t_production_task(org_id);

ALTER TABLE t_algorithm_task ADD COLUMN org_id BIGINT DEFAULT NULL;
CREATE INDEX idx_algorithm_task_org ON t_algorithm_task(org_id);

-- ============================================================
-- 4. Seed default organization
-- ============================================================

INSERT INTO t_organization (org_name, org_code, status) VALUES ('默认组织', 'DEFAULT', 1);

-- ============================================================
-- 5. Migrate existing users to default organization
-- ============================================================

UPDATE t_user SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');

-- Set org_role based on existing RBAC roles
UPDATE t_user SET org_role = 'org_admin' WHERE role_type = 1;
UPDATE t_user SET org_role = 'operator' WHERE role_type = 2 AND username != 'viewer01';
UPDATE t_user SET org_role = 'viewer' WHERE username = 'viewer01';

-- ============================================================
-- 6. Migrate existing business data to default organization
-- ============================================================

UPDATE t_customer SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_board SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_order SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_order_item SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_offcut SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_layout_result SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_production_task SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');
UPDATE t_algorithm_task SET org_id = (SELECT org_id FROM t_organization WHERE org_code = 'DEFAULT');

-- ============================================================
-- 7. Add org_admin role and permissions
-- ============================================================

INSERT INTO t_role (role_code, role_name, description) VALUES
('org_admin', '组织管理员', '组织内管理权限，可管理用户和客户板材');

-- org_admin permissions: customer/board read+write, order:read, layout:read, user:manage
INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='org_admin'), perm_id
FROM t_permission WHERE perm_code IN (
    'customer:read', 'customer:write', 'board:read', 'board:write',
    'order:read', 'layout:read', 'user:manage'
);

-- ============================================================
-- 8. Assign org_admin role to existing org_admin users
-- ============================================================

INSERT INTO t_user_role (user_id, role_id)
SELECT u.user_id, (SELECT role_id FROM t_role WHERE role_code = 'org_admin')
FROM t_user u
WHERE u.org_role = 'org_admin'
  AND NOT EXISTS (
    SELECT 1 FROM t_user_role ur
    WHERE ur.user_id = u.user_id
      AND ur.role_id = (SELECT role_id FROM t_role WHERE role_code = 'org_admin')
  );

-- ============================================================
-- 9. Add t_organization to tenant ignore list (done in Java)
-- ============================================================
