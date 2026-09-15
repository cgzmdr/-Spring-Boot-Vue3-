-- ============================================================================
-- V6 · 自建机器翻译译文缓存（不依赖任何商业翻译 API）
--   译文按「原文指纹」失效：正文被编辑后 source_hash 变化，旧译文自然不再命中。
-- 幂等脚本，可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V6__translation.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS discussion_translation (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- topic 帖子 / post 楼层 / message 私信 / board 板块
    target_type    varchar(20)  NOT NULL,
    target_id      uuid         NOT NULL,
    -- 目标语言（读者要看的语言，如 en / zh）
    target_locale  varchar(8)   NOT NULL,
    -- 原文语言（落库时的 lang 字段，仅作展示，不参与唯一键）
    source_locale  varchar(8),
    -- 原文指纹（sha256，正文改动即失效）
    source_hash    varchar(64)  NOT NULL,
    -- 原文摘要（后台排查用，不含全文）
    source_excerpt varchar(120),
    -- 译文（已渲染为可展示文本，含换行）
    content        text         NOT NULL,
    -- 产出来源：libretranslate / ollama / glossary
    provider       varchar(32)  NOT NULL,
    created_at     timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT discussion_translation_unique
        UNIQUE (target_type, target_id, target_locale, source_hash)
);
CREATE INDEX IF NOT EXISTS idx_translation_target
    ON discussion_translation (target_type, target_id, target_locale, created_at DESC);

-- 站内公告（后台 OA 群发）审计：复用 discussion_moderation_log，target_type='broadcast'
-- 无需新增表，此处仅补索引以加速按 target_type 过滤。
CREATE INDEX IF NOT EXISTS idx_moderation_type ON discussion_moderation_log (target_type, created_at DESC);

-- 校验：
-- SELECT target_type, target_locale, provider, count(*) FROM discussion_translation GROUP BY 1,2,3;
