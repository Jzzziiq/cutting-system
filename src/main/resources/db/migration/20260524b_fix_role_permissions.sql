-- Fix: narrow org_admin permissions and expand operator permissions
-- org_admin loses order:read, order:write, layout:read (operator/producer pages)
-- operator gains layout:read (for layout workbench)

-- 1. Remove order:read, order:write, layout:read from org_admin
DELETE rp FROM t_role_permission rp
JOIN t_role r ON rp.role_id = r.role_id
JOIN t_permission p ON rp.perm_id = p.perm_id
WHERE r.role_code = 'org_admin'
  AND p.perm_code IN ('order:read', 'order:write', 'layout:read');

-- 2. Replace operator's implicit permission list with explicit list (adds layout:read)
DELETE rp FROM t_role_permission rp
JOIN t_role r ON rp.role_id = r.role_id
WHERE r.role_code = 'operator';

INSERT INTO t_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id FROM t_role r, t_permission p
WHERE r.role_code = 'operator'
  AND p.perm_code IN (
    'customer:read','customer:write',
    'board:read','board:write',
    'order:read','order:write',
    'algorithm:execute','layout:read'
  );
