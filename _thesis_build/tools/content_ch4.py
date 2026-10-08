# -*- coding: utf-8 -*-
"""正文内容：第 4 章 系统设计（含数据库表设计，字段信息取自实际数据库结构）。"""
import collections
import io

TSV = r'C:\codeDev\56\app\_thesis_build\data\columns.tsv'

# 字段注释字典（按字段名），个别表存在同名不同义的字段时用 OVERRIDE 覆盖
COMMON = {
    'id': '主键ID',
    'slug': '唯一标识（URL 友好名）',
    'name': '名称',
    'name_en': '英文名称',
    'title': '标题',
    'title_en': '英文标题',
    'subtitle': '副标题',
    'summary': '内容简介',
    'summary_en': '英文简介',
    'description': '详细描述',
    'description_en': '英文描述',
    'description_en_source': '英文来源（manual 人工 / machine 机器翻译）',
    'content': '正文内容',
    'body': '检索正文',
    'body_en': '英文检索正文',
    'cover_image': '封面图访问地址',
    'image': '配图访问地址',
    'images': '图片地址列表（JSONB）',
    'tags': '标签（JSONB）',
    'theme_color': '民族主题色',
    'order_num': '显示顺序',
    'sort_order': '条目排序号',
    'created_by': '创建人ID',
    'updated_by': '更新人ID',
    'created_at': '创建时间',
    'updated_at': '更新时间',
    'pinyin': '名称拼音',
    'population': '人口数（2020 年第七次全国人口普查口径）',
    'language_family': '所属语系',
    'region': '主要聚居地（JSONB）',
    'languages': '使用的语言（JSONB）',
    'scripts': '使用的文字（JSONB）',
    'religion': '宗教信仰（JSONB）',
    'self_name': '民族语自称',
    'ethnic_group_id': '所属民族ID（外键 ethnic_group.id）',
    'ethnic_group_name': '所属民族名称',
    'ethnic_name': '所属民族名称',
    'origin': '起源与由来',
    'customs': '习俗活动列表（JSONB）',
    'solar_date': '公历日期',
    'lunar_date': '农历（或其他历法）日期表述',
    'province': '所属省级行政区',
    'city': '所属城市',
    'longitude': '经度',
    'latitude': '纬度',
    'person_name': '人物姓名',
    'role_type': '角色类型（inheritor 代表性传承人 / master 历史文化名家）',
    'domain': '所属领域',
    'lifespan': '生卒年',
    'bio': '个人简介',
    'avatar': '头像地址',
    'account': '登录账号',
    'password_hash': '密码（AES 加密存储）',
    'nickname': '昵称',
    'mobile': '手机号',
    'email': '邮箱',
    'lang': '内容／界面语言',
    'locale': '语言区域',
    'timezone': '时区',
    'website': '个人网站',
    'security_question': '密保问题',
    'security_answer': '密保答案（AES 加密存储）',
    'last_active_at': '最后活跃时间',
    'muted_until': '禁言截止时间',
    'banned_until': '封禁截止时间',
    'trust_level': '社区信任等级',
    'allow_stranger_message': '是否允许陌生人私信',
    'role_id': '角色ID（外键 role.id）',
    'permission_id': '权限ID（外键 permission.id）',
    'user_id': '用户ID（外键 user_account.id）',
    'tag_id': '兴趣标签ID（外键 interest_tag.id）',
    'code': '编码',
    'doc_type': '内容类型（ethnic 民族 / festival 节日 / art 艺术 …）',
    'doc_id': '内容主键ID',
    'url': '原文链接',
    'pinyin_full': '名称拼音全拼（如 mengguzu）',
    'pinyin_abbr': '名称拼音首字母（如 mgz）',
    'popularity': '热度值',
    'content_at': '内容发布时间',
    'category': '分类',
    'entry_type': '内容类型',
    'entry_id': '内容主键ID',
    'entry_title': '内容标题',
    'content_version': '内容版本号',
    'status': '状态',
    'type': '类型',
    'level': '行政级别（自治区 / 自治州 / 自治县·旗）',
    'ethnic_groups': '自治民族（多个以顿号分隔）',
    'established_year': '成立年份',
    'seat': '行政中心',
    'ethnic_origins': '相关民族',
    'sub_events': '子项列表',
    'equipment': '场地器材',
    'venue': '比赛场地',
    'team_size': '参赛人数',
    'first_event_year': '入会（列入竞赛项目）年份',
    'heritage_link': '相关非物质文化遗产',
    'image_path': '图片相对路径',
    'caption': '图片说明',
    'credit_status': '署名核实状态（pending 来源待核 / verified 已核实）',
    'author': '图片作者',
    'license': '许可协议',
    'license_url': '许可协议链接',
    'source_url': '来源页面链接',
    'source_site': '来源站点',
    'attribution_required': '许可是否要求署名',
    'remark': '备注',
    'collect_method': '采集方式（scrape 程序抓取 / ocr 识别 / manual 人工整理 / api 接口）',
    'source_type': '来源类型（official 官方权威 / open 开放许可 / other 其他）',
    'publisher': '发布机构',
    'publisher_short': '发布机构简称',
    'document_title': '文档名称',
    'note': '关联说明',
    'target_type': '目标内容类型',
    'target_id': '目标内容主键ID',
    'source_id': '来源ID（外键 content_source.id）',
    'board_id': '所属板块ID',
    'parent_id': '父级回复ID',
    'floor_no': '楼层号',
    'quote_post_id': '引用的回复ID',
    'like_count': '点赞数',
    'reply_count': '回复数',
    'favorite_count': '收藏数',
    'view_count': '浏览数',
    'floor_count': '楼层总数',
    'last_reply_at': '最后回复时间',
    'last_reply_user_id': '最后回复人ID',
    'edited_at': '最后编辑时间',
    'edit_count': '编辑次数',
    'pinned': '是否置顶',
    'featured': '是否加精',
    'locked': '是否锁定（禁止回复）',
    'linked_type': '关联内容类型',
    'linked_id': '关联内容ID',
    'risk_level': '内容风险等级',
    'hit_words': '命中的敏感词',
    'payload': '附加数据（JSONB）',
    'read_at': '已读时间',
    'emailed': '是否已发送邮件通知',
    'actor_id': '触发者用户ID',
    'dimension': '标签维度（ethnic 民族 / region 地域 / content_type 内容类型 / topic 主题）',
    'color': '标签配色',
    'enabled': '是否启用',
    'weight': '权重',
    'action': '行为类型（view / search / favorite / like …）',
    'keyword': '检索关键词',
    'schema': '表单结构定义（JSONB）',
    'submitter_id': '提交人ID',
    'reviewer_id': '审核人ID',
    'reject_reason': '驳回原因',
    'submitted_at': '提交时间',
    'reviewed_at': '审核完成时间',
    'instance_id': '流程实例ID（外键 workflow_instance.id）',
    'last_opinion': '最近一次审批意见',
    'process_definition_id': '流程定义ID',
    'process_definition_version': '流程定义版本',
    'process_instance_key': '流程实例键（引擎返回）',
    'business_key': '业务键（内容类型 + 内容ID + 版本号）',
    'current_stage': '当前环节',
    'submitter_name': '提交人姓名',
    'current_assignee_id': '当前处理人ID',
    'current_task_key': '当前任务键',
    'current_task_name': '当前任务名称',
    'round_no': '审批轮次',
    'started_at': '流程启动时间',
    'finished_at': '流程结束时间',
    'stage': '流程环节',
    'decision': '审批结论（approve 通过 / reject 退回）',
    'opinion': '审批意见',
    'operator_id': '操作人ID',
    'operator_name': '操作人姓名',
    'operator_role': '操作人角色',
    'camunda_task_key': '引擎任务键',
    'task_name': '任务名称',
    'source_locale': '源语言',
    'target_locale': '目标语言',
    'term': '术语原文',
    'translation': '术语译文',
    'topic_id': '所属专题ID（外键 topic.id）',
}

OVERRIDE = {
    ('festival', 'type'): '节日类型（traditional 传统 / religious 宗教 / agricultural 农事）',
    ('festival', 'customs'): '习俗活动（JSONB）',
    ('art', 'category'): '艺术类别（music 音乐 / dance 舞蹈 / drama 戏剧 / costume 服饰 / craft 手工艺 / architecture 建筑）',
    ('ethnic_custom', 'category'): '风俗类别（服饰 / 饮食 / 婚嫁 / 礼仪 …）',
    ('ethnic_custom', 'title'): '风俗条目标题',
    ('ethnic_custom', 'content'): '风俗正文',
    ('traditional_sport', 'category'): '项目类别（球类 / 水上 / 力量对抗 / 射击与技巧 / 竞速 / 武术 / 马术 / 健身操 / 秋千）',
    ('traditional_sport', 'name'): '项目名称',
    ('autonomous_area', 'name'): '自治地方名称',
    ('search_document', 'title'): '标题（参与检索）',
    ('search_document', 'summary'): '摘要（参与检索）',
    ('search_document', 'region'): '所属地域',
    ('search_document', 'category'): '分类标签',
    ('discussion_topic', 'title'): '帖子标题',
    ('discussion_topic', 'content'): '帖子正文',
    ('discussion_post', 'content'): '回复正文',
    ('discussion_post', 'status'): '审核状态（pending 待审 / published 通过 / rejected 驳回）',
    ('notification', 'type'): '通知类型（reply / mention / system / workflow …）',
    ('notification', 'title'): '通知标题',
    ('notification', 'content'): '通知正文',
    ('content_source', 'name'): '来源名称',
    ('content_source', 'url'): '来源链接',
    ('content_review', 'status'): '审核状态（pending 待审 / approved 通过 / rejected 驳回）',
    ('content_review', 'entry_type'): '内容类型',
    ('form_config', 'name'): '表单名称',
    ('form_config', 'description'): '表单说明',
    ('form_config', 'status'): '状态（enabled 启用 / disabled 停用）',
    ('user_account', 'status'): '账号状态（active 正常 / disabled 已禁用）',
    ('role', 'name'): '角色名称',
    ('permission', 'name'): '权限点名称',
    ('translate_glossary', 'enabled'): '是否启用',
    ('interest_tag', 'name'): '标签名称',
    ('interest_tag', 'name_en'): '标签英文名',
    ('interest_tag', 'description'): '标签说明',
    ('topic', 'title'): '专题标题',
    ('topic', 'description'): '专题说明',
    ('image_credit', 'remark'): '备注',
    ('ethnic_group', 'region'): '主要聚居地（JSONB）',
    ('ethnic_group', 'status'): '内容状态（draft 草稿 / pending 待审批 / published 已发布 / offline 已下线）',
}

TYPES = {
    'character varying': 'varchar',
    'character': 'char',
    'timestamp with time zone': 'timestamp',
    'timestamp without time zone': 'timestamp',
    'integer': 'int',
    'bigint': 'bigint',
    'smallint': 'smallint',
    'boolean': 'boolean',
    'numeric': 'numeric',
    'double precision': 'double',
    'date': 'date',
    'uuid': 'uuid',
    'text': 'text',
    'jsonb': 'jsonb',
    'json': 'json',
}

TABLES = [
    ('user_account', '用户账号表'),
    ('role', '角色表'),
    ('permission', '权限点表'),
    ('user_role', '用户角色关联表'),
    ('role_permission', '角色权限关联表'),
    ('ethnic_group', '民族表'),
    ('ethnic_custom', '民族风俗表'),
    ('ethnic_location', '民族聚居地表'),
    ('festival', '节日表'),
    ('art', '传统艺术与非遗表'),
    ('food', '民族美食表'),
    ('person_profile', '人物档案表'),
    ('autonomous_area', '民族自治地方表'),
    ('traditional_sport', '民族传统体育表'),
    ('topic', '文化专题表'),
    ('topic_entry', '专题条目关联表'),
    ('favorite', '收藏表'),
    ('like_record', '点赞记录表'),
    ('search_document', '统一检索索引表'),
    ('interest_tag', '兴趣标签表'),
    ('user_interest', '用户兴趣表'),
    ('user_behavior', '用户行为表'),
    ('workflow_instance', '审批流程实例表'),
    ('workflow_opinion', '审批意见表'),
    ('content_review', '内容审核记录表'),
    ('discussion_topic', '讨论主题帖表'),
    ('discussion_post', '讨论回复表'),
    ('translate_glossary', '翻译词表'),
    ('image_credit', '图片署名表'),
    ('content_source', '内容来源表'),
    ('content_source_link', '内容来源关联表'),
    ('form_config', '表单配置表'),
    ('notification', '站内通知表'),
]


def load_columns():
    cols = collections.OrderedDict()
    with io.open(TSV, encoding='utf-8-sig') as f:
        for line in f:
            p = line.rstrip('\n').split('\t')
            if len(p) < 8:
                continue
            cols.setdefault(p[0], []).append(p)
    return cols


def build_table_blocks():
    cols = load_columns()
    headers = ['编号', '字段名', '类型', '长度', '是否非空', '是否主键', '注释']
    widths = [1.1, 3.5, 2.0, 1.4, 1.6, 1.6, 4.2]
    blocks = []
    for idx, (tname, cn) in enumerate(TABLES, start=1):
        rows = []
        for i, c in enumerate(cols.get(tname, []), start=1):
            _, pos, col, dtype, length, nullable, default, is_pk = c[:8]
            tp = TYPES.get(dtype, dtype)
            comment = OVERRIDE.get((tname, col)) or COMMON.get(col, '')
            rows.append([
                str(i), col, tp, length or '', '是' if nullable == 'N' else '否',
                '是' if col == 'id' else '否', comment,
            ])
        blocks.append(('tbl', '表 4-%d %s（%s）' % (idx, tname, cn), headers, rows, widths))
    return blocks


BLOCKS = []
B = BLOCKS.append

B(('h1', '4 系统设计'))
B(('h2', '4.1 系统架构设计'))
B(('p', '系统采用前后端分离的分层架构，自上而下分成客户端与展示层、接口与安全层、业务服务层、数据与引擎层四层，总体架构如图 4-1 所示。这样切分的原因很简单：让每一层只管一件事，改动的影响面就可控。'))
B(('p', '（1）客户端与展示层。由 C 端门户、中后台管理系统和可选的移动端组成。三者都以 HTTP 接口为唯一数据来源，彼此独立部署。C 端面向公众，重点是内容呈现、检索交互和响应式适配；中后台面向运营人员，重点是表格、表单和流程操作的效率；移动端基于 React Native 与 Expo 实现，直接复用同一套后端接口。三者之间没有直接耦合，改一个不影响另外两个。'))
B(('p', '（2）接口与安全层。Spring MVC 控制器统一对外提供 RESTful 接口，返回值结构一致；Sa-Token 负责登录状态校验与权限点校验，未登录或权限不足时由全局异常处理器转成规范的错误响应。跨域策略、请求参数校验和接口限流也在这一层完成，比如上传接口会按用户与时间窗口限流。这些防护集中在入口处，业务代码里就不用重复判断了。'))
B(('p', '（3）业务服务层。按业务域划分成内容服务（民族、节日、艺术、美食、风俗、专题、人物、自治地方、传统体育）、检索与推荐服务、互动与讨论区服务、翻译服务、通知服务和审批工作流服务等模块。模块之间通过接口调用，加密、缓存、分页、文件存储这类公共能力则下沉成独立组件，免得每个模块各写一套。'))
B(('p', '（4）数据与引擎层。PostgreSQL 18 承担全部业务数据和流程引擎 ACT_* 表的存储，其中半结构化内容用 JSONB、模糊检索靠 pg_trgm、更新时间由触发器维护；Redis 承担验证码、计数与热点缓存；Camunda 7 以嵌入式方式与业务服务跑在同一个 JVM 里，并共用同一个数据源，内容状态变更与流程推进因此处在同一个事务边界内。同库同事务带来的便利，在后面讲审批流程时体现得最明显。'))
B(('fig', '_thesis_build/figures/fig4-1-arch.png', '图 4-1 系统总体架构图'))

B(('h2', '4.2 系统结构设计'))
B(('p', '先说结论。按「前台门户 + 中后台管理」的总体划分，系统的功能结构自上而下分成五个功能域：门户浏览与检索、文化资料专栏、互动与个人中心、内容运营后台、治理与系统管理。每个功能域再往下细分若干功能模块，整体结构如图 4-2 所示。这样分层的好处是，新加一个频道能很快找到它该待的位置。'))
B(('p', '门户浏览与检索功能域承担面向公众的内容呈现，包括首页与专题聚合、民族频道与详情、节日频道与日历、传统艺术与非遗名录、美食与风俗及民族语文、全文检索六个模块。这几个模块的入口都放在顶部导航里，路径一般不超过两层。文化资料专栏功能域负责文化专题类内容的组织，包括人物专栏、民族自治地方、民族传统体育、民族服饰与民居建筑、人口图谱与分布地图、兴趣与个性化推荐六个模块。互动与个人中心功能域面向注册用户，包括注册登录与个人资料、收藏点赞与分享、讨论区发帖与回复、私信与通知、我的社区与举报、中英文切换六个模块。内容运营后台功能域面向内容编辑与内容管理员，覆盖各类内容的维护、专题与表单配置、检索索引与兴趣标签、翻译词表与图片署名、内容来源维护等工作。治理与系统管理功能域面向审核员与系统管理员，包括内容审批工作流待办、BPMN 流程建模与部署、用户与角色权限管理、讨论区治理与审核、统计看板与反馈、上传与文件管理六个模块。这五个功能域合起来，就是平台的全部功能。'))
B(('fig', '_thesis_build/figures/fig4-2-func-tree.png', '图 4-2 系统整体功能结构图'))

B(('h2', '4.3 系统功能设计'))
B(('p', '这一节用流程图把系统在开发过程和典型业务操作中的处理步骤讲清楚。流程图统一使用开始事件、处理过程、判断分支和结束事件等标准符号，判断分支标「是／否」，需要退回上一步重新处理的分支用虚线箭头表示。这样即使先不看正文，也能把流程大致读懂。'))
B(('h3', '4.3.1 系统开发流程'))
B(('p', '开发过程大致是这样走的：先做需求调研与分析，把用户角色和功能边界定下来；接着做总体设计，确定技术选型、功能结构和数据库结构；然后搭环境、建表、初始化内容数据，再分别开发后端接口和前端页面；前后端联调完就进入测试，功能或性能不达标就返回修复并回归测试，直到达标才部署上线、整理文档。整个过程本身没什么特别，难的是每一步都留下痕迹，后面出了问题能回溯到具体环节。系统开发流程如图 4-3 所示。'))
B(('fig', '_thesis_build/figures/fig4-3-dev-flow.png', '图 4-3 系统开发流程图'))
B(('h3', '4.3.2 用户登录流程'))
B(('p', '登录是使用受保护功能的前提。用户打开登录页输入账号密码后，前端先做必填项和格式校验；通过之后才把请求发给后端。后端按账号（昵称、邮箱或手机号任一匹配）查出用户记录，比对密码，同时检查账号是否被禁用，三项都过了才由 Sa-Token 签发令牌。前端把令牌写进本地存储，加载用户信息和权限点，再按角色跳到前台门户或个人工作台。任何一个环节失败，系统都给明确的错误提示并退回输入状态，不会含糊地「转圈」。这里特意不区分「账号不存在」和「密码错误」，统一提示「账号或密码错误」，免得给试探账号的人留下线索。用户登录流程如图 4-4 所示。'))
B(('fig', '_thesis_build/figures/fig4-4-login-flow.png', '图 4-4 用户登录流程图'))
B(('h3', '4.3.3 系统操作流程'))
B(('p', '普通用户的典型操作是这样的：访问系统，加载首页和全局导航；系统判断是否已登录，没登录就以游客身份浏览公开内容；进入某个内容频道后提交筛选或检索条件，后端按条件查数据、返回结果集；前端渲染列表，用户点进详情页阅读。如果用户想收藏、点赞、评论或分享，系统会先校验登录状态，再写入互动记录并更新计数；不互动的话，这次操作就以「仅浏览」结束，这也符合大多数人的使用习惯。游客能看什么、登录之后能做什么，这条界线在接口层就切开了，前端只是照着显示。系统操作流程如图 4-5 所示。'))
B(('fig', '_thesis_build/figures/fig4-5-operate-flow.png', '图 4-5 系统操作流程图'))
B(('h3', '4.3.4 添加信息流程'))
B(('p', '添加信息是后台内容维护的入口。管理员在后台选好内容类型、点「新增」，在表单里填名称、所属民族、分类、正文、图片等字段；提交时后端校验必填项与数据格式，不通过就提示具体错误并退回表单；通过则写入对应业务表，同时同步更新统一检索索引与统计口径，最后回到列表页提示添加成功。添加和修改共用一套表单组件，字段校验规则也是同一份配置，省得两处逻辑各改各的。添加信息流程如图 4-6 所示。'))
B(('fig', '_thesis_build/figures/fig4-6-add-flow.png', '图 4-6 添加信息流程图'))
B(('h3', '4.3.5 修改信息流程'))
B(('p', '修改流程和添加大体相似，差别在于要先按主键读出原记录并回填表单，更新时记下内容版本号和操作日志。这两步看着琐碎，却是版本能追溯的基础。这里有一个业务上的关键点：已发布内容的修改不会直接生效，而是走一遍内容审批流程，审核员通过之后才重新发布。这样一来，线上内容的每次变更同样可追溯。换句话说，发布过的内容不存在「悄悄改掉」这回事。修改信息流程如图 4-7 所示。'))
B(('fig', '_thesis_build/figures/fig4-7-update-flow.png', '图 4-7 修改信息流程图'))
B(('h3', '4.3.6 删除信息流程'))
B(('p', '删除不可逆，所以流程里设了两道保护。第一道是前端弹出二次确认对话框，用户确认才继续；第二道是后端在真正删除前校验操作权限和关联数据，如果该内容已经被收藏、被专题引用或者被流程实例关联，就直接拒绝并给出提示，免得留下悬空引用。两道都过了才按主键删除记录，并同步清理检索索引与关联表中的数据。真要删，就得先把关联清干净，这也倒逼运营人员在删之前想清楚。删除信息流程如图 4-8 所示。'))
B(('fig', '_thesis_build/figures/fig4-8-delete-flow.png', '图 4-8 删除信息流程图'))

B(('h2', '4.4 数据库设计'))
B(('h3', '4.4.1 概念设计'))
B(('p', '概念设计这一步其实不难，重点是把系统中的实体、属性和它们之间的联系抽象出来，暂时别纠缠具体字段和数据类型。按需求分析的结果，系统的核心实体包括民族、风俗、聚居地、节日、传统艺术（非遗）、美食、人物、民族自治地方、传统体育、专题、用户、角色与权限、互动记录、检索索引、兴趣标签、用户行为、审批流程实例与审批意见、讨论主题与回复、翻译词条、图片署名、内容来源和表单配置等。实体数量不少，但真正复杂的关系集中在民族和内容这两端。'))
B(('p', '实体之间的联系基本都以「民族」为中心铺开。一个民族可以有多条风俗、多个聚居地、多个节日、多项传统艺术和多条美食，所以民族与这些实体都是一对多；一项传统艺术可能对应多位代表性传承人，人物档案与民族、领域之间是多对一；一个专题能聚合多个不同类型的内容条目，专题与条目之间通过关联表构成多对多；用户与角色、角色与权限都是多对多，分别用用户角色关联表和角色权限关联表落地；收藏和点赞则通过「用户标识 + 内容类型 + 内容标识」实现跨类型关联，不需要为每种内容各建一张表；审批流程实例与内容条目之间用「内容类型 + 内容标识 + 内容版本号」关联，因此一条内容在其生命周期里可以对应多个版本的流程实例。这样设计的好处是，同一内容的历次审批互不干扰，翻查起来也清楚。数据库 E-R 图如图 4-9 所示。'))
B(('fig', '_thesis_build/figures/fig4-9-er.png', '图 4-9 数据库 E-R 图'))
B(('h3', '4.4.2 数据库表设计'))
B(('p', '按概念设计的结果，系统在 PostgreSQL 18 中把业务数据表建了起来。除业务表外，库里还有 Camunda 7 自动创建的 ACT_* 引擎表（流程定义、运行实例、任务、变量等），由引擎自己维护，本文不再展开。下面列出主要业务表的结构。「长度」列只对可变长字符类型有意义；「是否主键」列标记唯一标识记录的字段。字段注释尽量写清楚，免得后面接手的人靠猜。'))
for blk in build_table_blocks():
    B(blk)
B(('p', '还有一点要交代：内容类表（民族、节日、艺术、美食、风俗、专题等）普遍存在 status、order_num、created_by、updated_by、created_at、updated_at、content_version 字段，它们分别承担状态流转、排序展示、操作留痕和版本控制的职责，是审批流程运转的基础。另外，ethnic_group、festival、art 等表还有 description_en、summary_en 等英文列和来源标记列，用来记录机器翻译结果及其来源，中英文内容因此可以分开维护，人工校订过的译文不会被机器翻译覆盖。'))
B(('pagebreak',))
