<script setup lang="ts">
import { ref, computed } from "vue";
import { ArrowRight } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import { useLangStore } from "@/stores/lang";
import { useAuthStore } from "@/stores/auth";
import { resolveStaticUrl } from "@/utils/format";
import LoginDialog from "./LoginDialog.vue";

const route = useRoute();
const router = useRouter();
const lang = useLangStore();
const auth = useAuthStore();

const loginDialog = ref<InstanceType<typeof LoginDialog> | null>(null);
const drawerOpen = ref(false);

const navItems = computed(() => [
	{ label: lang.t("nav_home"), to: "/", match: "/" },
	{ label: lang.t("nav_ethnic"), to: "/ethnic", match: "/ethnic" },
	{ label: lang.t("nav_festival"), to: "/festival", match: "/festival" },
	{ label: lang.t("nav_art"), to: "/art", match: "/art" },
	{ label: lang.t("nav_about"), to: "/about", match: "/about" },
]);

function isActive(path: string) {
	if (path === "/") return route.path === "/";
	return route.path.startsWith(path);
}

function goSearch() {
	router.push("/search");
}

function openLogin() {
	loginDialog.value?.open();
}

const userInitial = computed(() =>
	auth.user?.nickname ? auth.user.nickname.trim().slice(0, 1) : "",
);
const userAvatar = computed(() => resolveStaticUrl(auth.user?.avatar));
</script>

<template>
	<nav class="mast sticky">
		<div class="container">
			<div class="mast-top">
				<span>{{ lang.t("site_slogan") }}</span>
				<div
					style="width: fit-content; margin: 0 auto 0 1rem"
					v-if="route.name !== 'home'"
				>
					<el-link
						type="danger"
						:icon="ArrowRight"
						@click="
							() => {
								router.back();
							}
						"
						>{{ lang.t("back_prev") }}</el-link
					>
				</div>
				<div class="links">
					<a @click="goSearch">{{ lang.t("search") }}</a>
					<span>·</span>
					<button
						class="lang-toggle"
						@click="lang.toggle()"
					>
						{{ lang.isEn ? "中" : "EN" }}
					</button>
					<template v-if="auth.isLoggedIn">
						<span>·</span>
						<router-link
							to="/profile"
							class="user-chip"
							:title="auth.user?.nickname || '个人中心'"
						>
							<img
								v-if="userAvatar"
								:src="userAvatar"
								class="chip-avatar"
								alt=""
							/>
							<template v-else>{{ userInitial }}</template>
						</router-link>
					</template>
					<template v-else>
						<span>·</span>
						<a @click="openLogin">{{ lang.t("login") }}</a>
					</template>
				</div>
			</div>

			<div class="mast-title">
				<h1 @click="router.push('/')">走进多彩 56 民族</h1>
				<div class="sub">{{ lang.t("site_sub") }}</div>
			</div>

			<div class="mast-nav">
				<router-link
					v-for="item in navItems"
					:key="item.to"
					:to="item.to"
					:class="{ active: isActive(item.match) }"
				>
					{{ item.label }}
				</router-link>
			</div>

			<div
				class="mast-burger"
				@click="drawerOpen = true"
			>
				<svg
					width="20"
					height="20"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="1.8"
				>
					<path
						d="M4 7h16M4 12h16M4 17h16"
						stroke-linecap="round"
					/>
				</svg>
				<span>菜单</span>
			</div>
		</div>
	</nav>

	<el-drawer
		v-model="drawerOpen"
		direction="rtl"
		size="260px"
		:with-header="false"
	>
		<div class="mobile-nav">
			<router-link
				v-for="item in navItems"
				:key="item.to"
				:to="item.to"
				:class="{ active: isActive(item.match) }"
				@click="drawerOpen = false"
			>
				{{ item.label }}
			</router-link>
			<a @click="goSearch">{{ lang.t("search") }}</a>
		</div>
	</el-drawer>

	<LoginDialog ref="loginDialog" />
</template>

<style scoped>
.user-chip {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 22px;
	height: 22px;
	border-radius: 50%;
	background: var(--ink);
	color: #fff;
	font-size: 12px;
	cursor: pointer;
	text-transform: uppercase;
	text-decoration: none;
	overflow: hidden;
}
.chip-avatar {
	width: 100%;
	height: 100%;
	object-fit: cover;
	display: block;
}
.mast-top .links a {
	cursor: pointer;
}
</style>
