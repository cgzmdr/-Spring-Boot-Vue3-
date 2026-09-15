package com.czdr.work.service;

import com.czdr.work.model.resource.SubscriptionResource;

import java.util.List;
import java.util.UUID;

/**
 * 订阅：帖子 / 板块 / 用户 的三级通知强度
 * level：all 全部通知 / mention 仅被 @ 时通知 / off 免打扰
 *
 * @author cz
 */
public interface SubscriptionService {

    String LEVEL_ALL = "all";
    String LEVEL_MENTION = "mention";
    String LEVEL_OFF = "off";

    String TYPE_TOPIC = "topic";
    String TYPE_BOARD = "board";
    String TYPE_USER = "user";

    /** 设置订阅等级（level=off 时保留记录，便于用户随时恢复） */
    String subscribe(UUID userId, String targetType, UUID targetId, String level);

    /** 当前用户的订阅等级；未订阅返回 null */
    String levelOf(UUID userId, String targetType, UUID targetId);

    /** 帖子是否有「全部通知」订阅者之外的默认接收（帖子作者默认 all 由创建时写入） */
    boolean wantsAll(UUID userId, String targetType, UUID targetId);

    /** 我的订阅列表（含目标名称） */
    List<SubscriptionResource> mySubscriptions(UUID userId);

    /** 某目标的订阅者（level=all），用于批量通知 */
    List<UUID> subscribersOf(String targetType, UUID targetId);

    /** 取关（删除记录） */
    void remove(UUID userId, String targetType, UUID targetId);
}
