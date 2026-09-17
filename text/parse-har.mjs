/**
 * HAR 解析：把生产环境的网络抓包整理成可读的性能报告。
 *
 * 用法：node parse-har.mjs <har 文件路径>
 *
 * 输出：
 *   1. 概览（总请求数、总耗时、总传输量、页面加载指标）
 *   2. 慢请求 Top N（按耗时）
 *   3. 大响应 Top N（按传输字节）
 *   4. 按资源类型/域名聚合
 *   5. 状态码分布与失败请求
 *   6. 后端接口（/api 或同源接口）耗时明细
 *   7. 缓存命中情况
 *   8. 可优化点（自动诊断）
 */
import { readFileSync } from 'node:fs'

const file = process.argv[2]
if (!file) {
  console.error('用法: node parse-har.mjs <har 文件路径>')
  process.exit(1)
}

const har = JSON.parse(readFileSync(file, 'utf8'))
const log = har.log
const entries = log.entries || []

const fmtMs = (v) => (v == null ? '-' : `${Math.round(v)} ms`)
const fmtKB = (v) => (v == null ? '-' : `${(v / 1024).toFixed(1)} KB`)
const fmtMB = (v) => `${(v / 1024 / 1024).toFixed(2)} MB`

/** 短路径显示：去掉 query 太长时截断 */
function shortUrl(u) {
  try {
    const x = new URL(u)
    const p = x.pathname + (x.search || '')
    return p.length > 90 ? p.slice(0, 90) + '…' : p
  } catch {
    return u.length > 90 ? u.slice(0, 90) + '…' : u
  }
}
function hostOf(u) {
  try { return new URL(u).host } catch { return '(invalid)' }
}
function extOf(u) {
  try {
    const p = new URL(u).pathname
    const m = p.match(/\.([a-z0-9]+)$/i)
    if (m) return m[1].toLowerCase()
    if (p.includes('/api/')) return 'api'
    return '(none)'
  } catch { return '(invalid)' }
}

console.log('='.repeat(78))
console.log('HAR 解析报告')
console.log('='.repeat(78))
console.log(`文件      : ${file}`)
console.log(`抓包工具  : ${log.creator?.name} ${log.creator?.version}`)
console.log(`HAR 版本  : ${log.version}`)
console.log(`记录条数  : ${entries.length}`)

// ---------------------------------------------------------------- 页面级指标
console.log('\n' + '-'.repeat(78))
console.log('【1】页面加载指标')
console.log('-'.repeat(78))
for (const p of log.pages || []) {
  console.log(`页面      : ${p.title}`)
  console.log(`开始时间  : ${p.startedDateTime}`)
  console.log(`DOM 内容加载 (onContentLoad): ${fmtMs(p.pageTimings?.onContentLoad)}`)
  console.log(`完全加载     (onLoad)       : ${fmtMs(p.pageTimings?.onLoad)}`)
}
if (!log.pages?.length) console.log('（HAR 未记录 pages 段）')

// 时间范围
if (entries.length) {
  const times = entries.map((e) => new Date(e.startedDateTime).getTime()).filter((n) => !isNaN(n))
  const first = Math.min(...times)
  const last = Math.max(...times.map((t, i) => t + (entries[i].time || 0)))
  console.log(`抓包时间跨度: ${new Date(first).toISOString()} → ${new Date(last).toISOString()}`)
  console.log(`             共 ${((last - first) / 1000).toFixed(1)} 秒`)
}

// ---------------------------------------------------------------- 概览
const totalTime = entries.reduce((s, e) => s + (e.time || 0), 0)
const totalSize = entries.reduce((s, e) => s + (e.response?.content?.size || 0), 0)
const totalTransferred = entries.reduce((s, e) => s + (e.response?._transferSize || 0), 0)

console.log('\n' + '-'.repeat(78))
console.log('【2】总体概览')
console.log('-'.repeat(78))
console.log(`请求总数      : ${entries.length}`)
console.log(`耗时累计      : ${fmtMs(totalTime)}（所有请求相加，非墙钟时间）`)
const maxEnd = Math.max(...entries.map((e) => new Date(e.startedDateTime).getTime() + (e.time || 0)))
const minStart = Math.min(...entries.map((e) => new Date(e.startedDateTime).getTime()))
console.log(`墙钟总时长    : ${fmtMs(maxEnd - minStart)}`)
console.log(`响应体总量    : ${fmtMB(totalSize)}（解压后）`)
if (totalTransferred) console.log(`实际传输量    : ${fmtMB(totalTransferred)}（含压缩，网络实传）`)

// ---------------------------------------------------------------- 慢请求
console.log('\n' + '-'.repeat(78))
console.log('【3】最慢请求 Top 15')
console.log('-'.repeat(78))
console.log(pad('耗时', 10) + pad('状态', 7) + pad('类型', 8) + pad('大小', 11) + 'URL')
const slow = [...entries].sort((a, b) => (b.time || 0) - (a.time || 0)).slice(0, 15)
for (const e of slow) {
  console.log(
    pad(fmtMs(e.time), 10) +
    pad(String(e.response?.status ?? '-'), 7) +
    pad(extOf(e.request.url), 8) +
    pad(fmtKB(e.response?.content?.size), 11) +
    shortUrl(e.request.url)
  )
}

// ---------------------------------------------------------------- 大响应
console.log('\n' + '-'.repeat(78))
console.log('【4】响应体最大 Top 15')
console.log('-'.repeat(78))
console.log(pad('大小', 11) + pad('耗时', 10) + pad('类型', 8) + 'URL')
const big = [...entries].sort((a, b) => (b.response?.content?.size || 0) - (a.response?.content?.size || 0)).slice(0, 15)
for (const e of big) {
  console.log(
    pad(fmtKB(e.response?.content?.size), 11) +
    pad(fmtMs(e.time), 10) +
    pad(extOf(e.request.url), 8) +
    shortUrl(e.request.url)
  )
}

// ---------------------------------------------------------------- 按类型
console.log('\n' + '-'.repeat(78))
console.log('【5】按资源类型聚合')
console.log('-'.repeat(78))
console.log(pad('类型', 10) + pad('数量', 7) + pad('总耗时', 12) + pad('平均', 11) + pad('总量', 12) + '最慢')
const byType = {}
for (const e of entries) {
  const t = extOf(e.request.url)
  byType[t] ??= { n: 0, time: 0, size: 0, max: 0, maxUrl: '' }
  const b = byType[t]
  b.n++
  b.time += e.time || 0
  b.size += e.response?.content?.size || 0
  if ((e.time || 0) > b.max) { b.max = e.time || 0; b.maxUrl = shortUrl(e.request.url) }
}
for (const [t, b] of Object.entries(byType).sort((a, c) => c[1].time - a[1].time)) {
  console.log(
    pad(t, 10) + pad(String(b.n), 7) + pad(fmtMs(b.time), 12) +
    pad(fmtMs(b.time / b.n), 11) + pad(fmtKB(b.size), 12) + b.maxUrl
  )
}

// ---------------------------------------------------------------- 按域名
console.log('\n' + '-'.repeat(78))
console.log('【6】按域名聚合')
console.log('-'.repeat(78))
const byHost = {}
for (const e of entries) {
  const h = hostOf(e.request.url)
  byHost[h] ??= { n: 0, time: 0, size: 0 }
  byHost[h].n++
  byHost[h].time += e.time || 0
  byHost[h].size += e.response?.content?.size || 0
}
for (const [h, b] of Object.entries(byHost).sort((a, c) => c[1].time - a[1].time)) {
  console.log(`${pad(h, 30)} 请求 ${pad(String(b.n), 5)} 耗时 ${pad(fmtMs(b.time), 11)} 总量 ${fmtKB(b.size)}`)
}

// ---------------------------------------------------------------- 状态码
console.log('\n' + '-'.repeat(78))
console.log('【7】状态码分布')
console.log('-'.repeat(78))
const byStatus = {}
for (const e of entries) {
  const s = e.response?.status ?? 0
  byStatus[s] = (byStatus[s] || 0) + 1
}
for (const [s, n] of Object.entries(byStatus).sort((a, c) => c[1] - a[1])) {
  const flag = s >= 400 || s === 0 ? '  <== 异常' : ''
  console.log(`  ${s} : ${n}${flag}`)
}

// 失败请求详情
const bads = entries.filter((e) => (e.response?.status ?? 0) >= 400 || e.response?.status === 0)
if (bads.length) {
  console.log('\n失败请求明细：')
  for (const e of bads.slice(0, 25)) {
    console.log(`  [${e.response?.status ?? 'ERR'}] ${fmtMs(e.time)}  ${e.request.method} ${shortUrl(e.request.url)}`)
    if (e.response?._error) console.log(`        error: ${e.response._error}`)
    if (e.response?.statusText && e.response.statusText !== 'OK') console.log(`        statusText: ${e.response.statusText}`)
  }
}

// ---------------------------------------------------------------- 接口明细
console.log('\n' + '-'.repeat(78))
console.log('【8】接口请求明细（XHR/fetch 或 /api 路径）')
console.log('-'.repeat(78))
const apis = entries.filter((e) =>
  e._resourceType === 'xhr' || e._resourceType === 'fetch' ||
  /\/api\/|backend-api/.test(e.request.url) ||
  (e.response?.content?.mimeType || '').includes('json')
)
if (!apis.length) {
  console.log('（本次抓包中没有接口请求）')
} else {
  console.log(pad('耗时', 10) + pad('状态', 7) + pad('大小', 11) + pad('方法', 7) + 'URL')
  for (const e of apis.sort((a, b) => (b.time || 0) - (a.time || 0))) {
    console.log(
      pad(fmtMs(e.time), 10) + pad(String(e.response?.status ?? '-'), 7) +
      pad(fmtKB(e.response?.content?.size), 11) + pad(e.request.method, 7) +
      shortUrl(e.request.url)
    )
  }
  if (apis.length) {
    const at = apis.reduce((s, e) => s + (e.time || 0), 0)
    console.log(`\n接口合计：${apis.length} 个，累计 ${fmtMs(at)}，平均 ${fmtMs(at / apis.length)}`)
  }
}

// ---------------------------------------------------------------- 缓存
console.log('\n' + '-'.repeat(78))
console.log('【9】缓存情况')
console.log('-'.repeat(78))
const fromCache = entries.filter((e) => e.cache && Object.keys(e.cache).length > 0 && (e.response?._transferSize === 0 || e.response?._transferSize == null))
const noCacheHeader = entries.filter((e) => {
  const h = (e.response?.headers || []).find((x) => x.name.toLowerCase() === 'cache-control')
  return !h
})
console.log(`带 cache 命中的请求     : ${fromCache.length} / ${entries.length}`)
console.log(`无 Cache-Control 响应头 : ${noCacheHeader.length} / ${entries.length}`)
if (noCacheHeader.length) {
  console.log('  未设缓存头的前 10 个（都是同源静态资源或接口）：')
  for (const e of noCacheHeader.slice(0, 10)) {
    console.log(`    ${extOf(e.request.url)}  ${shortUrl(e.request.url)}`)
  }
}

// ---------------------------------------------------------------- 诊断
console.log('\n' + '-'.repeat(78))
console.log('【10】自动诊断与优化建议')
console.log('-'.repeat(78))
const tips = []
const slowCount = entries.filter((e) => (e.time || 0) > 500).length
const slowCount1s = entries.filter((e) => (e.time || 0) > 1000).length
if (slowCount1s) tips.push(`有 ${slowCount1s} 个请求耗时超过 1 秒，${slowCount} 个超过 500ms —— 优先排查这些。`)
else if (slowCount) tips.push(`有 ${slowCount} 个请求耗时超过 500ms，可关注。`)

const huge = entries.filter((e) => (e.response?.content?.size || 0) > 500 * 1024)
if (huge.length) {
  tips.push(`有 ${huge.length} 个响应体超过 500KB：`)
  for (const e of huge.slice(0, 8)) tips.push(`    ${fmtKB(e.response?.content?.size)}  ${shortUrl(e.request.url)}`)
}

// 检查是否用了压缩
const uncompressed = entries.filter((e) => {
  const size = e.response?.content?.size || 0
  const transfer = e.response?._transferSize
  const mime = e.response?.content?.mimeType || ''
  const compressible = /json|javascript|text|css|html/.test(mime)
  return compressible && transfer && size > 1024 && transfer >= size * 0.9
})
if (uncompressed.length) {
  tips.push(`有 ${uncompressed.length} 个可压缩响应未启用压缩（传输量≈原始大小）：`)
  for (const e of uncompressed.slice(0, 8)) {
    tips.push(`    ${fmtKB(e.response?.content?.size)} 传 ${fmtKB(e.response?._transferSize)}  ${shortUrl(e.request.url)}`)
  }
} else {
  tips.push('可压缩资源看起来已启用 gzip/br（传输量明显小于原始大小）。')
}

// HTTP 版本
const http1 = entries.filter((e) => e.response?.httpVersion === 'http/1.1').length
const http2 = entries.filter((e) => (e.response?.httpVersion || '').startsWith('h2') || (e.response?.httpVersion || '').includes('2')).length
tips.push(`HTTP 版本：http/1.1 = ${http1}，h2 = ${http2}${http1 > 0 && http2 === 0 ? '（建议启用 HTTP/2）' : ''}`)

// 同源/跨域
const hosts = new Set(entries.map((e) => hostOf(e.request.url)))
if (hosts.size > 1) tips.push(`请求跨 ${hosts.size} 个域名，跨域请求会有额外握手与预检开销。`)

// 连接复用
const reused = entries.filter((e) => e.connection && e.connection !== '').length
tips.push(`复用已有连接的请求：${reused} / ${entries.length}`)

for (const t of tips) console.log('  · ' + t)

console.log('\n' + '='.repeat(78))

function pad(s, n) {
  s = String(s)
  // 中文按 2 宽度估算，保证表格对齐
  let w = 0
  for (const ch of s) w += /[\u4e00-\u9fa5\uff00-\uffef]/.test(ch) ? 2 : 1
  return s + ' '.repeat(Math.max(0, n - w))
}
