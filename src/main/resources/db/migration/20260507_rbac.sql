-- RBAC: role-based access control tables.
-- Run with: USE board_cutting_db; source this file;

USE board_cutting_db;

-- 1. Role table
CREATE TABLE t_role (
    role_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    role_code VARCHAR(30) NOT NULL COMMENT 'role code, e.g. admin/operator/viewer',
    role_name VARCHAR(50) NOT NULL COMMENT 'display name',
    description VARCHAR(200) DEFAULT NULL COMMENT 'role description',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role';

-- 2. Permission table
CREATE TABLE t_permission (
    perm_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    perm_code VARCHAR(50) NOT NULL COMMENT 'permission code, e.g. customer:write',
    perm_name VARCHAR(50) NOT NULL COMMENT 'display name',
    description VARCHAR(200) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (perm_id),
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='permission';

-- 3. User-role join table
CREATE TABLE t_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user-role mapping';

-- 4. Role-permission join table
CREATE TABLE t_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    perm_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES t_role(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (perm_id) REFERENCES t_permission(perm_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role-permission mapping';

-- 5. Seed roles
INSERT INTO t_role (role_code, role_name, description) VALUES
('admin', '超级管理员', '系统全部权限'),
('operator', '操作员', '业务操作与排样执行'),
('viewer', '观察员', '只读访问');

-- 6. Seed permissions
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

-- 7. Assign permissions to roles
-- admin: all permissions
INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='admin'), perm_id FROM t_permission;

-- operator: all except user:manage
INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='operator'), perm_id
FROM t_permission WHERE perm_code <> 'user:manage';

-- viewer: read-only
INSERT INTO t_role_permission (role_id, perm_id)
SELECT (SELECT role_id FROM t_role WHERE role_code='viewer'), perm_id
FROM t_permission WHERE perm_code IN ('customer:read', 'board:read', 'order:read', 'layout:read');

-- 8. Migrate existing users to roles (roleType: 1=admin, 2=operator)
INSERT INTO t_user_role (user_id, role_id)
SELECT u.user_id, (SELECT role_id FROM t_role WHERE role_code = CASE WHEN u.role_type = 1 THEN 'admin' ELSE 'operator' END)
FROM t_user u
WHERE NOT EXISTS (SELECT 1 FROM t_user_role ur WHERE ur.user_id = u.user_id);
