# 走进多彩 56 个民族世界

一个以「中华民族 · 多元一体」为主题的文化数字化展示平台：用 Spring Boot + Vue3 全栈实现 56 个民族的风俗、节日、艺术、美食等内容的展示、检索与互动。

## 功能特性

- **C 端门户**（`/`，即仓库根）：民族 / 节日 / 艺术 / 民族团结专题的浏览、检索、双语（中/英）展示，点赞 / 收藏 / 分享 / 浏览统计
- **民族分布地图**：以离线 SVG 渲染 56 个民族的 112 处聚居地（同城多点合并为气泡），不依赖 ECharts / 在线瓦片，断网可用
- **节日日历**：把「农历正月初一」这类记载**自动换算为公历日期**并按月排布，含今日 / 未来 30 天节日
- **人口图谱**：基于 2020 年七普数据的人口 Top 榜、语系构成环形图、聚居地域分布与规模分档
- **非遗名录**（`/heritage`）：165 项非遗按级别（世界级 / 国家级）、类别、民族逐层检索，展示代表性传承人
- **人物专栏**（`/persons`）：163 位人物按领域 / 民族 / 角色检索 —— 明确区分**代表性传承人**（160 位，文旅部认定）与**历史文化名家**（4 位，如梅兰芳、王羲之）
- **民族自治地方**（`/autonomous`）：155 个民族自治地方（5 自治区 / 30 自治州 / 120 自治县·旗），支持按级别、按自治民族（44 个）、按省级行政区三种浏览方式
- **传统体育**（`/sports`）：20 个民族传统体育项目（以全国少数民族传统体育运动会 18 个竞赛项目为主体），含子项、场地、器材与相关非遗
- **内容可溯源**：每条内容在详情页展示「参考资料」——逐条列出数据出处（发布机构、文档名、原文链接、采集方式），
  共 6 个权威来源挂接全站内容；后台「系统设置 → 内容来源」可维护
- **历史沿革结构化**（民族详情「历史沿革」栏）：把 `【历史沿革】` 小节解析为
  **时间轴 / 时代分期 / 全文索引** 三种视图。⚠️ 该小节是**编年体散文**而非年表
  （仅 10/56 行的段落本身按时间排序，31% 的段落完全没有时间锚点），
  因此**只收录原文明写出具体年份的段落**，不把「13世纪初」折算成具体年份，
  并在页面上如实标注锚定比例（如「共 27 段，其中 22% 含明确年份已列入时间轴」）
- **英文正文**：`festival` / `art` / `food` 三表共 **525 行**中文正文已全部补齐英文译文，
  经站点既有的 AI 翻译通道生成并标记来源为 `machine`；英文语境下自动显示译文、无译文时回退中文，
  并提示「本页英文由机器翻译生成」。覆盖进度可用 `SELECT * FROM v_english_coverage` 查看
- **图片版权署名**：图片角标（悬停显示作者/许可/来源）与详情页「图片来源」汇总区。
  ⚠️ **注意**：原始版权元数据在采集后丢失（文件被重命名、采集清单已删除），
  574 张图片当前**全部标为「来源待核」**，页面如实显示核实进度而非编造署名；
  待核清单见 `docs/图片署名待核清单.csv`，后台「系统设置 → 图片署名」可逐张录入
- **民族语文**（`/ethnic/languages`）：按语系与文字两个维度梳理 56 个民族的语言文字（7 大语系 / 59 种语言 / 28 种文字）
- **文化专题**：**民族服饰**（`/culture/costume`，40 民族 / 57 条）与**民居建筑**（`/culture/dwelling`，14 民族 / 16 条）——
  把「风俗习惯」的文化解读与「非遗名录」的配图/级别按民族合流，支持按工艺技法与建筑形制分类筛选
- **个人中心**（`/profile`）：基本信息与头像、邮箱绑定（验证码）、昵称修改；账号密码（支持旧密码 / 邮箱验证码 / 密保问题三种方式验证后改密）；基础设置（页面字号、界面语言）；我的收藏（按类型筛选、取消收藏、分页）
- **中后台管理系统**（`admin/`）：侧边栏**按业务域分组**（工作台 / 内容运营 / 社区治理 / 系统设置）；
  覆盖民族 / 节日 / 艺术 / 专题 / 表单配置，以及方向 B 新增的三类文化资料
  —— **人物档案、自治地方、传统体育**（完整 CRUD + RBAC 权限点）。
  内容审核、用户 / 角色 / 权限管理、统计看板，以及**检索索引与兴趣标签**维护
- **内容审批工作流（Camunda 7 嵌入式引擎）**：审核员审批 → 内容上线 → 内容管理员审查 →
  有问题则暂时下线 → 内容编辑修改（可看前序全部意见）→ 二次审批 → 重新上线 → 再审查……闭环。
  **引擎内嵌在应用同一 JVM、复用业务 PostgreSQL**，生产部署**不需要**独立部署
  Zeebe / Elasticsearch 等任何流程引擎组件（详见 [工作流文档](./docs/内容审批工作流-Camunda7.md)）。
  支持网页版建模器（bpmn-js 画布 + 属性面板 + lint、Camunda Form 设计器、引擎状态与一键重部署）
- **全文检索**（`/search`）：把站内 **8 类共 1142 条内容**纳入统一索引（改造前仅 3 类 413 条），
  支持**中文子串、拼音全拼（`mengguzu`）、拼音首字母（`mgz`）、英文名**四种输入，
  带**关键词高亮**、内容类型分面过滤与命中方式提示（如「按拼音匹配」「按首字母匹配」）；
  结果按相关度排序并展示检索耗时（实测 40~55 ms）
- **个性化推荐**（`/interests`）：混合推荐引擎——**兴趣标签**（显式）+ **浏览行为**（隐式）+
  内容相似 + 热度兜底四路召回。新用户可直接勾选兴趣（民族/地域/内容类型/主题，共 111 个标签，
  全部由库中真实数据推导）解决冷启动。
  ⚠️ **如实说明**：当前站内行为样本极少（用户 5 个、行为计数 23 条），推荐以内容相似度与热度为主；
  接口会返回 `basis` / `dataNote` / `confidence` 明确告知本次推荐的**实际依据与样本量**，
  不把热度包装成「为你推荐」
- **民族频道信息密度**（`/ethnic`）：列表卡片直接显示**关联内容计数**
  （非遗 / 人物 / 自治地方 / 节日 / 美食 / 风俗 / 聚居地）与**人口排名徽标**，
  无需点进详情即可横向比较；筛选支持地域、语系、**人口分档**、**「只看有非遗 / 有代表人物 / 有自治地方」**，
  排序支持默认 / 人口升降序 / 拼音 / 名称。
  民族详情页新增「**关联信息**」区：**相关人物**（代表性传承人 / 历史文化名家，可跳人物专栏）
  与**民族自治地方**（按自治区 → 自治州 → 自治县·旗排序，可跳自治地方页），
  并在基本资料中标注**人口位次**（第 N / 总数）。
  ⚠️ 计数全部来自库中真实数据聚合（`EthnicMetricsService`），匹配不到的维度按 0 处理、不臆造
- **只读目录接口缓存**（`ReadCache`，`app.cache.*`）：民族人口统计、自治地方、人物专栏、
  传统体育等「读极多、写极少」的目录类接口走进程内缓存（默认 60 秒 TTL，后台内容变更时主动失效）。
  民族人口统计命中缓存时**不再执行任何 SQL**（实测 30ms → 2ms）。
  > ⚠️ 性能要点：`ethnic_group` 表只有 56 行，但 `description`（民族简介正文）合计约 **1.1 MB**。
  > 人口统计/列表指标这类聚合**只取所需短列**（id/name/population/language_family/region），
  > 单次查询数据量由约 **1792 KB 降到 6 KB（约 1/300）**；
  > 若误用返回完整实体的 `findAll()`，每次请求都要白白传输并反序列化上百 KB 正文。
- **移动端**（`mobile/`，可选）：React Native / Expo 浏览端

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 `backend/` | Java 21 · Spring Boot 4 · Sa-Token（鉴权） · EasyQuery（ORM，PostgreSQL） · Redis · JavaMail · Lombok · Gradle |
| 内容审批工作流 | **Camunda 7.24 嵌入式引擎**（`camunda-bpm-spring-boot-starter`，与应用同 JVM、复用业务库，无需外部集群） |
| C 端门户（仓库根） | Vue 3.5 · TypeScript · Vite 8 · Element Plus · Pinia · Vue Router · pnpm |
| 中后台 `admin/` | Vue 3 · TypeScript · Vite 6 · Element Plus · Pinia · ECharts · bpmn-js · form-js |
| 移动端 `mobile/` | React Native · Expo（可选） |
| 数据库 / 缓存 | PostgreSQL（业务数据 + 流程引擎 `ACT_*` 表） · Redis（验证码 / 计数缓存） |

## 仓库结构

```
.
├── backend/                  # Spring Boot 后端（端口 20256）
│   ├── src/main/resources/   # application.yaml(基础) / application-example.yaml(模板)
│   └── schema.sql            # 数据库结构快照（pg_dump 导出，供建表参考）
├── admin/                    # Vue3 中后台管理系统（端口 5273）
├── mobile/                   # React Native / Expo 移动端（可选）
├── src/                      # C 端门户源码（Vue3，端口 5173）
├── docs/                     # PRD / API / UI 规范 / 数据资源说明
└── README.md
```

## 快速开始

### 环境要求

- JDK 21+、Gradle（使用仓库自带 `gradlew`）
- PostgreSQL（建议 14+）与 Redis（建议 6+）
- Node.js 20+ 与 pnpm 9+
- **无需安装任何流程引擎组件**：Camunda 7 以依赖库形式内嵌，`ACT_*` 表在首次启动时自动创建

### 1. 初始化数据库

```bash
# 创建数据库（示例库名 56_app）
psql -U postgres -c "CREATE DATABASE \"56_app\";"

# 建表（结构快照；已存在的库可跳过）
psql -U postgres -d 56_app -f backend/schema.sql
```

> 增量结构变更以 `backend/src/main/resources/db/migration/` 下的脚本为准（幂等，可重复执行）。
> 内容数据（民族 / 节日 / 艺术等）需通过中后台录入，或从现有环境导入。
> 流程引擎的 `ACT_*` 表由引擎自动创建，**不需要**手工执行建表脚本
> （除非把 `camunda.bpm.database.schema-update` 设为 `false`）。

### 2. 配置后端环境变量（敏感信息不入库）

仓库中的 `application.yaml` 只包含**占位值**，不含任何真实凭据。首次运行请：

```bash
cd backend/src/main/resources
cp application-example.yaml application-dev.yaml   # 按注释填写数据库 / Redis / 邮箱 / 加密密钥
```

> `application-dev.yaml`、`application-prod.yaml` 已被 `.gitignore` 忽略，请勿提交。
> 生产环境密钥（`app.crypto.aes-key`）务必更换；若已有历史加密数据请保持密钥不变。

### 3. 启动后端（端口 20256）

```bash
cd backend
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

- 首次启动会自动初始化角色 / 权限，并用 `app.admin` 配置创建超级管理员账号。
- 首次启动会自动创建 Camunda 引擎表（`ACT_*`）并部署内置审批流程，
  日志应出现 `ENGINE-00001 Process Engine default created.` 与
  `Camunda 内置流程资源部署完成: processes=1, failures=0`。
- 邮件验证码模板：将 `backend/sendemail.html` 复制到运行目录的 `dist/sendemail.html`（后端按 `./dist/sendemail.html` 读取）。

### 4. 启动 C 端门户（端口 5173）

```bash
# 仓库根目录
pnpm install
pnpm dev                 # 访问 http://localhost:5173
```

> 开发环境已配置 Vite 代理：`/backend-api` → `http://localhost:20256`，无需改接口地址。

### 5. 启动中后台管理（端口 5273）

```bash
cd admin
pnpm install
pnpm dev                 # 访问 http://localhost:5273（使用 app.admin 配置的管理员账号登录）
```

### 6.（可选）移动端

```bash
cd mobile
pnpm install
npx expo start
```

### 生产构建

```bash
# 前端（C 端 / 中后台）
pnpm build               # C 端，产物在 dist/
cd admin && pnpm build   # 中后台，产物在 admin/dist/

# 后端可执行 jar（静态资源输出至 build/libs/static）
cd backend && ./gradlew bootJar
```

## 配置与安全说明

- **敏感信息**：数据库 / Redis / 邮箱 / AES 密钥 / 初始管理员等真实凭据只放在各环境的 `application-dev.yaml` / `application-prod.yaml`（已被 git 忽略）；远程仓库只保留 `application-example.yaml` 占位模板。
- **加密密钥**：`app.crypto.aes-key` 用于密码 / 密保答案等字段加密，生产必须使用强随机密钥；已上线数据更换密钥将导致历史数据无法解密。
- **手机短信**：验证码目前仅邮箱通道可用（手机号绑定/改密 UI 已预留，短信服务接入后放开）。
- **管理员账号**：由 `app.admin.account` / `app.admin.password` 配置在首次启动时种子化。
- **机器翻译（默认「词表优先 + AI 兜底」）**：`app.translate.provider` = `glossary`（默认，本地文化词表，词表存库、后台可维护，零依赖离线可用）/ `spring-ai`（复用 `AiConfig` 的 `translateChatClient`，模型与密钥取自 `spring.ai.openai.*`）/ `libretranslate` / `ollama`（两者需配 `APP_TRANSLATE_BASE_URL`）/ `none`（关闭，前端自动隐藏「译」按钮）。`app.translate.fallback-provider`（默认 `spring-ai`）在主通道失败**或词表覆盖率不足**（`app.translate.glossary-min-coverage`，默认 0.8）时接手，最终降级为仅返回原文、不阻塞页面。
- **AI 翻译会实时优化词表**：`spring-ai` 通道在译文末尾附一段术语表，后端切分后把民族文化术语写回 `translate_glossary`（`remark='AI 自动提取'`，只新增不覆盖、术语必须出现在原文里，可用 `APP_TRANSLATE_AUTO_GLOSSARY=false` 关闭），因此词表越用越准、越省模型调用。
- **翻译词表存库**：术语表在 `translate_glossary` 表（迁移 `V7__translate_glossary.sql` 已内置 154 条民族/节日/非遗/美食术语），后台「翻译词表」页可增删改、批量导入导出、试译预览（含覆盖率）、刷新缓存；改动最长 60 秒自动生效（保存即失效缓存）。CJK 术语按最长匹配、拉丁术语按词边界匹配，**反向方向自动推导**（只维护 zh→en 一份即可）。
- **AI 装配集中在 `AiConfig`**：通用对话 `chatClient`（`@Primary`）与翻译 `translateChatClient`（含术语表要求）系统提示词相互隔离，后续新增 AI 用途在此扩展。
- **邮件通知范围**：仅「审核结果」自动发信（`app.notify.email-enabled`，默认 `true`）；回复 / @提及 / 私信 / 关注 / 举报回执**一律仅站内**；网站维护与公告由后台「讨论区治理 → 站内公告」OA 群发按次决定是否发信（单次上限 `APP_NOTIFY_BROADCAST_MAX`，默认 2000）。开发环境的 SMTP 为真实账号，测试请使用可真实收信的邮箱，或临时把开关设为 `false`。
- **节气 / 农历换算（零依赖）**：`LunarCalendarUtil` 内置 1900–2100 年农历数据表，把「农历八月初八」这类表述换算成公历日期；
  节日日历接口（`/festivals/calendar`）对 192 条节日中的 159 条农历表述做换算（2026 年命中 101 条），
  **伊斯兰历 / 傣历 / 藏历等无法可靠换算的记录会被跳过而不猜测日期**；仅精确到月的表述按该月十五估算并在接口中标记 `dateSource=approx`，前端以虚线弱化显示。
- **管理端白名单**：`SaTokenConfigure#ADMIN_PUBLIC_PATHS` 放行 C 端需只读调用的 `/admin/**` 接口（当前仅 `/admin/view-counts`）。
  新增条目务必确认不返回敏感字段。
- **流程引擎（Camunda 7，内嵌）**：引擎与应用同 JVM、复用业务数据源，**生产无需部署任何外部引擎组件**。
  配置项集中在 `camunda.bpm.*`：`database.schema-update`（自动建表）、`job-execution.enabled`（服务任务异步作业需要）、
  `history-level: audit`（够回溯前序审批意见）、`auto-deployment-enabled: false`（改由 `WorkflowDataInitializer` 显式部署，避免重复）。
  引擎自带的 Cockpit / Tasklist / Admin webapp 与 REST API 已在 `build.gradle` 中排除（后台 UI 由本项目 admin 承担、鉴权走 Sa-Token）。
  匿名遥测由 `CamundaConfig` 在启动后调用 `ManagementService#toggleTelemetry(false)` 关闭（starter 无对应配置项）。
  > ⚠️ 迁移自 Camunda 8 的库**必须先执行 `V19__camunda7_embedded.sql`**：
  > 引擎标识由长整型变为字符串，`process_instance_key` / `current_task_key` / `camunda_task_key`
  > 三列需由 `bigint` 改为 `varchar`，否则字符串 ID 写不进去。

## 文档

`docs/` 目录包含 [PRD](./docs/PRD.md)、[API 契约](./docs/API.md)、[UI 规范](./docs/UI-SPEC.md)、
[内容审批工作流（Camunda 7）](./docs/内容审批工作流-Camunda7.md) 与[全站文档](./docs/全站文档.md)。
