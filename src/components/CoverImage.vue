<script setup lang="ts">
import { ref, computed } from 'vue'
import { aiImage } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    /** 真实图片地址（后端下发，可能为空） */
    src?: string | null
    /** 内容名称（用于占位图与失败兜底） */
    name?: string
    /** 主题色（兜底块背景） */
    theme?: string
    /** AI 占位图提示词（mode=ai 时使用；现已本地化，仅作兜底） */
    prompt?: string
    /** 占位模式：theme=色块兜底（快）；ai=AI 生成图（慢，用于 Hero） */
    mode?: 'theme' | 'ai'
    /**
     * 图片适配方式：
     * · cover   —— 铺满容器（默认，列表卡片 / Hero 使用，会裁切）
     * · natural —— 按图片原始尺寸与比例渲染（不裁切、不拉伸），正文浮动配图使用
     */
    fit?: 'cover' | 'natural'
    /** 加载策略：正文配图用 eager，避免原尺寸模式下未加载时高度为 0 引起布局跳动 */
    loading?: 'lazy' | 'eager'
    /** AI 图尺寸 */
    size?: string
    alt?: string
  }>(),
  { src: null, name: '', theme: '#B6402E', prompt: '', mode: 'theme', fit: 'cover', loading: 'lazy', size: 'landscape_16_9', alt: '' },
)

const aiFailed = ref(false)

const API_BASE: string = (import.meta.env.VITE_API_BASE as string) || ''
const STATIC_BASE: string = (import.meta.env.VITE_STATIC_BASE as string) || API_BASE

/** 图片等静态资源直接指向后端静态服务，不依赖 Vite 代理 */
function resolveImageUrl(src: string): string {
  if (/^https?:\/\//i.test(src)) return src
  if (src.startsWith('/') && /^https?:\/\//i.test(STATIC_BASE)) {
    try {
      // 取后端 origin，避免 STATIC_BASE 带路径时拼错图片地址
      return new URL(STATIC_BASE).origin + src
    } catch {
      return STATIC_BASE.replace(/\/+$/, '') + src
    }
  }
  return src
}

/** 最终图片地址（真实图 > AI 占位图） */
const src = computed(() => {
  if (props.src) return resolveImageUrl(props.src)
  if (props.mode === 'ai' && props.prompt && !aiFailed.value) return resolveImageUrl(aiImage(props.prompt, props.size))
  return ''
})

/** 两阶段加载：先显示 tiny 模糊预览（{base}-blur.webp），清晰图加载完成后浮现替换 */
const blurSrc = computed(() => (src.value && src.value.endsWith('.webp') ? src.value.replace(/\.webp$/, '-blur.webp') : ''))
const fullLoaded = ref(false)
const fullFailed = ref(false)

/**
 * 关键：清晰层必须「始终渲染」以触发浏览器加载（@load），
 * 不能用 v-if 依赖 fullLoaded —— 否则清晰层不挂载、load 永不触发，形成死锁。
 * 这里用 opacity 过渡控制可见性：加载完成前清晰层透明（模糊层可见），完成后淡入。
 */
const showFull = computed(() => !!src.value)
const fullRevealed = computed(() => fullLoaded.value || !blurSrc.value || fullFailed.value)
const showBlur = computed(() => !!blurSrc.value && !fullFailed.value)
const blurHidden = computed(() => fullLoaded.value || fullFailed.value)
const showFallback = computed(() => !src.value || fullFailed.value)

function onFullLoad() {
  fullLoaded.value = true
}
function onFullError() {
  fullFailed.value = true
  aiFailed.value = true
}

/** 兜底色块背景（渐变 + 主题色） */
const fallbackStyle = computed(() => {
  const c = props.theme || '#B6402E'
  return {
    background: `linear-gradient(145deg, ${c} 0%, ${c}cc 55%, ${c}88 100%)`,
  }
})

/** 名称首字（色块中央大字） */
const initial = computed(() => (props.name ? props.name.trim().slice(0, 1) : ''))
</script>

<template>
  <div
    class="cover-wrap"
    :class="{ 'is-natural': fit === 'natural', 'is-fallback': showFallback }"
  >
    <!-- 清晰图：始终渲染以触发加载；加载完成前透明，完成后淡入 -->
    <img
      v-if="showFull"
      class="cover-img cover-full"
      :class="{ reveal: fullRevealed }"
      :src="src"
      :alt="alt || name"
      :loading="loading"
      decoding="async"
      @load="onFullLoad"
      @error="onFullError"
    />
    <!-- 模糊预览（tiny webp）：垫在清晰层之下，清晰图加载完成后淡出 -->
    <img
      v-if="showBlur"
      class="cover-img cover-blur"
      :class="{ hide: blurHidden }"
      :src="blurSrc"
      :alt="alt || name"
      aria-hidden="true"
      :loading="loading"
      decoding="async"
    />
    <!-- 兜底色块（无图 / 加载失败时） -->
    <div v-if="showFallback" class="cover-fallback" :style="fallbackStyle" role="img" :aria-label="alt || name">
      <span v-if="initial" class="cf-letter">{{ initial }}</span>
    </div>
  </div>
</template>

<style scoped>
.cover-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--paper-1, #f5f2ec);
}
.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* 模糊预览层：放大 + 高斯模糊，毛玻璃占位；清晰图就绪后淡出 */
.cover-blur {
  filter: blur(14px);
  transform: scale(1.12);
  opacity: 0.9;
  transition: opacity 0.35s ease-out;
}
.cover-blur.hide {
  opacity: 0;
}
/* 清晰层：默认透明（模糊层可见），加载完成后淡入浮现；置于模糊层之上 */
.cover-full {
  opacity: 0;
  transition: opacity 0.4s ease-out;
  z-index: 1;
}
.cover-full.reveal {
  opacity: 1;
}
.cover-fallback {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
/* 原尺寸模式：容器高度由图片自身决定（不裁切 / 不拉伸），文字可环绕其排布 */
.cover-wrap.is-natural {
  height: auto;
  /* 图片尚未就绪时给出最小高度，避免浮动配图塌陷造成布局跳动 */
  min-height: 90px;
}
.cover-wrap.is-natural .cover-full {
  position: static;
  width: auto;
  height: auto;
  max-width: 100%;
  object-fit: contain;
}
/* 原尺寸模式下无图（或加载失败）时，兜底色块给定比例，避免高度塌陷 */
.cover-wrap.is-natural.is-fallback {
  aspect-ratio: 16 / 10;
}
.cover-fallback::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 30% 30%, rgba(255,255,255,.22), transparent 60%);
}
.cf-letter {
  font-family: var(--serif);
  font-size: 3.4em;
  font-weight: 800;
  color: rgba(255,255,255,.92);
  text-shadow: 0 2px 12px rgba(0,0,0,.18);
  position: relative;
  z-index: 1;
}
</style>
