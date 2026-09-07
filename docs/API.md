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
- **响应 `data`**：完整 `EthnicGroup` 对象（含 `customs` / `festivals` / `arts` / `foods` / `locations` 各维度）。

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

### 5.5 艺术列表

- **接口**：`GET /api/v1/arts`
- **查询参数**：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `category` | string | 否 | `music` / `dance` / `drama` / `costume` / `craft` / `architecture` |
| `ethnicGroupId` | string | 否 | 所属民族 |
| `intangibleHeritageLevel` | string | 否 | `world` / `national` / `provincial` |
| `page` / `pageSize` | int | 否 | 分页 |

### 5.6 艺术详情

- **接口**：`GET /api/v1/arts/{id}`

### 5.7 专题列表

- **接口**：`GET /api/v1/topics`

### 5.8 专题详情

- **接口**：`GET /api/v1/topics/{id}`

### 5.9 56 民族全家福（简略列表）

- **接口**：`GET /api/v1/ethnic-groups/all`
- **说明**：返回 56 个民族的 `id`、`name`、`themeColor`、`coverImage` 极简字段，供「全家福互动墙」使用，不分页。

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

### 5.11 热门搜索词

- **接口**：`GET /api/v1/search/hot`
- **说明**：返回 Redis ZSet 排行榜前 N 个热门词。

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
