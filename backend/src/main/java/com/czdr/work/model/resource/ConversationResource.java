package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 私信会话（列表 / 会话头）
 */
@Schema(description = "私信会话")
public record ConversationResource(
        @Schema(description = "会话 ID") String id,
        @Schema(description = "对方用户 ID") String peerId,
        @Schema(description = "对方昵称") String peerName,
        @Schema(description = "对方头像") String peerAvatar,
        @Schema(description = "最后一条消息摘要") String lastPreview,
        @Schema(description = "最后消息时间") String lastMessageAt,
        @Schema(description = "最后一条是否由我发出") boolean lastMine,
        @Schema(description = "我的未读数") int unread,
        @Schema(description = "我是否已拉黑对方（拉黑期间双方不能互发消息）") boolean blocked
) {
}
