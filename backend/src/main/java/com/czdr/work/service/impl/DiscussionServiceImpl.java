package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ContentReview;
import com.czdr.work.model.entity.DiscussionBoard;
import com.czdr.work.model.entity.DiscussionPost;
import com.czdr.work.model.entity.DiscussionTopic;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.request.DiscussionPostCreateRequest;
import com.czdr.work.model.request.DiscussionTopicCreateRequest;
import com.czdr.work.model.request.DiscussionTopicUpdateRequest;
import com.czdr.work.model.resource.DiscussionAuthorResource;
import com.czdr.work.model.resource.DiscussionBoardResource;
import com.czdr.work.model.resource.DiscussionPostResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.DiscussionTopicDetailResource;
import com.czdr.work.service.DiscussionService;
import com.czdr.work.service.NotificationService;
import com.czdr.work.service.RateLimitService;
import com.czdr.work.service.SensitiveCheckResult;
import com.czdr.work.service.SensitiveWordService;
import com.czdr.work.service.SubscriptionService;
import com.czdr.work.util.MentionParser;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 讨论区核心实现。
 *
 * 发帖策略（先发后审）：
 * · 普通内容直接 published；
 * · 命中 block 级敏感词或板块要求先审后发 → pending（仅作者与管理员可见，并写入 content_review 审核队列）；
 * · 命中 watch 级敏感词 → 仍发布，但打上 risk_level 供审核抽查。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    public static final String TYPE_TOPIC = "discussion_topic";
    public static final String TYPE_POST = "discussion_post";

    /** 与 EthnicConvert 保持一致：项目未提供 ObjectMapper Bean，直接构建静态实例 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private static final int MAX_TITLE = 200;
    private static final int MIN_TITLE = 2;
    private static final int MAX_CONTENT = 20000;
    private static final int MAX_REPLY = 5000;
    private static final int MAX_IMAGES = 9;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EasyEntityQuery entityQuery;
    private final SensitiveWordService sensitiveWordService;
    private final RateLimitService rateLimitService;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    // ------------------------------------------------------------------ 板块

    @Override
    public List<DiscussionBoardResource> boards() {
        return entityQuery.queryable(DiscussionBoard.class)
                .where(b -> b.status().eq("active"))
                .orderBy(b -> b.orderNum().asc())
                .toList()
                .stream()
                .map(b -> new DiscussionBoardResource(
                        b.getId().toString(), b.getSlug(), b.getName(), b.getNameEn(),
                        b.getDescription(), b.getIcon(), b.getThemeColor(), b.getTopicCount()))
                .toList();
    }

    // ------------------------------------------------------------------ 帖子

    @Override
    public EasyPageResult<DiscussionTopicBriefResource> topics(String boardId, String keyword, String sort,
                                                               String linkedType, String linkedId, String authorId,
                                                               UUID viewerId, Pageable pageable) {
        UUID board = parseUuid(boardId);
        UUID linked = parseUuid(linkedId);
        UUID author = parseUuid(authorId);
        EasyPageResult<DiscussionTopic> page = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.status().eq("published");
                    if (board != null) {
                        t.boardId().eq(board);
                    }
                    if (linked != null && linkedType != null && !linkedType.isBlank()) {
                        t.linkedType().eq(linkedType);
                        t.linkedId().eq(linked);
                    }
                    if (author != null) {
                        t.authorId().eq(author);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        t.title().like(keyword.trim());
                    }
                })
                .orderBy(t -> {
                    t.pinned().desc();
                    switch (sort == null ? "latest" : sort) {
                        case "hot" -> {
                            t.replyCount().desc();
                            t.likeCount().desc();
                            t.createdAt().desc();
                        }
                        case "featured" -> {
                            t.featured().desc();
                            t.createdAt().desc();
                        }
                        default -> t.createdAt().desc();
                    }
                })
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());

        return new DefaultPageResult<>(page.getTotal(), toBriefList(page.getData(), viewerId));
    }

    @Override
    public DiscussionTopicDetailResource topic(String id, UUID viewerId, boolean admin) {
        DiscussionTopic topic = requireTopic(id);
        boolean mine = viewerId != null && viewerId.equals(topic.getAuthorId());
        if (!"published".equals(topic.getStatus()) && !mine && !admin) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        DiscussionBoard board = boardOf(topic.getBoardId());
        Map<UUID, UserAuth> authors = loadUsers(List.of(topic.getAuthorId()));
        UserAuth author = authors.get(topic.getAuthorId());
        String reviewNote = null;
        if (mine && !"published".equals(topic.getStatus())) {
            reviewNote = "pending".equals(topic.getStatus())
                    ? "内容正在审核中，仅你本人可见"
                    : "内容未通过审核：" + rejectReasonOf(TYPE_TOPIC, topic.getId());
        }
        return new DiscussionTopicDetailResource(
                topic.getId().toString(),
                topic.getBoardId().toString(),
                board == null ? null : board.getName(),
                topic.getTitle(),
                topic.getContent(),
                parseImages(topic.getImages()),
                authorOf(author, true),
                topic.getLang(),
                topic.getStatus(),
                topic.getPinned(),
                topic.getFeatured(),
                topic.getLocked(),
                topic.getLinkedType(),
                topic.getLinkedId() == null ? null : topic.getLinkedId().toString(),
                topic.getReplyCount(),
                topic.getLikeCount(),
                topic.getFavoriteCount(),
                topic.getViewCount(),
                format(topic.getCreatedAt()),
                format(topic.getEditedAt()),
                mine,
                reviewNote
        );
    }

    @Override
    @Transactional
    public String createTopic(UUID userId, DiscussionTopicCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发帖内容不能为空");
        }
        UserAuth user = requirePostableUser(userId);
        rateLimitService.consume("topic", userId.toString(), 3, 60, "发帖过于频繁，请稍后再试");

        UUID boardId = request.boardId();
        if (boardId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择板块");
        }
        DiscussionBoard board = entityQuery.queryable(DiscussionBoard.class)
                .where(b -> b.id().eq(boardId))
                .firstOrNull();
        if (board == null || !"active".equals(board.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "板块不存在或已关闭");
        }

        String title = required(request.title(), "标题不能为空").trim();
        if (title.length() < MIN_TITLE || title.length() > MAX_TITLE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题长度需在 2～200 字之间");
        }
        String content = required(request.content(), "正文不能为空").trim();
        if (content.length() > MAX_CONTENT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文过长（最多 20000 字）");
        }
        List<String> images = validateImages(request.images());

        SensitiveCheckResult check = sensitiveWordService.check(title + "\n" + content);
        String status = "published";
        if (check.blocked() || "review_then_post".equals(board.getPostPolicy())) {
            status = "pending";
        }

        UUID topicId = UUID.randomUUID();
        DiscussionTopic topic = new DiscussionTopic();
        topic.setId(topicId);
        topic.setBoardId(boardId);
        topic.setAuthorId(userId);
        topic.setTitle(title);
        topic.setContent(maskedBody(title, content, check));
        topic.setImages(writeImages(images));
        topic.setLang(defaultLang(request.lang(), user));
        topic.setStatus(status);
        topic.setPinned(false);
        topic.setFeatured(false);
        topic.setLocked(false);
        topic.setLinkedType(blankToNull(request.linkedType()));
        topic.setLinkedId(request.linkedId());
        topic.setReplyCount(0);
        topic.setLikeCount(0);
        topic.setFavoriteCount(0);
        topic.setViewCount(0L);
        topic.setFloorCount(1);
        topic.setRiskLevel(check.level());
        topic.setHitWords(check.hitWords());
        topic.setEditCount(0);
        topic.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(topic).executeRows();

        board.setTopicCount((board.getTopicCount() == null ? 0 : board.getTopicCount()) + 1);
        entityQuery.updatable(board).executeRows();

        if ("pending".equals(status)) {
            submitReview(TYPE_TOPIC, topicId, userId);
        } else {
            // 作者默认订阅自己的帖子（可在帖子里设为「免打扰」）；发帖即通知被 @ 的用户与板块订阅者
            subscriptionService.subscribe(userId, SubscriptionService.TYPE_TOPIC, topicId, SubscriptionService.LEVEL_ALL);
            notifyMentions(content, user, TYPE_TOPIC, topicId, title, truncate(content, 60));
            notifyBoardSubscribers(board, topicId, title, userId, user);
        }
        return topicId.toString();
    }

    @Override
    @Transactional
    public void updateTopic(UUID userId, String id, DiscussionTopicUpdateRequest request) {
        DiscussionTopic topic = requireTopic(id);
        if (!userId.equals(topic.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能编辑自己发布的帖子");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "编辑内容不能为空");
        }
        requirePostableUser(userId);
        String title = required(request.title(), "标题不能为空").trim();
        if (title.length() < MIN_TITLE || title.length() > MAX_TITLE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "标题长度需在 2～200 字之间");
        }
        String content = required(request.content(), "正文不能为空").trim();
        if (content.length() > MAX_CONTENT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "正文过长（最多 20000 字）");
        }
        SensitiveCheckResult check = sensitiveWordService.check(title + "\n" + content);
        topic.setTitle(title);
        topic.setContent(maskedBody(title, content, check));
        topic.setImages(writeImages(validateImages(request.images())));
        // 语言：请求未携带时保留原语言（否则编辑一篇英文帖会被误判成中文，影响翻译方向判断）
        if (request.lang() != null && !request.lang().isBlank()) {
            topic.setLang(defaultLang(request.lang(), null));
        } else if (topic.getLang() == null || topic.getLang().isBlank()) {
            topic.setLang("zh");
        }
        topic.setRiskLevel(check.level());
        topic.setHitWords(check.hitWords());
        topic.setEditedAt(LocalDateTime.now());
        topic.setEditCount((topic.getEditCount() == null ? 0 : topic.getEditCount()) + 1);
        // 编辑后若命中 block 级敏感词，重新进审核队列
        if (check.blocked()) {
            topic.setStatus("pending");
            submitReview(TYPE_TOPIC, topic.getId(), userId);
            notificationService.notify(userId, "review_result", null, TYPE_TOPIC, topic.getId(),
                    "帖子已进入审核", "你编辑的帖子《%s》含需人工复核的内容，审核通过后将重新公开。".formatted(title), null);
        }
        entityQuery.updatable(topic).executeRows();
    }

    @Override
    @Transactional
    public void deleteTopic(UUID userId, String id, boolean admin) {
        DiscussionTopic topic = requireTopic(id);
        if (!admin && !userId.equals(topic.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己发布的帖子");
        }
        if ("deleted".equals(topic.getStatus())) {
            return;
        }
        topic.setStatus("deleted");
        entityQuery.updatable(topic).executeRows();
        DiscussionBoard board = boardOf(topic.getBoardId());
        if (board != null) {
            board.setTopicCount(Math.max(0, (board.getTopicCount() == null ? 1 : board.getTopicCount()) - 1));
            entityQuery.updatable(board).executeRows();
        }
    }

    // ------------------------------------------------------------------ 楼层

    @Override
    public EasyPageResult<DiscussionPostResource> posts(String topicId, UUID viewerId, boolean admin, Pageable pageable) {
        DiscussionTopic topic = requireTopic(topicId);
        boolean mine = viewerId != null && viewerId.equals(topic.getAuthorId());
        if (!"published".equals(topic.getStatus()) && !mine && !admin) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        EasyPageResult<DiscussionPost> page = entityQuery.queryable(DiscussionPost.class)
                .where(p -> {
                    p.topicId().eq(topic.getId());
                    if (!admin && !mine) {
                        p.status().eq("published");
                    }
                })
                .orderBy(p -> p.floorNo().asc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());

        List<DiscussionPost> rows = page.getData();
        List<UUID> userIds = rows.stream().map(DiscussionPost::getAuthorId).distinct().toList();
        Map<UUID, UserAuth> users = loadUsers(userIds);
        Map<UUID, DiscussionPost> quotes = loadQuotes(rows);
        List<DiscussionPostResource> data = rows.stream()
                .map(p -> toPostResource(p, pick(users, p.getAuthorId()), pick(quotes, p.getQuotePostId()), users, viewerId))
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    @Override
    @Transactional
    public DiscussionPostResource createPost(UUID userId, String topicId, DiscussionPostCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }
        UserAuth user = requirePostableUser(userId);
        DiscussionTopic topic = requireTopic(topicId);
        boolean mine = userId.equals(topic.getAuthorId());
        if (!"published".equals(topic.getStatus()) && !mine) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不可回复");
        }
        if (Boolean.TRUE.equals(topic.getLocked())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该帖子已锁定，不再接受回复");
        }
        rateLimitService.consume("reply", userId.toString(), 8, 60, "回复过于频繁，请稍后再试");

        String content = required(request.content(), "回复内容不能为空").trim();
        if (content.length() > MAX_REPLY) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复过长（最多 5000 字）");
        }
        List<String> images = validateImages(request.images());
        SensitiveCheckResult check = sensitiveWordService.check(content);

        int replyNo = (topic.getReplyCount() == null ? 0 : topic.getReplyCount()) + 1;
        DiscussionPost post = new DiscussionPost();
        post.setId(UUID.randomUUID());
        post.setTopicId(topic.getId());
        post.setAuthorId(userId);
        post.setContent(check.cleaned() == null ? content : check.cleaned());
        post.setImages(writeImages(images));
        post.setLang(defaultLang(request.lang(), user));
        post.setStatus(check.blocked() ? "pending" : "published");
        post.setFloorNo(replyNo + 1);
        post.setQuotePostId(request.quotePostId());
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setRiskLevel(check.level());
        post.setHitWords(check.hitWords());
        post.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(post).executeRows();

        topic.setReplyCount(replyNo);
        topic.setFloorCount(replyNo + 1);
        topic.setLastReplyAt(LocalDateTime.now());
        topic.setLastReplyUserId(userId);
        entityQuery.updatable(topic).executeRows();

        if ("pending".equals(post.getStatus())) {
            submitReview(TYPE_POST, post.getId(), userId);
        } else {
            notifyReply(topic, post, request.quotePostId(), user);
        }
        Map<UUID, UserAuth> users = loadUsers(List.of(userId));
        return toPostResource(post, pick(users, userId), null, users, userId);
    }

    @Override
    @Transactional
    public void deletePost(UUID userId, String id, boolean admin) {
        DiscussionPost post = requirePost(id);
        if (!admin && !userId.equals(post.getAuthorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除自己的回复");
        }
        if ("deleted".equals(post.getStatus())) {
            return;
        }
        post.setStatus("deleted");
        entityQuery.updatable(post).executeRows();
        DiscussionTopic topic = requireTopic(post.getTopicId().toString());
        topic.setReplyCount(Math.max(0, (topic.getReplyCount() == null ? 1 : topic.getReplyCount()) - 1));
        entityQuery.updatable(topic).executeRows();
    }

    // ------------------------------------------------------------------ 其他

    @Override
    public List<DiscussionTopicBriefResource> linkedTopics(String linkedType, String linkedId, int size) {
        UUID linked = parseUuid(linkedId);
        if (linked == null || linkedType == null || linkedType.isBlank()) {
            return List.of();
        }
        List<DiscussionTopic> rows = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.linkedType().eq(linkedType);
                    t.linkedId().eq(linked);
                    t.status().eq("published");
                })
                .orderBy(t -> t.createdAt().desc())
                .limit(Math.max(1, Math.min(size, 20)))
                .toList();
        return toBriefList(rows, null);
    }

    @Override
    public EasyPageResult<DiscussionTopicBriefResource> myTopics(UUID userId, Pageable pageable) {
        EasyPageResult<DiscussionTopic> page = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.authorId().eq(userId);
                    t.status().ne("deleted");
                })
                .orderBy(t -> t.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
        return new DefaultPageResult<>(page.getTotal(), toBriefList(page.getData(), userId));
    }

    @Override
    public EasyPageResult<DiscussionPostResource> myPosts(UUID userId, Pageable pageable) {
        EasyPageResult<DiscussionPost> page = entityQuery.queryable(DiscussionPost.class)
                .where(p -> {
                    p.authorId().eq(userId);
                    p.status().ne("deleted");
                })
                .orderBy(p -> p.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
        List<DiscussionPost> rows = page.getData();
        Map<UUID, UserAuth> users = loadUsers(List.of(userId));
        Map<UUID, DiscussionPost> quotes = loadQuotes(rows);
        List<DiscussionPostResource> data = rows.stream()
                .map(p -> toPostResource(p, pick(users, userId), pick(quotes, p.getQuotePostId()), users, userId))
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    // ------------------------------------------------------------------ 内部方法

    /** 发帖/回复前的用户校验：封禁、禁言、账号状态 */
    UserAuth requirePostableUser(UUID userId) {
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被封禁至 " + format(user.getBannedUntil()));
        }
        if (user.getMutedUntil() != null && user.getMutedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号处于禁言状态，解除时间 " + format(user.getMutedUntil()));
        }
        return user;
    }

    /** 写入审核队列（复用 content_review） */
    void submitReview(String entryType, UUID entryId, UUID submitterId) {
        ContentReview review = new ContentReview(UUID.randomUUID(), entryType, entryId, "pending");
        review.setSubmitterId(submitterId);
        review.setSubmittedAt(LocalDateTime.now());
        entityQuery.insertable(review).executeRows();
    }

    private void notifyReply(DiscussionTopic topic, DiscussionPost post, UUID quotePostId, UserAuth actor) {
        String excerpt = truncate(post.getContent(), 60);
        String actorName = actor.getNickname() == null ? "有人" : actor.getNickname();

        // 接收者 = 订阅了该帖（level=all）的用户 + 楼主（默认接收，设为「免打扰」则跳过），排除发帖人自己
        Set<UUID> recipients = new LinkedHashSet<>(
                subscriptionService.subscribersOf(SubscriptionService.TYPE_TOPIC, topic.getId()));
        if (subscriptionService.wantsAll(topic.getAuthorId(), SubscriptionService.TYPE_TOPIC, topic.getId())) {
            recipients.add(topic.getAuthorId());
        }
        recipients.remove(actor.getId());
        for (UUID userId : recipients) {
            notificationService.notify(userId, "topic_reply", actor.getId(), TYPE_TOPIC, topic.getId(),
                    "你关注的帖子有新回复", "%s 回复了《%s》：%s".formatted(actorName, topic.getTitle(), excerpt), null);
        }

        if (quotePostId != null) {
            DiscussionPost quoted = entityQuery.queryable(DiscussionPost.class)
                    .where(p -> p.id().eq(quotePostId))
                    .firstOrNull();
            if (quoted != null
                    && subscriptionService.wantsAll(quoted.getAuthorId(), SubscriptionService.TYPE_TOPIC, topic.getId())) {
                notificationService.notify(quoted.getAuthorId(), "post_reply", actor.getId(), TYPE_TOPIC, topic.getId(),
                        "你的回复被引用", "%s 引用了你的回复：%s".formatted(actorName, excerpt), null);
            }
        }
        notifyMentions(post.getContent(), actor, TYPE_TOPIC, topic.getId(), topic.getTitle(), excerpt);
    }

    /** 新帖发布 → 通知板块订阅者（level=all） */
    private void notifyBoardSubscribers(DiscussionBoard board, UUID topicId, String title, UUID authorId, UserAuth actor) {
        String actorName = actor.getNickname() == null ? "有人" : actor.getNickname();
        for (UUID userId : subscriptionService.subscribersOf(SubscriptionService.TYPE_BOARD, board.getId())) {
            if (userId.equals(authorId)) {
                continue;
            }
            notificationService.notify(userId, "board_topic", authorId, TYPE_TOPIC, topicId,
                    "你订阅的板块有新帖", "%s 在「%s」发布了《%s》".formatted(actorName, board.getName(), title), null);
        }
    }

    /**
     * @提及通知：解析内容中的 `@昵称`，命中真实用户即发通知。
     * 被提及者、楼主与引用者可能重叠，重复通知由 NotificationService 侧不可控，这里按昵称去重一次。
     */
    private void notifyMentions(String content, UserAuth actor, String targetType, UUID targetId,
                               String topicTitle, String excerpt) {
        List<String> names = MentionParser.parse(content);
        if (names.isEmpty()) {
            return;
        }
        List<UserAuth> users = entityQuery.queryable(UserAuth.class)
                .where(u -> u.nickname().in(names))
                .toList();
        for (UserAuth user : users) {
            if (user.getId().equals(actor.getId())) {
                continue;
            }
            // 通知强度语义：off 免打扰（完全静默，含 @）；mention 仅被 @ 时通知（就是本路径）；all 全部通知
            String level = subscriptionService.levelOf(user.getId(), SubscriptionService.TYPE_TOPIC, targetId);
            if (SubscriptionService.LEVEL_OFF.equals(level)) {
                continue;
            }
            notificationService.notify(user.getId(), "mention", actor.getId(), targetType, targetId,
                    "有人在讨论中提到了你",
                    "%s 在《%s》中提到了你：%s".formatted(
                            actor.getNickname() == null ? "有人" : actor.getNickname(), topicTitle, excerpt), null);
        }
    }

    private List<DiscussionTopicBriefResource> toBriefList(List<DiscussionTopic> rows, UUID viewerId) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, UserAuth> users = loadUsers(rows.stream()
                .map(DiscussionTopic::getAuthorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<UUID, String> boardNames = boardNames(rows.stream()
                .map(DiscussionTopic::getBoardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return rows.stream()
                .map(t -> new DiscussionTopicBriefResource(
                        t.getId().toString(),
                        t.getBoardId().toString(),
                        pick(boardNames, t.getBoardId()),
                        t.getTitle(),
                        truncate(t.getContent(), 120),
                        parseImages(t.getImages()),
                        authorOf(pick(users, t.getAuthorId()), viewerId != null && viewerId.equals(t.getAuthorId())),
                        t.getLang(),
                        t.getStatus(),
                        t.getPinned(),
                        t.getFeatured(),
                        t.getLocked(),
                        t.getReplyCount(),
                        t.getLikeCount(),
                        t.getViewCount(),
                        format(t.getLastReplyAt()),
                        format(t.getCreatedAt()),
                        viewerId != null && viewerId.equals(t.getAuthorId())
                ))
                .toList();
    }

    private DiscussionPostResource toPostResource(DiscussionPost post, UserAuth author, DiscussionPost quoted,
                                                  Map<UUID, UserAuth> users, UUID viewerId) {
        UserAuth quoteAuthor = quoted == null ? null : pick(users, quoted.getAuthorId());
        return new DiscussionPostResource(
                post.getId().toString(),
                post.getTopicId().toString(),
                post.getFloorNo(),
                post.getContent(),
                parseImages(post.getImages()),
                authorOf(author, false),
                post.getLang(),
                post.getStatus(),
                post.getLikeCount(),
                post.getQuotePostId() == null ? null : post.getQuotePostId().toString(),
                quoted == null ? null : truncate(quoted.getContent(), 80),
                quoteAuthor == null ? null : quoteAuthor.getNickname(),
                format(post.getCreatedAt()),
                viewerId != null && viewerId.equals(post.getAuthorId())
        );
    }

    private Map<UUID, DiscussionPost> loadQuotes(List<DiscussionPost> posts) {
        List<UUID> ids = posts.stream()
                .map(DiscussionPost::getQuotePostId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        // 注意：返回可变 HashMap —— 引用楼层可能为空，空键查询在不可变 Map 上会抛 NPE
        Map<UUID, DiscussionPost> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        entityQuery.queryable(DiscussionPost.class)
                .where(p -> p.id().in(ids))
                .toList()
                .forEach(p -> map.put(p.getId(), p));
        return map;
    }

    /** 空键安全的取值（UUID 可能为 null，不可变 Map 不接受空键查询） */
    private <V> V pick(Map<UUID, V> map, UUID key) {
        return key == null ? null : map.get(key);
    }

    private Map<UUID, UserAuth> loadUsers(List<UUID> ids) {
        List<UUID> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return new HashMap<>();
        }
        Map<UUID, UserAuth> map = new HashMap<>();
        entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().in(distinct))
                .toList()
                .forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private Map<UUID, String> boardNames(List<UUID> boardIds) {
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> map = new HashMap<>();
        entityQuery.queryable(DiscussionBoard.class)
                .where(b -> b.id().in(boardIds))
                .toList()
                .forEach(b -> map.put(b.getId(), b.getName()));
        return map;
    }

    private DiscussionBoard boardOf(UUID boardId) {
        if (boardId == null) {
            return null;
        }
        return entityQuery.queryable(DiscussionBoard.class)
                .where(b -> b.id().eq(boardId))
                .firstOrNull();
    }

    private DiscussionAuthorResource authorOf(UserAuth user, boolean owner) {
        if (user == null) {
            return new DiscussionAuthorResource(null, "已注销用户", null, 0, owner);
        }
        return new DiscussionAuthorResource(
                user.getId().toString(),
                user.getNickname() == null || user.getNickname().isBlank() ? "民族之友" : user.getNickname(),
                user.getAvatar(),
                user.getTrustLevel() == null ? 0 : user.getTrustLevel(),
                owner
        );
    }

    private DiscussionTopic requireTopic(String id) {
        UUID uuid = parseUuid(id);
        if (uuid == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "帖子 ID 不正确");
        }
        DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.id().eq(uuid))
                .firstOrNull();
        if (topic == null || "deleted".equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在或已删除");
        }
        return topic;
    }

    private DiscussionPost requirePost(String id) {
        UUID uuid = parseUuid(id);
        if (uuid == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复 ID 不正确");
        }
        DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                .where(p -> p.id().eq(uuid))
                .firstOrNull();
        if (post == null || "deleted".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "回复不存在或已删除");
        }
        return post;
    }

    private String rejectReasonOf(String entryType, UUID entryId) {
        ContentReview review = entityQuery.queryable(ContentReview.class)
                .where(r -> {
                    r.entryType().eq(entryType);
                    r.entryId().eq(entryId);
                })
                .orderBy(r -> r.submittedAt().desc())
                .firstOrNull();
        if (review == null || review.getRejectReason() == null || review.getRejectReason().isBlank()) {
            return "内容不符合社区规范";
        }
        return review.getRejectReason();
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
                throw new BusinessException(ErrorCode.PARAM_ERROR, "最多上传 " + MAX_IMAGES + " 张图片");
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

    private String defaultLang(String lang, UserAuth user) {
        if (lang != null && !lang.isBlank()) {
            return lang.trim().length() > 8 ? lang.trim().substring(0, 8) : lang.trim();
        }
        if (user != null && user.getLang() != null && !user.getLang().isBlank()) {
            return user.getLang();
        }
        return "zh";
    }

    /**
     * 敏感词检测是对「标题 + 正文」整体做的，replace 级掩码后的文本会带上标题前缀；
     * 这里剥掉标题前缀，只保留（可能被掩码的）正文，避免把标题写进正文里。
     */
    private String maskedBody(String title, String content, SensitiveCheckResult check) {
        String cleaned = check.cleaned();
        if (cleaned == null) {
            return content;
        }
        String prefix = title + "\n";
        return cleaned.startsWith(prefix) ? cleaned.substring(prefix.length()) : cleaned;
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        String flat = text.replaceAll("\\s+", " ").trim();
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
