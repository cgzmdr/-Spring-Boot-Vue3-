<script setup lang="ts">
import { computed } from "vue";
import PageHead from "@/components/PageHead.vue";
import CoverImage from "@/components/CoverImage.vue";
import Reveal from "@/components/Reveal.vue";
import RichArticle from "@/components/RichArticle.vue";
import type { ArticleFigure } from "@/utils/article";
import { useLangStore } from "@/stores/lang";

const lang = useLangStore();

/** 平台内容量（与 data 资源说明一致，展示数字化档案规模） */
const stats = computed(() => [
	{ num: "56", label: lang.pick("民族 Ethnic Groups", "Ethnic Groups") },
	{ num: "192", label: lang.pick("节日 Festivals", "Festivals") },
	{ num: "165", label: lang.pick("艺术 Arts", "Arts") },
	{ num: "222", label: lang.pick("风俗 Customs", "Customs") },
	{ num: "168", label: lang.pick("美食 Foods", "Foods") },
]);

const intro = computed(() => [
	{
		kind: "para" as const,
		text: lang.pick(
			"中华民族由 56 个独特民族构成，承载着丰富多彩的文化传统与民族风情。传统民族文化传播长期面临信息碎片化、呈现形式单一、可触达性弱、年轻群体触达不足等痛点。",
			"The Chinese nation is made up of 56 distinct ethnic groups, each carrying rich cultural traditions.",
		),
	},
	{
		kind: "para" as const,
		text: lang.pick(
			"本网站以数字化方式，将各民族的风俗习惯、节日庆典与传统艺术系统、权威、结构化地呈现给全球观众，打破地域与语言的藩篱，讲好中华民族共同体的故事。",
			"This platform presents customs, festivals and traditional arts in a systematic and structured digital form.",
		),
	},
]);

/** 介绍配图：民族全家福主题影像 */
const figures = computed<ArticleFigure[]>(() => [
	{
		src: null,
		prompt: "Chinese ethnic minority groups in colorful traditional costumes, cultural archive photography",
		caption: lang.pick("五十六个民族 · 文化影像", "Culture archive"),
		theme: "#B6402E",
	},
]);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> › <span>关于</span>
		</div>

		<PageHead
			kicker="ABOUT · 关于我们"
			title="关于我们"
			dek="《走进多彩 56 个民族世界》——一站式、沉浸式、可探索的中华民族数字文化博物馆。"
		/>

		<section
			class="section"
			style="padding-top: 24px"
		>
			<!-- 主视觉横幅：介绍不再只是纯文本 -->
			<div
				class="about-hero"
				v-motion-pop-in
			>
				<CoverImage
					mode="ai"
					:name="lang.pick('中华民族', 'Ethnic China')"
					:theme="'#B6402E'"
					:prompt="'Chinese ethnic minority groups celebrating together in traditional costumes, wide cultural banner, high quality photography'"
					size="landscape_16_9"
				/>
				<div class="about-hero-cap">
					{{ lang.pick("五十六个民族 · 多元一体", "One Family, 56 Ethnic Groups") }}
				</div>
			</div>

			<Reveal
				class="intro-wrap"
				:y="18"
			>
				<RichArticle
					:blocks="intro"
					:images="figures"
					:max-figures="1"
				/>
			</Reveal>

			<!-- 内容规模指标 -->
			<div
				class="metrics about-metrics"
				v-motion-fade-up
			>
				<div
					v-for="s in stats"
					:key="s.label"
					class="metric"
				>
					<div class="num">{{ s.num }}</div>
					<div class="lbl">{{ s.label }}</div>
				</div>
			</div>
		</section>

		<section
			class="section"
			style="padding-top: 0"
		>
			<Reveal :y="14">
				<div class="section-rule">
					<span class="no">01</span>
					<h3>核心目标</h3>
					<span class="line"></span>
				</div>
			</Reveal>
			<div class="grid grid-3">
				<Reveal
					v-for="(goal, i) in [
						{
							title: '系统性呈现',
							desc: '为 56 个民族建立统一、完整、权威的数字化文化档案。',
							prompt: 'archival ethnic culture documents and photographs',
						},
						{
							title: '沉浸式体验',
							desc: '通过图文、音视频、动效与地图打造可探索的文化空间。',
							prompt: 'immersive museum exhibition of ethnic culture',
						},
						{
							title: '全球化触达',
							desc: '多语言、无障碍、高性能，服务全球观众。',
							prompt: 'global audience reading about Chinese ethnic culture',
						},
					]"
					:key="goal.title"
					class="goal-card"
					:tag="'article'"
					:delay="i * 80"
					:y="20"
				>
					<div class="goal-img">
						<CoverImage
							mode="ai"
							:name="goal.title"
							:prompt="goal.prompt"
							:theme="'#B6402E'"
							size="landscape_4_3"
						/>
					</div>
					<div class="goal-body">
						<h4>{{ goal.title }}</h4>
						<p>{{ goal.desc }}</p>
					</div>
				</Reveal>
			</div>
		</section>

		<section
			class="section"
			style="padding-top: 0"
		>
			<Reveal :y="14">
				<div class="section-rule">
					<span class="no">02</span>
					<h3>数据与版权</h3>
					<span class="line"></span>
				</div>
			</Reveal>
			<div class="duo">
				<Reveal
					class="panel"
					:y="18"
				>
					<h3>数据来源</h3>
					<div class="article">
						<p>
							内容素材来源于公开权威资料，经团队整理、校订与结构化录入。涉及少数民族语言、民俗的专业表述，均经多方资料交叉核对。
						</p>
						<p>占位图片由 AI 生成，正式上线后将替换为真实授权图片素材。</p>
						<figure class="about-figure">
							<div class="fig-frame">
								<CoverImage
									mode="ai"
									name="数据整理"
									:prompt="'ethnic culture data archiving and research desk, warm light'"
									:theme="'#B6402E'"
									size="landscape_16_9"
								/>
							</div>
							<figcaption>结构化整理 · 多源交叉核对</figcaption>
						</figure>
					</div>
				</Reveal>
				<Reveal
					class="panel"
					:x="20"
					:delay="80"
				>
					<h3>版权与免责</h3>
					<div class="article">
						<p>
							本站内容仅供文化科普与学习交流使用。如涉及版权问题，请通过下方联系方式与我们取得联系。
						</p>
						<p>对因使用本站信息所产生的一切后果，本站不承担任何法律责任。</p>
						<figure class="about-figure">
							<div class="fig-frame">
								<CoverImage
									mode="ai"
									name="版权声明"
									:prompt="'copyright and license documents on a wooden desk, minimal, warm tone'"
									:theme="'#B6402E'"
									size="landscape_16_9"
								/>
							</div>
							<figcaption>内容授权与免责说明</figcaption>
						</figure>
					</div>
				</Reveal>
			</div>
		</section>

		<section
			class="section"
			style="padding-top: 0"
		>
			<Reveal :y="14">
				<div class="section-rule">
					<span class="no">03</span>
					<h3>联系我们</h3>
					<span class="line"></span>
				</div>
			</Reveal>
			<Reveal
				class="contact-wrap"
				:y="18"
			>
				<div
					class="info-card"
					style="max-width: 560px"
				>
					<dl>
						<dt>{{ lang.pick("邮箱", "email") }}</dt>
						<dd>cgzmdr@foxmail.com</dd>
						<dt>地址</dt>
						<dd>中华人民共和国</dd>
						<dt>反馈</dt>
						<dd>欢迎来信提出内容勘误与改进建议</dd>
					</dl>
				</div>
				<div class="contact-img">
					<CoverImage
						mode="ai"
						name="联系我们"
						:prompt="'warm welcoming cultural museum entrance with ethnic patterns'"
						:theme="'#B6402E'"
						size="landscape_4_3"
					/>
				</div>
			</Reveal>
		</section>
	</div>
</template>

<style scoped>
.about-hero {
	position: relative;
	aspect-ratio: 21 / 8;
	overflow: hidden;
	border: 1px solid var(--line);
	margin-bottom: 28px;
}
.about-hero :deep(img),
.about-hero :deep(.cover-fallback) {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.about-hero-cap {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	background: linear-gradient(transparent, rgba(20, 20, 20, 0.72));
	color: #fff;
	font-size: 13px;
	letter-spacing: 0.14em;
	padding: 26px 20px 14px;
}
.intro-wrap {
	margin-bottom: 28px;
}
.about-metrics {
	grid-template-columns: repeat(5, 1fr);
}
@media (max-width: 900px) {
	.about-metrics {
		grid-template-columns: repeat(2, 1fr);
	}
	.about-hero {
		aspect-ratio: 16 / 9;
	}
}
/* 核心目标卡片：图文并茂 */
.goal-card {
	border: 1px solid var(--line);
	background: var(--paper);
	overflow: hidden;
	display: block;
}
.goal-img {
	aspect-ratio: 16 / 10;
	overflow: hidden;
	background: var(--paper-2);
}
.goal-img :deep(img),
.goal-img :deep(.cover-fallback) {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.goal-body {
	padding: 20px 22px 24px;
}
.goal-body h4 {
	font-size: 20px;
	margin-bottom: 8px;
}
.goal-body p {
	font-size: 14.5px;
	line-height: 1.85;
	color: var(--muted);
}
.about-figure {
	margin: 18px 0 0;
}
.about-figure .fig-frame {
	aspect-ratio: 16 / 9;
	overflow: hidden;
	border: 1px solid var(--line);
}
.about-figure figcaption {
	font-size: 12.5px;
	color: var(--muted);
	margin-top: 8px;
	padding-left: 12px;
	border-left: 2px solid var(--accent);
}
.contact-wrap {
	display: grid;
	grid-template-columns: 560px 1fr;
	gap: 24px;
	align-items: start;
}
.contact-img {
	aspect-ratio: 16 / 9;
	overflow: hidden;
	border: 1px solid var(--line);
	max-width: 460px;
}
.contact-img :deep(img),
.contact-img :deep(.cover-fallback) {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
@media (max-width: 900px) {
	.contact-wrap {
		grid-template-columns: 1fr;
	}
}
</style>
