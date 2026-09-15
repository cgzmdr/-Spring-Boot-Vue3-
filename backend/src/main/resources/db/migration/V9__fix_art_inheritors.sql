-- ============================================================================
-- V9 · 修正 art.inheritors 中的非人名记录
--
-- 背景（数据质量）：原库中「藏戏」的传承人记录为 "觉木隆"，但
-- 「觉木隆」实为**藏戏演出团体名**（拉萨觉木隆藏戏队），并非传承人姓名。
-- 把团体名当作个人传承人展示会造成事实性错误，故予以修正。
--
-- 修正依据：文化和旅游部《国家级非物质文化遗产代表性项目代表性传承人》
-- 第六批名单 ——「藏戏（拉萨觉木隆）」的官方代表性传承人为：
--   边点旺久、洛桑扎西
--
-- 幂等：仅当当前值仍为 ["觉木隆"] 时才替换，避免覆盖后续人工修订。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V9__fix_art_inheritors.sql
-- ============================================================================

DO $$
DECLARE
    updated integer := 0;
BEGIN
    UPDATE art
       SET inheritors = '["边点旺久","洛桑扎西"]'::jsonb
     WHERE name = '藏戏'
       AND inheritors::text = '["觉木隆"]';
    IF FOUND THEN
        updated := updated + 1;
    END IF;

    RAISE NOTICE 'V9 修正完成：本次更新 % 行', updated;
END $$;

-- 校验：
-- SELECT name, inheritors FROM art WHERE name = '藏戏';
-- 期望："["边点旺久", "洛桑扎西"]"
