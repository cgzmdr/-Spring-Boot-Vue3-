/**
 * 通过 Chrome DevTools Protocol 真实渲染 C 端「民族」列表页，
 * 校验「信息量增强」的三处改动是否真的出现在 DOM 上：
 *   1. 卡片指标条（非遗/人物/自治地方/节日/美食/风俗/聚居地）
 *   2. 人口排名徽标
 *   3. 「只看」与「人口分档」快捷筛选
 *
 * 用法：node verify-ethnic-ui.mjs [url]
 * 前置：C 端 dev server 已在 5173、（可选）后端在 20256。
 */
import { spawn } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const URL_ = process.argv[2] || 'http://localhost:5173/ethnic'
const CHROME = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const PORT = 9333

let pass = 0
let fail = 0
const ok = (m, e = '') => { pass++; console.log(`  \u2713 ${m}${e ? '  ' + e : ''}`) }
const bad = (m, d = '') => { fail++; console.log(`  \u2717 ${m}${d ? '  -> ' + d : ''}`) }
const assert = (c, m, d = '') => (c ? ok(m, d) : bad(m, d))

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function cdpTargets() {
  for (let i = 0; i < 40; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${PORT}/json/list`)
      const list = await res.json()
      const page = list.find((t) => t.type === 'page')
      if (page) return page
    } catch { /* 还没起来 */ }
    await sleep(250)
  }
  throw new Error('无法连接 Chrome CDP')
}

/** 极简 CDP 客户端：够用即可，避免引入依赖 */
function connect(wsUrl) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(wsUrl)
    let id = 0
    const pending = new Map()
    const events = []
    ws.onmessage = (ev) => {
      const msg = JSON.parse(ev.data)
      if (msg.id !== undefined && pending.has(msg.id)) {
        const { res, rej } = pending.get(msg.id)
        pending.delete(msg.id)
        msg.error ? rej(new Error(JSON.stringify(msg.error))) : res(msg.result)
      } else if (msg.method) {
        events.push(msg)
      }
    }
    ws.onerror = (e) => reject(new Error('WS 错误: ' + e.message))
    ws.onopen = () => resolve({
      send(method, params = {}) {
        const myId = ++id
        return new Promise((res, rej) => {
          pending.set(myId, { res, rej })
          ws.send(JSON.stringify({ id: myId, method, params }))
        })
      },
      close: () => ws.close(),
      events
    })
  })
}

const profileDir = mkdtempSync(join(tmpdir(), 'dsh-verify-'))
const chrome = spawn(CHROME, [
  '--headless=new',
  '--disable-gpu',
  '--no-first-run',
  '--no-default-browser-check',
  `--remote-debugging-port=${PORT}`,
  `--user-data-dir=${profileDir}`,
  '--window-size=1440,1200',
  URL_
], { stdio: 'ignore' })

let client
try {
  const target = await cdpTargets()
  client = await connect(target.webSocketDebuggerUrl)
  await client.send('Runtime.enable')
  await client.send('Page.enable')

  // 等待列表数据到位（卡片出现）
  let cardHtml = ''
  for (let i = 0; i < 60; i++) {
    const r = await client.send('Runtime.evaluate', {
      expression: `document.querySelectorAll('.ethnic-card').length`,
      returnByValue: true
    })
    if ((r.result?.value ?? 0) > 0) break
    await sleep(400)
  }

  const grab = async (expr) => {
    const r = await client.send('Runtime.evaluate', { expression: expr, returnByValue: true })
    return r.result?.value
  }

  console.log('[1] 民族卡片与指标条')
  const cards = await grab(`document.querySelectorAll('.ethnic-card').length`)
  assert(cards > 0, '卡片已渲染', `count=${cards}`)

  const statBars = await grab(`document.querySelectorAll('.ethnic-card .stats').length`)
  assert(statBars > 0, '卡片带指标条', `count=${statBars}`)

  const statItems = await grab(`document.querySelectorAll('.ethnic-card .stat').length`)
  assert(statItems > 0, '指标条含具体指标', `count=${statItems}`)

  const firstStats = await grab(
    `Array.from(document.querySelectorAll('.ethnic-card')[0].querySelectorAll('.stat')).map(s=>s.textContent.trim()).join(' | ')`
  )
  assert(!!firstStats, '首个卡片的指标内容可读', firstStats)

  // 指标条不应出现「0 项」这种空洞展示
  const zeroStats = await grab(
    `Array.from(document.querySelectorAll('.ethnic-card .stat b')).filter(b=>b.textContent.trim()==='0').length`
  )
  assert(zeroStats === 0, '指标条不显示数值为 0 的项', `zeroCount=${zeroStats}`)

  console.log('\n[2] 人口排名徽标')
  const badges = await grab(`document.querySelectorAll('.rank-badge').length`)
  assert(badges > 0, '排名徽标已渲染', `count=${badges}`)
  const badgeText = await grab(`document.querySelector('.rank-badge')?.textContent?.trim() || ''`)
  assert(/#\d+/.test(badgeText), '徽标显示人口名次', badgeText)

  console.log('\n[3] 快捷筛选')
  const onlyChips = await grab(
    `Array.from(document.querySelectorAll('.filter .chip')).map(c=>c.textContent.trim()).filter(t=>/^有/.test(t)).join(',')`
  )
  assert(!!onlyChips, '「只看」筛选已渲染', onlyChips)

  const bandChips = await grab(
    `Array.from(document.querySelectorAll('.filter .chip')).map(c=>c.textContent.trim()).filter(t=>/以上|以下|百万|十万/.test(t)).join(',')`
  )
  assert(!!bandChips, '人口分档筛选已渲染', bandChips)

  console.log('\n[4] 排序能力')
  const sortChips = await grab(
    `Array.from(document.querySelectorAll('.filter .chip')).map(c=>c.textContent.trim()).filter(t=>/排序|人口|拼音|名称/.test(t)).join(',')`
  )
  assert(!!sortChips, '排序选项已渲染', sortChips)

  console.log('\n[5] 「只看」筛选真实生效（点击后卡片数应减少或提示出现）')
  const before = await grab(`document.querySelectorAll('.ethnic-card').length`)
  await grab(`(()=>{const c=Array.from(document.querySelectorAll('.filter .chip')).find(x=>x.textContent.trim()==='有自治地方'); if(c){c.click(); return true} return false})()`)
  await sleep(900)
  const after = await grab(`document.querySelectorAll('.ethnic-card').length`)
  const noteShown = await grab(`!!document.querySelector('.only-note')`)
  assert(after !== null, '点击后页面仍可读', `before=${before} after=${after}`)
  assert(after < before || noteShown === true || after === 0, '「有自治地方」筛选改变了结果',
    `before=${before} after=${after} note=${noteShown}`)

  console.log('\n[6] 控制台无报错')
  const errs = client.events.filter((e) => e.method === 'Runtime.exceptionThrown')
  assert(errs.length === 0, '渲染过程无 JS 异常', errs.length ? JSON.stringify(errs[0]).slice(0, 200) : '')

  console.log(`\n=== 结果 ===\n通过: ${pass}    失败: ${fail}`)
  process.exitCode = fail === 0 ? 0 : 1
} catch (e) {
  console.error('验证异常:', e.message)
  process.exitCode = 1
} finally {
  try { client?.close() } catch { /* ignore */ }
  chrome.kill()
  await sleep(500)
  try { rmSync(profileDir, { recursive: true, force: true }) } catch { /* ignore */ }
}
