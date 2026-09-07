<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '@/api/modules'
import type { SearchResult, EthnicListItem, FestivalListItem, ArtListItem } from '@/api/types'
import { artCategoryLabel, festivalTypeLabel } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

const route = useRoute()
const router = useRouter()
const lang = useLangStore()

const q = ref((route.query.q as string) || '')
const hot = ref<string[]>([])
const result = ref<SearchResult | null>(null)
const searching = ref(false)
const searched = ref(false)

async function loadHot() {
  try {
    hot.value = await searchApi.hot()
  } catch {
    hot.value = []
  }
}

async function doSearch() {
  const keyword = q.value.trim()
  if (!keyword) return
  router.replace({ path: '/search', query: { q: keyword } })
  searching.value = true
  searched.value = true
  try {
    result.value = await searchApi.search(keyword, 'all')
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

function groupTotal(): number {
  if (!result.value) return 0
  return result.value.ethnic.total + result.value.festival.total + result.value.art.total
}

onMounted(() => {
  loadHot()
  if (q.value) doSearch()
})
</script>

<template>
  <div class="container">
    <div class="search-hero">
      <div class="kicker" style="font-size: 12px; letter-spacing: 3px; color: var(--accent); text-transform: uppercase; margin-bottom: 18px">
        {{ lang.pick('SEARCH · 搜索', 'SEARCH') }}
      </div>
      <div class="search-box">
        <input v-model="q" type="text" :placeholder="lang.pick('搜索民族 / 节日 / 艺术…', 'Search ethnic groups, festivals, arts…')" @keyup.enter="doSearch" />
        <button class="go" aria-label="搜索" @click="doSearch">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5" stroke-linecap="round"/></svg>
        </button>
      </div>
      <div v-if="hot.length" class="hot">
        <span>{{ lang.pick('热门搜索', 'HOT') }}</span>
        <button v-for="w in hot" :key="w" class="chip" @click="useHot(w)">{{ w }}</button>
      </div>
    </div>

    <div v-if="searched" class="result-count">{{ lang.t('result_count', { n: groupTotal() }) }}</div>

    <el-skeleton v-if="searching" :rows="8" animated />
    <el-empty v-else-if="searched && !result" description="搜索失败，请重试" style="padding: 48px 0" />
    <el-empty v-else-if="searched && groupTotal() === 0" description="没有找到相关结果" style="padding: 48px 0" />

    <template v-else-if="result">
      <!-- 民族 -->
      <div v-if="result.ethnic.total > 0" class="search-group">
        <div class="g-head"><h3>民族</h3><span class="cnt">{{ result.ethnic.total }}</span></div>
        <div class="grid grid-4">
          <router-link v-for="e in result.ethnic.list as EthnicListItem[]" :key="e.id" class="ethnic-card" :to="`/ethnic/${e.id}`">
            <div class="body">
              <div class="name">{{ e.name }}</div>
              <div class="meta">{{ e.region?.[0] || '' }} · {{ e.languageFamily || '' }}</div>
            </div>
          </router-link>
        </div>
      </div>

      <!-- 节日 -->
      <div v-if="result.festival.total > 0" class="search-group">
        <div class="g-head"><h3>节日</h3><span class="cnt">{{ result.festival.total }}</span></div>
        <div class="grid grid-3">
          <router-link v-for="f in result.festival.list as FestivalListItem[]" :key="f.id" class="feature" :to="`/festival/${f.id}`">
            <div class="t">
              <span class="no">{{ festivalTypeLabel[f.type] || f.type }}</span>
              <h4>{{ f.name }}</h4>
              <p>{{ f.ethnicGroupName }} · {{ f.lunarDate || f.solarDate || '' }}</p>
            </div>
          </router-link>
        </div>
      </div>

      <!-- 艺术 -->
      <div v-if="result.art.total > 0" class="search-group">
        <div class="g-head"><h3>艺术</h3><span class="cnt">{{ result.art.total }}</span></div>
        <div class="grid grid-3">
          <router-link v-for="a in result.art.list as ArtListItem[]" :key="a.id" class="feature" :to="`/art/${a.id}`">
            <div class="t">
              <span class="no">{{ artCategoryLabel[a.category] || a.category }}</span>
              <h4>{{ a.name }}</h4>
              <p>{{ a.ethnicGroupName }} · {{ a.description }}</p>
            </div>
          </router-link>
        </div>
      </div>
    </template>
  </div>
</template>
