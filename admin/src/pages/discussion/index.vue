<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  broadcastAudience,
  deleteWord,
  discussionStats,
  handleReport,
  importWords,
  listBoards,
  listReports,
  listReviewItems,
  listWords,
  muteUser,
  reviewItem,
  reviewItemsBatch,
  saveBoard,
  saveWord,
  sendBroadcast,
  setTopicFlag,
  toggleBoard,
  type AdminBoard,
  type BroadcastAudience,
  type DiscussionReport,
  type DiscussionReviewItem,
  type SensitiveWord,
} from '@/api/modules/discussion'

/** 页签：审核队列 / 举报工作台 / 敏感词 / 板块管理 / 站内公告 */
const tab = ref<'review' | 'report' | 'words' | 'boards' | 'broadcast'>('review')

// ---------------------------------------------------------------- 审核队列

const reviewQuery = reactive({ targetType: 'discussion_topic', status: 'pending', sort: 'oldest', page: 0, size: 20 })
const reviewItems = ref<DiscussionReviewItem[]>([])
const reviewTotal = ref(0)
const reviewLoading = ref(false)
const selected = ref<DiscussionReviewItem[]>([])

async function loadReviews() {
  reviewLoading.value = true
  try {
    const res = await listReviewItems({ ...reviewQuery, sort: reviewQuery.sort as 'oldest' | 'priority' })
    reviewItems.value = res.data
    reviewTotal.value = res.total
    selected.value = []
  } finally {
    reviewLoading.value = false
  }
}

/** 批量通过 / 批量驳回 */
async function batchDecide(status: 'approved' | 'rejected') {
  if (!selected.value.length) {
    ElMessage.warning('请先勾选要处置的内容')
    return
  }
  let reason = ''
  if (status === 'rejected') {
    try {
      const input = await ElMessageBox.prompt(`批量驳回 ${selected.value.length} 条内容的理由（会通知作者）`, '批量驳回', {
        inputPlaceholder: '例如：含广告引流内容',
        inputValidator: (v) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的理由'),
      })
      reason = input.value
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(`确认批量通过 ${selected.value.length} 条内容？`, '批量通过', { type: 'warning' })
    } catch {
      return
    }
  }
  const res = await reviewItemsBatch({
    targetType: reviewQuery.targetType,
    ids: selected.value.map((i) => i.targetId),
    status,
    reason: reason || undefined,
  })
  ElMessage.success(`处置完成：成功 ${res.success} 条${res.failed ? `，失败 ${res.failed} 条` : ''}`)
  await Promise.all([loadReviews(), loadStats()])
}

async function decide(item: DiscussionReviewItem, status: 'approved' | 'rejected') {
  let reason = ''
  if (status === 'rejected') {
    try {
      const input = await ElMessageBox.prompt('驳回理由（会通知作者）', '驳回内容', {
        inputPlaceholder: '例如：含广告引流内容 / 与板块主题无关',
        inputValidator: (v) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的理由'),
      })
      reason = input.value
    } catch {
      return
    }
  }
  await reviewItem({ targetType: item.targetType, targetId: item.targetId, status, reason: reason || undefined })
  ElMessage.success(status === 'approved' ? '已通过' : '已驳回')
  await loadReviews()
  await loadStats()
}

async function flag(topicId: string, name: 'pinned' | 'featured' | 'locked', value: boolean) {
  await setTopicFlag(topicId, name, value)
  ElMessage.success('已更新')
  await loadReviews()
}

async function mute(authorId: string | null, days: number) {
  if (!authorId) return
  const label = days > 0 ? `禁言 ${days} 天` : '解除禁言'
  try {
    await ElMessageBox.confirm(`确认对该用户执行「${label}」？`, '用户处置', { type: 'warning' })
  } catch {
    return
  }
  await muteUser(authorId, days, '后台处置')
  ElMessage.success(label + '成功')
  await loadReviews()
}

// ---------------------------------------------------------------- 举报工作台

const reportQuery = reactive({ status: 'pending', page: 0, size: 20 })
const reports = ref<DiscussionReport[]>([])
const reportTotal = ref(0)
const reportLoading = ref(false)
/** 举报成立后的处置弹窗 */
const handleVisible = ref(false)
const handleTarget = ref<DiscussionReport | null>(null)
const handleForm = reactive<{ note: string; contentAction: 'none' | 'hide' | 'delete'; muteDays: number }>({
  note: '',
  contentAction: 'hide',
  muteDays: 0,
})

async function loadReports() {
  reportLoading.value = true
  try {
    const res = await listReports({ ...reportQuery })
    reports.value = res.data
    reportTotal.value = res.total
  } finally {
    reportLoading.value = false
  }
}

async function resolveReport(row: DiscussionReport, accepted: boolean) {
  if (accepted) {
    // 打开处置弹窗（处理说明 / 内容处置 / 禁言天数）
    handleTarget.value = row
    handleForm.note = ''
    handleForm.contentAction = 'hide'
    handleForm.muteDays = 0
    handleVisible.value = true
    return
  }
  try {
    const input = await ElMessageBox.prompt('驳回说明（会回执给举报人）', '举报不成立', {
      inputPlaceholder: '例如：经核实未违反社区规范',
    })
    await handleReport(row.id, { status: 'rejected', note: input.value || '经核实未违反社区规范' })
  } catch {
    return
  }
  ElMessage.success('已处理')
  await loadReports()
  await loadStats()
}

/** 举报成立 → 提交处置 */
async function submitHandle() {
  if (!handleTarget.value) return
  await handleReport(handleTarget.value.id, {
    status: 'accepted',
    note: handleForm.note || '已核实并处理',
    contentAction: handleForm.contentAction,
    muteDays: handleForm.muteDays,
  })
  handleVisible.value = false
  ElMessage.success('已处置并回执举报人')
  await Promise.all([loadReports(), loadReviews(), loadStats()])
}

// ---------------------------------------------------------------- 敏感词

const wordQuery = reactive({ keyword: '', page: 0, size: 20 })
const words = ref<SensitiveWord[]>([])
const wordTotal = ref(0)
const wordLoading = ref(false)
const wordForm = reactive({ word: '', level: 'watch', locale: 'zh', remark: '' })

async function loadWords() {
  wordLoading.value = true
  try {
    const res = await listWords({ ...wordQuery })
    words.value = res.data
    wordTotal.value = res.total
  } finally {
    wordLoading.value = false
  }
}

async function addWord() {
  if (!wordForm.word.trim()) {
    ElMessage.warning('请输入词条')
    return
  }
  await saveWord({ ...wordForm, enabled: true })
  ElMessage.success('已保存（立即生效）')
  wordForm.word = ''
  wordForm.remark = ''
  await loadWords()
}

async function removeWord(row: SensitiveWord) {
  try {
    await ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  await deleteWord(row.id)
  ElMessage.success('已删除')
  await loadWords()
}

/** 批量导入敏感词（换行/逗号/顿号分隔） */
const wordImportVisible = ref(false)
const wordImportForm = reactive({ text: '', level: 'watch', locale: 'zh' })

async function submitImport() {
  if (!wordImportForm.text.trim()) {
    ElMessage.warning('请粘贴要导入的词条')
    return
  }
  const res = await importWords({ ...wordImportForm })
  wordImportVisible.value = false
  wordImportForm.text = ''
  ElMessage.success(`导入完成：新增 ${res.added} 条，跳过 ${res.skipped} 条（重复或超长）`)
  await loadWords()
}

/** 导出为 txt（每行一条，可直接再导入） */
function exportWords() {
  const text = words.value.map((w) => w.word).join('\n')
  if (!text) {
    ElMessage.warning('当前页没有可导出的词条')
    return
  }
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `sensitive-words-${new Date().toISOString().slice(0, 10)}.txt`
  a.click()
  URL.revokeObjectURL(url)
}

// ---------------------------------------------------------------- 板块管理

const boards = ref<AdminBoard[]>([])
const boardLoading = ref(false)
const boardVisible = ref(false)
const boardForm = reactive({
  id: '',
  name: '',
  nameEn: '',
  description: '',
  orderNum: 0,
  postPolicy: 'post_then_review',
})

async function loadBoards() {
  boardLoading.value = true
  try {
    boards.value = await listBoards()
  } finally {
    boardLoading.value = false
  }
}

function openBoard(row?: AdminBoard) {
  boardForm.id = row?.id || ''
  boardForm.name = row?.name || ''
  boardForm.nameEn = row?.nameEn || ''
  boardForm.description = row?.description || ''
  boardForm.orderNum = row?.orderNum ?? 0
  boardForm.postPolicy = row?.postPolicy || 'post_then_review'
  boardVisible.value = true
}

async function submitBoard() {
  if (!boardForm.name.trim()) {
    ElMessage.warning('请填写板块名称')
    return
  }
  await saveBoard({ ...boardForm })
  boardVisible.value = false
  ElMessage.success('已保存')
  await Promise.all([loadBoards(), loadStats()])
}

async function switchBoard(row: AdminBoard) {
  const next = row.status !== 'active'
  try {
    await ElMessageBox.confirm(
      next ? `确认启用板块「${row.name}」？` : `确认停用板块「${row.name}」？停用后前台不再展示。`,
      '板块状态',
      { type: 'warning' },
    )
  } catch {
    return
  }
  await toggleBoard(row.id, next)
  ElMessage.success(next ? '已启用' : '已停用')
  await loadBoards()
}

// ---------------------------------------------------------------- 看板

const stats = ref<Record<string, number>>({})

async function loadStats() {
  try {
    stats.value = await discussionStats()
  } catch {
    stats.value = {}
  }
}

const LEVEL_LABEL: Record<string, string> = {
  block: '进待审',
  watch: '仅打标',
  replace: '替换为 *',
}

const REASON_LABEL: Record<string, string> = {
  spam: '广告 / 垃圾信息',
  abuse: '辱骂攻击',
  porn: '色情低俗',
  political: '违法违规',
  copyright: '侵权',
  other: '其他',
}

// ---------------------------------------------------------------- 站内公告（OA 群发）

const audience = ref<Partial<BroadcastAudience>>({})
const broadcasting = ref(false)
const broadcastForm = reactive({
  title: '',
  content: '',
  audience: 'all' as 'all' | 'active',
  link: '',
  email: false,
})
/** 本次会话内的发送记录（审计日志在服务端 discussion_moderation_log） */
const broadcastLog = ref<{ title: string; audience: string; recipients: number; email: boolean; sentAt: string }[]>([])

async function loadAudience() {
  try {
    audience.value = await broadcastAudience()
  } catch {
    audience.value = {}
  }
}

async function submitBroadcast() {
  const title = broadcastForm.title.trim()
  const content = broadcastForm.content.trim()
  if (!title) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (!content) {
    ElMessage.warning('请填写公告正文')
    return
  }
  const count = broadcastForm.audience === 'active' ? audience.value.active : audience.value.all
  try {
    await ElMessageBox.confirm(
      `将向「${broadcastForm.audience === 'active' ? '近 30 天活跃用户' : '全部注册用户'}」（约 ${count ?? '未知'} 人）发送站内公告` +
        (broadcastForm.email ? '，并同时发送邮件' : '') +
        '。确认发送？',
      '发送站内公告',
      { type: 'warning' },
    )
  } catch {
    return
  }
  broadcasting.value = true
  try {
    const res = await sendBroadcast({
      title,
      content,
      email: broadcastForm.email,
      audience: broadcastForm.audience,
      link: broadcastForm.link.trim() || undefined,
    })
    broadcastLog.value.unshift({
      title: res.title,
      audience: res.audience === 'active' ? '活跃用户' : '全部用户',
      recipients: res.recipients,
      email: res.email,
      sentAt: new Date().toLocaleString(),
    })
    ElMessage.success(`已发送给 ${res.recipients} 位用户${res.email ? '（邮件异步发送中）' : ''}`)
    broadcastForm.title = ''
    broadcastForm.content = ''
    broadcastForm.link = ''
    broadcastForm.email = false
    await Promise.all([loadStats(), loadAudience()])
  } catch (e) {
    ElMessage.error((e as Error).message || '发送失败')
  } finally {
    broadcasting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadStats(), loadReviews(), loadReports(), loadWords(), loadBoards(), loadAudience()])
})
</script>

<template>
  <div class="page">
    <!-- 看板 -->
    <el-row :gutter="12" class="stats">
      <el-col :span="4">
        <div class="stat-card"><span class="num">{{ stats.pendingTopics ?? 0 }}</span><span class="lbl">待审帖子</span></div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card"><span class="num">{{ stats.pendingPosts ?? 0 }}</span><span class="lbl">待审回复</span></div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card"><span class="num">{{ stats.pendingReports ?? 0 }}</span><span class="lbl">待处理举报</span></div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card"><span class="num">{{ stats.watchTopics ?? 0 }}</span><span class="lbl">敏感词打标</span></div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card"><span class="num">{{ stats.todayTopics ?? 0 }}</span><span class="lbl">今日新帖</span></div>
      </el-col>
      <el-col :span="4">
        <div class="stat-card"><span class="num">{{ stats.todayPosts ?? 0 }}</span><span class="lbl">今日回复</span></div>
      </el-col>
    </el-row>

    <el-tabs v-model="tab" class="tabs">
      <!-- 审核队列 -->
      <el-tab-pane label="审核队列" name="review">
        <div class="toolbar">
          <el-radio-group v-model="reviewQuery.targetType" @change="((reviewQuery.page = 0), loadReviews())">
            <el-radio-button value="discussion_topic">帖子</el-radio-button>
            <el-radio-button value="discussion_post">回复</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="reviewQuery.status" @change="((reviewQuery.page = 0), loadReviews())">
            <el-radio-button value="pending">待审</el-radio-button>
            <el-radio-button value="watch">敏感词打标</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="reviewQuery.sort" @change="((reviewQuery.page = 0), loadReviews())">
            <el-radio-button value="oldest">最早优先</el-radio-button>
            <el-radio-button value="priority">高优先级优先</el-radio-button>
          </el-radio-group>
          <el-button :disabled="!selected.length" type="success" @click="batchDecide('approved')">
            批量通过{{ selected.length ? `（${selected.length}）` : '' }}
          </el-button>
          <el-button :disabled="!selected.length" type="danger" @click="batchDecide('rejected')">
            批量驳回{{ selected.length ? `（${selected.length}）` : '' }}
          </el-button>
          <el-button @click="loadReviews">刷新</el-button>
        </div>

        <el-table
          v-loading="reviewLoading"
          :data="reviewItems"
          border
          stripe
          @selection-change="(rows: DiscussionReviewItem[]) => (selected = rows)"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              {{ row.targetType === 'discussion_topic' ? '帖子' : '回复' }}
            </template>
          </el-table-column>
          <el-table-column label="标题 / 位置" width="220">
            <template #default="{ row }">
              <div class="cell-title">{{ row.title }}</div>
              <div class="cell-sub">{{ row.context }}</div>
            </template>
          </el-table-column>
          <el-table-column label="内容摘要" min-width="280">
            <template #default="{ row }">
              <div class="cell-excerpt">{{ row.excerpt }}</div>
            </template>
          </el-table-column>
          <el-table-column label="作者" width="130" prop="authorName" />
          <el-table-column label="命中" width="170">
            <template #default="{ row }">
              <el-tag v-if="row.riskLevel === 'block'" type="danger" size="small">敏感词 · 进待审</el-tag>
              <el-tag v-else-if="row.riskLevel === 'watch'" type="warning" size="small">敏感词 · 打标</el-tag>
              <span v-else class="cell-sub">—</span>
              <div v-if="row.hitWords" class="cell-sub">{{ row.hitWords }}</div>
            </template>
          </el-table-column>
          <el-table-column label="被举报" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.reportCount" type="danger" size="small">{{ row.reportCount }}</el-tag>
              <span v-else>0</span>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="150" prop="createdAt" />
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="decide(row as DiscussionReviewItem, 'approved')">通过</el-button>
              <el-button size="small" type="danger" @click="decide(row as DiscussionReviewItem, 'rejected')">驳回</el-button>
              <el-button
                v-if="row.targetType === 'discussion_topic'"
                size="small"
                @click="flag(row.targetId, 'featured', true)"
                >加精</el-button
              >
              <el-button v-if="row.targetType === 'discussion_topic'" size="small" @click="flag(row.targetId, 'locked', true)"
                >锁定</el-button
              >
              <el-button size="small" type="warning" @click="mute(row.authorId, 1)">禁言1天</el-button>
              <el-button v-if="row.authorId" size="small" @click="mute(row.authorId, 0)">解禁</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pager"
          layout="total, prev, pager, next"
          :total="reviewTotal"
          :page-size="reviewQuery.size"
          :current-page="reviewQuery.page + 1"
          @current-change="(p: number) => ((reviewQuery.page = p - 1), loadReviews())"
        />
      </el-tab-pane>

      <!-- 举报工作台 -->
      <el-tab-pane label="举报工作台" name="report">
        <div class="toolbar">
          <el-radio-group v-model="reportQuery.status" @change="((reportQuery.page = 0), loadReports())">
            <el-radio-button value="pending">待处理</el-radio-button>
            <el-radio-button value="accepted">已成立</el-radio-button>
            <el-radio-button value="rejected">已驳回</el-radio-button>
            <el-radio-button value="all">全部</el-radio-button>
          </el-radio-group>
          <el-button @click="loadReports">刷新</el-button>
        </div>
        <el-table v-loading="reportLoading" :data="reports" border stripe>
          <el-table-column label="对象" width="100">
            <template #default="{ row }">
              {{ row.targetType === 'discussion_topic' ? '帖子' : '回复' }}
            </template>
          </el-table-column>
          <el-table-column label="被举报内容" min-width="260">
            <template #default="{ row }">
              <div class="cell-excerpt">{{ row.targetExcerpt }}</div>
              <div class="cell-sub">作者：{{ row.targetAuthor }}</div>
            </template>
          </el-table-column>
          <el-table-column label="举报人" width="120" prop="reporter" />
          <el-table-column label="原因" width="130">
            <template #default="{ row }">{{ REASON_LABEL[row.reason] || row.reason }}</template>
          </el-table-column>
          <el-table-column label="补充说明" min-width="160" prop="detail" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'pending'" type="warning" size="small">待处理</el-tag>
              <el-tag v-else-if="row.status === 'accepted'" type="success" size="small">已成立</el-tag>
              <el-tag v-else type="info" size="small">已驳回</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="150" prop="createdAt" />
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button size="small" type="danger" @click="resolveReport(row as DiscussionReport, true)">成立并处置</el-button>
                <el-button size="small" @click="resolveReport(row as DiscussionReport, false)">驳回</el-button>
              </template>
              <span v-else class="cell-sub">{{ row.resultNote || '—' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pager"
          layout="total, prev, pager, next"
          :total="reportTotal"
          :page-size="reportQuery.size"
          :current-page="reportQuery.page + 1"
          @current-change="(p: number) => ((reportQuery.page = p - 1), loadReports())"
        />
      </el-tab-pane>

      <!-- 敏感词 -->
      <el-tab-pane label="敏感词库" name="words">
        <div class="toolbar">
          <el-input v-model="wordQuery.keyword" placeholder="搜索词条" style="width: 200px" @keyup.enter="loadWords" />
          <el-button @click="loadWords">查询</el-button>
          <div class="spacer" />
          <el-input v-model="wordForm.word" placeholder="新增词条" style="width: 160px" />
          <el-select v-model="wordForm.level" style="width: 130px">
            <el-option label="进待审" value="block" />
            <el-option label="仅打标" value="watch" />
            <el-option label="替换为 *" value="replace" />
          </el-select>
          <el-input v-model="wordForm.remark" placeholder="备注（可选）" style="width: 160px" />
          <el-button type="primary" @click="addWord">添加</el-button>
          <el-button @click="wordImportVisible = true">批量导入</el-button>
          <el-button @click="exportWords">导出</el-button>
        </div>
        <el-alert
          type="info"
          :closable="false"
          title="本地敏感词：命中 block 的内容直接进入待审队列；watch 仅打标便于抽查；replace 会把词替换为 *。词库改动立即生效。"
        />
        <el-table v-loading="wordLoading" :data="words" border stripe>
          <el-table-column label="词条" min-width="160" prop="word" />
          <el-table-column label="语种" width="90" prop="locale" />
          <el-table-column label="等级" width="120">
            <template #default="{ row }">
              <el-tag :type="row.level === 'block' ? 'danger' : row.level === 'watch' ? 'warning' : 'info'" size="small">
                {{ LEVEL_LABEL[row.level] || row.level }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="80">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160" prop="remark" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="removeWord(row as SensitiveWord)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pager"
          layout="total, prev, pager, next"
          :total="wordTotal"
          :page-size="wordQuery.size"
          :current-page="wordQuery.page + 1"
          @current-change="(p: number) => ((wordQuery.page = p - 1), loadWords())"
        />
      </el-tab-pane>

      <!-- 板块管理 -->
      <el-tab-pane label="板块管理" name="boards">
        <div class="toolbar">
          <el-button type="primary" @click="openBoard()">新建板块</el-button>
          <el-button @click="loadBoards">刷新</el-button>
          <span class="cell-sub">停用后前台不再展示；「先审后发」会让该板块所有新帖进入待审队列。</span>
        </div>
        <el-table v-loading="boardLoading" :data="boards" border stripe>
          <el-table-column label="排序" width="70" prop="orderNum" />
          <el-table-column label="名称" min-width="160">
            <template #default="{ row }">
              <div class="cell-title">{{ row.name }}</div>
              <div class="cell-sub">{{ row.slug }}{{ row.nameEn ? ' · ' + row.nameEn : '' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="简介" min-width="240" prop="description" />
          <el-table-column label="发帖策略" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="row.postPolicy === 'review_then_post' ? 'warning' : 'info'">
                {{ row.postPolicy === 'review_then_post' ? '先审后发' : '先发后审' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="主题数" width="90" prop="topicCount" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openBoard(row as AdminBoard)">编辑</el-button>
              <el-button size="small" :type="row.status === 'active' ? 'danger' : 'success'" @click="switchBoard(row as AdminBoard)">
                {{ row.status === 'active' ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 站内公告（OA 群发） -->
      <el-tab-pane label="站内公告" name="broadcast">
        <el-alert
          type="warning"
          :closable="false"
          title="站内公告用于网站维护、规则变更等官方通知：会向所选受众写入站内通知（系统通知），勾选邮件时同时异步发信。社区事件（回复 / @ / 私信 / 举报回执）不再自动发邮件，只有审核结果会发信。"
        />
        <el-form label-width="90px" style="margin-top: 16px; max-width: 720px">
          <el-form-item label="标题" required>
            <el-input v-model="broadcastForm.title" maxlength="80" show-word-limit placeholder="例如：9 月 20 日 02:00-04:00 站点维护" />
          </el-form-item>
          <el-form-item label="正文" required>
            <el-input
              v-model="broadcastForm.content"
              type="textarea"
              :rows="6"
              maxlength="500"
              show-word-limit
              placeholder="维护期间站点将短暂不可访问，给您带来不便敬请谅解。"
            />
          </el-form-item>
          <el-form-item label="受众">
            <el-radio-group v-model="broadcastForm.audience">
              <el-radio-button value="all">全部注册用户（{{ audience.all ?? '-' }}）</el-radio-button>
              <el-radio-button value="active">近 {{ audience.activeWindowDays ?? 30 }} 天活跃（{{ audience.active ?? '-' }}）</el-radio-button>
            </el-radio-group>
            <span class="cell-sub" style="margin-left: 10px">
              单次上限 {{ audience.maxRecipients ?? '-' }} 人
            </span>
          </el-form-item>
          <el-form-item label="跳转链接">
            <el-input v-model="broadcastForm.link" placeholder="可选，站内路径，如 /discussion" />
          </el-form-item>
          <el-form-item label="邮件">
            <el-switch v-model="broadcastForm.email" />
            <span class="cell-sub" style="margin-left: 10px">
              打开后会向每位收件人的注册邮箱异步发信（仅审核结果以外的公告用途，请谨慎开启）
            </span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="broadcasting" @click="submitBroadcast">发送公告</el-button>
            <el-button @click="loadAudience">刷新受众人数</el-button>
            <span class="cell-sub" style="margin-left: 10px">每小时最多发送 10 次，每次都会写入审核审计日志。</span>
          </el-form-item>
        </el-form>
        <el-table v-if="broadcastLog.length" :data="broadcastLog" border stripe style="max-width: 720px">
          <el-table-column label="标题" min-width="200" prop="title" />
          <el-table-column label="受众" width="120" prop="audience" />
          <el-table-column label="收件人" width="90" prop="recipients" />
          <el-table-column label="邮件" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.email ? 'warning' : 'info'">{{ row.email ? '已发' : '未发' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发送时间" width="170" prop="sentAt" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 敏感词批量导入弹窗 -->
    <el-dialog v-model="wordImportVisible" title="批量导入敏感词" width="520">
      <el-alert
        type="info"
        :closable="false"
        title="每行一条，也支持逗号、顿号、分号分隔；已存在的词条会自动跳过（不覆盖其等级）。"
      />
      <el-input
        v-model="wordImportForm.text"
        style="margin-top: 12px"
        type="textarea"
        :rows="8"
        placeholder="例如：&#10;加微信&#10;代购,刷单&#10;博彩、赌博"
      />
      <div style="display: flex; gap: 10px; margin-top: 12px">
        <el-select v-model="wordImportForm.level" style="width: 150px">
          <el-option label="进待审（block）" value="block" />
          <el-option label="仅打标（watch）" value="watch" />
          <el-option label="替换为 *（replace）" value="replace" />
        </el-select>
        <el-select v-model="wordImportForm.locale" style="width: 120px">
          <el-option label="中文 zh" value="zh" />
          <el-option label="英文 en" value="en" />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="wordImportVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- 板块编辑弹窗 -->
    <el-dialog v-model="boardVisible" :title="boardForm.id ? '编辑板块' : '新建板块'" width="520">
      <el-form label-width="96px">
        <el-form-item label="板块名称">
          <el-input v-model="boardForm.name" maxlength="64" placeholder="例如：民族音乐" />
        </el-form-item>
        <el-form-item label="英文名称">
          <el-input v-model="boardForm.nameEn" maxlength="64" placeholder="Ethnic Music" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="boardForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="boardForm.orderNum" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="发帖策略">
          <el-select v-model="boardForm.postPolicy" style="width: 100%">
            <el-option label="先发后审（推荐）" value="post_then_review" />
            <el-option label="先审后发（严格）" value="review_then_post" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="boardVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBoard">保存</el-button>
      </template>
    </el-dialog>

    <!-- 举报成立 · 处置弹窗 -->
    <el-dialog v-model="handleVisible" title="举报成立 · 处置" width="460">
      <el-form label-width="96px">
        <el-form-item label="被举报内容">
          <div class="cell-excerpt">{{ handleTarget?.targetExcerpt }}</div>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="handleForm.note" placeholder="例如：已核实并隐藏内容（会回执给举报人）" />
        </el-form-item>
        <el-form-item label="内容处置">
          <el-select v-model="handleForm.contentAction" style="width: 100%">
            <el-option label="隐藏内容" value="hide" />
            <el-option label="删除内容" value="delete" />
            <el-option label="不处置内容" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item label="禁言天数">
          <el-input-number v-model="handleForm.muteDays" :min="0" :max="365" />
          <span class="cell-sub" style="margin-left: 10px">0 表示不禁言</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">提交处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 4px;
}
.stats {
  margin-bottom: 14px;
}
.stat-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--el-bg-color);
}
.stat-card .num {
  font-size: 22px;
  font-weight: 700;
  color: #b6402e;
}
.stat-card .lbl {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.spacer {
  flex: 1;
}
.cell-title {
  font-weight: 600;
}
.cell-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.cell-excerpt {
  font-size: 13px;
  line-height: 1.7;
  word-break: break-all;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
