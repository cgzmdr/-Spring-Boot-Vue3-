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
		nav_culture: "文化",
		nav_ethnic_all: "民族概览",
		nav_ethnic_all_desc: "56 个民族的卡片墙与筛选",
		nav_languages: "语文",
		nav_languages_desc: "语系、语言与传统文字",
		nav_costume: "服饰",
		nav_costume_desc: "各民族的衣着与工艺",
		nav_dwelling: "民居",
		nav_dwelling_desc: "干栏式、穹庐式等建筑形制",
		nav_map: "分布地图",
		nav_map_desc: "聚居地地理分布",
		nav_autonomous: "自治地方",
		nav_autonomous_desc: "自治区 / 自治州 / 自治县",
		nav_festival: "节日",
		nav_festival_all: "节日列表",
		nav_festival_all_desc: "按类型与民族浏览",
		nav_calendar: "日历",
		nav_calendar_desc: "农历换算与今日节日",
		nav_art: "艺术",
		nav_art_desc: "音乐、舞蹈、戏剧与手工艺",
		nav_heritage: "非遗",
		nav_heritage_desc: "非物质文化遗产名录",
		nav_persons: "人物",
		nav_persons_desc: "传承人与文化名家",
		nav_sports: "体育",
		nav_sports_desc: "民族传统体育项目",
		nav_discussion: "讨论",
		nav_interests: "兴趣推荐",
		nav_interests_desc: "选择兴趣，获得个性化推荐",
		nav_about: "关于",
		/** 英文正文为机器翻译时的提示（方向 C-3） */
		mt_notice: "本页英文由机器翻译生成，仅供参考",
		mt_reviewed: "本页英文已经人工校对",
		read_zh: "阅读中文原文",
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
		nav_culture: "Culture",
		nav_ethnic_all: "Overview",
		nav_ethnic_all_desc: "Browse all 56 groups",
		nav_languages: "Languages",
		nav_languages_desc: "Families, languages, scripts",
		nav_costume: "Costumes",
		nav_costume_desc: "Dress and textile crafts",
		nav_dwelling: "Dwellings",
		nav_dwelling_desc: "Stilt, yurt and other forms",
		nav_map: "Map",
		nav_map_desc: "Geographic distribution",
		nav_autonomous: "Regions",
		nav_autonomous_desc: "Regions, prefectures, counties",
		nav_festival: "Festival",
		nav_festival_all: "All festivals",
		nav_festival_all_desc: "Browse by type and group",
		nav_calendar: "Calendar",
		nav_calendar_desc: "Lunar conversion & today",
		nav_art: "Art",
		nav_art_desc: "Music, dance, drama, crafts",
		nav_heritage: "Heritage",
		nav_heritage_desc: "Intangible cultural heritage",
		nav_persons: "People",
		nav_persons_desc: "Inheritors and masters",
		nav_sports: "Sports",
		nav_sports_desc: "Traditional ethnic sports",
		nav_discussion: "Community",
		nav_interests: "For You",
		nav_interests_desc: "Pick interests for personalized picks",
		nav_about: "About",
		/** 英文正文为机器翻译时的提示（方向 C-3） */
		mt_notice: "English text on this page is machine-translated and provided for reference only.",
		mt_reviewed: "The English text on this page has been reviewed.",
		read_zh: "Read the original Chinese",
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

	/**
	 * 启动时把初始语言同步到 <html lang>。
	 *
	 * 之前只在 apply()（切换语言）里同步，刷新页面后 store 虽从 storage 恢复了语言，
	 * 但 documentElement.lang 一直是初始的 zh-CN——既影响无障碍朗读与搜索引擎判断语种，
	 * 也让「当前是否为英文」在不同地方出现不一致的判据。
	 */
	function syncDocumentLang() {
		if (typeof document !== "undefined") {
			document.documentElement.lang = lang.value === "en" ? "en" : "zh-CN";
		}
	}
	syncDocumentLang();

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
