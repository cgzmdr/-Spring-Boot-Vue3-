package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.DiscussionPost;
import com.czdr.work.model.entity.DiscussionReport;
import com.czdr.work.model.entity.DiscussionTopic;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.request.DiscussionReportRequest;
import com.czdr.work.model.resource.DiscussionReportResource;
import com.czdr.work.service.RateLimitService;
import com.czdr.work.service.ReportService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import com.easy.query.core.api.pagination.DefaultPageResult;
import com.easy.query.core.api.pagination.EasyPageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 举报实现：先发后审模式下的用户侧发现通道。
 * 同一用户对同一内容只能举报一次；举报内容进入后台工作台，处理结果回执给举报人。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final Set<String> REASONS = Set.of("spam", "abuse", "porn", "political", "copyright", "other");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EasyEntityQuery entityQuery;
    private final RateLimitService rateLimitService;

    @Override
    public void report(UUID userId, DiscussionReportRequest request) {
        if (request == null || request.targetType() == null || request.targetId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "举报参数不完整");
        }
        String type = request.targetType();
        if (!DiscussionServiceImpl.TYPE_TOPIC.equals(type) && !DiscussionServiceImpl.TYPE_POST.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的举报对象");
        }
        String reason = request.reason() == null ? "other" : request.reason().trim();
        if (!REASONS.contains(reason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "举报原因不合法");
        }
        String detail = request.detail() == null ? null : request.detail().trim();
        if (detail != null && detail.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "补充说明过长（最多 500 字）");
        }
        rateLimitService.consume("report", userId.toString(), 20, 86400, "今日举报次数已达上限");

        // 校验目标存在，且不能举报自己
        UUID authorId = targetAuthor(type, request.targetId());
        if (authorId != null && authorId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能举报自己发布的内容");
        }
        DiscussionReport exist = entityQuery.queryable(DiscussionReport.class)
                .where(r -> {
                    r.targetType().eq(type);
                    r.targetId().eq(request.targetId());
                    r.reporterId().eq(userId);
                })
                .firstOrNull();
        if (exist != null) {
            throw new BusinessException(ErrorCode.REPEATED_OPERATION, "你已举报过该内容，我们正在处理");
        }

        DiscussionReport report = new DiscussionReport();
        report.setId(UUID.randomUUID());
        report.setTargetType(type);
        report.setTargetId(request.targetId());
        report.setReporterId(userId);
        report.setReason(reason);
        report.setDetail(detail);
        report.setStatus("pending");
        report.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(report).executeRows();
    }

    @Override
    public EasyPageResult<DiscussionReportResource> myReports(UUID userId, Pageable pageable) {
        EasyPageResult<DiscussionReport> page = entityQuery.queryable(DiscussionReport.class)
                .where(r -> r.reporterId().eq(userId))
                .orderBy(r -> r.createdAt().desc())
                .toPageResult(pageable.getPageNumber() + 1, pageable.getPageSize());
        List<DiscussionReport> rows = page.getData();
        Map<UUID, String> authors = new HashMap<>();
        List<DiscussionReportResource> data = rows.stream()
                .map(r -> {
                    String excerpt = null;
                    String author = null;
                    String path = null;
                    if (DiscussionServiceImpl.TYPE_POST.equals(r.getTargetType())) {
                        DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                                .where(p -> p.id().eq(r.getTargetId()))
                                .firstOrNull();
                        if (post != null) {
                            excerpt = excerpt(post.getContent(), 100);
                            author = authors.computeIfAbsent(post.getAuthorId(), this::nicknameOf);
                            path = "/discussion/topic/" + post.getTopicId();
                        }
                    } else {
                        DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                                .where(t -> t.id().eq(r.getTargetId()))
                                .firstOrNull();
                        if (topic != null) {
                            excerpt = excerpt(topic.getTitle(), 100);
                            author = authors.computeIfAbsent(topic.getAuthorId(), this::nicknameOf);
                            path = "/discussion/topic/" + topic.getId();
                        }
                    }
                    return new DiscussionReportResource(
                            r.getId().toString(),
                            r.getTargetType(),
                            r.getTargetId().toString(),
                            excerpt,
                            author,
                            path,
                            null,
                            r.getReason(),
                            r.getDetail(),
                            r.getStatus(),
                            r.getResultNote(),
                            r.getCreatedAt() == null ? null : r.getCreatedAt().format(TIME_FORMAT),
                            r.getHandledAt() == null ? null : r.getHandledAt().format(TIME_FORMAT)
                    );
                })
                .toList();
        return new DefaultPageResult<>(page.getTotal(), data);
    }

    private UUID targetAuthor(String type, UUID targetId) {
        if (DiscussionServiceImpl.TYPE_POST.equals(type)) {
            DiscussionPost post = entityQuery.queryable(DiscussionPost.class)
                    .where(p -> p.id().eq(targetId))
                    .firstOrNull();
            if (post == null || "deleted".equals(post.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "被举报的回复不存在");
            }
            return post.getAuthorId();
        }
        DiscussionTopic topic = entityQuery.queryable(DiscussionTopic.class)
                .where(t -> t.id().eq(targetId))
                .firstOrNull();
        if (topic == null || "deleted".equals(topic.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "被举报的帖子不存在");
        }
        return topic.getAuthorId();
    }

    private String nicknameOf(UUID userId) {
        if (userId == null) {
            return null;
        }
        UserAuth user = entityQuery.queryable(UserAuth.class)
                .where(u -> u.id().eq(userId))
                .firstOrNull();
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
}
