<script setup lang="ts">
import { ref, onMounted, computed, reactive } from "vue";
import { ElIcon, ElMessage } from "element-plus";
import { Warning, ArrowRight } from "@element-plus/icons-vue";
import SectionRule from "@/components/SectionRule.vue";
import EthnicCard from "@/components/EthnicCard.vue";
import CoverImage from "@/components/CoverImage.vue";
import {
	ethnicApi,
	festivalApi,
	artApi,
	topicApi,
	formApi,
	feedbackApi,
} from "@/api/modules";
import type {
	EthnicBrief,
	FestivalListItem,
	ArtListItem,
	TopicListItem,
	FeedBackFormType,
} from "@/api/types";
import { useLangStore } from "@/stores/lang";
import { useRouter } from "vue-router";

const lang = useLangStore();
const centerDialogVisible = ref(false);
const ethnicBriefs = ref<EthnicBrief[]>([]);
const festivals = ref<FestivalListItem[]>([]);
const arts = ref<ArtListItem[]>([]);
const topics = ref<TopicListItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const schema = ref();
const form = reactive<FeedBackFormType>({
	name: "",
	contact: "",
	topic: "bug",
	rating: "good",
	content: "",
	visitDate: "",
});
/** 首页固定专题（后端 /topics 为空时兜底） */
const staticFeatures = computed(() => [
	{
		no: "FEATURE 01",
		title: lang.pick("节日盛宴", "Festival Feast"),
		desc: lang.pick("一年四季的欢庆时刻", "Celebrations through the year"),
		to: "/festival",
		prompt:
			"Chinese ethnic minority festival celebration, lanterns and dance, night, photography",
		cover: null as string | null,
	},
	{
		no: "FEATURE 02",
		title: lang.pick("非遗传承", "Intangible Heritage"),
		desc: lang.pick("指尖上的千年技艺", "Craftsmanship through the ages"),
		to: "/art",
		prompt:
			"Chinese ethnic minority traditional handicraft and embroidery, close up, photography",
		cover: null as string | null,
	},
	{
		no: "FEATURE 03",
		title: lang.pick("民族团结", "Ethnic Unity"),
		desc: lang.pick(
			"像石榴籽一样紧紧抱在一起",
			"United like the seeds of a pomegranate",
		),
		to: "/unity",
		prompt:
			"Chinese ethnic minority traditional dance performance, photography",
		cover: null as string | null,
	},
]);
const router = useRouter();

onMounted(async () => {
	loading.value = true;
	try {
		const [briefs, fRes, aRes, tRes, formRes] = await Promise.allSettled([
			ethnicApi.list({ page: 1, size: 12, sort: "orderNum,asc" }),
			festivalApi.list({ page: 0, size: 6 }),
			artApi.list({ page: 0, size: 6 }),
			topicApi.list({ page: 0, size: 6 }),
			formApi.getByCode("feedback"),
		]);
		if (formRes.status === "fulfilled")
			schema.value = JSON.parse(formRes.value.schema);

		if (briefs.status === "fulfilled") {
			ethnicBriefs.value = briefs.value.data;
		}
		if (fRes.status === "fulfilled")
			festivals.value = fRes.value.data.slice(0, 3);
		if (aRes.status === "fulfilled") arts.value = aRes.value.data.slice(0, 3);
		// 后端有专题数据时优先展示，否则回退到内置固定专题
		if (tRes.status === "fulfilled" && tRes.value.data?.length)
			topics.value = tRes.value.data.slice(0, 3);
	} catch {
		ElMessage.error("首页数据加载失败");
	} finally {
		loading.value = false;
	}
});

/** 精选专题：后端 /topics 有数据用真实专题，否则用固定兜底 */
const features = computed(() =>
	topics.value.length
		? topics.value.map((t, i) => ({
				no: `FEATURE ${String(i + 1).padStart(2, "0")}`,
				title: t.title,
				desc: t.subtitle || t.description || "",
				to: "/topic/" + t.id,
				prompt: "",
				cover: t.coverImage || null,
			}))
		: staticFeatures.value,
);

/** 访问日期统一为 YYYY-MM-DD（el-date-picker 返回 Date 对象） */
function formatDate(d: unknown): string {
	if (!d) return "";
	const date = new Date(d as string | number | Date);
	if (Number.isNaN(date.getTime())) return String(d);
	const pad = (n: number) => String(n).padStart(2, "0");
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** 提交反馈表单到后端 */
async function handleSubmit() {
	if (!schema.value) {
		ElMessage.warning("反馈表单未加载，请刷新页面后重试");
		return;
	}
	const required = new Set<string>(
		(schema.value.fields || [])
			.filter((f: { required?: boolean }) => f.required)
			.map((f: { key: string }) => f.key),
	);
	if (required.has("name") && !form.name.trim()) {
		ElMessage.warning("请填写您的称呼");
		return;
	}
	if (required.has("topic") && !form.topic) {
		ElMessage.warning("请选择反馈类型");
		return;
	}
	if (required.has("content") && !form.content.trim()) {
		ElMessage.warning("请填写反馈内容");
		return;
	}
	submitting.value = true;
	try {
		await feedbackApi.submit({
			name: form.name.trim(),
			contact: form.contact.trim(),
			topic: form.topic,
			rating: form.rating,
			content: form.content.trim(),
			visitDate: formatDate(form.visitDate),
		});
		ElMessage.success("反馈提交成功，感谢您的建议！");
		centerDialogVisible.value = false;
		form.name = "";
		form.contact = "";
		form.topic = "bug";
		form.rating = "good";
		form.content = "";
		form.visitDate = "";
	} catch {
		// 错误提示由请求拦截器统一弹出
	} finally {
		submitting.value = false;
	}
}
</script>

<template>
	<!-- 封面故事 -->
	<header class="hero">
		<div class="container hero-duo">
			<div class="hero-text">
				<div class="kicker">
					{{ lang.pick("COVER STORY · 封面故事", "COVER STORY") }}
				</div>
				<h2>
					{{
						lang.pick(
							"走进多彩\n\r56 个民族世界",
							"The World of\n\r56 Ethnic Groups",
						)
					}}
				</h2>
				<p class="dek">
					{{
						lang.pick(
							"以数字化的方式，将各民族的风俗习惯、节日庆典与传统艺术呈现给全球观众，让文化跨越地域的藩篱。",
							"Bringing the customs, festivals and traditional arts of China's ethnic groups to audiences worldwide.",
						)
					}}
				</p>
				<div class="cta">
					<router-link
						class="btn btn-solid"
						to="/ethnic"
						>{{ lang.t("explore") }}</router-link
					>
					<router-link
						class="btn"
						to="/unity"
						>{{ lang.t("learn_56") }}</router-link
					>
				</div>
			</div>
			<figure class="hero-media">
				<CoverImage
					mode="ai"
					src="/images/placeholders/placeholder.webp"
					:prompt="'56 Chinese ethnic minorities in colorful traditional costumes celebrating festival together, joyful festive atmosphere, cinematic wide shot, high quality photography'"
					size="landscape_16_9"
					name="五十六个民族"
					:theme="'#B6402E'"
				/>
				<figcaption>
					{{
						lang.pick("五十六个民族 · 一家亲", "One Family, 56 Ethnic Groups")
					}}
				</figcaption>
			</figure>
		</div>
	</header>

	<!-- 01 五十六个民族 -->
	<section class="section">
		<div class="container">
			<SectionRule
				:no="'01'"
				:title="lang.t('section_all_ethnic')"
			/>
			<div
				class="grid grid-6"
				style="gap: 14px"
			>
				<EthnicCard
					v-for="e in ethnicBriefs"
					:key="e.id"
					:id="e.id"
					:name="e.name"
					:theme-color="e.themeColor"
					:cover-image="e.coverImage"
					:meta="e.name"
				/>
				<el-skeleton
					v-if="loading"
					:rows="1"
					animated
					style="grid-column: 1 / -1"
				/>
			</div>
			<div style="margin: 8px 0 0 auto; width: fit-content">
				<el-link
					type="danger"
					:icon="ArrowRight"
					@click="() => router.push('/ethnic')"
					>查看更多民族</el-link
				>
			</div>
		</div>
	</section>

	<!-- 02 精选专题 -->
	<section
		class="section"
		style="background: var(--paper-2)"
	>
		<div class="container">
			<SectionRule
				:no="'02'"
				:title="lang.t('section_features')"
			/>
			<div class="grid grid-3 seam">
				<router-link
					v-for="f in features"
					:key="f.no"
					:to="f.to"
					class="feature"
				>
					<div class="img">
						<CoverImage
							:src="f.cover"
							:name="f.title"
							:prompt="f.prompt"
							size="landscape_4_3"
							mode="ai"
							:theme="'#B6402E'"
						/>
					</div>
					<div class="t">
						<span class="no">{{ f.no }}</span>
						<h4>{{ f.title }}</h4>
						<p>{{ f.desc }}</p>
					</div>
				</router-link>
			</div>
		</div>
	</section>

	<!-- 03 文化之窗 -->
	<section class="section">
		<div class="container">
			<SectionRule
				:no="'03'"
				:title="lang.t('section_culture')"
			/>
			<div class="grid grid-2 seam">
				<div class="culture-col">
					<h4>{{ lang.t("section_festivals") }}</h4>
					<template
						v-for="(f, i) in festivals"
						:key="f.id"
					>
						<router-link
							class="list-item"
							:to="`/festival/${f.id}`"
						>
							<div class="idx">NO. 0{{ i + 1 }}</div>
							<h5>{{ f.name }}</h5>
							<p>
								{{ f.ethnicGroupName }} ·
								{{ f.lunarDate || f.solarDate || f.type }}
							</p>
							<div class="meta">
								{{ (f.customs || []).slice(0, 3).join(" · ") }}
							</div>
						</router-link>
					</template>
					<el-empty
						v-if="!loading && festivals.length === 0"
						description="暂无节日数据"
						:image-size="72"
					/>
				</div>
				<div class="culture-col">
					<h4>{{ lang.t("section_arts") }}</h4>
					<template
						v-for="(a, i) in arts"
						:key="a.id"
					>
						<router-link
							class="list-item"
							:to="`/art/${a.id}`"
						>
							<div class="idx">NO. 0{{ i + 1 }}</div>
							<h5>{{ a.name }}</h5>
							<p>{{ a.ethnicGroupName }} · {{ a.category }}</p>
							<div class="meta">
								{{ (a.inheritors || []).slice(0, 3).join(" · ") }}
							</div>
						</router-link>
					</template>
					<el-empty
						v-if="!loading && arts.length === 0"
						description="暂无艺术数据"
						:image-size="72"
					/>
				</div>
			</div>
		</div>
	</section>

	<div class="aside-btn">
		<div
			class="feedback"
			@click="centerDialogVisible = true"
		>
			<el-tooltip
				class="box-item"
				effect="light"
				content="反馈"
				placement="left"
			>
				<el-icon size="24">
					<Warning />
				</el-icon>
			</el-tooltip>
		</div>
	</div>
	<el-dialog
		v-model="centerDialogVisible"
		title="反馈"
		width="500"
		align-center
	>
		<el-form
			:model="form"
			label-width="auto"
			style="max-width: 600px"
		>
			<el-form-item
				v-for="field in schema.fields"
				:key="field.key"
				:label="field.placeholder || field.label"
				:required="field.required"
			>
				<el-input
					v-model="form.name"
					v-if="field.key === 'name'"
					:placeholder="field.label"
				/>
				<el-input
					v-model="form.contact"
					v-if="field.key === 'contact'"
					:placeholder="field.label"
				/>
				<el-select
					v-model="form.topic"
					v-if="field.key === 'topic'"
				>
					<el-option
						v-for="value in field.options"
						:label="value.label"
						:value="value.value"
					/>
				</el-select>
				<el-select
					v-model="form.rating"
					v-if="field.key === 'rating'"
				>
					<el-option
						v-for="value in field.options"
						:label="value.label"
						:value="value.value"
					/>
				</el-select>
				<el-input
					v-model="form.content"
					v-if="field.key === 'content'"
					:placeholder="field.label"
					type="textarea"
				/>
				<el-date-picker
					v-if="field.key === 'visitDate'"
					v-model="form.visitDate"
					type="date"
					:placeholder="field.label"
					clearable
				/>
			</el-form-item>
		</el-form>
		<template #footer>
			<div class="dialog-footer">
				<el-button @click="centerDialogVisible = false">关闭</el-button>
				<el-button
					type="primary"
					:loading="submitting"
					@click="handleSubmit"
				>
					提交
				</el-button>
			</div>
		</template>
	</el-dialog>
</template>

<style scoped>
.culture-col {
	background: var(--paper);
	padding: 30px 34px;
}
.culture-col h4 {
	font-family: var(--serif);
	font-size: 24px;
	letter-spacing: 2px;
	margin-bottom: 20px;
	padding-bottom: 14px;
	border-bottom: 1px solid var(--line);
}
.aside-btn {
	position: fixed;
	right: 1rem;
	bottom: 2rem;
	.feedback {
		box-shadow: -1px 2px 6px 4px rgba(0, 0, 0, 0.04);
		border: 1px solid var(--line);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 36px;
		height: 36px;
		cursor: pointer;
		background-color: var(--paper);
		transition: all ease-in-out 0.3s;
	}
	.feedback:hover {
		background-color: var(--paper-2);
		color: var(--accent);
		transform: scale(1.05);
	}
}
</style>
