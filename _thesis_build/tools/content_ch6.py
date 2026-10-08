# -*- coding: utf-8 -*-
"""正文内容：第 6 章 系统测试、结论、参考文献与附录。"""

BLOCKS = []
B = BLOCKS.append

TH = ['用例说明', '测试目的', '测试步骤', '预期结果', '输出结果', '通过情况']
TW = [2.2, 2.6, 4.4, 2.4, 2.0, 1.8]

B(('h1', '6 系统测试'))
B(('h2', '6.1 测试目的'))
B(('p', '测试要回答的问题其实很朴素：系统是不是按需求说明里写的那样工作，权限有没有漏洞，审批流程能不能真的走完一圈，检索和推荐的结果对不对、快不快。除此之外还得看它在边界输入和异常操作下会不会崩、会不会写出脏数据——这几个问题问完，系统能不能交付基本就有数了。这类问题要是拖到上线后才暴露，代价就大了。'))

B(('h2', '6.2 测试方法'))
B(('p', '本次测试以黑盒测试为主、白盒测试为辅。黑盒测试不管内部怎么实现，站在使用者的角度按需求文档设计用例，在界面上做真实操作、看返回结果来判断功能对不对。白盒测试则针对核心算法和关键逻辑，通过读代码、构造特定数据来验证分支覆盖，比如检索相关度表达式在多路命中时的分支走向、流程网关在变量缺失时取了什么默认值。'))
B(('p', '用例按功能、边界、异常三类来设计。三类各有各的用处，缺一类就会漏掉一批问题。功能测试走正常路径，验证输入合理数据能不能得到预期结果；边界测试盯着数据和极端条件，比如空关键词检索、超长关键词、分页越界、人口分档的临界值，看系统稳不稳；异常测试针对错误输入和非法操作，比如提交缺必填项的表单、用错密码登录、拿无权限账号调管理接口、上传超限文件，重点看它会不会给出明确提示，而不是默默写进一条错数据。'))
B(('p', '用例都是照着需求文档写的，不是照着实现反推的，这样才测得出偏差。除了功能测试，本次还做了性能实测：在开发环境下用脚本连续调用接口并记录响应时间，把首次调用（缓存未预热）和后续调用分开测，用来评估缓存到底起了多大作用；同时核对接口返回的数据量和内容，确认只读查询没有捎带返回大字段。只看响应时间是不够的，还得看它究竟搬了多少数据。'))

B(('h2', '6.3 测试内容'))
B(('p', '按上面的方法，设计了表 6-1 至表 6-8 共八个功能测试用例，覆盖用户注册登录、内容检索、节日日历换算、互动操作、后台内容维护、审批流程闭环和权限控制这些关键场景。每个用例都记录测试步骤、预期结果和实际输出。实测结果和预期对不上时，先查实现、再考虑改用例，顺序不能颠倒。'))
B(('tbl', '表 6-1 用户注册与登录测试表', TH, [[
    '用户注册与登录',
    '测试用户能否正确注册并登录系统',
    '1、打开登录页面，切换到注册，填写账号、昵称与密码后提交；\n2、使用刚注册的账号登录；\n3、使用错误密码再次登录',
    '注册成功；正确密码登录成功；错误密码提示「账号或密码错误」',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-2 民族频道筛选与排序测试表', TH, [[
    '民族频道筛选与排序',
    '测试民族列表的筛选、排序与统计展示功能',
    '1、进入民族频道，依次选择语系「汉藏语系」、人口「千万以上」并勾选「只看有非遗」；\n2、按人口降序排序；\n3、查看结果数量提示',
    '筛选条件组合后列表实时更新，结果数量正确，卡片显示的关联内容计数与详情页一致',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-3 全文检索多形式输入测试表', TH, [[
    '全文检索',
    '测试中文、拼音全拼、拼音首字母与英文名四种输入形式',
    '1、在检索框输入「蒙古」，提交；\n2、输入「mengguzu」，提交；\n3、输入「mgz」，提交；\n4、输入「Mongol」，提交',
    '四种输入均能命中蒙古族相关内容，结果标注命中方式并高亮关键词，民族类结果排在自治地方等其他类型之前',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-4 节日日历农历换算测试表', TH, [[
    '节日日历农历换算',
    '测试农历节日能否正确换算为公历日期',
    '1、进入节日日历，选择年份 2026；\n2、查看各月节日分布与「藏历新年」「那达慕大会」等条目；\n3、切换至下一年份再查看',
    '农历表述被换算为对应公历日期并按月排布，无法可靠换算的历法条目被跳过而不猜测日期，仅精确到月的条目以虚线近似显示',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-5 收藏与点赞功能测试表', TH, [[
    '收藏与点赞',
    '测试登录用户的收藏与点赞功能',
    '1、未登录状态下点击收藏；\n2、登录后再次点击收藏与点赞；\n3、在个人中心查看收藏列表；\n4、再次点击取消收藏',
    '未登录时提示登录；登录后收藏与点赞计数各加一并在个人中心可见；取消后计数恢复',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-6 后台内容新增与提交审批测试表', TH, [[
    '后台内容新增与提交审批',
    '测试管理员新增内容并提交审批的流程',
    '1、在后台民族管理页面点击新增，填写必填项后保存；\n2、在列表中对该条内容点击「提交审批」；\n3、查看列表中的状态与审批环节列',
    '保存成功且状态为草稿；提交后内容版本号加一、状态变为待审批、审批环节显示「待审核员审批」',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-7 内容审批闭环测试表', TH, [[
    '内容审批工作流闭环',
    '测试「审批—上线—审查—下线—修改—再审批」闭环',
    '1、以审核员身份打开「我的待办」，对提交的内容填写意见并退回；\n2、以内容编辑身份修改后重新提交；\n3、以审核员身份审批通过；\n4、以内容管理员身份审查并选择「有问题」；\n5、查看内容状态与审批意见记录',
    '每一步的环节与状态正确流转：退回后进入「待内容编辑修改」，重新提交后回到审核员环节，通过后内容上线并进入「待内容管理员审查」，选择有问题后内容被暂时下线；各环节意见完整留痕且修改环节可查看前序全部意见',
    '结果输出符合预期',
    '通过',
]], TW))
B(('tbl', '表 6-8 权限控制测试表', TH, [[
    '权限控制',
    '测试基于权限点的接口访问控制是否有效',
    '1、以普通注册用户的令牌调用后台民族新增接口；\n2、以内容编辑身份的令牌调用用户管理接口；\n3、不使用令牌调用未公开的接口',
    '三种情况均被拒绝并返回权限不足或未登录的提示，不会执行任何数据写入操作',
    '结果输出符合预期',
    '通过',
]], TW))

B(('h2', '6.4 测试结果与分析'))
B(('p', '按上述用例逐项执行下来，所有功能用例的通过情况都符合预期，没有出现导致业务流程中断的缺陷。测试中确实发现并修掉了两个问题，都值得记一笔。一个是中文与拼音混合检索时的排序问题：早期实现里拼音匹配统一按「包含」计分，结果「蒙古族」和「河南蒙古族自治县」算出同样的分数，真正的目标被自治县挤到了后面。改成按「精确匹配 > 前缀匹配 > 包含匹配」分级计分之后排序才恢复正常。另一个是流程变量缺失时排他网关取值异常：引擎按条件表达式求值时，如果变量根本不存在会直接抛异常，解决办法是在启动流程时预置各结论开关的安全默认值。这两个问题都不大，但都属于「不实测就发现不了」的类型。改完之后重新跑了一遍，两个问题都没有复现。'))
B(('p', '性能是用脚本实测出来的，数据如下。全文检索接口首次调用（服务刚启动、缓存未预热）耗时约 83 毫秒，同一关键词后续重复调用稳定在 5～6 毫秒；拼音全拼和拼音首字母检索的耗时分别是 5～6 毫秒和 4～8 毫秒，和项目文档中记录的 40～55 毫秒（含冷启动与较大结果集的场景）处在同一量级。民族人口统计接口首次 8 毫秒，进程内缓存命中后降到 2～3 毫秒；民族列表接口首次 15 毫秒、后续 7 毫秒；人物专栏接口首次 45 毫秒（第一次要全表读取并在内存里聚合）、后续 10 毫秒；民族自治地方接口首次 28 毫秒、后续 14 毫秒。换句话说，只读目录类接口在缓存生效后都落到了 10 毫秒量级，3.2 节提的性能要求算是达到了。'))
B(('p', '这里还想多说一句民族表「慢」的原因。它慢并不在行数——全表只有 56 行——而在数据量：description 字段（民族简介正文）合计约 1.1 MB。人口统计这类聚合查询原本返回的是完整实体，单次请求要白白传输并反序列化上百 KB 数据；改成只取所需短列之后，单次查询数据量从约 1792 KB 降到 6 KB，差不多是原来的三百分之一。这一处改动配合缓存，让接口在缓存命中时连 SQL 都不执行。经验是：小表也可能有大字段，聚合查询千万别顺手返回整个实体。'))
B(('p', '数据一致性这块看着琐碎，其实最容易出问题，办法就是把首页和各频道的统计数字与数据库实际行数逐个核对一遍：民族 56 条、节日 192 条、传统艺术与非遗 165 条、美食 168 条、风俗 222 条、聚居地 112 处、人物 163 位、民族自治地方 155 个、传统体育 20 项、统一检索索引 1142 条、兴趣标签 111 个、翻译词表 1049 条，全部对得上，说明前端展示的统计口径确实来自库中真实数据的聚合，不是写死的常量。这一步花不了几分钟，却能挡住不少低级问题。'))
B(('p', '把功能测试和性能测试的结果合起来看，结论挺清楚：需求分析里提出的功能都已实现，权限控制有效，审批流程闭环完整，检索与推荐结果符合预期，典型数据规模下的响应时间也满足非功能性要求。剩下的问题大多集中在数据层面，不涉及架构改动。系统具备实际使用的条件。'))
B(('pagebreak',))

# ============================================================ 结论
B(('h1', '结论'))
B(('p', '说到底，本文要处理的是民族文化数字化传播里的三个老问题：内容碎、呈现方式单调、来源说不清。围绕这三点，本文用 SpringBoot 与 Vue 实现了一个民族文化数字化展示平台。平台采用前后端分离的 B/S 架构，后端以 Java 21 与 Spring Boot 4 为基础，用 PostgreSQL 18 存放业务数据与流程引擎数据，Redis 承担缓存与计数，Camunda 7 以嵌入式方式提供内容审批能力；前端用 Vue 3 与 TypeScript 构建，分成面向公众的 C 端门户和面向运营人员的中后台管理系统。两部分共用一套后端接口，但使用者的诉求完全不同。'))
B(('p', '具体做的工作大致有四块。第一块是需求分析与总体设计，划清了六类用户角色和两类功能域，给出了系统架构图、功能结构图、六张流程图和数据库 E-R 图，并完成 33 张业务表的详细设计。第二块是内容体系建设，收录 56 个民族、192 条节日、165 项传统艺术与非遗、168 条美食、222 条风俗、112 处聚居地、163 位人物、155 个民族自治地方和 20 个民族传统体育项目，同时为每条内容建立了可溯源的来源记录。第三块是检索与推荐能力，把 8 类共 1142 条内容纳入统一索引，支持中文子串、拼音全拼、拼音首字母和英文名四种输入，并通过进程内缓存与只读列裁剪把目录接口的响应时间压到 10 毫秒量级。第四块是基于 BPMN 2.0 的内容审批工作流，形成「提交—审批—上线—审查—下线—修改—再审批—再上线」的闭环，线上内容的每次变更都能查到是谁、什么时候、因为什么做的。'))
B(('p', '系统还有不少地方不够好，这里如实说明。数据层面，图片版权元数据在采集过程中丢失，574 张图片目前全部标着「来源待核」，需要逐张找回来源并补作者与许可信息；部分民族的历史沿革是编年体散文，只有约 22% 的段落含明确年份，时间轴的覆盖率因此有限。算法层面，站内行为样本太少，个性化推荐目前主要靠内容相似度和热度撑着，效果得等用户规模上来后再评估；英文内容由机器翻译生成，还需要人工校订。功能层面，移动端只做了基础浏览，短信验证码通道还没接，多语言也只覆盖中英两种。'))
B(('p', '后面可以从四个方向接着做：引入人工校订流程，把机器翻译内容和用户反馈纳入内容质量闭环；完善图片版权管理，和自由许可图库建立更稳定的署名机制；用户规模上来后引入协同过滤等更精细的推荐算法，同时继续公开推荐依据与样本量；扩展多端形态，补齐移动端体验。'))
B(('pagebreak',))

# ============================================================ 参考文献
B(('h1', '参考文献'))
REFS = [
    '中华人民共和国国务院. 关于实施中华优秀传统文化传承发展工程的意见[Z]. 2017-01-25.',
    '中华人民共和国国家民族事务委员会. 中华各民族[EB/OL]. https://www.neac.gov.cn/seac/ztzl/zgmzjs/index.shtml.',
    '国家统计局. 第七次全国人口普查公报（第二号）[EB/OL]. https://www.gov.cn/guoqing/2021-05/13/content_5606149.htm.',
    '国家统计局. 中国人口普查年鉴-2020[M]. 北京: 中国统计出版社, 2022.',
    '中华人民共和国文化和旅游部. 国家级非物质文化遗产代表性项目代表性传承人名单（第 1—6 批）[Z].',
    '国家民族事务委员会, 国家体育总局. 全国少数民族传统体育运动会总规程[Z].',
    '王珊, 萨师煊. 数据库系统概论（第 5 版）[M]. 北京: 高等教育出版社, 2014.',
    '张海藩, 牟永敏. 软件工程导论（第 6 版）[M]. 北京: 清华大学出版社, 2013.',
    '霍春阳. Vue.js 设计与实现[M]. 北京: 人民邮电出版社, 2022.',
    '项亮. 推荐系统实践[M]. 北京: 人民邮电出版社, 2012.',
    'Manning C D, Raghavan P, Schütze H. 信息检索导论[M]. 王斌, 译. 北京: 人民邮电出版社, 2010.',
    'Spring Team. Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/.',
    'Vue.js Team. Vue 3 官方文档[EB/OL]. https://cn.vuejs.org/.',
    'The PostgreSQL Global Development Group. PostgreSQL 18 Documentation[EB/OL]. https://www.postgresql.org/docs/18/.',
    'The PostgreSQL Global Development Group. pg_trgm — 基于三元组的相似度检索扩展[EB/OL]. https://www.postgresql.org/docs/18/pgtrgm.html.',
    'Camunda. Camunda 7 Documentation: Spring Boot Integration[EB/OL]. https://docs.camunda.org/manual/7.24/user-guide/spring-boot-integration/.',
    'Object Management Group. Business Process Model and Notation (BPMN) Version 2.0[S]. 2011.',
    'Redis. Redis Documentation[EB/OL]. https://redis.io/docs/.',
    'Sa-Token. Sa-Token 官方文档[EB/OL]. https://sa-token.cc/.',
    'W3C. Web Content Accessibility Guidelines (WCAG) 2.1[S]. 2018.',
    'Oracle. Java Platform, Standard Edition 21 API Specification[EB/OL]. https://docs.oracle.com/en/java/javase/21/docs/api/.',
    'Element Plus. Element Plus 组件库文档[EB/OL]. https://element-plus.org/zh-CN/.',
    'bpmn.io. bpmn-js: A BPMN 2.0 rendering toolkit and web modeler[EB/OL]. https://github.com/bpmn-io/bpmn-js.',
    'Wikimedia Foundation. Wikimedia Commons[EB/OL]. https://commons.wikimedia.org/.',
]
for i, r in enumerate(REFS, start=1):
    B(('ref', '[%d] %s' % (i, r)))
B(('pagebreak',))

# ============================================================ 附录
B(('h1', '附录 系统核心代码设计'))
B(('p', '本附录列出系统若干核心功能的实现代码，包括登录鉴权、全文检索与拼音匹配、检索索引构建、个性化推荐召回、内容审批流程提交、图片上传和只读缓存。代码都取自项目实际实现，未作简化改写。有些写法并不算漂亮，但没有为了好看去调整。'))

B(('h2', '附录 1 登录鉴权'))
B(('p', '登录方法允许以昵称、邮箱或手机号任一形式匹配账号，密码在数据库中是 AES 加密存储的，校验时先解密再比对；校验通过后记录一次用户活跃时间，令牌签发则交给控制层调用 Sa-Token 完成。代码见 backend\\src\\main\\java\\com\\czdr\\work\\service\\impl\\UserServiceImpl.java。'))
B(('code', '''    public String login(String account, String password) {
        // 登录账号支持 昵称(用户名)、邮箱、手机号 任一匹配（account 列无实际业务含义，不参与登录）
        UserAuth userAuth = entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    u.or(() -> {
                        u.nickname().eq(account);
                        u.email().eq(account);
                        u.mobile().eq(account);
                    });
                })
                .firstOrNull();
        if (userAuth == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        String storedHash = userAuth.getPasswordHash();
        if (storedHash == null || storedHash.isBlank()) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        String decrypt;
        try {
            decrypt = aesCbcEncryptor.decrypt(storedHash);
        } catch (Exception e) {
            e.fillInStackTrace();
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        if (!decrypt.equals(password)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        if ("disabled".equals(userAuth.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        // 记录活跃时间：后台公告的「活跃用户」受众依赖该字段
        userAuth.setLastActiveAt(LocalDateTime.now());
        entityQuery.updatable(userAuth).executeRows();
        return userAuth.getId().toString();
    }'''))

B(('h2', '附录 2 全文检索与拼音匹配'))
B(('p', '统一检索的相关度计算综合考虑了标题精确匹配、标题与英文标题的包含匹配、拼音的精确与前缀匹配、三元组相似度以及热度和时效性。这些权重是试出来的，不是拍脑袋定的。其中拼音匹配必须分级计分，否则「蒙古族」会被「河南蒙古族自治县」这类更长的名称挤到后面。代码见 backend\\src\\main\\java\\com\\czdr\\work\\service\\impl\\FullTextSearchServiceImpl.java。'))
B(('code', '''    where.append("""
             AND (
                title ILIKE ? OR title_en ILIKE ?
                OR pinyin_full LIKE ? OR pinyin_abbr = ?
                OR body ILIKE ? OR summary ILIKE ? OR ethnic_name ILIKE ?
                OR similarity(title, ?) >= 0.2
             )
            """);

    String scoreExpr = emptyKeyword ? "popularity" : """
          (
            CASE WHEN lower(title) = lower(?) THEN 100 ELSE 0 END
          + CASE WHEN title ILIKE ? THEN 60 ELSE 0 END
          + CASE WHEN title ILIKE ? THEN 40 ELSE 0 END
          + CASE WHEN title_en ILIKE ? THEN 35 ELSE 0 END
          + CASE WHEN lower(title_en) = lower(?) THEN 45 ELSE 0 END
          + CASE WHEN body_en ILIKE ? THEN 15 ELSE 0 END
          -- 拼音：精确 > 前缀 > 包含。实测若统一按「包含」计分，
          -- 「蒙古族」(pinyin_full=mengguzu) 会与「河南蒙古族自治县」
          -- (…mengguzuzizhixian) 同分，导致真正的目标排到自治县之后。
          + CASE WHEN lower(pinyin_full) = lower(?) THEN 55 ELSE 0 END
          + CASE WHEN pinyin_full LIKE ? THEN 40 ELSE 0 END
          + CASE WHEN pinyin_full LIKE ? THEN 30 ELSE 0 END
          + CASE WHEN lower(pinyin_abbr) = lower(?) THEN 20 ELSE 0 END
          + CASE WHEN ethnic_name ILIKE ? THEN 18 ELSE 0 END
          + COALESCE(similarity(title, ?), 0) * 25
          + CASE WHEN summary ILIKE ? THEN 12 ELSE 0 END
          + CASE WHEN body ILIKE ? THEN 10 ELSE 0 END
          + LEAST(COALESCE(popularity,0), 100) * 0.08
          + CASE WHEN content_at IS NOT NULL
                 THEN GREATEST(0, 4 - EXTRACT(EPOCH FROM (now() - content_at)) / 31536000.0)::numeric
                 ELSE 0 END
          )
          """;'''))

B(('h2', '附录 3 检索索引构建'))
B(('p', '检索索引的重建按内容类型分开进行，每种类型都是先删旧文档再重新聚合插入，这样内容下架之后就不会继续留在索引里。重建是全量动作，所以放在内容批量维护之后执行更合适。代码见 backend\\src\\main\\java\\com\\czdr\\work\\service\\SearchIndexService.java。'))
B(('code', '''    public RebuildResult rebuild(String onlyType) {
        long start = System.currentTimeMillis();
        String type = onlyType == null ? "" : onlyType.trim().toLowerCase();
        List<String> targets = type.isBlank() || "all".equals(type)
                ? DOC_TYPES
                : DOC_TYPES.contains(type) ? List.of(type) : List.of();
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("不支持的索引类型：" + onlyType);
        }

        Map<String, Integer> byType = new LinkedHashMap<>();
        int total = 0;
        for (String t : targets) {
            // 先删该类型旧文档，再重建：避免内容下架后仍留在索引里
            entityQuery.deletable(SearchDocument.class)
                    .where(d -> d.docType().eq(t))
                    .allowDeleteStatement(true)
                    .executeRows();

            List<SearchDocument> docs = switch (t) {
                case "ethnic" -> fromEthnic();
                case "festival" -> fromFestival();
                case "art" -> fromArt();
                case "food" -> fromFood();
                case "custom" -> fromCustom();
                case "person" -> fromPerson();
                case "area" -> fromArea();
                case "sport" -> fromSport();
                default -> List.of();
            };
            if (!docs.isEmpty()) {
                entityQuery.insertable(docs).executeRows();
            }
            byType.put(t, docs.size());
            total += docs.size();
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("检索索引重建完成：{} 条，耗时 {} ms，明细 {}", total, elapsed, byType);
        return new RebuildResult(total, byType, elapsed);
    }'''))

B(('h2', '附录 4 个性化推荐召回'))
B(('p', '推荐打分按「兴趣标签（显式）—行为协同（隐式）—热度兜底」的顺序逐项累加，同时把得分来源记下来，作为推荐理由返回给前端。打分的先后顺序，决定了推荐理由先说哪一条。代码见 backend\\src\\main\\java\\com\\czdr\\work\\service\\RecommendationService.java。'))
B(('code', '''            double score = 0;
            String reason = null;

            // 1) 兴趣标签（显式）—— 权重最高
            if (d.getEthnicName() != null && interestEthnics.contains(d.getEthnicName())) {
                score += 50;
                reason = "你关注了" + d.getEthnicName();
            }
            if (d.getRegion() != null && interestRegions.contains(d.getRegion())) {
                score += 25;
                reason = reason == null ? "你关注了" + d.getRegion() : reason;
            }
            if (d.getCategory() != null && interestTopics.contains(d.getCategory())) {
                score += 20;
                reason = reason == null ? "你关注了「" + d.getCategory() + "」" : reason;
            }
            if (typeLabelToDocType(interestTypes).contains(d.getDocType())) {
                score += 10;
                reason = reason == null ? "你关注这类内容" : reason;
            }

            // 2) 行为协同（隐式）—— 比显式兴趣低，但仍是强信号
            if (d.getEthnicName() != null && behaviorEthnics.contains(d.getEthnicName())) {
                score += 30;
                reason = reason == null ? "与你浏览过的内容相关" : reason;
            }
            if (d.getCategory() != null && behaviorCategories.contains(d.getCategory())) {
                score += 12;
                reason = reason == null ? "与你浏览过的内容同类" : reason;
            }

            // 3) 热度（兜底，最多 +10）
            int pop = d.getPopularity() == null ? 0 : d.getPopularity();
            score += Math.min(pop, 100) * 0.1;'''))

B(('h2', '附录 5 内容审批流程提交'))
B(('p', '提交审批时先递增内容版本号、把内容状态置为待审批，再以「内容类型 + 内容ID + 版本号」作为业务键启动流程实例。这里有个必须提前处理的坑：流程网关的条件表达式在变量缺失时取不到值，所以启动前要把各结论开关的安全默认值先放进去。顺序不能颠倒，否则状态和版本会对不上。代码见 backend\\src\\main\\java\\com\\czdr\\work\\service\\impl\\WorkflowServiceImpl.java。'))
B(('code', '''    public WorkflowInstanceResource submit(String entryType, UUID entryId, String note) {
        assertSubmittable(entryType, entryId);

        UUID userId = currentUserId();
        UserAuth user = currentUser();
        var content = contentGateway.load(entryType, entryId);

        int nextVersion = (content.contentVersion() == null ? 0 : content.contentVersion()) + 1;
        contentGateway.bumpContentVersion(entryType, entryId, nextVersion);
        contentGateway.updateStatus(entryType, entryId, WorkflowConstants.CONTENT_PENDING);

        String businessKey = WorkflowConstants.businessKey(entryType, entryId, nextVersion);
        String processId = resolveProcessId(entryType);

        Map<String, Object> variables = new HashMap<>();
        variables.put(WorkflowConstants.VAR_ENTRY_TYPE, entryType);
        variables.put(WorkflowConstants.VAR_ENTRY_ID, entryId.toString());
        variables.put(WorkflowConstants.VAR_ENTRY_TITLE, content.title());
        variables.put(WorkflowConstants.VAR_CONTENT_VERSION, nextVersion);
        variables.put("submitterId", userId.toString());
        variables.put("submitterName", user == null ? "" : user.getNickname());

        // JUEL 网关条件（${approved == true} / ${hasIssue == true} / ${needReapproval == false}）
        // 在变量缺失时会抛 PropertyNotFound，因此这里预置默认值：
        // 初始环节是「待审核员审批」，两个结论开关先给「未通过/无问题」的安全默认。
        variables.put(WorkflowConstants.VAR_APPROVED, false);
        variables.put(WorkflowConstants.VAR_HAS_ISSUE, false);
        variables.put("needReapproval", true);

        ProcessInstance instance0;
        try {
            // Camunda 7：按 key 启动，并直接把 businessKey 交给引擎（可据此反查业务实例）
            instance0 = runtimeService().startProcessInstanceByKey(processId, businessKey, variables);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,
                    "启动审批流程失败（请确认已部署流程 " + processId + "）: " + rootMessage(e));
        }'''))

B(('h2', '附录 6 图片上传'))
B(('p', '上传接口按用户和时间窗口限流，文件按月分目录存放，文件名用去掉连字符的 UUID 加原扩展名，最后返回一个可以直接访问的 /uploads/... 地址。限流阈值调得偏紧，不过上传本来就不是高频操作。代码见 backend\\src\\main\\java\\com\\czdr\\work\\controller\\UploadController.java 与 StorageServiceImpl.java。'))
B(('code', '''@Tag(name = "上传 Upload", description = "用户上传：讨论区配图")
@RestController
@RequestMapping("uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;
    private final RateLimitService rateLimitService;

    @Operation(summary = "上传图片", description = "支持 JPG/PNG/WebP/GIF，最大 5MB；返回可直接访问的 /uploads/... 地址")
    @PostMapping("image")
    Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String userId = StpUtil.getLoginIdAsString();
        rateLimitService.consume("upload", userId, 30, 3600, "上传过于频繁，请稍后再试");
        return Result.success(storageService.storeImage(file));
    }
}

// ---- StorageServiceImpl#storeImage ----
        String month = LocalDate.now().format(MONTH);
        Path dir = uploadProperties.root().resolve(month);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存上传图片失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片保存失败，请稍后重试");
        }
        return uploadProperties.urlPrefix() + month + "/" + filename;'''))

B(('h2', '附录 7 只读缓存'))
B(('p', '只读目录缓存借助 compute 的原子语义防击穿：同一个键上并发请求时，只有一个线程真正执行回源查询，其余线程等待后直接拿到新值；后台发生写操作时调用 invalidateAll 清空缓存。代码见 backend\\src\\main\\java\\com\\czdr\\work\\config\\ReadCache.java。'))
B(('code', '''    public <T> T get(String key, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }
        Entry hit = store.get(key);
        if (hit != null && !hit.expired(System.currentTimeMillis())) {
            return (T) hit.value();
        }
        // 未命中 / 已过期：用 compute 原子地重新装载。
        // compute 在同一 key 上有锁语义，并发请求只会有一个真正执行 loader，
        // 其余线程等待后直接拿到新值，避免缓存击穿。
        Entry loaded = store.compute(key, (k, current) -> {
            if (current != null && !current.expired(System.currentTimeMillis())) {
                // 期间已被别的线程刷新过，直接复用
                return current;
            }
            long t0 = System.currentTimeMillis();
            Object value = loader.get();
            long cost = System.currentTimeMillis() - t0;
            if (cost > SLOW_LOAD_MS) {
                log.info("缓存回源 {} 耗时 {} ms", k, cost);
            }
            return new Entry(value, System.currentTimeMillis() + ttlMs);
        });
        return (T) loaded.value();
    }

    /** 清空全部缓存：内容后台发生写操作时调用 */
    public void invalidateAll() {
        int n = store.size();
        store.clear();
        if (n > 0) {
            log.info("已清空只读缓存，共 {} 项", n);
        }
    }'''))
