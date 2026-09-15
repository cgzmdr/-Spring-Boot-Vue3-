-- ============================================================================
-- V2 · 讨论区/社区（迭代 1：板块 / 帖子 / 楼层 / 举报 / 敏感词 / 通知 / 审核日志）
-- 幂等脚本，可重复执行；命名与既有表保持一致（snake_case、jsonb、gen_random_uuid）
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V2__discussion.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 板块
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_board (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    slug         varchar(64)  NOT NULL UNIQUE,
    name         varchar(64)  NOT NULL,
    name_en      varchar(64),
    description  varchar(500),
    icon         varchar(32),
    theme_color  varchar(7),
    order_num    integer      NOT NULL DEFAULT 0,
    status       varchar(16)  NOT NULL DEFAULT 'active',
    -- post_then_review：先发后审（默认）；review_then_post：先审后发
    post_policy  varchar(20)  NOT NULL DEFAULT 'post_then_review',
    min_trust_level integer   NOT NULL DEFAULT 0,
    topic_count  integer      NOT NULL DEFAULT 0,
    created_at   timestamp with time zone NOT NULL DEFAULT now(),
    updated_at   timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_board_status_check CHECK (status IN ('active', 'hidden'))
);

-- ---------------------------------------------------------------------------
-- 帖子
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_topic (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id      uuid         NOT NULL,
    author_id     uuid         NOT NULL,
    title         varchar(200) NOT NULL,
    content       text         NOT NULL,
    images        jsonb        NOT NULL DEFAULT '[]'::jsonb,
    lang          varchar(8)   NOT NULL DEFAULT 'zh',
    -- published 正常可见 / pending 待审（仅作者与管理员可见）/ hidden 已隐藏 / rejected 驳回 / deleted 已删除
    status        varchar(20)  NOT NULL DEFAULT 'published',
    pinned        boolean      NOT NULL DEFAULT false,
    featured      boolean      NOT NULL DEFAULT false,
    locked        boolean      NOT NULL DEFAULT false,
    linked_type   varchar(20),
    linked_id     uuid,
    reply_count   integer      NOT NULL DEFAULT 0,
    like_count    integer      NOT NULL DEFAULT 0,
    favorite_count integer     NOT NULL DEFAULT 0,
    view_count    bigint       NOT NULL DEFAULT 0,
    floor_count   integer      NOT NULL DEFAULT 0,
    last_reply_at timestamp with time zone,
    last_reply_user_id uuid,
    -- 敏感词命中情况（审核辅助）：none / watch / block
    risk_level    varchar(16)  NOT NULL DEFAULT 'none',
    hit_words     varchar(200),
    edited_at     timestamp with time zone,
    edit_count    integer      NOT NULL DEFAULT 0,
    created_at    timestamp with time zone NOT NULL DEFAULT now(),
    updated_at    timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_topic_status_check
        CHECK (status IN ('published', 'pending', 'hidden', 'rejected', 'deleted'))
);
CREATE INDEX IF NOT EXISTS idx_topic_board ON discussion_topic (board_id, status, pinned DESC, last_reply_at DESC NULLS LAST, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_topic_created ON discussion_topic (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_topic_author ON discussion_topic (author_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_topic_linked ON discussion_topic (linked_type, linked_id);
CREATE INDEX IF NOT EXISTS idx_topic_risk ON discussion_topic (risk_level) WHERE risk_level <> 'none';

-- ---------------------------------------------------------------------------
-- 楼层回复
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_post (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id     uuid        NOT NULL,
    parent_id    uuid,
    author_id    uuid        NOT NULL,
    content      text        NOT NULL,
    images       jsonb       NOT NULL DEFAULT '[]'::jsonb,
    lang         varchar(8)  NOT NULL DEFAULT 'zh',
    status       varchar(20) NOT NULL DEFAULT 'published',
    floor_no     integer     NOT NULL DEFAULT 1,
    quote_post_id uuid,
    like_count   integer     NOT NULL DEFAULT 0,
    reply_count  integer     NOT NULL DEFAULT 0,
    risk_level   varchar(16) NOT NULL DEFAULT 'none',
    hit_words    varchar(200),
    created_at   timestamp with time zone NOT NULL DEFAULT now(),
    updated_at   timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_post_status_check
        CHECK (status IN ('published', 'pending', 'hidden', 'deleted'))
);
CREATE INDEX IF NOT EXISTS idx_post_topic ON discussion_post (topic_id, status, floor_no);
CREATE INDEX IF NOT EXISTS idx_post_author ON discussion_post (author_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_post_risk ON discussion_post (risk_level) WHERE risk_level <> 'none';

-- ---------------------------------------------------------------------------
-- 举报
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_report (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type  varchar(20) NOT NULL,
    target_id    uuid        NOT NULL,
    reporter_id  uuid        NOT NULL,
    reason       varchar(32) NOT NULL,
    detail       varchar(500),
    status       varchar(16) NOT NULL DEFAULT 'pending',
    handled_by   uuid,
    handled_at   timestamp with time zone,
    result_note  varchar(200),
    created_at   timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_report_status_check CHECK (status IN ('pending', 'accepted', 'rejected')),
    CONSTRAINT discussion_report_unique UNIQUE (target_type, target_id, reporter_id)
);
CREATE INDEX IF NOT EXISTS idx_report_status ON discussion_report (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_report_target ON discussion_report (target_type, target_id);

-- ---------------------------------------------------------------------------
-- 本地敏感词库（纯本地匹配，不依赖第三方）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sensitive_word (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    word       varchar(64) NOT NULL UNIQUE,
    locale     varchar(8)  NOT NULL DEFAULT 'zh',
    -- block：直接进待审队列；watch：仅打标提示；replace：替换为 *
    level      varchar(16) NOT NULL DEFAULT 'watch',
    enabled    boolean     NOT NULL DEFAULT true,
    remark     varchar(200),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT sensitive_word_level_check CHECK (level IN ('block', 'watch', 'replace'))
);

-- ---------------------------------------------------------------------------
-- 站内通知（邮件通知复用同一行，emailed 标记是否已发信）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL,
    type        varchar(32) NOT NULL,
    actor_id    uuid,
    target_type varchar(20),
    target_id   uuid,
    title       varchar(200),
    content     varchar(500),
    payload     jsonb       NOT NULL DEFAULT '{}'::jsonb,
    read_at     timestamp with time zone,
    emailed     boolean     NOT NULL DEFAULT false,
    created_at  timestamp with time zone NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notification_user ON notification (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_unread ON notification (user_id) WHERE read_at IS NULL;

-- ---------------------------------------------------------------------------
-- 审核操作日志（合规审计）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discussion_moderation_log (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    target_type varchar(20) NOT NULL,
    target_id   uuid        NOT NULL,
    action      varchar(32) NOT NULL,
    operator_id uuid,
    reason      varchar(300),
    snapshot    text,
    created_at  timestamp with time zone NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_moderation_target ON discussion_moderation_log (target_type, target_id, created_at DESC);

-- ---------------------------------------------------------------------------
-- user_account 扩展：社区资料与处置状态
-- ---------------------------------------------------------------------------
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS bio varchar(200);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS locale varchar(8);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS timezone varchar(64);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS website varchar(200);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS last_active_at timestamp with time zone;
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS muted_until timestamp with time zone;
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS banned_until timestamp with time zone;
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS trust_level integer NOT NULL DEFAULT 0;
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS allow_stranger_message boolean NOT NULL DEFAULT false;

-- ---------------------------------------------------------------------------
-- updated_at 自动维护（复用库中已存在的 set_updated_at() 函数）
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['discussion_board', 'discussion_topic', 'discussion_post']
    LOOP
        IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_%s_updated ON %I', t, t);
            EXECUTE format('CREATE TRIGGER trg_%s_updated BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION set_updated_at()', t, t);
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 种子数据：4 个初始板块（固定 UUID，保证幂等）
-- ---------------------------------------------------------------------------
INSERT INTO discussion_board (id, slug, name, name_en, description, icon, theme_color, order_num)
VALUES
    ('71000000-0000-0000-0000-000000000001', 'culture', '民族文化漫谈', 'Culture Talk',   '聊聊各民族的历史、语言、服饰与风俗，分享你了解的文化细节。', 'chat',  '#B6402E', 1),
    ('71000000-0000-0000-0000-000000000002', 'travel',  '旅行见闻',     'Travel Notes',   '民族地区的旅行攻略、路线与见闻，欢迎配图分享。',               'map',   '#2E6B4F', 2),
    ('71000000-0000-0000-0000-000000000003', 'food',    '美食分享',     'Food Sharing',   '各民族特色美食的做法、味道与故事。',                           'food',  '#B07D2B', 3),
    ('71000000-0000-0000-0000-000000000004', 'meta',    '站务与建议',   'Feedback',       '站点使用问题、内容勘误与改进建议。',                           'info',  '#3C5A8A', 4)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 种子数据：初始敏感词（纯本地匹配；block 直接进待审，watch 仅打标）
-- 仅收录典型垃圾信息特征词，可由后台继续维护
-- ---------------------------------------------------------------------------
INSERT INTO sensitive_word (id, word, locale, level, remark)
VALUES
    ('72000000-0000-0000-0000-000000000001', '加微信',   'zh', 'block', '引流'),
    ('72000000-0000-0000-0000-000000000002', '加我微信', 'zh', 'block', '引流'),
    ('72000000-0000-0000-0000-000000000003', '代购',     'zh', 'watch', '广告'),
    ('72000000-0000-0000-0000-000000000004', '刷单',     'zh', 'block', '黑产'),
    ('72000000-0000-0000-0000-000000000005', '博彩',     'zh', 'block', '赌博'),
    ('72000000-0000-0000-0000-000000000006', '赌博',     'zh', 'block', '赌博'),
    ('72000000-0000-0000-0000-000000000007', '办证',     'zh', 'block', '黑产'),
    ('72000000-0000-0000-0000-000000000008', '贷款',     'zh', 'watch', '广告'),
    ('72000000-0000-0000-0000-000000000009', '色情',     'zh', 'block', '违禁'),
    ('72000000-0000-0000-0000-000000000010', '私聊优惠', 'zh', 'block', '引流'),
    ('72000000-0000-0000-0000-000000000011', '免费领取', 'zh', 'watch', '广告'),
    ('72000000-0000-0000-0000-000000000012', 'www.',     'en', 'watch', '外链'),
    ('72000000-0000-0000-0000-000000000013', 'http://',  'en', 'watch', '外链'),
    ('72000000-0000-0000-0000-000000000014', 'https://', 'en', 'watch', '外链')
ON CONFLICT (word) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 校验
-- ---------------------------------------------------------------------------
-- SELECT slug, name, post_policy FROM discussion_board ORDER BY order_num;
-- SELECT level, count(*) FROM sensitive_word GROUP BY level;
