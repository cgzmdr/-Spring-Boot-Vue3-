# 走进多彩 56 个民族世界

一个以「中华民族 · 多元一体」为主题的文化数字化展示平台：用 Spring Boot + Vue3 全栈实现 56 个民族的风俗、节日、艺术、美食等内容的展示、检索与互动。

## 功能特性

- **C 端门户**（`/`，即仓库根）：民族 / 节日 / 艺术 / 民族团结专题的浏览、检索、双语（中/英）展示，点赞 / 收藏 / 分享 / 浏览统计
- **个人中心**（`/profile`）：基本信息与头像、邮箱绑定（验证码）、昵称修改；账号密码（支持旧密码 / 邮箱验证码 / 密保问题三种方式验证后改密）；基础设置（页面字号、界面语言）；我的收藏（按类型筛选、取消收藏、分页）
- **中后台管理系统**（`admin/`）：内容（民族 / 节日 / 艺术 / 专题 / 动态表单）运营、内容审核、用户 / 角色 / 权限管理、统计看板
- **移动端**（`mobile/`，可选）：React Native / Expo 浏览端

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 `backend/` | Java 21 · Spring Boot 4 · Sa-Token（鉴权） · EasyQuery（ORM，PostgreSQL） · Redis · JavaMail · Lombok · Gradle |
| C 端门户（仓库根） | Vue 3.5 · TypeScript · Vite 8 · Element Plus · Pinia · Vue Router · pnpm |
| 中后台 `admin/` | Vue 3 · TypeScript · Vite 6 · Element Plus · Pinia · ECharts |
| 移动端 `mobile/` | React Native · Expo（可选） |
| 数据库 / 缓存 | PostgreSQL（业务数据） · Redis（验证码 / 计数缓存） |

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

### 1. 初始化数据库

```bash
# 创建数据库（示例库名 56_app）
psql -U postgres -c "CREATE DATABASE \"56_app\";"

# 建表（结构快照；已存在的库可跳过）
psql -U postgres -d 56_app -f backend/schema.sql
```

> 增量结构变更以 `backend/src/main/resources/db/migration/` 下的脚本为准（幂等，可重复执行）。
> 内容数据（民族 / 节日 / 艺术等）需通过中后台录入，或从现有环境导入。

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

## 文档

`docs/` 目录包含 [PRD](./docs/PRD.md)、[API 契约](./docs/API.md)、[UI 规范](./docs/UI-SPEC.md) 与[全站文档](./docs/全站文档.md)。
