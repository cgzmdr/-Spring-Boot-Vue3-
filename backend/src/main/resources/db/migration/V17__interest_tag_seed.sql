-- ============================================================================
-- V17 · 兴趣标签种子数据（方向 D）
--
-- 设计原则：标签**全部由库中真实数据推导**，不凭空造分类。
--   · ethnic 维度：56 个民族名（取自 ethnic_group.name）
--   · region 维度：民族主要聚居地中的省级行政区（取自 ethnic_group.region）
--   · type   维度：8 类内容类型（与 search_document.doc_type 一致）
--   · topic  维度：非遗类别 / 节日类型 / 艺术类别 / 体育类别（取自各表实际取值）
--
-- 幂等：ON CONFLICT (dimension, name) DO NOTHING。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V17__interest_tag_seed.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) 内容类型维度：8 类，固定
-- ---------------------------------------------------------------------------
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
VALUES
    ('type', '民族', 'Ethnic Groups', '56 个民族的族源、语言、聚居与概况', '#B6402E', 1),
    ('type', '节日', 'Festivals', '各民族传统节日与庆典', '#C9843E', 2),
    ('type', '艺术', 'Arts', '音乐、舞蹈、技艺等非物质文化遗产', '#2E6DA4', 3),
    ('type', '美食', 'Food', '各民族特色饮食', '#7A8B3A', 4),
    ('type', '风俗', 'Customs', '服饰、民居、婚丧、节庆等民俗', '#8A5A9E', 5),
    ('type', '人物', 'People', '非遗代表性传承人与历史文化名家', '#4A7C6F', 6),
    ('type', '自治地方', 'Autonomous Areas', '自治区、自治州、自治县', '#A0522D', 7),
    ('type', '传统体育', 'Traditional Sports', '民族传统体育项目', '#B03A5B', 8)
ON CONFLICT (dimension, name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2) 民族维度：直接取库中 56 个民族（真实数据，不硬编码）
-- ---------------------------------------------------------------------------
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
SELECT 'ethnic', e.name, e.name_en,
       COALESCE(e.summary, e.name), e.theme_color, COALESCE(e.order_num, 0)
FROM ethnic_group e
WHERE e.status = 'published'
ON CONFLICT (dimension, name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3) 地域维度：从民族聚居地中抽取省级行政区
--    region 列本身即 jsonb 数组（如 ["内蒙古自治区","辽宁省",...]），
--    无需再转 text 比较（直接 e.region <> '' 会报 JSON 语法错误）。
-- ---------------------------------------------------------------------------
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
SELECT 'region', r.region_name, NULL,
       '主要分布地区：' || r.region_name, NULL, r.cnt
FROM (
    SELECT region_name, count(*) AS cnt
    FROM (
        SELECT DISTINCT e.name AS ethnic_name,
               jsonb_array_elements_text(e.region) AS region_name
        FROM ethnic_group e
        WHERE e.status = 'published'
          AND e.region IS NOT NULL
          AND jsonb_typeof(e.region) = 'array'
    ) x
    GROUP BY region_name
) r
WHERE r.region_name IS NOT NULL
  AND length(btrim(r.region_name)) BETWEEN 2 AND 30
  -- 排除「全国各省区市」这类聚合表述（汉族的 region 值）：它不是可选题材的地域，
  -- 若作为兴趣标签会让「按地域推荐」失去区分度。
  AND r.region_name NOT LIKE '%全国%'
  AND r.region_name NOT LIKE '%各省%'
ON CONFLICT (dimension, name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4) 主题维度：从各表实际出现的分类值抽取（非遗类别 / 节日类型 / 艺术类别 / 体育类别）
-- ---------------------------------------------------------------------------
-- 4.1 艺术类别（如 服饰/建筑/音乐/舞蹈/技艺…）
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
SELECT DISTINCT 'topic', a.category, NULL, '艺术类别：' || a.category, NULL, 10
FROM art a
WHERE a.category IS NOT NULL AND btrim(a.category) <> ''
ON CONFLICT (dimension, name) DO NOTHING;

-- 4.2 节日类型（traditional/religious/agricultural → 中文）
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
SELECT DISTINCT 'topic',
       CASE f.type WHEN 'traditional' THEN '传统节日'
                   WHEN 'religious'   THEN '宗教节日'
                   WHEN 'agricultural' THEN '农事节日'
                   ELSE f.type END,
       NULL,
       '节日类型',
       NULL, 20
FROM festival f
WHERE f.type IS NOT NULL AND btrim(f.type) <> ''
ON CONFLICT (dimension, name) DO NOTHING;

-- 4.3 体育项目类别
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
SELECT DISTINCT 'topic', s.category, NULL, '传统体育项目类别', NULL, 30
FROM traditional_sport s
WHERE s.category IS NOT NULL AND btrim(s.category) <> ''
ON CONFLICT (dimension, name) DO NOTHING;

-- 4.4 非遗级别（世界级 / 国家级等）
INSERT INTO interest_tag (dimension, name, name_en, description, color, order_num)
SELECT DISTINCT 'topic', a.intangible_heritage, NULL, '非物质文化遗产级别', NULL, 40
FROM art a
WHERE a.intangible_heritage IS NOT NULL AND btrim(a.intangible_heritage) <> ''
ON CONFLICT (dimension, name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 校验：
-- SELECT dimension, count(*) FROM interest_tag GROUP BY 1 ORDER BY 1;
-- ---------------------------------------------------------------------------
