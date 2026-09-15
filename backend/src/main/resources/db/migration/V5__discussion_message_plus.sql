-- ============================================================================
-- V5 · 私信增强：图片、撤回、拉黑
-- 幂等脚本，可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V5__discussion_message_plus.sql
-- ============================================================================

-- 私信图片（最多 4 张，复用 /uploads/image 上传）
ALTER TABLE discussion_message ADD COLUMN IF NOT EXISTS images jsonb NOT NULL DEFAULT '[]'::jsonb;
-- 撤回标记（发送者 2 分钟内可撤回；撤回后正文清空、保留占位）
ALTER TABLE discussion_message ADD COLUMN IF NOT EXISTS recalled boolean NOT NULL DEFAULT false;
ALTER TABLE discussion_message ADD COLUMN IF NOT EXISTS recalled_at timestamp with time zone;

-- 拉黑关系（单向）：拉黑后双方不能互发私信，并解除已有互关
CREATE TABLE IF NOT EXISTS discussion_block (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL,
    blocked_user_id uuid NOT NULL,
    created_at      timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_block_unique UNIQUE (user_id, blocked_user_id),
    CONSTRAINT discussion_block_no_self CHECK (user_id <> blocked_user_id)
);
CREATE INDEX IF NOT EXISTS idx_block_user ON discussion_block (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_block_target ON discussion_block (blocked_user_id);

-- 校验：
-- SELECT count(*) FROM discussion_block;
-- SELECT column_name FROM information_schema.columns WHERE table_name='discussion_message';
