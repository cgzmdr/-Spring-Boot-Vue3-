# 《走进多彩 56 个民族世界》后端接口文档（API Spec）

> 版本：v1.0
> 状态：待评审
> 关联文档：`PRD.md`
> 更新日期：2026-08-13

---

## 1. 文档说明

本文档定义「走进多彩 56 个民族世界」主题网站**后端接口契约**，作为前端联调、后端实现与测试的统一依据。内容覆盖：

- 全局约定（响应结构、错误码、鉴权、分页、语言）。
- 认证与用户体系（sa-token 登录态流转）。
- C 端公开内容接口（民族 / 节日 / 艺术 / 专题 / 搜索）。
- C 端互动接口（点赞 / 收藏 / 分享）。
- 后台管理接口（CMS + 内容审核 + 用户/角色管理，RBAC）。
- Redis 缓存设计与流式传输（SSE）约定。

---

## 2. 架构决策（对应 PRD）

### 2.1 是否登录

- **需要登录，但采用「游客可浏览 + 登录可互动」的渐进式鉴权模型**。
- 公开内容（民族、节日、艺术、专题、搜索）**无需登录**，匿名可访问。
- 以下能力需要登录：点赞、收藏云同步、个人中心、分享海报生成。
- 会话由 **sa-token** 统一管理。

### 2.2 是否后台管理

- **需要后台管理（CMS）**，承载民族/节日/艺术/专题内容的新增、编辑、发布、上下线。
- 引入 **RBAC 角色控制**，关键角色与权限见第 10 章。

### 2.3 是否 Redis

- **需要 Redis**，用于：
  1. 热点内容缓存（列表 / 详情 / 专题）。
  2. sa-token 分布式会话与权限缓存（多实例部署必需）。
  3. 点赞 / 收藏计数缓存（异步落库，防高并发击穿 DB）。
  4. 热门搜索词（ZSet 排行榜）。
  5. 接口限流 / 防刷（Rate Limit）。
  6. 用户搜索历史（按用户维度）。

### 2.4 是否流式传输

- **预留，MVP 非必需**。普通业务全部走同步 REST。
- 预留场景：AI 智能讲解 / 摘要生成、多语言实时翻译、大文件导出。
- 采用 **SSE（Server-Sent Events）** 作为流式协议，约定见第 12 章。

---

## 3. 全局约定

### 3.1 基础信息

| 项 | 值 |
| --- | --- |
| 协议 | HTTPS（生产） / HTTP（开发） |
| 域名 | 生产待定；开发 `http://localhost:8080` |
| 基础路径 | `/api/v1` |
| 数据格式 | `application/json; charset=utf-8` |
| 编码 | UTF-8 |
| 时间格式 | ISO 8601，如 `2026-08-13T10:00:00+08:00` |

### 3.2 统一响应结构

所有接口返回统一结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1786780000000
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 业务状态码，`0` 表示成功 |
| `message` | string | 提示信息 |
| `data` | object / array / null | 业务数据 |
| `timestamp` | long | 服务器时间戳（毫秒） |

### 3.3 分页约定

列表接口统一使用分页参数：

| 参数 | 类型 | 必填 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| `page` | int | 否 | 1 | 页码，从 1 开始 |
| `pageSize` | int | 否 | 20 | 每页条数，最大 100 |

分页返回结构（包裹在 `data` 中）：

```json
{
  "list": [],
  "total": 56,
  "page": 1,
  "pageSize": 20,
  "hasMore": true
}
```

### 3.4 鉴权约定（sa-token）

- 登录成功返回 `tokenName` 与 `tokenValue`。
- 前端将 `tokenValue` 持久化（`localStorage`），并在请求头携带。
- **默认请求头名**：`satoken`（sa-token 默认从 header 读取，也可配置为 `Authorization`）。
- 公开接口不校验登录；需登录接口在无 token / token 失效时返回 `1003`。

**管理端接口白名单**：C 端有少量数据在实现上委派给 `/admin/**` 下的接口，
这些路径已通过 `SaTokenConfigure#ADMIN_PUBLIC_PATHS` 排除出 Sa-Token 拦截，
使未登录访客也能读取（后台自身的 `@SaCheckPermission` 仍照常约束写操作）：

| 路径 | 用途 | 放行理由 |
| --- | --- | --- |
| `/admin/view-counts` | 批量查询浏览量 | C 端列表页展示浏览数，属公开统计；不返回任何敏感字段 |

> 新增白名单条目时，务必确认对应接口不返回敏感字段。

### 3.5 语言约定

- 全局请求头 `Accept-Language`：`zh-CN` / `en-US`。
- 或查询参数 `?lang=zh-CN`（优先级高于请求头）。
- 后端据此返回对应语言的 `name`、`description` 等文案字段。

---

## 4. 认证与用户模块（sa-token）

### 4.1 Token 流转时序

```
┌────────┐          ┌──────────────┐          ┌─────────┐          ┌────────┐
│ 前端   │          │ 网关/后端     │          │ sa-token │          │ Redis  │
└───┬────┘          └──────┬───────┘          └────┬────┘          └───┬────┘
    │  1. POST /auth/login │                      │                    │
    │─────────────────────>│                      │                    │
    │                      │ 2. 校验账号密码       │                    │
    │                      │ 3. StpUtil.login(uid)│                    │
    │                      │─────────────────────>│                    │
    │                      │                      │ 4. 生成 token + 写会话 │
    │                      │                      │───────────────────>│
    │  5. 返回 tokenName/tokenValue               │                    │
    │<─────────────────────│                      │                    │
    │                      │                      │                    │
    │  6. 后续请求携带 header: satoken            │                    │
    │─────────────────────>│                      │                    │
    │                      │ 7. StpUtil.checkLogin() 校验               │
    │                      │─────────────────────>│                    │
    │                      │                      │ 8. 从 Redis 读会话    │
    │                      │                      │───────────────────>│
    │  9. 返回业务数据      │                      │                    │
    │<─────────────────────│                      │                    │
```

### 4.2 登录

- **接口**：`POST /api/v1/auth/login`
- **鉴权**：公开
- **请求体**：
```json
{
  "account": "user@example.com",
  "password": "******"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `account` | string | 是 | 账号（邮箱 / 手机号） |
| `password` | string | 是 | 密码 |

- **响应 `data`**：

```json
{
  "tokenName": "satoken",
  "tokenValue": "a1b2c3d4...",
  "userInfo": {
    "id": "u_10001",
    "nickname": "文化爱好者",
    "avatar": "https://cdn.example.com/avatar/u_10001.png",
    "roles": ["user"]
  }
}
```

### 4.3 登出

- **接口**：`POST /api/v1/auth/logout`
- **鉴权**：需登录
- **说明**：后端调用 `StpUtil.logout()` 销毁会话。

### 4.4 获取当前用户信息

- **接口**：`GET /api/v1/auth/me`
- **鉴权**：需登录
- **响应 `data`**：同 4.2 的 `userInfo`。

### 4.5 刷新 Token

- **接口**：`POST /api/v1/auth/refresh`
- **鉴权**：需登录（携带旧 token）
- **说明**：sa-token 支持 token 自动续期（`tokenTimeout` 到期后滑动续期），也可显式刷新。

### 4.6 发送验证码（预留）

- **接口**：`POST /api/v1/auth/captcha`
- **鉴权**：公开
- **请求体**：`{ "mobile": "13800000000" }`
- **说明**：用于手机验证码登录 / 注册，配合 Redis 存储验证码与过期时间。

---

## 5. C 端公开内容接口

> 以下接口均**无需登录**，供首页、列表、详情、搜索使用。

### 5.1 民族列表

- **接口**：`GET /api/v1/ethnic-groups`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `region` | string | 否 | 地域（东北/西北/西南/中南/东南/内蒙古/其他） |
| `languageFamily` | string | 否 | 语系 |
| `populationMin` | int | 否 | 人口下限 |
| `populationMax` | int | 否 | 人口上限 |
| `keyword` | string | 否 | 名称/拼音模糊匹配 |
| `sort` | string | 否 | `population` / `pinyin` / `default` |
| `page` / `pageSize` | int | 否 | 分页 |

- **响应 `data`**：分页结构，`list` 项为民族简略对象：

```json
{
  "id": "ethnic_tibetan",
  "name": "藏族",
  "pinyin": "zangzu",
  "population": 7000000,
  "region": ["西藏", "青海", "四川", "云南", "甘肃"],
  "languageFamily": "汉藏语系",
  "summary": "生活在青藏高原的古老民族",
  "coverImage": "https://cdn.example.com/ethnic/tibetan.jpg",
  "themeColor": "#B6402E"
}
```

### 5.2 民族详情

- **接口**：`GET /api/v1/ethnic-groups/{id}`
- **路径参数**：`id`（如 `ethnic_tibetan`）
- **响应 `data`**：完整 `EthnicGroup` 对象（含 `customs` / `festivals` / `arts` / `foods` / `locations` 各维度），
  另含 `history`：由【历史沿革】小节解析出的结构化数据（方向 C-2，见 5.2.1）。

### 5.2.1 历史沿革结构化（方向 C-2）

`data.history` 由后端 `EthnicHistoryParser` 解析 `ethnic_group.description` 中
`【历史沿革】` 小节得到，**只做可验证的结构化，不推断年份**：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `timeline` | array | 时间轴。**仅收录原文明写出具体年份**（如 `1206年`）的段落 |
| `timeline[].year` | int | 原文年份；公元前为负数（如 `-221`） |
| `timeline[].yearText` | string | 年份的原文写法（`1206年` / `公元前221年`），不做归一化 |
| `timeline[].text` | string | 该段原文，未改写、未截断 |
| `timeline[].index` | int | 对应 `paragraphs` 的下标 |
| `eras` | array | 时代分期，按时间先后排序（先秦→…→近现代） |
| `eras[].paragraphIndexes` | int[] | 命中该时代的段落下标 |
| `paragraphs` | array | 全部段落（`index` / `text` / `eras` / `year`） |
| `timelineCount` / `eraCount` / `paragraphCount` | int | 各项计数 |
| `anchoredRatio` | double | 含明确年份的段落占比（0~1） |

**为什么不是「年表」**：实测 56 个民族的【历史沿革】是**编年体散文**而非年表——
仅 10/56 行的段落本身按时间排序，346/1121 段（31%）完全没有时间锚点
（如藏族的「差巴/堆穷/朗生」农奴等级、蒙古族的「畜牧业经济部门」）。
把「13世纪初」「公元3世纪」这类模糊表述强行折算成具体年份属于编造，
因此后端只收录原文写出具体年份的段落，其余段落进入 `paragraphs` 供侧栏索引；
前端在时间轴顶部**如实标注锚定比例**，并提示其余为专题叙述。

**实测覆盖**（56 行真实数据，1460 条断言全部通过）：
段落 1121、时间轴条目 420、含时间轴的民族 54/56
（普米族、乌孜别克族原文无具体年份，仅有时代分期，页面自动落到「时代分期」视图）。

### 5.3 节日列表

- **接口**：`GET /api/v1/festivals`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ethnicGroupId` | string | 否 | 所属民族 |
| `type` | string | 否 | `traditional` / `religious` / `agricultural` |
| `month` | int | 否 | 公历月份 |
| `page` / `pageSize` | int | 否 | 分页 |

### 5.4 节日详情

- **接口**：`GET /api/v1/festivals/{id}`
- **说明**：返回节日完整信息，除 `origin`（起源）外还包含 `description`（发展沿革/详细描述）。

### 5.4.0 节日日历（农历换算）

- **接口**：`GET /api/v1/festivals/calendar`
- **查询参数**：`year`（int，可选，默认当前年；支持 1900–2100）
- **鉴权**：公开
- **说明**：按**公历月份**聚合全部已发布节日，供 C 端节日日历页（`/festival/calendar`）使用。

**为什么需要这个接口**：`festival` 表中 192 条节日里只有 **33 条**填了 `solar_date`，
其余 **159 条**只记录了农历或民族历法表述（如 `农历正月初一`、`农历八月`、`伊斯兰历十月一日`）。
日历必须把它们统一到公历日期上，而农历换算无法在 SQL 中表达，故在服务端完成。

**换算实现**（`LunarCalendarUtil` + `LunarFestivalDateResolver`，**零第三方依赖**）：

| 原文形式 | 处理方式 | `dateSource` |
| --- | --- | --- |
| `solar_date` 有值（2026-03-21） | 直接采用 | `solar` |
| `农历正月初一` / `农历八月十五` / `农历五月二十九` | 查农历表精确换算 | `lunar` |
| `农历七月十二日至九月十五日`（区间） | 取**起始日** | `lunar` |
| `农历八月` / `农历七八月`（仅到月） | 按该月**十五**估算 | `approx` |
| `伊斯兰历…` / `傣历…` / `藏历…` | **不换算**，日历中略过（避免给出错误日期） | — |

> 农历数据表覆盖 1900–2100 年，已用春节 / 中秋 / 端午 / 元宵 / 腊八 / 闰月等
> 已知日期校验（2020–2026 共 26 项断言全部通过）。
> 2026 年 192 条节日中有 **101 条**成功落到日历，其余为民族历法或表述无法解析。

- **响应 `data`**：

```json
{
  "year": 2026,
  "months": [
    { "month": 9, "count": 3, "festivals": [
      { "id": "…", "name": "中秋节", "nameEn": "Mid-Autumn Festival",
        "ethnicGroupName": "汉族", "type": "traditional",
        "date": "2026-09-25", "day": 25, "lunarDate": "农历八月十五",
        "dateSource": "lunar", "daysFromToday": 10 }
    ]}
  ],
  "today": null,
  "upcoming": [ { "…": "未来 30 天内的节日，按日期升序" } ]
}
```

- 字段说明：`dateSource` = `solar`（原始公历）/ `lunar`（农历精确换算）/ `approx`（仅精确到月，估算）；
  前端对 `approx` 用虚线样式弱化，避免与确切日期混淆。`months` 固定返回 12 项（无节日的月份 `count=0`）。

### 5.4.1 民族风俗习惯详情

- **接口**：`GET /api/v1/ethnic-customs/{id}`
- **鉴权**：公开
- **响应 `data`**：

```json
{
  "id": "20000000-0000-0000-0000-000000000011",
  "ethnicGroupId": "00000000-0000-0000-0000-000000000001",
  "ethnicGroupName": "汉族",
  "category": "礼仪",
  "title": "拜年礼",
  "content": "春节走亲访友互致新年祝福……",
  "image": null,
  "orderNum": 1
}
```

### 5.4.2 民族美食详情

- **接口**：`GET /api/v1/foods/{id}`
- **鉴权**：公开
- **说明**：C 端美食详情页（`/food/:id`）数据源；`origin` 为发展沿革。
- **响应 `data`**：

```json
{
  "id": "50000000-0000-0000-0000-000000000011",
  "ethnicGroupId": "00000000-0000-0000-0000-000000000001",
  "ethnicGroupName": "汉族",
  "name": "饺子",
  "nameEn": "Dumplings",
  "description": "以面皮包裹馅料……",
  "origin": "饺子源于古代的角子……",
  "image": "/images/food/han-jiaozi.webp",
  "orderNum": 1
}
```

> 互动（点赞 / 收藏 / 浏览 / 分享）已支持 `type=food`，与民族 / 节日 / 艺术 / 专题一致。

### 5.5 艺术列表

- **接口**：`GET /api/v1/arts`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `category` | string | 否 | `music` / `dance` / `drama` / `costume` / `craft` / `architecture` |
| `ethnicGroupId` | string | 否 | 所属民族 |
| `intangibleHeritageLevel` | string | 否 | `world` / `national` / `provincial` |
| `keyword` | string | 否 | 项目名称模糊匹配 |
| `page` / `pageSize` | int | 否 | 分页 |

> 「非遗名录」页（`/heritage`）复用本接口，组合 `intangibleHeritageLevel` + `category` +
> `ethnicGroupId` + `keyword` 四个条件做名录检索。

### 5.5.1 非遗名录统计

- **接口**：`GET /api/v1/arts/heritage-stats`
- **鉴权**：公开
- **说明**：基于 `art` 表 165 项非遗项目聚合，供非遗名录页顶部概览与筛选条件（含计数）使用。
  与 5.9.2 人口统计同一策略：数据量小，内存聚合，不额外引入 SQL 分组。

```json
{
  "total": 165,
  "withInheritor": 109,
  "ethnicCount": 56,
  "levels": [
    { "code": "world", "label": "世界级", "count": 16 },
    { "code": "national", "label": "国家级", "count": 149 }
  ],
  "categories": [
    { "code": "craft", "label": "手工艺", "count": 52 },
    { "code": "music", "label": "音乐", "count": 38 }
  ],
  "spotlight": [
    { "id": "…", "name": "中国剪纸", "intangibleHeritage": "world",
      "ethnicGroupName": "汉族", "inheritors": ["张秀芳", "范祚信", "周淑英"] }
  ]
}
```

- 字段说明：`withInheritor` 为**有传承人记录**的项目数（当前 **109/165**，由 21 提升而来），
  前端据此展示传承人覆盖率；`spotlight` 为传承人记录最多的项目（按人数降序，最多 12 条）。
  `levels` 只返回实际有数据的级别 —— 当前数据库**不含省级项目**，故不出现 `provincial`。

> **传承人数据来源**：补充自文化和旅游部《国家级非物质文化遗产代表性项目代表性传承人》
> 第 1–6 批名单（共 4001 条），按项目名匹配；官方未设个人传承人的项目（多为「XX族服饰」类）
> 保持为空数组，**不编造**。详见 `docs/56民族数据与图片资源说明.md` 第 11.2 节与迁移脚本
> `V8__art_inheritors.sql` / `V9__fix_art_inheritors.sql`。

### 5.5.2 人物专栏（传承人 / 历史文化名家）

- **接口**：`GET /api/v1/persons`
- **鉴权**：公开
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 姓名**或所属非遗项目名**（如搜「唐卡」可找到两位画师） |
| `domain` | string | 否 | 音乐 / 舞蹈 / 戏剧 / 服饰 / 技艺 / 建筑 |
| `ethnic` | string | 否 | 民族名称 |
| `roleType` | string | 否 | `inheritor` 代表性传承人 / `master` 历史文化名家 |

- **说明**：人物姓名存在 `art.inheritors`（JSONB 数组），人物扩展属性存在 `person_profile`，
  两者以 **`(person_name, ethnic_group_name)`** 关联。

> **为什么关联键要带民族**：存在**同名不同族**的真实情况 ——
> 「马金山」同时是**回族花儿**与**东乡族花儿**的代表性传承人。
> 仅按姓名关联会把两个人物错误合并，因此业务键必须包含民族。

- **`roleType` 的角色区分（重要）**：数据中混有两类人物，性质不同，必须分开呈现：

| 角色 | 含义 | 数量 |
| --- | --- | --- |
| `inheritor` | 文化和旅游部认定的**国家级非遗代表性项目代表性传承人**（第 1–6 批） | 160 |
| `master` | **历史文化名家**（如梅兰芳、谭鑫培、王羲之、颜真卿）—— 在艺术史上有地位，但**并非现行认定的非遗传承人** | 4 |

> 把梅兰芳、王羲之与在世传承人混同展示会造成事实性错误。
> 该区分在**数据层**（`person_profile.role_type`）确定，前端在筛选、卡片徽标、
> 说明文案三处均明确标注。

- **响应 `data`**：

```json
{
  "summary": { "personCount": 163, "inheritorCount": 160, "masterCount": 4,
               "ethnicCount": 49, "projectCount": 109 },
  "filters": {
    "domains": [ { "value": "技艺", "label": "技艺", "count": 56 } ],
    "ethnics":  [ { "value": "侗族", "label": "侗族", "count": 8 } ],
    "roles":    [ { "value": "inheritor", "label": "代表性传承人", "count": 160 } ]
  },
  "persons": [
    {
      "name": "马金山", "ethnicGroupName": "回族", "roleType": "inheritor",
      "roleLabel": "代表性传承人", "domain": "音乐", "lifespan": null, "bio": null,
      "topLevel": "world",
      "projects": [ { "id": "…", "name": "花儿", "intangibleHeritage": "world",
                      "ethnicGroupName": "回族", "detailPath": "/art/…" } ]
    }
  ],
  "total": 164
}
```

- **口径说明**：
  - `summary.personCount` = **163**（按姓名去重），而 `total` 与 `persons` 为 **164** 条 ——
    差额来自「马金山」这一同名不同族的两个人，各自成条，属预期行为。
  - `bio` / `lifespan` **仅对确有可靠依据的人物填写**（当前仅 4 位名家）。
    其余传承人不出生卒年与生平，避免编造。
  - 默认排序：代表性传承人在前 → 世界级在前 → 按姓名。

### 5.6 艺术详情

- **接口**：`GET /api/v1/arts/{id}`

### 5.7 专题列表

- **接口**：`GET /api/v1/topics`

### 5.8 专题详情

- **接口**：`GET /api/v1/topics/{id}`

### 5.9 56 民族全家福（简略列表）

- **接口**：`GET /api/v1/ethnic-groups/all`
- **说明**：返回 56 个民族的 `id`、`name`、`themeColor`、`coverImage` 极简字段，供「全家福互动墙」使用，不分页。

### 5.9.1 民族分布地图

- **接口**：`GET /api/v1/ethnic-groups/map`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ethnicGroupId` | uuid | 否 | 只看某个民族的聚居地 |

- **说明**：以 `ethnic_location` 为主表，返回全部已发布民族的聚居地（**仅含经纬度齐全者**，约 112 条，不分页）。
  同时带上所属民族的名称 / slug / 主题色，前端据此渲染散点与气泡，无需二次请求。

```json
{
  "code": 0,
  "data": [
    {
      "id": "…", "ethnicGroupId": "…", "ethnicGroupName": "汉族",
      "ethnicGroupSlug": "han", "themeColor": "#B6402E",
      "province": "北京市", "city": "北京市",
      "longitude": 116.4074, "latitude": 39.9042,
      "description": "…"
    }
  ]
}
```

- **前端实现**：`src/components/EthnicMap.vue` 用**离线 SVG** 渲染（省级轮廓来自 `public/data/china-provinces.json`，
  等距圆柱投影 + 0.85 纵向压缩），不依赖 ECharts / Leaflet 或任何在线瓦片服务，离线与内网环境同样可用。
  同城多个民族的点位按「省 + 市 + 坐标」合并为一个气泡，气泡内数字表示该聚居地的民族数。

### 5.9.2 民族人口统计（七普口径）

- **接口**：`GET /api/v1/ethnic-groups/population-stats`
- **查询参数**：`topN`（int，默认 10，人口榜单条数）
- **说明**：基于 `ethnic_group.population`（2020 年第七次全国人口普查）做四类聚合，供首页「人口图谱」使用。
  数据量固定为 56 条，在内存中聚合，不额外引入 SQL 分组。

| 字段 | 说明 |
| --- | --- |
| `censusYear` | 统计口径年份（`2020`） |
| `totalGroups` / `totalPopulation` | 已发布民族数与人口合计 |
| `largestGroupName` / `largestGroupPopulation` | 人口最多的民族 |
| `topGroups` | 人口前 N 名（降序），每项 `{name, groupCount, population}` |
| `languageFamilies` | 按 `language_family` 聚合（降序） |
| `regions` | 按 `region`（jsonb 数组）聚合（降序） |
| `buckets` | 人口规模 5 档分布：1000 万以上 / 100 万–1000 万 / 10 万–100 万 / 1 万–10 万 / 1 万以下 |

> **口径提示**：`regions` 中一个民族可命中多个地域，会分别计入每个地域，
> 因此该维度的人口合计**大于** `totalPopulation`，属预期行为。

### 5.9.3 民族语文专栏

- **接口**：`GET /api/v1/ethnic-groups/language-atlas`
- **鉴权**：公开
- **说明**：基于各民族的 `language_family` / `languages` / `scripts` 三个字段聚合，
  供「民族语文」专栏页（`/ethnic/languages`）使用。**56 个民族这三个字段均已录入**，
  因此本接口无需新增数据表，属已有数据的重新组织。

| 字段 | 说明 |
| --- | --- |
| `summary` | 概览：`groupCount` / `languageCount` / `scriptCount` / `familyCount` |
| `families` | 语系分组：`name`（原值如「汉藏语系·藏缅语族」）、`family`（大语系）、`branch`（语族）、`groups`、`population` |
| `scripts` | 文字一览：`name`、`groups`（使用该文字的民族）、`nativeScript` |
| `groupsWithOwnScript` | 有本民族传统文字的民族数 |
| `groupsUsingChinese` | 使用汉语的民族数 |

- **`nativeScript` 的判定**：`汉字` / `阿拉伯文` / `拉丁文` / `斯拉夫文` 视为**借用或跨境通用文字**，
  标记 `false`；其余（如藏文、蒙古文、维吾尔文、东巴文）标记 `true`。前端据此分成
  「本民族传统文字」与「通用 / 借用文字」两组展示。
- **`Family.branch`**：由 `name` 按 `·` 拆分得到；若原值无 `·`（如「南岛语系」），
  `branch` 为 `null` 且 `family` 即原值。

> **口径提示**：一种语言或文字常被多个民族共同使用（当前数据中「汉字」被 49 个民族使用），
> 因此 `scripts[].groups` 的计数之和**远大于** 56，`families[].population` 亦同理。
> 本接口仅作文化展示，**非官方统计口径**；前端页面已就此显式声明。

### 5.9.4 文化专题（民族服饰 / 民居建筑）

- **接口**：`GET /api/v1/culture-topics/{topic}`
- **路径参数**：`topic` = `costume`（民族服饰）/ `dwelling`（民居建筑）；其他值返回 `1001`
- **鉴权**：公开
- **说明**：把同一主题下**分散在两张表**的内容按民族合流为一个专题：

| 来源 | 表 | 提供什么 | 局限 |
| --- | --- | --- | --- |
| `custom` | `ethnic_custom` | **文化解读**（如「苗族银饰盛装」「傣族竹楼」） | **无配图**（该表 `image` 字段全为空） |
| `art` | `art`（`category=costume` / `architecture`） | **非遗属性与专属配图**（如「瑶族服饰」国家级非遗） | 只覆盖 25 / 3 个民族 |

以**民族**为主线合流后：`custom` 提供文化解读、`art` 提供名录级别与配图，
配图优先级为「本专题下该民族的非遗配图 → 该民族其他非遗配图 → 民族封面」。

- **响应 `data`**：

```json
{
  "topic": "costume",
  "title": "民族服饰",
  "titleEn": "Ethnic Costumes",
  "intro": "服饰是民族最直观的文化标识……",
  "summary": { "groupCount": 40, "entryCount": 57, "heritageCount": 25, "withImage": 40 },
  "categories": [ { "code": "刺绣", "label": "刺绣", "count": 9 } ],
  "entries": [
    {
      "ethnicGroupId": "…", "ethnicGroupName": "哈尼族", "themeColor": "#…", "region": "云南省",
      "coverImage": "/images/art/hani-costume-u.webp",
      "items": [
        { "source": "custom", "id": "…", "title": "哈尼梯田服饰", "category": "印染",
          "content": "哈尼妇女以蓝靛染布制衣……", "intangibleHeritage": null,
          "image": null, "detailPath": "/ethnic/…/…#customs" },
        { "source": "art", "id": "…", "title": "哈尼族服饰", "category": null,
          "content": "…", "intangibleHeritage": "national", "image": "/images/art/…webp",
          "detailPath": "/art/…" }
      ]
    }
  ]
}
```

- **分类（`categories`）的口径说明**：库中 `ethnic_custom.category` 只有「服饰」「居住」「建筑」
  这类**粗分类**，不足以支撑专题内的细分浏览。因此服务端按内容关键词**推导**更贴近读者的分类：
  - **服饰**看工艺技法：织锦 / 刺绣 / 印染 / 银饰 / 编织 / 皮毛工艺 / 其他；
  - **民居**看建筑形制：干栏式 / 穹庐式 / 碉楼碉房 / 木构 / 庭院式 / 宗教建筑 / 其他。

> **实现注意（曾出过的 bug）**：分类统计与条目的 `category` 字段**必须由同一套规则产生**。
> 早期版本统计用规则推导、条目却沿用库中粗分类（「服饰」），
> 导致前端点击分类筛选时返回 0 条。现已统一到同一个 `classify()` 方法，
> 并逐项验证「chip 上的数字 == 点击后返回的条目数」。

- **实测数据**：服饰 40 民族 / 57 条 / 25 项非遗 / 40 个民族有配图；
  民居 14 民族 / 16 条 / 3 项非遗。

### 5.10.1 民族自治地方

- **接口**：`GET /api/v1/autonomous-areas`
- **鉴权**：公开
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `level` | string | 否 | `autonomous_region` 自治区 / `autonomous_prefecture` 自治州 / `autonomous_county` 自治县·旗 |
| `keyword` | string | 否 | 名称关键词 |
| `ethnic` | string | 否 | 自治民族名（如「朝鲜族」），返回该民族冠名的自治地方 |

- **说明**：数据表 `autonomous_area` 收录全国 **155 个民族自治地方** ——
  **5 个自治区 + 30 个自治州 + 120 个自治县（旗）**。
  与 `ethnic_group.region`（民族主要聚居的省级行政区，粗略线索）不同，本接口返回的是**具体行政区划实体**。

- **响应 `data`**：

```json
{
  "summary": { "total": 155, "regionCount": 5, "prefectureCount": 30, "countyCount": 120,
               "ethnicCount": 44, "provinceCount": 20 },
  "levels": [
    { "level": "autonomous_prefecture", "label": "自治州", "count": 30,
      "areas": [
        { "name": "延边朝鲜族自治州", "level": "autonomous_prefecture", "levelLabel": "自治州",
          "ethnicGroups": ["朝鲜族"], "province": "吉林省",
          "establishedYear": 1952, "seat": "延吉市" }
      ] }
  ],
  "ethnics": [
    { "ethnic": "苗族", "count": 27, "matchedName": "苗族", "matchedSlug": "miao",
      "themeColor": "#…", "areas": [ "…" ] }
  ],
  "provinces": [
    { "province": "云南省", "count": 37, "areas": [ "…" ] }
  ]
}
```

- **口径说明**：
  - **一个自治地方可冠名多个民族**，如「双江拉祜族佤族布朗族傣族自治县」（4 个民族）、
    「积石山保安族东乡族撒拉族自治县」（3 个）。这些地方在 `ethnics` 中会**分别计入每个民族**，
    因此 `ethnics[].count` 之和 **大于** 155，属预期行为。
  - 内蒙古的县级自治地方称「**自治旗**」（鄂伦春自治旗、莫力达瓦达斡尔族自治旗、鄂温克族自治旗），
    归入 `autonomous_county` 级别。
  - `establishedYear` / `seat` 在数据不可考时为 `null`，**不做推测**。
  - **与内容库的关联**：`matchedName` / `matchedSlug` / `themeColor` 是自治民族与
    `ethnic_group` 的**尽力匹配**结果 —— 当前 44 个自治民族全部可匹配到民族档案，
    前端据此提供跳转；匹配不上时为 `null`，只展示名称。

### 5.10.2 传统体育
- **接口**：`GET /api/v1/traditional-sports`
- **鉴权**：公开
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `category` | string | 否 | `ball` 球类 / `water` 水上 / `strength` 力量对抗 / `accuracy` 射击与技巧 / `speed` 竞速 / `martial` 武术 / `equestrian` 马术 / `gymnastics` 健身操 / `swing` 秋千 |
| `ethnic` | string | 否 | 起源民族名（如「壮族」），匹配 `ethnic_origins` |
| `keyword` | string | 否 | 项目名称或描述关键词 |

- **说明**：数据表 `traditional_sport` 收录 **20 个传统体育项目**，
  以**全国少数民族传统体育运动会竞赛项目**为主体（18 个竞赛项目），
  另含 2 个在库中已有线索的民族传统体育（哈萨克族「姑娘追」、仡佬族「打花龙」）。

- **数据来源（均为权威公开来源）**：
  1. 国家民委、国家体育总局《全国少数民族传统体育运动会总规程》—— 竞赛项目清单；
  2. 湖北省民族宗教事务委员会《少数民族传统体育项目》—— 项目简介。

  来源文本存于 `docs/refs/` 供核查。

- **响应 `data`**：

```json
{
  "summary": { "total": 20, "categoryCount": 9, "ethnicCount": 25,
               "withSubEvents": 4, "withHeritage": 1 },
  "sports": [
    {
      "name": "珍珠球", "category": "ball", "categoryLabel": "球类",
      "ethnicOrigins": ["满族"],
      "description": "原名「采珍珠」，来源于长白山以北、黑龙江中上游、乌苏里江流域等地的采珍珠生产活动……",
      "equipment": "珍珠球、球拍、抄网",
      "venue": "长 28 米、宽 15 米",
      "teamSize": "每队上场 7 人",
      "firstEventYear": 1991,
      "subEvents": [],
      "heritageLink": "满族珍珠球",
      "matchedEthnics": [ { "id": "…", "name": "满族", "slug": "manchu", "themeColor": "#…" } ]
    }
  ]
}
```

- **子项（`subEvents`）**：多子项项目会列出子项名称，例如
  - **民族式摔跤**（6 项）：搏克（蒙古族）、且里西（维吾尔族）、格（彝族）、北嘎（藏族）、绊跤（回族）、希日木（朝鲜族）
  - **民族马术**（5 项）：民族赛马、走马、跑马射击、跑马射箭、跑马拾哈达
  - **龙舟**：标准龙舟、小龙舟；**射弩**：立姿、跪姿

- **口径与诚实性说明**：
  - 多数项目的 `firstEventYear` 在权威来源中**未逐一给出**，因此当前仅 **珍珠球（1991 年）**
    有值 —— 该年份出自湖北省民宗委项目简介。其余留 `null`，**不做推测**。
  - `venue` / `teamSize` / `equipment` 同理：仅在有明确来源时填写。
  - 前端**不显示空缺字段**，也不以「暂无资料」之外的措辞填充。
  - `matchedEthnics` 是起源民族与 `ethnic_group` 的**尽力匹配**，用于跳转民族详情；
    当前 49 个民族引用**全部匹配成功**。

### 5.10.3 内容来源（可溯源）

- **接口**：`GET /api/v1/sources` ／ `GET /api/v1/sources/{targetType}/{targetId}`
- **鉴权**：公开
- **说明**：站内数据的权威出处（方向 C-1）。前者返回全部来源（供「数据来源」汇总），
  后者返回**某条内容**的数据出处（详情页「参考资料」区块使用）。

**`targetType` 取值**：`ethnic` / `festival` / `art` / `food` / `topic` / `person` / `area` / `sport`

- **响应 `data`**（按内容查询）：

```json
[
  {
    "id": "…",
    "name": "国家民委《中华各民族》栏目",
    "publisher": "中华人民共和国国家民族事务委员会",
    "publisherShort": "国家民委",
    "documentTitle": "《中华各民族》专题栏目（概况 / 历史沿革 / 风俗文化 / 发展现状，共 224 页）",
    "url": "https://www.neac.gov.cn/seac/ztzl/zgmzjs/index.shtml",
    "sourceType": "official",
    "sourceTypeLabel": "官方权威",
    "collectMethod": "scrape",
    "collectMethodLabel": "程序抓取",
    "remark": "民族名单、概况文本、历史沿革、风俗文化的**主来源**。…",
    "note": "概况 / 历史沿革 / 风俗文化文本"
  }
]
```

- **`note` 字段**：该来源**在该条内容上**具体提供了什么（如「2020 年人口数据」「封面图片（自由许可）」），
  仅在按内容查询时返回。这让「参考资料」不只是罗列来源，而是说明每条内容用了什么。
- **来源层级 `sourceType`**：`official` 官方权威 / `academic` 学术资料 / `open` 开放图库 / `other`
- **采集方式 `collectMethod`**：`scrape` 程序抓取 / `ocr` OCR 识别 / `manual` 人工整理 / `api` 接口获取
- 无来源的内容返回**空数组**（`code=0`），不报错 —— 前端整块不渲染。

**当前来源（6 个）与挂接范围**：

| 来源 | 层级 / 采集 | 挂接的内容 |
| --- | --- | --- |
| 国家民委《中华各民族》栏目 | 官方 / 程序抓取 | 民族 56、节日 192、艺术 165、美食 168 |
| 《中国人口普查年鉴-2020》表 2-1 | 官方 / OCR | 民族 56（人口数据） |
| 《第七次全国人口普查公报（第二号）》 | 官方 / 人工 | 人口校验依据 |
| 文化和旅游部国家级非遗代表性传承人名录 | 官方 / 人工 | 非遗项目 109、人物档案 160 |
| 国家民委、国家体育总局《全国少数民族传统体育运动会总规程》 | 官方 / 人工 | 传统体育 20 |
| Wikimedia Commons | 开放图库 / 接口 | 民族 56（封面图片） |

### 5.10.4 图片版权署名（方向 C-4）

- **接口**：`GET /api/v1/image-credits/stats` ／ `GET /api/v1/image-credits/by-paths` ／ `GET /api/v1/image-credits/{targetType}/{targetId}`
- **鉴权**：公开

| 接口 | 说明 |
| --- | --- |
| `/image-credits/stats` | 署名核实进度：总数、已核实、待核、合规缺口、完成率 |
| `/image-credits/by-paths?paths=a,b,c` | 按图片路径批量查询（图集页用，逗号分隔；不存在的路径被忽略） |
| `/image-credits/{targetType}/{targetId}` | 按内容查询（详情页「图片来源」汇总区） |

- **`creditStatus` 三态**：

| 值 | 含义 | 说明 |
| --- | --- | --- |
| `verified` | 已核实 | 有作者与许可，`creditLine` 返回可直接展示的署名文本 |
| `unverified` | 来源待核 | **当前 574 张全部为此状态**；`creditLine` 为 `null` |
| `original` | 原创 / 无需署名 | — |

> **诚实性说明（重要）**：图片采集后被重命名为 `cover.webp`，采集清单目录已删除、
> `media_asset` 表为空，**原始 Commons 文件名与许可元数据已丢失**，无法对应回来源页。
> 因此本接口**不返回编造的署名** —— 未核实图片的 `creditLine` 恒为 `null`，
> 由前端显示「来源待核」。这比填一个假作者更诚实，也让合规缺口可见、可追踪。

- **响应示例**：

```json
{
  "imagePath": "/images/ethnic/miao/cover.webp",
  "caption": "苗族、芦笙节 等 · 配图",
  "creditStatus": "unverified",
  "creditStatusLabel": "来源待核",
  "author": null, "license": null, "licenseUrl": null, "sourceUrl": null,
  "sourceSite": "Wikimedia Commons（待核）",
  "attributionRequired": true,
  "remark": "原采集自 Wikimedia Commons 民族分类（自由许可），但原始文件名与许可信息已丢失，需用反向图片搜索逐张核对来源页。",
  "creditLine": null
}
```

### 5.10 搜索

- **接口**：`GET /api/v1/search`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `q` | string | 是 | 关键词（支持拼音，如 `zangzu`） |
| `type` | string | 否 | 限定类型：`ethnic` / `festival` / `art` / `all` |
| `page` / `pageSize` | int | 否 | 分页 |

- **响应 `data`**：按类型分组：

```json
{
  "ethnic": { "list": [], "total": 1 },
  "festival": { "list": [], "total": 3 },
  "art": { "list": [], "total": 2 }
}
```

- **命中范围**：民族按 `name` / `pinyin` / `nameEn` / **`tags`（标签数组，jsonb 转文本后模糊匹配）** 命中，节日与艺术按名称（中/英）命中。
  民族详情页「基本资料 → 标签」点击即跳转 `/search?q=标签&from=tag`，由标签命中返回该民族等结果；`from=tag` 仅用于前端展示「标签来源」提示。

### 5.11 热门搜索词

- **接口**：`GET /api/v1/search/hot`
- **说明**：返回 Redis ZSet 排行榜前 N 个热门词。

### 5.11.1 统一全文检索（方向 D）

- **接口**：`GET /api/v1/search/full`

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `q` | string | 否 | 检索词；**为空时按热度返回全部内容** |
| `type` | string | 否 | `ethnic` / `festival` / `art` / `food` / `custom` / `person` / `area` / `sport`，`all` 不限 |
| `ethnic` | string | 否 | 按所属民族精确筛选 |
| `page` / `size` | int | 否 | 分页（`size` 上限 50） |

**响应 `data`**

| 字段 | 说明 |
| --- | --- |
| `keyword` / `total` / `tookMs` | 检索词、命中总数、**检索耗时（毫秒，前台如实展示）** |
| `list[]` | 统一结果流，按相关度降序 |
| `list[].titleHtml` / `summaryHtml` | **关键词已高亮**（`<em class="hl">`），原文已做 HTML 转义 |
| `list[].matchBy` | 命中方式：`title` / `titleEn` / `pinyin` / `abbr` / `body` |
| `list[].score` | 相关度得分（便于核对排序依据） |
| `facets` | 分面计数：内容类型 → 命中数 |
| `highlights` | 命中来源分布 |

**支持的输入形式**（实测均可用）：

| 输入 | 示例 | 命中方式 |
| --- | --- | --- |
| 中文精确 / 子串 | `蒙古族` / `蒙古` | `title` |
| 拼音全拼 | `mengguzu` / `menggu` | `pinyin` |
| 拼音首字母 | `mgz` | `abbr` |
| 英文名 | `mongolian` | `titleEn` |

**实现要点（为什么这样设计）**

- **技术选型**：本机**没有 Docker/Podman，也没有 Elasticsearch/Meilisearch 在运行**，
  而 PostgreSQL 已装 `pg_trgm`；因此采用 **PostgreSQL 原生方案**（trigram + 子串 + 拼音），
  不引入新的外部依赖。
- **不单纯依赖 trigram**：实测 `similarity('维吾尔族','维吾尔')=0.5`，
  而 `蒙古` 这类两字输入对其余民族的相似度**普遍为 0**（默认阈值 0.3 直接漏召回）。
  故检索采**三路并用**：子串匹配（中文主力）+ 拼音（全拼/首字母）+ trigram（错别字补充）。
- **打分权重**（保证「标题精确 > 标题子串 > 拼音精确 > 拼音前缀 > 首字母 > 正文」）：
  标题完全相等 100 / 标题前缀 60 / 标题子串 40；
  英文完全相等 45 / 英文子串 35；拼音精确 55 / 前缀 40 / 包含 30；首字母精确 20；
  民族名命中 18；trigram 相似度 ×25；摘要 12 / 正文 10；
  热度 `min(popularity,100)×0.08`；时效最多 +4。
- **首字母权重刻意压低**：实测 56 个民族中有 **6 组首字母冲突**
  （`hz`=汉族/回族、`mz`=苗族/满族、`zz`=藏族/壮族…），首字母不能作为唯一判据。
- **索引**：`search_document` 宽表（8 类内容聚合），详见 §11.9；内容变更后调用
  `POST /admin/search-index/rebuild` 即可让新内容立即可搜。

### 5.11.2 兴趣标签与个性化推荐（方向 D）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/interests/tags` | 公开 | 兴趣标签字典，按维度分组：`ethnic` / `region` / `type` / `topic` |
| GET | `/interests/mine` | 登录 | 我选中的兴趣标签；未登录返回空数组 |
| POST | `/interests/mine` | 登录 | **覆盖式**保存兴趣（body `{tagIds:[...]}`，空数组表示清空） |
| GET | `/recommend` | 公开 | 个性化推荐（游客可用） |
| POST | `/behaviors/view` | 登录 | 上报浏览行为（隐式信号）；**未登录静默忽略** |

**`GET /recommend` 参数**：`size`（1~30）、`excludeType`（排除某类型，详情页推荐用）。

**响应 `data` —— 重点在「诚实性字段」**

| 字段 | 说明 |
| --- | --- |
| `list[]` | 推荐条目，含 `reason`（推荐理由，如「你关注了藏族」「与你浏览过的内容相关」） |
| `basis` | **本次推荐的实际依据**：`personalized` / `interest` / `popularity` |
| `basisLabel` | 依据中文说明，前台直接展示 |
| `dataNote` | **如实告知当前可用样本量**（如「当前仅有 5 条行为记录、2 个兴趣标签，样本较少…」） |
| `confidence` | 画像可信度 0~1（行为越少越低） |
| `behaviorCount` / `interestCount` | 参与计算的行为条数与兴趣数 |

**四路召回 + 加权融合**（按可用信号自动降级）：

1. **兴趣标签**（显式）——冷启动主解法：民族 +50 / 地域 +25 / 主题 +20 / 类型 +10；
2. **行为协同**（隐式）——从浏览·点赞·收藏提取兴趣民族（+30）与兴趣分类（+12），并**排除已读**；
3. **内容相似**——以民族/分类为特征；
4. **热度兜底**——`min(popularity,100)×0.1` + 时效加权 + 民族类 +3。

**为什么不做经典 User-CF**：实测全站仅 **5 个用户**，行为共 `view_counter` 23 / `favorite` 3 / `like_record` 6 条，
用户-物品矩阵极度稀疏，任意两用户的共同物品几乎为 0，相似度不可用。
因此「协同」是基于**内容特征**的，并在接口中标注 `confidence` 与 `dataNote`。

### 5.12 站点地图（SEO）

- **接口**：`GET /api/v1/sitemap.xml`
- **说明**：返回 XML 格式站点地图，供搜索引擎抓取。

---

## 6. C 端互动接口（需登录）

> 统一使用内容类型标识：`ethnic` / `festival` / `art` / `topic`。

### 6.1 点赞

- **接口**：`POST /api/v1/contents/{type}/{id}/like`
- **鉴权**：需登录
- **响应**：`{ "liked": true, "likeCount": 123 }`

### 6.2 取消点赞

- **接口**：`DELETE /api/v1/contents/{type}/{id}/like`
- **鉴权**：需登录

### 6.3 收藏

- **接口**：`POST /api/v1/contents/{type}/{id}/favorite`
- **鉴权**：需登录

### 6.4 取消收藏

- **接口**：`DELETE /api/v1/contents/{type}/{id}/favorite`
- **鉴权**：需登录

### 6.5 我的收藏列表

- **接口**：`GET /api/v1/me/favorites`
- **鉴权**：需登录
- **查询参数**：`type`（可选）、`page` / `pageSize`

### 6.6 互动统计

- **接口**：`GET /api/v1/contents/{type}/{id}/stats`
- **鉴权**：公开
- **响应**：`{ "likeCount": 123, "favoriteCount": 45 }`

### 6.7 生成分享链接 / 海报

- **接口**：`POST /api/v1/share`
- **鉴权**：需登录
- **请求体**：`{ "type": "ethnic", "id": "ethnic_tibetan" }`
- **响应**：`{ "shareUrl": "...", "posterUrl": "..." }`（海报生成能力 P2）

---

## 7. 后台管理接口（CMS，需登录 + RBAC）

> 全部需登录，且需具备对应角色/权限（见第 10 章）。基础路径：`/api/v1/admin`。

### 7.1 民族管理

| 接口 | 方法 | 权限 | 说明 |
| --- | --- | --- | --- |
| `/admin/ethnic-groups` | POST | `ethnic:create` | 新增民族 |
| `/admin/ethnic-groups/{id}` | PUT | `ethnic:update` | 编辑民族 |
| `/admin/ethnic-groups/{id}` | DELETE | `ethnic:delete` | 删除民族 |
| `/admin/ethnic-groups` | GET | `ethnic:list` | 后台列表（含草稿/下架） |
| `/admin/ethnic-groups/{id}` | GET | `ethnic:view` | 后台详情 |

### 7.2 节日管理

| 接口 | 方法 | 权限 |
| --- | --- | --- |
| `/admin/festivals` | POST | `festival:create` |
| `/admin/festivals/{id}` | PUT | `festival:update` |
| `/admin/festivals/{id}` | DELETE | `festival:delete` |
| `/admin/festivals` | GET | `festival:list` |

### 7.3 艺术管理

| 接口 | 方法 | 权限 |
| --- | --- | --- |
| `/admin/arts` | POST | `art:create` |
| `/admin/arts/{id}` | PUT | `art:update` |
| `/admin/arts/{id}` | DELETE | `art:delete` |
| `/admin/arts` | GET | `art:list` |

### 7.4 专题管理

| 接口 | 方法 | 权限 |
| --- | --- | --- |
| `/admin/topics` | POST | `topic:create` |
| `/admin/topics/{id}` | PUT | `topic:update` |
| `/admin/topics/{id}` | DELETE | `topic:delete` |
| `/admin/topics` | GET | `topic:list` |

### 7.5 内容审核

| 接口 | 方法 | 权限 | 说明 |
| --- | --- | --- | --- |
| `/admin/reviews` | GET | `review:list` | 待审内容列表 |
| `/admin/reviews/{id}/approve` | POST | `review:approve` | 通过 |
| `/admin/reviews/{id}/reject` | POST | `review:reject` | 驳回（可附原因） |

### 7.6 用户与角色管理

| 接口 | 方法 | 权限 |
| --- | --- | --- |
| `/admin/users` | GET | `user:list` |
| `/admin/users/{id}` | GET | `user:view` |
| `/admin/users/{id}/roles` | PUT | `user:assignRole` |
| `/admin/roles` | GET | `role:list` |
| `/admin/roles` | POST | `role:create` |
| `/admin/roles/{id}/permissions` | PUT | `role:assignPermission` |

### 7.7 统计报表

| 接口 | 方法 | 权限 | 说明 |
| --- | --- | --- | --- |
| `/admin/stats/overview` | GET | `stats:view` | 访问 / 互动总览 |
| `/admin/stats/content` | GET | `stats:view` | 内容维度统计 |

### 7.8 B 系列文化资料（方向 B 新增内容的维护）

人物档案 / 民族自治地方 / 传统体育此前只有 C 端展示接口，后台不可维护。本节补齐完整 CRUD。

| 资源 | 接口 | 方法 | 权限 |
| --- | --- | --- | --- |
| 人物档案 | `/admin/persons` | GET（列表，支持 `keyword` / `roleType` / `domain`） | `person:list` |
| | `/admin/persons/{id}` | GET | `person:list` |
| | `/admin/persons` | POST | `person:create` |
| | `/admin/persons/{id}` | PUT | `person:update` |
| | `/admin/persons/{id}` | DELETE | `person:delete` |
| 自治地方 | `/admin/areas` | GET（列表，支持 `keyword` / `level` / `province`） | `area:list` |
| | `/admin/areas/{id}` | GET | `area:list` |
| | `/admin/areas` | POST | `area:create` |
| | `/admin/areas/{id}` | PUT | `area:update` |
| | `/admin/areas/{id}` | DELETE | `area:delete` |
| 传统体育 | `/admin/sports` | GET（列表，支持 `keyword` / `category`） | `sport:list` |
| | `/admin/sports/{id}` | GET | `sport:list` |
| | `/admin/sports` | POST | `sport:create` |
| | `/admin/sports/{id}` | PUT | `sport:update` |
| | `/admin/sports/{id}` | DELETE | `sport:delete` |

**权限分配**：`content_admin` 获得全部 12 个权限点；`editor` 获得只读 + 编辑（无增删）。

**唯一性约束**（服务层显式校验，冲突返回 `2001` 而非 500）：

| 资源 | 业务唯一键 |
| --- | --- |
| 人物档案 | **姓名 + 民族**（同名不同族允许，如「马金山」同属回族花儿与东乡族花儿） |
| 自治地方 | 名称 |
| 传统体育 | 名称 |

> **实现要点（两个曾出错的点，已修复并回归验证）**：
> 1. **jsonb 数组入参**：`autonomous_area.ethnic_groups`、`traditional_sport.ethnic_origins` /
>    `sub_events` 是 jsonb 列，实体中以 **JSON 文本（String）** 承载。
>    若直接用实体接收请求体，前端传来的 JSON 数组无法写入 String 字段，会报参数错误（`1001`）。
>    因此这两类资源改用 **DTO**（`AutonomousAreaSaveRequest` / `TraditionalSportSaveRequest`，
>    字段为 `List<String>`），由服务层序列化后写入实体 —— 与 `DiscussionServiceImpl` 处理
>    `images` 数组的做法一致。
> 2. **物理删除**：EasyQuery 默认禁止物理删除，需在删除链路上显式调用
>    `.allowDeleteStatement(true)`，否则报 5000。

### 7.9 内容来源（方向 C-1）

| 接口 | 方法 | 权限 | 说明 |
| --- | --- | --- | --- |
| `/admin/sources` | GET | `source:list` | 列表，支持 `keyword` / `sourceType` |
| `/admin/sources/{id}` | GET | `source:list` | 详情 |
| `/admin/sources` | POST | `source:create` | 新增（名称唯一，冲突返回 `2001`） |
| `/admin/sources/{id}` | PUT | `source:update` | 编辑 |
| `/admin/sources/{id}` | DELETE | `source:delete` | 删除（**级联清理内容关联**） |
| `/admin/sources/{id}/usage` | GET | `source:list` | 该来源被多少条内容引用（删除前评估影响面） |

**权限分配**：`content_admin` 全部 4 项；`editor` 只读 + 编辑。

> **设计说明**：删除来源会连带解除它与所有内容（可能上千条）的关联，
> 因此提供 `usage` 接口让后台在删除前提示影响面，前端确认框会显示引用数。
> 数据表上同时有 `ON DELETE CASCADE`，服务层也显式删除关联以便统计与记录。

### 7.10 图片署名（方向 C-4）

| 接口 | 方法 | 权限 | 说明 |
| --- | --- | --- | --- |
| `/admin/image-credits` | GET | `credit:list` | 列表，支持 `keyword` / `status` |
| `/admin/image-credits/{id}` | GET | `credit:list` | 详情 |
| `/admin/image-credits/{id}` | PUT | `credit:update` | 核实 / 编辑署名 |

> **防「假核实」约束**：把 `creditStatus` 置为 `verified` 时，
> **必须至少填写 `author` 或 `license`**，否则返回 `1001`。
> 否则会出现「状态是已核实、页面却显示不出署名」的空壳记录，
> 把待核伪装成已完成 —— 这正是本轮要避免的。

---

## 8. 数据模型（后端实体）

### 8.1 民族 `EthnicGroup`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 主键 |
| `name` | string | 民族名 |
| `selfName` | string | 民族语自称 |
| `pinyin` | string | 拼音 |
| `population` | long | 人口 |
| `region` | string[] | 主要聚居地 |
| `languageFamily` | string | 语系 |
| `languages` | string[] | 语言 |
| `scripts` | string[] | 文字 |
| `religion` | string[] | 宗教 |
| `summary` | string | 一句话简介 |
| `description` | string | 详细介绍 |
| `coverImage` | string | 封面图 |
| `themeColor` | string | 主题色 |
| `tags` | string[] | 标签 |
| `customs` / `festivals` / `arts` / `foods` / `locations` | object[] | 六大维度子对象 |
| `status` | string | 内容状态：`draft` / `pending` / `published` / `offline` |

### 8.2 节日 `Festival`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 主键 |
| `name` | string | 节日名 |
| `ethnicGroupId` | string | 所属民族 |
| `type` | string | `traditional` / `religious` / `agricultural` |
| `solarDate` | string | 公历 |
| `lunarDate` | string | 农历 |
| `origin` | string | 起源 |
| `customs` | string[] | 习俗活动 |
| `images` | string[] | 图集 |

### 8.3 艺术 `Art`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 主键 |
| `name` | string | 艺术名 |
| `category` | string | 类别 |
| `ethnicGroupId` | string | 所属民族 |
| `description` | string | 介绍 |
| `intangibleHeritageLevel` | string | 非遗级别 |
| `inheritors` | string[] | 传承人 |
| `media` | object[] | 影像资料 |

### 8.4 专题 `Topic`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 主键 |
| `title` | string | 标题 |
| `subtitle` | string | 副标题 |
| `coverImage` | string | 封面 |
| `entryIds` | string[] | 关联条目 |
| `order` | int | 排序 |

### 8.5 用户 `User`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 主键 |
| `account` | string | 登录账号 |
| `passwordHash` | string | 密码摘要 |
| `nickname` | string | 昵称 |
| `avatar` | string | 头像 |
| `roles` | string[] | 角色 |
| `status` | string | `active` / `disabled` |

---

## 9. Redis 使用设计

| Key 模式 | 类型 | 用途 | TTL |
| --- | --- | --- | --- |
| `sa-token:login:{token}` | string | sa-token 会话 | 30min（滑动续期） |
| `cache:ethnic:{id}` | string/json | 民族详情缓存 | 1h |
| `cache:ethnic:list:{hash}` | string/json | 民族列表缓存（按筛选参数哈希） | 10min |
| `cache:festival:{id}` / `cache:art:{id}` | string/json | 节日/艺术详情缓存 | 1h |
| `like:count:{type}:{id}` | string | 点赞计数 | 永久（异步落库） |
| `favorite:user:{userId}` | set | 用户收藏集合 | 永久 |
| `search:hot` | zset | 热门搜索词排行 | 永久 |
| `search:history:{userId}` | list | 用户搜索历史 | 7d |
| `rate:limit:{api}:{ip}` | string | 接口限流计数 | 1min |

**缓存一致性策略**：
- 内容变更（后台发布/更新/下架）时，主动删除对应缓存（Cache Aside）。
- 点赞/收藏计数采用「先更新 Redis，异步消息落库」，保证高并发下不击穿 DB。

---

## 10. 角色权限矩阵（RBAC）

| 角色 | 权限范围 | 典型权限标识 |
| --- | --- | --- |
| `super_admin` 超级管理员 | 全部 | `*` |
| `content_admin` 内容管理员 | 内容增删改查 + 发布 + 专题 | `ethnic:*`, `festival:*`, `art:*`, `topic:*` |
| `editor` 内容编辑 | 内容新增/编辑，提交审核 | `ethnic:create/update`, `festival:create/update`, `art:create/update` |
| `reviewer` 审核员 | 内容审核 | `review:list/approve/reject` |
| `operator` 运营 | 专题、热门词、报表查看 | `topic:update`, `search:hot:update`, `stats:view` |
| `user` 普通用户 | 浏览 + 互动 | `content:like/favorite`, `share:create` |

**社区（讨论区）追加权限点**（见 §11.1～§11.5）：

| 权限标识 | 说明 | 默认授予角色 |
| --- | --- | --- |
| `discussion:review` | 讨论区审核与举报处理 | `reviewer`, `operator`（`super_admin` 全量） |
| `discussion:user:mute` | 讨论区用户禁言 | `super_admin` |
| `discussion:board` | 讨论区板块管理 | `super_admin` |
| `system:broadcast` | 站内公告群发（OA） | `super_admin` |
| `translate:glossary` | 翻译词表维护（见 §11.6） | `super_admin`, `content_admin` |

**鉴权方式**：后端使用 sa-token 注解/API：
- `StpUtil.checkLogin()` —— 校验登录态。
- `StpUtil.checkRole("editor")` —— 校验角色。
- `StpUtil.checkPermission("ethnic:create")` —— 校验权限。

---

## 11. 错误码表

| code | 说明 | HTTP 状态 |
| --- | --- | --- |
| 0 | 成功 | 200 |
| 1001 | 参数错误 | 400 |
| 1002 | 资源不存在 | 404 |
| 1003 | 未登录或 token 失效 | 401 |
| 1004 | 无权限（角色/权限不足） | 403 |
| 1005 | token 已过期 | 401 |
| 1006 | 登录失败（账号或密码错误） | 401 |
| 1007 | 账号被禁用 | 403 |
| 1008 | 请求过于频繁（限流） | 429 |
| 1009 | 验证码错误或过期 | 400 |
| 2001 | 内容已存在（重复创建） | 409 |
| 2002 | 重复操作（重复点赞/收藏） | 409 |
| 5000 | 服务器内部错误 | 500 |

---

## 11.1 讨论区 / 社区（迭代 1 已实现）

> 建表脚本：`backend/src/main/resources/db/migration/V2__discussion.sql`（幂等，可重复执行）
> 发帖策略：**先发后审**。命中 block 级敏感词或板块配置 `review_then_post` 的内容进入 `pending`（仅作者与管理员可见），并写入既有 `content_review` 审核队列。

### 11.1.1 C 端公开接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/discussion/boards` | 板块列表 |
| GET | `/api/v1/discussion/topics` | 帖子列表：`boardId` / `keyword` / `sort=latest\|hot\|featured` / `linkedType`+`linkedId` / `authorId` / `page` / `size` |
| GET | `/api/v1/discussion/topics/linked` | 内容关联讨论（内容详情页底部讨论栏），参数 `linkedType`、`linkedId`、`size` |
| GET | `/api/v1/discussion/topics/{id}` | 帖子详情（作者可见自己的待审/驳回内容，并返回 `reviewNote`） |
| POST | `/api/v1/discussion/topics` | 发帖（登录） |
| PUT | `/api/v1/discussion/topics/{id}` | 编辑（仅作者） |
| DELETE | `/api/v1/discussion/topics/{id}` | 删除（软删，作者或治理员） |
| GET | `/api/v1/discussion/topics/{id}/posts` | 楼层列表（1 楼为楼主首帖） |
| POST | `/api/v1/discussion/topics/{id}/posts` | 回复（登录；锁定后拒绝） |
| DELETE | `/api/v1/discussion/posts/{id}` | 删除回复（软删） |
| POST | `/api/v1/discussion/reports` | 举报（登录；同内容不可重复举报，不能举报自己） |
| POST | `/api/v1/uploads/image` | 上传帖子配图（登录，≤5MB，JPG/PNG/WebP/GIF，含魔数校验）→ 返回 `/uploads/yyyyMM/xxx.jpg` |

### 11.1.2 我的社区数据（登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/me/discussion-topics` | 我的发帖 |
| GET | `/api/v1/me/discussion-posts` | 我的回复 |
| GET | `/api/v1/me/discussion-reports` | 我的举报（含处理进度） |
| GET | `/api/v1/me/notifications` | 站内通知（`unreadOnly`、分页）；`type=system` 的公告会带 `link` 字段用于站内跳转 |
| GET | `/api/v1/me/notifications/unread-count` | 未读角标 |
| POST | `/api/v1/me/notifications/read` | 标记已读（body 为空数组表示全部已读） |

### 11.1.3 治理接口（后台，权限点见 §10）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/discussion/reviews` | `discussion:review` | 待审队列：`targetType=discussion_topic\|discussion_post`、`status=pending\|watch\|all` |
| POST | `/admin/discussion/reviews` | `discussion:review` | 审核结论（approve/reject + 理由，通知作者并写 `content_review`） |
| GET | `/admin/discussion/reports` | `discussion:review` | 举报工作台 |
| POST | `/admin/discussion/reports/{id}` | `discussion:review` | 处理举报（可同时隐藏/删除内容并对作者禁言，回执举报人） |
| POST | `/admin/discussion/topics/{id}/flag` | `discussion:review` | 置顶 / 精华 / 锁定（`flag=pinned\|featured\|locked`） |
| POST | `/admin/discussion/content/hide` | `discussion:review` | 直接隐藏内容 |
| POST | `/admin/discussion/users/{id}/mute` | `discussion:user:mute` | 禁言 / 解除禁言（`days<=0` 解禁） |
| GET/POST/DELETE | `/admin/discussion/words[/{id}]` | `discussion:review` | 本地敏感词维护（block 进待审 / watch 打标 / replace 替换） |
| GET | `/admin/discussion/stats` | `discussion:review` | 社区看板 |

### 11.1.4 复用与约定

- **互动**：点赞/收藏/浏览/分享复用 `/contents/{type}/{id}/...`，新增类型 `discussion_topic`、`discussion_post`；点赞数与浏览量会回写到帖子/楼层行，便于列表展示与热度排序。
- **@提及**：内容中的 `@昵称`（昵称支持中英文、数字、`_`、`-`、`·`，最长 32）会被解析；命中真实用户即写入 `notification(type=mention)`，单条内容最多解析 10 个；自己 @ 自己不通知。前端对 `@昵称` 做高亮。
- **@提及联想**：回复框 / 发帖页输入「@」即调用 `GET /api/v1/discussion/users/suggest` 提示候选（默认 10 条，最多 20），插入固定为 `@昵称 `（含末尾空格）；昵称含空格 / emoji 的用户无法被解析，因此不出现在候选中。候选 = 本页已参与用户（楼主与楼层作者，前端本地即时）+ 服务端按昵称联想（无人称关键字时返回「我关注的人」，不足部分用高信任等级用户补齐）。
- **内容页联动**：`linkedType`（ethnic/festival/art/food/topic）+ `linkedId` 把帖子与内容绑定；内容详情页底部「相关讨论」区块复用 `/discussion/topics/linked`，`/discussion/new` 携带 `linkedType/linkedId/linkedLabel` 即自动带上关联内容。
- **个人中心**：`/api/v1/me/discussion-topics`、`/me/discussion-posts`、`/me/discussion-reports` 供「我的社区」页展示（含待审/驳回/隐藏状态与举报处理进度）。
- **通知**：`notification` 表统一承载站内消息；邮件通道由 `app.notify.email-enabled` 控制（默认关闭），开启后异步发送。
- **限流**：Redis 计数窗口 —— 发帖 3 次/分、回复 8 次/分、举报 20 次/天、上传 30 次/时；超限返回 `1008`。
- **状态机**：`published`（正常）→ `pending`（待审，仅作者/治理员可见）→ `published`/`rejected`（驳回附理由并通知作者）；`hidden`（举报成立或后台隐藏）、`deleted`（软删）。
- **附件**：迭代 1 仅图片，存 `app.upload.location`（默认 `./uploads`），经 `/uploads/**` 静态映射对外提供；文件名为 UUID，按月分目录。

---

## 11.2 社区社交：关注 / 粉丝 / 私信（迭代 2 已实现）

> 建表脚本：`backend/src/main/resources/db/migration/V3__discussion_social.sql`（幂等）
> 产品决议：**仅「互相关注」的用户之间可发起私信**（`discussion_conversation` + `discussion_message`），避免陌生人骚扰。

### 11.2.1 关注与用户主页

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/discussion/users/{id}` | 可选 | 社区用户主页：昵称/头像/简介/加入时间 + 关注数/粉丝数/发帖数 + `followed`/`mutual`/`self` |
| POST | `/api/v1/discussion/users/{id}/follow` | 登录 | 关注（返回 `true`）；不能关注自己（`1001`） |
| DELETE | `/api/v1/discussion/users/{id}/follow` | 登录 | 取消关注（返回 `false`） |
| GET | `/api/v1/discussion/users/{id}/follows` | 可选 | `type=following` 关注的人 / `type=followers` 粉丝，分页 |
| GET | `/api/v1/discussion/users/mutual` | 登录 | 我的好友（互相关注）ID 列表 |
| GET | `/api/v1/discussion/users/suggest` | 可选 | **@提及联想**：`keyword`（昵称关键字，可空）+ `limit`（默认 10，最大 20），返回 `id` / `nickname` / `avatar` / `bio`；关键字为空时优先返回我关注的人（未登录则返回信任等级高的用户） |
| GET | `/api/v1/discussion/topics/following` | 登录 | **关注流**：我关注的用户发布的帖子（拉模式时间线，分页） |

### 11.2.2 私信

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/me/conversations/with/{userId}` | 获取或创建与某用户的会话；**非互关返回 `1004`** |
| GET | `/api/v1/me/conversations` | 会话列表（对方资料、最后消息摘要、是否我发出、我的未读数） |
| GET | `/api/v1/me/conversations/{id}` | 会话详情（仅参与者可见，否则 `1002`） |
| GET | `/api/v1/me/conversations/{id}/messages` | 消息分页（**时间倒序**，`size` 默认 30；前端反转后按聊天顺序展示） |
| POST | `/api/v1/me/conversations/{id}/messages` | 发送（`{content, lang}`）：≥1 字、≤2000 字、**30 条/小时限流**；双方非互关返回 `1004` |
| POST | `/api/v1/me/conversations/{id}/read` | 标记会话已读（返回标记条数） |
| GET | `/api/v1/me/conversations/unread-count` | 未读私信总数（前台角标） |

### 11.2.3 实现约定与注意事项

- **会话唯一性**：`discussion_conversation(user_a, user_b)` 唯一，且 `CHECK (user_a < user_b)`；注意 **PostgreSQL 的 UUID 比较是字节序，而 Java `UUID.compareTo` 是有符号比较**，服务端统一改用十六进制文本比较，避免约束冲突。
- **未读数**：分别记在会话的 `unread_a` / `unread_b` 两侧；发消息自增对方、读消息清零自己，角标取两侧之和。
- **通知类型**：新增 `follow`（被关注）与 `message`（新私信）；注意 `notification.target_type` 已扩容至 `varchar(32)` 以容纳 `discussion_conversation`。
- **消息时间**：消息时间戳精确到秒（`yyyy-MM-dd HH:mm:ss`），避免同一分钟内的消息在前端排序不稳定。
- **删除语句**：EasyQuery 的实体删除必须显式 `.allowDeleteStatement(true)`，否则静默不执行（取消关注、删除敏感词均依赖此点）。

---

## 11.3 订阅体系与板块管理（迭代 3 已实现）

> 建表脚本：`backend/src/main/resources/db/migration/V4__discussion_subscription.sql`（幂等，并回填历史帖子的作者订阅）

### 11.3.1 订阅（帖子 / 板块 / 用户）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/v1/discussion/subscriptions?targetType=topic\|board\|user&targetId=&level=` | 登录 | 设置通知强度并返回等级；等级非法 `1001` |
| DELETE | `/api/v1/discussion/subscriptions?targetType=&targetId=` | 登录 | 取消订阅（删除记录） |
| GET | `/api/v1/discussion/subscriptions/level?targetType=&targetId=` | 可选 | 查询我的等级；未订阅返回 `null` |
| GET | `/api/v1/me/subscriptions` | 登录 | 我的订阅（含目标名称、封面、可跳转 `path`、等级） |

**三级通知强度语义**（`discussion_subscription.level`）：

| 等级 | 行为 |
| --- | --- |
| `all` | 全部通知：帖子新回复、板块新帖、被 @ |
| `mention` | **仅被 @ 时**通知（普通回复/板块新帖不打扰） |
| `off` | **免打扰**：该目标下所有通知静默，**包括 @** |

**通知分发规则**（`DiscussionServiceImpl`）：
- 帖子新回复 → 该帖 `all` 级订阅者 + 楼主（无订阅记录时默认接收，`off` 时跳过），自动排除发帖人自己；
- 引用回复 → 被引用者（其该帖等级非 `off` 时）；
- 新帖发布 → 板块 `all` 级订阅者（`notification.type = board_topic`，排除作者自己）；
- 发帖人自动订阅自己的帖子（`all`），随时可改为 `mention` / `off`；
- `@提及` → 命中用户且其该帖等级为 `null`/`all`/`mention` 时通知，`off` 静默。

### 11.3.2 板块管理（后台）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/discussion/boards` | `discussion:board` | 板块列表（含隐藏） |
| POST | `/admin/discussion/boards` | `discussion:board` | 新建（`id` 为空）/ 更新（名称、英文名、简介、排序、发帖策略） |
| POST | `/admin/discussion/boards/{id}/status?active=` | `discussion:board` | 启用 / 停用（停用后前台不再展示） |

- 板块 `postPolicy=review_then_post` 时，该板块**所有新帖自动进入待审队列**（先审后发）；
- 新建板块的 `slug` 由名称自动生成（中文名退化为 `board-N`），保证唯一；
- 所有板块改动写入 `discussion_moderation_log`。

### 11.3.3 前端入口

- 帖子详情页作者行：订阅控件（全部通知 / 仅被 @ 时 / 免打扰 / 取消订阅）；
- 讨论区列表：选中板块后工具栏出现板块订阅控件；
- 个人中心「我的订阅」栏：列出订阅目标与等级，可一键取消订阅。

### 11.2.4 私信增强（迭代 4）：图片 / 撤回 / 拉黑

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/me/conversations/{id}/messages` | body 支持 `images`（≤4 张，复用 `/uploads/image` 上传的 `/uploads/...` 地址）；内容与图片至少填一项 |
| POST | `/api/v1/me/messages/{id}/recall` | 撤回：仅发送者本人、**2 分钟内**；撤回后正文清空、返回 `recalled=true`，会话摘要变「消息已撤回」 |
| POST | `/api/v1/me/blocks/{userId}?block=true\|false` | 拉黑 / 解除拉黑 |

- **拉黑语义**：拉黑即解除双方互相关注；拉黑期间**双方都不能发私信**（`1004`）、**不能重新关注**（`1004`）；会话列表/详情返回 `blocked` 字段，前端禁用输入框并给出提示。解除拉黑后需重新互关才能继续私信。
- **撤回规则**：`MessageResource.recallable` 表示「可撤回」（自己发送 + 未撤回 + 2 分钟内）；他人消息返回 `1004`，超时返回 `1004`，重复撤回幂等成功。
- **实现注意**：项目注册的 `PgSQLStringSupportJsonbTypeHandler` 会把**以 `[` 或 `{` 开头的字符串按 jsonb 绑定**，因此写入 varchar 列的文本（如会话摘要）不要用方括号前缀，否则 PG 会报 `invalid input syntax for type json`（本轮即由此修复 `[图片]` / `[消息已撤回]` 两处摘要）。

### 11.3.4 审核效率（迭代 5）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/discussion/reviews?sort=oldest\|priority` | `discussion:review` | `priority`：block 级敏感词 → 被举报多 → 提交更早（扫描窗口 ≤300 条后在内存排序，社区规模上万后应改为 SQL 排序 + 索引） |
| POST | `/admin/discussion/reviews/batch` | `discussion:review` | 批量处置：`{ targetType, ids[], status, reason }`，单条失败不影响其余，返回 `{success, failed, failures}`；单次 ≤100 条 |
| POST | `/admin/discussion/words/import` | `discussion:review` | 批量导入：换行/逗号/顿号/分号分隔，已存在自动跳过，返回 `{added, skipped}` |
| GET | `/admin/discussion/words/export` | `discussion:review` | 导出全部词条（`text/plain`，每行一条，可直接回灌导入） |

后台「讨论区治理」页对应新增：队列排序切换（最早 / 高优先级）、表格多选 + 批量通过/驳回、敏感词批量导入弹窗与导出下载。

## 11.4 机器翻译（迭代 6）：可插拔 provider + 译文缓存

**原则：默认不接商业翻译 API**；模型类通道复用站点已有的大模型接入（`AiConfig` 中的 Spring AI `ChatClient`），其余为自托管/本地实现。服务不可用时按 `fallback-provider` 回退，最终**降级为仅原文**，绝不阻塞页面。

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/v1/translate/status` | 公开 | 返回 `{enabled, provider, label, cacheEnabled, fallbackProvider}`；`enabled=false` 时前端隐藏「译」按钮 |
| POST | `/api/v1/translate` | 公开（`message` 类型需登录） | body `{targetType, targetId, targetLocale, scope}`；返回 `TranslationResource` |

`POST /api/v1/translate` 细节：

- `targetType`：`topic` 帖子 / `post` 楼层 / `message` 私信 / `board` 板块（非法值 → `1001`）。
- `scope`：`title` 仅标题（**列表页只翻标题，成本可控**）/ `body` 仅正文 / `all` 标题+正文（默认）。
- `targetLocale`：目标语言（读者语言，如 `en`）；与原文语言（落库的 `lang`）一致时直接返回 `translated=false` + `message=原文已是该语言`。
- **私信译文仅会话双方可见**：非参与方与游客一律返回 `1002`（不泄露是否存在该消息）。
- 响应：`{targetType,targetId,targetLocale,sourceLocale,content,provider,translated,cached,stale,message}`。
  - `translated=false` ⇒ `content` 为**原文**、`provider=none`，前端不得展示译文切换，只提示 `message`。
  - `provider`：`spring-ai` / `libretranslate` / `ollama` / `glossary`，前端据此标注「机器翻译 · …」（**必须显式标注**）。
- 限流：`rl:translate:{userId|ip}` = 60 次/分钟（超限 `1008`），游客按 IP 计数。
- 缓存：`discussion_translation` 按 `UNIQUE(target_type,target_id,target_locale,source_hash)` 命中，`source_hash` 为**原文 sha256**——正文被编辑后指纹变化，旧译文自然不再命中（表为纯缓存，可随时清理）。
- 超过 `app.translate.max-chars`（默认 4000 字）不翻译，直接降级为原文。

**配置（`app.translate.*`）**

| 配置项 | 默认 | 说明 |
| --- | --- | --- |
| `provider` | `glossary` | `none` 关闭 / `spring-ai`（复用 `AiConfig` 的 `translateChatClient`）/ `libretranslate` / `ollama` / `glossary`（本地文化词表，**词表存库**，见 §11.6）。**默认「词表优先」** |
| `fallback-provider` | `spring-ai` | 主提供方失败（超时 / 额度 / 断网）**或词表覆盖率不足**时的兜底提供方；`none` 表示直接降级为「仅显示原文」；与主提供方相同时视为不启用。**默认「AI 兜底」** |
| `glossary-min-coverage` | `0.8` | 词表可接受的最低覆盖率（命中替换掉的有效字符占比，空白与标点不计入）：低于该比例说明词表只能译出半截，直接让给兜底提供方 |
| `auto-glossary-enabled` | `true` | AI 翻译时把译文里的民族文化术语**实时沉淀**进词表（只新增、不覆盖已有词条） |
| `auto-glossary-max-terms` | `8` | 单次 AI 翻译最多沉淀的术语条数（同时写入系统提示词） |
| `base-url` | 空 | 自托管服务地址，如 `http://127.0.0.1:5000`（LibreTranslate）或 `http://127.0.0.1:11434`（Ollama）；`spring-ai` 通道不使用此项（其地址见 `spring.ai.openai.base-url`） |
| `api-key` | 空 | 可选，LibreTranslate 支持 |
| `model` | `qwen2.5:7b` | Ollama 使用的本地模型；`spring-ai` 的模型由 `spring.ai.openai.chat.model` 决定 |
| `timeout-seconds` | 8 | 单次调用超时；超时即进入兜底/降级 |
| `max-chars` | 4000 | 单次翻译原文长度上限 |
| `cache-enabled` | true | 译文缓存开关 |

**调用链（默认配置）**：词表（覆盖率达标就直接用）→ 覆盖率不足或未命中 → AI（`spring-ai`）→ AI 也不可用 → `translated=false` 返回原文并提示。响应里的 `provider` 是**本次实际产出译文的通道**，前端标注必须跟着它走（同一页面不同内容可能分别是「词表兜底」与「AI 大模型」）。

**各 provider 实现**

- `SpringAiTranslateProvider`（`provider=spring-ai`）：注入 `AiConfig#translateChatClient`（系统提示词固定为「只输出译文」**并在译文末尾附术语表**——与通用对话人格 `chatClient` 隔离），模型 / `base-url` / `api-key` 全部走 `spring.ai.openai.*`。用户消息形如「目标语言：英文（en）／原文语言：中文／文本：…」。返回内容按 `---TERMS---` 切分：**前半段是译文（返回给前端，不含标记），后半段是「原文术语=译法」逐行术语表**，交给 `GlossaryService#learnTerms` 实时沉淀（受 `auto-glossary-enabled` 控制，沉淀失败不影响本次译文）。调用在虚拟线程中执行并受 `timeout-seconds` 约束；`ChatClient` 用 `ObjectProvider` **延迟解析**，站点未配置 `spring.ai.openai` 时只有真正用到该通道才会报错并降级，不会影响 `/translate/status`。
- `LibreTranslateProvider`：`POST {base-url}/translate`（`q/source/target/format=text`，可选 `api_key`），读 `translatedText`。
- `OllamaProvider`：`POST {base-url}/api/generate`（`stream=false`，提示词固定「只输出译文，不要解释」），读 `response`。
- `GlossaryTranslateProvider`：**词表存库**（表 `translate_glossary`，见 §11.6），按「术语 + 目标语言」做最长匹配替换；CJK 术语按前缀扫描、拉丁术语按**词边界**忽略大小写匹配、**反向词条自动推导**（zh→en 的词条同时可用于 en→zh）。命中后还会计算**覆盖率**：低于 `glossary-min-coverage` 时视为「只能译出半截」，抛错交给兜底提供方，避免向前端展示混杂文本。

> **成本提示**：`spring-ai` / `ollama` / `libretranslate` 按次调用模型或服务。列表页只翻标题、译文按原文指纹缓存、以及把 `fallback-provider` 设为 `glossary`，都能显著降低成本与失败率。


## 11.5 站内公告（迭代 6）：后台 OA 群发

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/discussion/broadcast/audience` | `system:broadcast` | `{all, active, maxRecipients, activeWindowDays}`；`active` = 近 `app.notify.active-window-days`（默认 30）天`last_active_at` 有记录且未禁用的账号 |
| POST | `/admin/discussion/broadcast` | `system:broadcast` | body `{title, content, email, audience: all\|active, link?}` → `{broadcastId, title, audience, recipients, email}` |

- 行为：向受众写入 `notification(type=system, target_type=system)`，`payload.link` 承载站内跳转（前端通知中心优先按 `link` 跳转）；`email=true` 时逐条**异步**发信（`MailService#sendHtml`，失败只记日志，不影响站内通知）。
- 保护：单次收件人上限 `app.notify.broadcast-max-recipients`（默认 2000，超出直接拒绝）；限流 `rl:broadcast:{operator}` = 10 次/小时；每次写入 `discussion_moderation_log(target_type=broadcast, target_id=本次公告 ID)` 审计。
- 校验：标题 2～80 字、正文 1～500 字、`link` 必须为 `/` 开头的站内路径（外链 → `1001`）。
- 后台 UI：「讨论区治理」页新增**站内公告**页签（标题/正文/受众单选 + 预计人数/跳转链接/是否邮件/发送），发送前二次确认。

**邮件通道收口（迭代 6）**：`NotificationServiceImpl.EMAIL_TYPES` 仅保留 `review_result`——审核结果自动发信；`topic_reply / post_reply / mention / message / follow / report_result` 一律**仅站内**；校验类邮件（注册/改密验证码）走 `AuthController` 的验证码通道；网站维护/公告由上方 OA 群发按次决定是否发信。`app.notify.email-enabled` 默认 `true`（开发环境的 SMTP 为真实账号，测试请用可真实收信的邮箱或临时关闭该开关）。

## 11.6 翻译词表（迭代 7）：词表入库 + 后台维护

词表是 `glossary` 提供方的数据源，原先是打包在 jar 里的 `classpath:translate/glossary-zh-en.tsv`，现改为**数据库表 `translate_glossary`**（迁移 `V7__translate_glossary.sql`，已内置 154 条民族名 / 节日 / 非遗 / 美食术语），可在后台随时增删改。

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/translate/glossary` | `translate:glossary` | 分页查询；`keyword` 同时匹配术语与译文，`targetLocale` 过滤方向（如 `en`） |
| POST | `/admin/translate/glossary` | `translate:glossary` | 新增 / 更新：body `{sourceLocale, targetLocale, term, translation, enabled, remark}`；**新术语已存在 → `2001`**（避免静默覆盖），编辑/启停请带 `?currentTerm=原术语`（重命名也走此参数，原地改名不残留旧行） |
| DELETE | `/admin/translate/glossary/{id}` | `translate:glossary` | 删除词条 |
| POST | `/admin/translate/glossary/import` | `translate:glossary` | 批量导入：body `{text, sourceLocale, targetLocale}`，每行一条「术语=译文」（也支持 `→` / 全角 `＝` / 制表符），已存在或格式不合法则跳过，返回 `{added, skipped, sourceLocale, targetLocale}` |
| GET | `/admin/translate/glossary/export?targetLocale=en` | `translate:glossary` | 导出（`text/plain`，每行「术语=译文」，可直接再导入） |
| GET | `/admin/translate/glossary/stats` | `translate:glossary` | `{total, enabled, locales}` |
| GET | `/admin/translate/glossary/locales` | `translate:glossary` | 各目标语言可用词条数（后台筛选下拉用） |
| POST | `/admin/translate/glossary/refresh` | `translate:glossary` | 刷新词表缓存并返回各语言词条数；保存/删除/导入本身已即时失效缓存，此接口用于外部直接改库后的即时生效 |
| POST | `/admin/translate/glossary/preview` | `translate:glossary` | 用当前词表试译一段文本（**不写译文缓存、不调模型**），返回 `{original, translated, hit, hits, coverage, accepted, minCoverage}`：`accepted=false` 表示覆盖率未达门限，这条内容会交给兜底提供方 |

**实现要点**

- 表结构：`UNIQUE(source_locale, target_locale, term)`；`enabled=false` 的词条保留在表中但不参与替换；`updated_at` 由既有 `set_updated_at()` 触发器维护。
- 加载策略：内存缓存 60 秒（后台增删改与 AI 沉淀都会立即 `refresh()`），外部直接改库最长 60 秒生效。
- 匹配规则：见 §11.4 的 `GlossaryTranslateProvider`；**反向方向自动推导**，因此只维护 zh→en 一份词条即可同时支持 en→zh。
- **覆盖率门限**：命中替换掉的有效字符占比低于 `app.translate.glossary-min-coverage`（默认 0.8）时，词表不产出译文，交给兜底提供方——避免把「只译出几个术语」的半截文本当成译文展示。
- **AI 实时沉淀**：AI 通道返回的术语表会写回本表，新增条目 `remark='AI 自动提取'`、`enabled=true`，术语必须原样出现在原文里（否则丢弃）、只新增不覆盖已有词条、单次最多 `auto-glossary-max-terms` 条；后台可按关键词「AI 自动提取」检索复核（关键词同时匹配术语、译文与备注）。
- 后台入口：管理端「翻译词表」页（`admin/src/pages/translate/index.vue`），支持列表/搜索/新增/编辑/启停/删除/批量导入/导出/试译预览（含覆盖率）/刷新缓存。

#### 英文正文回填（方向 C-3）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/translate/description-en/pending` | `translate:glossary` | 待译统计，返回 `{festival:{total,pending,done}, art:{...}, food:{...}}`，**不调用模型** |
| POST | `/admin/translate/description-en/backfill` | `translate:glossary` | 回填英文正文：query `type=festival\|art\|food`、`limit`（0=不限）、`dryRun`（true 只试译不写库） |

**背景**：`festival` / `art` / `food` 三表原先只有 `name_en`（英文名），**没有 `description_en`（英文正文）字段**，
由迁移 `V15__description_en.sql` 新增；三类合计 525 行中文正文（11,868 字符）此前无英文。
`name_en` 581 行已全部填写（含 `ethnic_group`），`ethnic_group.description_en` 亦早已补齐（12,131 字符），均无需处理。

**实现要点**

- **复用站点既有翻译通道**：走 `TranslateProvider`，按 `app.translate.provider` → `fallback-provider` 的**降级链**依次尝试。
  默认配置 `provider=glossary`（本地词表，只能逐词替换，长句必然不达覆盖率门限而抛错）、`fallback-provider=spring-ai`（AI 大模型），
  因此**真正产出译文的是 AI 通道**——若只取 `provider` 会全部失败。
- **来源标记**：写入的译文标 `description_en_source='machine'`；`reviewed`（人工校对）/`manual`（后台录入）的行**不会被覆盖**。
- **可续跑**：已有英文的行跳过，中途中断后重复调用即可继续，不会重复消耗模型额度。
- **不编造**：翻译失败的行保持 `NULL`，不做占位填充，前端回退显示中文。
- **`limit` 语义**：限制的是**本次实际调用翻译通道的条数**，而不是遍历行数——否则已译行会持续吃掉配额，导致批次原地打转。
- **超限保护**：中文正文超过 `app.translate.max-chars`（默认 4000）的行跳过并计入 `failures`，不截断翻译。
- **开发环境注意**：`application-dev.yaml` 的 `spring.ai.openai.base-url` 若指向未常驻启动的本机模型服务
  （`127.0.0.1:8080`）会直接超时失败；本机开发请与 `application-prod.yaml` 一样指向可用端点。

#### 11.x 检索索引与兴趣标签维护（方向 D）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/search-index/stats` | `translate:glossary` | 索引统计：`{total, byType:[{docType,count}], supportedTypes}` |
| POST | `/admin/search-index/rebuild` | `translate:glossary` | 重建索引；`?type=xxx` 只重建该类型，留空为全量。返回 `{total, byType, elapsedMs}` |
| GET | `/admin/interest-tags` | `translate:glossary` | 兴趣标签分页查询；`dimension` / `keyword` 筛选 |
| POST | `/admin/interest-tags` | `translate:glossary` | 新增或更新（按 `dimension`+`name` 唯一）；已存在则更新说明/英文名/配色/启停/排序 |
| DELETE | `/admin/interest-tags/{id}` | `translate:glossary` | 删除标签（`user_interest` 由外键级联删除） |
| GET | `/admin/interest-tags/stats` | `translate:glossary` | 各维度标签数 |

**权限说明**：复用既有 `translate:glossary`（内容运营域），不新增权限点，避免后台角色配置复杂度上升。
后台入口：管理端「系统设置 → 检索与推荐」（`admin/src/pages/search-index/index.vue`）。

**实测**：后台接口 **16 项断言全部通过**，覆盖索引统计（8 类 1142 条）、单类型重建、
兴趣标签列表（111 条）与维度统计，以及完整 CRUD（新增 → 编辑 → 校验更新生效 → 删除 → 校验已消失）。


---

## 12. 流式传输约定（SSE，预留）

### 12.1 适用场景

- AI 智能讲解 / 文化摘要生成。
- 多语言实时翻译。
- 大文件导出进度推送。

### 12.2 协议

- 基于 **SSE（`text/event-stream`）**，单向下行流式推送。
- 客户端通过 `EventSource` 或 `fetch` + `ReadableStream` 消费。

### 12.3 接口示例（预留）

- **接口**：`POST /api/v1/ai/explain` 或 `GET /api/v1/ai/explain/stream`
- **请求体**：`{ "ethnicGroupId": "ethnic_tibetan" }`

SSE 事件流格式：

```
event: delta
data: {"content": "藏族，主要聚居在青藏高原……"}

event: delta
data: {"content": "其传统节日雪顿节……"}

event: done
data: {"contentId": "explain_123"}
```

- 心跳：服务端每 15s 发送 `: ping` 保持连接。
- 断线重连：客户端基于 `Last-Event-ID` 续传。

---

## 13. 附录

### 13.1 鉴权头示例

```
POST /api/v1/contents/ethnic/ethnic_tibetan/like
satoken: a1b2c3d4...
Content-Type: application/json
```

### 13.2 sa-token 集成要点

| 配置项 | 建议值 | 说明 |
| --- | --- | --- |
| `token-name` | `satoken` | token 名称 / header 名 |
| `token-timeout` | `1800`（秒） | 有效期 30min |
| `is-read-header` | `true` | 从请求头读取 token |
| `is-read-cookie` | `false` | 默认不读 cookie |
| `is-concurrent` | `true` | 允许同端并发登录 |
| `is-share` | `false` | 不同端不同 token |
| 会话存储 | Redis | 使用 `sa-token-redis` 集成，多实例共享会话 |

> 前端侧：登录后存储 `tokenValue`，Axios 拦截器统一注入 `satoken` 请求头；收到 `1003` / `1005` 时跳转登录或刷新。
