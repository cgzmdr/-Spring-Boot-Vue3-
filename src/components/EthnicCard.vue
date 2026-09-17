<script setup lang="ts">
import { computed } from 'vue'
import CoverImage from './CoverImage.vue'
import { fadeUp, stagger } from '@/utils/motion'
import { formatNumber } from '@/utils/format'

/** 卡片上的一个指标（图标 + 数值 + 悬浮说明） */
export interface CardStat {
	/** 数值（0 表示无，会被弱化显示） */
	value: number
	/** 单位/后缀说明，如「项」「位」 */
	unit?: string
	/** 悬浮提示 */
	title: string
}

const props = defineProps<{
	id: string
	name: string
	meta?: string
	coverImage?: string | null
	themeColor?: string
	/** 列表序号：用于错峰入场（@vueuse/motion） */
	index?: number
	/** 摘要指标（非遗/节日/人物/自治地方等）；为空则不渲染指标条 */
	stats?: CardStat[]
	/** 人口排名徽标（1 = 人口最多）；为空则不显示 */
	populationRank?: number
	/** 人口数值（用于排名徽标的悬浮说明） */
	population?: number
}>()

const link = computed(() => `/ethnic/${props.id}`)
/** 入场动效：按列表序号错峰上浮淡入 */
const motion = computed(() => fadeUp({ delay: stagger(props.index ?? 0) }))

/** 只展示有数据的指标，避免整排「0」占位显得空洞 */
const visibleStats = computed(() => (props.stats ?? []).filter((s) => s.value > 0))
</script>

<template>
	<router-link
		:to="link"
		class="ethnic-card"
		v-motion="motion"
	>
		<div class="img">
			<CoverImage
				:src="coverImage"
				:name="name"
				:theme="themeColor"
				:prompt="`${name} ethnic group in China in traditional costume, portrait, photography`"
				size="portrait_4_3"
			/>
			<!-- 人口排名徽标：一眼看出该民族的人口位次 -->
			<div
				v-if="populationRank"
				class="rank-badge"
				:title="population ? `人口 ${formatNumber(population)}（2020 年七普口径），在 56 个民族中排第 ${populationRank} 位` : `人口排名第 ${populationRank} 位`"
			>
				人口 #{{ populationRank }}
			</div>
		</div>
		<div class="body">
			<div class="name">{{ name }}</div>
			<div class="meta">{{ meta }}</div>
			<!-- 关联内容计数：把「点进详情才知道」的信息提前到卡片 -->
			<div
				v-if="visibleStats.length"
				class="stats"
			>
				<span
					v-for="s in visibleStats"
					:key="s.title"
					class="stat"
					:title="s.title"
				>
					<b>{{ s.value }}</b
					><i v-if="s.unit">{{ s.unit }}</i>
				</span>
			</div>
		</div>
	</router-link>
</template>

<style scoped>
/* 排名徽标：叠在封面右上角，不占用正文高度 */
.rank-badge {
	position: absolute;
	top: 8px;
	right: 8px;
	padding: 2px 8px;
	border-radius: 999px;
	font-size: 11px;
	font-weight: 600;
	letter-spacing: 0.2px;
	color: #fff;
	background: rgba(0, 0, 0, 0.55);
	backdrop-filter: blur(4px);
	pointer-events: none;
}
.img {
	position: relative;
}

/* 指标条：小字号、低对比，保持卡片整体的克制感 */
.stats {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
	margin-top: 8px;
	padding-top: 8px;
	border-top: 1px dashed var(--line, rgba(0, 0, 0, 0.1));
	font-size: 12px;
	color: var(--ink-3, #6b6b6b);
}
.stat {
	display: inline-flex;
	align-items: baseline;
	gap: 1px;
	cursor: help;
}
.stat b {
	font-weight: 600;
	color: var(--ink-1, #1a1a1a);
	font-variant-numeric: tabular-nums;
}
.stat i {
	font-style: normal;
	opacity: 0.75;
}
</style>
