-- ============================================================================
-- V12 · 传统体育（B-5）
--
-- 背景：库中此前**没有体育类内容** —— ethnic_custom 的 10 个分类里没有「体育」，
-- art 表 165 个非遗项目里也没有体育项目。仅在 content 里零星出现 5 条
-- 与竞技有关的线索（如哈萨克族「姑娘追」、仡佬族「打花龙」）。
-- 本表补齐以**全国少数民族传统体育运动会竞赛项目**为主体的传统体育内容。
--
-- 数据来源（权威）：
--   1. 国家民委、国家体育总局《全国少数民族传统体育运动会总规程》（竞赛项目清单）
--   2. 湖北省民族宗教事务委员会《少数民族传统体育项目》项目简介
-- 详见 docs/refs/ 下的来源文本。
--
-- 设计取舍：
--   · sports 项目数量少（约 20 项），字段按「项目」这一层次设计；
--     民族式摔跤、民族马术等多子项项目用 sub_events 承载子项名称。
--   · ethnic_origins 存民族名数组，可与 ethnic_group.name 关联用于跳转，
--     但不做外键 —— 体育项目的起源民族可能不在内容库的 56 个民族内。
--   · 不确定的字段一律留空，不编造（与 person_profile / autonomous_area 同一原则）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING。
-- 应用方式：psql -h localhost -U postgres -d 56_app -f V12__traditional_sport.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS traditional_sport (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 项目名（全国少数民族传统体育运动会竞赛项目名称）
    name           varchar(128) NOT NULL UNIQUE,
    -- 类别：ball 球类 / water 水上 / strength 力量对抗 / accuracy 射击技巧
    --       speed 竞速 / martial 武术 / equestrian 马术 / gymnastics 健身操 / swing 秋千
    category       varchar(32)  NOT NULL,
    -- 起源与主要流行的民族（名称数组）
    ethnic_origins jsonb        NOT NULL DEFAULT '[]'::jsonb,
    -- 客观描述（是什么、怎么玩、来源）
    description    text,
    -- 主要器材
    equipment      varchar(256),
    -- 场地规格
    venue          varchar(256),
    -- 参赛人数说明
    team_size      varchar(128),
    -- 首次成为全国少数民族传统体育运动会竞赛项目的年份
    first_event_year integer,
    -- 多子项项目的子项名称（如民族式摔跤的搏克/且里西/格/北嘎/绊跤/希日木）
    sub_events     jsonb        NOT NULL DEFAULT '[]'::jsonb,
    -- 相关的国家级非物质文化遗产项目名（若有）
    heritage_link  varchar(128),
    created_at     timestamp with time zone NOT NULL DEFAULT now(),
    updated_at     timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sport_category ON traditional_sport (category);
CREATE INDEX IF NOT EXISTS idx_sport_ethnic   ON traditional_sport USING gin (ethnic_origins);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_traditional_sport_updated ON traditional_sport;
        CREATE TRIGGER trg_traditional_sport_updated BEFORE UPDATE ON traditional_sport
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- 校验：
-- SELECT category, count(*) FROM traditional_sport GROUP BY 1 ORDER BY 2 DESC;
-- SELECT count(*) FROM traditional_sport;

-- ---------------------------------------------------------------------------
-- 种子数据：全国少数民族传统体育运动会竞赛项目
-- 来源：国家民委、国家体育总局《全国少数民族传统体育运动会总规程》竞赛项目清单；
--       湖北省民族宗教事务委员会《少数民族传统体育项目》项目简介。
-- 不确定的字段留空（NULL），不编造。幂等：ON CONFLICT (name) DO NOTHING。
-- ---------------------------------------------------------------------------

INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('花炮', 'ball', '["侗族","壮族"]'::jsonb, '又称「抢花炮」，源于侗族、壮族等民族的传统民俗活动，原为庙会与节庆中争夺花炮的仪式。比赛中两队争夺投掷升空后落下的花炮，持炮方以将花炮送入对方炮台得分。因对抗激烈、观赏性强，被称为「东方橄榄球」。', '花炮（橡胶制）、炮台', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('珍珠球', 'ball', '["满族"]'::jsonb, '原名「采珍珠」，来源于长白山以北、黑龙江中上游、乌苏里江流域等地的采珍珠生产活动：当地群众将采来的蛤蚌直接入筐，或在船上把蛤蚌扔到岸上的筐里，抛得准、接得准能极大提高采珠速度。基本技术与比赛方法类似篮球，也被称为「移动篮筐的小篮球」。每队上场 7 人，其中 1 名持抄网队员在得分区抄接本方队员投来的球，封锁区内 2 名防守队员用球拍封挡拦截。1991 年第四届全国少数民族传统体育运动会首次将其列为竞赛项目。', '珍珠球、球拍、抄网', '长 28 米、宽 15 米', '每队上场 7 人', 1991, '[]'::jsonb, '满族珍珠球') ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('木球', 'ball', '["回族"]'::jsonb, '由回族传统体育活动「打木球」发展而来，使用木质球和击球板，以将木球击入对方球门得分。', '木球、击球板', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('蹴球', 'ball', '["满族"]'::jsonb, '源于古代蹴鞠的踢石球游戏，比赛时以脚底蹴球，使本方球撞击对方球出界或使本方球靠近中心得分。比赛分两个阶段：第一阶段分组循环赛，第二阶段由各组优胜名次共 8 人（对）采用淘汰赛决出名次。', '蹴球', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('毽球', 'ball', '["侗族","苗族","水族"]'::jsonb, '由侗族、苗族、水族等民族中广泛流传的踢毽子活动发展而来，结合排球、足球与羽毛球规则形成网前对抗的竞技项目。', '毽球', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('龙舟', 'water', '["汉族","苗族","侗族","傣族","白族"]'::jsonb, '在多民族中广泛流传的水上竞渡活动，以多人划桨驱动龙形舟竞速。比赛分标准龙舟与小龙舟，采用预赛与决赛排定名次。', '龙舟、舵桨（划桨自备）', NULL, NULL, NULL, '["标准龙舟","小龙舟"]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('独竹漂', 'water', '["苗族","土家族","侗族"]'::jsonb, '又称「独竹舟」，发源于赤水河流域，参与者脚踏一根楠竹漂行于水面，手持细竹竿划水，比速度也比技巧，是贵州赤水一带流传的水上绝技。', '竹漂、划竿', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('秋千', 'swing', '["朝鲜族","阿昌族","哈尼族"]'::jsonb, '在朝鲜族、阿昌族、哈尼族等民族中广泛流传的荡秋千活动，比赛以荡起高度判定成绩。全国少数民族传统体育运动会的秋千项目设有规定高度与自选动作。', '秋千架、秋千绳', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('射弩', 'accuracy', '["傈僳族","怒族","独龙族","苗族","瑶族"]'::jsonb, '由弩弓狩猎演化而来的射击比赛，在傈僳族、怒族、独龙族等民族中历史悠久。比赛分立姿、跪姿，分团体赛与个人赛，各分两个阶段进行。', '弩、箭（自备）', NULL, '团体赛每队 4 人', NULL, '["立姿","跪姿"]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('陀螺', 'accuracy', '["佤族","壮族","瑶族","黎族"]'::jsonb, '又称「打陀螺」，在佤族、壮族、瑶族等民族中广为流传。比赛时一方旋放陀螺，另一方掷出陀螺将其击倒或使其转出界外，分团体赛与个人赛。', '陀螺、鞭杆、鞭绳、胶垫', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('押加', 'strength', '["藏族"]'::jsonb, '又称「大象拔河」，是藏族传统力量项目。两名选手背向跪撑，用颈部与肩部套住一条长绸带，向相反方向用力将对方拉过中线者获胜。', '押加带', NULL, '每场 2 人', NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('高脚竞速', 'speed', '["土家族","苗族"]'::jsonb, '俗称「高脚马」或「竹马」，在土家族、苗族等民族中流传。选手脚踏绑缚在腿上的竹制高脚，手持平衡杆在跑道上竞速。', '高脚（竹马）、平衡杆', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('板鞋竞速', 'speed', '["壮族"]'::jsonb, '源于壮族传统活动「板鞋舞」，多名队员同穿一副长板鞋，需步调一致方可前进，以团队协作与速度取胜。', '板鞋', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('民族武术', 'martial', '["回族","苗族","傣族","彝族"]'::jsonb, '各民族传统武术的总称，包含拳术、器械等不同门类，如回族的查拳、苗族的苗拳、傣族的孔雀拳等，在全国少数民族传统体育运动会上设有多项套路比赛。', NULL, NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('民族式摔跤', 'strength', '["蒙古族","维吾尔族","彝族","藏族","回族","朝鲜族"]'::jsonb, '各民族传统摔跤形式的总称。全国少数民族传统体育运动会的民族式摔跤设 6 个子项，分别对应不同民族的传统摔跤：搏克（蒙古族）、且里西（维吾尔族）、格（彝族）、北嘎（藏族）、绊跤（回族）、希日木（朝鲜族）。', NULL, NULL, '每场 2 人', NULL, '["搏克","且里西","格","北嘎","绊跤","希日木"]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('民族马术', 'equestrian', '["蒙古族","哈萨克族","藏族","维吾尔族"]'::jsonb, '各民族传统马术技艺的竞赛形式。全国少数民族传统体育运动会的马术项目设 5 个子项：民族赛马、走马、跑马射击、跑马射箭、跑马拾哈达。', NULL, NULL, NULL, NULL, '["民族赛马","走马","跑马射击","跑马射箭","跑马拾哈达"]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('民族健身操', 'gymnastics', '[]'::jsonb, '将各民族传统舞蹈与体育动作元素编创而成的健身操，兼具健身性与观赏性，是全国少数民族传统体育运动会较新的竞赛项目。', NULL, NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('攀椰竞速', 'speed', '["黎族"]'::jsonb, '源于黎族群众攀爬椰子树采摘椰子的生产活动，选手徒手攀爬模拟椰子树竞速，以用时最短者获胜。', NULL, NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('姑娘追', 'equestrian', '["哈萨克族"]'::jsonb, '哈萨克族传统马背娱乐活动。青年男女骑马并行时小伙子可向姑娘表达爱慕，返程时姑娘扬鞭追赶，既是马术竞技也是情感表达，多在喜庆节日举行。', NULL, NULL, '每组 2 人', NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
INSERT INTO traditional_sport (name, category, ethnic_origins, description, equipment, venue, team_size, first_event_year, sub_events, heritage_link) VALUES ('打花龙', 'ball', '["仡佬族"]'::jsonb, '仡佬族传统体育活动。青年男女以藤编的「花龙」球互相抛接，边打边唱，兼具竞技与娱乐性质，多在节日中举行。', '藤编花龙球', NULL, NULL, NULL, '[]'::jsonb, NULL) ON CONFLICT (name) DO NOTHING;
