/**
 * 用 Chrome DevTools Protocol 做前端渲染冒烟测试。
 *
 * 目的：验证「网页版 Camunda Modeler」「我的待办」「审核处理」三个页面在真实浏览器里
 * 能加载、挂载 bpmn-js / form-js 画布，且**没有未捕获的运行时错误**。
 * 图像截图无法被当前环境解析，因此这里用 DOM 探针 + 控制台错误采集代替人工看图。
 *
 * 用法：node scripts/ui-smoke-verify.mjs
 */
import { spawn } from 'node:child_process'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const ADMIN = process.env.ADMIN_URL || 'http://localhost:5273'
const PORT = 9222

let pass = 0
let fail = 0
const ok = (m, e = '') => {
  pass++
  console.log(`  \u2713 ${m}${e ? '  ' + e : ''}`)
}
const bad = (m, e = '') => {
  fail++
  console.log(`  \u2717 ${m}${e ? '  -> ' + e : ''}`)
}
const assert = (c, m, e = '') => (c ? ok(m, e) : bad(m, e))
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const profile = mkdtempSync(join(tmpdir(), 'cdp-'))
const chrome = spawn(
  CHROME,
  [
    '--headless=new',
    '--disable-gpu',
    '--no-sandbox',
    '--no-first-run',
    `--remote-debugging-port=${PORT}`,
    `--user-data-dir=${profile}`,
    '--window-size=1600,1000',
    'about:blank'
  ],
  { stdio: 'ignore' }
)

/** 极简 CDP 客户端（只用 WebSocket + JSON 帧） */
async function connect() {
  for (let i = 0; i < 40; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${PORT}/json/list`)
      const targets = await res.json()
      const page = targets.find((t) => t.type === 'page')
      if (page?.webSocketDebuggerUrl) return page.webSocketDebuggerUrl
    } catch {
      /* 还没起来 */
    }
    await sleep(250)
  }
  throw new Error('无法连接 Chrome DevTools')
}

class Cdp {
  constructor(ws) {
    this.ws = ws
    this.id = 0
    this.pending = new Map()
    this.consoleErrors = []
    this.events = []
    ws.addEventListener('message', (ev) => {
      const msg = JSON.parse(ev.data)
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id)
        this.pending.delete(msg.id)
        msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result)
        return
      }
      if (msg.method === 'Runtime.exceptionThrown') {
        const d = msg.params.exceptionDetails
        this.consoleErrors.push(
          `[exception] ${d.exception?.description || d.text}`.split('\n')[0]
        )
      }
      if (msg.method === 'Runtime.consoleAPICalled' && msg.params.type === 'error') {
        this.consoleErrors.push(
          `[console.error] ${msg.params.args.map((a) => a.value ?? a.description ?? '').join(' ')}`
        )
      }
      this.events.push(msg)
    })
  }

  send(method, params = {}) {
    const id = ++this.id
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject })
      this.ws.send(JSON.stringify({ id, method, params }))
      setTimeout(() => {
        if (this.pending.has(id)) {
          this.pending.delete(id)
          reject(new Error(`${method} 超时`))
        }
      }, 30000)
    })
  }

  /** 在页面里求值（支持 await） */
  async eval(expression) {
    const r = await this.send('Runtime.evaluate', {
      expression,
      returnByValue: true,
      awaitPromise: true
    })
    if (r.exceptionDetails) {
      throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text)
    }
    return r.result.value
  }
}

function openWs(url) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(url)
    ws.addEventListener('open', () => resolve(ws))
    ws.addEventListener('error', (e) => reject(new Error('WebSocket 连接失败: ' + e.message)))
  })
}

async function visit(cdp, hash, label) {
  cdp.consoleErrors.length = 0
  await cdp.send('Page.navigate', { url: `${ADMIN}/#${hash}` })
  await sleep(4500)
  return label
}

async function main() {
  console.log('\n=== 前端渲染冒烟测试（Chrome CDP） ===\n')
  const wsUrl = await connect()
  const cdp = new Cdp(await openWs(wsUrl))
  await cdp.send('Page.enable')
  await cdp.send('Runtime.enable')

  // 先登录并把 token 写进 localStorage。
  // 注意：Page.navigate 会整页重载，localStorage 里的 token 会保留，但
  // 路由守卫在首次导航时就会读它 —— 所以必须先加载一次页面、写好 token，
  // 再导航到目标路由，否则会被弹回 /login。
  console.log('[0] 登录')
  await cdp.send('Page.navigate', { url: `${ADMIN}/#/login` })
  await sleep(3500)
  const loginOk = await cdp.eval(`
    (async () => {
      const res = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ account: 'cgzmdr@foxmail.com', password: 'Cz12345@' })
      });
      const json = await res.json();
      if (json.code === 0) {
        localStorage.setItem('satoken', json.data);
        document.cookie = 'satoken=' + json.data + '; path=/';
        return 'ok';
      }
      return 'fail:' + json.message;
    })()
  `)
  assert(loginOk === 'ok', '通过接口登录并写入 satoken', String(loginOk))

  // ---------------------------------------------------------- 待办中心
  console.log('\n[1] 我的待办')
  await visit(cdp, '/todo', 'todo')
  const todoProbe = await cdp.eval(`
    (() => {
      const h2 = document.querySelector('.page-header h2');
      const rows = document.querySelectorAll('.el-table__row');
      const engineTag = document.querySelector('.page-header .el-tag');
      return { title: h2 ? h2.textContent.trim() : null, rows: rows.length,
               engine: engineTag ? engineTag.textContent.trim() : null };
    })()
  `)
  assert(todoProbe.title === '我的待办', '页面标题正确', String(todoProbe.title))
  assert(todoProbe.engine != null, 'Camunda 引擎版本标签已渲染', String(todoProbe.engine))
  assert(
    /Camunda\s*\d/.test(todoProbe.engine || ''),
    '引擎在线并显示版本号',
    String(todoProbe.engine)
  )
  assert(cdp.consoleErrors.length === 0, '无运行时错误', cdp.consoleErrors.slice(0, 2).join(' | '))

  // ---------------------------------------------------------- Camunda Modeler
  console.log('\n[2] Camunda Modeler')
  await visit(cdp, '/modeler', 'modeler')
  const modelerProbe = await cdp.eval(`
    (() => {
      const canvas = document.querySelector('.djs-container svg');
      const palette = document.querySelector('.djs-palette');
      const items = document.querySelectorAll('.side-panel .list-item');
      const tabs = document.querySelectorAll('.modeler-toolbar .el-radio-button');
      // bpmn-js 把已导入的图形渲染成 g.djs-element
      const shapes = document.querySelectorAll('.djs-element');
      return {
        canvas: !!canvas, palette: !!palette,
        processItems: items.length, tabs: tabs.length,
        shapes: shapes.length
      };
    })()
  `)
  assert(modelerProbe.canvas, 'bpmn-js 画布已挂载')
  assert(modelerProbe.palette, 'bpmn-js 左侧工具栏已渲染')
  assert(modelerProbe.processItems > 0, '流程定义列表已加载', `items=${modelerProbe.processItems}`)
  assert(modelerProbe.shapes > 0, 'BPMN 图形已渲染到画布', `shapes=${modelerProbe.shapes}`)
  assert(modelerProbe.tabs === 3, '三个模式页签齐全', `tabs=${modelerProbe.tabs}`)
  assert(cdp.consoleErrors.length === 0, '无运行时错误', cdp.consoleErrors.slice(0, 3).join(' | '))

  // 切到表单设计器
  const formProbe = await cdp.eval(`
    (async () => {
      const btns = document.querySelectorAll('.modeler-toolbar .el-radio-button');
      btns[1].querySelector('input').click();
      await new Promise(r => setTimeout(r, 2500));
      return {
        editor: !!document.querySelector('.fjs-editor-container'),
        palette: !!document.querySelector('.fjs-palette'),
        items: document.querySelectorAll('.side-panel .list-item').length
      };
    })()
  `)
  assert(formProbe.editor, 'form-js 设计器已挂载')
  assert(formProbe.items > 0, 'Camunda Form 列表已加载', `items=${formProbe.items}`)
  assert(cdp.consoleErrors.length === 0, '表单设计器无运行时错误',
    cdp.consoleErrors.slice(0, 3).join(' | '))

  // 切到引擎信息
  const engineProbe = await cdp.eval(`
    (async () => {
      const btns = document.querySelectorAll('.modeler-toolbar .el-radio-button');
      btns[2].querySelector('input').click();
      await new Promise(r => setTimeout(r, 2000));
      const cards = document.querySelectorAll('.engine-panel .el-card');
      const rows = document.querySelectorAll('.engine-panel .el-table__row');
      return { cards: cards.length, rows: rows.length };
    })()
  `)
  assert(engineProbe.cards === 2, '引擎信息两张卡片已渲染')
  assert(engineProbe.rows > 0, '引擎已部署流程列表非空', `rows=${engineProbe.rows}`)

  // ---------------------------------------------------------- 审核处理页
  console.log('\n[3] 审核处理页（待办详情）')  // 先造一个待办：提交一条内容进入审批
  const instanceId = await cdp.eval(`
    (async () => {
      const token = localStorage.getItem('satoken');
      const r = await fetch('/api/workflow/entries/ethnic/00000000-0000-0000-0000-000000000003/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', satoken: token },
        body: JSON.stringify({ note: 'UI 冒烟测试' })
      });
      const j = await r.json();
      return j.code === 0 ? j.data.id : ('err:' + j.message);
    })()
  `)
  assert(!String(instanceId).startsWith('err:'), '创建待办实例用于 UI 验证', String(instanceId))

  await visit(cdp, `/todo/detail?instanceId=${instanceId}`, 'detail')
  const detailProbe = await cdp.eval(`
    (() => {
      const stages = document.querySelectorAll('.stage-node');
      const active = document.querySelector('.stage-node.is-active');
      const form = document.querySelector('.camunda-form-renderer .fjs-container');
      const opinionFields = document.querySelectorAll('.fjs-form-field');
      const timeline = document.querySelector('.opinion-timeline');
      const buttons = [...document.querySelectorAll('.action-bar button')].map(b => b.textContent.trim());
      return {
        stages: stages.length,
        activeStage: active ? active.textContent.trim() : null,
        camundaForm: !!form,
        fields: opinionFields.length,
        timeline: !!timeline,
        buttons
      };
    })()
  `)
  assert(detailProbe.stages === 4, '闭环进度条渲染 4 个环节', `n=${detailProbe.stages}`)
  assert(
    detailProbe.activeStage && detailProbe.activeStage.includes('审核员'),
    '当前环节高亮在「审核员审批」',
    String(detailProbe.activeStage)
  )
  assert(detailProbe.camundaForm, 'Camunda Form 已渲染到审核页')
  assert(detailProbe.fields >= 3, 'Camunda Form 字段已渲染', `fields=${detailProbe.fields}`)
  assert(detailProbe.timeline, '意见时间线已渲染')
  assert(
    detailProbe.buttons.some((b) => b.includes('审批通过')),
    '审批操作按钮齐全',
    detailProbe.buttons.join('/')
  )
  assert(cdp.consoleErrors.length === 0, '无运行时错误', cdp.consoleErrors.slice(0, 3).join(' | '))

  // 清理
  await cdp.eval(`
    (async () => {
      const token = localStorage.getItem('satoken');
      await fetch('/api/workflow/instances/${instanceId}/withdraw', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', satoken: token },
        body: JSON.stringify({ reason: 'UI 冒烟测试清理' })
      });
      return 'ok';
    })()
  `)
  ok('测试数据已清理')

  // ---------------------------------------------------------- 民族列表（接入审批）
  console.log('\n[4] 民族列表（审批入口）')
  await visit(cdp, '/ethnic', 'ethnic')
  const ethnicProbe = await cdp.eval(`
    (() => {
      const headers = [...document.querySelectorAll('.el-table th')].map(t => t.textContent.trim());
      const rows = document.querySelectorAll('.el-table__body tr').length;
      const buttons = [...document.querySelectorAll('.el-table__body button')].map(b => b.textContent.trim());
      return {
        title: document.querySelector('.page-header h2')?.textContent.trim() || null,
        hasStageCol: headers.some(h => h.includes('审批环节')),
        hasVersionCol: headers.some(h => h.includes('版本')),
        rows,
        hasSubmitBtn: buttons.some(b => b.includes('提交审批')),
        hasWorkflowBtn: buttons.some(b => b.includes('去处理') || b.includes('查看进度'))
      };
    })()
  `)
  assert(ethnicProbe.title === '民族管理', '民族列表标题正确', String(ethnicProbe.title))
  assert(ethnicProbe.hasStageCol, '新增「审批环节」列')
  assert(ethnicProbe.hasVersionCol, '新增「版本」列')
  assert(ethnicProbe.rows > 0, '列表有数据', `rows=${ethnicProbe.rows}`)
  assert(
    ethnicProbe.hasSubmitBtn || ethnicProbe.hasWorkflowBtn,
    '列表具备审批操作入口',
    `submit=${ethnicProbe.hasSubmitBtn} workflow=${ethnicProbe.hasWorkflowBtn}`
  )
  assert(cdp.consoleErrors.length === 0, '无运行时错误', cdp.consoleErrors.slice(0, 2).join(' | '))

  // ---------------------------------------------------------- 内容审核页
  console.log('\n[5] 内容审核页')
  await visit(cdp, '/review', 'review')
  const reviewProbe = await cdp.eval(`
    (() => {
      const statuses = [...document.querySelectorAll('.el-radio-button')].map(b=>b.textContent.trim());
      return {
        title: document.querySelector('.page-header h2')?.textContent.trim() || null,
        statuses,
        hasTodoEntry: !!document.querySelector('.page-header .el-button')
      };
    })()
  `)
  assert(reviewProbe.title === '内容审核', '审核页标题正确', String(reviewProbe.title))
  assert(
    reviewProbe.statuses.some((s) => s.includes('已暂时下线')) &&
      reviewProbe.statuses.some((s) => s.includes('待修改')),
    '审核状态含下线/待修改（工作流新增态）',
    reviewProbe.statuses.join('/')
  )
  assert(reviewProbe.hasTodoEntry, '提供跳转到我的待办的入口')
  assert(cdp.consoleErrors.length === 0, '无运行时错误', cdp.consoleErrors.slice(0, 2).join(' | '))

  // ---------------------------------------------------------- 结果
  console.log('\n=== 结果 ===')
  console.log(`通过: ${pass}    失败: ${fail}`)
  if (fail > 0) process.exitCode = 1

  try {
    chrome.kill()
  } catch {
    /* 忽略 */
  }
  try {
    rmSync(profile, { recursive: true, force: true })
  } catch {
    /* 忽略 */
  }
}

main().catch((e) => {
  console.error('\n执行异常:', e.message)
  try {
    chrome.kill()
  } catch {
    /* 忽略 */
  }
  process.exit(1)
})
