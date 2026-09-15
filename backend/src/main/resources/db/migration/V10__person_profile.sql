-- ============================================================================
-- V10 · 人物专栏基础表（B-7 民族人物 / 传承人专栏）
--
-- 背景：art.inheritors 中已存有 163 位人物（补充自文化和旅游部第 1–6 批国家级
-- 非遗代表性传承人名单），但只有**姓名**，缺少人物专栏所需的结构化信息：
--   · 角色类型 —— 需区分「代表性传承人」与「历史文化名家」；
--   · 领域归类 —— 需把 165 个非遗项目归入音乐/舞蹈/戏剧/技艺/服饰/建筑等；
--   · 人物简介 —— 用于专栏列表与详情展示。
--
-- 设计取舍：**不复制、不改写 art.inheritors**，本表只承载「人物维度的扩展属性」，
-- 以 (person_name, ethnic_group_name) 为业务键与 art 表关联。
-- 这样 art 表仍是传承人姓名的唯一数据源，避免两处维护导致不一致。
--
-- 为什么业务键含民族：同名不同族确实存在（如「马金山」同时是回族花儿与
-- 东乡族花儿的代表性传承人），仅按姓名会错误合并两个人物。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING，可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V10__person_profile.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS person_profile (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 人物姓名（与 art.inheritors 中的写法完全一致）
    person_name       varchar(128) NOT NULL,
    -- 所属民族名（与 ethnic_group.name 对应；同名不同族靠此区分）
    ethnic_group_name varchar(64),
    -- 角色类型：inheritor 代表性传承人 / master 历史文化名家
    role_type         varchar(32)  NOT NULL DEFAULT 'inheritor',
    -- 领域（音乐/舞蹈/戏剧/技艺/服饰/建筑/民俗等），由所属非遗项目的 category 映射
    domain            varchar(64),
    -- 人物简介（可为空；空时前端只展示其项目与民族，不编造生平）
    bio               text,
    -- 生卒年等补充信息（如「1894—1961」），仅在有可靠依据时填写
    lifespan          varchar(64),
    created_at        timestamp with time zone NOT NULL DEFAULT now(),
    updated_at        timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT person_profile_unique UNIQUE (person_name, ethnic_group_name)
);

CREATE INDEX IF NOT EXISTS idx_person_role   ON person_profile (role_type);
CREATE INDEX IF NOT EXISTS idx_person_domain ON person_profile (domain);
CREATE INDEX IF NOT EXISTS idx_person_ethnic ON person_profile (ethnic_group_name);

-- updated_at 自动维护（复用库中已有的 set_updated_at 函数）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_person_profile_updated ON person_profile;
        CREATE TRIGGER trg_person_profile_updated BEFORE UPDATE ON person_profile
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 种子：从 art.inheritors 展开全部人物，写入基准档案
--   角色默认 inheritor；领域由所属项目 category 映射。
-- ---------------------------------------------------------------------------
INSERT INTO person_profile (person_name, ethnic_group_name, role_type, domain)
SELECT DISTINCT
    p.name AS person_name,
    e.name AS ethnic_group_name,
    'inheritor' AS role_type,
    CASE a.category
        WHEN 'music'        THEN '音乐'
        WHEN 'dance'        THEN '舞蹈'
        WHEN 'drama'        THEN '戏剧'
        WHEN 'costume'      THEN '服饰'
        WHEN 'craft'        THEN '技艺'
        WHEN 'architecture' THEN '建筑'
        WHEN 'fine_art'     THEN '美术'
        ELSE '其他'
    END AS domain
FROM art a
LEFT JOIN ethnic_group e ON e.id = a.ethnic_group_id,
LATERAL jsonb_array_elements_text(a.inheritors::jsonb) AS p(name)
WHERE a.inheritors IS NOT NULL
  AND a.inheritors::text NOT IN ('[]', 'null')
  AND p.name IS NOT NULL
  AND p.name <> ''
ON CONFLICT (person_name, ethnic_group_name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 角色修正：以下 4 位是**历史文化名家 / 已故艺术大师**，并非现行认定并
-- 在世的「国家级非遗代表性传承人」。把他们与在世传承人混同展示会造成
-- 事实性错误，故单独归类为 master（历史文化名家）。
--   王羲之（303—361）东晋书法家   —— 中国书法的历史源头人物
--   颜真卿（709—785）唐代书法家   —— 楷书典范
--   梅兰芳（1894—1961）京剧大师   —— 京剧旦角艺术体系创立者
--   谭鑫培（1847—1917）京剧大师   —— 京剧老生宗师
-- ---------------------------------------------------------------------------
UPDATE person_profile SET role_type = 'master',
       lifespan = '303—361'  WHERE person_name = '王羲之';
UPDATE person_profile SET role_type = 'master',
       lifespan = '709—785'  WHERE person_name = '颜真卿';
UPDATE person_profile SET role_type = 'master',
       lifespan = '1894—1961' WHERE person_name = '梅兰芳';
UPDATE person_profile SET role_type = 'master',
       lifespan = '1847—1917' WHERE person_name = '谭鑫培';

UPDATE person_profile SET bio = '东晋书法家，《兰亭序》作者，被后世尊为「书圣」。'
    WHERE person_name = '王羲之';
UPDATE person_profile SET bio = '唐代书法家，楷书端庄雄浑，与柳公权并称「颜筋柳骨」。'
    WHERE person_name = '颜真卿';
UPDATE person_profile SET bio = '京剧表演艺术大师，创立「梅派」旦角艺术体系。'
    WHERE person_name = '梅兰芳';
UPDATE person_profile SET bio = '京剧老生宗师，谭派创始人，对京剧唱腔与表演影响深远。'
    WHERE person_name = '谭鑫培';

-- 校验：
-- SELECT role_type, count(*) FROM person_profile GROUP BY 1;
-- SELECT person_name, role_type, domain, lifespan FROM person_profile WHERE role_type='master';
-- SELECT count(DISTINCT person_name) FROM person_profile;
