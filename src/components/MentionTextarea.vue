<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from "vue";
import type { InputInstance } from "element-plus";
import UserAvatar from "@/components/UserAvatar.vue";
import { socialApi } from "@/api/modules";
import type { MentionUser } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * @提及输入框：在普通文本域上叠加「@ 提及自动提示」。
 *
 * 交互约定（与后端 `MentionParser` 保持一致）：
 * · 输入 `@` 后开始联想，昵称支持中英文 / 数字 / `_` / `-` / `·`，最长 32；
 * · ↑↓ 选择、Enter / Tab 插入、Esc 关闭；鼠标点选用 mousedown.prevent，不会打断输入焦点；
 * · 插入的文本固定为 `@昵称 `（末尾空格用于结束提及），后端按昵称精确匹配并通知被提及者；
 * · 候选 = 本页已参与的用户（即时、离线可用）+ 服务端联想（默认 10 条，上限 20）；
 * · 服务端联想不可用时静默降级为本地候选，不打断回复流程。
 */

/** 本地候选（帖子作者 / 已参与楼层的用户）：只有昵称是必需的，hint 用于标注来源 */
interface MentionLocal {
	id?: string | null;
	nickname?: string | null;
	avatar?: string | null;
	bio?: string | null;
	hint?: string;
}

/** 面板中的一条候选：昵称是唯一键（后端按昵称解析提及） */
interface Candidate {
	id: string;
	nickname: string;
	avatar: string | null;
	bio: string | null;
	hint?: string;
}

const props = withDefaults(
	defineProps<{
		modelValue: string;
		rows?: number;
		maxlength?: number;
		placeholder?: string;
		disabled?: boolean;
		/** 本地候选：输入 `@` 时立即提示（无需等服务端） */
		locals?: MentionLocal[];
		/** 是否请求服务端联想（接口不可用时自动静默降级为本地候选） */
		remoteSuggest?: boolean;
	}>(),
	{ rows: 5, maxlength: 5000, placeholder: "", disabled: false, remoteSuggest: true, locals: () => [] },
);

const emit = defineEmits<{
	(e: "update:modelValue", value: string): void;
	/** 成功插入一条提及（供外层做轻提示） */
	(e: "pick", user: Candidate): void;
	/** 联想面板开关（供外层联动提示文案） */
	(e: "toggle", open: boolean): void;
}>();

const lang = useLangStore();

const inputRef = ref<InputInstance | null>(null);
const fieldEl = ref<HTMLElement | null>(null);

const open = ref(false);
const loading = ref(false);
const openUp = ref(false);
const items = ref<Candidate[]>([]);
const active = ref(0);
/** 当前提及的 `@` 在文本中的位置，以及 `@` 之后已输入的关键字 */
const queryStart = ref(0);
const queryText = ref("");
const composing = ref(false);

const MAX_SUGGEST = 10;
/** 昵称字符集：与后端 MentionParser 的正则一致 */
const NAME_RE = /^[\p{L}\p{N}_\-·]{0,32}$/u;
/** 可作为提及目标的完整昵称（至少 1 个字符，全字符合法） */
const MENTIONABLE_RE = /^[\p{L}\p{N}_\-·]{1,32}$/u;
/** 允许出现在 `@` 之前的字符（行首或标点之后视为一次新提及） */
const BOUNDARY_RE = /[\s(（[【{<>《「『"'，,。；;：:！!？?、~～|/\\+*&^%$#=`]/u;

const titleText = computed(() => lang.pick("选择要提及的用户", "Mention someone"));
const metaText = computed(() => {
	const prefix = queryText.value ? `@${queryText.value}` : lang.pick("推荐提及", "Suggested");
	return `${prefix} · ${lang.pick(`最多 ${MAX_SUGGEST} 条`, `up to ${MAX_SUGGEST}`)}`;
});
const emptyText = computed(() =>
	loading.value
		? lang.pick("搜索中…", "Searching…")
		: lang.pick("没有匹配的用户，可直接输入完整昵称", "No match — type the full nickname"),
);
const footText = computed(() =>
	lang.pick(
		"↑↓ 选择 · Enter / Tab 插入 · Esc 关闭；被提及者会收到站内通知",
		"↑↓ select · Enter / Tab insert · Esc close; they get notified",
	),
);

function textareaEl(): HTMLTextAreaElement | null {
	const el = inputRef.value as unknown as { textarea?: HTMLTextAreaElement } | null;
	return el?.textarea ?? null;
}

/**
 * 当前文本：以 DOM 里的实时值为准。
 * `props.modelValue` 要等父组件重渲染后才更新（比原生 input 事件慢一帧），
 * 用它做联想判断会始终慢一个字符、甚至永远判断不到刚输入的「@」。
 */
function currentText(): string {
	return textareaEl()?.value ?? props.modelValue;
}

/** 归一化本地候选：剔除空昵称、重复昵称与无法被 `@昵称` 解析的昵称 */
function localCandidates(): Candidate[] {
	const out: Candidate[] = [];
	const seen = new Set<string>();
	for (const raw of props.locals || []) {
		const nickname = (raw.nickname || "").trim();
		if (!nickname || seen.has(nickname) || !MENTIONABLE_RE.test(nickname)) continue;
		seen.add(nickname);
		out.push({
			id: raw.id || `local:${nickname}`,
			nickname,
			avatar: raw.avatar ?? null,
			bio: raw.bio ?? null,
			hint: raw.hint,
		});
	}
	return out;
}

/** 光标前是否正处于一次「@提及」输入中（返回 `@` 的位置与关键字） */
function detectQuery(): { start: number; text: string } | null {
	const el = textareaEl();
	if (!el) return null;
	const value = currentText();
	const caret = el.selectionStart ?? value.length;
	if (caret <= 0 || caret > value.length) return null;
	const before = value.slice(0, caret);
	const at = before.lastIndexOf("@");
	if (at < 0) return null;
	const prev = at > 0 ? before.slice(at - 1, at) : "";
	if (prev && !BOUNDARY_RE.test(prev)) return null;
	const text = before.slice(at + 1);
	if (!NAME_RE.test(text)) return null;
	return { start: at, text };
}

let timer: number | undefined;
let blurTimer: number | undefined;

function close() {
	window.clearTimeout(blurTimer);
	if (!open.value) return;
	open.value = false;
	items.value = [];
	active.value = 0;
	emit("toggle", false);
}

/** 根据输入框在视口中的位置决定向上还是向下展开（回复框常在页面底部） */
function place() {
	const el = fieldEl.value;
	if (!el) return;
	const rect = el.getBoundingClientRect();
	const panelHeight = 280;
	openUp.value = rect.bottom + panelHeight > window.innerHeight && rect.top > panelHeight;
}

function scheduleSearch() {
	if (props.disabled) return close();
	const q = detectQuery();
	if (!q) return close();
	queryStart.value = q.start;
	queryText.value = q.text;
	place();
	open.value = true;
	emit("toggle", true);
	window.clearTimeout(timer);
	timer = window.setTimeout(() => search(q.text), 150);
}

async function search(text: string) {
	const local = localCandidates().filter((c) => !text || c.nickname.toLowerCase().includes(text.toLowerCase()));
	items.value = local.slice(0, MAX_SUGGEST);
	active.value = 0;
	if (!props.remoteSuggest) return;
	loading.value = true;
	try {
		const remote: MentionUser[] = await socialApi.suggestUsers(text, MAX_SUGGEST);
		// 过期响应（用户已继续输入或面板已关闭）直接丢弃
		if (text !== queryText.value || !open.value) return;
		items.value = merge(local, remote).slice(0, MAX_SUGGEST);
	} catch {
		/* 服务端联想不可用：保留本地候选，不打断回复 */
	} finally {
		loading.value = false;
	}
}

/** 本地候选优先，并按昵称去重（后端按昵称解析提及，重复昵称只需一条） */
function merge(local: Candidate[], remote: MentionUser[]): Candidate[] {
	const out: Candidate[] = [];
	const seen = new Set<string>();
	for (const c of [...local, ...remote.map(toCandidate)]) {
		const key = c.nickname.toLowerCase();
		if (seen.has(key)) continue;
		seen.add(key);
		out.push(c);
	}
	return out;
}

function toCandidate(user: MentionUser): Candidate {
	return { id: user.id, nickname: user.nickname, avatar: user.avatar ?? null, bio: user.bio ?? null };
}

/** 插入 `@昵称 `，并把光标移到空格之后，便于继续输入 */
async function pick(item?: Candidate) {
	if (!item) return;
	const el = textareaEl();
	const value = currentText();
	const caret = el?.selectionStart ?? value.length;
	const start = queryStart.value;
	const inserted = `@${item.nickname} `;
	const next = value.slice(0, start) + inserted + value.slice(Math.max(start, caret));
	const nextCaret = start + inserted.length;
	emit("update:modelValue", next);
	emit("pick", item);
	close();
	await nextTick();
	const after = textareaEl();
	if (after) {
		after.focus();
		after.setSelectionRange(nextCaret, nextCaret);
	}
}

function onInput(value: string) {
	emit("update:modelValue", value);
	// 中文输入法组合期间不联想，等 compositionend 再触发
	if (composing.value) return;
	scheduleSearch();
}

function onCompositionEnd() {
	composing.value = false;
	scheduleSearch();
}

// el-input 的 keydown 事件类型为 Event | KeyboardEvent（Element Plus 透传），此处按键盘事件处理
function onKeydown(raw: Event | KeyboardEvent) {
	const event = raw as KeyboardEvent;
	if (event.key === "Escape" && open.value) {
		event.preventDefault();
		close();
		return;
	}
	if (!open.value || !items.value.length) return;
	if (event.key === "ArrowDown") {
		event.preventDefault();
		active.value = (active.value + 1) % items.value.length;
	} else if (event.key === "ArrowUp") {
		event.preventDefault();
		active.value = (active.value - 1 + items.value.length) % items.value.length;
	} else if ((event.key === "Enter" || event.key === "Tab") && !event.isComposing) {
		event.preventDefault();
		pick(items.value[active.value]);
	}
}

/** 光标移动 / 点击后重新判断是否仍处于提及态 */
function syncCaret() {
	if (!composing.value) scheduleSearch();
}

function onFocusOut() {
	// 面板内点选使用 mousedown.prevent，不会失焦；此处兜底关闭
	window.clearTimeout(blurTimer);
	blurTimer = window.setTimeout(close, 160);
}

onBeforeUnmount(() => {
	window.clearTimeout(timer);
	window.clearTimeout(blurTimer);
});

defineExpose({ focus: () => textareaEl()?.focus() });
</script>

<template>
	<div
		ref="fieldEl"
		class="mention-field"
	>
		<el-input
			ref="inputRef"
			type="textarea"
			:model-value="modelValue"
			:rows="rows"
			:maxlength="maxlength"
			show-word-limit
			:disabled="disabled"
			:placeholder="placeholder"
			@update:model-value="onInput"
			@keydown="onKeydown"
			@click="syncCaret"
			@keyup="syncCaret"
			@compositionstart="composing = true"
			@compositionend="onCompositionEnd"
			@blur="onFocusOut"
		/>

		<!-- @提及候选：本页参与者 + 服务端联想（默认 10 条） -->
		<div
			v-if="open"
			class="mention-panel"
			:class="{ up: openUp }"
			role="listbox"
		>
			<div class="mp-head">
				<span>{{ titleText }}</span>
				<span class="mp-meta">{{ metaText }}</span>
			</div>
			<ul
				v-if="items.length"
				class="mp-list"
			>
				<li
					v-for="(item, i) in items"
					:key="item.id || item.nickname"
					class="mp-item"
					:class="{ active: i === active }"
					role="option"
					:aria-selected="i === active"
					@mouseenter="active = i"
					@mousedown.prevent="pick(item)"
				>
					<UserAvatar
						:name="item.nickname"
						:src="item.avatar"
						:size="24"
					/>
					<span class="mp-name">@{{ item.nickname }}</span>
					<span class="mp-desc">{{ item.hint || item.bio || "" }}</span>
				</li>
			</ul>
			<div
				v-else
				class="mp-empty"
			>
				{{ emptyText }}
			</div>
			<div class="mp-foot">{{ footText }}</div>
		</div>
	</div>
</template>

<style scoped>
.mention-field {
	position: relative;
}
.mention-panel {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(100% + 6px);
	z-index: 40;
	background: var(--paper);
	border: 1px solid var(--ink);
	box-shadow: 0 14px 30px rgba(0, 0, 0, 0.12);
	animation: mention-in 0.16s var(--ease-out);
}
.mention-panel.up {
	top: auto;
	bottom: calc(100% + 6px);
}
@keyframes mention-in {
	from {
		opacity: 0;
		transform: translateY(-4px);
	}
	to {
		opacity: 1;
		transform: none;
	}
}
.mp-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
	padding: 8px 12px;
	border-bottom: 1px solid var(--line);
	font-size: 12px;
	color: var(--muted);
	letter-spacing: 0.04em;
	background: var(--paper-2);
}
.mp-meta {
	color: var(--accent);
}
.mp-list {
	margin: 0;
	padding: 0;
	list-style: none;
	max-height: 232px;
	overflow-y: auto;
}
.mp-item {
	display: flex;
	align-items: center;
	gap: 10px;
	padding: 7px 12px;
	cursor: pointer;
	border-bottom: 1px solid var(--line);
}
.mp-item:last-child {
	border-bottom: none;
}
.mp-item.active {
	background: var(--paper-2);
}
.mp-item.active .mp-name {
	color: var(--accent);
	font-weight: 600;
}
.mp-name {
	font-size: 13.5px;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	max-width: 46%;
}
.mp-desc {
	font-size: 12px;
	color: var(--muted);
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	margin-left: auto;
}
.mp-empty {
	padding: 14px 12px;
	font-size: 13px;
	color: var(--muted);
}
.mp-foot {
	padding: 6px 12px;
	border-top: 1px solid var(--line);
	font-size: 11.5px;
	color: var(--muted);
	background: var(--paper-2);
}
</style>
