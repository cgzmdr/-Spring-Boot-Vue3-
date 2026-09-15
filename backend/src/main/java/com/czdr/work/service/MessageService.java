package com.czdr.work.service;

import com.czdr.work.model.resource.ConversationResource;
import com.czdr.work.model.resource.MessageResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * 私信：仅「互相关注」的用户之间可会话（产品决议，避免陌生人骚扰）
 *
 * @author cz
 */
public interface MessageService {

    /** 获取或创建与某用户的会话（非互关抛 1004） */
    ConversationResource openConversation(UUID userId, UUID peerId);

    /** 我的会话列表 */
    EasyPageResult<ConversationResource> conversations(UUID userId, Pageable pageable);

    /** 会话详情（仅参与者可见） */
    ConversationResource conversation(UUID userId, String conversationId);

    /** 会话消息（按时间倒序分页，前端反转展示） */
    EasyPageResult<MessageResource> messages(UUID userId, String conversationId, Pageable pageable);

    /** 发送消息（非互关抛 1004；限流；可带最多 4 张图片） */
    MessageResource send(UUID userId, String conversationId, String content, String lang, java.util.List<String> images);

    /** 撤回自己发送的消息（2 分钟内） */
    MessageResource recall(UUID userId, String messageId);

    /** 拉黑 / 解除拉黑（拉黑会解除双方互关，并阻止双方互发消息） */
    boolean block(UUID userId, UUID peerId, boolean block);

    /** 我是否已拉黑对方 */
    boolean isBlocked(UUID userId, UUID peerId);

    /** 双方之间是否存在任意方向的拉黑（用于禁止私信与关注） */
    boolean blockedBetween(UUID userId, UUID peerId);

    /** 标记该会话对方发来的消息为已读 */
    int markRead(UUID userId, String conversationId);

    /** 未读私信总数（角标） */
    long unreadTotal(UUID userId);
}
