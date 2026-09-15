<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import CoverImage from "@/components/CoverImage.vue";
import InteractionBar from "@/components/InteractionBar.vue";
import Badge from "@/components/Badge.vue";
import Reveal from "@/components/Reveal.vue";
import RichArticle from "@/components/RichArticle.vue";
import LinkedDiscussion from "@/components/LinkedDiscussion.vue";
import SourceReferences from "@/components/SourceReferences.vue";
import ImageCredits from "@/components/ImageCredits.vue";
import MachineTranslationNotice from "@/components/MachineTranslationNotice.vue";
import { foodApi, ethnicApi } from "@/api/modules";
import type { EthnicDetail, FoodDetail } from "@/api/types";
import { parseArticle } from "@/utils/article";
import type { ArticleFigure } from "@/utils/article";
import { useLangStore } from "@/stores/lang";
import { useViewTracking } from "@/composables/useViewTracking";

const route = useRoute();
const lang = useLangStore();

// 浏览行为上报在 detail 声明之后调用（见下方）

const detail = ref<FoodDetail | null>(null);
const ethnic = ref<EthnicDetail | null>(null);
const loading = ref(true);
const error = ref("");

// 浏览行为上报（方向 D：个性化推荐的隐式信号；未登录时后端静默忽略）
// 放在 detail 声明之后，避免引用未初始化的变量
useViewTracking("food", () => detail.value?.id);

/**
 * 当前是否确实在展示英文正文（供机器翻译提示判断）：
 * 处于英文语境且该条内容有英文正文，避免中文阅读时误报。
 */
const showingEnglish = computed(
	() => lang.isEn && !!(detail.value?.descriptionEn || "").trim(),
);

/** 正文段落：简介 + 发展沿革（拆成独立小节） */
const blocks = computed(() => {
	const d = detail.value;
	if (!d) return [];
	// 英文语境下优先用 descriptionEn（方向 C-3，机器翻译），无译文时回退中文
	const list = parseArticle(lang.pick(d.description || "", d.descriptionEn));
	if (d.origin) list.push({ kind: "heading", text: "发展沿革" }, ...parseArticle(d.origin));
	return list;
});

/** 同族其他美食 */
const related = computed(() =>
	(ethnic.value?.foods || [])
		.filter((f) => f.id !== detail.value?.id)
		.slice(0, 3),
);

/** 正文配图：优先同族美食实拍，保证「图文并茂」 */
const figures = computed<ArticleFigure[]>(() => {
	const d = detail.value;
	if (!d) return [];
	const pool: ArticleFigure[] = related.value
		.filter((f) => f.image)
		.map((f) => ({ src: f.image, caption: `${f.name} · ${ethnic.value?.name || ""}美食` }));
	if (!pool.length && d.image) pool.push({ src: d.image, caption: d.name });
	return pool;
});

const themeColor = computed(() => ethnic.value?.themeColor || "#B6402E");

async function load() {
	loading.value = true;
	error.value = "";
	const id = route.params.id as string;
	const ethnicId = (route.query.ethnic as string) || "";

	try {
		if (ethnicId) {
			// 常规入口：从民族详情携带所属民族，直接定位该美食（无需额外接口）
			const group = await ethnicApi.detail(ethnicId);
			ethnic.value = group;
			const found = group.foods?.find((f) => f.id === id);
			if (found) {
				detail.value = {
					id: found.id,
					ethnicGroupId: group.id,
					ethnicGroupName: group.name,
					name: found.name,
					nameEn: found.nameEn || null,
					description: found.description || "",
					origin: found.origin || null,
					image: found.image || null,
				};
			}
		}
		if (!detail.value) {
			// 直接访问 /food/:id（分享链接、收藏夹等）：走后端美食详情接口
			detail.value = await foodApi.detail(id);
		}
		// 同族内容（同族美食 / 面包屑 / 主题色）
		const groupId = detail.value?.ethnicGroupId || ethnicId;
		if (groupId && ethnic.value?.id !== groupId) {
			try {
				ethnic.value = await ethnicApi.detail(groupId);
			} catch {
				/* 同族信息缺失不影响美食正文 */
			}
		}
	} catch {
		error.value = "美食信息加载失败";
	} finally {
		loading.value = false;
	}
}

onMounted(load);
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">首页</router-link> ›
			<router-link to="/ethnic">民族</router-link> ›
			<router-link
				v-if="detail?.ethnicGroupId"
				:to="`/ethnic/${detail.ethnicGroupId}`"
			>
				{{ detail?.ethnicGroupName || "民族" }}
			</router-link>
			<span>{{ detail?.name || "加载中" }}</span>
		</div>
	</div>

	<div class="container">
		<el-skeleton
			v-if="loading"
			:rows="8"
			animated
		/>
		<el-empty
			v-else-if="error || !detail"
			:description="error || '美食不存在'"
		>
			<el-button
				type="primary"
				@click="load"
				>重试</el-button
			>
		</el-empty>

		<template v-else>
			<div class="page-head">
				<div
					class="badge-wrap"
					style="margin-bottom: 14px"
					v-motion-fade-in
				>
					<Badge :text="lang.pick('FOOD · 民族美食', 'FOOD')" />
					<Badge
						v-if="detail.ethnicGroupName"
						:text="detail.ethnicGroupName"
						ink
					/>
				</div>
				<h2 v-motion-fade-up>{{ lang.pick(detail.name, detail.nameEn) }}</h2>
				<p
					class="dek"
					v-motion-fade-up
				>
					{{ detail.ethnicGroupName || "民族美食" }}
					<span v-if="detail.nameEn"> · {{ detail.nameEn }}</span>
				</p>
			</div>

			<div
				class="hero-img-wrap"
				v-motion-pop-in
			>
				<CoverImage
					:src="detail.image"
					:name="detail.name"
					:theme="themeColor"
					:prompt="`${detail.name} ethnic food, food photography`"
					size="landscape_16_9"
				/>
			</div>

			<InteractionBar
				type="food"
				:id="detail.id"
			/>

			<div class="duo">
				<Reveal
					class="panel"
					tag="div"
					:y="18"
				>
					<div class="panel-head">
						<h3>美食介绍</h3>
						<span class="panel-hint">{{ blocks.length }} 段落 · 图文并茂</span>
					</div>
					<MachineTranslationNotice
						:source="detail.descriptionEnSource"
						:active="showingEnglish"
					/>
					<RichArticle
						v-if="blocks.length"
						:blocks="blocks"
						:images="figures"
						:theme="themeColor"
						:max-figures="3"
					/>
					<p
						v-else
						class="article"
					>
						暂无该美食的详细介绍。
					</p>
				</Reveal>

				<Reveal
					class="panel"
					tag="div"
					:x="20"
					:delay="80"
				>
					<h3>基本资料</h3>
					<div
						class="info-card"
						style="border: none; padding: 0"
					>
						<dl>
							<dt>美食名称</dt>
							<dd>{{ detail.name }}</dd>
							<dt>英文名称</dt>
							<dd>{{ detail.nameEn || "—" }}</dd>
							<dt>所属民族</dt>
							<dd>
								<router-link
									v-if="detail.ethnicGroupId"
									class="inline-link"
									:to="`/ethnic/${detail.ethnicGroupId}#foods`"
								>
									{{ detail.ethnicGroupName || "查看民族" }}
								</router-link>
								<template v-else>—</template>
							</dd>
							<dt>聚居地区</dt>
							<dd>{{ ethnic?.region?.join("、") || "—" }}</dd>
							<dt>相关标签</dt>
							<dd>
								<div class="tags">
									<Badge
										:text="lang.pick('饮食文化', 'Food')"
										ink
									/>
									<Badge
										v-if="ethnic?.languageFamily"
										:text="ethnic.languageFamily"
										ink
									/>
								</div>
							</dd>
						</dl>
					</div>
					<div
						v-if="detail.ethnicGroupId"
						class="panel-foot"
					>
						<router-link
							class="link-btn"
							:to="`/ethnic/${detail.ethnicGroupId}#foods`"
						>
							查看 {{ detail.ethnicGroupName || "该民族" }} 的全部美食
						</router-link>
					</div>
				</Reveal>
			</div>

			<template v-if="related.length">
				<div class="section same-group">
					<Reveal :y="14">
						<div class="section-rule">
							<span class="no">01</span>
							<h3>同族美食</h3>
							<span class="line"></span>
						</div>
					</Reveal>
					<div class="grid grid-3">
						<router-link
							v-for="f in related"
							:key="f.id"
							class="feature"
							:to="{ path: `/food/${f.id}`, query: { ethnic: detail.ethnicGroupId || '' } }"
							v-motion-fade-up
						>
							<div class="img">
								<CoverImage
									:src="f.image"
									:name="f.name"
									:theme="themeColor"
									:prompt="`${f.name} ethnic food, food photography`"
									size="landscape_4_3"
								/>
							</div>
							<div class="t">
								<span class="no">{{ lang.pick("FOOD · 美食", "FOOD") }}</span>
								<h4>{{ lang.pick(f.name, f.nameEn) }}</h4>
								<p>{{ f.description }}</p>
							</div>
						</router-link>
					</div>
				</div>
			</template>

			<!-- 参考资料（数据出处，方向 C-1 可溯源） -->
			<SourceReferences
				target-type="food"
				:target-id="detail.id"
			/>

			<!-- 图片来源与许可（方向 C-4 署名） -->
			<ImageCredits
				target-type="food"
				:target-id="detail.id"
			/>

			<!-- 相关讨论（社区联动） -->
			<LinkedDiscussion
				linked-type="food"
				:linked-id="detail.id"
				:label="detail.name"
			/>
		</template>
	</div>
</template>

<style scoped>
.hero-img-wrap {
	margin: 24px 0 8px;
	aspect-ratio: 16 / 6;
	overflow: hidden;
	border: 1px solid var(--line);
}
.hero-img-wrap :deep(img),
.hero-img-wrap :deep(.cover-fallback) {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.panel-hint {
	font-size: 12px;
	letter-spacing: 0.06em;
	color: var(--muted);
}
.panel-foot {
	margin-top: 20px;
}
.link-btn {
	display: inline-block;
	border: 1px solid var(--line);
	padding: 10px 16px;
	color: var(--ink);
	text-decoration: none;
	font-size: 14px;
	transition: all var(--dur-fast) ease;
}
.link-btn:hover {
	border-color: var(--accent);
	color: var(--accent);
}
.inline-link {
	color: var(--accent);
	text-decoration: none;
}
.inline-link:hover {
	text-decoration: underline;
}
.same-group {
	padding-top: 28px;
}
</style>
