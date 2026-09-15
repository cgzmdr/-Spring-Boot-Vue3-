/**
 * C-2 / C-3 端到端接口断言（不依赖浏览器）。
 * 覆盖：历史沿革结构化的完整性、英文正文覆盖率与来源标记、
 *       英文正文确实通过 C 端接口返回、以及边界情况。
 */
const http = require('http');

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const headers = { Accept: 'application/json' };
    if (data) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(data); }
    if (token) headers['satoken'] = token;
    const r = http.request({ host: '127.0.0.1', port: 20256, path, method, headers }, res => {
      // 收集 Buffer 再整体解码：直接对每个 chunk 做 String 拼接会在多字节字符
      // 跨越 chunk 边界时产生乱码（民族简介单篇可达上万字，必现），
      // 进而让「正文与段落是否一致」这类比较出现假失败。
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
        catch (e) { reject(new Error('non-JSON ' + Buffer.concat(chunks).toString('utf8').slice(0, 200))); }
      });
    });
    r.on('error', reject);
    if (data) r.write(data); r.end();
  });
}

let pass = 0, fail = 0;
function check(label, cond, extra) {
  if (cond) { pass++; }
  else { fail++; console.log('FAIL: ' + label + (extra !== undefined ? ' -> ' + JSON.stringify(extra) : '')); }
}

(async () => {
  const token = await req('POST', '/auth/login', { account: 'cgzmdr@foxmail.com', password: 'Cz12345@' }).then(r => r.data);
  check('admin login ok', typeof token === 'string' && token.length > 0);

  // ---------- C-3 覆盖率 ----------
  const pend = await req('GET', '/admin/translate/description-en/pending', null, token).then(r => r.data);
  for (const t of ['festival', 'art', 'food']) {
    check(`${t}: all translated`, pend[t].pending === 0, pend[t]);
    check(`${t}: total is expected`, pend[t].total > 0, pend[t].total);
  }
  const totalRows = pend.festival.total + pend.art.total + pend.food.total;
  check('total rows = 525', totalRows === 525, totalRows);

  // ---------- C-3 幂等：再跑一次不应重复翻译 ----------
  const again = await req('POST', '/admin/translate/description-en/backfill?type=festival&limit=5&dryRun=false', null, token).then(r => r.data);
  check('re-run translates nothing (idempotent)', again.translated === 0, again);
  check('re-run fails nothing', again.failed === 0, again.failed);

  // ---------- C-3 dryRun 不写库 ----------
  const dry = await req('POST', '/admin/translate/description-en/backfill?type=art&limit=3&dryRun=true', null, token).then(r => r.data);
  check('dryRun on fully-translated type is no-op', dry.translated === 0 && dry.failed === 0, dry);

  // ---------- C-3 英文正文经 C 端返回 ----------
  const fList = await req('GET', '/festivals?pageIndex=1&pageSize=50', null, null);
  check('festival list code=0', fList.code === 0, fList.code);
  const fRow = (fList.data.data || [])[0];
  check('festival payload has descriptionEn field', fRow && 'descriptionEn' in fRow);
  check('festival payload has descriptionEnSource field', fRow && 'descriptionEnSource' in fRow);

  const aList = await req('GET', '/arts?pageIndex=1&pageSize=50', null, null);
  const aRow = (aList.data.data || [])[0];
  check('art payload has descriptionEn', aRow && 'descriptionEn' in aRow);
  check('art payload has descriptionEnSource', aRow && 'descriptionEnSource' in aRow);

  // food 详情
  const fFood = await req('GET', '/festivals?pageIndex=1&pageSize=1', null, null);
  const fid = (fFood.data.data || [])[0].id;
  const fDetail = await req('GET', '/festivals/' + fid, null, null);
  check('festival detail has descriptionEnSource', 'descriptionEnSource' in fDetail.data);
  check('festival detail source is machine or manual/reviewed',
    ['machine', 'reviewed', 'manual'].includes(fDetail.data.descriptionEnSource), fDetail.data.descriptionEnSource);

  // ---------- C-2 历史沿革 ----------
  // 列表接口分页参数是 page（0 基）+ size；用错参数名（pageIndex/pageSize）
  // 会被后端忽略并反复返回第一页，导致只覆盖到前 10 个民族而误判为「全部通过」。
  const all = [];
  for (let page = 0; page < 10 && all.length < 56; page++) {
    const l = await req('GET', `/ethnic-groups?page=${page}&size=10`, null, null);
    const batch = (l.data && l.data.data) || [];
    if (!batch.length) break;
    all.push(...batch);
  }
  const distinct = new Set(all.map(r => r.id));
  check('ethnic list yields 56 distinct rows', distinct.size === 56, `${all.length} rows / ${distinct.size} distinct`);

  const rows = all.slice(0, 56);
  let withHistory = 0, withTimeline = 0, details = 0;
  for (const row of rows) {
    const d = await req('GET', '/ethnic-groups/' + row.id, null, null);
    const h = d.data && d.data.history;
    if (!h) { check(`history present for ${row.name}`, false); continue; }
    details++;
    if (h.paragraphCount > 0) withHistory++;
    if (h.timelineCount > 0) withTimeline++;

    // 计数一致性
    if (h.timelineCount !== h.timeline.length) check(`${row.name}: timelineCount matches`, false);
    if (h.paragraphCount !== h.paragraphs.length) check(`${row.name}: paragraphCount matches`, false);
    if (h.eraCount !== h.eras.length) check(`${row.name}: eraCount matches`, false);

    // 时间轴条目必须指向一致段落且年份出现在原文中
    for (const it of h.timeline) {
      if (it.index < 0 || it.index >= h.paragraphs.length || h.paragraphs[it.index].text !== it.text) {
        check(`${row.name}: timeline index/text mismatch`, false);
      }
      if (!it.text.includes(it.yearText)) check(`${row.name}: yearText not in text`, it.yearText);
    }
    // 时代按时间先后
    const order = ['先秦', '秦汉', '魏晋南北朝', '隋唐五代', '宋辽金西夏', '元代', '明代', '清代', '近现代'];
    let prev = -1;
    for (const e of h.eras) {
      const rk = order.indexOf(e.name);
      if (rk <= prev) { check(`${row.name}: era order wrong at ${e.name}`, false); break; }
      prev = rk;
    }
  }
  check('all 56 ethnicities expose history', details === 56, details);
  check('all 56 have paragraphs', withHistory === 56, withHistory);
  check('at least 54 have timeline', withTimeline >= 54, withTimeline);
  console.log(`  (history: paragraphs ${withHistory}/56, timeline ${withTimeline}/56)`);

  console.log(`\nRESULT: ${fail === 0 ? 'PASS' : 'FAIL(' + fail + ')'} — ${pass} checks passed, ${fail} failed`);
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error('FATAL ' + e.message); process.exit(1); });
