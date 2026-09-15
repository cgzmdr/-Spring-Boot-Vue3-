-- ============================================================================
-- V15 · 英文正文补齐（方向 C-3）
--
-- 背景与**重要现状**：
--   · 三类内容表（festival / art / food）此前只有 name_en（英文名），
--     **没有 description_en（英文正文）字段** —— 本迁移负责补上这三个字段。
--   · 实测现状（迁移前）：festival 192 / art 165 / food 168 = 525 行，
--     中文 description 合计 11,868 字符，英文正文覆盖率为 0。
--   · name_en 581 行（含 ethnic_group）**已全部填写**，无需处理。
--   · ethnic_group 已有 description_en 且已填（12,131 字符），本迁移不动它。
--
-- 设计决策：**只加字段与来源标记，不在迁移里写入任何译文**。
--   译文由 scripts/backfill-description-en 调用站点既有的翻译通道
--   （app.translate.provider，默认 spring-ai → DeepSeek）批量生成后回填，
--   与用户在后台逐条手工维护共用同一套字段，互不冲突。
--
-- description_en_source 记录该译文的来源，便于区分「机器翻译」与「人工校对」：
--   machine  机器翻译（未人工校对）
--   reviewed 人工校对过
--   manual   后台手工撰写/录入
--   NULL     尚无英文正文（页面回退显示中文）
--
-- 幂等：ADD COLUMN IF NOT EXISTS + 可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V15__description_en.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. 新增英文正文字段
-- ---------------------------------------------------------------------------
ALTER TABLE festival ADD COLUMN IF NOT EXISTS description_en        text;
ALTER TABLE festival ADD COLUMN IF NOT EXISTS description_en_source varchar(32);

ALTER TABLE art ADD COLUMN IF NOT EXISTS description_en        text;
ALTER TABLE art ADD COLUMN IF NOT EXISTS description_en_source varchar(32);

ALTER TABLE food ADD COLUMN IF NOT EXISTS description_en        text;
ALTER TABLE food ADD COLUMN IF NOT EXISTS description_en_source varchar(32);

-- ---------------------------------------------------------------------------
-- 2. 约束：来源标记只允许约定的取值（NULL 表示未翻译）
--    PostgreSQL 不支持 ADD CONSTRAINT IF NOT EXISTS，故用 DO 块做存在性判断。
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['festival', 'art', 'food']
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_' || t || '_description_en_source'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I CHECK (description_en_source IS NULL OR description_en_source IN (''machine'', ''reviewed'', ''manual''))',
                t, 'ck_' || t || '_description_en_source');
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 3. 统计视图：英文正文覆盖率（供后台「内容运营」概览与文档核对）
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_english_coverage AS
SELECT 'festival' AS content_type,
       count(*) AS total,
       count(*) FILTER (WHERE description_en IS NOT NULL AND btrim(description_en) <> '') AS translated,
       count(*) FILTER (WHERE description_en_source = 'machine')  AS machine,
       count(*) FILTER (WHERE description_en_source = 'reviewed') AS reviewed,
       count(*) FILTER (WHERE description_en_source = 'manual')   AS manual
FROM festival
UNION ALL
SELECT 'art', count(*),
       count(*) FILTER (WHERE description_en IS NOT NULL AND btrim(description_en) <> ''),
       count(*) FILTER (WHERE description_en_source = 'machine'),
       count(*) FILTER (WHERE description_en_source = 'reviewed'),
       count(*) FILTER (WHERE description_en_source = 'manual')
FROM art
UNION ALL
SELECT 'food', count(*),
       count(*) FILTER (WHERE description_en IS NOT NULL AND btrim(description_en) <> ''),
       count(*) FILTER (WHERE description_en_source = 'machine'),
       count(*) FILTER (WHERE description_en_source = 'reviewed'),
       count(*) FILTER (WHERE description_en_source = 'manual')
FROM food;

-- ---------------------------------------------------------------------------
-- 校验：
-- SELECT * FROM v_english_coverage ORDER BY content_type;
--  迁移刚应用完成时 translated 应全为 0（译文由脚本回填）。
-- ---------------------------------------------------------------------------
