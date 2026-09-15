/*
 * 图片署名回填脚本（方向 C-4）
 *
 * 从「数据库引用的图片路径」+「磁盘上的实际文件」两侧扫描，
 * 为每张图片生成一条 image_credit 记录，全部标记为 unverified（来源待核）。
 *
 * 为什么全部标 unverified：
 *   原始 Commons 文件名与许可元数据已丢失（文件被重命名为 cover.webp，
 *   采集清单 scripts/ 目录已删除，media_asset 表为空），无法把图片对应回来源页。
 *   本脚本**不编造作者与许可**，只如实登记「这张图存在、出处待核」。
 *
 * 生成结果由人工或反向图片搜索逐张补齐为 verified 后，页面自动显示真实署名。
 */
const fs = require('fs');
const path = require('path');

const APP = 'C:/codeDev/56/app';
const IMG_ROOT = path.join(APP, 'backend/src/main/resources/static/images');
const OUT = path.join(APP, 'backend/src/main/resources/db/seed-image-credits.sql');

/**
 * 各目录的来源背景（来自 docs/56民族数据与图片资源说明.md 的实际记录）。
 * 注意：这是「目录级」的背景说明，不是逐图署名 —— 因此仍需标为待核，
 * 只在 remark 中如实记录该背景，帮助后续核实。
 */
const DIR_CONTEXT = {
  ethnic: {
    site: 'Wikimedia Commons（待核）',
    remark: '原采集自 Wikimedia Commons 民族分类（自由许可），但原始文件名与许可信息已丢失，需用反向图片搜索逐张核对来源页。'
  },
  festival: {
    site: '来源待核',
    remark: '节日专属封面；采集时的具体来源未记录在案，需核对。'
  },
  art: {
    site: '来源待核',
    remark: '艺术专属封面；采集时的具体来源未记录在案，需核对。'
  },
  food: {
    site: '来源待核',
    remark: '文档记录为「百度图搜来源，毕设展示用」，非自由许可图库，正式上线前需替换或取得授权。'
  },
  topic: {
    site: '来源待核',
    remark: '专题封面；采集时的具体来源未记录在案，需核对。'
  },
  placeholders: {
    site: '项目内置',
    remark: '占位图，取材自站内已有图片。'
  }
};

/** 递归收集实际图片文件（排除 -blur 模糊预览图） */
function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, acc);
    else if (/\.(webp|jpg|jpeg|png)$/i.test(e.name) && !/-blur\./i.test(e.name)) acc.push(p);
  }
  return acc;
}

const files = walk(IMG_ROOT);
console.log('扫描到实际图片文件:', files.length);

// 转成 URL 路径（与数据库 cover_image 字段一致）
const rows = files.map((f) => {
  const rel = f.replace(IMG_ROOT, '').replace(/\\/g, '/'); // /ethnic/miao/cover.webp
  const urlPath = '/images' + rel;
  const seg = rel.split('/').filter(Boolean);
  const dirKey = seg[0];
  const ctx = DIR_CONTEXT[dirKey] || { site: '来源待核', remark: '来源未记录，需核对。' };
  // caption：用倒数第二段（slug）与文件名组合，便于人工识别
  const caption = seg.length >= 3 ? `${seg[1]} / ${seg[seg.length - 1]}` : urlPath;
  return { urlPath, dirKey, caption, ctx };
});

const esc = (s) => String(s == null ? '' : s).replace(/'/g, "''");
const lines = [];
lines.push('-- ============================================================================');
lines.push('-- 图片署名回填（由 scripts 扫描实际图片生成，勿手工编辑）');
lines.push('--');
lines.push('-- 全部记录标记为 unverified（来源待核）：');
lines.push('-- 原始 Commons 文件名与许可元数据已丢失，无法对应回来源页，故不编造作者与许可。');
lines.push('-- 逐张核实后，将其 credit_status 改为 verified 并补齐 author / license / source_url，');
lines.push('-- 页面即自动显示真实署名。');
lines.push('--');
lines.push('-- 幂等：ON CONFLICT (image_path) DO NOTHING（不覆盖已人工核实的记录）。');
lines.push('-- ============================================================================');
lines.push('');

for (const r of rows) {
  lines.push(
    `INSERT INTO image_credit (image_path, caption, credit_status, source_site, attribution_required, remark) VALUES (` +
      `'${esc(r.urlPath)}', '${esc(r.caption)}', 'unverified', '${esc(r.ctx.site)}', true, '${esc(r.ctx.remark)}') ` +
      `ON CONFLICT (image_path) DO NOTHING;`
  );
}

fs.writeFileSync(OUT, lines.join('\n') + '\n', 'utf8');
console.log('生成 SQL:', OUT, fs.statSync(OUT).size, 'bytes');
const byDir = {};
for (const r of rows) byDir[r.dirKey] = (byDir[r.dirKey] || 0) + 1;
console.log('按目录:', JSON.stringify(byDir));
