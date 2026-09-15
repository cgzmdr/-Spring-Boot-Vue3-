package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.DiscussionPostCreateRequest;
import com.czdr.work.model.request.DiscussionReportRequest;
import com.czdr.work.model.request.DiscussionTopicCreateRequest;
import com.czdr.work.model.request.DiscussionTopicUpdateRequest;
import com.czdr.work.model.resource.DiscussionBoardResource;
import com.czdr.work.model.resource.DiscussionPostResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.DiscussionTopicDetailResource;
import com.czdr.work.service.DiscussionService;
import com.czdr.work.service.ReportService;
import com.czdr.work.service.SubscriptionService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 讨论区接口（C 端公开 + 登录后发帖/回复/举报）
 *
 * @author cz
 */
@Tag(name = "讨论区 Discussion", description = "C 端社区：板块 / 帖子 / 楼层 / 举报")
@RestController
@RequestMapping("discussion")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;
    private final ReportService reportService;
    private final SubscriptionService subscriptionService;

    /** 板块列表 */
    @Operation(summary = "讨论区板块列表")
    @GetMapping("boards")
    Result<List<DiscussionBoardResource>> boards() {
        return Result.success(discussionService.boards());
    }

    /** 帖子列表：sort = latest / hot / featured；可按板块、关键词、关联内容、作者筛选 */
    @Operation(summary = "帖子列表", description = "sort：latest 最新 / hot 热门 / featured 精华；支持板块、关键词、关联内容（linkedType+linkedId）、作者筛选")
    @GetMapping("topics")
    Result<EasyPageResult<DiscussionTopicBriefResource>> topics(
            @RequestParam(required = false) @Parameter(description = "板块 ID") String boardId,
            @RequestParam(required = false) @Parameter(description = "标题关键词") String keyword,
            @RequestParam(required = false, defaultValue = "latest") @Parameter(description = "排序：latest/hot/featured") String sort,
            @RequestParam(required = false) @Parameter(description = "关联内容类型：ethnic/festival/art/food/topic") String linkedType,
            @RequestParam(required = false) @Parameter(description = "关联内容 ID") String linkedId,
            @RequestParam(required = false) @Parameter(description = "作者 ID") String authorId,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(discussionService.topics(boardId, keyword, sort, linkedType, linkedId, authorId,
                currentUserId(), pageable));
    }

    /** 某内容关联的讨论（民族/节日/艺术/美食详情页底部） */
    @Operation(summary = "内容关联讨论", description = "按关联内容查询讨论帖，供内容详情页展示")
    @GetMapping("topics/linked")
    Result<List<DiscussionTopicBriefResource>> linkedTopics(
            @RequestParam @Parameter(description = "关联内容类型") String linkedType,
            @RequestParam @Parameter(description = "关联内容 ID") String linkedId,
            @RequestParam(required = false, defaultValue = "5") Integer size) {
        return Result.success(discussionService.linkedTopics(linkedType, linkedId, size));
    }

    /** 帖子详情 */
    @Operation(summary = "帖子详情", description = "作者本人可见自己的待审/驳回内容，并返回审核提示")
    @GetMapping("topics/{id}")
    Result<DiscussionTopicDetailResource> topic(@PathVariable @Parameter(description = "帖子 ID") String id) {
        return Result.success(discussionService.topic(id, currentUserId(), isAdmin()));
    }

    /** 发帖 */
    @Operation(summary = "发帖", description = "需登录；命中 block 级敏感词或板块先审后发时进入待审队列")
    @PostMapping("topics")
    Result<String> createTopic(@RequestBody DiscussionTopicCreateRequest request) {
        return Result.success(discussionService.createTopic(requireUserId(), request));
    }

    /** 编辑帖子 */
    @Operation(summary = "编辑帖子", description = "仅作者本人")
    @PutMapping("topics/{id}")
    Result<Void> updateTopic(@PathVariable String id, @RequestBody DiscussionTopicUpdateRequest request) {
        discussionService.updateTopic(requireUserId(), id, request);
        return Result.success(null);
    }

    /** 删除帖子（软删） */
    @Operation(summary = "删除帖子")
    @DeleteMapping("topics/{id}")
    Result<Void> deleteTopic(@PathVariable String id) {
        discussionService.deleteTopic(requireUserId(), id, isAdmin());
        return Result.success(null);
    }

    /** 楼层列表（2 楼起；1 楼为楼主首帖，见帖子详情） */
    @Operation(summary = "楼层列表")
    @GetMapping("topics/{id}/posts")
    Result<EasyPageResult<DiscussionPostResource>> posts(
            @PathVariable String id,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(discussionService.posts(id, currentUserId(), isAdmin(), pageable));
    }

    /** 回复帖子 */
    @Operation(summary = "回复帖子", description = "需登录；帖子锁定后不可回复")
    @PostMapping("topics/{id}/posts")
    Result<DiscussionPostResource> createPost(@PathVariable String id, @RequestBody DiscussionPostCreateRequest request) {
        return Result.success(discussionService.createPost(requireUserId(), id, request));
    }

    /** 删除回复 */
    @Operation(summary = "删除回复")
    @DeleteMapping("posts/{id}")
    Result<Void> deletePost(@PathVariable String id) {
        discussionService.deletePost(requireUserId(), id, isAdmin());
        return Result.success(null);
    }

    /** 举报帖子或回复 */
    @Operation(summary = "举报内容", description = "需登录；同一内容不可重复举报")
    @PostMapping("reports")
    Result<Void> report(@RequestBody DiscussionReportRequest request) {
        reportService.report(requireUserId(), request);
        return Result.success(null);
    }

    /* ---------------------------- 订阅（帖子 / 板块 / 用户） ---------------------------- */

    @Operation(summary = "设置订阅通知强度", description = "level：all 全部通知 / mention 仅被 @ 时通知 / off 免打扰")
    @PostMapping("subscriptions")
    Result<String> subscribe(@RequestParam @Parameter(description = "topic / board / user") String targetType,
                             @RequestParam String targetId,
                             @RequestParam(defaultValue = "all") String level) {
        return Result.success(subscriptionService.subscribe(requireUserId(), targetType, UUID.fromString(targetId), level));
    }

    @Operation(summary = "取消订阅（删除记录）")
    @DeleteMapping("subscriptions")
    Result<Void> unsubscribe(@RequestParam String targetType, @RequestParam String targetId) {
        subscriptionService.remove(requireUserId(), targetType, UUID.fromString(targetId));
        return Result.success(null);
    }

    @Operation(summary = "查询我对某目标的订阅等级", description = "未订阅返回 null（前端按「未订阅」展示）")
    @GetMapping("subscriptions/level")
    Result<String> subscriptionLevel(@RequestParam String targetType, @RequestParam String targetId) {
        UUID userId = currentUserId();
        if (userId == null) {
            return Result.success(null);
        }
        return Result.success(subscriptionService.levelOf(userId, targetType, UUID.fromString(targetId)));
    }

    // ------------------------------------------------------------------ 工具

    /** 当前登录用户；未登录时抛出 NotLoginException（统一响应 1003，前端据此弹登录框） */
    private UUID requireUserId() {
        return UUID.fromString(StpUtil.getLoginIdAsString());
    }

    /** 当前登录用户（未登录返回 null，用于「我的帖子是否本人」等展示） */
    private UUID currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        try {
            return UUID.fromString(StpUtil.getLoginIdAsString());
        } catch (Exception e) {
            return null;
        }
    }

    /** 是否具备讨论区治理权限（后台审核员） */
    private boolean isAdmin() {
        return StpUtil.isLogin() && StpUtil.hasPermission("discussion:review");
    }
}
