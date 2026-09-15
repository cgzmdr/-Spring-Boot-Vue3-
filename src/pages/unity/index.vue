<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SectionRule from '@/components/SectionRule.vue'
import CoverImage from '@/components/CoverImage.vue'
import Reveal from '@/components/Reveal.vue'
import { ethnicApi } from '@/api/modules'
import type { EthnicBrief } from '@/api/types'
import { stagger, fadeUp } from '@/utils/motion'

const router = useRouter()
const wall = ref<EthnicBrief[]>([])

/** 全家福民族标签错峰浮现（超过 20 个后不再叠加延迟） */
function wallVariants(index: number) {
  return fadeUp({ delay: Math.min(index * 18, 360), distance: 10, duration: 460 })
}

const STORIES = [
  {
    title: '文成公主与松赞干布',
    desc: '汉藏和亲，架起唐蕃友好往来的桥梁，雪域高原自此有了茶马互市。',
    period: '唐 · 公元 641 年',
    prompt: 'Tang dynasty princess wedding procession to Tibet, ancient silk road, historical illustration',
  },
  {
    title: '土尔扈特万里东归',
    desc: '蒙古土尔扈特部冲破沙俄阻挠，行程万里回到祖国怀抱。',
    period: '清 · 公元 1771 年',
    prompt: 'Mongolian Torghut tribe migrating east across the steppe, historical illustration',
  },
  {
    title: '乌兰牧骑精神',
    desc: '草原红色文艺轻骑兵，把党的声音送到最后一公里。',
    period: '1957 年至今',
    prompt: 'Ulan Muqir grassland performance troupe, red flag, prairie, photography',
  },
]

const TIMELINE = [
  { date: '1949', title: '共同纲领：民族平等', desc: '《共同纲领》确立民族平等、团结、互助的基本政策。' },
  { date: '1954', title: '宪法确立民族区域自治', desc: '民族区域自治制度写入宪法，成为国家基本政治制度。' },
  { date: '1984', title: '民族区域自治法施行', desc: '保障少数民族当家作主权利，促进共同繁荣发展。' },
  { date: '2014', title: '中华民族共同体意识', desc: '中央民族工作会议强调铸牢中华民族共同体意识。' },
]

const POLICIES = [
  {
    title: '乡村振兴',
    desc: '民族地区巩固脱贫攻坚成果，接续推进乡村振兴，让各族人民共享发展成果。',
    prompt: 'rural revitalization in ethnic minority area, terraced fields and new village houses',
  },
  {
    title: '语言文字保护',
    desc: '推广国家通用语言文字，同时支持少数民族语言文字的保护与发展。',
    prompt: 'ethnic minority language manuscripts and dictionaries on a desk, warm light',
  },
]

onMounted(async () => {
  try {
    wall.value = await ethnicApi.all()
  } catch {
    wall.value = []
  }
})

function goEthnic(id: string) {
  router.push(`/ethnic/${id}`)
}
</script>

<template>
  <div class="container">
    <!-- 主视觉 -->
    <div class="hero-duo" style="border-top: 1px solid var(--line); margin-top: 24px">
      <div class="hero-text">
        <div class="kicker">UNITY · 民族团结</div>
        <h2>多元一体<br />中华民族共同体</h2>
        <p class="dek">
          五十六个民族，如石榴籽一样紧紧抱在一起。各民族共同开拓了辽阔的疆域，共同书写了悠久的历史，共同创造了灿烂的文化。
        </p>
        <div class="cta">
          <router-link class="btn btn-solid" to="/ethnic">认识各民族</router-link>
        </div>
      </div>
      <figure class="hero-media">
        <CoverImage
          mode="ai"
          name="民族团结"
          :theme="'#B6402E'"
          :prompt="'Chinese ethnic minority people of different groups standing together, unity and harmony, colorful traditional costumes, photography'"
          size="landscape_16_9"
        />
        <figcaption>五十六个民族 · 一家亲</figcaption>
      </figure>
    </div>

    <!-- 01 民族团结故事（图文并茂） -->
    <section class="section">
      <SectionRule no="01" title="民族团结故事" />
      <div class="grid grid-3 seam">
        <Reveal
          v-for="(s, i) in STORIES"
          :key="i"
          class="story-panel"
          tag="article"
          :delay="stagger(i, 90, 300)"
          :y="20"
        >
          <div class="story-img">
            <CoverImage mode="ai" :name="s.title" :prompt="s.prompt" :theme="'#B6402E'" size="landscape_4_3" />
          </div>
          <div class="story-body">
            <div class="idx">NO. 0{{ i + 1 }}</div>
            <h4>{{ s.title }}</h4>
            <p>{{ s.desc }}</p>
            <div class="period">{{ s.period }}</div>
          </div>
        </Reveal>
      </div>
    </section>

    <!-- 02 历史时间线 -->
    <section class="section" style="background: var(--paper-2)">
      <SectionRule no="02" title="历史时间线" />
      <div class="timeline">
        <div v-for="t in TIMELINE" :key="t.date" class="tl-item" v-motion-fade-up>
          <div class="date">{{ t.date }}</div>
          <h5>{{ t.title }}</h5>
          <p>{{ t.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 03 政策解读（图文并茂） -->
    <section class="section">
      <SectionRule no="03" title="政策解读" />
      <div class="grid grid-2 seam">
        <Reveal
          v-for="(p, i) in POLICIES"
          :key="i"
          class="policy-panel"
          tag="article"
          :delay="stagger(i, 90, 300)"
          :y="20"
        >
          <div class="policy-img">
            <CoverImage mode="ai" :name="p.title" :prompt="p.prompt" :theme="'#B6402E'" size="landscape_16_9" />
          </div>
          <div class="policy-body">
            <h4>{{ p.title }}</h4>
            <p>{{ p.desc }}</p>
          </div>
        </Reveal>
      </div>
    </section>

    <!-- 04 56 民族全家福 -->
    <section class="section">
      <SectionRule no="04" title="56 民族全家福" />
      <div class="wall">
        <button
          v-for="(e, i) in wall"
          :key="e.id"
          class="w-chip"
          :style="{ borderColor: e.themeColor }"
          v-motion="wallVariants(i)"
          @click="goEthnic(e.id)"
        >
          {{ e.name }}
        </button>
      </div>
      <p style="font-size: 12px; letter-spacing: 1px; color: var(--muted); margin-top: 16px; text-transform: uppercase">
        {{ wall.length }}/56 · 更多民族持续收录中
      </p>
    </section>
  </div>
</template>

<style scoped>
.story-panel,
.policy-panel {
  background: var(--paper);
  overflow: hidden;
}
.story-body,
.policy-body {
  padding: 24px 30px 30px;
}
.story-img,
.policy-img {
  overflow: hidden;
  background: var(--paper-2);
  aspect-ratio: 16 / 10;
}
.story-img :deep(img),
.story-img :deep(.cover-fallback),
.policy-img :deep(img),
.policy-img :deep(.cover-fallback) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.story-panel .idx {
  font-family: var(--serif);
  font-size: 12px;
  color: var(--accent);
  font-weight: 700;
}
.story-panel h4,
.policy-panel h4 {
  font-family: var(--serif);
  font-size: 21px;
  letter-spacing: 0.04em;
  margin: 10px 0 10px;
}
.story-panel p,
.policy-panel p {
  font-size: 14.5px;
  color: var(--muted);
  line-height: 1.85;
}
.story-panel .period {
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--accent);
  text-transform: uppercase;
  margin-top: 14px;
}
</style>
