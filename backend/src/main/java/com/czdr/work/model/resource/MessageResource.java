package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 私信消息
 */
@Schema(description = "私信消息")
public record MessageResource(
        @Schema(description = "消息 ID") String id,
        @Schema(description = "会话 ID") String conversationId,
        @Schema(description = "发送者 ID") String senderId,
        @Schema(description = "是否为我发送") boolean mine,
        @Schema(description = "内容（撤回后为空）") String content,
        @Schema(description = "图片列表") List<String> images,
        @Schema(description = "语言") String lang,
        @Schema(description = "是否已撤回") boolean recalled,
        @Schema(description = "我是否可以撤回（自己发送且 2 分钟内）") boolean recallable,
        @Schema(description = "对方是否已读") boolean read,
        @Schema(description = "发送时间") String createdAt
) {
}
