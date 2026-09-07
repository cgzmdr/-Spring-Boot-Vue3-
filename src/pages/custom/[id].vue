<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import CoverImage from '@/components/CoverImage.vue'
import Badge from '@/components/Badge.vue'
import { customApi, ethnicApi } from '@/api/modules'
import type { EthnicCustomDetail, EthnicDetail } from '@/api/types'

const route = useRoute()

const detail = ref<EthnicCustomDetail | null>(null)
const ethnic = ref<EthnicDetail | null>(null)
const loading = ref(true)
const error = ref(false)

const contentBlocks = computed(() => {
  const text = detail.value?.content || ''
  return text
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean)
    .map((p) => (/^【.+】/.test(p) ? { kind: 'heading' as const, text: p } : { kind: 'para' as const, text: p }))
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
        <div class="badge-wrap" style="margin-bottom: 14px">
          <Badge :text="detail.category" />
          <Badge :text="detail.ethnicGroupName" ink />
        </div>
        <h2>{{ detail.title }}</h2>
        <p class="dek">{{ detail.ethnicGroupName }} · {{ detail.category }}</p>
      </div>

      <div v-if="detail.image" class="hero-img-wrap">
        <CoverImage :src="detail.image" :name="detail.title" :prompt="`${detail.title} ethnic custom, traditional culture photography`" size="landscape_16_9" />
      </div>

      <div class="duo">
        <div class="panel">
          <h3>风俗详情</h3>
          <div class="article">
            <template v-if="contentBlocks.length">
              <template v-for="(b, i) in contentBlocks" :key="i">
                <h4 v-if="b.kind === 'heading'" class="sec-title">{{ b.text }}</h4>
                <p v-else :class="{ dropcap: i === 0 }">{{ b.text }}</p>
              </template>
            </template>
            <p v-else class="dropcap">{{ detail.content }}</p>
          </div>
        </div>
        <div class="panel">
          <h3>基本信息</h3>
          <div class="info-card">
            <dl>
              <dt>所属民族</dt><dd>{{ detail.ethnicGroupName }}</dd>
              <dt>风俗分类</dt><dd>{{ detail.category }}</dd>
              <dt>风俗名称</dt><dd>{{ detail.title }}</dd>
            </dl>
          </div>
          <div style="margin-top: 20px">
            <router-link :to="`/ethnic/${detail.ethnicGroupId}#customs`" class="link-btn">返回该民族风俗列表</router-link>
          </div>
        </div>
      </div>

      <div v-if="relatedCustoms.length" class="section" style="padding-top: 24px">
        <div class="section-rule"><span class="no">01</span><h3>同族风俗</h3><span class="line"></span></div>
        <div class="grid grid-3">
          <router-link v-for="c in relatedCustoms" :key="c.id" class="feature" :to="`/custom/${c.id}`">
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
