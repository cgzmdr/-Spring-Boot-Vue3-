<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHead from '@/components/PageHead.vue'
import CoverImage from '@/components/CoverImage.vue'
import AppPagination from '@/components/AppPagination.vue'
import { artApi } from '@/api/modules'
import type { ArtListItem } from '@/api/types'
import { artCategoryLabel, heritageLabel, artImagePrompt } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

const lang = useLangStore()

const CATEGORIES = [
  { value: 'music', label: '音乐' },
  { value: 'dance', label: '舞蹈' },
  { value: 'drama', label: '戏剧' },
  { value: 'costume', label: '服饰' },
  { value: 'craft', label: '手工艺' },
  { value: 'architecture', label: '建筑' },
]
const HERITAGES = [
  { value: 'world', label: '世界级' },
  { value: 'national', label: '国家级' },
  { value: 'provincial', label: '省级' },
]

const list = ref<ArtListItem[]>([])
const total = ref(0)
const page = ref(0)
const size = 12
const category = ref('')
const heritage = ref('')
const loading = ref(false)
const errored = ref(false)

async function load() {
  loading.value = true
  errored.value = false
  try {
    const res = await artApi.list({
      page: page.value,
      size,
      category: category.value || undefined,
      intangibleHeritage: heritage.value || undefined,
    })
    list.value = res.data
    total.value = res.total
  } catch {
    errored.value = true
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function pickCategory(v: string) {
  category.value = category.value === v ? '' : v
  page.value = 0
  load()
}
function pickHeritage(v: string) {
  heritage.value = heritage.value === v ? '' : v
  page.value = 0
  load()
}
function onPage(p: number) {
  page.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div class="container">
    <div class="crumb"><router-link to="/">首页</router-link> › <span>艺术</span></div>

    <PageHead
      :kicker="lang.pick('ARTS · 艺术频道', 'ARTS')"
      :title="lang.pick('传统艺术', 'Traditional Arts')"
      :dek="lang.pick('音乐、舞蹈、戏剧、手工艺与建筑——每一个民族都以自己独有的方式表达美。', 'Music, dance, drama, crafts and architecture — every group expresses beauty in its own way.')"
    />

    <div class="filter">
      <div class="row">
        <span class="label">类别</span>
        <button v-for="c in CATEGORIES" :key="c.value" class="chip" :class="{ active: category === c.value }" @click="pickCategory(c.value)">
          {{ c.label }}
        </button>
        <button v-if="category" class="chip" @click="pickCategory(category)">✕ 清除</button>
      </div>
      <div class="row">
        <span class="label">非遗级别</span>
        <button v-for="h in HERITAGES" :key="h.value" class="chip" :class="{ active: heritage === h.value }" @click="pickHeritage(h.value)">
          {{ h.label }}
        </button>
        <button v-if="heritage" class="chip" @click="pickHeritage(heritage)">✕ 清除</button>
      </div>
    </div>

    <div class="result-count">{{ lang.t('result_count', { n: total }) }}</div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="errored" class="ghost-block" style="text-align: center">
      <el-empty description="艺术数据加载失败">
        <el-button type="primary" @click="load">重新加载</el-button>
      </el-empty>
    </div>

    <template v-else-if="list.length">
      <div class="grid grid-3">
        <router-link v-for="a in list" :key="a.id" class="feature" :to="`/art/${a.id}`">
          <div class="img">
            <CoverImage :src="a.coverImage" :name="a.name" :prompt="artImagePrompt(a.name)" size="landscape_4_3" />
          </div>
          <div class="t">
            <span class="no">{{ artCategoryLabel[a.category] || a.category }}</span>
            <h4>{{ a.name }}</h4>
            <p>{{ a.ethnicGroupName }} · {{ heritageLabel[a.intangibleHeritage] || '' }}</p>
            <p>{{ a.description }}</p>
          </div>
        </router-link>
      </div>
      <AppPagination :current="page" :total="total" :size="size" @change="onPage" />
    </template>

    <el-empty v-else description="暂无艺术数据" style="padding: 48px 0" />
  </div>
</template>
