<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteGlossary,
  exportGlossaryText,
  glossaryLocales,
  glossaryStats,
  importGlossary,
  listGlossary,
  previewGlossary,
  refreshGlossary,
  saveGlossary,
  type GlossaryStats,
  type GlossaryTerm,
} from '@/api/modules/translate'

/**
 * 翻译词表：glossary（本地零依赖兜底翻译）的术语库，存库、改完即生效。
 * 匹配规则：CJK 术语按最长匹配、拉丁术语按词边界忽略大小写；只有正向词条时反向自动推导。
 */
const query = reactive({ keyword: '', targetLocale: 'en', page: 0, size: 20 })
const rows = ref<GlossaryTerm[]>([])
const total = ref(0)
const loading = ref(false)
const stats = ref<GlossaryStats>({ total: 0, enabled: 0, locales: {} })
const locales = ref<Record<string, number>>({})

const LOCALE_LABEL: Record<string, string> = {
  zh: '中文',
  en: '英文',
  ja: '日文',
  ko: '韩文',
  fr: '法文',
  de: '德文',
  es: '西班牙文',
  ru: '俄文',
  ar: '阿拉伯文',
}

const localeOptions = computed(() => {
  const options = Object.keys(locales.value).map((code) => ({
    value: code,
    label: `${LOCALE_LABEL[code] || code}（${code}）· ${locales.value[code]} 条`,
  }))
  if (!options.some((o) => o.value === query.targetLocale)) {
    options.unshift({ value: query.targetLocale, label: `${LOCALE_LABEL[query.targetLocale] || query.targetLocale}（${query.targetLocale}）` })
  }
  return options
})

async function load() {
  loading.value = true
  try {
    const res = await listGlossary({ ...query })
    rows.value = res.data
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  try {
    const [s, l] = await Promise.all([glossaryStats(), glossaryLocales()])
    stats.value = s
    locales.value = l
  } catch {
    stats.value = { total: 0, enabled: 0, locales: {} }
    locales.value = {}
  }
}

function search() {
  query.page = 0
  load()
}

// ---------------------------------------------------------------- 新增 / 编辑

const formVisible = ref(false)
const submitting = ref(false)
const form = reactive({
  id: '',
  /** 原术语：重命名时用于定位（为空表示新增） */
  currentTerm: '',
  sourceLocale: 'zh',
  targetLocale: 'en',
  term: '',
  translation: '',
  enabled: true,
  remark: '',
})

function openForm(row?: GlossaryTerm) {
  form.id = row?.id || ''
  form.currentTerm = row?.term || ''
  form.sourceLocale = row?.sourceLocale || 'zh'
  form.targetLocale = row?.targetLocale || 'en'
  form.term = row?.term || ''
  form.translation = row?.translation || ''
  form.enabled = row?.enabled ?? true
  form.remark = row?.remark || ''
  formVisible.value = true
}

async function submitForm() {
  if (!form.term.trim()) {
    ElMessage.warning('请填写术语')
    return
  }
  if (!form.translation.trim()) {
    ElMessage.warning('请填写译文')
    return
  }
  submitting.value = true
  try {
    await saveGlossary(
      {
        sourceLocale: form.sourceLocale,
        targetLocale: form.targetLocale,
        term: form.term.trim(),
        translation: form.translation.trim(),
        enabled: form.enabled,
        remark: form.remark,
      },
      form.currentTerm || undefined,
    )
    formVisible.value = false
    ElMessage.success('已保存（最长 60 秒内生效，可点「刷新缓存」立即生效）')
    await Promise.all([load(), loadMeta()])
  } catch (e) {
    // 统一错误提示已由请求拦截器给出
    void e
  } finally {
    submitting.value = false
  }
}

async function toggleEnabled(row: GlossaryTerm) {
  await saveGlossary(
    {
      sourceLocale: row.sourceLocale,
      targetLocale: row.targetLocale,
      term: row.term,
      translation: row.translation,
      enabled: !row.enabled,
      remark: row.remark || undefined,
    },
    row.term,
  )
  ElMessage.success(row.enabled ? '已停用（该术语不再参与替换）' : '已启用')
  await Promise.all([load(), loadMeta()])
}

async function remove(row: GlossaryTerm) {
  try {
    await ElMessageBox.confirm(`确认删除词条「${row.term} = ${row.translation}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  await deleteGlossary(row.id)
  ElMessage.success('已删除')
  await Promise.all([load(), loadMeta()])
}

// ---------------------------------------------------------------- 批量导入 / 导出

const importVisible = ref(false)
const importForm = reactive({ text: '', sourceLocale: 'zh', targetLocale: 'en' })

function openImport() {
  importForm.sourceLocale = 'zh'
  importForm.targetLocale = query.targetLocale
  importVisible.value = true
}

async function submitImport() {
  if (!importForm.text.trim()) {
    ElMessage.warning('请粘贴要导入的词条')
    return
  }
  const res = await importGlossary({ ...importForm })
  importVisible.value = false
  importForm.text = ''
  ElMessage.success(`导入完成：新增 ${res.added} 条，跳过 ${res.skipped} 条（重复、缺「=」或超长）`)
  await Promise.all([load(), loadMeta()])
}

async function exportAll() {
  const text = await exportGlossaryText(query.targetLocale)
  if (!text) {
    ElMessage.warning('该目标语言暂无可导出的词条')
    return
  }
  const count = text.split('\n').filter(Boolean).length
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `glossary-${query.targetLocale}-${new Date().toISOString().slice(0, 10)}.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${count} 条（每行「术语=译文」，可直接再导入）`)
}

async function doRefresh() {
  const res = await refreshGlossary()
  const summary = Object.entries(res.locales || {})
    .map(([k, v]) => `${k}: ${v}`)
    .join('，')
  ElMessage.success(`词表缓存已刷新${summary ? `（${summary}）` : ''}`)
  await loadMeta()
}

// ---------------------------------------------------------------- 试译预览

const previewVisible = ref(false)
const previewForm = reactive({ text: '', sourceLocale: 'zh', targetLocale: 'en' })
const previewResult = ref<{
  original: string
  translated: string
  hit: boolean
  hits: number
  coverage: number
  accepted: boolean
  minCoverage: number
} | null>(null)

function openPreview() {
  previewForm.sourceLocale = 'zh'
  previewForm.targetLocale = query.targetLocale
  previewResult.value = null
  previewVisible.value = true
}

async function runPreview() {
  if (!previewForm.text.trim()) {
    ElMessage.warning('请输入要试译的文本')
    return
  }
  previewResult.value = await previewGlossary({ ...previewForm })
}

onMounted(async () => {
  await Promise.all([load(), loadMeta()])
})
</script>

<template>
  <div class="page">
    <el-row :gutter="12" class="stats">
      <el-col :span="6">
        <div class="stat-card"><span class="num">{{ stats.total ?? 0 }}</span><span class="lbl">词条总数</span></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card"><span class="num">{{ stats.enabled ?? 0 }}</span><span class="lbl">启用中</span></div>
      </el-col>
      <el-col :span="12">
        <div class="stat-card">
          <span class="num">{{ Object.keys(stats.locales || {}).length }}</span>
          <span class="lbl">
            目标语言：{{
              Object.entries(stats.locales || {})
                .map(([k, v]) => `${LOCALE_LABEL[k] || k} ${v}`)
                .join(' · ') || '暂无'
            }}
          </span>
        </div>
      </el-col>
    </el-row>

    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      <template #title>
        站点默认「<strong>词表优先 + AI 兜底</strong>」：先按词表翻译，命中术语但
        <strong>覆盖率不足</strong>（默认 80%，见 <code>app.translate.glossary-min-coverage</code>）或一条未命中时，
        自动改由 AI 通道（<code>spring-ai</code>）翻译。AI 翻译时会把它给出的民族文化术语
        <strong>实时沉淀</strong>回本词表（备注为「AI 自动提取」，只新增不覆盖，术语必须出现在原文里），
        因此词表会越用越准——可搜索「AI 自动提取」复核这些术语。
        匹配规则：CJK 术语按<strong>最长匹配</strong>、拉丁术语按<strong>词边界忽略大小写</strong>；
        只有正向词条时<strong>反向自动推导</strong>（zh→en 的词条同时可用于 en→zh）。改动最长 60 秒自动生效，点「刷新缓存」立即生效。
      </template>
    </el-alert>

    <div class="toolbar">
      <el-select v-model="query.targetLocale" style="width: 220px" @change="search">
        <el-option v-for="o in localeOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索术语 / 译文"
        style="width: 220px"
        clearable
        @keyup.enter="search"
      />
      <el-button @click="search">查询</el-button>
      <div class="spacer" />
      <el-button type="primary" @click="openForm()">新增词条</el-button>
      <el-button @click="openImport">批量导入</el-button>
      <el-button @click="exportAll">导出全部</el-button>
      <el-button @click="openPreview">试译预览</el-button>
      <el-button @click="doRefresh">刷新缓存</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column label="术语" min-width="200">
        <template #default="{ row }">
          <div class="cell-title">{{ row.term }}</div>
        </template>
      </el-table-column>
      <el-table-column label="译文" min-width="200">
        <template #default="{ row }">
          <div class="cell-title">{{ row.translation }}</div>
        </template>
      </el-table-column>
      <el-table-column label="方向" width="130">
        <template #default="{ row }">
          <el-tag size="small" type="info">
            {{ (LOCALE_LABEL[row.sourceLocale] || row.sourceLocale) }} → {{ (LOCALE_LABEL[row.targetLocale] || row.targetLocale) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="140">
        <template #default="{ row }">
          <span class="cell-sub">{{ row.remark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openForm(row as GlossaryTerm)">编辑</el-button>
          <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggleEnabled(row as GlossaryTerm)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="remove(row as GlossaryTerm)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page + 1"
      @current-change="(p: number) => ((query.page = p - 1), load())"
    />

    <!-- 新增 / 编辑词条 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑词条' : '新增词条'" width="560">
      <el-form label-width="90px">
        <el-form-item label="术语" required>
          <el-input v-model="form.term" placeholder="如：蒙古族 / Mongolian" />
        </el-form-item>
        <el-form-item label="译文" required>
          <el-input v-model="form.translation" placeholder="如：Mongolian / 蒙古族" />
        </el-form-item>
        <el-form-item label="方向">
          <el-select v-model="form.sourceLocale" style="width: 130px">
            <el-option v-for="(label, code) in LOCALE_LABEL" :key="code" :label="label" :value="code" />
          </el-select>
          <span style="margin: 0 8px">→</span>
          <el-select v-model="form.targetLocale" style="width: 130px">
            <el-option v-for="(label, code) in LOCALE_LABEL" :key="code" :label="label" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
          <span class="cell-sub" style="margin-left: 10px">停用后该术语不参与替换（但仍保留在词表中）</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选，便于后续维护" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVisible" title="批量导入词条" width="620">
      <el-alert
        type="info"
        :closable="false"
        title="每行一条「术语=译文」，也支持 → / 全角＝ / 制表符分隔；已存在的术语会自动跳过，不会覆盖已有译文。"
      />
      <div style="display: flex; gap: 10px; margin: 12px 0">
        <el-select v-model="importForm.sourceLocale" style="width: 140px">
          <el-option v-for="(label, code) in LOCALE_LABEL" :key="code" :label="label" :value="code" />
        </el-select>
        <span style="line-height: 32px">→</span>
        <el-select v-model="importForm.targetLocale" style="width: 140px">
          <el-option v-for="(label, code) in LOCALE_LABEL" :key="code" :label="label" :value="code" />
        </el-select>
      </div>
      <el-input
        v-model="importForm.text"
        type="textarea"
        :rows="10"
        placeholder="蒙古族=Mongolian&#10;那达慕=Nadam&#10;马头琴=morin khuur"
      />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- 试译预览 -->
    <el-dialog v-model="previewVisible" title="试译预览（不写译文缓存）" width="620">
      <div style="display: flex; gap: 10px; margin-bottom: 12px">
        <el-select v-model="previewForm.sourceLocale" style="width: 140px">
          <el-option v-for="(label, code) in LOCALE_LABEL" :key="code" :label="label" :value="code" />
        </el-select>
        <span style="line-height: 32px">→</span>
        <el-select v-model="previewForm.targetLocale" style="width: 140px">
          <el-option v-for="(label, code) in LOCALE_LABEL" :key="code" :label="label" :value="code" />
        </el-select>
      </div>
      <el-input
        v-model="previewForm.text"
        type="textarea"
        :rows="4"
        placeholder="粘贴一段帖子正文，查看当前词表会替换成什么"
      />
      <div style="margin-top: 12px">
        <el-button type="primary" @click="runPreview">试译</el-button>
      </div>
      <template v-if="previewResult">
        <el-divider content-position="left">结果</el-divider>
        <el-alert
          :type="previewResult.accepted ? 'success' : previewResult.hit ? 'warning' : 'info'"
          :closable="false"
          :title="
            previewResult.accepted
              ? `词表直接可用（命中 ${previewResult.hits} 条，覆盖率 ${(previewResult.coverage * 100).toFixed(1)}%）`
              : previewResult.hit
                ? `命中 ${previewResult.hits} 条，但覆盖率仅 ${(previewResult.coverage * 100).toFixed(1)}%（门限 ${(previewResult.minCoverage * 100).toFixed(0)}%）：这条内容会交给兜底通道翻译`
                : '未命中任何词条：这条内容会交给兜底通道翻译'
          "
        />
        <p class="cell-excerpt" style="margin-top: 10px">{{ previewResult.translated || '（词表未产出译文）' }}</p>
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
