package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.SensitiveWord;
import com.czdr.work.model.request.BroadcastRequest;
import com.czdr.work.model.request.DiscussionHandleRequest;
import com.czdr.work.model.request.DiscussionReviewRequest;
import com.czdr.work.model.request.SensitiveWordRequest;
import com.czdr.work.model.resource.DiscussionReportResource;
import com.czdr.work.model.resource.DiscussionReviewItemResource;
import com.czdr.work.service.AdminDiscussionService;
import com.czdr.work.service.SensitiveWordService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 讨论区治理接口（后台，需要讨论区治理权限）
 *
 * @author cz
 */
@Tag(name = "讨论区治理 Admin · Discussion", description = "后台：审核队列 / 举报处理 / 用户处置 / 敏感词")
@RestController
@RequestMapping("admin/discussion")
@RequiredArgsConstructor
public class AdminDiscussionController {

    private final AdminDiscussionService adminDiscussionService;
    private final SensitiveWordService sensitiveWordService;

    // ---------------------------------------------------------------- 审核队列

    @Operation(summary = "待审队列", description = "targetType：discussion_topic 帖子 / discussion_post 回复；status：pending 待审 / watch 敏感词打标 / all；sort：oldest 最早优先（默认）/ priority 高优先级优先（block 级敏感词 → 被举报多 → 最早）")
    @SaCheckPermission("discussion:review")
    @GetMapping("reviews")
    Result<EasyPageResult<DiscussionReviewItemResource>> reviews(
            @RequestParam(required = false, defaultValue = "discussion_topic") String targetType,
            @RequestParam(required = false, defaultValue = "pending") String status,
            @RequestParam(required = false, defaultValue = "oldest") String sort,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(adminDiscussionService.reviewQueue(targetType, status, sort, pageable));
    }

    @Operation(summary = "审核处置", description = "approved 通过 / rejected 驳回（驳回必须填理由，会通知作者）")
    @SaCheckPermission("discussion:review")
    @PostMapping("reviews")
    Result<Void> review(@RequestBody DiscussionReviewRequest request) {
        adminDiscussionService.review(StpUtil.getLoginIdAsString(), request);
        return Result.success(null);
    }

    @Operation(summary = "批量审核", description = "body：{ targetType, ids[], status, reason }，单条失败不影响其余，返回 { success, failed, failures }")
    @SaCheckPermission("discussion:review")
    @PostMapping("reviews/batch")
    Result<Map<String, Object>> reviewBatch(@RequestBody Map<String, Object> body) {
        String targetType = body == null ? null : (String) body.get("targetType");
        String status = body == null ? null : (String) body.get("status");
        String reason = body == null ? null : (String) body.get("reason");
        List<String> ids = List.of();
        Object raw = body == null ? null : body.get("ids");
        if (raw instanceof List<?> list) {
            ids = list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return Result.success(adminDiscussionService.reviewBatch(StpUtil.getLoginIdAsString(), targetType, ids, status, reason));
    }

    // ---------------------------------------------------------------- 举报工作台

    @Operation(summary = "举报列表", description = "status：pending / accepted / rejected / all")
    @SaCheckPermission("discussion:review")
    @GetMapping("reports")
    Result<EasyPageResult<DiscussionReportResource>> reports(
            @RequestParam(required = false, defaultValue = "pending") String status,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(adminDiscussionService.reports(status, pageable));
    }

    @Operation(summary = "处理举报", description = "可同时隐藏/删除被举报内容并对作者禁言；结果回执给举报人")
    @SaCheckPermission("discussion:review")
    @PostMapping("reports/{id}")
    Result<Void> handleReport(@PathVariable String id, @RequestBody(required = false) DiscussionHandleRequest request) {
        adminDiscussionService.handleReport(StpUtil.getLoginIdAsString(), id, request);
        return Result.success(null);
    }

    // ---------------------------------------------------------------- 内容与用户处置

    @Operation(summary = "置顶 / 精华 / 锁定", description = "flag：pinned / featured / locked")
    @SaCheckPermission("discussion:review")
    @PostMapping("topics/{id}/flag")
    Result<Void> setFlag(@PathVariable String id,
                         @RequestParam String flag,
                         @RequestParam(defaultValue = "true") boolean value) {
        adminDiscussionService.setFlag(StpUtil.getLoginIdAsString(), id, flag, value);
        return Result.success(null);
    }

    @Operation(summary = "隐藏内容")
    @SaCheckPermission("discussion:review")
    @PostMapping("content/hide")
    Result<Void> hideContent(@RequestParam String targetType,
                             @RequestParam String targetId,
                             @RequestParam(required = false) String reason) {
        adminDiscussionService.hideContent(StpUtil.getLoginIdAsString(), targetType, targetId, reason);
        return Result.success(null);
    }

    @Operation(summary = "禁言 / 解除禁言", description = "days <= 0 表示解除禁言")
    @SaCheckPermission("discussion:user:mute")
    @PostMapping("users/{id}/mute")
    Result<Void> muteUser(@PathVariable String id,
                          @RequestParam(defaultValue = "0") Integer days,
                          @RequestParam(required = false) String reason) {
        adminDiscussionService.muteUser(StpUtil.getLoginIdAsString(), id, days, reason);
        return Result.success(null);
    }

    // ---------------------------------------------------------------- 敏感词

    @Operation(summary = "敏感词列表")
    @SaCheckPermission("discussion:review")
    @GetMapping("words")
    Result<EasyPageResult<SensitiveWord>> words(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(sensitiveWordService.list(keyword, pageable));
    }

    @Operation(summary = "新增 / 更新敏感词", description = "word 已存在时更新其等级与启用状态")
    @SaCheckPermission("discussion:review")
    @PostMapping("words")
    Result<String> saveWord(@RequestBody SensitiveWordRequest request,
                            @RequestParam(required = false) @Parameter(description = "原词条（重命名时使用）") String currentWord) {
        return Result.success(sensitiveWordService.save(request, currentWord));
    }

    @Operation(summary = "删除敏感词")
    @SaCheckPermission("discussion:review")
    @DeleteMapping("words/{id}")
    Result<Void> deleteWord(@PathVariable String id) {
        sensitiveWordService.delete(UUID.fromString(id));
        return Result.success(null);
    }

    @Operation(summary = "批量导入敏感词", description = "支持换行/逗号/顿号/分号分隔，返回 { added, skipped }")
    @SaCheckPermission("discussion:review")
    @PostMapping("words/import")
    Result<Map<String, Object>> importWords(@RequestBody Map<String, String> body) {
        String text = body == null ? null : body.get("text");
        String level = body == null ? "watch" : body.get("level");
        String locale = body == null ? "zh" : body.get("locale");
        return Result.success(sensitiveWordService.importWords(text, level, locale));
    }

    @Operation(summary = "导出敏感词（每行一条，可直接再导入）")
    @SaCheckPermission("discussion:review")
    @GetMapping(value = "words/export", produces = "text/plain;charset=UTF-8")
    String exportWords() {
        return String.join("\n", sensitiveWordService.exportWords());
    }

    // ---------------------------------------------------------------- 站内公告（OA 群发）

    @Operation(summary = "公告受众人数预览", description = "返回 { all 全部注册用户, active 近 N 天活跃用户, maxRecipients 单次上限 }")
    @SaCheckPermission("system:broadcast")
    @GetMapping("broadcast/audience")
    Result<Map<String, Object>> broadcastAudience() {
        return Result.success(adminDiscussionService.broadcastAudience());
    }

    @Operation(summary = "发送站内公告", description = "body：{ title, content, email, audience: all|active, link? }；写站内通知（type=system）并按需发邮件，受单次收件人上限与每小时限流保护，写审核审计日志")
    @SaCheckPermission("system:broadcast")
    @PostMapping("broadcast")
    Result<Map<String, Object>> broadcast(@RequestBody BroadcastRequest request) {
        return Result.success(adminDiscussionService.broadcast(StpUtil.getLoginIdAsString(), request));
    }

    // ---------------------------------------------------------------- 看板

    @Operation(summary = "社区数据看板")
    @SaCheckPermission("discussion:review")
    @GetMapping("stats")
    Result<Map<String, Object>> stats() {
        return Result.success(adminDiscussionService.stats());
    }

    // ---------------------------------------------------------------- 板块管理

    @Operation(summary = "板块列表（含隐藏）")
    @SaCheckPermission("discussion:board")
    @GetMapping("boards")
    Result<List<Map<String, Object>>> boards() {
        return Result.success(adminDiscussionService.boards());
    }

    @Operation(summary = "新增 / 更新板块", description = "id 为空表示新建；postPolicy：post_then_review 先发后审 / review_then_post 先审后发")
    @SaCheckPermission("discussion:board")
    @PostMapping("boards")
    Result<String> saveBoard(@RequestBody Map<String, Object> payload) {
        return Result.success(adminDiscussionService.saveBoard(StpUtil.getLoginIdAsString(), payload));
    }

    @Operation(summary = "启用 / 停用板块")
    @SaCheckPermission("discussion:board")
    @PostMapping("boards/{id}/status")
    Result<Void> toggleBoard(@PathVariable String id, @RequestParam boolean active) {
        adminDiscussionService.toggleBoard(StpUtil.getLoginIdAsString(), id, active);
        return Result.success(null);
    }
}
