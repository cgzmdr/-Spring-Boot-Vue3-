import { readFileSync } from 'node:fs'

const har = JSON.parse(readFileSync('124.222.194.126.har', 'utf8'))
const es = har.log.entries
const BASE = 'http://124.222.194.126:20256'
const t0 = new Date(es[0].startedDateTime).getTime()

const rows = es
  .map((e) => {
    const s = new Date(e.startedDateTime).getTime() - t0
    const wait = Math.round(e.timings?.wait ?? 0)
    const total = Math.round(e.time)
    return {
      url: e.request.url.replace(BASE, ''),
      start: s,
      wait,
      total,
      end: s + total,
      status: e.response.status,
      proto: e.response.httpVersion,
      mime: e.response?.content?.mimeType || '',
      size: e.response?.content?.size || 0
    }
  })
  .sort((a, b) => a.start - b.start)

console.log('=== 时间轴（相对第一个请求，单位 ms）===')
for (const r of rows) {
  console.log(
    't+' + String(r.start).padStart(4) + 'ms → t+' +
    String(Math.round(r.end)).padStart(5) + 'ms  服务端 ' +
    String(r.wait).padStart(5) + 'ms  ' + String(r.status) + '  ' + r.url
  )
}

console.log('')
console.log('=== 关键观察 ===')
console.log('并发发起（相对起点 < 50ms）:', rows.every((r) => r.start < 50))
console.log('服务端 wait 耗时序列      :', rows.map((r) => r.wait).join(', ') + ' ms')
console.log('墙钟总时长                :', Math.round(Math.max(...rows.map((r) => r.end))) + ' ms')
console.log('服务端 wait 累计          :', rows.reduce((s, r) => s + r.wait, 0) + ' ms')

const fast = rows.filter((r) => r.wait < 500)
const slow = rows.filter((r) => r.wait >= 1000)
console.log('')
console.log('快速请求（<500ms）:', fast.length, fast.map((r) => r.url).join(' | '))
console.log('慢请求（>=1s）   :', slow.length, slow.map((r) => r.url).join(' | '))

console.log('')
console.log('=== 响应头（看是否有压缩/缓存/网关）===')
for (const e of es) {
  const u = e.request.url.replace(BASE, '')
  const hs = (e.response.headers || []).map((h) => h.name + ': ' + h.value)
  console.log('')
  console.log('URL: ' + u)
  for (const h of hs) console.log('   ' + h)
}
