package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.DiscussionBlock;
import com.czdr.work.model.entity.DiscussionConversation;
import com.czdr.work.model.entity.DiscussionMessage;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.resource.ConversationResource;
import com.czdr.work.model.resource.MessageResource;
import com.czdr.work.service.MessageService;
import com.czdr.work.service.NotificationService;
import com.czdr.work.service.RateLimitService;
import com.czdr.work.service.SocialService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 私信实现。
 * 隐私策略：仅互相关注（好友）之间可发起会话与发送消息；
 * 单条 2000 字以内，30 条/小时限流；会话以 (user_a, user_b) 唯一，未读数分别记在两侧。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final int MAX_CONTENT = 2000;
    private static final int MAX_IMAGES = 4;
    /** 撤回时限：2 分钟 */
    private static final long RECALL_WINDOW_SECONDS = 120;
    /** 消息时间精确到秒：同一分钟内的多条消息前端才能稳定排序 */
    private static final DateTimeFormatter MESSAGE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 与 DiscussionServiceImpl 一致：项目未提供 ObjectMapper Bean，直接构建静态实例 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private final EasyEntityQuery entityQuery;
    private final SocialService socialService;
    private final NotificationService notificationService;
    private final RateLimitService rateLimitService;

    @Override
    @Transactional
    public ConversationResource openConversation(UUID userId, UUID peerId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (peerId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少对方用户 ID");
        }
        if (userId.equals(peerId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能给自己发私信");
        }
        requireMutual(userId, peerId);
        DiscussionConversation conversation = findOrCreate(userId, peerId);
        return toResource(conversation, userId, null);
    }

    @Override
    public EasyPageResult<ConversationResource> conversations(UUID userId, Pageable pageable) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // 会话可能落在 user_a 或 user_b 两侧：分别查询后合并分页
        List<DiscussionConversation> all = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> {
                    c.userA().eq(userId);
                })
                .orderBy(c -> c.lastMessageAt().desc())
                .toList();
        List<DiscussionConversation> asB = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> {
                    c.userB().eq(userId);
                })
                .orderBy(c -> c.lastMessageAt().desc())
                .toList();
        all.addAll(asB);
        all.sort((x, y) -> {
            LocalDateTime left = x.getLastMessageAt() == null ? x.getCreatedAt() : x.getLastMessageAt();
            LocalDateTime right = y.getLastMessageAt() == null ? y.getCreatedAt() : y.getLastMessageAt();
            return right.compareTo(left);
        });

        int from = pageable.getPageNumber() * pageable.getPageSize();
        int to = Math.min(all.size(), from + pageable.getPageSize());
        List<DiscussionConversation> slice = from >= all.size() ? List.of() : all.subList(from, to);
        Map<UUID, UserAuth> peers = loadUsers(slice.stream().map(c -> peerOf(c, userId)).toList());
        List<ConversationResource> data = slice.stream()
                .map(c -> toResource(c, userId, peers.get(peerOf(c, userId))))
                .toList();
        return new DefaultPageResult<>((long) all.size(), data);
    }

    @Override
    public ConversationResource conversation(UUID userId, String conversationId) {
        DiscussionConversation conversation = requireConversation(userId, conversationId);
        return toResource(conversation, userId, null);
    }

    @Override
    public EasyPageResult<MessageResource> messages(UUID userId, String conversationId, Pageable pageable) {
        DiscussionConversation conversation = requireConversation(userId, conversationId);
        EasyPageResult<DiscussionMessage> page = entityQuery.queryable(DiscussionMessage.class)
                .where(m -> m.conversationId().eq(conversation.getId()))
                .orderBy(m -> m.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
        List<MessageResource> data = page.getData().stream()
                .map(m -> toMessageResource(m, userId))
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    /** 消息 → 资源（含图片、撤回状态与可撤回判断） */
    private MessageResource toMessageResource(DiscussionMessage message, UUID viewerId) {
        boolean mine = message.getSenderId().equals(viewerId);
        boolean recalled = Boolean.TRUE.equals(message.getRecalled());
        boolean recallable = mine && !recalled && message.getCreatedAt() != null
                && message.getCreatedAt().plusSeconds(RECALL_WINDOW_SECONDS).isAfter(LocalDateTime.now());
        return new MessageResource(
                message.getId().toString(),
                message.getConversationId().toString(),
                message.getSenderId().toString(),
                mine,
                recalled ? "" : message.getContent(),
                recalled ? List.of() : parseImages(message.getImages()),
                message.getLang(),
                recalled,
                recallable,
                message.getReadAt() != null,
                message.getCreatedAt() == null ? null : message.getCreatedAt().format(MESSAGE_TIME)
        );
    }

    @Override
    @Transactional
    public MessageResource send(UUID userId, String conversationId, String content, String lang, List<String> images) {
        DiscussionConversation conversation = requireConversation(userId, conversationId);
        UUID peerId = peerOf(conversation, userId);
        requireMutual(userId, peerId);
        if (blockedBetween(userId, peerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你们之间存在拉黑关系，无法发送私信");
        }
        String text = content == null ? "" : content.trim();
        List<String> safeImages = validateImages(images);
        if (text.isEmpty() && safeImages.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息内容不能为空");
        }
        if (text.length() > MAX_CONTENT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息过长（最多 2000 字）");
        }
        rateLimitService.consume("dm", userId.toString(), 30, 3600, "发送过于频繁，请稍后再试");

        DiscussionMessage message = new DiscussionMessage();
        message.setId(UUID.randomUUID());
        message.setConversationId(conversation.getId());
        message.setSenderId(userId);
        message.setContent(text);
        message.setImages(writeImages(safeImages));
        message.setLang(lang == null || lang.isBlank() ? "zh" : lang);
        message.setRecalled(false);
        message.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(message).executeRows();

        boolean senderIsA = conversation.getUserA().equals(userId);
        if (senderIsA) {
            conversation.setUnreadB((conversation.getUnreadB() == null ? 0 : conversation.getUnreadB()) + 1);
        } else {
            conversation.setUnreadA((conversation.getUnreadA() == null ? 0 : conversation.getUnreadA()) + 1);
        }
        conversation.setLastMessageAt(message.getCreatedAt());
        // 注意：项目注册的 jsonb TypeHandler 会把「以 [ 或 { 开头的字符串」按 jsonb 绑定，
        // 因此会话摘要不要用方括号前缀，避免 varchar 列被当成 JSON 解析而报错。
        conversation.setLastPreview(excerpt(safeImages.isEmpty() ? text : "图片 · " + text, 200));
        conversation.setLastSenderId(userId);
        entityQuery.updatable(conversation).executeRows();

        notificationService.notify(peerId, "message", userId, "discussion_conversation", conversation.getId(),
                "你有一条新私信", "%s：%s".formatted(nicknameOf(userId), excerpt(text.isEmpty() ? "图片" : text, 60)), null);

        return toMessageResource(message, userId);
    }

    @Override
    @Transactional
    public MessageResource recall(UUID userId, String messageId) {
        UUID id = parseUuid(messageId);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "消息 ID 不正确");
        }
        DiscussionMessage message = entityQuery.queryable(DiscussionMessage.class)
                .where(m -> m.id().eq(id))
                .firstOrNull();
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息不存在");
        }
        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能撤回自己发送的消息");
        }
        if (Boolean.TRUE.equals(message.getRecalled())) {
            return toMessageResource(message, userId);
        }
        if (message.getCreatedAt() == null
                || message.getCreatedAt().plusSeconds(RECALL_WINDOW_SECONDS).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "超过 2 分钟的消息不能撤回");
        }
        message.setRecalled(true);
        message.setRecalledAt(LocalDateTime.now());
        message.setContent("");
        message.setImages("[]");
        entityQuery.updatable(message).executeRows();

        // 会话摘要同步刷新为「已撤回」
        DiscussionConversation conversation = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> c.id().eq(message.getConversationId()))
                .firstOrNull();
        if (conversation != null && message.getCreatedAt() != null
                && conversation.getLastMessageAt() != null
                && !conversation.getLastMessageAt().isAfter(message.getCreatedAt())) {
            conversation.setLastPreview("消息已撤回");
            entityQuery.updatable(conversation).executeRows();
        }
        return toMessageResource(message, userId);
    }

    @Override
    @Transactional
    public boolean block(UUID userId, UUID peerId, boolean block) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (peerId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少对方用户 ID");
        }
        if (userId.equals(peerId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能拉黑自己");
        }
        DiscussionBlock existing = entityQuery.queryable(DiscussionBlock.class)
                .where(b -> {
                    b.userId().eq(userId);
                    b.blockedUserId().eq(peerId);
                })
                .firstOrNull();
        if (block) {
            if (existing == null) {
                DiscussionBlock created = new DiscussionBlock();
                created.setId(UUID.randomUUID());
                created.setUserId(userId);
                created.setBlockedUserId(peerId);
                created.setCreatedAt(LocalDateTime.now());
                entityQuery.insertable(created).executeRows();
            }
            // 拉黑即解除双方互相关注（私信依赖互关，因此立即失效）
            socialService.follow(userId, peerId, false);
            socialService.follow(peerId, userId, false);
            return true;
        }
        if (existing != null) {
            entityQuery.deletable(existing).allowDeleteStatement(true).executeRows();
        }
        return false;
    }

    @Override
    public boolean isBlocked(UUID userId, UUID peerId) {
        if (userId == null || peerId == null) {
            return false;
        }
        return entityQuery.queryable(DiscussionBlock.class)
                .where(b -> {
                    b.userId().eq(userId);
                    b.blockedUserId().eq(peerId);
                })
                .firstOrNull() != null;
    }

    @Override
    public boolean blockedBetween(UUID userId, UUID peerId) {
        return isBlocked(userId, peerId) || isBlocked(peerId, userId);
    }

    @Override
    @Transactional
    public int markRead(UUID userId, String conversationId) {
        DiscussionConversation conversation = requireConversation(userId, conversationId);
        List<DiscussionMessage> unread = entityQuery.queryable(DiscussionMessage.class)
                .where(m -> {
                    m.conversationId().eq(conversation.getId());
                    m.senderId().ne(userId);
                    m.readAt().isNull();
                })
                .toList();
        LocalDateTime now = LocalDateTime.now();
        for (DiscussionMessage message : unread) {
            message.setReadAt(now);
            entityQuery.updatable(message).executeRows();
        }
        if (conversation.getUserA().equals(userId)) {
            conversation.setUnreadA(0);
        } else {
            conversation.setUnreadB(0);
        }
        entityQuery.updatable(conversation).executeRows();
        return unread.size();
    }

    @Override
    public long unreadTotal(UUID userId) {
        if (userId == null) {
            return 0L;
        }
        long asA = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> c.userA().eq(userId))
                .toList()
                .stream()
                .mapToLong(c -> c.getUnreadA() == null ? 0 : c.getUnreadA())
                .sum();
        long asB = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> c.userB().eq(userId))
                .toList()
                .stream()
                .mapToLong(c -> c.getUnreadB() == null ? 0 : c.getUnreadB())
                .sum();
        return asA + asB;
    }

    // ---------------------------------------------------------------- 内部方法

    private void requireMutual(UUID userId, UUID peerId) {
        if (blockedBetween(userId, peerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你们之间存在拉黑关系，无法私信");
        }
        if (!socialService.isMutual(userId, peerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅互相关注的好友之间可以私信，先去对方主页点个关注吧");
        }
    }

    private List<String> validateImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (String image : images) {
            if (image == null || image.isBlank()) {
                continue;
            }
            String value = image.trim();
            if (value.length() > 300) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "图片地址过长");
            }
            if (!value.startsWith("/") && !value.startsWith("http://") && !value.startsWith("https://")) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "图片地址不合法");
            }
            list.add(value);
            if (list.size() > MAX_IMAGES) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "最多发送 " + MAX_IMAGES + " 张图片");
            }
        }
        return list;
    }

    private String writeImages(List<String> images) {
        try {
            return OBJECT_MAPPER.writeValueAsString(images == null ? List.of() : images);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private DiscussionConversation findOrCreate(UUID userId, UUID peerId) {
        // 注意：数据库 CHECK (user_a < user_b) 按 UUID 字节序比较，
        // 而 Java UUID.compareTo 是按「有符号 long」比较，两者在大 MSB 时结论相反；
        // 这里统一用十六进制文本比较（与 PG 的字节序一致）。
        boolean userFirst = userId.toString().compareTo(peerId.toString()) <= 0;
        UUID a = userFirst ? userId : peerId;
        UUID b = userFirst ? peerId : userId;
        DiscussionConversation existing = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> {
                    c.userA().eq(a);
                    c.userB().eq(b);
                })
                .firstOrNull();
        if (existing != null) {
            return existing;
        }
        DiscussionConversation created = new DiscussionConversation();
        created.setId(UUID.randomUUID());
        created.setUserA(a);
        created.setUserB(b);
        created.setUnreadA(0);
        created.setUnreadB(0);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        entityQuery.insertable(created).executeRows();
        return created;
    }

    private DiscussionConversation requireConversation(UUID userId, String conversationId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        UUID id = parseUuid(conversationId);
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "会话 ID 不正确");
        }
        DiscussionConversation conversation = entityQuery.queryable(DiscussionConversation.class)
                .where(c -> c.id().eq(id))
                .firstOrNull();
        if (conversation == null || !isParticipant(conversation, userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    private boolean isParticipant(DiscussionConversation conversation, UUID userId) {
        return userId.equals(conversation.getUserA()) || userId.equals(conversation.getUserB());
    }

    private UUID peerOf(DiscussionConversation conversation, UUID userId) {
        return userId.equals(conversation.getUserA()) ? conversation.getUserB() : conversation.getUserA();
    }

    private ConversationResource toResource(DiscussionConversation conversation, UUID userId, UserAuth peer) {
        UUID peerId = peerOf(conversation, userId);
        UserAuth peerUser = peer != null ? peer : entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(peerId))
                .firstOrNull();
        int unread = userId.equals(conversation.getUserA())
                ? (conversation.getUnreadA() == null ? 0 : conversation.getUnreadA())
                : (conversation.getUnreadB() == null ? 0 : conversation.getUnreadB());
        return new ConversationResource(
                conversation.getId().toString(),
                peerId.toString(),
                nickname(peerUser),
                peerUser == null ? null : peerUser.getAvatar(),
                conversation.getLastPreview(),
                format(conversation.getLastMessageAt()),
                conversation.getLastSenderId() != null && conversation.getLastSenderId().equals(userId),
                unread,
                isBlocked(userId, peerId)
        );
    }

    private Map<UUID, UserAuth> loadUsers(List<UUID> ids) {
        List<UUID> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        Map<UUID, UserAuth> map = new HashMap<>();
        if (distinct.isEmpty()) {
            return map;
        }
        entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().in(distinct))
                .toList()
                .forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private String nicknameOf(UUID userId) {
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        return nickname(user);
    }

    private String nickname(UserAuth user) {
        if (user == null) {
            return "已注销用户";
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? "民族之友" : user.getNickname();
    }

    private String excerpt(String text, int max) {
        String flat = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return flat.length() > max ? flat.substring(0, max) + "…" : flat;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
