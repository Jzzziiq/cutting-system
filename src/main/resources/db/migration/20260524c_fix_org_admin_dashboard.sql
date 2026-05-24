-- Fix: org_admin needs order:read to access the Dashboard (工作台)
-- The DashboardController requires order:read; org_admin previously
-- lost this permission when it was narrowed in 20260524b.

INSERT INTO t_role_permission (role_id, perm_id)
SELECT r.role_id, p.perm_id
FROM t_role r, t_permission p
WHERE r.role_code = 'org_admin'
  AND p.perm_code = 'order:read'
  AND NOT EXISTS (
    SELECT 1 FROM t_role_permission rp
    WHERE rp.role_id = r.role_id AND rp.perm_id = p.perm_id
  );
