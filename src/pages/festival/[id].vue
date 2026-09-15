<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CoverImage from '@/components/CoverImage.vue'
import InteractionBar from '@/components/InteractionBar.vue'
import Badge from '@/components/Badge.vue'
import Reveal from '@/components/Reveal.vue'
import RichArticle from '@/components/RichArticle.vue'
import LinkedDiscussion from '@/components/LinkedDiscussion.vue'
import SourceReferences from '@/components/SourceReferences.vue'
import ImageCredits from '@/components/ImageCredits.vue'
import MachineTranslationNotice from '@/components/MachineTranslationNotice.vue'
import { festivalApi } from '@/api/modules'
import type { FestivalDetail, FestivalListItem } from '@/api/types'
import { festivalTypeLabel, parseArray, festivalImagePrompt } from '@/utils/format'
import { parseArticle } from '@/utils/article'
import type { ArticleBlock, ArticleFigure } from '@/utils/article'
import { useLangStore } from '@/stores/lang'
import { useViewTracking } from '@/composables/useViewTracking'

const route = useRoute()
const lang = useLangStore()

// 浏览行为上报在 detail 声明之后调用（见下方）

/**
 * 当前是否确实在展示英文正文（供机器翻译提示判断）。
 * 需要同时满足：处于英文语境 + 该条内容有英文正文；
 * 否则会出现「用中文阅读却提示本条为机器翻译」的误报。
 */
const showingEnglish = computed(
  () => lang.isEn && !!(detail.value?.descriptionEn || '').trim(),
)

const detail = ref<FestivalDetail | null>(null)
const related = ref<FestivalListItem[]>([])
const loading = ref(true)
const error = ref(false)

// 浏览行为上报（方向 D：个性化推荐的隐式信号；未登录时后端静默忽略）
// 放在 detail 声明之后，避免引用未初始化的变量
useViewTracking('festival', () => detail.value?.id)

/** 正文段落：起源传说 + 详细介绍（【】小节标题自动成节）
 *  英文语境下优先用 descriptionEn（方向 C-3，机器翻译），无译文时回退中文。 */
const blocks = computed<ArticleBlock[]>(() => {  const d = detail.value
  if (!d) return []
  const list: ArticleBlock[] = []
  if (d.origin) list.push({ kind: 'para', text: d.origin })
  list.push(...parseArticle(lang.pick(d.description || '', d.descriptionEn)))
  return list
})

/** 正文配图：节日图集 + 封面（图文并茂） */
const figures = computed<ArticleFigure[]>(() => {
  const d = detail.value
  if (!d) return []
  const list: ArticleFigure[] = parseArray<string>(d.images).map((src, i) => ({
    src,
    caption: `${d.name} · 影像 ${i + 1}`,
  }))
  if (d.coverImage) list.push({ src: d.coverImage, caption: `${d.name} · ${d.ethnicGroupName}` })
  return list
})

async function load() {
  loading.value = true
  error.value = false
  try {
    const id = route.params.id as string
    const d = await festivalApi.detail(id)
    detail.value = { ...d, customs: parseArray(d.customs) }
    // 相关节日：同民族其余节日
    const all = await festivalApi.list({ page: 0, size: 20 })
    related.value = all.data.filter((f) => f.id !== id && f.ethnicGroupName === d.ethnicGroupName).slice(0, 3)
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="container">
    <div class="crumb">
      <router-link to="/">首页</router-link> › <router-link to="/festival">节日</router-link> ›
      <span>{{ detail?.name || '加载中' }}</span>
    </div>
  </div>

  <div class="container">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="error" description="节日信息加载失败">
      <el-button type="primary" @click="load">重试</el-button>
    </el-empty>

    <template v-else-if="detail">
      <div class="page-head">
        <div class="badge-wrap" style="margin-bottom: 14px" v-motion-fade-in>
          <Badge :text="festivalTypeLabel[detail.type] || detail.type" />
          <Badge :text="detail.ethnicGroupName" ink />
        </div>
        <h2 v-motion-fade-up>{{ lang.pick(detail.name, detail.nameEn) }}</h2>
        <p class="dek" v-motion-fade-up>{{ detail.lunarDate || detail.solarDate || '日期待考' }} · {{ detail.ethnicGroupName }}</p>
      </div>

      <div class="hero-img-wrap" v-motion-pop-in>
        <CoverImage :src="detail.coverImage" :name="detail.name" :prompt="festivalImagePrompt(detail.name)" size="landscape_16_9" />
      </div>

      <InteractionBar type="festival" :id="detail.id" />

      <div class="duo">
        <Reveal class="panel" :y="18">
          <div class="panel-head">
            <h3>起源传说</h3>
            <span class="panel-hint">{{ blocks.length }} 段落 · 图文并茂</span>
          </div>
          <MachineTranslationNotice
            :source="detail.descriptionEnSource"
            :active="showingEnglish"
          />
          <RichArticle v-if="blocks.length" :blocks="blocks" :images="figures" :max-figures="3" />
          <p v-else class="article">暂无记载</p>        </Reveal>
        <Reveal class="panel" :x="20" :delay="80">
          <h3>习俗活动</h3>
          <div class="info-card" style="border: none; padding: 0">
            <dl>
              <dt>所属民族</dt><dd>{{ detail.ethnicGroupName }}</dd>
              <dt>节日类型</dt><dd>{{ festivalTypeLabel[detail.type] || detail.type }}</dd>
              <dt>公历日期</dt><dd>{{ detail.solarDate || '—' }}</dd>
              <dt>农历日期</dt><dd>{{ detail.lunarDate || '—' }}</dd>
            </dl>
          </div>
          <div style="margin-top: 20px; display: flex; flex-wrap: wrap; gap: 8px">
            <Badge v-for="(c, i) in detail.customs" :key="i" :text="c" ink />
          </div>
        </Reveal>
      </div>

      <template v-if="related.length">
        <div class="section" style="padding-top: 24px">
          <Reveal :y="14">
            <div class="section-rule"><span class="no">01</span><h3>相关节日</h3><span class="line"></span></div>
          </Reveal>
          <div class="grid grid-3">
            <router-link v-for="f in related" :key="f.id" class="feature" :to="`/festival/${f.id}`" v-motion-fade-up>
              <div class="img">
                <CoverImage :src="f.coverImage" :name="f.name" :prompt="festivalImagePrompt(f.name)" size="landscape_4_3" />
              </div>
              <div class="t">
                <span class="no">{{ festivalTypeLabel[f.type] || f.type }}</span>
                <h4>{{ f.name }}</h4>
                <p>{{ f.lunarDate || f.solarDate || '' }}</p>
              </div>
            </router-link>
          </div>
        </div>
      </template>

      <!-- 参考资料（数据出处，方向 C-1 可溯源） -->
      <SourceReferences
        target-type="festival"
        :target-id="detail.id"
      />

      <!-- 图片来源与许可（方向 C-4 署名） -->
      <ImageCredits
        target-type="festival"
        :target-id="detail.id"
      />

      <!-- 相关讨论（社区联动） -->
      <LinkedDiscussion
        linked-type="festival"
        :linked-id="detail.id"
        :label="detail.name"
      />
    </template>
  </div>
</template>

<style scoped>
.hero-img-wrap {
  margin: 24px 0 8px;
  aspect-ratio: 16/6;
  overflow: hidden;
  border: 1px solid var(--line);
}
.hero-img-wrap :deep(img),
.hero-img-wrap :deep(.cover-fallback) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.panel-hint {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--muted);
}
</style>
