<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLangStore } from '@/stores/lang'
import { topicApi, ethnicApi, festivalApi, artApi, interactionApi } from '@/api/modules'
import type { TopicDetail, TopicEntry } from '@/api/types'
import CoverImage from '@/components/CoverImage.vue'
import PageHead from '@/components/PageHead.vue'
import Reveal from '@/components/Reveal.vue'
import RichArticle from '@/components/RichArticle.vue'
import { parseArticle } from '@/utils/article'
import type { ArticleFigure } from '@/utils/article'
import { stagger, fadeUp } from '@/utils/motion'

const route = useRoute()
const lang = useLangStore()

const loading = ref(false)
const error = ref('')
const detail = ref<TopicDetail | null>(null)
const viewCount = ref(0)
const entries = ref<{ entry: TopicEntry; title: string; cover: string | null; path: string; themeColor?: string }[]>([])

const TYPE_LABEL: Record<string, string> = { ethnic: '民族', festival: '节日', art: '艺术', topic: '专题' }

/** 专题简介：解析为结构化段落（支持图文并茂） */
const blocks = computed(() => parseArticle(detail.value?.description))

/** 正文配图：专题条目封面（让专题介绍不再只是纯文本） */
const figures = computed<ArticleFigure[]>(() =>
  entries.value
    .filter((e) => e.cover)
    .slice(0, 4)
    .map((e) => ({ src: e.cover, caption: `${e.title} · ${TYPE_LABEL[e.entry.entryType] || ''}` })),
)

/** 条目卡片错峰入场（@vueuse/motion） */
function fadeUpVariants(index: number) {
  return fadeUp({ delay: stagger(index, 50, 400), distance: 16 })
}

async function loadEntry(item: TopicEntry) {
  try {
    if (item.entryType === 'ethnic') {
      const d = await ethnicApi.detail(item.entryId)
      return { title: d.name, cover: d.coverImage, path: '/ethnic/' + item.entryId, themeColor: d.themeColor }
    }
    if (item.entryType === 'festival') {
      const d = await festivalApi.detail(item.entryId)
      return { title: d.name, cover: d.coverImage, path: '/festival/' + item.entryId }
    }
    if (item.entryType === 'art') {
      const d = await artApi.detail(item.entryId)
      return { title: d.name, cover: d.coverImage, path: '/art/' + item.entryId }
    }
    return { title: item.entryId, cover: null, path: '' }
  } catch {
    return { title: item.entryId.slice(0, 8), cover: null, path: '' }
  }
}

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await topicApi.detail(route.params.id as string)
    entries.value = await Promise.all(
      (detail.value.entries || []).map(async (e) => ({ entry: e, ...(await loadEntry(e)) })),
    )
    // 记录浏览量并刷新展示（失败不影响页面）
    try {
      viewCount.value = await interactionApi.view('topic', route.params.id as string)
    } catch {
      /* 忽略浏览量上报失败 */
    }
  } catch {
    error.value = '专题加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <template v-if="detail">
      <PageHead :kicker="'TOPIC'" :title="detail.title" :dek="detail.subtitle" />
      <div class="container topic-body">
        <div class="hero" v-motion-fade-in>
          <CoverImage :src="detail.coverImage" :name="detail.title" :theme="'#B6402E'" size="landscape_16_9" />
        </div>

        <!-- 专题介绍：图文并茂（段落间穿插条目封面） -->
        <RichArticle v-if="blocks.length" :blocks="blocks" :images="figures" :max-figures="3" />

        <div class="meta-row">
          <span class="view-count">浏览量 {{ viewCount }}</span>
        </div>

        <Reveal :y="14">
          <h3 class="sec-title">{{ lang.pick('专题内容', 'Contents') }} <span class="count">{{ entries.length }}</span></h3>
        </Reveal>
        <div class="grid grid-3">
          <router-link
            v-for="(e, i) in entries"
            :key="e.entry.id"
            :to="e.path"
            class="entry-card"
            v-motion="fadeUpVariants(i)"
          >
            <div class="img">
              <CoverImage :src="e.cover" :name="e.title" :theme="e.themeColor || '#B6402E'" size="landscape_4_3" />
            </div>
            <div class="t">
              <span class="tag">{{ TYPE_LABEL[e.entry.entryType] || e.entry.entryType }}</span>
              <h4>{{ e.title }}</h4>
            </div>
          </router-link>
        </div>
        <el-empty v-if="!entries.length" :description="lang.pick('暂无内容', 'No contents yet')" />
      </div>
    </template>

    <el-skeleton v-else-if="loading" :rows="6" animated class="container" />
    <el-empty v-else :description="error || lang.pick('专题不存在', 'Topic not found')" class="container" />
  </div>
</template>

<style scoped>
.topic-body { max-width: 1080px; padding: 24px 0 64px; }
.hero { border-radius: 14px; overflow: hidden; aspect-ratio: 16 / 9; }
.desc { font-size: 15px; line-height: 1.9; color: var(--muted); margin: 20px 0 8px; max-width: 720px; }
.meta-row { margin: 0 0 24px; }
.view-count { font-size: 13px; color: var(--muted); letter-spacing: 1px; }
.sec-title { font-family: var(--serif); margin-bottom: 14px; }
.sec-title .count { font-size: 13px; color: var(--muted); }
.entry-card { text-decoration: none; color: inherit; }
.entry-card .img { aspect-ratio: 4 / 3; border-radius: 10px; overflow: hidden; }
.entry-card .t { padding: 8px 2px 0; }
.entry-card .tag { font-size: 11px; color: var(--accent); letter-spacing: 0.08em; text-transform: uppercase; }
.entry-card h4 { margin: 2px 0 0; font-family: var(--serif); font-size: 16px; }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
@media (max-width: 760px) { .grid-3 { grid-template-columns: 1fr; } }
</style>
