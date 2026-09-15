-- ============================================================================
-- V11 · 民族自治地方（B-6）
--
-- 背景：民族区域自治是「多元一体」的制度落点，但库中此前**没有任何行政区划实体**，
-- 只有 ethnic_group.region（民族主要聚居的省级行政区，如 ["云南省","贵州省"]）
-- ——那是「民族散居在哪些省」的粗略线索，并非自治地方本身。
-- 本表补齐「5 个自治区 + 30 个自治州 + 120 个自治县/旗 = 155 个」自治地方实体。
--
-- 设计取舍：
--   · ethnic_groups 存自治民族的**名称数组**（jsonb），与 ethnic_group.name 同名可关联，
--     但不做外键 —— 自治地方是行政区划事实，不应因内容库调整而级联受损。
--   · established_year / seat 允许为空：宁缺勿造（与 person_profile 同一原则）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING，可重复执行。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V11__autonomous_area.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS autonomous_area (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 官方全称，如「延边朝鲜族自治州」「双江拉祜族佤族布朗族傣族自治县」
    name             varchar(128) NOT NULL UNIQUE,
    -- 级别：autonomous_region 自治区 / autonomous_prefecture 自治州 / autonomous_county 自治县·自治旗
    level            varchar(32)  NOT NULL,
    -- 冠名的自治民族（名称数组，如 ["蒙古族","藏族"]）
    ethnic_groups    jsonb        NOT NULL DEFAULT '[]'::jsonb,
    -- 所属省级行政区（自治区本身填其自身）
    province         varchar(64),
    -- 成立年份（不可考时为空，不推测）
    established_year integer,
    -- 行政中心 / 首府（不可考时为空）
    seat             varchar(64),
    created_at       timestamp with time zone NOT NULL DEFAULT now(),
    updated_at       timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_area_level    ON autonomous_area (level);
CREATE INDEX IF NOT EXISTS idx_area_province ON autonomous_area (province);

-- 按民族检索（jsonb 数组包含判断）
CREATE INDEX IF NOT EXISTS idx_area_ethnic   ON autonomous_area USING gin (ethnic_groups);

-- updated_at 自动维护
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_autonomous_area_updated ON autonomous_area;
        CREATE TRIGGER trg_autonomous_area_updated BEFORE UPDATE ON autonomous_area
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- 校验：
-- SELECT level, count(*) FROM autonomous_area GROUP BY 1;   -- 期望 5 / 30 / 120
-- SELECT count(*) FROM autonomous_area;                      -- 期望 155

-- ---------------------------------------------------------------------------
-- 种子数据：155 个民族自治地方（5 自治区 + 30 自治州 + 120 自治县/旗）
-- 数据来源：中国行政区划公开事实整理；成立年份与驻地不可考者留空，不推测。
-- 幂等：ON CONFLICT (name) DO NOTHING
-- ---------------------------------------------------------------------------
-- 由 tools 脚本生成：民族自治地方种子数据（B-6）
-- 幂等：ON CONFLICT (name) DO NOTHING，可重复执行

INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('内蒙古自治区', 'autonomous_region', '["蒙古族"]'::jsonb, '内蒙古自治区', 1947, '呼和浩特市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('广西壮族自治区', 'autonomous_region', '["壮族"]'::jsonb, '广西壮族自治区', 1958, '南宁市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('西藏自治区', 'autonomous_region', '["藏族"]'::jsonb, '西藏自治区', 1965, '拉萨市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('宁夏回族自治区', 'autonomous_region', '["回族"]'::jsonb, '宁夏回族自治区', 1958, '银川市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('新疆维吾尔自治区', 'autonomous_region', '["维吾尔族"]'::jsonb, '新疆维吾尔自治区', 1955, '乌鲁木齐市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('延边朝鲜族自治州', 'autonomous_prefecture', '["朝鲜族"]'::jsonb, '吉林省', 1952, '延吉市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('恩施土家族苗族自治州', 'autonomous_prefecture', '["土家族","苗族"]'::jsonb, '湖北省', 1983, '恩施市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('湘西土家族苗族自治州', 'autonomous_prefecture', '["土家族","苗族"]'::jsonb, '湖南省', 1957, '吉首市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('阿坝藏族羌族自治州', 'autonomous_prefecture', '["藏族","羌族"]'::jsonb, '四川省', 1953, '马尔康市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('甘孜藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '四川省', 1950, '康定市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('凉山彝族自治州', 'autonomous_prefecture', '["彝族"]'::jsonb, '四川省', 1952, '西昌市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('黔东南苗族侗族自治州', 'autonomous_prefecture', '["苗族","侗族"]'::jsonb, '贵州省', 1956, '凯里市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('黔南布依族苗族自治州', 'autonomous_prefecture', '["布依族","苗族"]'::jsonb, '贵州省', 1956, '都匀市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('黔西南布依族苗族自治州', 'autonomous_prefecture', '["布依族","苗族"]'::jsonb, '贵州省', 1982, '兴义市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('楚雄彝族自治州', 'autonomous_prefecture', '["彝族"]'::jsonb, '云南省', 1958, '楚雄市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('红河哈尼族彝族自治州', 'autonomous_prefecture', '["哈尼族","彝族"]'::jsonb, '云南省', 1957, '蒙自市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('文山壮族苗族自治州', 'autonomous_prefecture', '["壮族","苗族"]'::jsonb, '云南省', 1958, '文山市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('西双版纳傣族自治州', 'autonomous_prefecture', '["傣族"]'::jsonb, '云南省', 1953, '景洪市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('大理白族自治州', 'autonomous_prefecture', '["白族"]'::jsonb, '云南省', 1956, '大理市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('德宏傣族景颇族自治州', 'autonomous_prefecture', '["傣族","景颇族"]'::jsonb, '云南省', 1953, '芒市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('怒江傈僳族自治州', 'autonomous_prefecture', '["傈僳族"]'::jsonb, '云南省', 1954, '泸水市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('迪庆藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '云南省', 1957, '香格里拉市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('临夏回族自治州', 'autonomous_prefecture', '["回族"]'::jsonb, '甘肃省', 1956, '临夏市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('甘南藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '甘肃省', 1953, '合作市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('海北藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '青海省', 1953, '海晏县') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('黄南藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '青海省', 1953, '同仁市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('海南藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '青海省', 1953, '共和县') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('果洛藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '青海省', 1954, '玛沁县') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('玉树藏族自治州', 'autonomous_prefecture', '["藏族"]'::jsonb, '青海省', 1951, '玉树市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('海西蒙古族藏族自治州', 'autonomous_prefecture', '["蒙古族","藏族"]'::jsonb, '青海省', 1954, '德令哈市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('昌吉回族自治州', 'autonomous_prefecture', '["回族"]'::jsonb, '新疆维吾尔自治区', 1954, '昌吉市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('博尔塔拉蒙古自治州', 'autonomous_prefecture', '["蒙古族"]'::jsonb, '新疆维吾尔自治区', 1954, '博乐市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('巴音郭楞蒙古自治州', 'autonomous_prefecture', '["蒙古族"]'::jsonb, '新疆维吾尔自治区', 1954, '库尔勒市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('克孜勒苏柯尔克孜自治州', 'autonomous_prefecture', '["柯尔克孜族"]'::jsonb, '新疆维吾尔自治区', 1954, '阿图什市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('伊犁哈萨克自治州', 'autonomous_prefecture', '["哈萨克族"]'::jsonb, '新疆维吾尔自治区', 1954, '伊宁市') ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('大厂回族自治县', 'autonomous_county', '["回族"]'::jsonb, '河北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('孟村回族自治县', 'autonomous_county', '["回族"]'::jsonb, '河北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('青龙满族自治县', 'autonomous_county', '["满族"]'::jsonb, '河北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('丰宁满族自治县', 'autonomous_county', '["满族"]'::jsonb, '河北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('围场满族蒙古族自治县', 'autonomous_county', '["满族","蒙古族"]'::jsonb, '河北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('宽城满族自治县', 'autonomous_county', '["满族"]'::jsonb, '河北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('鄂伦春自治旗', 'autonomous_county', '["鄂伦春族"]'::jsonb, '内蒙古自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('莫力达瓦达斡尔族自治旗', 'autonomous_county', '["达斡尔族"]'::jsonb, '内蒙古自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('鄂温克族自治旗', 'autonomous_county', '["鄂温克族"]'::jsonb, '内蒙古自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('喀喇沁左翼蒙古族自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('阜新蒙古族自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('新宾满族自治县', 'autonomous_county', '["满族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('岫岩满族自治县', 'autonomous_county', '["满族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('清原满族自治县', 'autonomous_county', '["满族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('本溪满族自治县', 'autonomous_county', '["满族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('桓仁满族自治县', 'autonomous_county', '["满族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('宽甸满族自治县', 'autonomous_county', '["满族"]'::jsonb, '辽宁省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('长白朝鲜族自治县', 'autonomous_county', '["朝鲜族"]'::jsonb, '吉林省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('前郭尔罗斯蒙古族自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '吉林省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('伊通满族自治县', 'autonomous_county', '["满族"]'::jsonb, '吉林省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('杜尔伯特蒙古族自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '黑龙江省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('景宁畲族自治县', 'autonomous_county', '["畲族"]'::jsonb, '浙江省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('长阳土家族自治县', 'autonomous_county', '["土家族"]'::jsonb, '湖北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('五峰土家族自治县', 'autonomous_county', '["土家族"]'::jsonb, '湖北省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('通道侗族自治县', 'autonomous_county', '["侗族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('新晃侗族自治县', 'autonomous_county', '["侗族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('芷江侗族自治县', 'autonomous_county', '["侗族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('靖州苗族侗族自治县', 'autonomous_county', '["苗族","侗族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('麻阳苗族自治县', 'autonomous_county', '["苗族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('江华瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('城步苗族自治县', 'autonomous_county', '["苗族"]'::jsonb, '湖南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('连南瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广东省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('连山壮族瑶族自治县', 'autonomous_county', '["壮族","瑶族"]'::jsonb, '广东省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('乳源瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广东省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('龙胜各族自治县', 'autonomous_county', '["侗族","瑶族","苗族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('恭城瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('金秀瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('融水苗族自治县', 'autonomous_county', '["苗族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('三江侗族自治县', 'autonomous_county', '["侗族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('巴马瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('都安瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('大化瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('环江毛南族自治县', 'autonomous_county', '["毛南族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('罗城仫佬族自治县', 'autonomous_county', '["仫佬族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('富川瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('隆林各族自治县', 'autonomous_county', '["苗族","彝族","仡佬族"]'::jsonb, '广西壮族自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('乐东黎族自治县', 'autonomous_county', '["黎族"]'::jsonb, '海南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('陵水黎族自治县', 'autonomous_county', '["黎族"]'::jsonb, '海南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('昌江黎族自治县', 'autonomous_county', '["黎族"]'::jsonb, '海南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('白沙黎族自治县', 'autonomous_county', '["黎族"]'::jsonb, '海南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('琼中黎族苗族自治县', 'autonomous_county', '["黎族","苗族"]'::jsonb, '海南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('保亭黎族苗族自治县', 'autonomous_county', '["黎族","苗族"]'::jsonb, '海南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('秀山土家族苗族自治县', 'autonomous_county', '["土家族","苗族"]'::jsonb, '重庆市', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('酉阳土家族苗族自治县', 'autonomous_county', '["土家族","苗族"]'::jsonb, '重庆市', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('彭水苗族土家族自治县', 'autonomous_county', '["苗族","土家族"]'::jsonb, '重庆市', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('石柱土家族自治县', 'autonomous_county', '["土家族"]'::jsonb, '重庆市', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('北川羌族自治县', 'autonomous_county', '["羌族"]'::jsonb, '四川省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('木里藏族自治县', 'autonomous_county', '["藏族"]'::jsonb, '四川省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('马边彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '四川省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('峨边彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '四川省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('道真仡佬族苗族自治县', 'autonomous_county', '["仡佬族","苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('务川仡佬族苗族自治县', 'autonomous_county', '["仡佬族","苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('威宁彝族回族苗族自治县', 'autonomous_county', '["彝族","回族","苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('关岭布依族苗族自治县', 'autonomous_county', '["布依族","苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('镇宁布依族苗族自治县', 'autonomous_county', '["布依族","苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('紫云苗族布依族自治县', 'autonomous_county', '["苗族","布依族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('玉屏侗族自治县', 'autonomous_county', '["侗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('印江土家族苗族自治县', 'autonomous_county', '["土家族","苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('沿河土家族自治县', 'autonomous_county', '["土家族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('松桃苗族自治县', 'autonomous_county', '["苗族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('三都水族自治县', 'autonomous_county', '["水族"]'::jsonb, '贵州省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('峨山彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('新平彝族傣族自治县', 'autonomous_county', '["彝族","傣族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('元江哈尼族彝族傣族自治县', 'autonomous_county', '["哈尼族","彝族","傣族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('屏边苗族自治县', 'autonomous_county', '["苗族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('河口瑶族自治县', 'autonomous_county', '["瑶族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('金平苗族瑶族傣族自治县', 'autonomous_county', '["苗族","瑶族","傣族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('普洱哈尼族彝族自治县', 'autonomous_county', '["哈尼族","彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('墨江哈尼族自治县', 'autonomous_county', '["哈尼族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('景东彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('景谷傣族彝族自治县', 'autonomous_county', '["傣族","彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('镇沅彝族哈尼族拉祜族自治县', 'autonomous_county', '["彝族","哈尼族","拉祜族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('江城哈尼族彝族自治县', 'autonomous_county', '["哈尼族","彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('孟连傣族拉祜族佤族自治县', 'autonomous_county', '["傣族","拉祜族","佤族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('澜沧拉祜族自治县', 'autonomous_county', '["拉祜族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('西盟佤族自治县', 'autonomous_county', '["佤族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('沧源佤族自治县', 'autonomous_county', '["佤族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('耿马傣族佤族自治县', 'autonomous_county', '["傣族","佤族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('双江拉祜族佤族布朗族傣族自治县', 'autonomous_county', '["拉祜族","佤族","布朗族","傣族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('宁洱哈尼族彝族自治县', 'autonomous_county', '["哈尼族","彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('漾濞彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('南涧彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('巍山彝族回族自治县', 'autonomous_county', '["彝族","回族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('永平彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('玉龙纳西族自治县', 'autonomous_county', '["纳西族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('宁蒗彝族自治县', 'autonomous_county', '["彝族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('德钦藏族自治县', 'autonomous_county', '["藏族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('贡山独龙族怒族自治县', 'autonomous_county', '["独龙族","怒族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('兰坪白族普米族自治县', 'autonomous_county', '["白族","普米族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('维西傈僳族自治县', 'autonomous_county', '["傈僳族"]'::jsonb, '云南省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('张家川回族自治县', 'autonomous_county', '["回族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('天祝藏族自治县', 'autonomous_county', '["藏族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('肃南裕固族自治县', 'autonomous_county', '["裕固族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('肃北蒙古族自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('阿克塞哈萨克族自治县', 'autonomous_county', '["哈萨克族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('东乡族自治县', 'autonomous_county', '["东乡族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('积石山保安族东乡族撒拉族自治县', 'autonomous_county', '["保安族","东乡族","撒拉族"]'::jsonb, '甘肃省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('门源回族自治县', 'autonomous_county', '["回族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('互助土族自治县', 'autonomous_county', '["土族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('化隆回族自治县', 'autonomous_county', '["回族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('循化撒拉族自治县', 'autonomous_county', '["撒拉族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('民和回族土族自治县', 'autonomous_county', '["回族","土族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('大通回族土族自治县', 'autonomous_county', '["回族","土族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('河南蒙古族自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '青海省', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('焉耆回族自治县', 'autonomous_county', '["回族"]'::jsonb, '新疆维吾尔自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('察布查尔锡伯自治县', 'autonomous_county', '["锡伯族"]'::jsonb, '新疆维吾尔自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('木垒哈萨克自治县', 'autonomous_county', '["哈萨克族"]'::jsonb, '新疆维吾尔自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('和布克赛尔蒙古自治县', 'autonomous_county', '["蒙古族"]'::jsonb, '新疆维吾尔自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('塔什库尔干塔吉克自治县', 'autonomous_county', '["塔吉克族"]'::jsonb, '新疆维吾尔自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO autonomous_area (name, level, ethnic_groups, province, established_year, seat) VALUES ('巴里坤哈萨克自治县', 'autonomous_county', '["哈萨克族"]'::jsonb, '新疆维吾尔自治区', NULL, NULL) ON CONFLICT (name) DO NOTHING;
