/*
 * 图片署名回填脚本（方向 C-4）
 *
 * 从「数据库引用的图片路径」+「磁盘实际文件」两侧扫描，为每张图生成一条
 * image_credit 记录，全部标记 unverified（来源待核）。
 *
 * 为什么全部标 unverified：
 *   原始 Commons 文件名与许可元数据已丢失（文件被重命名为 cover.webp，
 *   采集清单 scripts/ 目录已删除，media_asset 表为空），无法把图片对应回来源页。
 *   本脚本**不编造作者与许可**，只如实登记「这张图存在、出处待核」。
 *
 * 与 generate.cjs 的区别：
 *   · 本脚本先从数据库读取各内容引用的图片路径，据此生成**有意义的 caption**
 *     （如「苗族 · 封面」「京剧 · 项目封面」），方便人工核实；
 *   · 未匹配到内容的图片兜底用文件名做 caption。
 */
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const APP = 'C:/codeDev/56/app';
const IMG_ROOT = path.join(APP, 'backend/src/main/resources/static/images');
const OUT = path.join(APP, 'backend/src/main/resources/db/seed-image-credits.sql');

const DIR_CONTEXT = {
  ethnic: {
    site: 'Wikimedia Commons（待核）',
    remark: '原采集自 Wikimedia Commons 民族分类（自由许可），但原始文件名与许可信息已丢失，需用反向图片搜索逐张核对来源页。'
  },
  festival: { site: '来源待核', remark: '节日专属封面；采集时的具体来源未记录在案，需核对。' },
  art: { site: '来源待核', remark: '艺术专属封面；采集时的具体来源未记录在案，需核对。' },
  food: { site: '来源待核', remark: '文档记录为「百度图搜来源，毕设展示用」，非自由许可图库，正式上线前需替换或取得授权。' },
  topic: { site: '来源待核', remark: '专题封面；采集时的具体来源未记录在案，需核对。' },
  placeholders: { site: '项目内置', remark: '占位图，取材自站内已有图片。' }
};

/**
 * 用 psql 执行查询，结果写入临时文件后读回。
 * 说明：`\copy (SELECT …) TO` 对多行 UNION 语句会解析失败，
 * 因此这里把查询存成文件、结果 TO 到另一个文件再读回。
 */
function query(sql) {
  const qFile = path.join(process.env.TEMP, 'credit_q.sql');
  const oFile = path.join(process.env.TEMP, 'credit_out.tsv').replace(/\\/g, '/');
  fs.writeFileSync(qFile, `\\copy (${sql}) TO '${oFile}' WITH (FORMAT csv, DELIMITER E'\\t', HEADER)`, 'utf8');
  execSync(
    `"C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe" -U postgres -h localhost -d 56_app -f "${qFile}"`,
    { env: { ...process.env, PGPASSWORD: '255810203' }, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }
  );
  const text = fs.readFileSync(path.join(process.env.TEMP, 'credit_out.tsv'), 'utf8');
  const lines = text.split(/\r?\n/).filter(Boolean);
  const header = lines.shift().split('\t');
  return lines.map((l) => {
    const cells = [];
    let cur = '', inQ = false;
    for (let i = 0; i < l.length; i++) {
      const c = l[i];
      if (inQ) {
        if (c === '"') { if (l[i + 1] === '"') { cur += '"'; i++; } else inQ = false; }
        else cur += c;
      } else {
        if (c === '"') inQ = true;
        else if (c === '\t') { cells.push(cur); cur = ''; }
        else cur += c;
      }
    }
    cells.push(cur);
    const o = {};
    header.forEach((h, i) => (o[h] = cells[i]));
    return o;
  });
}

function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, acc);
    else if (/\.(webp|jpg|jpeg|png)$/i.test(e.name) && !/-blur\./i.test(e.name)) acc.push(p);
  }
  return acc;
}

console.log('=== 1) 从数据库读取图片引用与内容名 ===');
// 注意：`\copy (…)` 不支持括号内跨行的 UNION 查询，故按表分别查询后合并
const SOURCES = [
  { sql: "SELECT cover_image AS img, name AS owner FROM ethnic_group WHERE cover_image IS NOT NULL AND cover_image <> ''", type: 'ethnic' },
  { sql: "SELECT image AS img, name AS owner FROM food WHERE image IS NOT NULL AND image <> ''", type: 'food' },
  { sql: "SELECT cover_image AS img, name AS owner FROM festival WHERE cover_image IS NOT NULL AND cover_image <> ''", type: 'festival' },
  { sql: "SELECT cover_image AS img, name AS owner FROM art WHERE cover_image IS NOT NULL AND cover_image <> ''", type: 'art' },
  { sql: "SELECT cover_image AS img, title AS owner FROM topic WHERE cover_image IS NOT NULL AND cover_image <> ''", type: 'topic' }
];
const ethnicImgs = [];
for (const s of SOURCES) {
  const rows = query(s.sql);
  for (const r of rows) ethnicImgs.push({ img: r.img, owner: r.owner, ttype: s.type });
  console.log(`  ${s.type}: ${rows.length}`);
}
console.log('  数据库引用的图片记录合计:', ethnicImgs.length);

// 路径 -> 所有者的映射（一个路径可能被多条内容引用，保留第一个并计数）
const ownerMap = new Map();
for (const r of ethnicImgs) {
  if (!r.img) continue;
  const p = r.img.trim();
  if (!ownerMap.has(p)) ownerMap.set(p, []);
  ownerMap.get(p).push({ owner: r.owner, type: r.ttype });
}

console.log('\n=== 2) 扫描磁盘文件 ===');
const files = walk(IMG_ROOT);
console.log('  实际图片文件:', files.length);

const rows = files.map((f) => {
  const rel = f.replace(IMG_ROOT, '').replace(/\\/g, '/');
  const urlPath = '/images' + rel;
  const seg = rel.split('/').filter(Boolean);
  const dirKey = seg[0];
  const ctx = DIR_CONTEXT[dirKey] || { site: '来源待核', remark: '来源未记录，需核对。' };

  const refs = ownerMap.get(urlPath);
  let caption;
  if (refs && refs.length) {
    const names = [...new Set(refs.map((r) => r.owner))];
    const suffix = urlPath.includes('cover2') || urlPath.endsWith('-2.webp') ? '备选图' : '配图';
    caption = names.length === 1 ? `${names[0]} · ${suffix}` : `${names.slice(0, 2).join('、')} 等 · ${suffix}`;
  } else {
    // 未被数据库引用的图片（如未被选用的备选图），用文件名兜底
    caption = seg[seg.length - 1].replace(/\.(webp|jpg|png)$/i, '');
  }
  return { urlPath, dirKey, caption, ctx, refs: refs || [] };
});

const matched = rows.filter((r) => r.refs.length).length;
console.log('  能匹配到内容的图片:', matched, '/', rows.length);

const esc = (s) => String(s == null ? '' : s).replace(/'/g, "''");
const lines = [];
lines.push('-- ============================================================================');
lines.push('-- 图片署名回填（由 scripts/backfill-image-credits 扫描实际图片与数据库引用生成）');
lines.push('--');
lines.push('-- 全部记录标记为 unverified（来源待核）：');
lines.push('-- 原始 Commons 文件名与许可元数据已丢失，无法对应回来源页，故不编造作者与许可。');
lines.push('-- 逐张核实后，将 credit_status 改为 verified 并补齐 author / license / source_url，');
lines.push('-- 页面即自动显示真实署名。');
lines.push('--');
lines.push('-- 幂等：ON CONFLICT (image_path) DO NOTHING（不覆盖已人工核实的记录）。');
lines.push('-- ============================================================================');
lines.push('');

for (const r of rows) {
  const ttype = r.refs.length ? `'${esc(r.refs[0].type)}'` : 'NULL';
  lines.push(
    `INSERT INTO image_credit (image_path, target_type, caption, credit_status, source_site, attribution_required, remark) VALUES (` +
      `'${esc(r.urlPath)}', ${ttype}, '${esc(r.caption)}', 'unverified', '${esc(r.ctx.site)}', true, '${esc(r.ctx.remark)}') ` +
      `ON CONFLICT (image_path) DO NOTHING;`
  );
}

fs.writeFileSync(OUT, lines.join('\n') + '\n', 'utf8');
console.log('\n生成:', OUT, fs.statSync(OUT).size, 'bytes');
const byDir = {};
for (const r of rows) byDir[r.dirKey] = (byDir[r.dirKey] || 0) + 1;
console.log('按目录:', JSON.stringify(byDir));
