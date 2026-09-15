/**
 * 方向 D 端到端接口断言（全文检索 + 推荐引擎）。
 * 覆盖：四类输入形式、分面、高亮、排序正确性、推荐依据与置信度、排除已读、边界情况。
 */
const http = require('http');

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const headers = { Accept: 'application/json' };
    if (data) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(data); }
    if (token) headers['satoken'] = token;
    const r = http.request({ host: '127.0.0.1', port: 20256, path, method, headers }, res => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
        catch (e) { reject(new Error('non-JSON: ' + Buffer.concat(chunks).toString('utf8').slice(0, 200))); }
      });
    });
    r.on('error', reject);
    if (data) r.write(data); r.end();
  });
}

let pass = 0, fail = 0;
function check(label, cond, extra) {
  if (cond) pass++;
  else { fail++; console.log('FAIL: ' + label + (extra !== undefined ? ' -> ' + JSON.stringify(extra) : '')); }
}
const enc = encodeURIComponent;

(async () => {
  // ============ 1. 索引完整性 ============
  const token = await req('POST', '/auth/login', { account: 'cgzmdr@foxmail.com', password: 'Cz12345@' }).then(r => r.data);
  const stats = await req('GET', '/admin/search-index/stats', null, token).then(r => r.data);
  check('索引覆盖 8 类内容', stats.byType.length === 8, stats.byType.length);
  check('索引总数 1142', stats.total === 1142, stats.total);
  const emptyTypes = stats.byType.filter(t => t.count === 0);
  check('无空类型', emptyTypes.length === 0, emptyTypes);

  // ============ 2. 四类输入形式 ============
  const cases = [
    ['中文精确', enc('蒙古族'), h => h.title === '蒙古族' && h.matchBy === 'title'],
    ['中文子串', enc('蒙古'), h => h.title.includes('蒙古')],
    ['拼音全拼', 'mengguzu', h => h.matchBy === 'pinyin'],
    ['拼音首字母', 'mgz', h => h.matchBy === 'abbr'],
    ['英文名', 'mongolian', h => h.matchBy === 'titleEn'],
  ];
  for (const [label, q, pred] of cases) {
    const r = await req('GET', `/search/full?q=${q}&size=5`).then(r => r.data);
    check(`${label}: 有结果`, r.list.length > 0, r.list.length);
    check(`${label}: 首条语义正确`, r.list.length > 0 && pred(r.list[0]),
      r.list[0] && `${r.list[0].title}/${r.list[0].matchBy}`);
    check(`${label}: 返回耗时`, typeof r.tookMs === 'number' && r.tookMs >= 0, r.tookMs);
  }

  // ============ 3. 排序正确性（回归守卫：曾出现同分导致目标排后） ============
  const py = await req('GET', '/search/full?q=mengguzu&size=10').then(r => r.data);
  check('拼音检索：蒙古族排第 1', py.list[0] && py.list[0].title === '蒙古族',
    py.list[0] && py.list[0].title);
  check('拼音检索：分数递减', py.list.every((h, i) => i === 0 || py.list[i - 1].score >= h.score));

  const en = await req('GET', '/search/full?q=mongolian&size=10').then(r => r.data);
  check('英文检索：蒙古族排第 1', en.list[0] && en.list[0].title === '蒙古族',
    en.list[0] && en.list[0].title);

  const zh = await req('GET', `/search/full?q=${enc('蒙古族')}&size=10`).then(r => r.data);
  check('中文检索：蒙古族排第 1', zh.list[0] && zh.list[0].title === '蒙古族',
    zh.list[0] && zh.list[0].title);

  // ============ 4. 高亮 ============
  const hl = await req('GET', `/search/full?q=${enc('刺绣')}&size=5`).then(r => r.data);
  const hasHl = hl.list.some(h => h.titleHtml.includes('<em class="hl">') || h.summaryHtml.includes('<em class="hl">'));
  check('关键词高亮生效', hasHl, hl.list[0] && hl.list[0].titleHtml);
  // 高亮必须是转义后的安全 HTML（不含裸标签注入）
  const unsafe = hl.list.some(h => /<(?!\/?em\b)[a-z]/i.test(h.titleHtml + h.summaryHtml));
  check('高亮 HTML 安全（仅 em 标签）', !unsafe);

  // ============ 5. 分面与过滤 ============
  const facet = await req('GET', `/search/full?q=${enc('藏族')}&size=5`).then(r => r.data);
  const facetSum = Object.values(facet.facets).reduce((a, b) => a + b, 0);
  check('分面计数之和 = 总数', facetSum === facet.total, `${facetSum} vs ${facet.total}`);
  const onlyArt = await req('GET', `/search/full?q=${enc('藏族')}&type=art&size=20`).then(r => r.data);
  check('类型过滤生效', onlyArt.list.every(h => h.docType === 'art'), onlyArt.list.map(h => h.docType));
  check('过滤后总数 = 分面中该类型数', onlyArt.total === facet.facets.art, `${onlyArt.total} vs ${facet.facets.art}`);

  // ============ 6. 空词与边界 ============
  const empty = await req('GET', '/search/full?q=&size=5').then(r => r.data);
  check('空词返回全部（按热度）', empty.total === 1142, empty.total);
  const noHit = await req('GET', '/search/full?q=' + enc('zzzz不存在的内容zzzz') + '&size=5').then(r => r.data);
  check('无结果时 total=0 且不报错', noHit.total === 0 && noHit.list.length === 0, noHit.total);
  const huge = await req('GET', '/search/full?q=' + enc('族') + '&size=999').then(r => r.data);
  check('size 上限被限制（<=50）', huge.list.length <= 50, huge.list.length);

  // ============ 7. 推荐：游客降级 ============
  const guest = await req('GET', '/recommend?size=5').then(r => r.data);
  check('游客推荐 basis=popularity', guest.basis === 'popularity', guest.basis);
  check('游客推荐如实说明无数据', guest.dataNote.includes('没有可用'), guest.dataNote);
  check('游客 confidence=0', guest.confidence === 0, guest.confidence);
  check('游客推荐有条目', guest.list.length > 0, guest.list.length);

  // ============ 8. 推荐：显式兴趣信号 ============
  const tags = await req('GET', '/interests/tags').then(r => r.data);
  check('标签四个维度', ['ethnic', 'region', 'type', 'topic'].every(d => tags[d] && tags[d].length > 0),
    Object.keys(tags));
  const zang = tags.ethnic.find(t => t.name === '藏族');
  const artT = tags.type.find(t => t.name === '艺术');
  await req('POST', '/interests/mine', { tagIds: [zang.id, artT.id] }, token);

  const personalized = await req('GET', '/recommend?size=8', null, token).then(r => r.data);
  // 该账号可能已累积浏览行为（前面的用例写过 user_behavior），
  // 此时「兴趣 + 行为」同时可用，basis 为 personalized 才是正确结果；
  // 只有确实没有任何行为时才应报 interest。
  const expectBasis = personalized.behaviorCount > 0 ? 'personalized' : 'interest';
  check(`有显式兴趣时 basis=${expectBasis}`, personalized.basis === expectBasis,
    `${personalized.basis} (behaviors=${personalized.behaviorCount})`);
  check('interestCount=2', personalized.interestCount === 2, personalized.interestCount);
  const zangHits = personalized.list.filter(i => i.ethnicName === '藏族').length;
  check('推荐包含所选民族内容', zangHits > 0, zangHits);
  check('推荐理由是「你关注了」', personalized.list.some(i => i.reason && i.reason.includes('你关注了')),
    personalized.list[0] && personalized.list[0].reason);

  // ============ 9. 推荐：隐式行为 + 排除已读 ============
  await req('POST', '/interests/mine', { tagIds: [] }, token);
  const s = await req('GET', `/search/full?q=${enc('苗族')}&size=10`).then(r => r.data);
  const browsed = s.list.filter(h => h.ethnicName === '苗族').slice(0, 4);
  for (const b of browsed) {
    await req('POST', '/behaviors/view', { targetType: b.docType, targetId: b.docId }, token);
  }
  const after = await req('GET', '/recommend?size=10', null, token).then(r => r.data);
  check('有行为后 basis=personalized', after.basis === 'personalized', after.basis);
  const browsedIds = new Set(browsed.map(b => b.docId));
  const repeated = after.list.filter(i => browsedIds.has(i.docId)).length;
  check('不重复推荐已浏览内容', repeated === 0, repeated);
  const miaoReco = after.list.filter(i => i.ethnicName === '苗族').length;
  check('推荐转向浏览过的民族', miaoReco > 0, miaoReco);
  check('confidence 随行为上升', after.confidence > 0, after.confidence);

  // ============ 10. 行为上报边界 ============
  const noAuth = await req('POST', '/behaviors/view', { targetType: 'ethnic', targetId: '00000000-0000-0000-0000-000000000001' }, null);
  check('未登录上报不报错（静默忽略）', noAuth.code === 0, noAuth.code);

  // ============ 11. 索引重建后仍完整 ============
  const rb = await req('POST', '/admin/search-index/rebuild', null, token).then(r => r.data);
  check('全量重建 1142', rb.total === 1142, rb.total);
  const afterRebuild = await req('GET', `/search/full?q=${enc('蒙古族')}&size=5`).then(r => r.data);
  check('重建后检索仍正常', afterRebuild.total > 0, afterRebuild.total);

  console.log(`\nRESULT: ${fail === 0 ? 'PASS' : 'FAIL(' + fail + ')'} — ${pass} checks passed, ${fail} failed`);
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error('FATAL ' + e.message); process.exit(1); });
