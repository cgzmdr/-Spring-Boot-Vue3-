/*
 * 输出图片署名待核清单（方向 C-4）
 *
 * 生成一份按目录分组的待办清单，供逐张用 Wikimedia Commons 反向图片搜索核实。
 * 输出为 CSV（Excel 可直接打开），列：序号/目录/说明/图片路径/本地文件/待核原因/核实状态
 */
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const APP = 'C:/codeDev/56/app';
const OUT = path.join(APP, 'docs/图片署名待核清单.csv');

const qFile = path.join(process.env.TEMP, 'pending_q.sql');
const oFile = path.join(process.env.TEMP, 'pending_out.tsv').replace(/\\/g, '/');
fs.writeFileSync(
  qFile,
  `\\copy (SELECT image_path, caption, source_site, remark, credit_status FROM image_credit WHERE credit_status <> 'verified' ORDER BY image_path) TO '${oFile}' WITH (FORMAT csv, DELIMITER E'\\t', HEADER)`,
  'utf8'
);
execSync(
  `"C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe" -U postgres -h localhost -d 56_app -f "${qFile}"`,
  { env: { ...process.env, PGPASSWORD: '255810203' }, encoding: 'utf8' }
);

const text = fs.readFileSync(path.join(process.env.TEMP, 'pending_out.tsv'), 'utf8');
const lines = text.split(/\r?\n/).filter(Boolean);
const header = lines.shift().split('\t');

function parseCsv(line) {
  const cells = [];
  let cur = '', inQ = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (inQ) {
      if (c === '"') { if (line[i + 1] === '"') { cur += '"'; i++; } else inQ = false; }
      else cur += c;
    } else {
      if (c === '"') inQ = true;
      else if (c === '\t') { cells.push(cur); cur = ''; }
      else cur += c;
    }
  }
  cells.push(cur);
  return cells;
}

const rows = lines.map(parseCsv).map((cells) => {
  const o = {};
  header.forEach((h, i) => (o[h] = cells[i]));
  return o;
});

// 按目录分组统计
const byDir = {};
for (const r of rows) {
  const d = r.image_path.split('/')[2] || 'other';
  byDir[d] = (byDir[d] || 0) + 1;
}

const csv = [];
csv.push('序号,目录,说明,图片路径,来源站点,待核原因,核实状态');
rows.forEach((r, i) => {
  const dir = r.image_path.split('/')[2] || 'other';
  const esc = (s) => `"${String(s == null ? '' : s).replace(/"/g, '""')}"`;
  csv.push([i + 1, esc(dir), esc(r.caption), esc(r.image_path), esc(r.source_site), esc(r.remark), esc(r.credit_status)].join(','));
});

fs.writeFileSync(OUT, '\ufeff' + csv.join('\n') + '\n', 'utf8');
console.log('待核清单已生成:', OUT);
console.log('总条数:', rows.length);
console.log('按目录:', JSON.stringify(byDir));
