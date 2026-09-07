<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CoverImage from '@/components/CoverImage.vue'
import InteractionBar from '@/components/InteractionBar.vue'
import Badge from '@/components/Badge.vue'
import { artApi } from '@/api/modules'
import type { ArtDetail, ArtListItem } from '@/api/types'
import { artCategoryLabel, heritageLabel, parseArray, artImagePrompt } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

const route = useRoute()
const lang = useLangStore()

const detail = ref<ArtDetail | null>(null)
const related = ref<ArtListItem[]>([])
const loading = ref(true)
const error = ref(false)

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
        <div class="badge-wrap" style="margin-bottom: 14px">
          <Badge :text="artCategoryLabel[detail.category] || detail.category" />
          <Badge v-if="detail.intangibleHeritage" :text="heritageLabel[detail.intangibleHeritage] || detail.intangibleHeritage" ink />
        </div>
        <h2>{{ lang.pick(detail.name, detail.nameEn) }}</h2>
        <p class="dek">{{ detail.ethnicGroupName }} · {{ artCategoryLabel[detail.category] || detail.category }}</p>
      </div>

      <div class="hero-img-wrap">
        <CoverImage :src="detail.coverImage" :name="detail.name" :prompt="artImagePrompt(detail.name)" size="landscape_16_9" />
      </div>

      <InteractionBar type="art" :id="detail.id" />

      <div class="duo">
        <div class="panel">
          <h3>艺术介绍</h3>
          <div class="article">
            <p class="dropcap">{{ detail.description || '暂无介绍' }}</p>
            <template v-if="detail.origin">
              <h4 class="sub-title">发展沿革</h4>
              <p>{{ detail.origin }}</p>
            </template>
          </div>
        </div>
        <div class="panel">
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
        </div>
      </div>

      <template v-if="related.length">
        <div class="section" style="padding-top: 24px">
          <div class="section-rule"><span class="no">01</span><h3>同类艺术</h3><span class="line"></span></div>
          <div class="grid grid-3">
            <router-link v-for="a in related" :key="a.id" class="feature" :to="`/art/${a.id}`">
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
