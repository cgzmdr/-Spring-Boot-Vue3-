import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { authApi } from "@/api/modules";
import { useAuthStore } from "@/stores/auth";

const STORAGE_KEY = "cend_lang";

export type Lang = "zh" | "en";

/** 轻量文案字典（覆盖导航 / 页脚 / 常用 UI；正文内容双语由后端 *En 字段承担） */
const messages: Record<Lang, Record<string, string>> = {
	zh: {
		site_slogan: "中华民族 · 多元一体",
		site_sub: "The World of 56 Ethnic Groups",
		search: "搜索",
		nav_home: "首页",
		nav_ethnic: "民族",
		nav_festival: "节日",
		nav_art: "艺术",
		nav_about: "关于",
		login: "登录",
		register: "注册",
		logout: "退出登录",
		footer_about: "关于我们",
		footer_source: "数据来源",
		footer_copyright: "版权声明",
		footer_contact: "联系我们",
		footer_rights: "© 2026 走进多彩 56 个民族世界",
		footer_brand: "走进多彩 56 民族世界",
		explore: "开始探索",
		learn_56: "了解 56 民族",
		section_all_ethnic: "五十六个民族",
		section_features: "精选专题",
		section_culture: "文化之窗",
		section_festivals: "节日庆典",
		section_arts: "传统艺术",
		result_count: "共 {n} 条结果",
		no_more: "暂无更多内容",
		empty: "暂无数据",
		loading: "加载中…",
		like: "点赞",
		favorite: "收藏",
		share: "分享",
		back_home: "返回首页",
		back_prev: "返回上一页",
		page_not_found: "页面走丢了",
		prev: "上一页",
		next: "下一页",
	},
	en: {
		site_slogan: "One Family, 56 Ethnic Groups",
		site_sub: "The World of 56 Ethnic Groups",
		search: "Search",
		nav_home: "Home",
		nav_ethnic: "Ethnic",
		nav_festival: "Festival",
		nav_art: "Art",
		nav_about: "About",
		login: "Sign in",
		register: "Sign up",
		logout: "Sign out",
		footer_about: "About",
		footer_source: "Data Sources",
		footer_copyright: "Copyright",
		footer_contact: "Contact",
		footer_rights: "© 2026 The World of 56 Ethnic Groups",
		footer_brand: "The World of 56 Ethnic Groups",
		explore: "Explore",
		learn_56: "Meet 56 Groups",
		section_all_ethnic: "The 56 Ethnic Groups",
		section_features: "Featured Topics",
		section_culture: "Culture Window",
		section_festivals: "Festivals",
		section_arts: "Arts",
		result_count: "{n} results",
		no_more: "No more content",
		empty: "No data",
		loading: "Loading…",
		like: "Like",
		favorite: "Favorite",
		share: "Share",
		back_home: "Back to Home",
		back_prev: "Back",
		page_not_found: "Page not found",
		prev: "Prev",
		next: "Next",
	},
};

export const useLangStore = defineStore("lang", () => {
	const lang = ref<Lang>((localStorage.getItem(STORAGE_KEY) as Lang) || "zh");

	const isEn = computed(() => lang.value === "en");

	function apply(l: Lang, persistToServer: boolean) {
		lang.value = l;
		localStorage.setItem(STORAGE_KEY, l);
		document.documentElement.lang = l === "en" ? "en" : "zh-CN";
		// 登录后同步到用户表（需求：i18n 偏好随用户持久化，换设备登录自动恢复）
		if (persistToServer) {
			const auth = useAuthStore();
			if (auth.isLoggedIn && auth.user?.id) {
				authApi.update(auth.user.id, { lang: l }).catch(() => {
					/* 静默失败，本地优先 */
				});
			}
		}
	}

	function setLang(l: Lang) {
		apply(l, true);
	}

	/** 从用户资料恢复语言（登录时调用，不回写避免回声请求） */
	function applyFromUser(l?: string) {
		if (l === "zh" || l === "en") apply(l, false);
	}

	function toggle() {
		setLang(isEn.value ? "zh" : "en");
	}

	/** 翻译静态文案 */
	function t(key: string, vars?: Record<string, string | number>) {
		let s = messages[lang.value][key] ?? messages.zh[key] ?? key;
		if (vars) {
			for (const [k, v] of Object.entries(vars)) {
				s = s.replace(`{${k}}`, String(v));
			}
		}
		return s;
	}

	/** 双语字段选择：lang=en 且有英文值时返回英文 */
	function pick(zh: string, en?: string | null) {
		if (lang.value === "en" && en) return en;
		return zh;
	}

	return { lang, isEn, setLang, applyFromUser, toggle, t, pick };
});
