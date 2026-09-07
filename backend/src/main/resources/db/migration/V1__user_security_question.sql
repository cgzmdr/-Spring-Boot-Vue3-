-- ============================================================================
-- 个人中心 · 密保问题（修改密码的验证方式之一）
-- 适用数据库：PostgreSQL
-- 执行方式：psql -h <host> -U <user> -d 56_app -f V1__user_security_question.sql
-- 幂等：可重复执行（IF NOT EXISTS）
-- ============================================================================

ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS security_question varchar(255) NULL,
    ADD COLUMN IF NOT EXISTS security_answer  varchar(255) NULL;

COMMENT ON COLUMN user_account.security_question IS '密保问题（明文问题文本，为空表示未设置）';
COMMENT ON COLUMN user_account.security_answer IS '密保答案（AES 加密存储）';
