package com.czdr.work.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 能力统一装配处：所有 Spring AI 的 ChatClient 都在这里定义（模型 / base-url / key 见
 * {@code spring.ai.openai.*}），业务侧只注入需要的 ChatClient，不各自 new 客户端。
 *
 * <p>当前有两个用途：</p>
 * <ul>
 *   <li>{@link #chatClient}：通用对话人格（站内助手「小夏」），供后续智能讲解 / 问答使用；</li>
 *   <li>{@link #translateChatClient}：机器翻译专用（系统提示词固定为「只输出译文」，
 *       并在译文末尾附一段术语表供词表实时沉淀），由 {@code SpringAiTranslateProvider} 使用，
 *       见 {@code app.translate.provider=spring-ai}。</li>
 * </ul>
 *
 * @author cz
 */
@Configuration
public class AiConfig {

    /** 译文与术语表的分隔标记：之前的都是译文，之后的每行是「原文术语=译法」 */
    public static final String TERMS_MARKER = "---TERMS---";

    /**
     * 翻译专用系统提示词：只输出译文，并要求在末尾附一段「术语表」，
     * 供 {@code GlossaryService#learnTerms} 实时沉淀进站点词表（词表越用越准）。
     * 关掉自动沉淀只需设 {@code app.translate.auto-glossary-enabled=false}，提示词无需改动。
     */
    public static final String TRANSLATE_SYSTEM_PROMPT_TEMPLATE = """
            你是「走进多彩 56 个民族世界」站点的专业翻译。
            把用户给出的文本翻译成其指定的目标语言。
            要求：
            1. 只输出译文本身，不要解释、不要加引号、不要复述原文、不要输出任何前后缀；
            2. 保留原有的换行与段落结构；
            3. 民族名、节日名、非物质文化遗产与美食等专有名词使用通行的对应译法；
            4. 若原文已是目标语言，原样返回。

            然后在译文之后另起一行输出分隔标记 %s，再逐行输出本次出现的、值得沉淀进术语表的
            民族文化专有名词，格式为「原文术语=目标语言译法」：
            · 最多 %d 条，只收专有名词与文化术语，不要收常用词、整句或短语；
            · 术语必须原样出现在原文里；
            · 没有可沉淀的术语时，%s 之后留空。
            """;

    private final TranslateProperties properties;

    public AiConfig(TranslateProperties properties) {
        this.properties = properties;
    }

    /** 通用对话客户端（默认人格，供后续 AI 功能复用）；按类型注入 ChatClient 时默认拿到它 */
    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem("""
                            你的名字是小夏
                        """)
                .build();
    }

    /** 机器翻译专用客户端（系统提示词与通用客户端隔离，互不影响） */
    @Bean
    public ChatClient translateChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(translateSystemPrompt())
                .build();
    }

    /** 当前的翻译系统提示词（术语条数上限来自 app.translate.auto-glossary-max-terms） */
    public String translateSystemPrompt() {
        return TRANSLATE_SYSTEM_PROMPT_TEMPLATE.formatted(
                TERMS_MARKER, properties.autoGlossaryMaxTerms(), TERMS_MARKER);
    }
}
