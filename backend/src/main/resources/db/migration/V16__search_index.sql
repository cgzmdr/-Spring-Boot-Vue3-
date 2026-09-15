-- ============================================================================
-- V16 · 全文检索索引 + 兴趣标签 + 推荐行为（方向 D）
--
-- 背景（实测）：
--   · 站内可检索内容共 8 类约 1142 条（民族 56 / 节日 192 / 艺术 165 / 美食 168 /
--     风俗习惯 222 / 人物 164 / 自治地方 155 / 传统体育 20），
--     而改造前的搜索只覆盖 民族/节日/艺术 3 类（413 条），其余 5 类完全搜不到。
--   · 本机**没有 Docker/Podman，也没有 Elasticsearch/Meilisearch 在运行**，
--     但 PostgreSQL 已安装 pg_trgm 1.6。因此采用 PostgreSQL 原生全文检索方案。
--   · 实测 pg_trgm 对中文偏弱：similarity('维吾尔族','维吾尔')=0.5，
--     而 '蒙古' 这类两字输入对其余民族相似度均为 0（默认阈值 0.3 直接漏召回）。
--     故本方案**不单纯依赖 trigram**，而是 子串匹配 + trigram 相似度 + 拼音 三路并用，
--     并按命中方式与字段权重排序（见 SearchServiceImpl）。
--
-- 本迁移建立三张表：
--   1. search_document  统一检索索引（8 类内容聚合成一张宽表）
--   2. interest_tag     兴趣标签字典（后台可维护）
--   3. user_interest    用户兴趣标签（显式信号，解决推荐冷启动）
-- 并扩展推荐所需的隐式行为记录。
--
-- 幂等：CREATE TABLE IF NOT EXISTS / ADD COLUMN IF NOT EXISTS / ON CONFLICT DO NOTHING。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V16__search_index.sql
-- ============================================================================

-- pg_trgm 提供相似度与模糊匹配（本机已安装；此处确保启用）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- 1. 统一检索索引
--    设计取舍：把 8 类内容聚合成一张宽表，而不是每次检索 union 8 张业务表。
--    好处：一次查询完成跨类型检索与统一排序；索引可批量重建。
--    代价：内容更新后需要重建对应文档（由 SearchIndexService 负责，支持全量/单类型重建）。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS search_document (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 内容类型：ethnic/festival/art/food/custom/person/area/sport
    doc_type       varchar(32)  NOT NULL,
    -- 业务表主键（与 doc_type 唯一确定一条内容）
    doc_id         uuid         NOT NULL,
    -- 详情页路由（前端直接跳转）
    url            varchar(300) NOT NULL,
    -- 标题（展示与高亮的主体）
    title          varchar(300) NOT NULL,
    -- 副标题/摘要（展示用，不参与高权重匹配）
    summary        text,
    -- 正文（参与匹配，不直接展示）
    body           text,
    -- 所属民族名（用于分面筛选与「按民族找」）
    ethnic_name    varchar(64),
    -- 封面图与主题色（结果卡片直接复用现有组件）
    cover_image    varchar(300),
    theme_color    varchar(32),
    -- 归属地区（自治地方用；亦可承载民族的主要聚居地）
    region         varchar(120),
    -- 分类（节日类型 / 艺术类别 / 人物领域 / 体育项目类别等，用于分面）
    category       varchar(64),
    -- 检索辅助：全拼与首字母缩写（如 蒙古族 → mengguzu / mgz）
    pinyin_full    varchar(300),
    pinyin_abbr    varchar(64),
    -- 英文名（支持英文检索）
    title_en       varchar(300),
    body_en        text,
    -- 热度（浏览量+点赞+收藏的加权，重建索引时刷新；用于默认排序与推荐）
    popularity     integer NOT NULL DEFAULT 0,
    -- 内容更新时间（用于「最新」排序，以及推荐中时效衰减）
    content_at     timestamp with time zone,
    updated_at     timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_search_document UNIQUE (doc_type, doc_id)
);

CREATE INDEX IF NOT EXISTS idx_sd_type       ON search_document (doc_type);
CREATE INDEX IF NOT EXISTS idx_sd_ethnic     ON search_document (ethnic_name);
CREATE INDEX IF NOT EXISTS idx_sd_popularity ON search_document (popularity DESC);
CREATE INDEX IF NOT EXISTS idx_sd_region     ON search_document (region);

-- trigram 索引：加速模糊匹配（标题/拼音/正文）
CREATE INDEX IF NOT EXISTS idx_sd_title_trgm  ON search_document USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_sd_pyfull_trgm ON search_document USING gin (pinyin_full gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_sd_pyabbr_trgm ON search_document USING gin (pinyin_abbr gin_trgm_ops);

-- 中文子串检索：trigram 对两字输入召回很差，故另建 lower(title) 的 btree 前缀索引辅助 LIKE 'x%'
CREATE INDEX IF NOT EXISTS idx_sd_title_lower ON search_document (lower(title) text_pattern_ops);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_search_document_updated ON search_document;
        CREATE TRIGGER trg_search_document_updated BEFORE UPDATE ON search_document
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. 兴趣标签字典（后台可维护）
--    多维度：民族 / 地域 / 内容类型 / 主题
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS interest_tag (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 标签维度：ethnic 民族 / region 地域 / type 内容类型 / topic 主题
    dimension   varchar(32) NOT NULL,
    -- 标签名（如「藏族」「西南」「非遗技艺」）
    name        varchar(64) NOT NULL,
    -- 英文名（英文站展示）
    name_en     varchar(128),
    -- 说明（前台展示给用户，帮助理解标签含义）
    description varchar(300),
    -- 标签色（前台 chip 配色，沿用民族主题色体系）
    color       varchar(32),
    enabled     boolean NOT NULL DEFAULT true,
    order_num   integer NOT NULL DEFAULT 0,
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    updated_at  timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_interest_tag UNIQUE (dimension, name)
);

CREATE INDEX IF NOT EXISTS idx_it_dimension ON interest_tag (dimension);
CREATE INDEX IF NOT EXISTS idx_it_enabled   ON interest_tag (enabled);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_interest_tag_updated ON interest_tag;
        CREATE TRIGGER trg_interest_tag_updated BEFORE UPDATE ON interest_tag
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 3. 用户兴趣标签（显式信号）
--    这是推荐系统的**冷启动解法**：新用户没有浏览历史时，
--    直接用显式选择的标签召回内容；老用户则由隐式行为持续修正画像。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_interest (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL,
    tag_id     uuid NOT NULL REFERENCES interest_tag (id) ON DELETE CASCADE,
    -- 权重：用户可把某个标签标为「特别关注」，用于加权
    weight     integer NOT NULL DEFAULT 1,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_interest UNIQUE (user_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_ui_user ON user_interest (user_id);

-- ---------------------------------------------------------------------------
-- 4. 用户行为明细（推荐引擎的隐式信号）
--    已有的 view_counter / like_record / favorite 是**聚合计数**，
--    无法回答「谁在什么时候看了什么」，因此推荐无法基于个人历史。
--    本表记录带用户与时间的明细；未登录用户不入库（无 user_id 不构成个人画像）。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_behavior (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL,
    -- 行为类型：view 浏览 / like 点赞 / favorite 收藏 / search 搜索
    action      varchar(32) NOT NULL,
    -- 内容类型与 ID（与 search_document.doc_type/doc_id 对应）
    target_type varchar(32),
    target_id   uuid,
    -- 搜索行为记录关键词（用于从搜索词反推兴趣）
    keyword     varchar(200),
    -- 行为权重（浏览 1 / 点赞 3 / 收藏 5 / 搜索 2，用于画像加权）
    weight      integer NOT NULL DEFAULT 1,
    created_at  timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ub_user_time ON user_behavior (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ub_target    ON user_behavior (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_ub_action    ON user_behavior (action);

-- ---------------------------------------------------------------------------
-- 5. 物化用户兴趣画像（由行为聚合而来，定时/按需刷新）
--    这样推荐查询不必每次扫行为明细表。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_profile (
    user_id        uuid PRIMARY KEY,
    -- 兴趣民族（jsonb 数组，按权重降序，如 [{"name":"藏族","score":12}]）
    top_ethnics    jsonb NOT NULL DEFAULT '[]'::jsonb,
    -- 兴趣分类
    top_categories jsonb NOT NULL DEFAULT '[]'::jsonb,
    -- 行为总量（前台据此如实说明「样本不足」）
    behavior_count integer NOT NULL DEFAULT 0,
    -- 画像可信度：0~1，行为越少越低；低于阈值时推荐以内容相似度/热度为主
    confidence     double precision NOT NULL DEFAULT 0,
    refreshed_at   timestamp with time zone NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- 校验：
-- SELECT doc_type, count(*) FROM search_document GROUP BY 1 ORDER BY 1;
-- SELECT dimension, count(*) FROM interest_tag GROUP BY 1 ORDER BY 1;
-- ---------------------------------------------------------------------------
