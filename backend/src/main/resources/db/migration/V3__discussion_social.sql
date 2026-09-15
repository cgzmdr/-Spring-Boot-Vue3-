-- ============================================================================
-- V3 · 社区社交关系（关注 / 粉丝 / 私信）
-- 私信策略（产品决议）：仅「互相关注」的用户之间可发起会话，避免陌生人骚扰。
-- 幂等脚本，可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V3__discussion_social.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 关注关系（单向；互相关注即成为好友，可互发私信）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_follow (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL,
    follow_user_id uuid NOT NULL,
    created_at     timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_follow_unique UNIQUE (user_id, follow_user_id),
    CONSTRAINT discussion_follow_no_self CHECK (user_id <> follow_user_id)
);
CREATE INDEX IF NOT EXISTS idx_follow_user ON discussion_follow (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_follow_target ON discussion_follow (follow_user_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- 私信会话（两人唯一：user_a < user_b，避免同两人产生两条会话）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_conversation (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_a             uuid NOT NULL,
    user_b             uuid NOT NULL,
    last_message_at    timestamp with time zone,
    last_preview       varchar(200),
    last_sender_id     uuid,
    unread_a           integer NOT NULL DEFAULT 0,
    unread_b           integer NOT NULL DEFAULT 0,
    created_at         timestamp with time zone NOT NULL DEFAULT now(),
    updated_at         timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_conversation_unique UNIQUE (user_a, user_b),
    CONSTRAINT discussion_conversation_order CHECK (user_a < user_b)
);
CREATE INDEX IF NOT EXISTS idx_conversation_a ON discussion_conversation (user_a, last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversation_b ON discussion_conversation (user_b, last_message_at DESC);

-- ---------------------------------------------------------------------------
-- 私信消息
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_message (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id uuid NOT NULL,
    sender_id       uuid NOT NULL,
    content         text NOT NULL,
    lang            varchar(8) NOT NULL DEFAULT 'zh',
    read_at         timestamp with time zone,
    created_at      timestamp with time zone NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_message_conversation ON discussion_message (conversation_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- 通知的目标类型扩容：需要容纳社区相关类型名（如 discussion_conversation，23 字符）
-- ---------------------------------------------------------------------------
ALTER TABLE notification ALTER COLUMN target_type TYPE varchar(32);

-- ---------------------------------------------------------------------------
-- updated_at 自动维护（复用库中已存在的 set_updated_at()）
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_discussion_conversation_updated ON discussion_conversation;
        CREATE TRIGGER trg_discussion_conversation_updated
            BEFORE UPDATE ON discussion_conversation
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- 校验：SELECT count(*) FROM discussion_follow;
