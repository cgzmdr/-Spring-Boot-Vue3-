<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useLangStore } from "@/stores/lang";

/**
 * 移动端导航抽屉。
 *
 * 为什么单独拆成一个组件：
 * 报头在每个页面的首屏，但抽屉只在**窄视口下点击「菜单」**才会出现。
 * 之前它内联在 AppMasthead 里，导致 el-drawer（连带 overlay、focus-trap、
 * 滚动锁等一整套逻辑与样式）被算进首屏关键路径。拆出来后由 AppMasthead
 * 用 defineAsyncComponent 按需加载：桌面端用户永远不会下载它。
 *
 * 导航数据由父组件传入，避免两处维护同一份信息架构。
 * 样式沿用 common.css 中既有的 `.mobile-nav` 规则（全局样式，非 scoped）。
 */

interface NavChild {
	label: string;
	to: string;
	desc?: string;
}

interface NavGroup {
	key: string;
	label: string;
	/** 一级栏目自身的落地页 */
	to: string;
	children?: NavChild[];
}

const props = defineProps<{
	groups: NavGroup[];
	/** 当前路径，用于高亮 */
	currentPath: string;
}>();

/** v-model：抽屉开关（由父组件持有） */
const open = defineModel<boolean>({ required: true });

const router = useRouter();
const lang = useLangStore();

/** 已展开的一级栏目（key） */
const expanded = ref<string | null>(null);

/** 当前路由是否落在某个一级栏目下（含其所有二级路径） */
function groupActive(g: NavGroup): boolean {
	if (g.to === "/") return props.currentPath === "/";
	const paths = [g.to, ...(g.children || []).map((c) => c.to)];
	return paths.some((p) => {
		const base = p.split("?")[0];
		if (base === "/") return props.currentPath === "/";
		return props.currentPath === base || props.currentPath.startsWith(base + "/");
	});
}

/** 二级条目是否处于当前路由 */
function childActive(c: NavChild): boolean {
	const base = c.to.split("?")[0];
	return props.currentPath === base || props.currentPath.startsWith(base + "/");
}

function toggleGroup(key: string) {
	expanded.value = expanded.value === key ? null : key;
}

function goSearch() {
	open.value = false;
	router.push("/search");
}
</script>

<template>
	<el-drawer
		v-model="open"
		direction="rtl"
		size="260px"
		:with-header="false"
	>
		<div class="mobile-nav">
			<template
				v-for="g in groups"
				:key="g.key"
			>
				<!-- 一级：无下级则直接跳转；有下级则可展开 -->
				<router-link
					v-if="!g.children?.length"
					:to="g.to"
					:class="{ active: groupActive(g) }"
					@click="open = false"
				>
					{{ g.label }}
				</router-link>
				<template v-else>
					<button
						class="mn-group"
						:class="{ active: groupActive(g), open: expanded === g.key }"
						@click="toggleGroup(g.key)"
					>
						<span>{{ g.label }}</span>
						<span class="mn-caret">{{ expanded === g.key ? "−" : "+" }}</span>
					</button>
					<div
						v-if="expanded === g.key"
						class="mn-children"
					>
						<router-link
							v-for="c in g.children"
							:key="c.to"
							:to="c.to"
							:class="{ active: childActive(c) }"
							@click="open = false"
						>
							{{ c.label }}
						</router-link>
					</div>
				</template>
			</template>
			<a @click="goSearch">{{ lang.t("search") }}</a>
		</div>
	</el-drawer>
</template>
