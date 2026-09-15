<script setup lang="ts">
import { computed } from "vue";
import CoverImage from "./CoverImage.vue";
import type { ArticleBlock, ArticleFigure } from "@/utils/article";

const props = withDefaults(
	defineProps<{
		/** 已解析的正文段落（utils/article.ts parseArticle 结果） */
		blocks: ArticleBlock[];
		/** 可插入正文的配图池：按段落均匀分布，实现「图文并茂」 */
		images?: ArticleFigure[];
		/** 兜底主题色 */
		theme?: string;
		/** 首段首字下沉 */
		dropcap?: boolean;
		/** 最多插入多少张配图 */
		maxFigures?: number;
	}>(),
	{
		images: () => [],
		theme: "#B6402E",
		dropcap: true,
		maxFigures: 4,
	},
);

/** 首个正文段的块下标（首字下沉用） */
const firstParaIndex = computed(() => props.blocks.findIndex((b) => b.kind === "para"));

/**
 * 配图排布计划：把图片池均匀分配到各段落「之前」，
 * 图片以原尺寸浮动（float），随后的段落文字环绕其排布；
 * 左右交替，避免整页配图都挤在同一侧。
 */
const figures = computed(() => {
	const map = new Map<number, ArticleFigure[]>();
	const pool = props.images.filter((img) => img && (img.src || img.prompt));
	const positions = props.blocks.reduce<number[]>((acc, b, i) => {
		if (b.kind === "para") acc.push(i);
		return acc;
	}, []);
	if (!pool.length || !positions.length) return map;

	const count = Math.min(props.maxFigures, pool.length);
	const step = positions.length / (count + 1);
	for (let k = 0; k < count; k++) {
		const at = positions[Math.min(positions.length - 1, Math.floor((k + 1) * step))];
		const bucket = map.get(at) ?? [];
		if (bucket.length < 2) bucket.push(pool[k]);
		map.set(at, bucket);
	}
	return map;
});

/** 取某段落之前的配图 */
function figsAt(index: number): ArticleFigure[] {
	return figures.value.get(index) ?? [];
}

/** 浮动方向：按配图序号左右交替 */
function floatSide(index: number): "left" | "right" {
	const order = [...figures.value.keys()].indexOf(index);
	return order % 2 === 0 ? "right" : "left";
}

/** 无真实图片但有提示词时，走 AI 占位图（现为本地占位图） */
function imgMode(fig?: ArticleFigure): "theme" | "ai" {
	return fig && !fig.src && fig.prompt ? "ai" : "theme";
}
</script>

<template>
	<div class="article rich-article">
		<template
			v-for="(b, i) in blocks"
			:key="i"
		>
			<!-- 正文配图：原尺寸浮动，文字环绕；同一位置两张时并排为图组 -->
			<figure
				v-if="figsAt(i).length === 1"
				class="rich-figure is-float"
				:class="`is-${floatSide(i)}`"
			>
				<div class="fig-frame">
					<CoverImage
						:src="figsAt(i)[0].src"
						:name="figsAt(i)[0].caption"
						:theme="figsAt(i)[0].theme || theme"
						:prompt="figsAt(i)[0].prompt"
						:mode="imgMode(figsAt(i)[0])"
						fit="natural"
						loading="eager"
						size="landscape_16_9"
					/>
				</div>
				<figcaption v-if="figsAt(i)[0].caption">
					{{ figsAt(i)[0].caption }}
				</figcaption>
			</figure>
			<div
				v-else-if="figsAt(i).length > 1"
				class="fig-duo"
			>
				<figure
					v-for="(fig, k) in figsAt(i)"
					:key="k"
					class="rich-figure"
				>
					<div class="fig-frame">
						<CoverImage
							:src="fig.src"
							:name="fig.caption"
							:theme="fig.theme || theme"
							:prompt="fig.prompt"
							:mode="imgMode(fig)"
							fit="natural"
							loading="eager"
							size="landscape_4_3"
						/>
					</div>
					<figcaption v-if="fig.caption">{{ fig.caption }}</figcaption>
				</figure>
			</div>

			<h4
				v-if="b.kind === 'heading'"
				class="sec-title"
			>
				{{ b.text }}
			</h4>

			<blockquote
				v-else-if="b.kind === 'quote'"
				class="quote"
			>
				{{ b.text }}
			</blockquote>

			<figure
				v-else-if="b.kind === 'image'"
				class="rich-figure is-block"
			>
				<div class="fig-frame">
					<CoverImage
						:src="b.src"
						:name="b.text"
						:theme="theme"
						fit="natural"
						loading="eager"
						size="landscape_16_9"
					/>
				</div>
				<figcaption v-if="b.text">{{ b.text }}</figcaption>
			</figure>

			<p
				v-else
				:class="{ dropcap: dropcap && i === firstParaIndex }"
			>
				{{ b.text }}
			</p>
		</template>
	</div>
</template>
