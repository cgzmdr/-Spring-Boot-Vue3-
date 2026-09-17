/**
 * 通过 CDP 真实渲染 C 端「民族详情」页，校验信息量增强的关联信息区块：
 *   1. 基本资料里的「人口位次」（第 N / M）
 *   2. 「关联信息」面板：民族自治地方（按级别徽标）与相关人物（角色徽标）
 *   3. 关联名字确实是可点击的站内链接
 *
 * 用法：node verify-ethnic-detail-ui.mjs [url]
 * 默认取壮族（自治地方最丰富：自治区/州/县三级俱全）。
 */
import { spawn } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const DEFAULT_ID = '00000000-0000-0000-0000-000000000008' // 壮族
const URL_ = process.argv[2] || `http://localhost:5173/#/ethnic/${DEFAULT_ID}`
const CHROME = process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const PORT = 9336

let pass = 0
let fail = 0
const ok = (m, e = '') => { pass++; console.log(`  \u2713 ${m}${e ? '  ' + e : ''}`) }
const bad = (m, d = '') => { fail++; console.log(`  \u2717 ${m}${d ? '  -> ' + d : ''}`) }
const assert = (c, m, d = '') => (c ? ok(m, d) : bad(m, d))
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const profileDir = mkdtempSync(join(tmpdir(), 'dsh-detail-'))
const chrome = spawn(CHROME, [
  '--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  `--remote-debugging-port=${PORT}`, `--user-data-dir=${profileDir}`,
  '--window-size=1440,1600', URL_
], { stdio: 'ignore' })

let ws
try {
  let page
  for (let i = 0; i < 40 && !page; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
      page = list.find((t) => t.type === 'page')
    } catch { /* wait */ }
    if (!page) await sleep(250)
  }
  if (!page) throw new Error('无法连接 Chrome CDP')

  ws = new WebSocket(page.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  const events = []
  ws.onmessage = (ev) => {
    const m = JSON.parse(ev.data)
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id) }
    else if (m.method) events.push(m)
  }
  await new Promise((r) => { ws.onopen = r })
  const send = (method, params = {}) => new Promise((res) => {
    const myId = ++id
    pending.set(myId, res)
    ws.send(JSON.stringify({ id: myId, method, params }))
  })
  const ev = async (expr) => (await send('Runtime.evaluate', { expression: expr, returnByValue: true })).result?.value

  await send('Runtime.enable')

  // 等详情数据到位（.rank-hint 或 关联面板 出现）
  for (let i = 0; i < 60; i++) {
    if (await ev(`!!document.querySelector('.info-card, .related-panel')`)) break
    await sleep(400)
  }

  console.log('[1] 基本资料：人口位次')
  const rankHint = await ev(`document.querySelector('.rank-hint')?.textContent?.trim() || ''`)
  assert(/第\s*\d+\s*\/\s*\d+/.test(rankHint), '显示人口位次「第 N / M」', rankHint)

  console.log('\n[2] 关联信息面板')
  const hasPanel = await ev(`!!document.querySelector('.related-panel')`)
  assert(hasPanel, '「关联信息」面板已渲染')

  const blocks = await ev(`document.querySelectorAll('.related-panel .related-block').length`)
  assert(blocks > 0, '含关联信息区块', `blocks=${blocks}`)

  const titles = await ev(
    `Array.from(document.querySelectorAll('.related-panel .related-title')).map(t=>t.textContent.replace(/\\s+/g,' ').trim()).join(' | ')`
  )
  assert(!!titles, '区块标题可读', titles)

  console.log('\n[3] 民族自治地方')
  const areaItems = await ev(
    `Array.from(document.querySelectorAll('.related-block')).filter(b=>b.textContent.includes('民族自治地方')).flatMap(b=>Array.from(b.querySelectorAll('li'))).length`
  )
  assert(areaItems > 0, '列出自治地方条目', `count=${areaItems}`)

  const areaBadges = await ev(
    `Array.from(document.querySelectorAll('.related-block')).filter(b=>b.textContent.includes('民族自治地方')).flatMap(b=>Array.from(b.querySelectorAll('.rel-badge'))).map(x=>x.textContent.trim()).join(',')`
  )
  assert(/自治区|自治州|自治县/.test(areaBadges), '自治地方带级别徽标', areaBadges)

  const areaLinks = await ev(
    `Array.from(document.querySelectorAll('.related-block')).filter(b=>b.textContent.includes('民族自治地方')).flatMap(b=>Array.from(b.querySelectorAll('a.rel-name'))).map(a=>a.getAttribute('href')).join(',')`
  )
  assert(/autonomous/.test(areaLinks), '自治地方为可点击站内链接', areaLinks)

  console.log('\n[4] 相关人物')
  const personItems = await ev(
    `Array.from(document.querySelectorAll('.related-block')).filter(b=>b.textContent.includes('相关人物')).flatMap(b=>Array.from(b.querySelectorAll('li'))).length`
  )
  assert(personItems > 0, '列出相关人物条目', `count=${personItems}`)

  const personBadges = await ev(
    `Array.from(document.querySelectorAll('.related-block')).filter(b=>b.textContent.includes('相关人物')).flatMap(b=>Array.from(b.querySelectorAll('.rel-badge'))).map(x=>x.textContent.trim()).join(',')`
  )
  assert(/传承人|名家|人物/.test(personBadges), '人物带角色徽标', personBadges)

  const personLinks = await ev(
    `Array.from(document.querySelectorAll('.related-block')).filter(b=>b.textContent.includes('相关人物')).flatMap(b=>Array.from(b.querySelectorAll('a.rel-name'))).map(a=>a.getAttribute('href')).join(',')`
  )
  assert(/persons/.test(personLinks), '人物为可点击站内链接', personLinks)

  console.log('\n[5] 无 JS 异常')
  const errs = events.filter((e) => e.method === 'Runtime.exceptionThrown')
  assert(errs.length === 0, '渲染过程无 JS 异常', errs.length ? JSON.stringify(errs[0]).slice(0, 200) : '')

  console.log(`\n=== 结果 ===\n通过: ${pass}    失败: ${fail}`)
  process.exitCode = fail === 0 ? 0 : 1
} catch (e) {
  console.error('验证异常:', e.message)
  process.exitCode = 1
} finally {
  try { ws?.close() } catch { /* ignore */ }
  chrome.kill()
  await sleep(500)
  try { rmSync(profileDir, { recursive: true, force: true }) } catch { /* ignore */ }
}
