<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { recommendApi } from '@/api/modules'
import type { Recommendation } from '@/api/types'
import CoverImage from '@/components/CoverImage.vue'
import { ethnicImagePrompt } from '@/utils/format'
import { useLangStore } from '@/stores/lang'

/**
 * 个性化推荐面板（方向 D）。
 *
 * 设计上的关键取舍：**如实展示推荐依据**。
 * 站内行为数据量小，很多时候推荐并非真正「个性化」，
 * 因此这里显式展示 basis（依据）+ dataNote（样本量说明），
 * 而不是笼统地写「为你推荐」。
 */
const props = withDefaults(
  defineProps<{
    /** 返回条数 */
    size?: number
    /** 排除的内容类型（详情页推荐时排除自身类型） */
    excludeType?: string
    /** 标题 */
    title?: string
  }>(),
  { size: 6, title: '' },
)

const lang = useLangStore()
const data = ref<Recommendation | null>(null)
const loading = ref(true)

/** 依据标签的配色：个性化 / 兴趣 / 热度 用不同底色，便于一眼看出推荐来源 */
const basisClass = (basis: string) =>
  basis === 'personalized' ? 'is-personal' : basis === 'interest' ? 'is-interest' : 'is-hot'

async function load() {
  loading.value = true
  try {
    data.value = await recommendApi.recommend({
      size: props.size,
      excludeType: props.excludeType,
    })
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
defineExpose({ reload: load })
</script>

<template>
  <section class="reco">
    <div class="reco-head">
      <h3>{{ title || lang.pick('为你推荐', 'Recommended') }}</h3>
      <!-- 依据标签：让用户知道推荐从何而来 -->
      <span v-if="data" class="reco-basis" :class="basisClass(data.basis)">
        {{ data.basisLabel }}
      </span>
    </div>

    <!-- 数据说明：样本不足时如实提示，而非假装个性化已成熟 -->
    <p v-if="data && data.dataNote" class="reco-note">
      {{ data.dataNote }}
    </p>

    <el-skeleton v-if="loading" :rows="3" animated />

    <el-empty
      v-else-if="!data || !data.list.length"
      :description="lang.pick('暂无推荐内容', 'No recommendations yet')"
      :image-size="72"
      style="padding: 24px 0"
    />

    <div v-else class="reco-grid">
      <router-link
        v-for="item in data.list"
        :key="item.docType + ':' + item.docId"
        :to="item.url"
        class="reco-item"
      >
        <div class="reco-cover">
          <CoverImage
            :src="item.coverImage"
            :name="item.title"
            :prompt="ethnicImagePrompt(item.title)"
            size="landscape_4_3"
          />
        </div>
        <div class="reco-body">
          <h4>{{ item.title }}</h4>
          <!-- 推荐理由：解释「为什么推这条」 -->
          <p v-if="item.reason" class="reco-reason">{{ item.reason }}</p>
          <p v-else-if="item.ethnicName" class="reco-reason">{{ item.ethnicName }}</p>
        </div>
      </router-link>
    </div>
  </section>
</template>

<style scoped>
.reco {
  padding: 28px 0;
}
.reco-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.reco-head h3 {
  margin: 0;
  font-size: 17px;
  font-family: var(--serif);
}
.reco-basis {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  border: 1px solid var(--line);
  color: var(--muted);
}
.reco-basis.is-personal {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 35%, transparent);
  background: color-mix(in srgb, var(--accent) 8%, transparent);
}
.reco-basis.is-interest {
  color: #2e6da4;
  border-color: color-mix(in srgb, #2e6da4 35%, transparent);
  background: color-mix(in srgb, #2e6da4 8%, transparent);
}
.reco-basis.is-hot {
  color: #c9843e;
  border-color: color-mix(in srgb, #c9843e 35%, transparent);
  background: color-mix(in srgb, #c9843e 8%, transparent);
}
.reco-note {
  margin: 0 0 18px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--muted);
}
.reco-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}
.reco-item {
  display: block;
  color: inherit;
  text-decoration: none;
}
.reco-cover {
  aspect-ratio: 4/3;
  overflow: hidden;
  border: 1px solid var(--line);
  margin-bottom: 10px;
}
.reco-cover :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}
.reco-item:hover .reco-cover :deep(img) {
  transform: scale(1.04);
}
.reco-body h4 {
  margin: 0 0 4px;
  font-size: 14.5px;
  font-family: var(--serif);
  color: var(--ink-1);
  transition: color 0.15s ease;
}
.reco-item:hover .reco-body h4 {
  color: var(--accent);
}
.reco-reason {
  margin: 0;
  font-size: 12px;
  color: var(--muted);
}
</style>
