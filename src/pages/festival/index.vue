<script setup lang="ts">
import { ref, onMounted } from 'vue'
import PageHead from '@/components/PageHead.vue'
import AppPagination from '@/components/AppPagination.vue'
import { festivalApi } from '@/api/modules'
import type { FestivalListItem } from '@/api/types'
import { festivalTypeLabel } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

const lang = useLangStore()

const TYPES = [
  { value: 'traditional', label: '传统节日' },
  { value: 'religious', label: '宗教节日' },
  { value: 'agricultural', label: '农事节日' },
]

const list = ref<FestivalListItem[]>([])
const total = ref(0)
const page = ref(0)
const size = 12
const typeFilter = ref('')
const loading = ref(false)
const errored = ref(false)

async function load() {
  loading.value = true
  errored.value = false
  try {
    const res = await festivalApi.list({
      page: page.value,
      size,
      type: typeFilter.value || undefined,
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

function pickType(t: string) {
  typeFilter.value = typeFilter.value === t ? '' : t
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
    <div class="crumb"><router-link to="/">首页</router-link> › <span>节日</span></div>

    <PageHead
      :kicker="lang.pick('FESTIVALS · 节日频道', 'FESTIVALS')"
      :title="lang.pick('节日庆典', 'Festivals & Celebrations')"
      :dek="lang.pick('从藏历新年到泼水节，各民族以自己的节令标记时间，也在欢庆中延续传统。', 'From the Tibetan New Year to the Water Splashing Festival, every group marks time in its own way.')"
    />

    <div class="filter">
      <div class="row">
        <span class="label">类型</span>
        <button v-for="t in TYPES" :key="t.value" class="chip" :class="{ active: typeFilter === t.value }" @click="pickType(t.value)">
          {{ t.label }}
        </button>
        <button v-if="typeFilter" class="chip" @click="pickType(typeFilter)">✕ 清除</button>
      </div>
    </div>

    <div class="result-count">{{ lang.t('result_count', { n: total }) }}</div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="errored" class="ghost-block" style="text-align: center">
      <el-empty description="节日数据加载失败">
        <el-button type="primary" @click="load">重新加载</el-button>
      </el-empty>
    </div>

    <template v-else-if="list.length">
      <div class="timeline">
        <div v-for="(f, i) in list" :key="f.id" class="tl-item">
          <router-link :to="`/festival/${f.id}`">
            <div class="date">{{ f.lunarDate || f.solarDate || `0${i + 1} · ${festivalTypeLabel[f.type] || f.type}` }}</div>
            <h5>{{ f.name }}</h5>
            <p>{{ f.ethnicGroupName }} · {{ f.origin }}</p>
            <p>{{ (f.customs || []).slice(0, 4).join(' · ') }}</p>
          </router-link>
        </div>
      </div>
      <AppPagination :current="page" :total="total" :size="size" @change="onPage" />
    </template>

    <el-empty v-else description="暂无节日数据" style="padding: 48px 0" />
  </div>
</template>
