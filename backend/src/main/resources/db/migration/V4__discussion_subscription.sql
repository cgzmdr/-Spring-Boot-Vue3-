-- ============================================================================
-- V4 · 社区订阅（帖子 / 板块 / 用户的三级通知强度）
-- level：all 全部通知 / mention 仅被 @ 时通知 / off 免打扰（屏蔽该目标的通知）
-- 幂等脚本，可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V4__discussion_subscription.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS discussion_subscription (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL,
    -- topic 帖子 / board 板块 / user 用户
    target_type varchar(16) NOT NULL,
    target_id   uuid        NOT NULL,
    -- all / mention / off
    level       varchar(16) NOT NULL DEFAULT 'all',
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    updated_at  timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_subscription_unique UNIQUE (user_id, target_type, target_id),
    CONSTRAINT discussion_subscription_level_check CHECK (level IN ('all', 'mention', 'off'))
);
CREATE INDEX IF NOT EXISTS idx_subscription_user ON discussion_subscription (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subscription_target ON discussion_subscription (target_type, target_id);

-- updated_at 自动维护（复用库中已存在的 set_updated_at()）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_discussion_subscription_updated ON discussion_subscription;
        CREATE TRIGGER trg_discussion_subscription_updated
            BEFORE UPDATE ON discussion_subscription
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- 回填：已有帖子的作者默认订阅自己的帖子（便于「免打扰」开关生效）
INSERT INTO discussion_subscription (user_id, target_type, target_id, level)
SELECT author_id, 'topic', id, 'all'
FROM discussion_topic
WHERE status <> 'deleted'
ON CONFLICT (user_id, target_type, target_id) DO NOTHING;

-- 校验：SELECT target_type, level, count(*) FROM discussion_subscription GROUP BY 1,2;
