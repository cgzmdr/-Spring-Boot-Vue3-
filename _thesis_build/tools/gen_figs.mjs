// Generate thesis figures as standalone HTML+SVG files, to be rasterised with Chrome.
import fs from 'node:fs';
import path from 'node:path';

const OUT = path.resolve('C:/codeDev/56/app/_thesis_build/figures');
fs.mkdirSync(OUT, { recursive: true });

const FONT = "'Microsoft YaHei','SimHei',sans-serif";
const C = {
  line: '#31445c',
  fill: '#eaf1fb',
  fill2: '#fdf1e3',
  fill3: '#e9f7ef',
  accent: '#c0392b',
  text: '#1f2937',
};

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function styleBlock() {
  return `<style>
    text{font-family:${FONT};fill:${C.text};}
    .t{font-size:15px}
    .ts{font-size:13px}
    .tb{font-size:16px;font-weight:bold}
    .tt{font-size:19px;font-weight:bold}
    .lbl{font-size:12px;fill:#5b6b7f}
  </style>`;
}

function multiline(cx, cy, text, cls = 't', lh = 19) {
  const lines = String(text).split('\n');
  const start = cy - ((lines.length - 1) * lh) / 2;
  return lines
    .map((l, i) => `<text class="${cls}" x="${cx}" y="${start + i * lh + 5}" text-anchor="middle">${esc(l)}</text>`)
    .join('');
}

function box(x, y, w, h, text, o = {}) {
  const fill = o.fill || C.fill;
  const rx = o.rx ?? 8;
  const sw = o.sw ?? 1.6;
  const parts = [`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" stroke="${o.stroke || C.line}" stroke-width="${sw}"/>`];
  if (text) parts.push(multiline(x + w / 2, y + h / 2, text, o.cls || 't', o.lh || 19));
  return parts.join('');
}

function stadium(x, y, w, h, text, o = {}) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${h / 2}" fill="${o.fill || C.fill3}" stroke="${C.line}" stroke-width="1.6"/>` +
    multiline(x + w / 2, y + h / 2, text, o.cls || 't');
}

function ellipse(cx, cy, rx, ry, text, o = {}) {
  return `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${o.fill || C.fill2}" stroke="${C.line}" stroke-width="1.5"/>` +
    multiline(cx, cy, text, o.cls || 'ts', 17);
}

function diamond(cx, cy, w, h, text, o = {}) {
  const pts = `${cx},${cy - h / 2} ${cx + w / 2},${cy} ${cx},${cy + h / 2} ${cx - w / 2},${cy}`;
  return `<polygon points="${pts}" fill="${o.fill || '#fff6e5'}" stroke="${C.line}" stroke-width="1.6"/>` +
    multiline(cx, cy, text, 'ts', 16);
}

function arrow(x1, y1, x2, y2, o = {}) {
  const dash = o.dash ? ' stroke-dasharray="6 5"' : '';
  const label = o.label
    ? `<text class="lbl" x="${(x1 + x2) / 2 + (o.lx ?? 8)}" y="${(y1 + y2) / 2 + (o.ly ?? -6)}" text-anchor="middle">${esc(o.label)}</text>`
    : '';
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${o.stroke || C.line}" stroke-width="1.4" marker-end="url(#ah)"${dash}/>${label}`;
}

function poly(pts, o = {}) {
  const d = pts.map((p, i) => `${i ? 'L' : 'M'}${p[0]},${p[1]}`).join(' ');
  return `<path d="${d}" fill="none" stroke="${o.stroke || C.line}" stroke-width="1.4" marker-end="url(#ah)"/>`;
}

function line(x1, y1, x2, y2, o = {}) {
  return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${o.stroke || C.line}" stroke-width="1.3"${o.dash ? ' stroke-dasharray="5 4"' : ''}/>`;
}

function head(w, h, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">
  <defs>
    <marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M0,0 L10,5 L0,10 z" fill="${C.line}"/>
    </marker>
  </defs>
  ${styleBlock()}
  ${body}
</svg>`;
}

function actor(x, y, label) {
  // stick figure, y = top of head
  return `
  <circle cx="${x}" cy="${y + 14}" r="13" fill="#fff" stroke="${C.line}" stroke-width="1.6"/>
  <line x1="${x}" y1="${y + 27}" x2="${x}" y2="${y + 66}" stroke="${C.line}" stroke-width="1.6"/>
  <line x1="${x - 20}" y1="${y + 42}" x2="${x + 20}" y2="${y + 42}" stroke="${C.line}" stroke-width="1.6"/>
  <line x1="${x}" y1="${y + 66}" x2="${x - 17}" y2="${y + 96}" stroke="${C.line}" stroke-width="1.6"/>
  <line x1="${x}" y1="${y + 66}" x2="${x + 17}" y2="${y + 96}" stroke="${C.line}" stroke-width="1.6"/>
  <text class="tb" x="${x}" y="${y + 122}" text-anchor="middle">${esc(label)}</text>`;
}

function write(name, w, h, body) {
  const html = `<!doctype html><html><head><meta charset="utf-8"><style>html,body{margin:0;padding:0;background:#fff}</style></head><body>${head(w, h, body)}</body></html>`;
  fs.writeFileSync(path.join(OUT, name + '.html'), html, 'utf8');
  return { name, w, h };
}

const figs = [];

/* ---------------- 图 3-1 普通用户用例图 ---------------- */
{
  const w = 1160, h = 620;
  const ax = 150;
  const items = [
    '浏览首页与专题',
    '检索民族 / 节日 / 艺术内容',
    '查看民族详情\n（风俗 · 节日 · 艺术 · 美食 · 聚居地）',
    '查看非遗名录与人物专栏',
    '查看民族自治地方与传统体育',
    '使用节日日历（农历换算）',
    '收藏 / 点赞 / 分享内容',
    '选择兴趣标签并获得推荐',
    '注册与登录 / 维护个人资料',
    '参与讨论区发帖与私信',
  ];
  let b = '';
  b += `<rect x="500" y="20" width="330" height="${h - 40}" rx="14" fill="#f7fafd" stroke="#b9c8da" stroke-width="1.4"/>`;
  b += `<text class="tb" x="665" y="48" text-anchor="middle">走进多彩 56 个民族世界（前台门户）</text>`;
  b += actor(ax, 240, '普通用户');
  const n = items.length;
  const top = 76, bottom = h - 50;
  const gap = (bottom - top) / n;
  items.forEach((t, i) => {
    const cy = top + gap * i + gap / 2;
    b += ellipse(665, cy, 218, 24, t, { cls: 'ts' });
    b += line(ax + 30, 300, 447, cy, { dash: false });
  });
  figs.push(write('fig3-1-usecase-user', w, h, b));
}

/* ---------------- 图 3-2 管理员用例图 ---------------- */
{
  const w = 1160, h = 660;
  const ax = 150;
  const items = [
    '登录后台并查看工作台统计',
    '民族内容维护（新增 / 编辑 / 删除 / 发布）',
    '节日 / 艺术 / 美食 / 风俗内容维护',
    '专题与人物档案维护',
    '民族自治地方与传统体育维护',
    '内容审批：提交、审核、修改、下线',
    'BPMN 流程建模与部署',
    '用户与角色权限管理（RBAC）',
    '讨论区治理与内容审核',
    '检索索引重建与兴趣标签维护',
    '翻译词表维护与图片署名核实',
  ];
  let b = '';
  b += `<rect x="500" y="20" width="330" height="${h - 40}" rx="14" fill="#f7fafd" stroke="#b9c8da" stroke-width="1.4"/>`;
  b += `<text class="tb" x="665" y="48" text-anchor="middle">56 民族 OA 中后台管理系统</text>`;
  b += actor(ax, 260, '管理员');
  const n = items.length;
  const top = 76, bottom = h - 50;
  const gap = (bottom - top) / n;
  items.forEach((t, i) => {
    const cy = top + gap * i + gap / 2;
    b += ellipse(665, cy, 228, 23, t, { cls: 'ts' });
    b += line(ax + 30, 320, 437, cy);
  });
  figs.push(write('fig3-2-usecase-admin', w, h, b));
}

/* ---------------- 图 4-1 系统架构图 ---------------- */
{
  const w = 1120, h = 700;
  let b = '';
  const L = 90, W = w - 2 * L;
  const layers = [
    { t: '客户端 / 展示层', h: 96, fill: '#eaf1fb', items: [['C 端门户（Vue 3 + TS + Vite）\nElement Plus · Pinia · Vue Router', 0], ['中后台（Vue 3 + Element Plus）\nECharts · bpmn-js · form-js', 1], ['移动端（React Native / Expo，可选）', 2]] },
    { t: '接口 / 安全层', h: 74, fill: '#fdf1e3', items: [['RESTful API（Spring MVC / Spring Boot 4）', 0], ['Sa-Token 统一鉴权 · RBAC 权限点 · CORS 白名单', 1], ['限流与参数校验 · 统一异常与响应封装', 2]] },
    { t: '业务服务层', h: 116, fill: '#e9f7ef', items: [['民族 / 节日 / 艺术 / 美食 / 风俗 内容服务', 0], ['专题 · 人物档案 · 自治地方 · 传统体育', 1], ['检索与推荐服务\n（索引 / 拼音 / 兴趣标签）', 2], ['讨论区 · 通知 · 私信 · 互动', 0], ['翻译服务\n（词表 + AI 兜底）', 1], ['内容审批工作流服务', 2]] },
    { t: '数据与引擎层', h: 96, fill: '#eef1f6', items: [['PostgreSQL 18\n业务数据 + 流程引擎 ACT_* 表', 0], ['Redis\n验证码 / 计数 / 缓存', 1], ['Camunda 7 流程引擎（嵌入式，同 JVM）', 2]] },
  ];
  let y = 34;
  b += `<text class="tt" x="${w / 2}" y="24" text-anchor="middle">系统总体架构</text>`;
  layers.forEach((ly, li) => {
    b += box(L, y, W, ly.h, '', { fill: ly.fill, rx: 10, sw: 1.5 });
    b += `<text class="tb" x="${L + 16}" y="${y + 24}" text-anchor="start">${esc(ly.t)}</text>`;
    const inner = ly.h - 40;
    const cw = (W - 32 - 2 * 14) / 3;
    ly.items.forEach(([txt, ci], i) => {
      const row = Math.floor(i / 3);
      const cx = L + 16 + ci * (cw + 14);
      const cy = y + 34 + row * (inner / Math.ceil(ly.items.length / 3) + 8);
      const ch = Math.min(inner / Math.ceil(ly.items.length / 3) - 8, 46);
      b += box(cx, cy, cw, ch, txt, { fill: '#ffffff', rx: 6, sw: 1.2, cls: 'ts', lh: 16 });
    });
    if (li < layers.length - 1) {
      b += arrow(L + W / 2, y + ly.h, L + W / 2, y + ly.h + 20);
      b += arrow(L + W / 2 - 34, y + ly.h + 20, L + W / 2 - 34, y + ly.h);
    }
    y += ly.h + 22;
  });
  figs.push(write('fig4-1-arch', w, h, b));
}

/* ---------------- 图 4-2 功能结构图 ---------------- */
{
  const w = 1180, h = 760;
  let b = '';
  b += box(w / 2 - 170, 16, 340, 46, '走进多彩 56 个民族世界', { fill: '#f7dfd8', cls: 'tb', rx: 23 });
  const groups = [
    { t: '门户浏览与检索', fill: '#eaf1fb', kids: ['首页与专题聚合', '民族频道与详情', '节日频道与日历', '传统艺术 / 非遗名录', '美食 / 风俗 / 民族语文', '全文检索（拼音 / 首字母）'] },
    { t: '文化资料专栏', fill: '#e9f7ef', kids: ['人物专栏（传承人 / 名家）', '民族自治地方', '民族传统体育', '民族服饰 / 民居建筑', '人口图谱与分布地图', '兴趣与个性化推荐'] },
    { t: '互动与个人中心', fill: '#fdf1e3', kids: ['注册登录 / 个人资料', '收藏 / 点赞 / 分享', '讨论区发帖与回复', '私信 / 通知 / 关注', '我的社区与举报', '中英文切换'] },
    { t: '内容运营后台', fill: '#eef1f6', kids: ['民族 / 节日 / 艺术维护', '人物 / 自治地方 / 体育维护', '专题与表单配置', '检索索引与兴趣标签', '翻译词表与图片署名', '内容来源维护'] },
    { t: '治理与系统管理', fill: '#f0eef9', kids: ['内容审批工作流待办', 'BPMN 流程建模与部署', '用户 / 角色权限管理', '讨论区治理与审核', '统计看板与反馈', '上传与文件管理'] },
  ];
  const colW = (w - 60 - 4 * 16) / 5;
  groups.forEach((g, i) => {
    const x = 30 + i * (colW + 16);
    b += box(x, 100, colW, 44, g.t, { fill: g.fill, cls: 'tb', rx: 8 });
    b += line(w / 2, 62, w / 2, 76);
    b += line(x + colW / 2, 76, w / 2, 76);
    b += line(x + colW / 2, 76, x + colW / 2, 100);
    g.kids.forEach((k, j) => {
      const ky = 160 + j * 56;
      b += box(x, ky, colW, 42, k, { fill: '#ffffff', rx: 6, sw: 1.2, cls: 'ts', lh: 16 });
      b += line(x + colW / 2, ky - 14, x + colW / 2, ky);
    });
    b += line(x + colW / 2, 144, x + colW / 2, 160);
  });
  figs.push(write('fig4-2-func-tree', w, h, b));
}

/* ---------------- 流程图工具 ---------------- */
function flow(name, steps) {
  const w = 940;
  const bw = 320;
  const bh = 46;
  const gap = 36;
  const cx = 300;
  let y = 34;
  const nodes = [];
  for (const s of steps) {
    const h = s.t === 'start' || s.t === 'end' ? 40 : s.t === 'dec' ? 84 : bh;
    nodes.push({ ...s, y, h });
    y += h + gap;
  }
  const H = y - gap + 34;
  let b = '';
  for (const nd of nodes) {
    if (nd.t === 'start' || nd.t === 'end') {
      b += stadium(cx - bw / 2, nd.y, bw, 40, nd.text, { fill: nd.t === 'start' ? '#e9f7ef' : '#f7dfd8' });
    } else if (nd.t === 'dec') {
      b += diamond(cx, nd.y + nd.h / 2, bw + 40, nd.h, nd.text);
    } else {
      b += box(cx - bw / 2, nd.y, bw, bh, nd.text, { cls: 'ts', lh: 17 });
    }
  }
  for (let i = 0; i < nodes.length - 1; i++) {
    const a = nodes[i];
    const c = nodes[i + 1];
    b += arrow(cx, a.y + a.h, cx, c.y, {
      label: a.t === 'dec' ? a.downLabel || '是' : '',
      lx: 13,
      ly: -6,
    });
  }
  // right-hand branches
  nodes.forEach((nd, i) => {
    if (!nd.branch) return;
    const bwd = 250;
    const bht = 58;
    const bx = cx + bw / 2 + 130;
    const by = nd.y + (nd.t === 'dec' ? nd.h / 2 - bht / 2 : 0);
    if (nd.terminal) {
      b += stadium(bx, by, bwd, bht, nd.branch, { fill: '#f7dfd8', cls: 'ts' });
    } else {
      b += box(bx, by, bwd, bht, nd.branch, { fill: '#fdf1e3', cls: 'ts', lh: 16 });
    }
    b += arrow(cx + (nd.t === 'dec' ? bw / 2 + 20 : bw / 2), nd.y + nd.h / 2, bx, by + bht / 2, {
      label: nd.branchLabel || '否',
      lx: 0,
      ly: -8,
    });
    if (nd.loop) {
      const prev = nodes[i - 1];
      const py = prev.y + prev.h / 2;
      const px = bx + bwd / 2;
      b += `<path d="M${px},${by} L${px},${py} L${cx + bw / 2},${py}" fill="none" stroke="${C.line}" stroke-width="1.4" stroke-dasharray="6 5" marker-end="url(#ah)"/>`;
      b += `<text class="lbl" x="${px + 8}" y="${(by + py) / 2}">${esc(nd.loopLabel || '返回修改')}</text>`;
    }
    if (nd.join) {
      const next = nodes[i + 1];
      const nx = bx + bwd / 2;
      const ny = next.y + next.h / 2;
      b += `<path d="M${nx},${by + bht} L${nx},${ny} L${cx + bw / 2},${ny}" fill="none" stroke="${C.line}" stroke-width="1.4" stroke-dasharray="6 5" marker-end="url(#ah)"/>`;
      b += `<text class="lbl" x="${nx + 8}" y="${(by + bht + ny) / 2}">${esc(nd.joinLabel || '继续')}</text>`;
    }
  });
  return write(name, w, H, b);
}

/* ---------------- 图 4-3 系统开发流程 ---------------- */
figs.push(flow('fig4-3-dev-flow', [
  { t: 'start', text: '开始' },
  { t: 'proc', text: '需求调研与分析\n（民族文化资料整理、用户角色与功能边界）' },
  { t: 'proc', text: '系统总体设计\n（架构选型、功能结构设计、数据库设计）' },
  { t: 'proc', text: '开发环境搭建\n（JDK 21 / PostgreSQL / Redis / Node.js 与 pnpm）' },
  { t: 'proc', text: '数据库建表与内容数据初始化' },
  { t: 'proc', text: '后端接口开发\n（内容服务、检索、推荐、审批工作流）' },
  { t: 'proc', text: '前端页面开发\n（C 端门户与中后台管理系统）' },
  { t: 'proc', text: '前后端联调与缺陷修复' },
  {
    t: 'dec',
    text: '功能与性能是否达标？',
    branch: '定位缺陷，修改代码并回归测试',
    loop: true,
    loopLabel: '返回修复',
  },
  { t: 'proc', text: '部署上线、编写使用与维护文档' },
  { t: 'end', text: '结束' },
]));

/* ---------------- 图 4-4 用户登录流程 ---------------- */
figs.push(flow('fig4-4-login-flow', [
  { t: 'start', text: '开始' },
  { t: 'proc', text: '打开登录页面，输入账号与密码' },
  {
    t: 'dec',
    text: '表单校验是否通过？',
    branch: '提示必填项缺失，返回表单',
    loop: true,
    loopLabel: '返回输入',
  },
  { t: 'proc', text: '提交登录请求（Sa-Token 统一鉴权）' },
  {
    t: 'dec',
    text: '账号与密码是否正确？',
    branch: '提示账号或密码错误',
    loop: true,
    loopLabel: '返回输入',
  },
  { t: 'proc', text: '签发 token，写入本地存储，加载用户信息与权限点' },
  { t: 'proc', text: '按角色跳转对应首页\n（前台门户 / 中后台工作台）' },
  { t: 'end', text: '结束' },
]));

/* ---------------- 图 4-5 系统操作流程 ---------------- */
figs.push(flow('fig4-5-operate-flow', [
  { t: 'start', text: '开始' },
  { t: 'proc', text: '用户访问系统，加载首页与全局导航' },
  {
    t: 'dec',
    text: '用户是否已登录？',
    branch: '以游客身份浏览公开内容',
    join: true,
    joinLabel: '继续浏览',
  },
  { t: 'proc', text: '进入内容频道，提交筛选或检索条件' },
  { t: 'proc', text: '后端按条件查询数据并返回结果集' },
  { t: 'proc', text: '前端渲染列表，用户进入详情页阅读' },
  {
    t: 'dec',
    text: '是否需要互动？\n（收藏 / 点赞 / 评论 / 分享）',
    branch: '仅浏览，不产生互动记录',
    terminal: true,
  },
  { t: 'proc', text: '校验登录状态，写入互动记录并更新计数' },
  { t: 'end', text: '结束' },
]));

/* ---------------- 图 4-6 添加信息流程 ---------------- */
figs.push(flow('fig4-6-add-flow', [
  { t: 'start', text: '开始' },
  { t: 'proc', text: '管理员在后台选择内容类型，点击「新增」' },
  { t: 'proc', text: '填写表单（名称、所属民族、分类、正文、图片等）' },
  {
    t: 'dec',
    text: '必填项与数据格式是否合法？',
    branch: '提示具体校验错误，返回表单',
    loop: true,
    loopLabel: '返回修改',
  },
  { t: 'proc', text: '提交保存，写入对应业务数据表' },
  { t: 'proc', text: '同步更新检索索引与统计口径' },
  { t: 'proc', text: '返回列表页并提示「添加成功」' },
  { t: 'end', text: '结束' },
]));

/* ---------------- 图 4-7 修改信息流程 ---------------- */
figs.push(flow('fig4-7-update-flow', [
  { t: 'start', text: '开始' },
  { t: 'proc', text: '在列表中选中记录，点击「编辑」' },
  { t: 'proc', text: '按主键读取记录并回填表单' },
  { t: 'proc', text: '修改字段内容并提交保存' },
  {
    t: 'dec',
    text: '修改后的数据是否合法？',
    branch: '提示错误信息，返回修改',
    loop: true,
    loopLabel: '返回修改',
  },
  { t: 'proc', text: '按主键执行更新，记录内容版本与操作日志' },
  {
    t: 'dec',
    text: '该内容是否已发布？',
    branch: '直接保存生效，无需审批',
    terminal: true,
  },
  { t: 'proc', text: '提交内容审批流程，审批通过后重新发布' },
  { t: 'end', text: '结束' },
]));

/* ---------------- 图 4-8 删除信息流程 ---------------- */
figs.push(flow('fig4-8-delete-flow', [
  { t: 'start', text: '开始' },
  { t: 'proc', text: '在列表中选中记录，点击「删除」' },
  { t: 'proc', text: '前端弹出二次确认对话框' },
  {
    t: 'dec',
    text: '是否确认删除？',
    branch: '取消本次删除操作',
    terminal: true,
  },
  { t: 'proc', text: '后端校验操作权限与关联数据（收藏 / 引用）' },
  {
    t: 'dec',
    text: '是否存在禁止删除的关联？',
    branch: '提示存在关联内容，禁止删除',
    terminal: true,
    branchLabel: '是',
    downLabel: '否',
  },
  { t: 'proc', text: '按主键删除记录，清理检索索引与关联表' },
  { t: 'proc', text: '刷新列表并提示「删除成功」' },
  { t: 'end', text: '结束' },
]));

/* ---------------- 图 4-9 数据库 E-R 图 ---------------- */
{
  const w = 1180, h = 780;
  let b = '';
  const ent = (x, y, w2, title, attrs, fill) => {
    const hh = 26 + attrs.length * 17 + 8;
    let s = `<rect x="${x}" y="${y}" width="${w2}" height="${hh}" rx="6" fill="${fill || '#ffffff'}" stroke="${C.line}" stroke-width="1.5"/>`;
    s += `<rect x="${x}" y="${y}" width="${w2}" height="26" rx="6" fill="${C.line}"/>`;
    s += `<text class="ts" x="${x + w2 / 2}" y="${y + 18}" text-anchor="middle" fill="#fff" font-weight="bold">${esc(title)}</text>`;
    attrs.forEach((a, i) => {
      s += `<text class="lbl" x="${x + 10}" y="${y + 44 + i * 17}" text-anchor="start">${esc(a)}</text>`;
    });
    return s;
  };
  b += ent(40, 60, 210, 'ethnic_group（民族）', ['PK id', 'name 名称 / pinyin 拼音', 'population 人口', 'language_family 语系', 'region 聚居地 / description'], '#eaf1fb');
  b += ent(40, 300, 210, 'food（美食）', ['PK id', 'ethnic_group_id FK', 'name / description', 'cooking_method'], '#e9f7ef');
  b += ent(40, 480, 210, 'ethnic_custom（风俗）', ['PK id', 'ethnic_group_id FK', 'custom_type / content'], '#e9f7ef');
  b += ent(340, 40, 210, 'festival（节日）', ['PK id', 'ethnic_group_id FK', 'type 类型', 'solar_date / lunar_date', 'origin / customs'], '#fdf1e3');
  b += ent(340, 300, 210, 'art（传统艺术 / 非遗）', ['PK id', 'ethnic_group_id FK', 'category 类别', 'heritage_level 级别', 'inheritors 传承人'], '#fdf1e3');
  b += ent(340, 500, 210, 'ethnic_location（聚居地）', ['PK id', 'ethnic_group_id FK', 'province / city', 'longitude / latitude'], '#e9f7ef');
  b += ent(640, 40, 220, 'person_profile（人物）', ['PK id', 'name / ethnic_group_id', 'role_type 角色', 'field 领域'], '#f0eef9');
  b += ent(640, 240, 220, 'autonomous_area（自治地方）', ['PK id', 'name / level 级别', 'ethnic_groups 自治民族', 'province / establish_year'], '#f0eef9');
  b += ent(640, 440, 220, 'traditional_sport（传统体育）', ['PK id', 'name / category', 'ethnic_groups', 'sub_items 子项'], '#f0eef9');
  b += ent(930, 40, 210, 'user_account（用户）', ['PK id', 'account / password_hash', 'nickname / avatar', 'status 状态'], '#f7dfd8');
  b += ent(930, 240, 210, 'role / permission', ['PK role.id', 'code / name', 'FK permission_id'], '#f7dfd8');
  b += ent(930, 400, 210, 'favorite / like_record', ['PK id', 'user_id FK', 'source_table / source_id'], '#f7dfd8');
  b += ent(930, 570, 210, 'search_document（检索索引）', ['PK id', 'doc_type / doc_id', 'title / content', 'pinyin / initials'], '#eef1f6');
  b += ent(340, 690, 210, 'topic / topic_entry（专题）', ['PK id', 'title / cover', 'entry_type / entry_id'], '#eef1f6');
  // relationships
  const rel = (x1, y1, x2, y2, label) => `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${C.line}" stroke-width="1.3"/><text class="lbl" x="${(x1 + x2) / 2}" y="${(y1 + y2) / 2 - 4}" text-anchor="middle">${esc(label)}</text>`;
  b += rel(250, 120, 340, 130, '1 : n');
  b += rel(250, 330, 340, 340, '1 : n');
  b += rel(250, 520, 340, 350, '1 : n');
  b += rel(250, 120, 250, 300, '1 : n');
  b += rel(250, 120, 250, 480, '1 : n');
  b += rel(550, 120, 640, 90, '1 : n');
  b += rel(550, 340, 640, 280, '1 : n');
  b += rel(550, 540, 640, 480, '1 : n');
  b += rel(250, 150, 930, 120, '1 : n');
  b += rel(930, 150, 930, 240, '');
  b += rel(930, 190, 930, 400, '');
  b += rel(340, 720, 640, 520, '1 : n');
  figs.push(write('fig4-9-er', w, h, b));
}

console.log(JSON.stringify(figs, null, 1));
