<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CoverImage from '@/components/CoverImage.vue'
import Badge from '@/components/Badge.vue'
import Reveal from '@/components/Reveal.vue'
import RichArticle from '@/components/RichArticle.vue'
import { customApi, ethnicApi } from '@/api/modules'
import type { EthnicCustomDetail, EthnicDetail } from '@/api/types'
import { parseArticle } from '@/utils/article'
import type { ArticleFigure } from '@/utils/article'

const route = useRoute()

const detail = ref<EthnicCustomDetail | null>(null)
const ethnic = ref<EthnicDetail | null>(null)
const loading = ref(true)
const error = ref(false)

/** 正文段落（【】小节标题自动成节） */
const blocks = computed(() => parseArticle(detail.value?.content))

/** 正文配图：所属民族的节日 / 美食 / 风俗图，实现图文并茂 */
const figures = computed<ArticleFigure[]>(() => {
  const d = detail.value
  const e = ethnic.value
  if (!d || !e) return []
  const list: ArticleFigure[] = []
  const push = (src: string | null | undefined, caption: string) => {
    if (src) list.push({ src, caption, theme: e.themeColor })
  }
  e.customs
    .filter((c) => c.id !== d.id)
    .forEach((c) => push(c.image, `${c.title} · ${c.category}`))
  e.foods.forEach((f) => push(f.image, `${f.name} · 特色美食`))
  e.festivals.forEach((f) => push(f.coverImage, `${f.name} · 节日庆典`))
  push(d.image, `${d.title} · ${e.name}`)
  return list
})

const relatedCustoms = computed(() => {
  const d = detail.value
  if (!d || !ethnic.value) return []
  return (ethnic.value.customs || []).filter((c) => c.id !== d.id).slice(0, 6)
})

async function load() {
  loading.value = true
  error.value = false
  try {
    const id = route.params.id as string
    const d = await customApi.detail(id)
    detail.value = d
    if (d.ethnicGroupId) {
      ethnic.value = await ethnicApi.detail(d.ethnicGroupId)
    }
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
      <router-link to="/">首页</router-link> ›
      <router-link to="/ethnic">民族</router-link> ›
      <router-link v-if="detail" :to="`/ethnic/${detail.ethnicGroupId}`">{{ detail.ethnicGroupName }}</router-link> ›
      <span>{{ detail?.title || '加载中' }}</span>
    </div>
  </div>

  <div class="container">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="error" description="风俗习惯加载失败">
      <el-button type="primary" @click="load">重试</el-button>
    </el-empty>

    <template v-else-if="detail">
      <div class="page-head">
        <div class="badge-wrap" style="margin-bottom: 14px" v-motion-fade-in>
          <Badge :text="detail.category" />
          <Badge :text="detail.ethnicGroupName" ink />
        </div>
        <h2 v-motion-fade-up>{{ detail.title }}</h2>
        <p class="dek" v-motion-fade-up>{{ detail.ethnicGroupName }} · {{ detail.category }}</p>
      </div>

      <div v-if="detail.image" class="hero-img-wrap" v-motion-pop-in>
        <CoverImage :src="detail.image" :name="detail.title" :theme="ethnic?.themeColor" :prompt="`${detail.title} ethnic custom, traditional culture photography`" size="landscape_16_9" />
      </div>

      <div class="duo">
        <Reveal class="panel" :y="18">
          <div class="panel-head">
            <h3>风俗详情</h3>
            <span class="panel-hint">{{ blocks.length }} 段落 · 图文并茂</span>
          </div>
          <RichArticle v-if="blocks.length" :blocks="blocks" :images="figures" :theme="ethnic?.themeColor" :max-figures="3" />
          <p v-else class="article">{{ detail.content }}</p>
        </Reveal>
        <Reveal class="panel" :x="20" :delay="80">
          <h3>基本信息</h3>
          <div class="info-card">
            <dl>
              <dt>所属民族</dt><dd>{{ detail.ethnicGroupName }}</dd>
              <dt>风俗分类</dt><dd>{{ detail.category }}</dd>
              <dt>风俗名称</dt><dd>{{ detail.title }}</dd>
              <dt>聚居地区</dt><dd>{{ ethnic?.region?.join('、') || '—' }}</dd>
            </dl>
          </div>
          <div style="margin-top: 20px">
            <router-link :to="`/ethnic/${detail.ethnicGroupId}#customs`" class="link-btn">返回该民族风俗列表</router-link>
          </div>
        </Reveal>
      </div>

      <div v-if="relatedCustoms.length" class="section" style="padding-top: 24px">
        <Reveal :y="14">
          <div class="section-rule"><span class="no">01</span><h3>同族风俗</h3><span class="line"></span></div>
        </Reveal>
        <div class="grid grid-3">
          <router-link v-for="c in relatedCustoms" :key="c.id" class="feature" :to="`/custom/${c.id}`" v-motion-fade-up>
            <div v-if="c.image" class="img">
              <CoverImage :src="c.image" :name="c.title" :theme="ethnic?.themeColor" size="landscape_4_3" />
            </div>
            <div class="t">
              <span class="no">{{ c.category }}</span>
              <h4>{{ c.title }}</h4>
              <p>{{ c.content.slice(0, 80) }}…</p>
            </div>
          </router-link>
        </div>
      </div>
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
.link-btn {
  display: inline-block;
  border: 1px solid var(--line);
  padding: 10px 16px;
  color: var(--ink);
  text-decoration: none;
  font-size: 14px;
}
.link-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
</style>
