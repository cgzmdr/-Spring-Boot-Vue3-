package com.czdr.work.config;

import com.czdr.work.comment.advisor.AuditLogAdvisor;
import com.czdr.work.comment.advisor.BudgetAdvisor;
import com.czdr.work.comment.advisor.InputGuardAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.model.ChatModel;

/**
 * AI 能力统一装配处。
 *
 * <p>所有 ChatClient 都在这里定义，并统一挂载 Harness 管控链
 * （输入防护 / 预算控制 / 审计日志）；通用客户端额外挂载 Skills 工具。</p>
 * @author cz
 */
@Configuration
public class AiConfig {

    public static final String TERMS_MARKER = "---TERMS---";

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

    // ==================== 通用对话客户端（含 Harness + Skills） ====================
    /**
     * 通用对话客户端（默认人格「小夏」）+ 完整 Harness 管控链 + 默认 Skills。
     * <p>按类型注入 ChatClient 时默认拿到它（由 {@code @Primary} 保证）。</p>
     */
    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel,
                                 InputGuardAdvisor inputGuard,
                                 BudgetAdvisor budgetAdvisor,
                                 AuditLogAdvisor auditLogAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你的名字是小夏
                        """)
                // 注入默认 Skills：模型会自动发现并按需加载 SKILL.md
                .defaultTools(
                        SkillsTool.builder()
                                .addSkillsDirectory("classpath:.claude/skills")
                                .build()
                )
                // Harness 管控链（按 order 顺序执行）
                .defaultAdvisors(
                        inputGuard,
                        budgetAdvisor,
                        auditLogAdvisor
                )
                .build();
    }

    // ==================== 翻译专用客户端（仅 Harness 管控，不含 Skills） ====================
    /**
     * 机器翻译专用客户端。
     * <p>与通用客户端隔离系统提示词；同样受 Harness 管控（预算 / 审计），
     * 但不挂载 Skills —— 翻译是确定性任务，无需技能发现。</p>
     * <p>若翻译场景也不希望走敏感词过滤，可以把 inputGuard 参数去掉。</p>
     */
    @Bean
    public ChatClient translateChatClient(ChatModel chatModel,
                                          BudgetAdvisor budgetAdvisor,
                                          AuditLogAdvisor auditLogAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultSystem(translateSystemPrompt())
                .defaultAdvisors(
                        budgetAdvisor,
                        auditLogAdvisor
                )
                .build();
    }

    public String translateSystemPrompt() {
        return TRANSLATE_SYSTEM_PROMPT_TEMPLATE.formatted(
                TERMS_MARKER, properties.autoGlossaryMaxTerms(), TERMS_MARKER);
    }
}
