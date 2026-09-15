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
import { artApi } from '@/api/modules'
import type { ArtDetail, ArtListItem } from '@/api/types'
import { artCategoryLabel, heritageLabel, parseArray, artImagePrompt } from '@/utils/format'
import { parseArticle } from '@/utils/article'
import type { ArticleBlock, ArticleFigure } from '@/utils/article'
import { useLangStore } from '@/stores/lang'
import { useViewTracking } from '@/composables/useViewTracking'

const route = useRoute()
const lang = useLangStore()

// 浏览行为上报在 detail 声明之后调用（见下方）

/**
 * 当前是否确实在展示英文正文（供机器翻译提示判断）：
 * 处于英文语境且该条内容有英文正文，避免中文阅读时误报。
 */
const showingEnglish = computed(
  () => lang.isEn && !!(detail.value?.descriptionEn || '').trim(),
)

const detail = ref<ArtDetail | null>(null)
const related = ref<ArtListItem[]>([])
const loading = ref(true)
const error = ref(false)

// 浏览行为上报（方向 D：个性化推荐的隐式信号；未登录时后端静默忽略）
// 放在 detail 声明之后，避免引用未初始化的变量
useViewTracking('art', () => detail.value?.id)

/** 正文段落：艺术介绍 + 发展沿革（独立小节） */
const blocks = computed<ArticleBlock[]>(() => {
  const d = detail.value
  if (!d) return []
  // 英文语境下优先用 descriptionEn（方向 C-3，机器翻译），无译文时回退中文
  const list = parseArticle(lang.pick(d.description || '', d.descriptionEn))
  if (d.origin) list.push({ kind: 'heading', text: '发展沿革' }, ...parseArticle(d.origin))
  return list
})

/** 正文配图：同类艺术封面 + 自身封面，保证图文并茂 */
const figures = computed<ArticleFigure[]>(() =>
  related.value
    .filter((a) => a.coverImage)
    .map((a) => ({ src: a.coverImage, caption: `${a.name} · ${a.ethnicGroupName}` })),
)

async function load() {
  loading.value = true
  error.value = false
  try {
    const id = route.params.id as string
    const d = await artApi.detail(id)
    detail.value = { ...d, inheritors: parseArray(d.inheritors) }
    const all = await artApi.list({ page: 0, size: 20 })
    related.value = all.data.filter((a) => a.id !== id && a.category === d.category).slice(0, 3)
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
      <router-link to="/">首页</router-link> › <router-link to="/art">艺术</router-link> ›
      <span>{{ detail?.name || '加载中' }}</span>
    </div>
  </div>

  <div class="container">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="error" description="艺术信息加载失败">
      <el-button type="primary" @click="load">重试</el-button>
    </el-empty>

    <template v-else-if="detail">
      <div class="page-head">
        <div class="badge-wrap" style="margin-bottom: 14px" v-motion-fade-in>
          <Badge :text="artCategoryLabel[detail.category] || detail.category" />
          <Badge v-if="detail.intangibleHeritage" :text="heritageLabel[detail.intangibleHeritage] || detail.intangibleHeritage" ink />
        </div>
        <h2 v-motion-fade-up>{{ lang.pick(detail.name, detail.nameEn) }}</h2>
        <p class="dek" v-motion-fade-up>{{ detail.ethnicGroupName }} · {{ artCategoryLabel[detail.category] || detail.category }}</p>
      </div>

      <div class="hero-img-wrap" v-motion-pop-in>
        <CoverImage :src="detail.coverImage" :name="detail.name" :prompt="artImagePrompt(detail.name)" size="landscape_16_9" />
      </div>

      <InteractionBar type="art" :id="detail.id" />

      <div class="duo">
        <Reveal class="panel" :y="18">
          <div class="panel-head">
            <h3>艺术介绍</h3>
            <span class="panel-hint">{{ blocks.length }} 段落 · 图文并茂</span>
          </div>
          <MachineTranslationNotice
            :source="detail.descriptionEnSource"
            :active="showingEnglish"
          />
          <RichArticle v-if="blocks.length" :blocks="blocks" :images="figures" :max-figures="3" />
          <p v-else class="article">暂无介绍</p>
        </Reveal>
        <Reveal class="panel" :x="20" :delay="80">
          <h3>基本信息</h3>
          <div class="info-card" style="border: none; padding: 0">
            <dl>
              <dt>所属民族</dt><dd>{{ detail.ethnicGroupName }}</dd>
              <dt>艺术类别</dt><dd>{{ artCategoryLabel[detail.category] || detail.category }}</dd>
              <dt>非遗级别</dt><dd>{{ heritageLabel[detail.intangibleHeritage] || '—' }}</dd>
              <dt>传承人</dt>
              <dd>{{ (detail.inheritors as string[]).join('、') || '—' }}</dd>
            </dl>
          </div>
        </Reveal>
      </div>

      <template v-if="related.length">
        <div class="section" style="padding-top: 24px">
          <Reveal :y="14">
            <div class="section-rule"><span class="no">01</span><h3>同类艺术</h3><span class="line"></span></div>
          </Reveal>
          <div class="grid grid-3">
            <router-link v-for="a in related" :key="a.id" class="feature" :to="`/art/${a.id}`" v-motion-fade-up>
              <div class="img">
                <CoverImage :src="a.coverImage" :name="a.name" :prompt="artImagePrompt(a.name)" size="landscape_4_3" />
              </div>
              <div class="t">
                <span class="no">{{ artCategoryLabel[a.category] || a.category }}</span>
                <h4>{{ a.name }}</h4>
                <p>{{ a.ethnicGroupName }} · {{ heritageLabel[a.intangibleHeritage] || '' }}</p>
              </div>
            </router-link>
          </div>
        </div>
      </template>

      <!-- 参考资料（数据出处，方向 C-1 可溯源） -->
      <SourceReferences
        target-type="art"
        :target-id="detail.id"
      />

      <!-- 图片来源与许可（方向 C-4 署名） -->
      <ImageCredits
        target-type="art"
        :target-id="detail.id"
      />

      <!-- 相关讨论（社区联动） -->
      <LinkedDiscussion
        linked-type="art"
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
