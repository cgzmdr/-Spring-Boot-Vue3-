# 《走进多彩 56 个民族世界》C 端 UI 规范（UI-SPEC）

> 版本：v1.0
> 状态：待评审
> 依据：`design/` 目录静态设计稿（11 个 HTML 页面 + `common.css`）
> 关联文档：`PRD.md` / `API.md`
> 更新日期：2026-08-13

---

## 1. 设计风格

**杂志编辑风（Editorial）**——参考 National Geographic / Kinfolk 的叙事排版气质：

- **纸张质感**：低饱和暖色纸张底（`#FAF9F5`），非纯白，营造印刷物质感。
- **墨色排版**：高对比墨色标题（`#141414`），衬线体大字号标题，字距拉开。
- **朱红点缀**：`#B6402E` 仅用于编号、强调文字、装饰线与状态，克制使用。
- **黑白摄影**：图片默认黑白（灰度滤镜），悬停转彩——全站签名式交互。
- **叙事结构**：章节带序号（01/02/03…）、竖线分隔、时间线等编辑语言。

---

## 2. 设计令牌（Design Tokens）

来源：[common.css](file:///c:/codeDev/56/app/design/common.css)

### 2.1 色彩

| Token | 色值 | 用途 |
| --- | --- | --- |
| `--paper` | `#FAF9F5` | 页面底色（纸张） |
| `--paper-2` | `#F2F0E9` | 次级底色（交替区块） |
| `--ink` | `#141414` | 墨色：主文字 / 按钮实心 / 粗分隔线 |
| `--muted` | `#6B6B66` | 次要文字（元信息、说明） |
| `--line` | `#D9D8D1` | 细分割线、边框 |
| `--accent` | `#B6402E` | 朱红：强调色、编号、活动态、kicker |

辅助色：搜索框占位符 `#b9b7ac`、正文文字 `#3a3a36`。

**用法约束**：强调色仅作点缀（编号/装饰线/活动态），大面积色块必须用 `--ink`；背景交替使用 `--paper` / `--paper-2` 表现章节区分，不使用彩色背景。

### 2.2 字体

| Token | 字体栈 | 用途 |
| --- | --- | --- |
| `--serif` | `Georgia, "Songti SC", "STSong", "Noto Serif SC", serif` | 标题（h1–h5）、大字号排版、编号数字 |
| `--sans` | `system-ui, -apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif` | 正文、导航、按钮、元信息 |

**排版层级**（字号均可用 `clamp()` 响应式缩放）：

| 层级 | 字号 | 字重 | 字距 | 字体 |
| --- | --- | --- | --- | --- |
| 报头大标题 h1 | `clamp(30px, 6vw, 52px)` | 800 | 8px | serif |
| 页头大标题 h2 | `clamp(32px, 5vw, 56px)` | 800 | 2px | serif |
| 封面故事标题 | `clamp(40px, 6vw, 72px)` | 800 | -1px | serif |
| 章节标题 h3 | 28px | 700 | 4px | serif |
| 卡片标题 h4 | 19–24px | — | 1–2px | serif |
| 正文 p | 16–18px | 400 | — | sans，行高 1.6 |
| 元信息 | 11–14px | 400 | 1–3px（大写化） | sans，muted 色 |

**语言标记**：英文/拼音等辅助文案使用 `text-transform: uppercase` + 大字母间距（如 `.sub`、`.kicker`、`.meta`）。

### 2.3 布局与间距

| Token | 值 | 说明 |
| --- | --- | --- |
| `--container` | 1240px | 内容最大宽度 |
| `--gutter` | 28px | 容器左右内边距 |
| 章节垂直间距 | 56px（`.section` padding） | 章节间留白 |
| 卡片间隙 | 20px（默认）/ 14px（grid-6）/ 1px（带边框拼接布局） | |

**双栏特性**：封面故事、专题主视觉均采用 `1.4fr 1fr` 左文右图布局，两栏之间以 1px `--line` 竖线分隔；双栏区块带 1px 外边框，内部区块用 `--line` 做 1px 拼接缝隙（gap:1px + background:--line 技法）。

---

## 3. 版式与网格

| 网格类 | 列数 | 典型用途 |
| --- | --- | --- |
| `.grid-2` | 2 | 文化之窗双栏、政策解读 |
| `.grid-3` | 3 | 精选专题、故事、艺术卡片 |
| `.grid-4` | 4 | 民族卡片列表页 |
| `.grid-6` | 6 | 首页民族入口、全家福网格 |

---

## 4. 组件规范

> 全部组件样式见 [common.css](file:///c:/codeDev/56/app/design/common.css)，以下为视觉与使用规范。

### 4.1 报头 `.mast` / `.mast-top` / `.mast-title` / `.mast-nav`

- 顶部信息条：两端对齐（左「中华民族 · 多元一体」，右「搜索 · 中 / EN」），12px 大写化。
- 居中大标题：衬线体 `clamp(30px, 6vw, 52px)`、字距 8px；下方英文副标 11px、字距 6px。
- 底部导航：14px、字距 2px，`gap: 40px` 居中；`border-top: 1px solid var(--line)`。
- 当前项：`--accent` 色 + 700 字重 + 底部 2px 朱红指示条（`.active::after`）。
- 报头下边框：**3px 墨色粗线**（全站标志性边界，页脚同样）。

### 4.2 页脚 `.footer`

- `border-top: 3px solid var(--ink)`，padding 48px 0。
- 三块内容：品牌（衬线 16px 字距 3px）/ 链接组 / 版权，12px 大写化 muted 色。

### 4.3 章节标题 `.section-rule`

`[编号 14px 朱红 700] [标题 28px 衬线 字距4px] [弹性横线 1px 墨色]`

编号为编辑语言核心元素，必须保留。

### 4.4 按钮 `.btn` / `.btn-solid` / `.btn-accent`

- 描边按钮：2px 墨色边框、透明底、13px 大写化、字距 2px、700 字重、padding 12px 26px。
- `.btn-solid`：墨色实底白字；`.btn-accent`：朱红实底白字。
- hover：opacity .88。

### 4.5 面包屑 `.crumb`

12px、muted 色、字距 1px；当前项朱红；`margin: 24px 0 8px`。

### 4.6 页头 `.page-head` / `.kicker` / `.dek`

- `.kicker`：12px 朱红大写化、字距 3px（如 "COVER STORY · 封面故事"）。
- h2：衬线大标题（见排版层级）。
- `.dek`：16px muted，`max-width: 56ch`。

### 4.7 徽章 `.badge` / `.badge.ink`

11px 大写化、1px 边框、padding 3px 10px；默认朱红，`.ink` 变体墨色。

### 4.8 民族卡片 `.ethnic-card`

- 3/4 竖版封面图（`aspect-ratio: 3/4`，object-fit cover）+ 底部内容区（padding 14px 16px 18px）。
- 名称：衬线 19px 字距 1px；元信息：12px muted。
- hover：`box-shadow: 0 10px 30px rgba(0,0,0,.10)`。

### 4.9 专题卡片 `.feature`

- 16/10 横版封面图 + 内容区（padding 20px 22px 24px）。
- 结构：编号（12px 朱红 700）→ 标题（衬线 22px）→ 描述（13px muted）。
- 通常以「1px 缝隙拼接」网格呈现（gap:1px + 外边框）。

### 4.10 列表条目 `.list-item`

- 18px 上下内边距 + 底部 1px `--line`（末项去线）。
- 结构：编号 `.idx`（12px 朱红 700）→ 标题 h5（衬线 19px）→ 描述（13px muted）→ `.meta`（12px 大写化）。

### 4.11 筛选栏 `.filter` / `.chip`

- 容器：1px `--line` 边框，padding 20px 22px，margin 24px 0。
- 行：`flex wrap` + 12px 24px 间距；`.label` 12px 大写化 muted。
- `.chip`：13px、1px 边框、padding 5px 14px；`.active`：墨色实底白字。
- `.result-count`：12px 大写化 muted，margin 18px 0 14px。

### 4.12 标签页 `.tabs` / `.tab`

- 容器：`border-bottom: 1px solid --line`，`gap: 32px`。
- `.tab`：14px 字距 2px，padding 10px 2px；活动态朱红 + 2px 朱红底边。

### 4.13 时间线 `.timeline` / `.tl-item`

- 容器：`border-left: 2px solid --line`，padding-left 28px。
- 节点：12px 朱红圆点（`::before`）。
- 结构：日期（14px 朱红 700）→ 标题 h5（衬线 20px）→ 描述（13px muted）。

### 4.14 搜索 `.search-box` / `.hot`

- 输入区：`border-bottom: 3px solid var(--ink)`，输入框衬线 26px 字距 1px。
- 热门词：12px muted + `.chip` 复用。

### 4.15 正文 `.article` / `.dropcap` / `.info-card`

- `.article`：`max-width: 720px`；h4 衬线 24px；p 16px `#3a3a36`。
- `.dropcap::first-letter`：首字下沉——衬线 56px、朱红、float left。
- `.info-card`：1px 边框，`dl` 双列（92px 标签列 + 内容列），dt muted。

### 4.16 分页 `.pagination`

居中，`gap: 8px`；页码项 36px 最小宽、1px 边框；当前页墨色实底白字。

---

## 5. 图片规范

- **占位图源**：`https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt={提示词}&image_size={尺寸}`，`image_size` ∈ `square_hd` / `square` / `portrait_4_3` / `portrait_16_9` / `landscape_4_3` / `landscape_16_9`。生产环境替换为 OSS/CDN 真实图片。
- **黑白转彩**：所有内容图片添加 `.img-bw`（`filter: grayscale(100%) contrast(1.05)`，hover → `grayscale(0)` + `scale(1.02)`，过渡 0.35s）。这是杂志编辑风签名式交互，**全站图片必须遵守**（卡片/封面/详情/图集一致）。
- **比例约束**：民族卡片 3/4；Feature 卡片 16/10；Hero 满幅 16/9。
- **图说**：Hero 图片底部覆盖墨色条 + 白字 12px 图说（如「五十六个民族 · 一家亲」）。

---

## 6. 响应式断点

| 断点 | 规则 |
| --- | --- |
| 桌面 ≥1024px | 完整布局；grid 3/4/6 列 |
| `@media (max-width: 900px)` | `grid-6 → 3列`；`grid-4 → 2列`；`grid-3/2 → 1列`；导航 `gap: 22px` |
| `@media (max-width: 560px)` | `grid-6 → 2列`；`info-card` 双列 dl 转单列 |

> 设计稿断点为 900px / 560px 两档（比常规 1024/768 更宽的平板档），前端落地时以本规范为准。

---

## 7. 页面清单（design/ 设计稿 → 前端映射）

| 设计稿 | 页面 | 主要区块 |
| --- | --- | --- |
| `home.html` | 首页 | 封面故事 Hero（1.4fr/1fr 左文右图）→ 01 五十六个民族（grid-6）→ 02 精选专题（grid-3 拼接）→ 03 文化之窗（grid-2 双栏 list-item） |
| `ethnic.html` | 民族列表 | 页头 → 筛选栏（地域/语系/排序 chips）→ 结果计数 → grid-4 卡片 → 分页 |
| `ethnic-detail.html` | 民族详情 | Hero 横幅（藏文自名）→ 4 概览指标 → Tabs（概况/风俗/节日/艺术/美食/图集）→ 文章 dropcap + info-card → 节日/艺术双栏 |
| `festival.html` | 节日列表 | 页头 → 筛选（民族/类型/视图）→ 时间线视图（春/夏/秋分组） |
| `festival-detail.html` | 节日详情 | 页头（badge）→ 主视觉 → 起源传说/习俗活动 + info-card → 图集 grid-3 → 相关节日 grid-3 |
| `art.html` | 艺术列表 | 页头 → 筛选（类别/非遗级别）→ grid-3 feature 卡片 → 分页 |
| `art-detail.html` | 艺术详情 | 页头（badge）→ 主视觉 → 介绍/代表作品 + info-card → 影像资料 grid-3 |
| `search.html` | 搜索 | 搜索 Hero（大搜索框 + 热门词）→ 分组结果（民族/节日/艺术） |
| `unity.html` | 民族团结专题 | 主视觉（多元一体）→ 01 故事 grid-3 → 02 历史时间线 → 03 政策解读 grid-2 → 04 56 民族全家福墙（chip 云） |
| `about.html` | 关于我们 | 背景/目标/团队/数据来源声明/联系方式 |
| `index.html` | 设计稿目录 | 全部页面入口 + 设计规范速览（不进入生产） |

---

## 8. 落地注意事项

1. `common.css` 为设计系统唯一事实来源：C 端 Vue 落地时**保持类名与视觉不变**，仅将 HTML 结构组件化、数据 API 化。
2. 强调色 `--accent` 使用克制：只用于编号、kicker、活动态、装饰线、首字下沉、节点圆点。
3. 所有列表接口对接 `docs/API.md`（`/api/v1`、`code===0`、`page/pageSize`、UUID）。
4. 互动（点赞/收藏）需登录（sa-token 头 `satoken`），未登录引导登录。
5. 图片加载：落地时保留 `.img-bw` 交互，并加 `loading="lazy"`；正式图替换 AI 占位图源。
