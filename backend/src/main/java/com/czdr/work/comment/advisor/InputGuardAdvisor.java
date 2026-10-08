package com.czdr.work.comment.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author cz
 */
@Component
public class InputGuardAdvisor implements CallAdvisor {

    private static final Set<String> SENSITIVE_WORDS = Set.of("暴力", "违禁", "攻击");

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 提取用户输入
        String userInput = request.prompt().getInstructions().stream()
                .filter(msg -> msg.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .collect(Collectors.joining(" "));

        // 2. 检查敏感词
        for (String word : SENSITIVE_WORDS) {
            if (userInput.contains(word)) {
                // 3. 阻断请求，不调用 chain.nextCall()
                return ChatClientResponse.builder()
                        .chatResponse(new ChatResponse(
                                List.of(new Generation(new AssistantMessage("输入包含敏感内容，请求已被拦截。")))
                        ))
                        .build();
            }
        }
        // 4. 无敏感词，继续传递请求
        return chain.nextCall(request);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public String getName() {
        return "InputGuardAdvisor";
    }
}