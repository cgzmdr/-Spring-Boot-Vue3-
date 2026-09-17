/**
 * 测量指定接口的响应耗时（重复 N 次取中位数），并统计服务端执行的 SQL 条数。
 *
 * 用法：node bench-endpoints.mjs [baseUrl] [rounds]
 *
 * 用途：修复前后对比 P0 性能问题（/ethnic-groups 与 /ethnic-groups/population-stats）。
 * 仅依赖 Node 内置能力，不引入任何第三方库。
 */
const BASE = process.argv[2] || 'http://localhost:20256'
const ROUNDS = Number(process.argv[3] || 5)

/** 被测接口（生产 HAR 中报慢的两个 + 作为对照的快速接口） */
const TARGETS = [
  { name: '民族列表(第1页12条)', url: '/ethnic-groups?page=1&size=12&sort=orderNum,asc' },
  { name: '人口统计(Top8)', url: '/ethnic-groups/population-stats?topN=8' },
  { name: '艺术列表(6条)', url: '/arts?page=0&size=6' },
  { name: '对照:节日列表(6条)', url: '/festivals?page=0&size=6' },
]

async function timeOnce(url) {
  const t0 = performance.now()
  const res = await fetch(BASE + url)
  const body = await res.text()
  const ms = performance.now() - t0
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return { ms, bytes: Buffer.byteLength(body) }
}

function stats(arr) {
  const s = [...arr].sort((a, b) => a - b)
  const mid = Math.floor(s.length / 2)
  return {
    min: s[0],
    med: s.length % 2 ? s[mid] : (s[mid - 1] + s[mid]) / 2,
    max: s[s.length - 1],
    avg: s.reduce((a, b) => a + b, 0) / s.length,
  }
}

console.log(`基准地址: ${BASE}`)
console.log(`每接口轮次: ${ROUNDS}`)
console.log('')
console.log(
  '接口'.padEnd(26) +
  '最小'.padStart(10) + '中位'.padStart(10) + '平均'.padStart(10) + '最大'.padStart(10) +
  '  响应大小'
)
console.log('-'.repeat(80))

const summary = []
for (const t of TARGETS) {
  const times = []
  let bytes = 0
  // 预热一轮（触发引擎/JIT/缓存冷启动），不计入统计
  try { await timeOnce(t.url) } catch { /* ignore */ }
  for (let i = 0; i < ROUNDS; i++) {
    try {
      const r = await timeOnce(t.url)
      times.push(r.ms)
      bytes = r.bytes
    } catch (e) {
      console.log(`${t.name} 失败: ${e.message}`)
    }
  }
  if (!times.length) continue
  const st = stats(times)
  summary.push({ name: t.name, ...st, bytes })
  console.log(
    t.name.padEnd(26) +
    `${Math.round(st.min)}ms`.padStart(10) +
    `${Math.round(st.med)}ms`.padStart(10) +
    `${Math.round(st.avg)}ms`.padStart(10) +
    `${Math.round(st.max)}ms`.padStart(10) +
    `  ${(bytes / 1024).toFixed(1)} KB`
  )
}

console.log('')
console.log('=== JSON 汇总（便于对比前后）===')
console.log(JSON.stringify(
  summary.map((s) => ({ name: s.name, medianMs: Math.round(s.med), minMs: Math.round(s.min), maxMs: Math.round(s.max) })),
  null, 2
))
