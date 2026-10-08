package com.czdr.work.comment.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

import static cn.dev33.satoken.SaManager.log;

/**
 * @author cz
 */
@Component
public class BudgetAdvisor implements CallAdvisor {

    private final AtomicLong remainingBudget = new AtomicLong(1_000_000);
    private final double costPerToken = 0.02;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 调用前预估/预留预算（此处简化处理，可加入更复杂的预留逻辑）
        if (remainingBudget.get() <= 0) {
            throw new IllegalStateException("预算已耗尽，请求被拒绝。");
        }

        // 2. 调用下游链
        ChatClientResponse response = chain.nextCall(request);

        // 3. 调用后，根据实际用量扣减预算
        if (response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
            var usage = response.chatResponse().getMetadata().getUsage();
            long totalTokens = usage.getTotalTokens();
            remainingBudget.addAndGet(-totalTokens);
            log.info("本次消耗 Token: {}, 剩余预算: {}", totalTokens, remainingBudget.get());
        }
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public String getName() {
        return "BudgetAdvisor";
    }
}