<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '@/api/modules'
import type { FullTextSearchResult, SearchHit } from '@/api/types'
import CoverImage from '@/components/CoverImage.vue'
import Reveal from '@/components/Reveal.vue'
import RecommendationPanel from '@/components/RecommendationPanel.vue'
import { ethnicImagePrompt } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

/**
 * 全文检索页（方向 D）。
 *
 * 与旧版的区别：
 *  · 覆盖 8 类内容（原仅 民族/节日/艺术），统一结果流按相关度混排；
 *  · 支持 中文子串 / 拼音全拼 / 拼音首字母 / 英文 四种输入；
 *  · 关键词高亮（后端返回 titleHtml / summaryHtml，已做 HTML 转义）；
 *  · 左侧分面按内容类型过滤，并显示每类命中数；
 *  · 如实展示检索耗时与命中方式（如「按拼音匹配」）。
 */
const route = useRoute()
const router = useRouter()
const lang = useLangStore()

const q = ref((route.query.q as string) || '')
const typeFilter = ref((route.query.type as string) || 'all')
const hot = ref<string[]>([])
const result = ref<FullTextSearchResult | null>(null)
const searching = ref(false)
const searched = ref(false)
const page = ref(0)
const size = 20

/** 内容类型 → 中文名（与后端 doc_type 对应） */
const DOC_TYPE_LABEL: Record<string, string> = {
  ethnic: '民族',
  festival: '节日',
  art: '艺术',
  food: '美食',
  custom: '风俗',
  person: '人物',
  area: '自治地方',
  sport: '传统体育',
}

/** 检索方式说明（让用户知道支持哪些输入） */
const PLACEHOLDER = '搜索民族 / 节日 / 艺术 / 美食 / 人物…（支持拼音 mgz、mengguzu、英文）'

const totalPages = computed(() =>
  result.value ? Math.max(1, Math.ceil(result.value.total / size)) : 1,
)

/** 命中方式提示：如「按拼音匹配」 */
function matchHint(hit: SearchHit): string {
  switch (hit.matchBy) {
    case 'pinyin': return lang.pick('按拼音匹配', 'matched by pinyin')
    case 'abbr': return lang.pick('按首字母匹配', 'matched by initials')
    case 'titleEn': return lang.pick('按英文名匹配', 'matched by English name')
    case 'title': return lang.pick('标题命中', 'in title')
    default: return lang.pick('正文命中', 'in content')
  }
}

async function loadHot() {
  try {
    hot.value = await searchApi.hot()
  } catch {
    hot.value = []
  }
}

async function doSearch(resetPage = true) {
  const keyword = q.value.trim()
  if (resetPage) page.value = 0
  searching.value = true
  searched.value = true
  try {
    result.value = await searchApi.full({
      q: keyword,
      type: typeFilter.value,
      page: page.value,
      size,
    })
    // 同步到地址栏，便于分享与前进后退
    router.replace({
      path: '/search',
      query: {
        ...(keyword ? { q: keyword } : {}),
        ...(typeFilter.value !== 'all' ? { type: typeFilter.value } : {}),
      },
    })
  } catch {
    result.value = null
  } finally {
    searching.value = false
  }
}

function useHot(word: string) {
  q.value = word
  doSearch()
}

function pickType(t: string) {
  typeFilter.value = t
  doSearch()
}

function goPage(p: number) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  doSearch(false)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/** 分面列表（按命中数降序，all 置顶） */
const facetList = computed(() => {
  if (!result.value) return [] as { key: string; label: string; count: number }[]
  const entries = Object.entries(result.value.facets || {})
    .sort((a, b) => b[1] - a[1])
    .map(([key, count]) => ({ key, label: DOC_TYPE_LABEL[key] || key, count }))
  return entries
})

onMounted(() => {
  loadHot()
  if (q.value) doSearch()
})

// 站内跳转（如点击标签）会复用组件实例，需跟随 query 变化
watch(
  () => route.query.q,
  (next) => {
    const keyword = String(next || '')
    if (keyword === q.value.trim()) return
    q.value = keyword
    if (keyword) doSearch()
    else {
      searched.value = false
      result.value = null
    }
  },
)
</script>

<template>
  <div class="container">
    <div class="search-hero">
      <div class="kicker">
        {{ lang.pick('SEARCH · 全文检索', 'SEARCH') }}
      </div>
      <div class="search-box">
        <input
          v-model="q"
          type="text"
          :placeholder="PLACEHOLDER"
          @keyup.enter="doSearch()"
        />
        <button class="go" aria-label="搜索" @click="doSearch()">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" stroke-linecap="round" />
          </svg>
        </button>
      </div>

      <!-- 输入方式说明：让用户知道支持拼音与首字母 -->
      <p class="hint">
        {{ lang.pick(
          '支持中文、中文片段、拼音全拼（mengguzu）、拼音首字母（mgz）、英文名检索',
          'Search by Chinese, pinyin (mengguzu), pinyin initials (mgz), or English name',
        ) }}
      </p>

      <div v-if="hot.length" class="hot">
        <span>{{ lang.pick('热门搜索', 'HOT') }}</span>
        <button v-for="w in hot" :key="w" class="chip" @click="useHot(w)">{{ w }}</button>
      </div>
    </div>

    <!-- 结果统计条：如实展示总数、耗时与类型分布 -->
    <div v-if="searched && result" class="stat-bar">
      <span class="stat-total">
        {{ lang.pick('共', '') }} <strong>{{ result.total }}</strong> {{ lang.pick('条结果', 'results') }}
      </span>
      <span class="stat-took">{{ result.tookMs }} ms</span>
      <span v-if="result.keyword" class="stat-kw">
        “<em>{{ result.keyword }}</em>”
      </span>
    </div>

    <div class="layout">
      <!-- 左侧分面 -->
      <aside v-if="searched && result && facetList.length" class="facets">
        <h4>{{ lang.pick('内容类型', 'Type') }}</h4>
        <ul>
          <li>
            <button :class="{ on: typeFilter === 'all' }" @click="pickType('all')">
              <span>{{ lang.pick('全部', 'All') }}</span>
              <b>{{ result.total }}</b>
            </button>
          </li>
          <li v-for="f in facetList" :key="f.key">
            <button :class="{ on: typeFilter === f.key }" @click="pickType(f.key)">
              <span>{{ f.label }}</span>
              <b>{{ f.count }}</b>
            </button>
          </li>
        </ul>
      </aside>

      <!-- 未检索时展示个性化推荐，让空状态也有内容可看 -->
      <div v-else-if="!searched" class="reco-slot">
        <RecommendationPanel :size="8" />
      </div>

      <!-- 结果列表 -->
      <div class="results">
        <el-skeleton v-if="searching" :rows="8" animated />
        <el-empty v-else-if="searched && !result" description="检索失败，请重试" style="padding: 48px 0" />
        <el-empty
          v-else-if="searched && result && result.total === 0"
          :description="`没有找到与「${result.keyword}」相关的内容`"
          style="padding: 48px 0"
        />

        <template v-else-if="result">
          <Reveal
            v-for="(hit, i) in result.list"
            :key="hit.docType + ':' + hit.docId"
            :y="12"
            :delay="Math.min(i * 30, 240)"
          >
            <router-link :to="hit.url" class="hit">
              <div class="hit-cover">
                <CoverImage
                  :src="hit.coverImage"
                  :name="hit.title"
                  :prompt="ethnicImagePrompt(hit.title)"
                  size="landscape_4_3"
                />
              </div>
              <div class="hit-body">
                <div class="hit-meta">
                  <span class="badge">{{ DOC_TYPE_LABEL[hit.docType] || hit.docType }}</span>
                  <span v-if="hit.ethnicName" class="ethnic">{{ hit.ethnicName }}</span>
                  <span v-if="hit.category" class="cat">{{ hit.category }}</span>
                  <span v-if="hit.region" class="cat">{{ hit.region }}</span>
                  <!-- 命中方式：拼音/首字母/英文命中时给出提示，解释为何这条会出现 -->
                  <span class="match" :class="hit.matchBy">{{ matchHint(hit) }}</span>
                </div>
                <!-- 标题与摘要使用后端高亮 HTML（已转义，安全） -->
                <h3 class="hit-title" v-html="hit.titleHtml"></h3>
                <p class="hit-summary" v-html="hit.summaryHtml"></p>
              </div>
            </router-link>
          </Reveal>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="pager">
            <button :disabled="page === 0" @click="goPage(page - 1)">
              {{ lang.pick('上一页', 'Prev') }}
            </button>
            <span>{{ page + 1 }} / {{ totalPages }}</span>
            <button :disabled="page >= totalPages - 1" @click="goPage(page + 1)">
              {{ lang.pick('下一页', 'Next') }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-hero {
  padding: 48px 0 8px;
  text-align: center;
}
.kicker {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--accent);
  text-transform: uppercase;
  margin-bottom: 18px;
}
.hint {
  margin: 12px 0 0;
  font-size: 12.5px;
  color: var(--muted);
}
.hot {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  color: var(--muted);
}

/* 统计条 */
.stat-bar {
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding: 14px 0;
  margin-top: 12px;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  font-size: 13px;
  color: var(--muted);
}
.stat-total strong {
  color: var(--accent);
  font-size: 17px;
  font-family: var(--serif);
}
.stat-took {
  font-variant-numeric: tabular-nums;
  font-size: 12px;
}
.stat-kw em {
  color: var(--ink-1);
  font-style: normal;
}

/* 左右布局：分面 + 结果 */
.layout {
  display: grid;
  grid-template-columns: 168px 1fr;
  gap: 32px;
  margin-top: 24px;
  align-items: start;
}
.facets h4 {
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--muted);
}
.facets ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.facets button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 7px 10px;
  border: 0;
  border-left: 2px solid transparent;
  background: none;
  font: inherit;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.15s ease;
}
.facets button:hover {
  color: var(--accent);
  background: var(--paper-2);
}
.facets button.on {
  border-left-color: var(--accent);
  color: var(--accent);
  font-weight: 600;
  background: var(--paper-2);
}
.facets button b {
  font-size: 11.5px;
  font-weight: 400;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}

/* 结果条目 */
.results {
  min-width: 0;
}
.hit {
  display: grid;
  grid-template-columns: 132px 1fr;
  gap: 18px;
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
  color: inherit;
  text-decoration: none;
}
.hit:hover .hit-title {
  color: var(--accent);
}
.hit-cover {
  aspect-ratio: 4/3;
  overflow: hidden;
  border: 1px solid var(--line);
}
.hit-cover :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hit-body {
  min-width: 0;
}
.hit-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.badge {
  padding: 1px 8px;
  border-radius: 3px;
  font-size: 11px;
  color: #fff;
  background: var(--accent);
}
.ethnic {
  font-size: 12px;
  color: var(--ink-2);
}
.cat {
  font-size: 11.5px;
  color: var(--muted);
}
/* 命中方式标签：拼音/首字母/英文命中用不同底色，便于理解排序来源 */
.match {
  margin-left: auto;
  padding: 1px 7px;
  border-radius: 3px;
  font-size: 10.5px;
  color: var(--muted);
  background: var(--paper-2);
  border: 1px solid var(--line);
}
.match.pinyin,
.match.abbr {
  color: #2e6da4;
  border-color: color-mix(in srgb, #2e6da4 30%, transparent);
  background: color-mix(in srgb, #2e6da4 8%, transparent);
}
.match.titleEn {
  color: #7a8b3a;
  border-color: color-mix(in srgb, #7a8b3a 30%, transparent);
  background: color-mix(in srgb, #7a8b3a 8%, transparent);
}
.hit-title {
  margin: 0 0 6px;
  font-size: 16.5px;
  font-family: var(--serif);
  font-weight: 600;
  color: var(--ink-1);
  transition: color 0.15s ease;
}
.hit-summary {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 关键词高亮：后端返回 <em class="hl">，需穿透 scoped */
:deep(.hl) {
  font-style: normal;
  color: var(--accent);
  font-weight: 600;
  background: color-mix(in srgb, var(--accent) 12%, transparent);
  padding: 0 1px;
  border-radius: 2px;
}

/* 未检索时的推荐区 */
.reco-slot {
  grid-column: 1 / -1;
}

/* 分页 */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
  font-size: 13px;
  color: var(--muted);
}
.pager button {
  padding: 6px 14px;
  border: 1px solid var(--line);
  background: none;
  font: inherit;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
}
.pager button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.pager button:not(:disabled):hover {
  border-color: var(--accent);
  color: var(--accent);
}

@media (max-width: 720px) {
  .layout {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .facets ul {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .facets button {
    width: auto;
    border-left: 0;
    border: 1px solid var(--line);
    border-radius: 999px;
    padding: 4px 12px;
    gap: 6px;
  }
  .facets button.on {
    border-color: var(--accent);
  }
  .hit {
    grid-template-columns: 96px 1fr;
    gap: 12px;
  }
}
</style>
