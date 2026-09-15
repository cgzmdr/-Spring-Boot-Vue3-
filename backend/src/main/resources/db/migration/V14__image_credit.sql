-- ============================================================================
-- V14 · 图片版权署名（方向 C-4）
--
-- 背景与**重要现状**（必须在文档与页面上如实体现）：
--   · 站内 574 张图片已本地化，但**原始版权元数据已丢失** ——
--     文件在采集后被统一重命名为 cover.webp / {slug}.webp，
--     采集清单（原 scripts/scrape/tmp/commons2/manifest.json）所在的 scripts/ 目录
--     已不在仓库中，media_asset 表为空，数据库中也没有保存原始 Commons 文件名。
--     因此**无法把现有图片对应回它的来源页**。
--   · 各类图片的出处也不齐：民族图 109 张原取自 Wikimedia Commons（自由许可）；
--     food 168 张文档记为「百度图搜来源，毕设展示用」；festival/art 290 张来源未记录。
--
-- 设计决策：**不编造署名**。本表把每张图的署名状态显式记录为
-- verified（已核实，有作者与许可）/ unverified（来源待核）/ original（原创或 CC0 无需署名），
-- 页面对 unverified 的图片显示「来源待核」而不是空白或假名。
-- 这样「哪些已核实、哪些还欠着」一目了然，也便于逐张补齐。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V14__image_credit.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS image_credit (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 图片路径（与数据库中 cover_image / image 等字段一致，如 /images/ethnic/miao/cover.webp）
    image_path    varchar(300) NOT NULL UNIQUE,
    -- 所属内容类型与 ID（便于在详情页按内容汇总）
    target_type   varchar(32),
    target_id     uuid,
    -- 图片说明（用于汇总区展示，如「苗族 · 封面」）
    caption       varchar(200),
    -- 署名核实状态：verified 已核实 / unverified 来源待核 / original 原创或无需署名
    credit_status varchar(32) NOT NULL DEFAULT 'unverified',
    -- 作者 / 摄影者（未核实时为空）
    author        varchar(200),
    -- 许可名称：CC BY-SA 4.0 / CC BY 2.0 / CC0 / Public Domain 等
    license       varchar(100),
    -- 许可条款链接
    license_url   varchar(300),
    -- 来源页面链接（如 Commons 文件页）
    source_url    varchar(500),
    -- 来源站点名（Wikimedia Commons / 原创 等）
    source_site   varchar(100),
    -- 是否需要署名（CC BY / CC BY-SA 为 true；CC0 / PD 为 false）
    attribution_required boolean NOT NULL DEFAULT true,
    -- 备注（如「元数据待用反向图片搜索补齐」）
    remark        text,
    created_at    timestamp with time zone NOT NULL DEFAULT now(),
    updated_at    timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_credit_target ON image_credit (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_credit_status ON image_credit (credit_status);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_image_credit_updated ON image_credit;
        CREATE TRIGGER trg_image_credit_updated BEFORE UPDATE ON image_credit
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 说明：本迁移**不插入任何编造的署名数据**。
-- 图片署名记录由 scripts/backfill-image-credits 目录下的脚本
-- 依据实际图片文件扫描生成（全部标为 unverified），再由人工/反向检索逐张补齐为 verified。
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- target_id 回填：把图片与其所属内容关联（供详情页按内容汇总图片来源）
-- 注：一张图片可能被多条内容共用（如多个节日复用同一封面），
--     此处按「文件名去重」的原则只归属到首个匹配的内容。
-- 说明：实际回填语句由 scripts/backfill-image-credits 生成并执行，
--       见 backend/src/main/resources/db/seed-image-credits.sql。
-- ---------------------------------------------------------------------------

-- 校验：
-- SELECT target_type, count(*) AS total, count(target_id) AS with_target
-- FROM image_credit GROUP BY 1 ORDER BY 1;