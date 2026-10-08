package com.czdr.work.comment.advisor;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * @author cz
 */
@Component
public class AuditLogAdvisor implements CallAdvisor {

    private static final Logger auditLog = LoggerFactory.getLogger("AI_AUDIT");

    @Override
    public @NotNull ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long startTime = System.currentTimeMillis();
        // 记录请求
        auditLog.info("REQUEST: {}", request.prompt().getInstructions());

        ChatClientResponse response = chain.nextCall(request);

        long duration = System.currentTimeMillis() - startTime;
        // 记录响应与元数据
        if (response.chatResponse() != null) {
            auditLog.info("RESPONSE: {}, DURATION: {}ms, USAGE: {}",
                    response.chatResponse().getResult().getOutput().getText(),
                    duration,
                    response.chatResponse().getMetadata().getUsage());
        }
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public @NotNull String getName() {
        return "AuditLogAdvisor";
    }
}