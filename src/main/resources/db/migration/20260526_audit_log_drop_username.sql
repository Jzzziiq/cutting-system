-- Drop username snapshot column from audit log; operator name resolved via JOIN at query time
ALTER TABLE t_audit_log DROP COLUMN username;
