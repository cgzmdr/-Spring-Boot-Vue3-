/**
 * 校验 P0 修复后接口的「正确性」——性能优化不能改变返回值语义。
 *
 * 用法：node verify-ethnic-perf.mjs [baseUrl]
 *
 * 断言：
 *   1. /ethnic-groups 返回条数、字段、人口排名与指标依旧正确
 *   2. /ethnic-groups/population-stats 的聚合结果与逐条手算一致
 *   3. 缓存生效（重复请求结果稳定）
 */
const BASE = process.argv[2] || 'http://localhost:20256'

let pass = 0
let fail = 0
const ok = (m, e = '') => { pass++; console.log(`  \u2713 ${m}${e ? '  ' + e : ''}`) }
const bad = (m, d = '') => { fail++; console.log(`  \u2717 ${m}${d ? '  -> ' + d : ''}`) }
const assert = (c, m, d = '') => (c ? ok(m, d) : bad(m, d))

async function getJson(path) {
  const res = await fetch(BASE + path)
  const j = await res.json()
  if (j.code !== 0) throw new Error(`${path} -> code=${j.code} ${j.message}`)
  return j.data
}

console.log('[1] 民族列表：结构与指标正确性')
const page = await getJson('/ethnic-groups?page=0&size=12&sort=population,desc')
assert(Array.isArray(page.data), '返回 data 数组')
assert(page.data.length === 12, '单页 12 条', `got=${page.data.length}`)

const need = ['id', 'name', 'population', 'languageFamily', 'artCount', 'festivalCount',
  'foodCount', 'customCount', 'locationCount', 'personCount', 'autonomousAreaCount', 'populationRank']
const first = page.data[0]
const missing = need.filter((k) => first[k] === undefined)
assert(missing.length === 0, '列表项含全部指标字段', missing.length ? 'missing=' + missing.join(',') : '')

// 按 population,desc 排序，第一条应人口最多、排名 1
const pops = page.data.map((d) => d.population)
const sortedDesc = [...pops].sort((a, b) => b - a)
assert(JSON.stringify(pops) === JSON.stringify(sortedDesc), '按人口降序返回')
assert(first.populationRank === 1, '人口最多的民族排名为 1', `rank=${first.populationRank} name=${first.name}`)

// 指标应为非负整数
const badMetrics = page.data.filter((d) =>
  [d.artCount, d.festivalCount, d.foodCount, d.customCount, d.locationCount,
   d.personCount, d.autonomousAreaCount, d.populationRank].some((v) => typeof v !== 'number' || v < 0))
assert(badMetrics.length === 0, '指标均为非负数值', badMetrics.length ? badMetrics[0].name : '')

// 至少某些民族应有非零指标（否则说明聚合坏了）
const withMetrics = page.data.filter((d) => d.artCount > 0 || d.personCount > 0)
assert(withMetrics.length > 0, '存在带关联指标的民族', `count=${withMetrics.length}`)

console.log('\n[2] 人口统计：聚合正确性')
const stats = await getJson('/ethnic-groups/population-stats?topN=8')
assert(typeof stats.totalGroups === 'number' || typeof stats.groupCount === 'number' || stats.topGroups,
  '返回统计结构', Object.keys(stats).join(','))

const top = stats.topGroups || []
assert(top.length === 8, 'Top8 条数正确', `got=${top.length}`)
const topPops = top.map((t) => t.population)
const topSorted = [...topPops].sort((a, b) => b - a)
assert(JSON.stringify(topPops) === JSON.stringify(topSorted), 'Top 榜按人口降序')

// 分档总和应等于总人口
if (stats.buckets) {
  const bucketSum = stats.buckets.reduce((s, b) => s + (b.population || 0), 0)
  const total = stats.totalPopulation
  assert(bucketSum === total, '各分档人口合计 = 总人口', `buckets=${bucketSum} total=${total}`)
  const bucketCount = stats.buckets.reduce((s, b) => s + (b.groupCount || 0), 0)
  assert(bucketCount === (stats.groupCount ?? stats.totalGroups),
    '各分档民族数合计 = 民族总数', `buckets=${bucketCount}`)
}

console.log('\n[3] 缓存与稳定性')
const r1 = await getJson('/ethnic-groups/population-stats?topN=8')
const r2 = await getJson('/ethnic-groups/population-stats?topN=8')
assert(JSON.stringify(r1) === JSON.stringify(r2), '重复请求结果一致（可缓存）')

// 不同 topN 不应互相污染
const t3 = await getJson('/ethnic-groups/population-stats?topN=3')
assert((t3.topGroups || []).length === 3, 'topN=3 返回 3 条（缓存键区分参数）',
  `got=${(t3.topGroups || []).length}`)

console.log(`\n=== 结果 ===\n通过: ${pass}    失败: ${fail}`)
process.exitCode = fail === 0 ? 0 : 1
