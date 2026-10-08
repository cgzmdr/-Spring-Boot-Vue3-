<script setup lang="ts">
import { ref, computed } from "vue";
import { aiImage, placeholderLqip, isLocalAsset } from "@/utils/format";

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
    /**
     * 解析优先级：当同屏有多张图竞争带宽时，把首屏主视觉标为 high，
     * 其余保持默认，避免 12 张卡片图一起抢 3G 的有限带宽。
     */
    fetchpriority?: 'high' | 'low' | 'auto'
  }>(),
  { src: null, name: '', theme: '#B6402E', prompt: '', mode: 'theme', fit: 'cover', loading: 'lazy', size: 'landscape_16_9', alt: '', fetchpriority: 'auto' },
)

const aiFailed = ref(false)

const API_BASE: string = (import.meta.env.VITE_API_BASE as string) || ''
const STATIC_BASE: string = (import.meta.env.VITE_STATIC_BASE as string) || API_BASE

/**
 * 图片地址解析：后端图片指向 API origin，前端自带占位图保持同源。
 *
 * 判断依据是 `isLocalAsset()`（只认 `/images/placeholders/`），
 * 而**不是**笼统的 `/images/` 前缀 —— 真实内容图（`/images/ethnic/...`、
 * `/images/topic/...`）存在后端，若被误当成本地资源会：
 *   · dev：Vite 把未知路径回退成 index.html，图片全部裂开；
 *   · 线上：直接 404。
 * 这个坑真实发生过，改为白名单前缀后修复。
 */
function resolveImageUrl(src: string): string {
  if (/^https?:\/\//i.test(src)) return src
  // 随前端发布的占位图 / 图标：同源直出，享受预加载与长缓存
  if (isLocalAsset(src)) return src
  if (src.startsWith("/") && /^https?:\/\//i.test(STATIC_BASE)) {
    try {
      // 取后端 origin，避免 STATIC_BASE 带路径时拼错图片地址
      return new URL(STATIC_BASE).origin + src
    } catch {
      return STATIC_BASE.replace(/\/+$/, "") + src
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

/**
 * 占位层：
 * · 同一静态目录下存在 `{base}-blur.webp` 时优先用它（更贴原图）；
 * · 否则退回内联 LQIP（data URL，零请求、永不失败）。
 *
 * 修正说明：原实现只在清晰图 URL 以 .webp 结尾时才生成 blur 地址，
 * 因此 .jpg / .png 封面在加载完成前是一块空白。现在 LQIP 覆盖所有情况。
 */
const lqipSrc = placeholderLqip
const blurSrc = computed(() => {
  if (!src.value) return ""
  if (src.value.endsWith(".webp")) return src.value.replace(/\.webp$/, "-blur.webp")
  return lqipSrc
})

/** 占位层直接作为 CSS 背景：data URL 零请求、解码即渲染，不存在破图图标问题 */
const placeholderStyle = computed(() => ({
  backgroundImage: `url("${blurSrc.value}")`,
}))

const fullLoaded = ref(false)
const fullFailed = ref(false)

/**
 * 关键：清晰层必须「始终渲染」以触发浏览器加载（@load），
 * 不能用 v-if 依赖 fullLoaded —— 否则清晰层不挂载、load 永不触发，形成死锁。
 * 这里用 opacity 过渡控制可见性：加载完成前透明（占位层可见），完成后淡入。
 */
const showFull = computed(() => !!src.value)
const fullRevealed = computed(() => fullLoaded.value || fullFailed.value)
const showBlur = computed(() => !!blurSrc.value && !fullFailed.value)
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
    :class="{ 'is-natural': fit === 'natural', 'is-fallback': showFallback, 'is-loaded': fullLoaded }"
    :style="showBlur ? placeholderStyle : undefined"
  >
    <!-- 清晰图：始终渲染以触发加载；加载完成前透明，完成后淡入 -->
    <img
      v-if="showFull"
      class="cover-img cover-full"
      :class="{ reveal: fullRevealed }"
      :src="src"
      :alt="alt || name"
      :loading="loading"
      :fetchpriority="fetchpriority"
      decoding="async"
      @load="onFullLoad"
      @error="onFullError"
    />
    <!-- 兜底色块（无图 / 加载失败时） -->
    <div v-if="showFallback" class="cover-fallback" :style="fallbackStyle" role="img" :aria-label="alt || name">
      <span v-if="initial" class="cf-letter">{{ initial }}</span>
    </div>
  </div>
</template>

<style scoped>
/*
 * 占位图作为容器的 CSS 背景（而非 <img>），好处：
 *   · LQIP 是内联 data URL，不产生网络请求，解码后立即绘制；
 *   · 背景图永远不会有「破图」图标，也不会进入无障碍树；
 *   · 低分辨率图交给渲染器缩放 + filter 模糊，比缩小 <img> 再放大更省事。
 * `is-loaded` 后背景淡出，露出下方已加载完成的清晰图。
 */
.cover-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: var(--paper-1, #f5f2ec);
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
}
/*
 * 占位层的模糊。用 ::after 叠一层带 backdrop-filter 的透明层，
 * 这样只模糊「容器自身的背景」（即 LQIP），不会把上层已加载的清晰图一起糊掉。
 * 清晰图加载完成后（.is-loaded）整层移除，避免无谓的合成开销。
 */
.cover-wrap:not(.is-loaded):not(.is-fallback)::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  backdrop-filter: blur(12px) saturate(1.05);
  -webkit-backdrop-filter: blur(12px) saturate(1.05);
  /* 略微放大以盖住 blur 在边缘产生的透明羽化 */
  transform: scale(1.06);
  pointer-events: none;
}
.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* 清晰层：默认透明（占位层可见），加载完成后淡入浮现；置于占位层之上 */
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
