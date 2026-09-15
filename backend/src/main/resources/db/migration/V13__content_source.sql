-- ============================================================================
-- V13 · 内容来源（方向 C-1：逐条来源引用 / 可溯源）
--
-- 背景：站内数据均有权威出处（国家民委、国家统计局七普、文化和旅游部非遗名录、
-- Wikimedia Commons 等），但这些出处只存在于 docs/*.md 文档里 ——
-- **页面级没有任何来源标注**，读者无法判断某条内容从何而来。
--
-- 设计要点：
--   1. 来源本身单独成表（content_source），可被多条内容复用，
--      也便于统一维护机构名与链接（同一份名单被多个民族/项目引用）。
--   2. 内容与来源是多对多：一个民族同时有「民委概况文本」「七普人口」「Commons 图片」
--      三个来源，一份名单（如文旅部传承人名录）也被 100+ 个非遗项目共用。
--   3. content_source_link 记录「哪条内容用了哪个来源」，并带上该来源在该条内容上
--      的具体说明（如「人口数据取自表 2-1」），比只挂一个外键更有信息量。
--
-- 为什么不用「在每张内容表加 source 字段」：
--   来源是跨表复用的实体（一份名单服务上百条内容），且需要机构/链接/采集方式等
--   多个属性；散落到各表会造成同一来源重复维护、链接失效后要改 N 处。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V13__content_source.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS content_source (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 来源简称（唯一），如「国家民委《中华各民族》」
    name          varchar(200) NOT NULL UNIQUE,
    -- 发布机构，如「中华人民共和国国家民族事务委员会」
    publisher     varchar(200),
    -- 发布机构简称，页面展示用
    publisher_short varchar(100),
    -- 文档 / 栏目名称，如「中国人口普查年鉴-2020 表 2-1」
    document_title varchar(300),
    -- 原始链接
    url           varchar(500),
    -- 来源层级：official 官方权威 / academic 学术 / open 开放图库 / other
    source_type   varchar(32) NOT NULL DEFAULT 'official',
    -- 采集方式：scrape 程序抓取 / ocr OCR 识别 / manual 人工整理 / api 接口获取
    collect_method varchar(64),
    -- 补充说明（口径、校验方法等）
    remark        text,
    -- 排序（页面展示顺序，权威来源优先）
    order_num     integer NOT NULL DEFAULT 0,
    created_at    timestamp with time zone NOT NULL DEFAULT now(),
    updated_at    timestamp with time zone NOT NULL DEFAULT now()
);

-- 内容与来源的关联（多对多）
CREATE TABLE IF NOT EXISTS content_source_link (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 内容类型：ethnic / festival / art / food / topic / person / area / sport
    target_type varchar(32)  NOT NULL,
    -- 内容 ID
    target_id   uuid         NOT NULL,
    -- 来源 ID
    source_id   uuid         NOT NULL REFERENCES content_source (id) ON DELETE CASCADE,
    -- 该来源在该条内容上的具体说明（可空），如「人口取自表 2-1」
    note        varchar(300),
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT content_source_link_unique UNIQUE (target_type, target_id, source_id)
);

CREATE INDEX IF NOT EXISTS idx_source_link_target ON content_source_link (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_source_link_source ON content_source_link (source_id);
CREATE INDEX IF NOT EXISTS idx_source_order       ON content_source (order_num, name);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_content_source_updated ON content_source;
        CREATE TRIGGER trg_content_source_updated BEFORE UPDATE ON content_source
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 种子数据：项目实际使用的 6 个权威来源
-- 均来自项目前期的数据采集工作（见 docs/56民族数据与图片资源说明.md）
-- ---------------------------------------------------------------------------
INSERT INTO content_source
    (name, publisher, publisher_short, document_title, url, source_type, collect_method, remark, order_num)
VALUES
    ('国家民委《中华各民族》栏目',
     '中华人民共和国国家民族事务委员会', '国家民委',
     '《中华各民族》专题栏目（概况 / 历史沿革 / 风俗文化 / 发展现状，共 224 页）',
     'https://www.neac.gov.cn/seac/ztzl/zgmzjs/index.shtml',
     'official', 'scrape',
     '民族名单、概况文本、历史沿革、风俗文化的**主来源**。概况文本源自《民族问题五种丛书》之《中国少数民族》卷（国家民委组织编纂的权威民族志）。',
     10),

    ('《中国人口普查年鉴-2020》表 2-1',
     '国家统计局', '国家统计局',
     '《中国人口普查年鉴-2020》表 2-1《全国各民族人口及比重》',
     'https://www.stats.gov.cn/sj/pcsj/rkpc/7rp/zk/indexch.htm',
     'official', 'ocr',
     '2020 年第七次全国人口普查口径（2020-11-01 零时）。原表为 PDF/JPG，经 OCR 识别后与《第七次全国人口普查公报（第二号）》交叉校验。',
     20),

    ('《第七次全国人口普查公报（第二号）》',
     '国家统计局', '国家统计局',
     '第七次全国人口普查公报（第二号）—— 全国人口情况',
     'https://www.gov.cn/guoqing/2021-05/13/content_5606149.htm',
     'official', 'manual',
     '用于人口数据的**校验**：汉族 1,286,311,334 人、少数民族合计 125,467,390 人，与本库汇总一致。',
     30),

    ('文化和旅游部国家级非遗代表性传承人名录',
     '中华人民共和国文化和旅游部', '文化和旅游部',
     '《国家级非物质文化遗产代表性项目代表性传承人》第 1–6 批名单',
     NULL,
     'official', 'manual',
     '非遗传承人（人物档案）的来源，共 4001 条记录（第一批 226 / 第二批 547 / 第三批 706 / 第四批 498 / 第五批 1082 / 第六批 942）。仅收录官方名单中确有的人名，官方未设个人传承人的项目保持为空。',
     40),

    ('国家民委、国家体育总局《全国少数民族传统体育运动会总规程》',
     '国家民族事务委员会、国家体育总局', '国家民委 / 国家体育总局',
     '全国少数民族传统体育运动会总规程',
     NULL,
     'official', 'manual',
     '传统体育项目的竞赛项目清单来源。湖北省民族宗教事务委员会《少数民族传统体育项目》补充了项目简介与场地、人数等细节。',
     50),

    ('Wikimedia Commons',
     'Wikimedia Foundation', 'Wikimedia',
     '自由许可图片库（民族服饰 / 人物 / 文化场景照片）',
     'https://commons.wikimedia.org/',
     'open', 'api',
     '民族等封面图片来源。Commons 仅收录自由许可或公有领域作品（CC BY / CC BY-SA / CC0 / PD）；CC BY 与 CC BY-SA **要求署名**，正式上线需按各图许可补充作者与许可信息。',
     60)
ON CONFLICT (name) DO NOTHING;

-- 校验：
-- SELECT name, publisher_short, source_type, collect_method FROM content_source ORDER BY order_num;

-- ---------------------------------------------------------------------------
-- 内容与来源的关联（按内容类型分派实际来源，不做无差别全挂）
-- ---------------------------------------------------------------------------
-- 关联口径：按「该类型内容的实际生产来源」分派，不做无差别全挂
-- 1) 民族：民委文本（主）+ 七普人口 + Commons 图片
INSERT INTO content_source_link (target_type, target_id, source_id, note)
SELECT 'ethnic', e.id, s.id,
       CASE s.order_num
           WHEN 10 THEN '概况 / 历史沿革 / 风俗文化文本'
           WHEN 20 THEN '2020 年人口数据'
           WHEN 60 THEN '封面图片（自由许可）'
       END
FROM ethnic_group e
CROSS JOIN content_source s
WHERE s.order_num IN (10, 20, 60)
ON CONFLICT DO NOTHING;

-- 2) 节日 / 艺术 / 美食：均基于民委风俗文化栏目与权威资料整理
INSERT INTO content_source_link (target_type, target_id, source_id, note)
SELECT t.tbl, t.id, s.id, '基于国家民委「风俗文化」栏目与权威资料整理'
FROM (
    SELECT 'festival' AS tbl, id FROM festival
    UNION ALL SELECT 'art', id FROM art
    UNION ALL SELECT 'food', id FROM food
) t
CROSS JOIN content_source s
WHERE s.order_num = 10
ON CONFLICT DO NOTHING;

-- 3) 非遗项目（art）：另挂文旅部传承人名录（该项目传承人数据来源）
INSERT INTO content_source_link (target_type, target_id, source_id, note)
SELECT 'art', a.id, s.id, '该项目代表性传承人'
FROM art a
CROSS JOIN content_source s
WHERE s.order_num = 40
  AND a.inheritors IS NOT NULL AND a.inheritors::text NOT IN ('[]','null')
ON CONFLICT DO NOTHING;

-- 4) 人物档案：文旅部名录
INSERT INTO content_source_link (target_type, target_id, source_id, note)
SELECT 'person', p.id, s.id, '传承人姓名'
FROM person_profile p
CROSS JOIN content_source s
WHERE s.order_num = 40 AND p.role_type = 'inheritor'
ON CONFLICT DO NOTHING;

-- 5) 传统体育：国家民委 + 国家体育总局规程
INSERT INTO content_source_link (target_type, target_id, source_id, note)
SELECT 'sport', sp.id, s.id, '竞赛项目清单与规则'
FROM traditional_sport sp
CROSS JOIN content_source s
WHERE s.order_num = 50
ON CONFLICT DO NOTHING;

-- 校验：
-- SELECT target_type, count(*) AS links, count(DISTINCT target_id) AS contents
-- FROM content_source_link GROUP BY 1 ORDER BY 1;