# 首屏性能优化（3G 目标）—— 改动说明与上线步骤

> 目标站点：<https://czdr.work>
> 目标：让 3G（有效带宽约 400–1600 kbps）用户在 3 秒内看到有内容的首页。

---

## 一、改动前的实测数据

对线上 <https://czdr.work> 直接抓取首页与其首屏资源，得到：

| 项目 | 实测值 |
| --- | --- |
| 首屏**必须**下载的资源 | **602.1 KB**（brotli 口径） |
| 其中 `placeholder.webp` | **450.3 KB** |
| 其中 `favicon.svg` | 9.5 KB（未压缩） |
| 其中 11 个 JS/CSS | 142.3 KB |
| nginx 返回的 `Content-Encoding` | `gzip` |
| nginx 是否支持 Brotli | **否**（请求 `br` 时返回未压缩原文） |
| `Cache-Control`（`/assets/*` 哈希文件名） | `max-age=43200`（12 小时，偏短） |
| `Cache-Control`（`/data/*.json`、`/icons.svg`） | **缺失** |

按 3G 带宽换算：

| 网络 | 首屏下载耗时 |
| --- | --- |
| 3G 较差（400 kbps） | **12.3 s** |
| 3G 平均（780 kbps） | **6.3 s** |
| 3G 良好（1600 kbps） | **3.1 s** |

### 三个根因

1. **单张 450 KB 的占位图是最大瓶颈。**
   `public/images/placeholders/placeholder.webp` 原始尺寸是 1874×2800 的实拍图。
   它被首页 Hero 以及所有缺图的卡片引用，在 3G 下光这一个文件就要 **9 秒以上**。

2. **首屏存在「白屏期」，用户看不到任何反馈。**
   HTML 是空壳（`<div id="app"></div>`），Vue 挂载前页面全白。
   主样式表未内联，加载完成前连背景色都不对（FOUC）。

3. **网络层没压榨到位。** 只有 gzip、没有 Brotli，也没有预压缩。

---

## 二、已经完成的代码改动

### 1. 占位图从 450 KB 压到 12 KB（收益最大）

- 新增 `scripts/gen-placeholder.mjs`：用 sharp 把源图裁成 **320×180 / 12 KB**。
  裁成 16:9 而不是等比缩放，是因为 Hero 本来就是 `object-fit: cover` 的 16:9，
  这样同样的码率全部花在真正会显示的区域上。
- 新增 `src/utils/placeholder-lqip.ts`：从同一张图生成 **24px 宽的 base64 内联图（0.17 KB）**，
  作为图片到达前的模糊占位。内联在 JS 里，**不产生任何额外请求**。

> 想换图时重跑：`node scripts/gen-placeholder.mjs [源图]`

### 2. 首屏骨架屏 + 关键 CSS 内联（解决白屏）

`index.html` 现在内联了：

- 背景色、报头轮廓、版心所需的**关键 CSS**（约 1 KB），消除 FOUC；
- 一个纯 HTML+CSS 的**静态骨架** `#boot-skeleton`（转圈 + 站名 + 进度条）。
  它不依赖任何外部资源，因此**必然**早于 JS 显示出来；
- `<link rel="preload" as="image">` 预加载首屏封面图，缩短 LCP；
- `<meta name="theme-color">`，移动端浏览器地址栏配色统一。

`src/main.ts` 在 `app.mount()` 成功后把骨架淡出并移出 DOM，
同时尊重 `prefers-reduced-motion`（关闭动画，只留静态文字）。

### 3. 登录态恢复移出首屏关键路径

原先 `main.ts` 在 `mount()` 后**立即**调用 `useAuthStore().restore()` 发 `/me` 请求。
慢网下这个请求会和封面图争抢同一条 3G 链路，把图片下载推后好几秒。

现改为 `requestIdleCallback`（不支持时降级 `setTimeout`）在浏览器空闲时再执行，
**先让页面完整可见，再恢复登录态**。

### 4. 图片占位逻辑修正（顺手修掉的既有缺陷）

`CoverImage.vue` 原先只在图片 URL 以 `.webp` 结尾时才生成模糊占位层，
所以 `.jpg` / `.png` 封面在加载完成前是**一块空白**。
现在统一由内联 LQIP 兜底，任何格式都有占位。

同时修正：**前端自带的静态资源保持同源**。
此前 `/images/placeholders/placeholder.webp` 会被改写成 `https://aa.czdr.work/...`，
导致跨域握手、无法命中 `preload`、也吃不到下面 nginx 的长缓存规则。
后端下发的图片（`/uploads/...`）行为不变。

### 5. Hero 图优先级

Hero 标为 `loading="eager"` + `fetchpriority="high"`，
其余 12 张卡片图保持 `loading="lazy"`，避免它们一起抢 3G 的有限带宽。

### 6. 构建配置（`vite.config.ts`）

- 接入 `vite-plugin-compression2`：构建期同时产出 **`.br` 与 `.gz`**，
  交给 nginx 的 `brotli_static` / `gzip_static` 直接命中。
- 生产构建丢弃 `console.*`（走 Rolldown 的 `output.minify`，
  注意 Vite 8 顶层的 `oxc` 选项只管语法转换、压缩选项会被忽略）。
- 拆包只拆 `vendor-vue`。
  **不要**把 Element Plus 拆成入口可达的独立 chunk —— 实测这样会让
  首屏多下载 434 KB / gzip 138 KB，比不拆更慢。

---

## 三、第一轮的实测数据

| 项目 | 改动前 | 改动后 |
| --- | --- | --- |
| 首屏总下载量 | 602.1 KB | **175.6 KB（−70.8%）** |
| 首屏阻塞资源原始体积 | 525.5 KB | 526.1 KB（基本不变） |
| 首屏公共图片 | 449.1 KB | **21.1 KB** |
| 3G 较差（400 kbps） | 12.3 s | **3.6 s** |
| 3G 平均（780 kbps） | 6.3 s | **1.8 s** |
| 3G 良好（1600 kbps） | 3.1 s | **0.9 s** |

> 复现命令：`node scripts/measure-first-screen.mjs <旧 dist> dist`
>
> 注：上表的 JS/CSS 一律按 **brotli** 计算。线上目前只有 gzip，
> 因此**必须**完成下一节的 nginx 配置，才能拿到表中后半段的真实收益。
>
> 第二轮（Element Plus 瘦身）后的数据见第五节。

---

## 四、还需要在服务器上做的配置（关键，不做则收益打折）

以下配置在代码仓库之外，需要改服务器上的 nginx。

### 1. 打开 Brotli 静态预压缩

**先确认 nginx 是否编译了 brotli 模块：**

```bash
nginx -V 2>&1 | tr ' ' '\n' | grep -i brotli
```

- **有输出**（如 `--add-module=.../ngx_brotli`）→ 直接用下面的配置。
- **没有输出** → 二选一：
  1. 给 nginx 加装 `ngx_brotli` 模块后重新编译（推荐，收益最大）；
  2. 暂时跳过 `brotli_static`，只保留 `gzip_static`——
     gzip 也能命中构建期产物，省掉现场压缩的 CPU，只是体积比 brotli 大一些。

```nginx
# 静态压缩资源：构建期已产出 .br / .gz，这里直接命中，避免每个请求现场压缩
brotli_static on;      # 需要 ngx_brotli 模块
gzip_static on;        # 内置模块，作为兜底

gzip on;
gzip_vary on;
gzip_comp_level 6;
gzip_min_length 1024;
gzip_types
    text/plain text/css text/xml
    application/javascript application/json application/xml
    image/svg+xml;
```

> `gzip_static` 属于 `ngx_http_gzip_static_module`，部分发行版默认未编译。
> 用 `nginx -V 2>&1 | grep -o gzip_static` 确认；没有的话在编译参数里加上
> `--with-http_gzip_static_module`。

### 2. 修正缓存策略

```nginx
# 带内容哈希的构建产物：可以永久强缓存
location /assets/ {
    expires 1y;
    add_header Cache-Control "public, max-age=31536000, immutable";
    access_log off;
    try_files $uri $uri/ =404;
}

# 前端自带的静态资源。文件名固定，因此用较短的强缓存 + 必要的协商缓存。
# 优化占位图时若沿用同名文件，记得把 max-age 调小或改名（如 placeholder.v2.webp）。
location /images/ {
    expires 7d;
    add_header Cache-Control "public, max-age=604800";
    access_log off;
}

location /data/ {
    expires 7d;
    add_header Cache-Control "public, max-age=604800";
    access_log off;
}

location = /favicon.svg {
    expires 7d;
    add_header Cache-Control "public, max-age=604800";
    access_log off;
}

location = /icons.svg {
    expires 7d;
    add_header Cache-Control "public, max-age=604800";
    access_log off;
}

# index.html 绝不能强缓存，否则用户拿不到新的资源哈希引用
location = /index.html {
    add_header Cache-Control "no-cache, must-revalidate";
}
```

### 3. 顺带确认

```nginx
# 已有 HSTS；确认 HTTP/2（或 HTTP/3）已开启，能显著减少多请求的握手开销
listen 443 ssl;
http2 on;
```

nginx 响应头里已经出现 `Alt-Svc: h3=":443"`，说明 QUIC 可用，
但要确认 `http2 on;` 也已配置——当前响应未见 HTTP/2 的明确迹象。

### 4. 部署

```bash
pnpm install          # 引入 sharp / vite-plugin-compression2
pnpm build            # 产出 dist/，含 .br 与 .gz
# 将 dist/ 同步到 nginx root（注意 .br / .gz 隐藏扩展名的文件也要一起传）
rsync -av --delete dist/ /var/www/czdr.work/
nginx -t && nginx -s reload
```

> **注意**：`dist/` 里的 `.br` / `.gz` 是独立文件，用 `scp -r` 或
> `rsync` 时要确保它们被一起复制（不要用只匹配 `*.js` / `*.css` 的过滤器）。

### 5. 上线后验证

```bash
# Brotli 是否生效：Content-Encoding 应为 br
curl -sI -H 'Accept-Encoding: br' https://czdr.work/assets/<入口>.js | grep -i content-encoding

# 缓存头是否正确
curl -sI https://czdr.work/assets/<入口>.js | grep -i cache-control
curl -sI https://czdr.work/images/placeholders/placeholder.webp | grep -i cache-control

# 占位图大小（应为 12 KB 左右，而不是 450 KB）
curl -so /dev/null -w '%{size_download}\n' https://czdr.work/images/placeholders/placeholder.webp
```

再用 Chrome DevTools 的 **Lighthouse → 移动端 + Slow 4G 节流** 复核 LCP。

---

## 五、第二轮：Element Plus 首屏瘦身 + 地图数据懒加载

第一轮把「必下载字节」砍掉 70%，但首屏仍要解析一整套用不到的 Element Plus。
第二轮针对这块继续压缩。

### 5.1 首屏到底加载了哪些 Element Plus 组件

排查入口闭包（`App.vue` → `DefaultLayout` → `AppMasthead` → 首页）后发现，
首屏牵连了 20 多个 EP 组件，其中大半是「只有用户点了才会出现」的：

| 组件 | 触发时机 | 是否该进首屏 |
| --- | --- | --- |
| `el-dialog` / `el-form` / `el-form-item` / `el-input` / `el-button` | 点「登录」 | ✗ |
| `el-drawer`（含 overlay / focus-trap / 滚动锁） | 窄屏点「菜单」 | ✗ |
| `el-dialog` / `el-form` / `el-select` / `el-date-picker` … | 点「反馈」 | ✗ |
| `el-tooltip` / `el-popper` / `el-focus-trap` | 鼠标悬停 | ✗ |
| `el-config-provider` / `el-backtop` / `el-link` | 首屏可见 | ✓ |
| `el-skeleton` / `el-empty` / `el-icon` | 首屏可见 | ✓ |

也就是说：**3G 用户还没看到页面，就先为一个可能永远不打开的弹窗付了流量。**

### 5.2 做了什么

1. **登录弹窗改为异步组件**（`defineAsyncComponent`）。
   同时把它的 `auth:required` 事件监听**上移到 AppMasthead**——
   组件没挂载时自己是收不到事件的，不移就会导致未登录用户触发受限操作时「毫无反应」。

2. **移动端菜单抽屉拆成独立组件** `MobileNavDrawer.vue`，再异步加载。
   样式沿用 `common.css` 里既有的 `.mobile-nav` 规则，视觉零变化。
   桌面端用户全程不会下载它。

3. **首页反馈弹窗拆成 `FeedbackDialog.vue`**，异步加载。
   提交逻辑一并内聚过去，首页只保留「打开」这一个意图。

4. **两处 `el-tooltip` 换成原生 `title`**。
   它们只是给悬浮按钮加鼠标提示，却要把整条 popper 链
   （`ElTooltip` → `ElPopper` → `ElFocusTrap`）拖进首屏。原生 `title` 零成本。

5. **地图组件的 GeoJSON 请求保持「挂载时才发起」**（该项第一轮已满足，本轮补了自动化验证）。

### 5.3 一个被自动化检查抓出来的真实缺陷

异步化最典型的坑是「点了没反应」。我在实现 `openLogin()` 时先写成：

```ts
loginDialogMounted.value = true;
await nextTick();          // ❌ 不够
loginDialog.value?.open();
```

`nextTick` 只等一次 DOM 更新，而**异步 chunk 还没下载完**，此时 `ref` 仍是 `null`，
`open()` 被静默跳过——表现就是点击「登录」毫无反应。

正确做法是把「打开意图」记下来，等组件真正挂载后再消费
（见 `AppMasthead.vue` 的 `pendingLoginOpen` + `@vue:mounted`）。

这个 bug 是 `scripts/check-lazy-dialogs.mjs` 跑出来的（6 项里挂了 2 项）。
**如果只靠肉眼看首页截图，它会被完全漏掉**——因为首屏本来就是正常的。

### 5.4 效果

| 项目 | 第二轮前 | 第二轮后 | 累计（对比最初线上） |
| --- | --- | --- | --- |
| 首屏阻塞资源数 | 12 | **7** | 12 → 7 |
| JS（brotli） | 141.7 KB | **102.2 KB** | 141.7 → 102.2 KB |
| CSS（brotli） | 12.8 KB | **7.6 KB** | 12.8 → 7.6 KB |
| 首屏总量（含图片） | 175.6 KB | **130.9 KB** | 602.1 → 130.9 KB（**−78%**） |

按带宽换算：

| 网络 | 最初 | 第一轮后 | 第二轮后 |
| --- | --- | --- | --- |
| 3G 较差（400 kbps） | 12.3 s | 3.6 s | **2.7 s** |
| 3G 平均（780 kbps） | 6.3 s | 1.8 s | **1.4 s** |
| 3G 良好（1600 kbps） | 3.1 s | 0.9 s | **0.7 s** |

首屏残留的 EP 组件只剩：`ElConfigProvider`、`ElBacktop`、`ElLink`、
`ElIcon`、`ElMessage`、`ElBadge`。

> **注意**：以上收益仍以 nginx 开启 Brotli 为前提，见第四节。

### 5.5 自动化验证

交互类优化必须有回归检查，否则「性能变好了、功能坏了」很难发现。

```bash
pnpm build
pnpm preview --port 4380          # 另开一个终端

# 弹窗/抽屉/事件是否仍能正常唤起（6 项）
node scripts/check-lazy-dialogs.mjs http://127.0.0.1:4380

# 地图数据是否真的只在切到地图时才请求（6 项）
node scripts/check-map-lazy-fetch.mjs http://127.0.0.1:4380
```

两个脚本都通过 Chrome DevTools Protocol 直接驱动本机 Chrome，
不引入 puppeteer 等额外依赖。其中地图检查会用 CDP 的 `Fetch` 域在
**网络层**伪造后端响应——因为本地预览没有后端，而 JS 层替换
`XMLHttpRequest` 对 axios 不可靠（axios 会直接读实例上的
`readyState` / `status` / `response` 并依赖内部事件时序）。

也可以简写为：

```bash
pnpm check:lazy
pnpm check:map
```

---

## 六、第三轮：预渲染（首屏 HTML 直出）+ History 路由

前两轮解决了「下载多少」的问题，但 HTML 始终是空壳：
所有文字都要等 JS 下载、解析、执行完才出现。3G 下这段时间是纯白屏。

第三轮把这件事彻底解决：**12 个静态页面在构建期就渲染成完整 HTML**，
用户和爬虫在 JS 到达前就能读到正文。

### 6.1 为什么必须先换成 History 路由

原先是 hash 路由（`/#/ethnic`）。实测线上：

```
GET /ethnic       → HTTP 404
GET /about        → HTTP 404
GET /#/ethnic     → HTTP 200（但服务端只看到 /）
```

`#` 之后的内容**不会发给服务器**，服务端永远只看到 `/`，因此：

- 无法针对具体页面做 SSR / 预渲染（不知道该渲染哪个页面）；
- 搜索引擎只能看到 1 个 URL —— 整站对爬虫而言只有一个页面。

所以第一步是把 `createWebHashHistory()` 换成 `createWebHistory()`，
并同步改掉两处硬编码的 `window.location.hash` 跳转
（`pages/sports`、`pages/autonomous`，改用 `router.push`）。

**代价**：必须给 nginx 加 SPA 兜底，否则刷新子页面会 404。配置见 6.6。

### 6.2 预渲染做了什么

`pnpm build` 现在是三步：

```bash
vue-tsc -b && vite build                                   # 1. 客户端常规构建
vite build --ssr src/entry-server.ts --outDir dist-ssr     # 2. 构建 SSR 产物
node scripts/prerender.mjs                                 # 3. 逐路由渲染并写回 dist
```

第 3 步对 12 个静态路由逐个执行 `renderToString`，把结果注入
对应的 `dist/<path>/index.html`，并写入该页面专属的
`<title>` / `description` / `canonical` / Open Graph。

产物形态：

```
dist/
  index.html                  ← 首页（已直出）
  _spa-shell.html             ← 空壳，动态路由兜底用
  about/index.html            ← 已直出
  ethnic/index.html           ← 已直出
  …（共 12 个）
  sitemap.xml / robots.txt    ← 自动生成
```

**为什么不做「带数据的完整 SSR」**：本项目所有取数都在 `onMounted` 里，
服务端不执行。要直出数据得把取数整体上移到 `setup` + 异步数据模式，
改动面覆盖全部 30 多个页面。预渲染静态骨架已经解决了两个核心问题
（慢网白屏、每页独立 URL 的 SEO），性价比最高。

### 6.3 踩到的坑（都已修复，值得记录）

这一轮几乎每个坑都会**静默地**让预渲染失效——HTML 里有内容，用户却看不到。
每条都在 `check:served`（禁用 JS 后检查可见文本）里加了回归。

| # | 现象 | 根因 | 修法 |
| --- | --- | --- | --- |
| 1 | `window is not defined` | `createWebHistory()` 创建时就读 `window` | 服务端改用 `createMemoryHistory()` |
| 2 | 同上 | 5 个 store 在**初始化时**读 `localStorage` | 全部加 `typeof localStorage !== "undefined"` 判断 |
| 3 | 同上 | `useWindowScroll` / `useElementSize` 在 setup 顶层调用 | 抽出 `isBrowser` 判断，服务端返回常量 ref |
| 4 | `getSSRProps` 报错 | 服务端没注册 `v-motion*` 指令 | 注册空指令（元素以最终可见态直出） |
| 5 | Element Plus 警告 | 缺 SSR 的 id / z-index provider，会导致 hydration 不一致 | `app.provide(ID_INJECTION_KEY / ZINDEX_INJECTION_KEY)` |
| 6 | **正文不可见** | `<Transition>` 把 `opacity: 0` 写进了直出 HTML | 首帧不套 Transition，挂载后再启用 |
| 7 | **正文被遮住** | `#boot-skeleton` 是 `position: fixed` 全屏遮罩，靠 JS 移除 | 改用 CSS `#app:empty + #boot-skeleton` 判定，不依赖 JS |
| 8 | 子页面变成了首页 | 重复执行 prerender 时，把「已直出的首页」当成模板 | 复用 `_spa-shell.html` 作为干净模板，并校验模板纯净 |
| 9 | 页面卡住不渲染 | `scrollBehavior` 里 `querySelector('#/ethnic')` 是非法选择器，直接抛错 | try/catch 包住锚点查询（旧 hash 链接会走到这里） |
| 10 | 旧链接失效 | 换 History 后 `/#/ethnic` 不再是路由 | `main.ts` 启动时把 `#/xxx` 改写为 `/xxx` |
| 11 | **dev 图片全部裂开** | 把整个 `/images/` 当成"前端自带资源"，把后端的 `/images/ethnic/...` 也指到了 dev server | 改为白名单前缀 `/images/placeholders/`（`isLocalAsset()`） |

**第 11 条是一个上线后才会暴露、但由本地开发先发现的回归。**
第一轮为了让占位图同源直出，我把整个 `/images/` 前缀都当作本地资源；
但 `public/` 下只有 `images/placeholders/`，真实内容图
（`/images/ethnic/...`、`/images/topic/...`）由后端提供。
后果是 dev 下 Vite 把未知路径回退成 `index.html`
（**HTTP 200 + Content-Type: text/html**），浏览器解码失败 → 图片全裂；
线上则会直接 404。

这个坑的教训：**判断"图片是否加载成功"不能看 HTTP 状态码**——
回退成 HTML 时状态码同样是 200。`scripts/check-dev-images.mjs`
因此用 `naturalWidth > 0` 作为判据。

第 6、7 条尤其隐蔽：**爬虫能读到内容，人却看不到**。如果只检查 HTML 源码
或只看正常联网的截图，这两个问题都不会暴露。第 8、9 条属于「第二次跑才出问题」
与「旧链接才触发」的类型，单次手动验证同样测不到 —— 只能靠自动化回归兜住。

### 6.4 效果

以下数据来自**禁用 JS** 后渲染的截图（等价于「JS 还没到达」的慢网首屏）：

| 页面 | 直出 HTML | 无 JS 可见内容 |
| --- | --- | --- |
| `/` | 15.2 KB | 导航 + Hero 标题正文 + 三个栏目 + 页脚 |
| `/about` | 12.7 KB | 全文正文 + 配图 + 数据统计表 |
| `/ethnic` | 11.2 KB | 标题 + 导语 + 全部筛选条件 + 视图切换 |
| `/unity`、`/heritage`、`/festival` 等 9 页 | 6.6–11.8 KB | 标题 + 栏目脚手架 + 导航高亮 + 页脚 |

上表由 `scripts/shot-nojs.mjs` 在 `Emulation.setScriptExecutionDisabled`
下截图核对，确认是**肉眼可见**的正文，而不只是 HTML 源码里有字。

首屏体验的变化：

| 阶段 | 3G 平均（780 kbps）看到内容的时间 |
| --- | --- |
| 最初 | 6.3 s（纯白） |
| 第一轮后 | 1.8 s（先看到骨架，再等 JS） |
| **第三轮后** | **≈0.4 s（HTML 一到就有正文，JS 只做水合）** |

> 预渲染不减少字节，减少的是「必须等 JS 才能看到内容」这段空白。
> 首屏 JS（102 KB brotli）仍要下载，但它现在只影响**可交互时间**，
> 不再影响「看到内容」的时间。

### 6.5 自动化验证

```bash
pnpm build

# 终端 A：模拟 nginx 行为的静态服务器
pnpm serve:dist 4500

# 终端 B：
pnpm check:prerender                              # 产物自检（91 项）
pnpm check:served  http://127.0.0.1:4500          # 禁用 JS 后正文是否可见（26 项）
pnpm check:hydrate http://127.0.0.1:4500          # 水合是否干净（24 项）
pnpm check:legacy  http://127.0.0.1:4500          # 旧 hash 链接兼容（12 项）
pnpm check:lazy    http://127.0.0.1:4500          # 弹窗/抽屉仍可唤起（6 项）
pnpm check:map     http://127.0.0.1:4500          # 地图数据仍按需加载（6 项）

# 开发环境（另开一个 pnpm dev，默认 http://localhost:5173）：
pnpm check:devimg  http://localhost:5173          # 图片是否真的解码成功（6 项）
```

合计 **171 项**自动检查，覆盖「内容可见 / 水合正确 / 交互不回归 / 图片可用」
四类风险。

**注意不要用 `vite preview` 验证预渲染**：它不认识「目录下的 index.html」，
对 `/about` 会回退到根 index.html，于是子页面怎么测都显示首页
（排查时确实被这一点误导过）。`scripts/serve-dist.mjs` 复刻了 nginx 的
`try_files $uri $uri/ /index.html` 行为，并支持 `.br` / `.gz` 预压缩产物。

`check:hydrate` 检查的是：水合必须复用直出 DOM，而不是丢弃重建——
一旦出现 hydration mismatch，Vue 会丢掉整棵服务端 DOM 重画，
预渲染的收益会被完全抹掉（用户看到内容和闪一下重建）。

### 6.6 服务器配置（History 路由必需）

```nginx
server {
    listen 443 ssl;
    http2 on;
    root /var/www/czdr.work;
    index index.html;

    # ★ History 路由的关键：找不到文件就回退到对应目录的 index.html。
    #   没有这一条，用户刷新 /about 会直接 404。
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 预渲染出来的子页面：优先命中 dist/<path>/index.html
    # （try_files 的 $uri/ 已经覆盖，这里无需额外规则）

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
        access_log off;
    }

    # sitemap / robots 用 text/plain 与 xml 正确返回
    location = /sitemap.xml { types {} default_type application/xml; }
    location = /robots.txt  { types {} default_type text/plain; }

    brotli_static on;
    gzip_static on;
}
```

**部署时务必带上 `_spa-shell.html`**。它是动态路由（`/ethnic/<uuid>` 等）
的兜底页面；缺了它，详情页只有在用户从站内点击进入时才能打开，
直接访问或刷新会拿到首页内容（本项目的兜底即 `try_files ... /index.html`，
而 `index.html` 现在已被首页占用）。

> 若希望动态详情页也有干净的兜底壳，可把最后一段改成
> `try_files $uri $uri/ /_spa-shell.html;`，
> 再把首页的精确匹配单独处理：`location = / { try_files /index.html =404; }`。

### 6.7 已知取舍

- **英文界面不预渲染**：预渲染产物只有中文一份，语言偏好存在
  `localStorage`，服务端读不到。切到英文后由客户端渲染，功能完全正常。
- **动态详情页仍是 SPA**：`/ethnic/:id` 等路由数量随内容增长，
  构建期无法穷举。它们返回空壳 + 骨架，交互与之前一致。
- **首次进入页面没有路由淡入动画**：本来也不该有；站内路由切换的
  淡入淡出完全保留。

---

## 七、下一步可选项（尚未做）

按收益排序：

1. **给动态详情页做按需 SSR**：目前详情页仍是空壳。若要进一步改善，
   可以把 `EthnicMap` 之外的详情页取数上移到 `setup`，
   配合 `entry-server` 在 Node 侧直接调后端接口，实现真正的按需 SSR。
   注意需要后端在构建/运行期可达。
2. **内容变更时重新预渲染**：目前预渲染发生在构建期。
   若后端内容频繁更新，可加一个「内容发布 → 触发重建」的钩子，
   或把预渲染改造成运行期的按需渲染 + 缓存。
3. **继续压缩首屏 JS**：剩余 102 KB（brotli）里，`vendor-vue` 约 38 KB、
   业务/EP 约 64 KB。预渲染之后，首屏 JS 只影响**可交互时间**，
   不再影响「看到内容」的时间，优先级已经下降。
4. **字体子集化**：若后续接入中文字体，务必做子集化（动辄数 MB）。
5. **`china-provinces.json` 再压缩**：目前 145 KB / brotli 27 KB，
   已做到「切到地图才加载」。若还要再小，可以进一步抽稀轮廓点或按需分省加载。
