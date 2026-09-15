package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.DiscussionPostResource;
import com.czdr.work.model.resource.DiscussionReportResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.NotificationResource;
import com.czdr.work.model.resource.SubscriptionResource;
import com.czdr.work.service.DiscussionService;
import com.czdr.work.service.NotificationService;
import com.czdr.work.service.ReportService;
import com.czdr.work.service.SubscriptionService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 我的社区数据：我的发帖/回复/举报与站内通知（均需登录）
 *
 * @author cz
 */
@Tag(name = "我的社区 Me · Discussion", description = "个人中心：我的发帖 / 回复 / 举报 / 通知")
@RestController
@RequestMapping("me")
@RequiredArgsConstructor
public class DiscussionMeController {

    private final DiscussionService discussionService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final SubscriptionService subscriptionService;

    @Operation(summary = "我的发帖")
    @GetMapping("discussion-topics")
    Result<EasyPageResult<DiscussionTopicBriefResource>> myTopics(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return Result.success(discussionService.myTopics(currentUserId(), pageable));
    }

    @Operation(summary = "我的回复")
    @GetMapping("discussion-posts")
    Result<EasyPageResult<DiscussionPostResource>> myPosts(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return Result.success(discussionService.myPosts(currentUserId(), pageable));
    }

    @Operation(summary = "我的举报（含处理进度）")
    @GetMapping("discussion-reports")
    Result<EasyPageResult<DiscussionReportResource>> myReports(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return Result.success(reportService.myReports(currentUserId(), pageable));
    }

    @Operation(summary = "站内通知列表")
    @GetMapping("notifications")
    Result<EasyPageResult<NotificationResource>> notifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(notificationService.list(currentUserId(), pageable, unreadOnly));
    }

    @Operation(summary = "未读通知数量（角标）")
    @GetMapping("notifications/unread-count")
    Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount(currentUserId()));
    }

    @Operation(summary = "标记通知已读", description = "ids 为空表示全部标记已读")
    @PostMapping("notifications/read")
    Result<Integer> markRead(@RequestBody(required = false) List<String> ids) {
        List<UUID> parsed = ids == null ? List.of() : ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(UUID::fromString)
                .toList();
        return Result.success(notificationService.markRead(currentUserId(), parsed));
    }

    @Operation(summary = "我的订阅（帖子 / 板块 / 用户）")
    @GetMapping("subscriptions")
    Result<List<SubscriptionResource>> subscriptions() {
        return Result.success(subscriptionService.mySubscriptions(currentUserId()));
    }

    private UUID currentUserId() {
        return UUID.fromString(StpUtil.getLoginIdAsString());
    }
}
