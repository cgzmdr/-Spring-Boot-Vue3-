package com.czdr.work.service.impl;

import com.czdr.work.model.entity.Notification;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.resource.NotificationResource;
import com.czdr.work.service.MailService;
import com.czdr.work.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.easy.query.core.api.pagination.DefaultPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
 * 站内通知实现：
 * · 站内消息落 notification 表（未读角标直接 count，量大后可加 Redis 缓存）；
 * · 邮件通道默认关闭（app.notify.email-enabled），开启后异步发送，失败只记日志。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /**
     * 值得发邮件的类型（按产品决策收口为「仅审核结果」）：
     * · 回复 / 引用 / @提及 / 私信 / 关注 / 举报回执 一律仅站内通知；
     * · 校验类邮件走 AuthController 的验证码通道，不经此处；
     * · 网站公告（维护等）由后台 OA 群发接口单独处理。
     */
    private static final Set<String> EMAIL_TYPES = Set.of("review_result");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** 批量插入分片大小 */
    private static final int BROADCAST_CHUNK = 200;

    private final EasyEntityQuery entityQuery;
    private final MailService mailService;

    @Value("${app.notify.email-enabled:false}")
    private boolean emailEnabled;

    @Override
    public void notify(UUID userId, String type, UUID actorId, String targetType, UUID targetId,
                       String title, String content, String payloadJson) {
        if (userId == null || type == null) {
            return;
        }
        // 不给自己的操作发通知
        if (actorId != null && actorId.equals(userId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setActorId(actorId);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTitle(title);
        notification.setContent(content == null ? null : truncate(content, 500));
        notification.setPayload(payloadJson == null ? "{}" : payloadJson);
        notification.setEmailed(false);
        notification.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(notification).executeRows();

        if (emailEnabled && EMAIL_TYPES.contains(type)) {
            sendMailAsync(userId, title, content);
        }
    }

    @Override
    public int broadcast(List<UUID> userIds, String title, String content, String link, boolean email) {
        if (userIds == null || userIds.isEmpty() || title == null || title.isBlank()) {
            return 0;
        }
        List<UUID> targets = userIds.stream().filter(Objects::nonNull).distinct().toList();
        String payload = link == null || link.isBlank() ? "{}" : linkPayload(link);
        LocalDateTime now = LocalDateTime.now();
        int written = 0;
        for (int i = 0; i < targets.size(); i += BROADCAST_CHUNK) {
            List<UUID> chunk = targets.subList(i, Math.min(targets.size(), i + BROADCAST_CHUNK));
            List<Notification> rows = new ArrayList<>(chunk.size());
            for (UUID userId : chunk) {
                Notification notification = new Notification();
                notification.setId(UUID.randomUUID());
                notification.setUserId(userId);
                notification.setType("system");
                notification.setActorId(null);
                notification.setTargetType("system");
                notification.setTargetId(null);
                notification.setTitle(truncate(title, 190));
                notification.setContent(content == null ? null : truncate(content, 500));
                notification.setPayload(payload);
                notification.setEmailed(email);
                notification.setCreatedAt(now);
                rows.add(notification);
            }
            written += entityQuery.insertable(rows).executeRows();
        }
        if (email) {
            for (UUID userId : targets) {
                sendBroadcastMailAsync(userId, title, content, link);
            }
        }
        return written;
    }

    @Override
    public long unreadCount(UUID userId) {
        if (userId == null) {
            return 0L;
        }
        return entityQuery.queryable(Notification.class)
                .where(n -> {
                    n.userId().eq(userId);
                    n.readAt().isNull();
                })
                .count();
    }

    @Override
    public EasyPageResult<NotificationResource> list(UUID userId, Pageable pageable, boolean unreadOnly) {
        EasyPageResult<Notification> page = entityQuery.queryable(Notification.class)
                .where(n -> {
                    n.userId().eq(userId);
                    if (unreadOnly) {
                        n.readAt().isNull();
                    }
                })
                .orderBy(n -> n.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());

        Map<UUID, UserAuth> actors = loadActors(page.getData().stream()
                .map(n -> n.getActorId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList());

        List<NotificationResource> data = page.getData().stream()
                .map(n -> new NotificationResource(
                        n.getId().toString(),
                        n.getType(),
                        n.getActorId() == null ? null : nicknameOf(actors.get(n.getActorId())),
                        n.getActorId() == null ? null : avatarOf(actors.get(n.getActorId())),
                        n.getTitle(),
                        n.getContent(),
                        n.getTargetType(),
                        n.getTargetId() == null ? null : n.getTargetId().toString(),
                        linkOf(n.getPayload()),
                        n.getReadAt() != null,
                        n.getCreatedAt() == null ? null : n.getCreatedAt().format(TIME_FORMAT)
                ))
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    @Override
    public int markRead(UUID userId, List<UUID> ids) {
        if (userId == null) {
            return 0;
        }
        List<Notification> list = entityQuery.queryable(Notification.class)
                .where(n -> {
                    n.userId().eq(userId);
                    n.readAt().isNull();
                    if (ids != null && !ids.isEmpty()) {
                        n.id().in(ids);
                    }
                })
                .toList();
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : list) {
            notification.setReadAt(now);
            entityQuery.updatable(notification).executeRows();
        }
        return list.size();
    }

    private Map<UUID, UserAuth> loadActors(List<UUID> ids) {
        Map<UUID, UserAuth> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<UserAuth> users = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().in(ids))
                .toList();
        users.forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private void sendMailAsync(UUID userId, String title, String content) {
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String to = user.getEmail();
        String subject = "【56 民族社区】" + (title == null ? "新通知" : title);
        String body = """
                <div style="font-family:sans-serif;line-height:1.8">
                  <p>%s</p>
                  <p style="color:#888;font-size:12px">来自「走进多彩 56 个民族世界」讨论区，如需关闭邮件提醒请在个人中心设置。</p>
                </div>
                """.formatted(content == null ? "" : content);
        Thread.ofVirtual().start(() -> {
            try {
                mailService.sendHtml(to, subject, body);
            } catch (Exception e) {
                log.warn("通知邮件发送失败（{}）：{}", to, e.getMessage());
            }
        });
    }

    /** 公告邮件的正文模板（与社区事件邮件区分开） */
    private void sendBroadcastMailAsync(UUID userId, String title, String content, String link) {
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        String to = user.getEmail();
        String subject = "【56 民族社区 · 站内公告】" + (title == null ? "站点通知" : title);
        String linkLine = link == null || link.isBlank()
                ? ""
                : "<p><a href=\"%s\">查看详情</a></p>".formatted(link);
        String body = """
                <div style="font-family:sans-serif;line-height:1.8">
                  <p>%s</p>
                  %s
                  <p style="color:#888;font-size:12px">本邮件由「走进多彩 56 个民族世界」后台统一发送，仅用于站点公告。</p>
                </div>
                """.formatted(content == null ? "" : content.replace("\n", "<br/>"), linkLine);
        Thread.ofVirtual().start(() -> {
            try {
                mailService.sendHtml(to, subject, body);
            } catch (Exception e) {
                log.warn("公告邮件发送失败（{}）：{}", to, e.getMessage());
            }
        });
    }

    /** payload.link —— 站内公告的自定义跳转链接 */
    private String linkOf(String payload) {
        if (payload == null || payload.isBlank() || !payload.trim().startsWith("{")) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(payload).get("link");
            return node == null || node.asText().isBlank() ? null : node.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String linkPayload(String link) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of("link", link));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String nicknameOf(UserAuth user) {
        return user == null || user.getNickname() == null ? null : user.getNickname();
    }

    private String avatarOf(UserAuth user) {
        return user == null ? null : user.getAvatar();
    }

    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
