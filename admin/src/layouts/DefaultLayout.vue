<template>
	<el-container class="app-layout">
		<el-aside
			:width="isCollapse ? '64px' : '220px'"
			class="app-aside"
		>
			<div class="logo">
				<img
					src="/logo.svg"
					alt="logo"
					class="logo-icon"
				/>
				<span
					v-show="!isCollapse"
					class="logo-text"
					>56 民族 OA</span
				>
			</div>
			<el-menu
				:default-active="activeMenu"
				:default-openeds="defaultOpeneds"
				:collapse="isCollapse"
				:collapse-transition="false"
				router
				background-color="#1f2937"
				text-color="#cbd5e1"
				active-text-color="#ffffff"
			>
				<!-- 按业务域分组：一级为分组标题，二级为具体功能 -->
				<el-sub-menu
					v-for="g in menus"
					:key="g.key"
					:index="g.key"
				>
					<template #title>
						<el-icon><component :is="g.icon" /></el-icon>
						<span>{{ g.title }}</span>
					</template>
					<el-menu-item
						v-for="item in g.children"
						:key="item.path"
						:index="item.path"
					>
						<el-icon><component :is="item.icon" /></el-icon>
						<template #title>{{ item.title }}</template>
					</el-menu-item>
				</el-sub-menu>
			</el-menu>
		</el-aside>

		<el-container>
			<el-header class="app-header">
				<div class="header-left">
					<el-icon
						class="collapse-btn"
						@click="isCollapse = !isCollapse"
					>
						<Expand v-if="isCollapse" />
						<Fold v-else />
					</el-icon>
					<el-breadcrumb separator="/">
						<el-breadcrumb-item :to="{ path: '/dashboard' }"
							>首页</el-breadcrumb-item
						>
						<el-breadcrumb-item
							v-if="route.meta.title && route.meta.title !== '仪表盘'"
						>
							{{ route.meta.title }}
						</el-breadcrumb-item>
					</el-breadcrumb>
				</div>
				<div class="header-right">
					<el-dropdown @command="handleCommand">
						<span class="user-info">
							<el-avatar
								:size="30"
								:src="userStore.avatar"
							>
								{{ userStore.nickname?.[0] || "A" }}
							</el-avatar>
							<span class="user-name">{{
								userStore.nickname || "管理员"
							}}</span>
							<el-icon><ArrowDown /></el-icon>
						</span>
						<template #dropdown>
							<el-dropdown-menu>
								<el-dropdown-item disabled>{{
									userStore.roles.map((r) => ROLE_LABEL[r] || r).join(", ")
								}}</el-dropdown-item>
								<el-dropdown-item>
									<el-icon><User /></el-icon>个人中心
								</el-dropdown-item>
								<el-dropdown-item
									divided
									command="logout"
								>
									<el-icon><SwitchButton /></el-icon>退出登录
								</el-dropdown-item>
							</el-dropdown-menu>
						</template>
					</el-dropdown>
				</div>
			</el-header>

			<el-main class="app-main">
				<router-view />
			</el-main>
		</el-container>
	</el-container>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/user";
import { MENU_GROUPS, ROLE_LABEL } from "@/constants";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapse = ref(false);

/**
 * 可见菜单：分组与分组内条目都按角色过滤。
 * 过滤后没有可见条目的分组整体隐藏，避免出现空分组。
 */
const menus = computed(() =>
	MENU_GROUPS.map((g) => ({
		...g,
		children: g.children.filter(
			(item) => item.roles.length === 0 || item.roles.some((r) => userStore.roles.includes(r)),
		),
	})).filter(
		(g) =>
			g.children.length > 0 &&
			(g.roles.length === 0 || g.roles.some((r) => userStore.roles.includes(r))),
	),
);

/** 默认展开当前路由所属的分组，其余保持收起 */
const defaultOpeneds = computed(() => {
	const current = menus.value.find((g) =>
		g.children.some((item) => route.path.startsWith(item.path)),
	);
	return current ? [current.key] : [menus.value[0]?.key].filter(Boolean);
});

const activeMenu = computed(() => route.path);

async function handleCommand(command: string) {
	if (command === "logout") {
		await ElMessageBox.confirm("确定退出登录吗？", "提示", { type: "warning" });
		await userStore.logout();
		router.replace("/login");
	}
}
const goBack = () => {
	console.log("go back");
};
</script>

<style scoped lang="scss">
.app-layout {
	height: 100vh;

	.app-aside {
		background-color: #1f2937;
		transition: width 0.2s;
		overflow-x: hidden;

		.logo {
			height: 56px;
			display: flex;
			align-items: center;
			gap: 10px;
			padding: 0 16px;
			color: #fff;
			border-bottom: 1px solid rgba(255, 255, 255, 0.08);

			.logo-icon {
				width: 30px;
				height: 30px;
				border-radius: 8px;
				flex-shrink: 0;
				display: block;
			}

			.logo-text {
				font-size: 15px;
				font-weight: 600;
				white-space: nowrap;
			}
		}

		.el-menu {
			border-right: none;
		}
	}

	.app-header {
		height: 56px;
		background: #fff;
		border-bottom: 1px solid #e5e7eb;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 16px;

		.header-left {
			display: flex;
			align-items: center;
			gap: 14px;

			.collapse-btn {
				font-size: 18px;
				cursor: pointer;
				color: #6b7280;
			}
		}

		.header-right {
			.user-info {
				display: flex;
				align-items: center;
				gap: 8px;
				cursor: pointer;
				color: #374151;
				outline: none;

				.user-name {
					font-size: 14px;
				}
			}
		}
	}

	.app-main {
		background: #f0f2f5;
		padding: 16px;
		overflow-y: auto;
	}
}
</style>
