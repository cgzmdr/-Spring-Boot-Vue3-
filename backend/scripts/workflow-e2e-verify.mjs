/**
 * 内容审批工作流端到端验证（Camunda 8）
 *
 * 验证完整闭环：
 *   提交 → 审核员审批(留意见) → 上线 → 内容管理员审查(留意见)
 *        → 发现问题下线 → 内容编辑修改(看得到前序意见) → 二审 → 重新上线 → 再审查 → 通过
 *
 * 脚本自带清理：开始前把目标内容上的历史实例撤回，保证可重复运行。
 *
 * 用法：node scripts/workflow-e2e-verify.mjs
 */
const BASE = process.env.API_BASE || 'http://localhost:20256'
const ACCOUNT = process.env.ADMIN_ACCOUNT || 'cgzmdr@foxmail.com'
const PASSWORD = process.env.ADMIN_PASSWORD || 'Cz12345@'

let token = null
let pass = 0
let fail = 0

function ok(label, extra = '') {
  pass++
  console.log(`  \u2713 ${label}${extra ? '  ' + extra : ''}`)
}

function bad(label, detail = '') {
  fail++
  console.log(`  \u2717 ${label}${detail ? '  -> ' + detail : ''}`)
}

function assert(cond, label, detail = '') {
  if (cond) ok(label, detail)
  else bad(label, detail)
}

async function api(method, path, body, opts = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.satoken = token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    json = { code: -1, message: text }
  }
  if (json.code !== 0 && !opts.allowError) {
    throw new Error(`${method} ${path} -> code=${json.code} ${json.message}`)
  }
  return json
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/** 等待工作流实例到达指定环节（Camunda 是异步流转，需轮询） */
async function waitStage(instanceId, expectStage, tries = 20) {
  for (let i = 0; i < tries; i++) {
    const r = await api('GET', `/api/workflow/instances/${instanceId}`, undefined, { allowError: true })
    if (r.code === 0 && r.data && r.data.currentStage === expectStage) return r.data
    await sleep(500)
  }
  const r = await api('GET', `/api/workflow/instances/${instanceId}`, undefined, { allowError: true })
  return r.data
}

/**
 * 等待实例满足自定义条件（用于「流程走完」这类终态断言）。
 *
 * 注意：用户任务完成后，引擎的二次存储有延迟，若提交请求内的短轮询没追上，
 * 实例会保持 running，由后端每 30s 一次的「工作流对账」任务收尾。
 * 因此这里的等待窗口必须大于对账周期（默认 40s + 余量）。
 */
async function waitFor(instanceId, predicate, tries = 90) {
  let last = null
  for (let i = 0; i < tries; i++) {
    const r = await api('GET', `/api/workflow/instances/${instanceId}`, undefined, { allowError: true })
    last = r.data
    if (last && predicate(last)) return last
    await sleep(1000)
  }
  return last
}

async function getEthnic(id) {
  const r = await api('GET', `/admin/ethnic-groups/${id}`)
  return r.data
}

async function main() {
  console.log('\n=== 内容审批工作流端到端验证 ===\n')

  // ---------------------------------------------------------------- 0. 登录
  console.log('[0] 登录')
  const login = await api('POST', '/auth/login', { account: ACCOUNT, password: PASSWORD })
  token = login.data
  assert(!!token, '获取登录 token', token)

  // ---------------------------------------------------------------- 1. 引擎健康
  console.log('\n[1] Camunda 引擎')
  const topo = await api('GET', '/api/workflow/engine/topology')
  assert(topo.data.healthy === true, 'Camunda 引擎连通', `gatewayVersion=${topo.data.gatewayVersion}`)
  const defs = await api('GET', '/api/workflow/engine/process-definitions')
  const reviewDef = (defs.data || []).find((d) => d.processDefinitionId === 'ethnic-content-review')
  assert(!!reviewDef, '民族审批流程已部署', reviewDef ? `v${reviewDef.version}` : '未找到')

  const bindings = await api('GET', '/api/workflow/processes?entryType=ethnic')
  assert((bindings.data || []).length > 0, '流程绑定已注册', `count=${(bindings.data || []).length}`)

  const forms = await api('GET', '/api/workflow/forms')
  const approvalForm = (forms.data || []).find((f) => f.formId === 'ethnic-approval-form')
  const inspectForm = (forms.data || []).find((f) => f.formId === 'ethnic-inspection-form')
  const reviseForm = (forms.data || []).find((f) => f.formId === 'ethnic-revision-form')
  assert(!!approvalForm, 'Camunda Form: 审批表单')
  assert(!!inspectForm, 'Camunda Form: 审查表单')
  assert(!!reviseForm, 'Camunda Form: 修改表单')
  assert(
    approvalForm && JSON.parse(approvalForm.schemaJson).components.length > 0,
    '审批表单 schema 可解析'
  )

  // ---------------------------------------------------------------- 2. 提交审批
  console.log('\n[2] 内容编辑提交审批')
  const ETHNIC_ID = process.env.ETHNIC_ID || '00000000-0000-0000-0000-000000000002'

  // 自清理：把上一次运行遗留的活跃实例撤回，保证脚本可重复执行
  const stale = await api('GET', `/api/workflow/entries/ethnic/${ETHNIC_ID}/task`, undefined, { allowError: true })
  if (stale.code === 0 && stale.data && stale.data.instanceId) {
    await api('POST', `/api/workflow/instances/${stale.data.instanceId}/withdraw`,
      { reason: 'E2E 重新运行前的清理' }, { allowError: true })
    await sleep(1200)
    console.log(`  · 已清理上一次遗留的实例 ${stale.data.instanceId}`)
  }

  const before = await getEthnic(ETHNIC_ID)
  console.log(`  目标内容: ${before.name}  当前状态=${before.status} 版本=v${before.contentVersion}`)

  const submit = await api('POST', `/api/workflow/entries/ethnic/${ETHNIC_ID}/submit`, {
    note: 'E2E 测试：提交蒙古族内容审批'
  })
  const instanceId = submit.data.id
  assert(!!instanceId, '流程实例已创建', instanceId)
  assert(submit.data.currentStage === 'pending_review', '进入待审批环节', submit.data.currentStage)
  assert(submit.data.contentVersion === (before.contentVersion || 0) + 1, '内容版本号 +1', `v${submit.data.contentVersion}`)

  const afterSubmit = await getEthnic(ETHNIC_ID)
  assert(afterSubmit.status === 'pending', '内容状态置为 pending', afterSubmit.status)

  const dup = await api('POST', `/api/workflow/entries/ethnic/${ETHNIC_ID}/submit`, { note: 'x' }, { allowError: true })
  assert(dup.code !== 0, '重复提交被拒绝', `code=${dup.code} ${dup.message}`)

  // ---------------------------------------------------------------- 3. 审核员审批（第一次）
  console.log('\n[3] 审核员审批（第一次）')
  const task1 = await api('GET', `/api/workflow/instances/${instanceId}`)
  assert(task1.data.currentStage === 'pending_review', '待办显示「待审核员审批」')
  assert(task1.data.actionable === true, 'super_admin 可处理该待办')

  await api('POST', `/api/workflow/instances/${instanceId}/approve`, {
    opinion: '第一次审批意见：内容完整、表述准确，同意上线。'
  })
  ok('提交审批通过 + 审批意见')

  const stage2 = await waitStage(instanceId, 'pending_inspect')
  assert(stage2 && stage2.currentStage === 'pending_inspect', '流转到「待内容管理员审查」',
    stage2 ? stage2.currentStage : 'timeout')

  const afterApprove = await getEthnic(ETHNIC_ID)
  assert(afterApprove.status === 'published', '审批通过后内容已上线', afterApprove.status)

  const ops1 = await api('GET', `/api/workflow/entries/ethnic/${ETHNIC_ID}/opinions`)
  assert(ops1.data.some((o) => o.stage === 'approve' && o.opinion.includes('第一次审批意见')),
    '审批意见已留档')
  assert(ops1.data.some((o) => o.stage === 'submit'), '提交记录已留档')
  assert(ops1.data.some((o) => o.stage === 'publish'), '上线动作已留档')

  // ---------------------------------------------------------------- 4. 内容管理员审查：发现问题 → 下线
  console.log('\n[4] 内容管理员审查：发现问题，暂时下线')
  const inspectTask = await api('GET', `/api/workflow/instances/${instanceId}`)
  assert(inspectTask.data.currentStage === 'pending_inspect', '待办显示「待内容管理员审查」')
  assert(!!inspectTask.data.formSchema, '审查环节带回 Camunda Form schema',
    inspectTask.data.formId)

  const offline = await api('POST', `/api/workflow/instances/${instanceId}/inspect/issue`, {
    reason: '审查意见：人口数据与七普口径不一致，请核对后修改。'
  }, { allowError: true })
  assert(offline.code === 0, '审查发现问题并下线', offline.message)

  const stage3 = await waitStage(instanceId, 'revising')
  assert(stage3 && stage3.currentStage === 'revising', '流转到「待内容编辑修改」',
    stage3 ? stage3.currentStage : 'timeout')

  const afterOffline = await getEthnic(ETHNIC_ID)
  assert(afterOffline.status === 'offline', '内容已暂时下线', afterOffline.status)

  // ---------------------------------------------------------------- 5. 内容编辑修改（看得到前序意见）
  console.log('\n[5] 内容编辑修改（应能看到前序审批/审查意见）')
  const reviseTask = await api('GET', `/api/workflow/instances/${instanceId}`)
  assert(reviseTask.data.currentStage === 'revising', '待办显示「待内容编辑修改」')

  const timeline = await api('GET', `/api/workflow/entries/ethnic/${ETHNIC_ID}/timeline`)
  const opinions = timeline.data.opinions
  const approvalOpinion = opinions.find((o) => o.stage === 'approve')
  const inspectOpinion = opinions.find((o) => o.stage === 'inspect')
  const offlineOpinion = opinions.find((o) => o.stage === 'offline')
  assert(!!approvalOpinion && approvalOpinion.opinion.includes('第一次审批意见'),
    '内容编辑可查看【审批意见】', approvalOpinion ? approvalOpinion.opinion : '缺失')
  assert(!!inspectOpinion && inspectOpinion.opinion.includes('人口数据'),
    '内容编辑可查看【审查意见】', inspectOpinion ? inspectOpinion.opinion : '缺失')
  assert(!!offlineOpinion, '下线动作有留痕')
  assert(approvalOpinion.operatorName != null, '意见带操作人', approvalOpinion.operatorName)

  await api('POST', `/api/workflow/instances/${instanceId}/revise`, {
    revisionNote: '修改说明：已按七普 2020 口径重新核对人口数据。',
    needReapproval: true
  })
  ok('内容编辑提交修改说明 → 二次审批')

  const stage4 = await waitStage(instanceId, 'pending_review')
  assert(stage4 && stage4.currentStage === 'pending_review', '流转回「待审核员审批」（第二次）',
    stage4 ? stage4.currentStage : 'timeout')

  // ---------------------------------------------------------------- 6. 审核员二次审批 → 重新上线
  console.log('\n[6] 审核员二次审批 → 重新上线')
  await api('POST', `/api/workflow/instances/${instanceId}/approve`, {
    opinion: '第二次审批意见：人口数据已修正，同意重新上线。'
  })
  ok('二次审批通过')

  const stage5 = await waitStage(instanceId, 'pending_inspect')
  assert(stage5 && stage5.currentStage === 'pending_inspect', '重新上线并再次进入审查',
    stage5 ? stage5.currentStage : 'timeout')

  const afterRePublish = await getEthnic(ETHNIC_ID)
  assert(afterRePublish.status === 'published', '内容已重新上线', afterRePublish.status)

  const ops2 = await api('GET', `/api/workflow/entries/ethnic/${ETHNIC_ID}/opinions`)
  const secondApproval = ops2.data.filter((o) => o.stage === 'approve')
  assert(secondApproval.length === 2, '两轮审批意见都在', `count=${secondApproval.length}`)
  assert(ops2.data.some((o) => o.stage === 'revise'), '修改说明已留档')
  assert(ops2.data.filter((o) => o.stage === 'publish').length === 2, '两次上线都有留痕')

  // ---------------------------------------------------------------- 7. 审查通过 → 闭环结束
  console.log('\n[7] 内容管理员审查通过 → 本轮闭环结束')
  await api('POST', `/api/workflow/instances/${instanceId}/inspect/pass`, {
    opinion: '复核审查意见：数据已按七普口径修正，无问题，保持在线。'
  })
  ok('审查通过')

  const inst = await waitFor(instanceId, (t) => t.status === 'completed')
  assert(inst && inst.status === 'completed', '实例状态 = completed', inst ? inst.status : '缺失')
  assert(inst && inst.currentStage === 'published', '当前环节 = published（已上线）',
    inst ? inst.currentStage : '缺失')

  const finalTimeline = await api('GET', `/api/workflow/entries/ethnic/${ETHNIC_ID}/timeline`)
  assert(finalTimeline.data.contentStatus === 'published', '内容最终在线',
    finalTimeline.data.contentStatus)

  const finalOps = finalTimeline.data.opinions
  assert(finalOps.filter((o) => o.stage === 'inspect').length === 2, '两轮审查意见都在',
    `count=${finalOps.filter((o) => o.stage === 'inspect').length}`)
  assert(finalOps.filter((o) => o.stage === 'approve').length === 2, '两轮审批意见都在')
  assert(finalOps.some((o) => o.stage === 'offline'), '下线记录在案')

  // ---------------------------------------------------------------- 8. 二次提交（第 2 版）
  console.log('\n[8] 再次提交（内容版本递增）')
  // 上一轮实例必须已收尾（否则会被「存在进行中的审批流程」拒绝）。
  // 正常由提交请求内的短轮询完成；引擎二次存储过慢时由对账任务兜底，这里显式等待。
  await waitFor(instanceId, (t) => t.status !== 'running')

  const submit2 = await api('POST', `/api/workflow/entries/ethnic/${ETHNIC_ID}/submit`, {
    note: 'E2E 测试：第二轮修订提交'
  })
  assert(submit2.data.contentVersion === 2, '内容版本号递增到 v2', `v${submit2.data.contentVersion}`)
  assert(submit2.data.roundNo === 1, '新实例轮次重新计数')

  const tl2 = await api('GET', `/api/workflow/entries/ethnic/${ETHNIC_ID}/timeline`)
  assert(tl2.data.instances.length === 2, '历史实例保留（可回看上一版意见）',
    `instances=${tl2.data.instances.length}`)
  assert(tl2.data.opinions.length > finalOps.length, '跨版本意见累积保留',
    `${finalOps.length} -> ${tl2.data.opinions.length}`)

  // 清理：撤回第二轮，让内容回到草稿，避免污染演示数据
  await api('POST', `/api/workflow/instances/${submit2.data.id}/withdraw`, { reason: 'E2E 测试清理' })
  await sleep(1200)
  const cleaned = await getEthnic(ETHNIC_ID)
  assert(cleaned.status === 'draft', '撤回后内容回到 draft', cleaned.status)

  // 恢复原状态（contentVersion 保留递增，符合真实业务语义）
  await api('PUT', `/admin/ethnic-groups/${ETHNIC_ID}`, { status: 'published' })
  ok('测试数据已恢复为 published', `v${cleaned.contentVersion}`)

  // ---------------------------------------------------------------- 汇总
  console.log('\n=== 结果 ===')
  console.log(`通过: ${pass}    失败: ${fail}`)
  if (fail > 0) {
    console.log('\n存在失败项，请检查上方 \u2717 标记。')
    process.exit(1)
  }
  console.log('\n全部通过：审批 → 上线 → 审查 → 下线 → 修改 → 二审 → 重新上线 → 再审查 闭环正常。')
}

main().catch((e) => {
  console.error('\n执行异常:', e.message)
  process.exit(1)
})
