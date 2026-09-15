/**
 * 机器翻译标注文案：按「实际产出译文的提供方」展示。
 *
 * 站点默认「词表优先 + AI 兜底」，同一次浏览里不同内容可能由不同通道产出，
 * 因此标注必须跟着返回的 provider 走，不能只用配置里的主通道文案。
 */
const PROVIDER_LABELS: Record<string, string> = {
	glossary: "机器翻译 · 词表兜底",
	"spring-ai": "机器翻译 · AI 大模型",
	libretranslate: "机器翻译 · LibreTranslate",
	ollama: "机器翻译 · 本地大模型",
	none: "机器翻译",
};

/** provider -> 标注文案（未知名回退为通用「机器翻译」） */
export function translateProviderLabel(provider?: string | null): string {
	if (!provider) return "机器翻译";
	return PROVIDER_LABELS[provider] || "机器翻译";
}
