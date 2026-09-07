<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CoverImage from '@/components/CoverImage.vue'
import InteractionBar from '@/components/InteractionBar.vue'
import Badge from '@/components/Badge.vue'
import { festivalApi } from '@/api/modules'
import type { FestivalDetail, FestivalListItem } from '@/api/types'
import { festivalTypeLabel, parseArray, festivalImagePrompt } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

const route = useRoute()
const lang = useLangStore()

const detail = ref<FestivalDetail | null>(null)
const related = ref<FestivalListItem[]>([])
const loading = ref(true)
const error = ref(false)

const descriptionBlocks = computed(() => {
  const text = detail.value?.description || ''
  return text
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
    .map((p) => (/^【.+】/.test(p) ? { kind: 'heading' as const, text: p } : { kind: 'para' as const, text: p }))
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
        <div class="badge-wrap" style="margin-bottom: 14px">
          <Badge :text="festivalTypeLabel[detail.type] || detail.type" />
          <Badge :text="detail.ethnicGroupName" ink />
        </div>
        <h2>{{ lang.pick(detail.name, detail.nameEn) }}</h2>
        <p class="dek">{{ detail.lunarDate || detail.solarDate || '日期待考' }} · {{ detail.ethnicGroupName }}</p>
      </div>

      <div class="hero-img-wrap">
        <CoverImage :src="detail.coverImage" :name="detail.name" :prompt="festivalImagePrompt(detail.name)" size="landscape_16_9" />
      </div>

      <InteractionBar type="festival" :id="detail.id" />

      <div class="duo">
        <div class="panel">
          <h3>起源传说</h3>
          <div class="article">
            <p class="dropcap">{{ detail.origin || '暂无记载' }}</p>
            <template v-if="descriptionBlocks.length">
              <template v-for="(b, i) in descriptionBlocks" :key="i">
                <h4 v-if="b.kind === 'heading'" class="sec-title">{{ b.text }}</h4>
                <p v-else>{{ b.text }}</p>
              </template>
            </template>
          </div>
        </div>
        <div class="panel">
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
        </div>
      </div>

      <template v-if="related.length">
        <div class="section" style="padding-top: 24px">
          <div class="section-rule"><span class="no">01</span><h3>相关节日</h3><span class="line"></span></div>
          <div class="grid grid-3">
            <router-link v-for="f in related" :key="f.id" class="feature" :to="`/festival/${f.id}`">
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
</style>
