<script setup lang="ts">
import { computed, ref, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import type { EthnicMapPoint } from "@/api/types";
import { useLangStore } from "@/stores/lang";

/**
 * 民族分布地图（离线 SVG 实现）
 *
 * 设计取舍：
 * · 不引入 ECharts / Leaflet 等地图库，也不依赖任何在线瓦片服务 ——
 *   地图数据（省级轮廓 GeoJSON）作为静态资源随包发布（`public/data/china-provinces.json`），
 *   因此断网、内网、离线答辩环境下同样可用。
 * · 投影用等距圆柱投影（经纬度线性映射）。中国跨约 62° 经度，纬度 18°–54°，
 *   在展示「聚居地分布」这一用途上，线性投影的形变可接受，且实现零依赖、可预测。
 * · 聚居地按经纬度落点，半径按该点位数量聚合（同城多点合并为一个气泡）。
 */

const props = withDefaults(
	defineProps<{
		/** 聚居地点位（来自 /ethnic-groups/map） */
		points: EthnicMapPoint[];
		/** 地图高度 */
		height?: number;
	}>(),
	{ height: 520 },
);

const router = useRouter();
const lang = useLangStore();

/* ------------------------------------------------------------------ 投影 */

/** 视口（SVG 用户坐标）—— 与容器等比，用 viewBox 自适应 */
const VB_W = 1000;
const VB_H = 800;

/**
 * 经纬度范围：略大于中国实际范围，留出边距。
 * 纬度上限含南海诸岛所在的南部海域，下限覆盖黑龙江漠河。
 */
const BBOX = { minLng: 73, maxLng: 136, minLat: 17, maxLat: 54 };

/** 经纬度 → SVG 坐标（等距圆柱投影 + 0.85 纵向压缩，贴近常见中国地图观感） */
const LNG_SPAN = BBOX.maxLng - BBOX.minLng;
const LAT_SPAN = BBOX.maxLat - BBOX.minLat;
const Y_SQUEEZE = 0.85;

function project(lng: number, lat: number): [number, number] {
	const x = ((lng - BBOX.minLng) / LNG_SPAN) * VB_W;
	const y = ((BBOX.maxLat - lat) / LAT_SPAN) * VB_H * Y_SQUEEZE + (VB_H * (1 - Y_SQUEEZE)) / 2;
	return [x, y];
}

/* -------------------------------------------------------------- 省级轮廓 */

interface GeoFeature {
	type: string;
	properties: { name?: string };
	geometry: { type: string; coordinates: unknown };
}

const provinces = ref<GeoFeature[]>([]);
const mapLoading = ref(true);
const mapError = ref(false);

/** GeoJSON 环 → SVG path 的 d 属性 */
function ringToPath(ring: number[][]): string {
	return (
		ring
			.map((p, i) => {
				const [x, y] = project(p[0], p[1]);
				return `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`;
			})
			.join(" ") + " Z"
	);
}

function geometryToPath(geom: GeoFeature["geometry"]): string {
	const coords = geom.coordinates as number[][][] | number[][][][];
	if (geom.type === "Polygon") {
		return (coords as number[][][]).map(ringToPath).join(" ");
	}
	return (coords as number[][][][]).map((poly) => poly.map(ringToPath).join(" ")).join(" ");
}

/** 预计算 path，避免模板里重复投影 */
const provincePaths = computed(() =>
	provinces.value.map((f) => ({
		name: f.properties.name || "",
		d: geometryToPath(f.geometry),
	})),
);

onMounted(async () => {
	try {
		// 静态资源：public/data/china-provinces.json（已抽稀，约 145KB）
		const res = await fetch("/data/china-provinces.json");
		if (!res.ok) throw new Error(String(res.status));
		const gj = (await res.json()) as { features: GeoFeature[] };
		provinces.value = gj.features || [];
	} catch {
		mapError.value = true;
	} finally {
		mapLoading.value = false;
	}
});

/* ---------------------------------------------------------- 聚居地聚合 */

interface Bubble {
	key: string;
	lng: number;
	lat: number;
	x: number;
	y: number;
	/** 该点聚合到的聚居地数量 */
	count: number;
	/** 涉及的民族（去重，按出现顺序） */
	groups: { id: string; name: string; themeColor: string }[];
	provinces: string[];
}

/**
 * 同城多点合并：以「省 + 市」为键聚合，避免同城多个民族的点互相遮挡。
 * 例：昆明市有多个民族的聚居地 → 合并为一个气泡，悬浮列出民族列表。
 */
const bubbles = computed<Bubble[]>(() => {
	const map = new Map<string, Bubble>();
	for (const p of props.points) {
		const key = `${p.province || ""}|${p.city || ""}|${p.longitude.toFixed(2)},${p.latitude.toFixed(2)}`;
		let b = map.get(key);
		if (!b) {
			const [x, y] = project(p.longitude, p.latitude);
			b = {
				key,
				lng: p.longitude,
				lat: p.latitude,
				x,
				y,
				count: 0,
				groups: [],
				provinces: [],
			};
			map.set(key, b);
		}
		b.count += 1;
		if (!b.groups.some((g) => g.id === p.ethnicGroupId)) {
			b.groups.push({
				id: p.ethnicGroupId,
				name: p.ethnicGroupName,
				themeColor: p.themeColor,
			});
		}
		if (p.province && !b.provinces.includes(p.province)) b.provinces.push(p.province);
	}
	return [...map.values()];
});

/** 气泡半径：随该点民族数增长（有上限，避免遮挡） */
function bubbleRadius(b: Bubble): number {
	return Math.min(5 + Math.sqrt(b.groups.length) * 3.4, 15);
}

const hovered = ref<Bubble | null>(null);

/** tooltip 位置（用 SVG 坐标转容器百分比，避免跟随鼠标造成的抖动） */
const tooltipStyle = computed(() => {
	if (!hovered.value) return {};
	const b = hovered.value;
	return {
		left: `${(b.x / VB_W) * 100}%`,
		top: `${(b.y / VB_H) * 100}%`,
	};
});

function goEthnic(id: string) {
	router.push(`/ethnic/${id}`);
}

/** 覆盖的省级行政区数量（用于统计提示） */
const coveredProvinces = computed(() => {
	const set = new Set<string>();
	for (const p of props.points) if (p.province) set.add(p.province);
	return set.size;
});

const groupCount = computed(() => {
	const set = new Set<string>();
	for (const p of props.points) set.add(p.ethnicGroupId);
	return set.size;
});

watch(
	() => props.points,
	() => (hovered.value = null),
);
</script>

<template>
	<div class="ethnic-map">
		<div class="map-stats">
			<span class="stat">
				<b>{{ groupCount }}</b>
				{{ lang.pick("个民族", "groups") }}
			</span>
			<span class="sep">·</span>
			<span class="stat">
				<b>{{ points.length }}</b>
				{{ lang.pick("处聚居地", "settlements") }}
			</span>
			<span class="sep">·</span>
			<span class="stat">
				<b>{{ coveredProvinces }}</b>
				{{ lang.pick("个省级行政区", "provinces") }}
			</span>
		</div>

		<div
			class="map-canvas"
			:style="{ height: `${height}px` }"
		>
			<el-skeleton
				v-if="mapLoading"
				:rows="6"
				animated
			/>
			<el-result
				v-else-if="mapError"
				icon="warning"
				:title="lang.pick('地图数据加载失败', 'Map data unavailable')"
				:sub-title="lang.pick('请检查 /data/china-provinces.json 是否可访问', 'Check that /data/china-provinces.json is reachable')"
			/>

			<svg
				v-else
				class="map-svg"
				:viewBox="`0 0 ${VB_W} ${VB_H}`"
				preserveAspectRatio="xMidYMid meet"
				role="img"
				:aria-label="lang.pick('中国民族分布地图', 'Map of ethnic group settlements in China')"
			>
				<!-- 省级行政区轮廓 -->
				<g class="provinces">
					<path
						v-for="p in provincePaths"
						:key="p.name"
						:d="p.d"
						class="province"
					/>
				</g>

				<!-- 聚居地气泡 -->
				<g class="bubbles">
					<g
						v-for="b in bubbles"
						:key="b.key"
						class="bubble"
						:class="{ active: hovered?.key === b.key }"
						@mouseenter="hovered = b"
						@mouseleave="hovered = null"
					>
						<circle
							:cx="b.x"
							:cy="b.y"
							:r="bubbleRadius(b) + 5"
							class="bubble-halo"
						/>
						<circle
							:cx="b.x"
							:cy="b.y"
							:r="bubbleRadius(b)"
							class="bubble-dot"
							:style="{ fill: b.groups[0]?.themeColor || 'var(--accent)' }"
						/>
						<!-- 气泡内数字：多民族聚居地才显示，单点保持干净 -->
						<text
							v-if="b.groups.length > 1"
							:x="b.x"
							:y="b.y + 3.4"
							class="bubble-count"
						>
							{{ b.groups.length }}
						</text>
					</g>
				</g>
			</svg>

			<!-- 悬浮卡：展示该聚居地的民族，可直接进入民族详情 -->
			<div
				v-if="hovered"
				class="map-tip"
				:style="tooltipStyle"
			>
				<div class="tip-head">
					{{ hovered.provinces.join(" / ") }}
				</div>
				<div class="tip-groups">
					<button
						v-for="g in hovered.groups"
						:key="g.id"
						class="tip-chip"
						:style="{ borderColor: g.themeColor }"
						@click="goEthnic(g.id)"
					>
						{{ g.name }}
					</button>
				</div>
			</div>
		</div>

		<p class="map-note">
			{{
				lang.pick(
					"气泡为一个聚居地（同城多个民族合并显示，数字为民族数）；点击可进入民族详情。地图为示意轮廓，不作为行政区划依据。",
					"Each bubble is a settlement (groups in the same city are merged; the number counts groups). Click to open a group. Boundaries are indicative only.",
				)
			}}
		</p>
	</div>
</template>

<style scoped>
.ethnic-map {
	border: 1px solid var(--line);
	background: var(--paper);
	padding: 18px 20px 14px;
}
.map-stats {
	display: flex;
	align-items: baseline;
	gap: 8px;
	font-size: 12px;
	letter-spacing: 0.08em;
	color: var(--muted);
	text-transform: uppercase;
	margin-bottom: 8px;
}
.map-stats b {
	font-family: var(--serif);
	font-size: 18px;
	color: var(--accent);
	margin-right: 3px;
}
.map-stats .sep {
	color: var(--line);
}
.map-canvas {
	position: relative;
	width: 100%;
}
.map-svg {
	width: 100%;
	height: 100%;
	display: block;
}
.province {
	fill: var(--paper-2);
	stroke: #b9b7ac;
	stroke-width: 0.7;
	vector-effect: non-scaling-stroke;
	transition: fill var(--dur) var(--ease-out);
}
.province:hover {
	fill: color-mix(in srgb, var(--accent) 7%, var(--paper-2));
}
.bubble {
	cursor: pointer;
}
.bubble .bubble-halo {
	fill: transparent;
	transition: fill var(--dur) var(--ease-out);
}
.bubble:hover .bubble-halo,
.bubble.active .bubble-halo {
	fill: color-mix(in srgb, var(--accent) 14%, transparent);
}
.bubble-dot {
	stroke: #fff;
	stroke-width: 1.1;
	opacity: 0.9;
	transition: opacity var(--dur) var(--ease-out);
}
.bubble:hover .bubble-dot,
.bubble.active .bubble-dot {
	opacity: 1;
	stroke-width: 1.6;
}
.bubble-count {
	text-anchor: middle;
	font-size: 8px;
	font-weight: 700;
	fill: #fff;
	pointer-events: none;
	font-family: var(--font-sans);
}
.map-tip {
	position: absolute;
	transform: translate(-50%, calc(-100% - 14px));
	background: var(--ink);
	color: #fff;
	padding: 10px 12px;
	min-width: 120px;
	max-width: 260px;
	pointer-events: auto;
	z-index: 5;
	box-shadow: 0 8px 22px rgba(0, 0, 0, 0.22);
}
.map-tip::after {
	content: "";
	position: absolute;
	left: 50%;
	bottom: -6px;
	margin-left: -6px;
	border: 6px solid transparent;
	border-top-color: var(--ink);
	border-bottom: none;
}
.tip-head {
	font-size: 11px;
	letter-spacing: 0.1em;
	color: rgba(255, 255, 255, 0.72);
	margin-bottom: 7px;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.tip-groups {
	display: flex;
	flex-wrap: wrap;
	gap: 5px;
	max-height: 132px;
	overflow-y: auto;
}
.tip-chip {
	border: 1px solid rgba(255, 255, 255, 0.5);
	background: transparent;
	color: #fff;
	font-size: 12px;
	padding: 2px 8px;
	cursor: pointer;
	font-family: var(--font-sans);
	transition: background var(--dur-fast) ease;
}
.tip-chip:hover {
	background: rgba(255, 255, 255, 0.18);
}
.map-note {
	font-size: 12px;
	line-height: 1.7;
	color: var(--muted);
	margin-top: 10px;
}
@media (max-width: 900px) {
	.ethnic-map {
		padding: 14px 12px 10px;
	}
	.map-stats {
		font-size: 11px;
	}
}
</style>
