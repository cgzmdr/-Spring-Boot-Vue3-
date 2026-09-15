package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.CommunityUserResource;
import com.czdr.work.model.resource.DiscussionTopicBriefResource;
import com.czdr.work.model.resource.MentionUserResource;
import com.czdr.work.service.SocialService;
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
 * 社区社交接口：用户主页 / 关注 / 粉丝 / 关注流
 *
 * @author cz
 */
@Tag(name = "社区社交 Social", description = "C 端：用户主页 / 关注粉丝 / 关注流")
@RestController
@RequestMapping("discussion")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @Operation(summary = "社区用户主页", description = "含关注/粉丝/发帖数与「我是否已关注」「是否互关」")
    @GetMapping("users/{id}")
    Result<CommunityUserResource> profile(@PathVariable @Parameter(description = "用户 ID") String id) {
        return Result.success(socialService.profile(id, currentUserId()));
    }

    @Operation(summary = "关注用户")
    @PostMapping("users/{id}/follow")
    Result<Boolean> follow(@PathVariable String id) {
        return Result.success(socialService.follow(requireUserId(), UUID.fromString(id), true));
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("users/{id}/follow")
    Result<Boolean> unfollow(@PathVariable String id) {
        return Result.success(socialService.follow(requireUserId(), UUID.fromString(id), false));
    }

    @Operation(summary = "关注 / 粉丝列表", description = "type = following 关注的人 / followers 粉丝")
    @GetMapping("users/{id}/follows")
    Result<EasyPageResult<CommunityUserResource>> follows(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "following") String type,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(socialService.follows(id, type, pageable));
    }

    @Operation(summary = "关注流", description = "我关注的用户发布的帖子（需登录）")
    @GetMapping("topics/following")
    Result<EasyPageResult<DiscussionTopicBriefResource>> followingFeed(
            @PageableDefault(page = 0, size = 15) Pageable pageable) {
        return Result.success(socialService.followingFeed(requireUserId(), pageable));
    }

    @Operation(summary = "我的好友（互相关注）ID 列表", description = "供私信选择联系人")
    @GetMapping("users/mutual")
    Result<List<String>> mutual() {
        return Result.success(socialService.mutualIds(requireUserId()));
    }

    /**
     * @提及联想：回复框 / 发帖页输入「@」时提示候选用户。
     * <p>未登录也可调用（返回推荐用户），登录后「我关注的人」优先；关键字按昵称模糊匹配，
     * 返回的昵称可直接写入 `@昵称`，后端按昵称解析并通知被提及者。</p>
     */
    @Operation(summary = "@提及联想用户", description = "按昵称关键字返回候选用户（默认 10 条，最多 20）；关键字为空时优先返回我关注的人")
    @GetMapping("users/suggest")
    Result<List<MentionUserResource>> suggestUsers(
            @RequestParam(required = false) @Parameter(description = "昵称关键字，可为空") String keyword,
            @RequestParam(required = false, defaultValue = "10") @Parameter(description = "返回条数，默认 10，最大 20") Integer limit) {
        return Result.success(socialService.suggestUsers(currentUserId(), keyword, limit == null ? 10 : limit));
    }

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

    private UUID requireUserId() {
        return UUID.fromString(StpUtil.getLoginIdAsString());
    }
}
