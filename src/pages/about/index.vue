<script setup lang="ts">
import PageHead from "@/components/PageHead.vue";
import { useLangStore } from "@/stores/lang";
const lang = useLangStore();
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
			<div class="article">
				<p class="dropcap">
					中华民族由 56
					个独特民族构成，承载着丰富多彩的文化传统与民族风情。传统民族文化传播长期面临信息碎片化、呈现形式单一、可触达性弱、年轻群体触达不足等痛点。
				</p>
				<p>
					本网站以数字化方式，将各民族的风俗习惯、节日庆典与传统艺术系统、权威、结构化地呈现给全球观众，打破地域与语言的藩篱，讲好中华民族共同体的故事。
				</p>
			</div>
		</section>

		<section
			class="section"
			style="padding-top: 0"
		>
			<div class="section-rule">
				<span class="no">01</span>
				<h3>核心目标</h3>
				<span class="line"></span>
			</div>
			<div class="grid grid-3">
				<div class="info-card">
					<h4
						style="
							font-family: var(--serif);
							font-size: 20px;
							letter-spacing: 1px;
							margin-bottom: 10px;
						"
					>
						系统性呈现
					</h4>
					<p style="font-size: 14px; color: var(--muted)">
						为 56 个民族建立统一、完整、权威的数字化文化档案。
					</p>
				</div>
				<div class="info-card">
					<h4
						style="
							font-family: var(--serif);
							font-size: 20px;
							letter-spacing: 1px;
							margin-bottom: 10px;
						"
					>
						沉浸式体验
					</h4>
					<p style="font-size: 14px; color: var(--muted)">
						通过图文、音视频、动效与地图打造可探索的文化空间。
					</p>
				</div>
				<div class="info-card">
					<h4
						style="
							font-family: var(--serif);
							font-size: 20px;
							letter-spacing: 1px;
							margin-bottom: 10px;
						"
					>
						全球化触达
					</h4>
					<p style="font-size: 14px; color: var(--muted)">
						多语言、无障碍、高性能，服务全球观众。
					</p>
				</div>
			</div>
		</section>

		<section
			class="section"
			style="padding-top: 0"
		>
			<div class="section-rule">
				<span class="no">02</span>
				<h3>数据与版权</h3>
				<span class="line"></span>
			</div>
			<div class="duo">
				<div class="panel">
					<h3>数据来源</h3>
					<div class="article">
						<p>
							内容素材来源于公开权威资料，经团队整理、校订与结构化录入。涉及少数民族语言、民俗的专业表述，均经多方资料交叉核对。
						</p>
						<p>占位图片由 AI 生成，正式上线后将替换为真实授权图片素材。</p>
					</div>
				</div>
				<div class="panel">
					<h3>版权与免责</h3>
					<div class="article">
						<p>
							本站内容仅供文化科普与学习交流使用。如涉及版权问题，请通过下方联系方式与我们取得联系。
						</p>
						<p>对因使用本站信息所产生的一切后果，本站不承担任何法律责任。</p>
					</div>
				</div>
			</div>
		</section>

		<section
			class="section"
			style="padding-top: 0"
		>
			<div class="section-rule">
				<span class="no">03</span>
				<h3>联系我们</h3>
				<span class="line"></span>
			</div>
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
		</section>
	</div>
</template>
