/**
 * C-3 全量英文正文回填执行器。
 * 依次处理 festival / art / food，每类分批调用后台接口（limit=20/批），
 * 记录进度到 c3-progress.log，失败条目单独记录以便重跑。
 */
const http = require('http');
const fs = require('fs');

const LOG = __dirname + '/c3-progress.log';
function log(msg) {
  const line = '[' + new Date().toISOString() + '] ' + msg;
  console.log(line);
  fs.appendFileSync(LOG, line + '\n');
}

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const headers = { Accept: 'application/json' };
    if (data) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(data); }
    if (token) headers['satoken'] = token;
    const r = http.request({ host: '127.0.0.1', port: 20256, path, method, headers }, res => {
      let d = ''; res.on('data', c => d += c);
      res.on('end', () => { try { resolve(JSON.parse(d)); } catch (e) { reject(new Error('non-JSON: ' + d.slice(0, 200))); } });
    });
    r.on('error', reject);
    if (data) r.write(data);
    r.end();
  });
}

function pending(token) {
  return req('GET', '/admin/translate/description-en/pending', null, token).then(r => r.data);
}

(async () => {
  fs.writeFileSync(LOG, '');
  const token = await req('POST', '/auth/login', { account: 'cgzmdr@foxmail.com', password: 'Cz12345@' }).then(r => r.data);
  if (!token) throw new Error('login failed');
  log('login OK');

  const TYPES = ['festival', 'art', 'food'];
  const allFailures = [];
  const totals = {};

  for (const type of TYPES) {
    let done = 0, failed = 0, zh = 0, en = 0;
    const before = await pending(token);
    const target = before[type].pending;
    log(`=== ${type}: ${target} rows pending ===`);

    for (let round = 0; round < 100; round++) {
      const res = await req('POST', `/admin/translate/description-en/backfill?type=${type}&limit=25&dryRun=false`, null, token);
      if (res.code !== 0) { log(`${type} batch error: ${res.message}`); break; }
      const b = res.data;
      done += b.translated; failed += b.failed; zh += b.zhChars; en += b.enChars;
      (b.failures || []).forEach(f => allFailures.push({ type, name: f.name, error: f.error }));
      log(`${type} batch#${round + 1}: translated=${b.translated} failed=${b.failed} skipped=${b.skipped} (cum translated=${done})`);
      // 用「待译数是否下降」判断是否还有活干：
      // 批次里的 skipped 是已译行，属正常现象，不能据此判定结束。
      const now = await pending(token);
      const left = now[type].pending;
      log(`${type} remaining=${left}`);
      if (left === 0) { log(`${type} all done`); break; }
      if (b.translated === 0) { log(`${type} no progress (translated=0), stop`); break; }
    }
    const after = await pending(token);
    log(`=== ${type} DONE: translated=${done} failed=${failed} zhChars=${zh} enChars=${en} | remaining=${after[type].pending} ===`);
    totals[type] = { done, failed, zh, en, remaining: after[type].pending };
  }

  log('ALL DONE');
  log('totals: ' + JSON.stringify(totals));
  if (allFailures.length) {
    log('failures (' + allFailures.length + '):');
    allFailures.forEach(f => log(`  ${f.type} | ${f.name} | ${f.error}`));
    fs.writeFileSync(__dirname + '/c3-failures.json', JSON.stringify(allFailures, null, 2));
  }
  const fin = await pending(token);
  log('final pending: ' + JSON.stringify(fin));
})().catch(e => { log('FATAL ' + e.message); process.exit(1); });
