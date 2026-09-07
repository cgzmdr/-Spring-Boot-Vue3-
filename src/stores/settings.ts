import { defineStore } from "pinia";
import { ref } from "vue";

const STORAGE_KEY = "cend_font_size";

export type FontSize = "small" | "normal" | "large" | "xlarge";

export interface FontSizeOption {
	value: FontSize;
	label: string;
	/** 页面缩放系数（等效字号调整，Chromium 支持 html zoom） */
	scale: number;
}

export const FONT_SIZE_OPTIONS: FontSizeOption[] = [
	{ value: "small", label: "小", scale: 0.9 },
	{ value: "normal", label: "标准", scale: 1 },
	{ value: "large", label: "大", scale: 1.1 },
	{ value: "xlarge", label: "特大", scale: 1.22 },
];

export const useSettingsStore = defineStore("settings", () => {
	const fontSize = ref<FontSize>(
		(localStorage.getItem(STORAGE_KEY) as FontSize) || "normal",
	);

	/** 将字号设置应用到页面（html zoom，等效整体缩放） */
	function apply() {
		const opt =
			FONT_SIZE_OPTIONS.find((o) => o.value === fontSize.value) ||
			FONT_SIZE_OPTIONS[1];
		(document.documentElement.style as unknown as { zoom?: string }).zoom =
			String(opt.scale);
	}

	function setFontSize(v: FontSize) {
		fontSize.value = v;
		localStorage.setItem(STORAGE_KEY, v);
		apply();
	}

	return { fontSize, setFontSize, apply };
});
