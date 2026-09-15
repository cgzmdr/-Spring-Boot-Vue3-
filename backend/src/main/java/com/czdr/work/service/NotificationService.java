package com.czdr.work.service;

import com.czdr.work.model.resource.NotificationResource;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 站内通知（含可选邮件通道）
 *
 * @author cz
 */
public interface NotificationService {

    /** 写入一条通知（异步发送邮件，失败不影响主流程） */
    void notify(UUID userId, String type, UUID actorId, String targetType, UUID targetId,
                String title, String content, String payloadJson);

    /**
     * 群发站内公告（type=system，后台 OA 主动发送，不随社区事件自动触发）。
     *
     * @param userIds 收件人
     * @param title   标题
     * @param content 正文
     * @param link    可选跳转链接（随 payload 下发）
     * @param email   是否同时发邮件
     * @return 成功写入的站内通知条数
     */
    int broadcast(List<UUID> userIds, String title, String content, String link, boolean email);

    /** 未读数量（角标） */
    long unreadCount(UUID userId);

    /** 分页列表 */
    EasyPageResult<NotificationResource> list(UUID userId, Pageable pageable, boolean unreadOnly);

    /** 标记已读：ids 为空表示全部已读 */
    int markRead(UUID userId, List<UUID> ids);
}
