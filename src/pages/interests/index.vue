<script setup lang="ts">
import { ref } from 'vue'
import InterestPicker from '@/components/InterestPicker.vue'
import RecommendationPanel from '@/components/RecommendationPanel.vue'
import { useLangStore } from '@/stores/lang'

/**
 * 兴趣与推荐页（方向 D）。
 *
 * 上面选兴趣（显式信号），下面立即按新兴趣刷新推荐，
 * 让用户直观看到「选了什么 → 推荐变了什么」的因果。
 */
const lang = useLangStore()
const panel = ref<InstanceType<typeof RecommendationPanel> | null>(null)

/** 保存后刷新推荐，让用户立刻看到兴趣选择的效果 */
function onSaved() {
  panel.value?.reload()
}
</script>

<template>
  <div class="container">
    <div class="page-head">
      <h2>{{ lang.pick('兴趣与推荐', 'Interests & Recommendations') }}</h2>
      <p class="dek">
        {{ lang.pick(
          '选择感兴趣的题材，获得更贴合你偏好的内容推荐',
          'Pick topics you care about to get more relevant recommendations',
        ) }}
      </p>
    </div>

    <InterestPicker @saved="onSaved" />

    <div class="reco-wrap">
      <RecommendationPanel ref="panel" :size="8" />
    </div>
  </div>
</template>

<style scoped>
.page-head {
  padding: 40px 0 8px;
}
.page-head h2 {
  margin: 0 0 8px;
  font-size: 26px;
  font-family: var(--serif);
}
.dek {
  margin: 0;
  font-size: 13.5px;
  color: var(--muted);
}
.reco-wrap {
  margin-top: 24px;
  border-top: 1px solid var(--line);
}
</style>
