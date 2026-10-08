<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import { ElMessage } from "element-plus";
import { feedbackApi } from "@/api/modules";
import type { FeedBackFormType } from "@/api/types";

/**
 * 首页反馈弹窗。
 *
 * 为什么单独拆成组件：它内部用到了 el-dialog / el-form / el-form-item /
 * el-input / el-select / el-option / el-date-picker / el-button 一整套表单组件，
 * 但只有用户点了右下角「反馈」才会出现。之前内联在首页里，
 * 相当于让每个 3G 用户在首屏就下载一个可能永远不打开的复杂表单。
 * 拆出后由首页用 defineAsyncComponent 按需加载。
 */

interface SchemaField {
	key: string;
	label: string;
	placeholder?: string;
	required?: boolean;
	options?: { label: string; value: string }[];
}
interface Schema {
	fields?: SchemaField[];
}

/** v-model：弹窗开关（父组件持有） */
const open = defineModel<boolean>({ required: true });

const props = defineProps<{
	/** 后端下发的表单 schema（JSON 字符串） */
	schemaJson?: string | null;
}>();

const submitting = ref(false);

const form = reactive<FeedBackFormType>({
	name: "",
	contact: "",
	topic: "bug",
	rating: "good",
	content: "",
	visitDate: "",
});

/** 解析表单 schema；字段顺序与 required 标记均由后端决定 */
const schema = computed<Schema>(() => {
	if (!props.schemaJson) return {};
	try {
		return JSON.parse(props.schemaJson) as Schema;
	} catch {
		return {};
	}
});

/** 访问日期统一为 YYYY-MM-DD（el-date-picker 返回 Date 对象） */
function formatDate(d: unknown): string {
	if (!d) return "";
	const date = new Date(d as string | number | Date);
	if (Number.isNaN(date.getTime())) return String(d);
	const pad = (n: number) => String(n).padStart(2, "0");
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

async function handleSubmit() {
	const fields = schema.value.fields || [];
	if (!fields.length) {
		ElMessage.warning("反馈表单未加载，请刷新页面后重试");
		return;
	}
	const required = new Set<string>(
		fields.filter((f) => f.required).map((f) => f.key),
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
		open.value = false;
		form.name = "";
		form.contact = "";
		form.topic = "bug";
		form.rating = "good";
		form.content = "";
		form.visitDate = "";
	} catch {
		/* 错误提示由请求拦截器统一弹出 */
	} finally {
		submitting.value = false;
	}
}
</script>

<template>
	<el-dialog
		v-model="open"
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
					v-if="field.key === 'name'"
					v-model="form.name"
					:placeholder="field.label"
				/>
				<el-input
					v-if="field.key === 'contact'"
					v-model="form.contact"
					:placeholder="field.label"
				/>
				<el-select
					v-if="field.key === 'topic'"
					v-model="form.topic"
				>
					<el-option
						v-for="value in field.options"
						:key="value.value"
						:label="value.label"
						:value="value.value"
					/>
				</el-select>
				<el-select
					v-if="field.key === 'rating'"
					v-model="form.rating"
				>
					<el-option
						v-for="value in field.options"
						:key="value.value"
						:label="value.label"
						:value="value.value"
					/>
				</el-select>
				<el-input
					v-if="field.key === 'content'"
					v-model="form.content"
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
				<el-button @click="open = false">关闭</el-button>
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
