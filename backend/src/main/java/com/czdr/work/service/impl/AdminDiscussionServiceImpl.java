package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.ContentReview;
import com.czdr.work.model.entity.DiscussionBoard;
import com.czdr.work.model.entity.DiscussionModerationLog;
import com.czdr.work.model.entity.DiscussionPost;
import com.czdr.work.model.entity.DiscussionReport;
import com.czdr.work.model.entity.DiscussionTopic;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.request.DiscussionHandleRequest;
import com.czdr.work.model.request.DiscussionReviewRequest;
import com.czdr.work.model.resource.DiscussionReportResource;
import com.czdr.work.model.resource.DiscussionReviewItemResource;
import com.czdr.work.service.AdminDiscussionService;
import com.czdr.work.service.NotificationService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * 讨论区治理实现：先发后审 + 举报 + 人工处置。
 * 所有处置动作都会写 discussion_moderation_log（合规审计）并通知相关用户。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDiscussionServiceImpl implements AdminDiscussionService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EasyEntityQuery entityQuery;
    private final NotificationService notificationService;
    private final com.czdr.work.service.RateLimitService rateLimitService;

    /** 单次公告收件人上限（防止误操作把全站打爆） */
    @org.springframework.beans.factory.annotation.Value("${app.notify.broadcast-max-recipients:2000}")
    private int broadcastMaxRecipients;

    /** 「活跃用户」受众的判定窗口（天） */
    @org.springframework.beans.factory.annotation.Value("${app.notify.active-window-days:30}")
    private int activeWindowDays;

    // ---------------------------------------------------------------- 审核队列

    @Override
    public EasyPageResult<DiscussionReviewItemResource> reviewQueue(String targetType, String status, String sort, Pageable pageable) {
        boolean posts = DiscussionServiceImpl.TYPE_POST.equals(targetType);
        String mode = status == null || status.isBlank() ? "pending" : status;
        boolean priority = "priority".equalsIgnoreCase(sort);
        // 优先级视图：block 级敏感词 → 被举报多 → 最早提交。
        // 需要在内存中排序，故限制扫描窗口（≤300 条），文档中已注明该取舍。
        int size = pageable.getPageSize();
        int window = priority ? Math.min(300, (pageable.getPageNumber() + 3) * Math.max(1, size)) : 0;
        int pageNo = priority ? 1 : pageable.getPageNumber() + 1;
        int pageSize = priority ? Math.max(1, window) : size;

        if (posts) {
            EasyPageResult<DiscussionPost> page = entityQuery.queryable(DiscussionPost.class)
                    .where(p -> {
                        if ("all".equals(mode)) {
                            p.id().isNotNull();
                        } else if ("watch".equals(mode)) {
                            p.riskLevel().eq("watch");
                        } else {
                            p.status().eq("pending");
                        }
                    })
                    .orderBy(p -> p.createdAt().asc())
                    .toPageResult(pageNo, pageSize);
            List<DiscussionPost> rows = new ArrayList<>(page.getData());
            Map<UUID, DiscussionTopic> topics = topicsOf(rows.stream().map(DiscussionPost::getTopicId).toList());
            Map<UUID, UserAuth> users = usersOf(rows.stream().map(DiscussionPost::getAuthorId).toList());
            Map<UUID, Long> reportCounts = reportCounts(DiscussionServiceImpl.TYPE_POST,
                    rows.stream().map(DiscussionPost::getId).toList());
            List<DiscussionReviewItemResource> data = rows.stream()
                    .map(p -> {
                        DiscussionTopic topic = topics.get(p.getTopicId());
                        UserAuth author = users.get(p.getAuthorId());
                        return new DiscussionReviewItemResource(
                                DiscussionServiceImpl.TYPE_POST,
                                p.getId().toString(),
                                "第 %d 楼".formatted(p.getFloorNo() == null ? 1 : p.getFloorNo()),
                                excerpt(p.getContent(), 200),
                                topic == null ? null : "《" + topic.getTitle() + "》",
                                p.getAuthorId() == null ? null : p.getAuthorId().toString(),
                                nickname(author),
                                p.getRiskLevel(),
                                p.getHitWords(),
                                reportCounts.getOrDefault(p.getId(), 0L),
                                p.getStatus(),
                                format(p.getCreatedAt())
                        );
                    })
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if (priority) {
                data.sort(priorityComparator(page.getData()));
                data = new ArrayList<>(slice(data, pageable));
            }
            return new DefaultPageResult<>(priority && page.getTotal() > window ? page.getTotal() : page.getTotal(), data);
        }

        EasyPageResult<DiscussionTopic> page = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.status().ne("deleted");
                    if ("all".equals(mode)) {
                        t.id().isNotNull();
                    } else if ("watch".equals(mode)) {
                        t.riskLevel().eq("watch");
                    } else {
                        t.status().eq("pending");
                    }
                })
                .orderBy(t -> t.createdAt().asc())
                .toPageResult(pageNo, pageSize);
        List<DiscussionTopic> rows = page.getData();
        Map<UUID, UserAuth> users = usersOf(rows.stream().map(DiscussionTopic::getAuthorId).toList());
        Map<UUID, String> boards = boardNames(rows.stream().map(DiscussionTopic::getBoardId).toList());
        Map<UUID, Long> reportCounts = reportCounts(DiscussionServiceImpl.TYPE_TOPIC,
                rows.stream().map(DiscussionTopic::getId).toList());
        List<DiscussionReviewItemResource> data = rows.stream()
                .map(t -> new DiscussionReviewItemResource(
                        DiscussionServiceImpl.TYPE_TOPIC,
                        t.getId().toString(),
                        t.getTitle(),
                        excerpt(t.getContent(), 200),
                        boards.get(t.getBoardId()),
                        t.getAuthorId() == null ? null : t.getAuthorId().toString(),
                        nickname(users.get(t.getAuthorId())),
                        t.getRiskLevel(),
                        t.getHitWords(),
                        reportCounts.getOrDefault(t.getId(), 0L),
                        t.getStatus(),
                        format(t.getCreatedAt())
                ))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (priority) {
            data.sort(priorityComparator(rows));
            data = new ArrayList<>(slice(data, pageable));
        }
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    /** 优先级排序：block 级敏感词 > 被举报次数多 > 提交更早 */
    private java.util.Comparator<DiscussionReviewItemResource> priorityComparator(List<?> rows) {
        return java.util.Comparator
                .comparingInt((DiscussionReviewItemResource r) -> "block".equals(r.riskLevel()) ? 0 : ("watch".equals(r.riskLevel()) ? 1 : 2))
                .thenComparing(r -> -r.reportCount())
                .thenComparing(r -> r.createdAt() == null ? "" : r.createdAt());
    }

    private List<DiscussionReviewItemResource> slice(List<DiscussionReviewItemResource> data, Pageable pageable) {
        int from = pageable.getPageNumber() * pageable.getPageSize();
        if (from >= data.size()) {
            return List.of();
        }
        int to = Math.min(data.size(), from + pageable.getPageSize());
        return data.subList(from, to);
    }

    @Override
    @Transactional
    public Map<String, Object> reviewBatch(String operatorId, String targetType, List<String> ids,
                                          String status, String reason) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要处置的内容");
        }
        if (ids.size() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单次最多处置 100 条");
        }
        int success = 0;
        List<String> failures = new ArrayList<>();
        for (String id : ids) {
            try {
                review(operatorId, new DiscussionReviewRequest(targetType, id, status, reason));
                success++;
            } catch (Exception e) {
                failures.add(id + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failures.size());
        result.put("failures", failures.stream().limit(10).toList());
        return result;
    }

    @Override
    @Transactional
    public void review(String operatorId, DiscussionReviewRequest request) {
        if (request == null || request.targetType() == null || request.targetId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核参数不完整");
        }
        boolean approved = "approved".equals(request.status());
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (!approved && reason.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回时必须填写理由（会通知作者）");
        }
        UUID targetId = UUID.fromString(request.targetId());
        String type = request.targetType();
        UUID authorId;
        String title;

        if (DiscussionServiceImpl.TYPE_POST.equals(type)) {
            DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                    .where(p -> p.id().eq(targetId))
                    .firstOrNull();
            if (post == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "回复不存在");
            }
            post.setStatus(approved ? "published" : "hidden");
            post.setRiskLevel("none");
            entityQuery.updatable(post).executeRows();
            authorId = post.getAuthorId();
            title = "你的回复";
            if (approved) {
                DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                        .where(t -> t.id().eq(post.getTopicId()))
                        .firstOrNull();
                if (topic != null) {
                    notificationService.notify(topic.getAuthorId(), "topic_reply", post.getAuthorId(),
                            DiscussionServiceImpl.TYPE_TOPIC, topic.getId(), "帖子有新回复",
                            "%s 的回复已通过审核：%s".formatted(nickname(userOf(post.getAuthorId())), excerpt(post.getContent(), 60)), null);
                }
            }
        } else {
            DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                    .where(t -> t.id().eq(targetId))
                    .firstOrNull();
            if (topic == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
            }
            topic.setStatus(approved ? "published" : "rejected");
            topic.setRiskLevel("none");
            entityQuery.updatable(topic).executeRows();
            authorId = topic.getAuthorId();
            title = "你的帖子《" + topic.getTitle() + "》";
        }

        finishReview(type, targetId, operatorId, approved, reason);
        writeLog(type, targetId, approved ? "review_approved" : "review_rejected", operatorId, reason, null);
        notificationService.notify(authorId, "review_result", null, type, targetId,
                approved ? "内容审核通过" : "内容未通过审核",
                approved ? title + "已通过审核，现已公开可见。" : title + "未通过审核：" + reason, null);
    }

    private void finishReview(String entryType, UUID entryId, String operatorId, boolean approved, String reason) {
        ContentReview existing = entityQuery.queryable(ContentReview.class)
                .where(r -> {
                    r.entryType().eq(entryType);
                    r.entryId().eq(entryId);
                })
                .orderBy(r -> r.submittedAt().desc())
                .firstOrNull();
        UUID reviewer = operatorId == null ? null : UUID.fromString(operatorId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            ContentReview created = new ContentReview(UUID.randomUUID(), entryType, entryId,
                    approved ? "approved" : "rejected");
            created.setReviewerId(reviewer);
            created.setRejectReason(approved ? null : reason);
            created.setSubmittedAt(now);
            created.setReviewedAt(now);
            entityQuery.insertable(created).executeRows();
            return;
        }
        existing.setStatus(approved ? "approved" : "rejected");
        existing.setRejectReason(approved ? null : reason);
        existing.setReviewerId(reviewer);
        existing.setReviewedAt(now);
        entityQuery.updatable(existing).executeRows();
    }

    // ---------------------------------------------------------------- 举报

    @Override
    public EasyPageResult<DiscussionReportResource> reports(String status, Pageable pageable) {
        String mode = status == null || status.isBlank() ? "pending" : status;
        EasyPageResult<DiscussionReport> page = entityQuery.queryable(DiscussionReport.class)
                .where(r -> {
                    if (!"all".equals(mode)) {
                        r.status().eq(mode);
                    }
                })
                .orderBy(r -> r.createdAt().asc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());

        List<DiscussionReport> rows = page.getData();
        Map<UUID, UserAuth> users = usersOf(stream(rows.stream().map(DiscussionReport::getReporterId),
                rows.stream().map(DiscussionReport::getHandledBy)));
        List<DiscussionReportResource> data = rows.stream()
                .map(r -> {
                    TargetInfo target = targetInfo(r.getTargetType(), r.getTargetId());
                    return new DiscussionReportResource(
                            r.getId().toString(),
                            r.getTargetType(),
                            r.getTargetId().toString(),
                            target.excerpt(),
                            target.authorName(),
                            target.path(),
                            nickname(users.get(r.getReporterId())),
                            r.getReason(),
                            r.getDetail(),
                            r.getStatus(),
                            r.getResultNote(),
                            format(r.getCreatedAt()),
                            format(r.getHandledAt())
                    );
                })
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    @Override
    @Transactional
    public void handleReport(String operatorId, String reportId, DiscussionHandleRequest request) {
        UUID id = UUID.fromString(reportId);
        DiscussionReport report = entityQuery.queryable(DiscussionReport.class)
                .where(r -> r.id().eq(id))
                .firstOrNull();
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "举报记录不存在");
        }
        if (!"pending".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "该举报已处理");
        }
        boolean accepted = request == null || !"rejected".equals(request.status());
        report.setStatus(accepted ? "accepted" : "rejected");
        report.setResultNote(request == null ? null : request.note());
        report.setHandledBy(operatorId == null ? null : UUID.fromString(operatorId));
        report.setHandledAt(LocalDateTime.now());
        entityQuery.updatable(report).executeRows();

        String action = request == null ? null : request.contentAction();
        UUID authorId = null;
        if (accepted && action != null && !"none".equals(action)) {
            authorId = applyContentAction(report.getTargetType(), report.getTargetId(),
                    "delete".equals(action) ? "deleted" : "hidden", operatorId,
                    request.note() == null ? "举报成立" : request.note());
        }
        if (accepted && request != null && request.muteDays() != null && request.muteDays() > 0 && authorId != null) {
            muteUser(operatorId, authorId.toString(), request.muteDays(),
                    request.note() == null ? "举报成立" : request.note());
        }
        notificationService.notify(report.getReporterId(), "report_result", null,
                report.getTargetType(), report.getTargetId(),
                accepted ? "举报已受理" : "举报未通过",
                accepted ? "感谢反馈，我们已处理你举报的内容。" : "经核实，你举报的内容暂未违反社区规范，感谢反馈。", null);
    }

    // ---------------------------------------------------------------- 用户与内容处置

    @Override
    @Transactional
    public void muteUser(String operatorId, String userId, Integer days, String reason) {
        UUID uid = UUID.fromString(userId);
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(uid))
                .firstOrNull();
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        boolean unmute = days == null || days <= 0;
        user.setMutedUntil(unmute ? null : LocalDateTime.now().plusDays(days));
        entityQuery.updatable(user).executeRows();
        writeLog("user", uid, unmute ? "unmute" : "mute", operatorId, reason, null);
        notificationService.notify(uid, "system", null, null, null,
                unmute ? "禁言已解除" : "你已被禁言",
                unmute ? "你的发言权限已恢复。" : "因违反社区规范，你被禁言 %d 天。%s".formatted(days, reason == null ? "" : reason), null);
    }

    @Override
    @Transactional
    public void setFlag(String operatorId, String topicId, String flag, boolean value) {
        DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.id().eq(UUID.fromString(topicId)))
                .firstOrNull();
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        switch (flag == null ? "" : flag) {
            case "pinned" -> topic.setPinned(value);
            case "featured" -> topic.setFeatured(value);
            case "locked" -> topic.setLocked(value);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的标记：" + flag);
        }
        entityQuery.updatable(topic).executeRows();
        writeLog(DiscussionServiceImpl.TYPE_TOPIC, topic.getId(), flag + "=" + value, operatorId, null, null);
    }

    @Override
    @Transactional
    public void hideContent(String operatorId, String targetType, String targetId, String reason) {
        UUID authorId = applyContentAction(targetType, UUID.fromString(targetId), "hidden", operatorId, reason);
        if (authorId != null) {
            notificationService.notify(authorId, "review_result", null, targetType, UUID.fromString(targetId),
                    "内容已被隐藏", "你的内容因违反社区规范已被隐藏。" + (reason == null ? "" : "理由：" + reason), null);
        }
    }

    private UUID applyContentAction(String targetType, UUID targetId, String status, String operatorId, String reason) {
        if (DiscussionServiceImpl.TYPE_POST.equals(targetType)) {
            DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                    .where(p -> p.id().eq(targetId))
                    .firstOrNull();
            if (post == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "回复不存在");
            }
            post.setStatus(status);
            entityQuery.updatable(post).executeRows();
            writeLog(targetType, targetId, status, operatorId, reason, excerpt(post.getContent(), 300));
            return post.getAuthorId();
        }
        DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.id().eq(targetId))
                .firstOrNull();
        if (topic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
        topic.setStatus(status);
        entityQuery.updatable(topic).executeRows();
        writeLog(targetType, targetId, status, operatorId, reason, excerpt(topic.getTitle() + " " + topic.getContent(), 300));
        return topic.getAuthorId();
    }

    // ---------------------------------------------------------------- 板块管理

    @Override
    public List<Map<String, Object>> boards() {
        return entityQuery.queryable(DiscussionBoard.class)
                .orderBy(b -> b.orderNum().asc())
                .toList()
                .stream()
                .map(b -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", b.getId().toString());
                    row.put("slug", b.getSlug());
                    row.put("name", b.getName());
                    row.put("nameEn", b.getNameEn());
                    row.put("description", b.getDescription());
                    row.put("orderNum", b.getOrderNum());
                    row.put("status", b.getStatus());
                    row.put("postPolicy", b.getPostPolicy());
                    row.put("topicCount", b.getTopicCount());
                    row.put("createdAt", format(b.getCreatedAt()));
                    return row;
                })
                .toList();
    }

    @Override
    @Transactional
    public String saveBoard(String operatorId, Map<String, Object> payload) {
        if (payload == null || payload.get("name") == null || String.valueOf(payload.get("name")).isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块名称不能为空");
        }
        String name = String.valueOf(payload.get("name")).trim();
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "板块名称过长（最多 64 字）");
        }
        String id = payload.get("id") == null ? null : String.valueOf(payload.get("id"));
        String policy = payload.get("postPolicy") == null ? "post_then_review" : String.valueOf(payload.get("postPolicy"));
        if (!"post_then_review".equals(policy) && !"review_then_post".equals(policy)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "发帖策略不合法");
        }
        String description = payload.get("description") == null ? null : String.valueOf(payload.get("description"));
        String nameEn = payload.get("nameEn") == null ? null : String.valueOf(payload.get("nameEn"));
        int orderNum = payload.get("orderNum") == null ? 0 : Integer.parseInt(String.valueOf(payload.get("orderNum")));

        DiscussionBoard board;
        if (id == null || id.isBlank() || "null".equals(id)) {
            board = new DiscussionBoard();
            board.setId(UUID.randomUUID());
            board.setSlug(slugify(name));
            board.setStatus("active");
            board.setMinTrustLevel(0);
            board.setTopicCount(0);
            board.setCreatedAt(LocalDateTime.now());
        } else {
            board = entityQuery.queryable(DiscussionBoard.class)
                    .where(b -> b.id().eq(UUID.fromString(id)))
                    .firstOrNull();
            if (board == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "板块不存在");
            }
        }
        board.setName(name);
        board.setNameEn(nameEn);
        board.setDescription(description);
        board.setOrderNum(orderNum);
        board.setPostPolicy(policy);
        board.setUpdatedAt(LocalDateTime.now());
        if (entityQuery.queryable(DiscussionBoard.class).where(b -> b.id().eq(board.getId())).firstOrNull() == null) {
            entityQuery.insertable(board).executeRows();
        } else {
            entityQuery.updatable(board).executeRows();
        }
        writeLog("board", board.getId(), "save", operatorId, name, null);
        return board.getId().toString();
    }

    @Override
    @Transactional
    public void toggleBoard(String operatorId, String boardId, boolean active) {
        DiscussionBoard board = entityQuery.queryable(DiscussionBoard.class)
                .where(b -> b.id().eq(UUID.fromString(boardId)))
                .firstOrNull();
        if (board == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "板块不存在");
        }
        board.setStatus(active ? "active" : "hidden");
        board.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(board).executeRows();
        writeLog("board", board.getId(), active ? "enable" : "disable", operatorId, null, null);
    }

    /** 板块标识：由名称生成 slug（中文名退化为 board-N），保证唯一 */
    private String slugify(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "board";
        }
        String slug = base;
        int suffix = 1;
        while (true) {
            final String candidate = slug;
            boolean taken = entityQuery.queryable(DiscussionBoard.class)
                    .where(b -> b.slug().eq(candidate))
                    .firstOrNull() != null;
            if (!taken) {
                return candidate;
            }
            slug = base + "-" + (++suffix);
        }
    }

    // ---------------------------------------------------------------- 看板

    @Override
    public Map<String, Object> stats() {
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pendingTopics", countTopics("pending"));
        map.put("pendingPosts", countPosts("pending"));
        map.put("pendingReports", countReports("pending"));
        map.put("publishedTopics", countTopics("published"));
        map.put("watchTopics", countWatchTopics());
        map.put("todayTopics", countTopicsSince(dayStart));
        map.put("todayPosts", countPostsSince(dayStart));
        return map;
    }

    private long countTopics(String status) {
        return entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.status().eq(status);
                    if ("published".equals(status)) {
                        t.id().isNotNull();
                    }
                })
                .toPageResult(1, 1)
                .getTotal();
    }

    private long countWatchTopics() {
        return entityQuery.queryable(DiscussionTopic.class)
                .where(t -> {
                    t.riskLevel().eq("watch");
                    t.status().eq("published");
                })
                .toPageResult(1, 1)
                .getTotal();
    }

    private long countPosts(String status) {
        return entityQuery.queryable(DiscussionPost.class)
                .where(p -> p.status().eq(status))
                .toPageResult(1, 1)
                .getTotal();
    }

    private long countReports(String status) {
        return entityQuery.queryable(DiscussionReport.class)
                .where(r -> r.status().eq(status))
                .toPageResult(1, 1)
                .getTotal();
    }

    private long countTopicsSince(LocalDateTime since) {
        return entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.createdAt().ge(since))
                .toPageResult(1, 1)
                .getTotal();
    }

    private long countPostsSince(LocalDateTime since) {
        return entityQuery.queryable(DiscussionPost.class)
                .where(p -> p.createdAt().ge(since))
                .toPageResult(1, 1)
                .getTotal();
    }

    // ---------------------------------------------------------------- 站内公告（OA 群发）

    @Override
    public Map<String, Object> broadcastAudience() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("all", audienceCount("all"));
        map.put("active", audienceCount("active"));
        map.put("maxRecipients", broadcastMaxRecipients);
        map.put("activeWindowDays", activeWindowDays);
        return map;
    }

    @Override
    public Map<String, Object> broadcast(String operatorId, com.czdr.work.model.request.BroadcastRequest request) {
        String title = request == null || request.title() == null ? "" : request.title().trim();
        String content = request == null || request.content() == null ? "" : request.content().trim();
        if (title.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告标题不能为空");
        }
        if (title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告标题不能超过 80 字");
        }
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告正文不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公告正文不能超过 500 字");
        }
        String audience = request != null && "active".equalsIgnoreCase(request.audience()) ? "active" : "all";
        boolean email = request != null && Boolean.TRUE.equals(request.email());
        String link = request == null || request.link() == null ? null : request.link().trim();
        if (link != null && !link.isEmpty() && !link.startsWith("/")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "跳转链接需为站内路径（以 / 开头）");
        }
        // 全站广播是最敏感的操作之一：限流 + 审计
        rateLimitService.consume("broadcast", operatorId, 10, 3600, "公告发送过于频繁（每小时最多 10 次）");

        List<UUID> recipients = audienceIds(audience);
        if (recipients.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前受众没有可发送的用户");
        }
        int written = notificationService.broadcast(recipients, title, content, link, email);
        UUID broadcastId = UUID.randomUUID();
        writeLog("broadcast", broadcastId, "broadcast", operatorId,
                title + "｜受众=" + audience + "｜收件人=" + written + "｜邮件=" + email,
                "{\"title\":\"" + title.replace("\"", "'") + "\",\"audience\":\"" + audience
                        + "\",\"recipients\":" + written + ",\"email\":" + email
                        + ",\"link\":\"" + (link == null ? "" : link) + "\"}");
        log.info("站内公告已发送：{}（受众 {}，收件人 {}，邮件 {}）", title, audience, written, email);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("broadcastId", broadcastId.toString());
        result.put("title", title);
        result.put("audience", audience);
        result.put("recipients", written);
        result.put("email", email);
        return result;
    }

    private long audienceCount(String audience) {
        LocalDateTime since = LocalDateTime.now().minusDays(activeWindowDays);
        boolean active = "active".equals(audience);
        return entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    u.status().eq("active");
                    if (active) {
                        u.lastActiveAt().ge(since);
                    }
                })
                .count();
    }

    private List<UUID> audienceIds(String audience) {
        LocalDateTime since = LocalDateTime.now().minusDays(activeWindowDays);
        boolean active = "active".equals(audience);
        EasyPageResult<UserAuth> page = entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    u.status().eq("active");
                    if (active) {
                        u.lastActiveAt().ge(since);
                    }
                })
                .toPageResult(1, broadcastMaxRecipients + 1);
        if (page.getTotal() > broadcastMaxRecipients) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "受众数量 %d 超过单次上限 %d，请缩小受众范围".formatted(page.getTotal(), broadcastMaxRecipients));
        }
        return page.getData().stream().map(UserAuth::getId).filter(Objects::nonNull).toList();
    }

    // ---------------------------------------------------------------- 工具

    private void writeLog(String targetType, UUID targetId, String action, String operatorId, String reason, String snapshot) {
        DiscussionModerationLog entity = new DiscussionModerationLog();
        entity.setId(UUID.randomUUID());
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setAction(action);
        entity.setOperatorId(operatorId == null ? null : UUID.fromString(operatorId));
        entity.setReason(reason == null ? null : (reason.length() > 300 ? reason.substring(0, 300) : reason));
        entity.setSnapshot(snapshot);
        entity.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(entity).executeRows();
    }

    private TargetInfo targetInfo(String targetType, UUID targetId) {
        if (DiscussionServiceImpl.TYPE_POST.equals(targetType)) {
            DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                    .where(p -> p.id().eq(targetId))
                    .firstOrNull();
            if (post == null) {
                return new TargetInfo(null, null, null);
            }
            UserAuth author = userOf(post.getAuthorId());
            return new TargetInfo(excerpt(post.getContent(), 120), nickname(author),
                    "/discussion/topic/" + post.getTopicId());
        }
        DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.id().eq(targetId))
                .firstOrNull();
        if (topic == null) {
            return new TargetInfo(null, null, null);
        }
        return new TargetInfo(excerpt(topic.getTitle() + " " + topic.getContent(), 120),
                nickname(userOf(topic.getAuthorId())), "/discussion/topic/" + topic.getId());
    }

    private Map<UUID, Long> reportCounts(String targetType, List<UUID> targetIds) {
        List<UUID> ids = targetIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> map = new HashMap<>();
        List<DiscussionReport> reports = entityQuery.queryable(DiscussionReport.class)
                .where(r -> {
                    r.targetType().eq(targetType);
                    r.targetId().in(ids);
                })
                .toList();
        reports.forEach(r -> map.merge(r.getTargetId(), 1L, Long::sum));
        return map;
    }

    private Map<UUID, DiscussionTopic> topicsOf(List<UUID> ids) {
        List<UUID> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.id().in(distinct))
                .toList()
                .stream()
                .collect(Collectors.toMap(DiscussionTopic::getId, t -> t, (a, b) -> a));
    }

    private Map<UUID, UserAuth> usersOf(List<UUID> ids) {
        List<UUID> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UserAuth> map = new HashMap<>();
        entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().in(distinct))
                .toList()
                .forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private Map<UUID, String> boardNames(List<UUID> ids) {
        List<UUID> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> map = new HashMap<>();
        entityQuery.queryable(DiscussionBoard.class)
                .where(b -> b.id().in(distinct))
                .toList()
                .forEach(b -> map.put(b.getId(), b.getName()));
        return map;
    }

    private UserAuth userOf(UUID id) {
        if (id == null) {
            return null;
        }
        return entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(id))
                .firstOrNull();
    }

    private String nickname(UserAuth user) {
        if (user == null) {
            return "已注销用户";
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? "民族之友" : user.getNickname();
    }

    private String excerpt(String text, int max) {
        if (text == null) {
            return null;
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() > max ? flat.substring(0, max) + "…" : flat;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    @SafeVarargs
    private List<UUID> stream(java.util.stream.Stream<UUID>... streams) {
        return java.util.Arrays.stream(streams)
                .flatMap(s -> s)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private record TargetInfo(String excerpt, String authorName, String path) {
    }
}
