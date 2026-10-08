# -*- coding: utf-8 -*-
"""正文内容：摘要、ABSTRACT、第 1—2 章。"""

BLOCKS = []
B = BLOCKS.append

# ============================================================ 摘要
B(('title', '摘  要'))
B(('p', '我国是统一的多民族国家，56 个民族在长期历史发展中形成了各具特色的风俗、节日、传统艺术与饮食文化。这些内容网上并不稀缺，但真正用起来并不方便：资料散落在百科条目、新闻报道和短视频里，同一项数据常有不同口径；呈现方式以静态图文为主，用户想按语系、地域或人口规模做横向比较基本做不到；最麻烦的是出处，内容被转载几轮之后，原始来源往往已经丢失，读者没法核对，做研究的人也不敢引用。把这些碎片重新组织成一套查得到、看得懂、来源清楚、手机上也能用的数字化平台，是本课题要解决的问题。'))
B(('p', '本文以「走进多彩 56 个民族世界」为主题，用 Spring Boot 与 Vue 实现了一套民族文化数字化展示平台，整体分成 C 端门户和中后台管理系统两块。C 端面向普通用户，包含民族频道、节日与节日日历、传统艺术与非遗名录、人物专栏、民族自治地方、传统体育、民族语文、文化专题、全文检索、兴趣推荐、讨论区和个人中心等功能；中后台面向内容编辑、审核员、内容管理员与系统管理员，负责民族、节日、艺术、专题、人物档案、自治地方、传统体育、内容来源、图片署名、表单配置等内容维护，另外还有用户与角色权限管理、讨论区治理、检索索引与兴趣标签维护、翻译词表维护和审批工作流。'))
B(('p', '系统采用前后端分离的 B/S 结构。后端以 Java 21 与 Spring Boot 4 为基础，鉴权交给 Sa-Token，权限落到具体权限点而不是写死的角色判断；业务数据与流程引擎数据统一存在 PostgreSQL 18，半结构化字段用 JSONB，中文模糊检索借助 pg_trgm 建 GIN 索引；Redis 负责验证码、计数与热点缓存。审批环节用 Camunda 7 嵌入式引擎，它和应用跑在同一个 JVM、共用同一数据源，部署时不需要额外维护流程引擎服务。前端使用 Vue 3 与 TypeScript，配合 Vite、Pinia 与 Element Plus；中后台还集成了 bpmn-js 与 form-js，可以在浏览器里画流程图、设计审批表单。'))
B(('p', '内容方面，平台目前收录 56 个民族、192 条节日、165 项传统艺术与非遗、168 条美食、222 条风俗、112 处聚居地、163 位人物、155 个民族自治地方和 20 个传统体育项目；8 类共 1142 条内容进入统一检索索引，中文子串、拼音全拼（mengguzu）、拼音首字母（mgz）和英文名都能查。推荐用兴趣标签、浏览行为、内容相似加热度兜底四路召回，并把推荐依据和样本量一并告诉用户。实测下来，全文检索首次调用 83 毫秒，之后重复查询稳定在 5 到 6 毫秒；人口统计这类只读接口命中缓存后不再执行 SQL。测试表明各模块运行稳定，检索与推荐结果符合预期，审批流程完整闭环。'))
B(('p_kw', '关键词：SpringBoot；Vue；PostgreSQL；民族文化数字化；全文检索；个性化推荐；内容审批工作流'))
B(('pagebreak',))

# ============================================================ ABSTRACT
B(('title', 'ABSTRACT'))
B(('p', "China is a unified multi-ethnic country. Over a long history, its 56 ethnic groups have developed distinctive customs, festivals, traditional arts and food cultures, and these together form an important part of Chinese culture. Such material is hardly scarce online, yet it is inconvenient to actually use. Descriptions of one ethnic group are scattered across encyclopedia entries, news reports and short videos, and figures such as population are often quoted differently from one source to another. Most platforms present static text and images, so comparing groups by language family, region or population is practically impossible. The most awkward problem is provenance: after being reposted several times, the original source is usually gone, readers cannot verify anything, and researchers dare not cite it. Reorganizing these fragments into a platform that is easy to search, easy to read, traceable and usable on a phone is the problem this thesis addresses."))
B(('p', 'This thesis takes \"Exploring the Colorful World of 56 Ethnic Groups\" as its theme and implements a digital exhibition platform for ethnic culture with Spring Boot and Vue. The platform consists of a client-side portal and an administration system. The portal serves ordinary users and offers an ethnic group channel, a festival channel with a lunar-calendar converter, a traditional arts and intangible cultural heritage directory, a people column, ethnic autonomous areas, traditional ethnic sports, ethnic languages, cultural topics, full-text search, interest-based recommendation, a discussion area and a personal center. The administration system serves content editors, reviewers, content administrators and system administrators; it maintains ethnic groups, festivals, arts, topics, person profiles, autonomous areas, traditional sports, content sources, image credits and form configuration, and also provides user and role-based permission management, discussion moderation, search index and interest tag maintenance, translation glossary maintenance and a content approval workflow.'))
B(('p', 'The system follows a front-end/back-end separated B/S structure. The back end is built on Java 21 and Spring Boot 4, with Sa-Token handling authentication and permissions being checked against concrete permission points rather than hard-coded roles. Business data and workflow data are stored together in PostgreSQL 18; semi-structured fields are kept as JSONB, and Chinese fuzzy search relies on GIN indexes built with pg_trgm. Redis holds verification codes, counters and hot-data caches. The approval stage uses the embedded Camunda 7 engine, which runs in the same JVM as the application and shares the same data source, so no separate process engine service has to be maintained. The front end uses Vue 3 and TypeScript with Vite, Pinia and Element Plus; the administration system additionally integrates bpmn-js and form-js so that process diagrams and approval forms can be designed in the browser.'))
B(('p', 'As for content, the platform currently holds 56 ethnic groups, 192 festivals, 165 traditional arts and intangible cultural heritage items, 168 foods, 222 customs, 112 settlements, 163 persons, 155 ethnic autonomous areas and 20 traditional sports items. A unified index covers 1,142 documents of 8 content types, and Chinese substrings, full pinyin (mengguzu), pinyin initials (mgz) and English names are all searchable. Recommendation combines four recall channels, namely interest tags, browsing behaviour, content similarity and a popularity fallback, and it reports both the basis of a recommendation and the size of the sample behind it. Measurements show that the first full-text search call took 83 ms while repeated queries settled at 5 to 6 ms, and that read-only interfaces such as ethnic population statistics execute no SQL once the in-process cache is hit, dropping from about 30 ms to 2 ms. Testing indicates that all modules run stably, that search and recommendation behave as expected, and that the approval workflow closes its loop properly.'))
B(('p_kw', 'key words: SpringBoot; Vue; PostgreSQL; Ethnic culture digitalization; Full-text search; Personalized recommendation; Content approval workflow'))
B(('pagebreak',))
B(('toctitle', '目  录'))
B(('toc',))

# ============================================================ 第 1 章
B(('h1', '1 绪论'))
B(('h2', '1.1 课题研究背景及意义'))
B(('p', '56 个民族共同开拓了我国的疆域，也共同塑造了中华文化。各民族的风俗、节日、传统艺术、饮食以及聚居地分布，既是民族文化研究的对象，也是民族团结进步教育最直接的素材。近几年，随着中华优秀传统文化传承发展工程的推进和非遗保护工作的深入，把这些资源数字化、做成公众真正能用的产品，已经从一句口号变成了文化建设中一项具体的工作。换句话说，问题不再是「要不要做」，而是「怎么做才做得住」。'))
B(('p', '从传播现状看，民族文化内容并不缺，缺的是好用的组织方式。内容碎是最直观的问题：同一个民族的概况、风俗、节日、艺术分散在百科、新闻、短视频和自媒体文章里，想在半小时内建立起整体认识，得在好几个平台之间来回跳，而且不同来源的人口数据、语系归类经常对不上。呈现方式也单调，多数平台就是把图文排成一列，没有结构化字段，也没有民族与内容之间的跳转，用户没办法按语系、地域或者人口规模横向比一比，「随便逛逛」的体验相当有限。更要紧的是出处问题。网络内容经过多轮转载之后普遍丢了来源，普通读者无从核实，做研究的人也没法引用。对一个以文化传播为目标的系统来说，这一点是不能接受的。'))
B(('p', '本课题的价值大体分三层。工程层面，本文给出了一套相对完整的民族文化数字化平台设计与实现方案，涉及内容建模、统一检索、个性化推荐和内容审批工作流，同类文化类信息系统的开发可以直接参考。数据治理层面，平台把「内容可溯源」当成硬约束：每条内容都要能挂上权威来源，详情页逐条列出发布机构、文档名、原文链接和采集方式，来源不明的二手内容因此没有机会被当成权威资料继续传播。技术层面，本文对中文短文本的模糊检索（中文子串、拼音全拼、首字母）、词表优先的机器翻译，以及嵌入式流程引擎在中小型业务系统中的落地方式都做了实现与实测，可以供后来者借鉴。'))

B(('h2', '1.2 国内外发展现状分析'))
B(('p', '国外文化遗产数字化的起步比国内早。欧洲的 Europeana 把数千家文化机构的馆藏元数据聚合到一起，用统一的数据模型和开放接口对外提供检索服务；史密森尼学会、大英博物馆等机构则通过虚拟展厅和在线馆藏数据库发布高精度影像与编目信息。它们的共同点是数据标准先行、元数据完整、接口开放。不过，这类平台大多以「物」和「机构」为中心组织内容，像本文这样以民族为主线做聚合的门户并不多见，而这恰恰是本文想补上的那一块。'))
B(('p', '国内的进展也有目共睹。中国非物质文化遗产网、国家民委官方网站的专题栏目提供了权威的名录与民族概况资料；「数字敦煌」在影像采集、三维重建和沉浸式展示方面做得相当出色；一些高校和科研机构还建设了民族语言、民族音乐等专门数据库，各级民族博物馆与文旅部门也在陆续上线线上展厅。这些成果为本课题提供了数据基础，也提供了不少可以借鉴的经验。数据能拿到，剩下的问题就是怎么组织、怎么让人查得到。'))
B(('p', '不过，把现有平台逐个用一遍就会发现几个共性问题。一是重展示、轻检索，栏目导航做得漂亮，但输入「mengguzu」「mgz」这类大家习惯的写法就查不到东西。二是重采集、轻关联，民族、节日、艺术、人物、自治地方之间的关系基本停留在同一页罗列，跨模块的跳转和聚合统计很少。三是重发布、轻治理，内容上线缺少可回溯的审批流和版本管理，出了问题不好定位责任人。四是重内容、轻溯源，出处往往只有一句笼统的说明，做不到逐条对应。'))
B(('p', '本文实现的平台正是冲着这四点来的。统一检索索引加拼音扩展解决「查不到」，民族维度的聚合与关联推荐解决「串不起来」，Camunda 7 嵌入式审批流解决「管不住」，内容来源表加详情页「参考资料」区块解决「说不清」。还有一点需要说明：对数据缺失的情况，平台选择如实标注而不是编造填充。尚未核实署名的图片，页面会直接显示「来源待核」和核实进度；统计数字全部来自库中真实数据聚合，匹配不到的维度按零处理。这种做法在同类系统里不太常见，但放到文化类内容上其实很关键——一旦用推测值填了空缺，平台的可信度就没了。'))

B(('h2', '1.3 组织结构'))
B(('p_ind', '本文正文共分六章，后面还有结论、参考文献和附录，具体安排如下。'))
B(('p_ind', '第 1 章 绪论。说明课题的背景与意义，梳理国内外相关平台的发展情况，指出既有工作的不足，并交代全文的组织结构。'))
B(('p_ind', '第 2 章 相关技术简介。介绍实现本系统所用的主要技术，包括 Java 语言、B/S 体系结构、SpringBoot 框架、Vue 技术、PostgreSQL 数据库，以及作为补充的 Redis 缓存与 Camunda 工作流引擎。'))
B(('p_ind', '第 3 章 系统需求分析。从功能与非功能两个角度分析系统应具备的能力，划分用户角色，做技术、经济、操作三方面的可行性论证，并用用例图描述用户与系统的交互。'))
B(('p_ind', '第 4 章 系统设计。给出总体架构设计、功能结构设计、主要业务流程设计，以及数据库的概念设计与表结构设计。'))
B(('p_ind', '第 5 章 系统实现。按 C 端门户与中后台管理系统两条主线，说明各功能模块的实现过程与运行界面。'))
B(('p_ind', '第 6 章 系统测试。说明测试目的与方法，列出功能测试用例与性能实测数据，并对结果进行分析。这一章的数据都是实测的，不是估算。'))
B(('p_ind', '结论。总结本文的主要工作与成果，说明系统仍然存在的问题与后续改进方向。'))
B(('pagebreak',))

# ============================================================ 第 2 章
B(('h1', '2 相关技术简介'))
B(('h2', '2.1 Java 语言'))
B(('p', '本系统的后端代码全部用 Java 编写。选它没什么悬念：Java 是面向对象的高级程序设计语言，1995 年发布至今一直是企业级应用开发的主流选择。它把源代码编译成与平台无关的字节码，再由 Java 虚拟机负责解释执行和即时编译，从而屏蔽了底层操作系统的差异，「一次编写、到处运行」说的就是这件事。语法严谨、类型安全、有自动内存管理和完整的异常处理机制，再加上丰富的标准类库和成熟的第三方生态，大型系统的开发和维护成本就降下来了，这一点在多人协作的项目里尤其明显。'))
B(('p', '具体到本项目，后端基于 Java 21 开发，用到了这个版本引入的几项现代特性。记录类和密封类用来表达不可变的数据载体与受限的继承层次，内容模型的语义因此更清楚，写起来也少了一大堆样板代码；增强的 switch 模式匹配简化了多分支的类型判断；虚拟线程通过 spring.threads.virtual.enabled 开启之后，大量阻塞式数据库访问不再受平台线程数量限制，吞吐能力上去了，却不必引入响应式编程那套复杂度。好处是代码仍然是同步写法，维护起来不费劲。'))

B(('h2', '2.2 B/S 架构'))
B(('p', '系统为什么做成浏览器访问的形式？答案在于维护成本。B/S（Browser/Server，浏览器／服务器）架构把浏览器当作统一客户端，用户不需要安装任何东西，打开网址就能用；系统升级时只要更新服务端，不必挨个给客户端打补丁。在这一架构中，页面表现由浏览器端的 HTML、CSS 和 JavaScript 负责，业务逻辑与数据访问集中在服务端，两边通过 HTTP/HTTPS 以 JSON 等文本格式交换数据。分工算是清楚的，调试时也容易判断问题出在哪一端。相较传统的 C/S 架构，它的优势很明显：用户那边省事，运维也省事。代价则是前端得自己处理状态、路由和缓存这些原本由客户端程序承担的事情。'))
B(('p', '本系统采用的是前后端分离的 B/S 架构：前端以单页应用的形式在浏览器中运行，通过 RESTful 接口和后端通信；后端只负责业务逻辑与数据访问，不渲染页面。这样划分的好处很直接，至少有三条。前后端可以并行开发、独立部署；同一套后端接口能同时服务 Web 门户、中后台管理系统以及后续的移动端；前端的交互体验（局部刷新、路由切换、状态保持）也比服务端渲染好得多。开发阶段有个细节其实挺省事：Vite 配置了代理，把 /backend-api 前缀的请求转发到后端的 20256 端口，本地调试因此不用处理跨域问题，省下的时间不算多，但确实省心。'))

B(('h2', '2.3 SpringBoot 框架'))
B(('p', 'SpringBoot 要解决的核心问题说起来很朴素：配置太多。传统 Spring 应用需要写大量 XML 或 Java 配置来描述组件扫描、数据源、事务管理器、MVC 处理器这些基础设施，而 SpringBoot 依靠自动配置机制，根据类路径中已有的依赖推断并装配常用组件，配合 starter 依赖把功能相关的一批库打包引入，开发者只需要关心业务代码。它遵循的「约定优于配置」原则，实际上是把大量样板配置提前替你写好。代价也有：真出问题时，得先弄清楚自动配置到底替你做了哪些事。'))
B(('p', '另一个好处是内嵌容器，这一点在部署的时候特别明显。SpringBoot 内嵌 Tomcat、Jetty 等 Servlet 容器，应用可以直接打包成可执行 JAR 运行，不必单独部署 Web 容器，上线流程因此简单了很多。框架还提供基于 Profile 的多环境配置能力，通过 application.yaml 与 application-dev.yaml、application-prod.yaml 的叠加，把开发、生产环境的数据库、缓存和密钥隔离开；@ConfigurationProperties 则支持把一组相关配置绑定到类型安全的配置类上，避免 @Value 散落各处。配置集中之后，换环境只需要换一个文件。'))
B(('p', '本系统使用 SpringBoot 4 构建后端服务，用到了它的自动配置、起步依赖、内嵌容器等能力，并在此基础上自行封装了统一响应体、全局异常处理、参数校验和跨域配置等基础设施。这些东西写一次就够，后面每个模块都能直接用。'))

B(('h2', '2.4 Vue 技术'))
B(('p', '前端选 Vue，看中的就是它的组件化和上手成本，这一点在项目前期特别重要。Vue 是用于构建用户界面的渐进式 JavaScript 框架，核心特点是数据驱动的声明式渲染与组件化开发：开发者描述界面和数据之间的映射关系，数据一变，框架自动更新受影响的 DOM 节点，不需要手动操作文档对象模型。Vue 3 引入的组合式 API 允许把同一业务逻辑相关的状态、计算属性和副作用函数组织在一起，配合 <script setup> 语法糖，复杂页面的代码结构清楚了不少，逻辑复用也更容易。这一点在民族详情那种内容特别多的页面上体现得最明显。'))
B(('p', '生态方面基本不用挑：Vue Router 负责基于路由的页面切换与代码分割，Pinia 提供轻量且类型友好的状态管理，Vite 则基于原生 ES 模块做到了极速冷启动和热更新。本系统前端使用 Vue 3.5 与 TypeScript，C 端门户和中后台分别打包成独立的单页应用，互不拖累：C 端侧重内容展示与检索交互，使用 Element Plus 组件库配合自研的响应式布局；中后台侧重表格、表单与流程操作的效率，除 Element Plus 外还集成了 ECharts 做统计图表、bpmn-js 做 BPMN 流程建模、form-js 做表单设计。组件按需引入，首屏体积也压得住。'))

B(('h2', '2.5 PostgreSQL 数据库'))
B(('p', '数据库选型时比较过 MySQL 与 PostgreSQL，最后定的是 PostgreSQL 18，理由主要有三条。'))
B(('p', '第一条，JSONB 类型适合表达半结构化的文化内容。这一条看着抽象，用处却很实在。民族的语言、文字、宗教、标签，节日的习俗列表，艺术的传承人名单，这些字段的元素个数不固定，而且通常整体读写，用 JSONB 存储既保留了文档模型的灵活性，又能建立 GIN 索引参与检索，省掉了为它们额外建从表带来的连接开销，查询时也不用再拼好几张表。真要拆成从表，读写反而更麻烦。'))
B(('p', '第二条，pg_trgm 扩展能做高效的中文模糊匹配。该扩展基于三元组相似度建立 GIN 索引，使 LIKE \'%关键词%\' 这类中文子串查询以及拼音全拼、拼音首字母查询可以走索引而不是全表扫描。本系统能做到「同一个关键词、多种输入形式都能命中」，靠的就是它。'))
B(('p', '第三条，业务数据与流程引擎数据可以放在同一个数据库实例。Camunda 7 的 ACT_* 表与本系统的业务表同库共存，流程实例与业务内容之间直接用外键字段关联，部署时只需维护一个数据库，运维压力小了很多；更要紧的是，「内容状态变更」和「流程推进」能落在同一个事务边界内。'))
B(('p', '在使用细节上，系统用 set_updated_at() 触发器函数在行更新时自动维护 updated_at 字段，用 gen_random_uuid() 生成主键以支持内容离线导入与幂等迁移，并把结构变更收敛到 db/migration 下的 V1 至 V19 号迁移脚本，保证脚本可以重复执行。迁移脚本能重复跑，部署时就不必先手工核对库结构。'))

B(('h2', '2.6 Redis 缓存'))
B(('p', '缓存分两层用。第一层是 Redis：它是基于内存的高性能键值数据库，支持字符串、哈希、列表、集合和有序集合等结构，还能给键设置过期时间，天然适合承担验证码、会话、计数器和热点数据缓存的职责。本系统用 Redis 存放邮箱验证码和一次性令牌，依靠过期机制让验证码自动失效，同时用它缓存点赞、收藏、浏览等高频计数操作的中间状态，降低数据库的瞬时写入压力。一句话，能用缓存挡掉的写入就别丢给数据库。'))
B(('p', '第二层是进程内的只读目录缓存。这一层平时不显眼，压力上来之后差别就很明显了。民族人口统计、民族自治地方、人物专栏、传统体育这类接口属于「读极多、写极少」的目录数据，只有后台内容变更时才需要刷新，因此给它们设计了默认 60 秒过期、并在后台写操作时主动失效的进程内缓存。这里有个容易被忽略的细节：民族表虽然只有 56 行，但 description 字段（民族简介正文）合计约 1.1 MB，人口统计这类聚合查询如果返回完整实体，每次请求都要白白传输并反序列化上百 KB 数据。改造为只取所需短列之后，单次查询数据量从约 1792 KB 降到 6 KB，差不多是原来的三百分之一；命中缓存时更是连 SQL 都不执行，接口响应时间由 30 毫秒左右降到 2 毫秒。这一处代码改动量不大，效果却最直接。'))

B(('h2', '2.7 Camunda 工作流引擎'))
B(('p', '内容审批需要一套能说清楚「谁在哪一步做什么」的机制，BPMN 2.0 正好提供了这套语言。Camunda 是遵循该规范的开源业务流程管理平台，用开始事件、用户任务、服务任务、排他网关和结束事件等标准元素描述流程，业务人员也能看懂流程图，需求沟通和流程评审因此顺畅很多，流程图本身就成了一份能拿出来讨论的文档。'))
B(('p', '本系统使用 Camunda 7 的 Spring Boot Starter，以嵌入式方式集成流程引擎：引擎作为依赖库运行在应用自身的 JVM 中，并与业务系统复用同一个 PostgreSQL 数据源。和「独立部署流程引擎服务 + 搜索引擎」的方案相比，嵌入式方式在中小规模业务系统里优势明显：部署时只要启动应用本身，不用额外运维中间件；流程数据与业务数据同库，便于联合查询；流程的启动、任务完成和变量设置可以直接纳入应用的本地事务。代价是引擎负载和应用抢资源，所以它更适合审核、发布这类并发量不高、但对可追溯性要求很高的场景。'))
B(('p', '工程处理上还有几个坑要填。系统关闭了引擎的自动部署开关，改由应用启动时的初始化组件显式部署内置流程，避免重复部署产生多余版本；历史级别设为 audit，既能回溯各环节审批意见，又不像 full 级别那样把流程变量的每次更新都完整留痕；通过管理服务在启动后关闭引擎的匿名遥测；引擎自带的 Cockpit、Tasklist、Admin 等 Web 应用与 REST 接口一律排除，后台界面完全由本系统的中后台管理系统承担，鉴权统一走 Sa-Token。'))
B(('pagebreak',))
