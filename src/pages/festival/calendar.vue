<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import PageHead from "@/components/PageHead.vue";
import { festivalApi } from "@/api/modules";
import type { CalendarFestival, FestivalCalendar } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 节日日历
 *
 * 后端已把 159/192 条「农历 X 月 X 日」的节日换算为当年公历日期
 * （/festivals/calendar），本页只负责呈现：
 * · 月历网格：按公历月排布，标注节日与农历换算来源
 * · 今日 / 近期：解决「今天/这周有什么节日」的高频诉求
 * · 来源标记：农历换算与「仅精确到月」的估算值分开提示，避免误导
 */

const lang = useLangStore();

const cal = ref<FestivalCalendar | null>(null);
const loading = ref(true);
const errored = ref(false);
const activeMonth = ref<number>(new Date().getMonth() + 1);
const year = ref<number>(new Date().getFullYear());

/** 可切换的年份（当前年 ±3） */
const YEARS = computed(() => {
	const now = new Date().getFullYear();
	return Array.from({ length: 7 }, (_, i) => now - 3 + i);
});

const MONTH_LABELS = ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"];

async function load() {
	loading.value = true;
	errored.value = false;
	try {
		cal.value = await festivalApi.calendar(year.value);
	} catch {
		errored.value = true;
		cal.value = null;
	} finally {
		loading.value = false;
	}
}

onMounted(load);

function pickYear(y: number) {
	year.value = y;
	load();
}

/** 当前选中月份的数据 */
const monthData = computed(() => cal.value?.months.find((m) => m.month === activeMonth.value) || null);

/** 某月的节日按「日」索引，便于在月历网格中定位 */
const dayMap = computed(() => {
	const map = new Map<number, CalendarFestival[]>();
	for (const f of monthData.value?.festivals || []) {
		const list = map.get(f.day) || [];
		list.push(f);
		map.set(f.day, list);
	}
	return map;
});

/** 该月 1 号是周几（用于月历网格前置空格；周一为一周起点） */
const firstWeekday = computed(() => {
	if (!cal.value) return 0;
	const d = new Date(cal.value.year, activeMonth.value - 1, 1);
	return (d.getDay() + 6) % 7;
});

/** 该月天数 */
const daysInMonth = computed(() => {
	if (!cal.value) return 30;
	return new Date(cal.value.year, activeMonth.value, 0).getDate();
});

const WEEKDAYS = ["一", "二", "三", "四", "五", "六", "日"];

/** 今日（仅当查看当前年时高亮） */
const todayDay = computed(() => {
	if (!cal.value || cal.value.year !== new Date().getFullYear()) return -1;
	if (activeMonth.value !== new Date().getMonth() + 1) return -1;
	return new Date().getDate();
});

const totalInYear = computed(() =>
	(cal.value?.months || []).reduce((s, m) => s + m.count, 0),
);

function typeLabel(t: string) {
	return (
		{ traditional: lang.pick("传统", "Traditional"), religious: lang.pick("宗教", "Religious"), agricultural: lang.pick("农事", "Agricultural") }[t] ||
		t
	);
}
</script>

<template>
	<div class="container">
		<div class="crumb">
			<router-link to="/">{{ lang.pick("首页", "Home") }}</router-link> ›
			<router-link to="/festival">{{ lang.pick("节日", "Festivals") }}</router-link> ›
			<span>{{ lang.pick("日历", "Calendar") }}</span>
		</div>

		<PageHead
			:kicker="lang.pick('CALENDAR · 节日日历', 'CALENDAR')"
			:title="lang.pick('节日日历', 'Festival Calendar')"
			:dek="
				lang.pick(
					'许多民族节日的日期依农历而定。这里把「农历正月初一」这样的记载换算成公历日期，按月排布，方便按时间浏览一年中的欢庆时刻。',
					'Many festivals follow the lunar calendar. Lunar dates are converted to solar dates so you can browse the year month by month.',
				)
			"
		/>

		<!-- 年份切换 -->
		<div class="filter">
			<div class="row">
				<span class="label">{{ lang.pick("年份", "Year") }}</span>
				<button
					v-for="y in YEARS"
					:key="y"
					class="chip"
					:class="{ active: year === y }"
					@click="pickYear(y)"
				>
					{{ y }}
				</button>
			</div>
		</div>

		<el-skeleton
			v-if="loading"
			:rows="8"
			animated
		/>

		<div
			v-else-if="errored"
			class="ghost-block"
			style="text-align: center"
		>
			<el-empty :description="lang.pick('节日日历加载失败', 'Failed to load calendar')">
				<el-button
					type="primary"
					@click="load"
					>{{ lang.pick("重新加载", "Retry") }}</el-button
				>
			</el-empty>
		</div>

		<template v-else-if="cal">
			<!-- 今日 / 近期节日 -->
			<section class="today-panel">
				<div class="today-block">
					<div class="tp-head">{{ lang.pick("今日节日", "Today") }}</div>
					<template v-if="cal.today">
						<router-link
							class="today-main"
							:to="`/festival/${cal.today.id}`"
						>
							<span class="tm-name">{{ lang.pick(cal.today.name, cal.today.nameEn || cal.today.name) }}</span>
							<span class="tm-meta">{{ cal.today.ethnicGroupName }} · {{ cal.today.lunarDate || cal.today.date }}</span>
						</router-link>
					</template>
					<p
						v-else
						class="tp-empty"
					>
						{{ lang.pick("今日没有收录的节日", "No festival recorded for today") }}
					</p>
				</div>

				<div class="upcoming-block">
					<div class="tp-head">
						{{ lang.pick("未来 30 天", "Next 30 days") }}
						<span class="tp-count">{{ cal.upcoming.length }}</span>
					</div>
					<ul
						v-if="cal.upcoming.length"
						class="up-list"
					>
						<li
							v-for="f in cal.upcoming.slice(0, 6)"
							:key="f.id"
						>
							<router-link :to="`/festival/${f.id}`">
								<span class="up-date">{{ f.date.slice(5) }}</span>
								<span class="up-name">{{ f.name }}</span>
								<span class="up-eth">{{ f.ethnicGroupName }}</span>
								<span class="up-days">
									{{ f.daysFromToday === 0 ? lang.pick("今天", "today") : `D+${f.daysFromToday}` }}
								</span>
							</router-link>
						</li>
					</ul>
					<p
						v-else
						class="tp-empty"
					>
						{{ lang.pick("未来 30 天暂无收录节日", "Nothing scheduled in the next 30 days") }}
					</p>
				</div>
			</section>

			<!-- 月份切换 -->
			<div class="month-tabs">
				<button
					v-for="m in cal.months"
					:key="m.month"
					class="month-tab"
					:class="{ active: activeMonth === m.month, empty: m.count === 0 }"
					@click="activeMonth = m.month"
				>
					<span class="mt-name">{{ MONTH_LABELS[m.month - 1] }}</span>
					<span class="mt-count">{{ m.count }}</span>
				</button>
			</div>

			<div class="cal-layout">
				<!-- 月历网格 -->
				<div class="cal-grid-wrap">
					<div class="grid-head">
						<h3>
							{{ cal.year }} · {{ MONTH_LABELS[activeMonth - 1] }}
							<span class="gh-count">
								{{ monthData?.count || 0 }} {{ lang.pick("个节日", "festivals") }}
							</span>
						</h3>
					</div>
					<div class="dow-row">
						<span
							v-for="w in WEEKDAYS"
							:key="w"
							class="dow"
							>{{ w }}</span
						>
					</div>
					<div class="cal-grid">
						<span
							v-for="n in firstWeekday"
							:key="`pad-${n}`"
							class="cell pad"
						/>
						<div
							v-for="d in daysInMonth"
							:key="d"
							class="cell"
							:class="{ today: d === todayDay, has: dayMap.has(d) }"
						>
							<span class="day-num">{{ d }}</span>
							<template v-if="dayMap.has(d)">
								<router-link
									v-for="f in dayMap.get(d)"
									:key="f.id"
									class="day-fest"
									:class="`src-${f.dateSource}`"
									:to="`/festival/${f.id}`"
									:title="`${f.name} · ${f.ethnicGroupName} · ${f.lunarDate || ''}`"
								>
									{{ f.name }}
								</router-link>
							</template>
						</div>
					</div>
					<p class="grid-note">
						{{
							lang.pick(
								"深色为农历换算得出的日期；浅色（虚线）表示原文仅精确到月份，按该月十五日估算。",
								"Solid entries are converted lunar dates; dashed entries are month-only records estimated at mid-month.",
							)
						}}
					</p>
				</div>

				<!-- 当月节日列表 -->
				<aside class="cal-side">
					<div class="panel-head">
						<h3>{{ MONTH_LABELS[activeMonth - 1] }}{{ lang.pick("节日", " festivals") }}</h3>
					</div>
					<template v-if="monthData?.festivals.length">
						<ol class="side-list">
							<li
								v-for="f in monthData.festivals"
								:key="f.id"
							>
								<router-link :to="`/festival/${f.id}`">
									<div class="sl-top">
										<span class="sl-day">{{ f.day }}</span>
										<span class="sl-name">{{ f.name }}</span>
										<span class="sl-type">{{ typeLabel(f.type) }}</span>
									</div>
									<div class="sl-meta">
										{{ f.ethnicGroupName }}
										<span v-if="f.lunarDate"> · {{ f.lunarDate }}</span>
									</div>
									<div
										v-if="f.dateSource === 'approx'"
										class="sl-flag"
									>
										{{ lang.pick("日期为估算（原文仅记月份）", "Estimated date (month-only record)") }}
									</div>
								</router-link>
							</li>
						</ol>
					</template>
					<el-empty
						v-else
						:description="lang.pick('该月暂无收录节日', 'No festivals recorded this month')"
						:image-size="70"
					/>
				</aside>
			</div>

			<p class="year-summary">
				{{
					lang.pick(
						`${cal.year} 年共收录 ${totalInYear} 个节日（另有部分节日依伊斯兰历、傣历等，无法换算为公历，未计入日历）。`,
						`${totalInYear} festivals placed on the ${cal.year} calendar (festivals on other calendars are not converted).`,
					)
				}}
			</p>
		</template>
	</div>
</template>

<style scoped>
/* ---- 今日 / 近期 ---- */
.today-panel {
	display: grid;
	grid-template-columns: minmax(0, 1fr) minmax(0, 1.3fr);
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin-bottom: 28px;
}
.today-block,
.upcoming-block {
	background: var(--paper);
	padding: 20px 24px;
}
.tp-head {
	font-size: 11px;
	letter-spacing: 0.16em;
	color: var(--muted);
	text-transform: uppercase;
	margin-bottom: 12px;
	display: flex;
	align-items: center;
	gap: 8px;
}
.tp-count {
	background: var(--accent);
	color: #fff;
	font-size: 10px;
	padding: 1px 6px;
	letter-spacing: 0;
}
.today-main {
	display: block;
}
.today-main .tm-name {
	display: block;
	font-family: var(--serif);
	font-size: 25px;
	color: var(--accent);
	letter-spacing: 0.03em;
}
.today-main .tm-meta {
	display: block;
	font-size: 13px;
	color: var(--muted);
	margin-top: 6px;
}
.today-main:hover .tm-name {
	opacity: 0.82;
}
.tp-empty {
	font-size: 13.5px;
	color: var(--muted);
}
.up-list {
	list-style: none;
}
.up-list li + li {
	border-top: 1px dashed var(--line);
}
.up-list a {
	display: grid;
	grid-template-columns: 46px minmax(0, 1fr) auto auto;
	align-items: baseline;
	gap: 10px;
	padding: 8px 0;
	font-size: 13.5px;
}
.up-list a:hover .up-name {
	color: var(--accent);
}
.up-date {
	font-family: var(--serif);
	color: var(--accent);
	font-variant-numeric: tabular-nums;
}
.up-name {
	font-family: var(--serif);
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	transition: color var(--dur-fast) ease;
}
.up-eth {
	font-size: 12px;
	color: var(--muted);
	white-space: nowrap;
}
.up-days {
	font-size: 11px;
	letter-spacing: 0.06em;
	color: var(--muted);
	text-transform: uppercase;
	white-space: nowrap;
}

/* ---- 月份切换 ---- */
.month-tabs {
	display: flex;
	flex-wrap: wrap;
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
	margin-bottom: 24px;
}
.month-tab {
	flex: 1 1 0;
	min-width: 74px;
	background: var(--paper);
	border: none;
	padding: 11px 6px 9px;
	cursor: pointer;
	font-family: var(--font-sans);
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 3px;
	transition: background var(--dur-fast) ease;
}
.month-tab:hover {
	background: var(--paper-2);
}
.month-tab.active {
	background: var(--ink);
}
.month-tab.active .mt-name,
.month-tab.active .mt-count {
	color: #fff;
}
.month-tab.empty .mt-name {
	color: #b9b7ac;
}
.mt-name {
	font-family: var(--serif);
	font-size: 14px;
	letter-spacing: 0.06em;
	color: var(--ink);
}
.mt-count {
	font-size: 10.5px;
	color: var(--muted);
	font-variant-numeric: tabular-nums;
}

/* ---- 月历 + 侧栏 ---- */
.cal-layout {
	display: grid;
	grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
	gap: 1px;
	background: var(--line);
	border: 1px solid var(--line);
}
.cal-grid-wrap {
	background: var(--paper);
	padding: 22px 24px 18px;
	min-width: 0;
}
.grid-head h3 {
	font-family: var(--serif);
	font-size: 20px;
	letter-spacing: 0.06em;
	display: flex;
	align-items: baseline;
	gap: 12px;
	margin-bottom: 16px;
}
.gh-count {
	font-family: var(--font-sans);
	font-size: 11.5px;
	color: var(--muted);
	letter-spacing: 0.08em;
}
.dow-row,
.cal-grid {
	display: grid;
	grid-template-columns: repeat(7, minmax(0, 1fr));
	gap: 4px;
}
.dow {
	text-align: center;
	font-size: 11.5px;
	color: var(--muted);
	padding-bottom: 7px;
	border-bottom: 1px solid var(--line);
	margin-bottom: 7px;
}
.cell {
	min-height: 62px;
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 4px 5px 5px;
	display: flex;
	flex-direction: column;
	gap: 3px;
	overflow: hidden;
}
.cell.pad {
	border: none;
	background: transparent;
}
.cell.has {
	background: color-mix(in srgb, var(--accent) 4%, var(--paper));
}
.cell.today {
	border-color: var(--accent);
	border-width: 2px;
	padding: 3px 4px 4px;
}
.day-num {
	font-family: var(--serif);
	font-size: 12px;
	color: var(--muted);
	font-variant-numeric: tabular-nums;
	line-height: 1.2;
}
.cell.has .day-num {
	color: var(--ink);
}
.day-fest {
	display: block;
	font-size: 11px;
	line-height: 1.35;
	color: #fff;
	background: var(--accent);
	padding: 2px 5px;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
	transition: opacity var(--dur-fast) ease;
}
.day-fest:hover {
	opacity: 0.85;
}
/* 仅精确到月的估算日期：虚线边框弱化，避免与确切日期混淆 */
.day-fest.src-approx {
	background: transparent;
	color: var(--accent);
	border: 1px dashed color-mix(in srgb, var(--accent) 60%, transparent);
}
.grid-note {
	font-size: 11.5px;
	color: var(--muted);
	line-height: 1.7;
	margin-top: 14px;
}

/* 侧栏 */
.cal-side {
	background: var(--paper);
	padding: 22px 24px;
	min-width: 0;
}
.cal-side .panel-head {
	border-bottom: 1px solid var(--line);
	padding-bottom: 12px;
	margin-bottom: 6px;
}
.cal-side .panel-head h3 {
	font-family: var(--serif);
	font-size: 18px;
	letter-spacing: 0.05em;
}
.side-list {
	list-style: none;
}
.side-list li {
	border-bottom: 1px dashed var(--line);
}
.side-list li:last-child {
	border-bottom: none;
}
.side-list a {
	display: block;
	padding: 11px 0;
}
.sl-top {
	display: flex;
	align-items: baseline;
	gap: 8px;
}
.sl-day {
	font-family: var(--serif);
	font-size: 17px;
	font-weight: 700;
	color: var(--accent);
	font-variant-numeric: tabular-nums;
	min-width: 24px;
}
.sl-name {
	font-family: var(--serif);
	font-size: 15.5px;
	letter-spacing: 0.02em;
	transition: color var(--dur-fast) ease;
	flex: 1;
	min-width: 0;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.side-list a:hover .sl-name {
	color: var(--accent);
}
.sl-type {
	font-size: 10.5px;
	letter-spacing: 0.08em;
	color: var(--muted);
	border: 1px solid var(--line);
	padding: 1px 6px;
	white-space: nowrap;
}
.sl-meta {
	font-size: 12px;
	color: var(--muted);
	margin-top: 4px;
	padding-left: 32px;
}
.sl-flag {
	font-size: 10.5px;
	color: var(--accent);
	margin-top: 5px;
	padding-left: 32px;
	opacity: 0.85;
}
.year-summary {
	font-size: 12.5px;
	color: var(--muted);
	line-height: 1.8;
	margin: 22px 0 8px;
	padding-left: 12px;
	border-left: 2px solid var(--line);
}
@media (max-width: 900px) {
	.today-panel,
	.cal-layout {
		grid-template-columns: minmax(0, 1fr);
	}
	.month-tab {
		min-width: 62px;
	}
	.cell {
		min-height: 52px;
	}
	.day-fest {
		font-size: 10px;
		padding: 1px 3px;
	}
	.sl-meta,
	.sl-flag {
		padding-left: 0;
	}
}
</style>
