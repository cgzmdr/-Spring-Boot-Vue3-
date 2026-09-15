package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.DiscussionBoard;
import com.czdr.work.model.entity.DiscussionSubscription;
import com.czdr.work.model.entity.DiscussionTopic;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.resource.SubscriptionResource;
import com.czdr.work.service.SubscriptionService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 订阅实现：帖子 / 板块 / 用户 三级通知强度。
 * 记录始终保留（level=off 表示免打扰），便于用户随时恢复。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Set<String> LEVELS = Set.of(LEVEL_ALL, LEVEL_MENTION, LEVEL_OFF);
    private static final Set<String> TYPES = Set.of(TYPE_TOPIC, TYPE_BOARD, TYPE_USER);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EasyEntityQuery entityQuery;

    @Override
    @Transactional
    public String subscribe(UUID userId, String targetType, UUID targetId, String level) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (!TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的订阅类型");
        }
        String value = level == null || level.isBlank() ? LEVEL_ALL : level.trim();
        if (!LEVELS.contains(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "通知强度不合法（all / mention / off）");
        }
        DiscussionSubscription existing = entityQuery.queryable(DiscussionSubscription.class)
                .where(s -> {
                    s.userId().eq(userId);
                    s.targetType().eq(targetType);
                    s.targetId().eq(targetId);
                })
                .firstOrNull();
        if (existing == null) {
            DiscussionSubscription created = new DiscussionSubscription();
            created.setId(UUID.randomUUID());
            created.setUserId(userId);
            created.setTargetType(targetType);
            created.setTargetId(targetId);
            created.setLevel(value);
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            entityQuery.insertable(created).executeRows();
        } else {
            existing.setLevel(value);
            existing.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(existing).executeRows();
        }
        return value;
    }

    @Override
    public String levelOf(UUID userId, String targetType, UUID targetId) {
        if (userId == null || targetId == null) {
            return null;
        }
        DiscussionSubscription row = entityQuery.queryable(DiscussionSubscription.class)
                .where(s -> {
                    s.userId().eq(userId);
                    s.targetType().eq(targetType);
                    s.targetId().eq(targetId);
                })
                .firstOrNull();
        return row == null ? null : row.getLevel();
    }

    @Override
    public boolean wantsAll(UUID userId, String targetType, UUID targetId) {
        String level = levelOf(userId, targetType, targetId);
        // 未订阅时按「默认接收」处理（例如帖子作者），显式 off 才屏蔽
        return level == null || LEVEL_ALL.equals(level);
    }

    @Override
    public List<SubscriptionResource> mySubscriptions(UUID userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<DiscussionSubscription> rows = entityQuery.queryable(DiscussionSubscription.class)
                .where(s -> s.userId().eq(userId))
                .orderBy(s -> s.createdAt().desc())
                .toList();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> topicTitles = new HashMap<>();
        Map<UUID, String> topicImages = new HashMap<>();
        Map<UUID, String> boardNames = new HashMap<>();
        Map<UUID, String> userNames = new HashMap<>();

        List<UUID> topicIds = idsOf(rows, TYPE_TOPIC);
        if (!topicIds.isEmpty()) {
            entityQuery.queryable(DiscussionTopic.class)
                    .where(t -> t.id().in(topicIds))
                    .toList()
                    .forEach(t -> {
                        topicTitles.put(t.getId(), t.getTitle());
                        topicImages.put(t.getId(), t.getImages());
                    });
        }
        List<UUID> boardIds = idsOf(rows, TYPE_BOARD);
        if (!boardIds.isEmpty()) {
            entityQuery.queryable(DiscussionBoard.class)
                    .where(b -> b.id().in(boardIds))
                    .toList()
                    .forEach(b -> boardNames.put(b.getId(), b.getName()));
        }
        List<UUID> userIds = idsOf(rows, TYPE_USER);
        if (!userIds.isEmpty()) {
            entityQuery.queryable(UserAuth.class)
                    .where(u -> u.id().in(userIds))
                    .toList()
                    .forEach(u -> userNames.put(u.getId(),
                            u.getNickname() == null || u.getNickname().isBlank() ? "民族之友" : u.getNickname()));
        }

        List<SubscriptionResource> data = new ArrayList<>();
        for (DiscussionSubscription row : rows) {
            String name;
            String path;
            String cover = null;
            switch (row.getTargetType()) {
                case TYPE_TOPIC -> {
                    name = topicTitles.getOrDefault(row.getTargetId(), "已删除的帖子");
                    cover = firstImage(topicImages.get(row.getTargetId()));
                    path = "/discussion/topic/" + row.getTargetId();
                }
                case TYPE_BOARD -> {
                    name = boardNames.getOrDefault(row.getTargetId(), "已关闭的板块");
                    path = "/discussion?board=" + row.getTargetId();
                }
                case TYPE_USER -> {
                    name = userNames.getOrDefault(row.getTargetId(), "已注销用户");
                    path = "/user/" + row.getTargetId();
                }
                default -> {
                    name = row.getTargetId().toString();
                    path = "/discussion";
                }
            }
            data.add(new SubscriptionResource(
                    row.getId().toString(),
                    row.getTargetType(),
                    row.getTargetId().toString(),
                    name,
                    cover,
                    path,
                    row.getLevel(),
                    row.getCreatedAt() == null ? null : row.getCreatedAt().format(TIME_FORMAT)
            ));
        }
        return data;
    }

    @Override
    public List<UUID> subscribersOf(String targetType, UUID targetId) {
        if (targetId == null) {
            return List.of();
        }
        return entityQuery.queryable(DiscussionSubscription.class)
                .where(s -> {
                    s.targetType().eq(targetType);
                    s.targetId().eq(targetId);
                    s.level().eq(LEVEL_ALL);
                })
                .toList()
                .stream()
                .map(DiscussionSubscription::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public void remove(UUID userId, String targetType, UUID targetId) {
        DiscussionSubscription row = entityQuery.queryable(DiscussionSubscription.class)
                .where(s -> {
                    s.userId().eq(userId);
                    s.targetType().eq(targetType);
                    s.targetId().eq(targetId);
                })
                .firstOrNull();
        if (row != null) {
            entityQuery.deletable(row).allowDeleteStatement(true).executeRows();
        }
    }

    private List<UUID> idsOf(List<DiscussionSubscription> rows, String type) {
        return rows.stream()
                .filter(r -> type.equals(r.getTargetType()))
                .map(DiscussionSubscription::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String firstImage(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        int start = json.indexOf('"');
        if (start < 0) {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        String value = json.substring(start + 1, end).trim();
        return value.isEmpty() ? null : value;
    }
}
