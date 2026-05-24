-- Role/permission refactoring: clarify role boundaries
-- New permissions: org:manage, account:manage, audit:read, production:read

-- 1. New permissions
INSERT IGNORE INTO t_permission (perm_code, perm_name, description) VALUES
('org:manage', '组织管理', '管理系统组织'),
('account:manage', '账号管理', '管理系统所有用户账号'),
('audit:read', '审计日志', '查看操作审计日志'),
('production:read', '任务查看', '查看分配给自己的生产任务');

-- 2. Clear existing role-permission mappings
DELETE FROM t_role_permission;

-- 3. Re-assign permissions per role

-- admin: system-level only (org:manage + account:manage)
INSERT INTO t_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM t_role r, t_permission p
WHERE r.role_code = 'admin' AND p.perm_code IN ('org:manage','account:manage');

-- org_admin: all business + user:manage + audit:read (no org:manage / account:manage)
INSERT INTO t_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM t_role r, t_permission p
WHERE r.role_code = 'org_admin'
  AND p.perm_code NOT IN ('org:manage','account:manage');

-- operator: business permissions only (no user:manage / audit:read / org:manage / account:manage / production:read)
-- production:read is viewer-only; operator uses order:read for task access
INSERT INTO t_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM t_role r, t_permission p
WHERE r.role_code = 'operator'
  AND p.perm_code NOT IN ('user:manage','audit:read','org:manage','account:manage','production:read');

-- viewer: only production:read
INSERT INTO t_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM t_role r, t_permission p
WHERE r.role_code = 'viewer' AND p.perm_code = 'production:read';

-- 4. Admin should not belong to any organization
UPDATE t_user SET org_id = NULL, org_role = NULL WHERE username = 'admin';

-- 5. Assign org_admin role to zhaoliu (org_admin user from seed data)
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.user_id, r.role_id FROM t_user u, t_role r
WHERE u.username = 'zhaoliu' AND r.role_code = 'org_admin';
