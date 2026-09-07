package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.LikeResource;
import com.czdr.work.model.resource.StatsResource;
import com.czdr.work.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 内容互动接口（点赞/收藏/统计）
 *
 * @author cz
 */
@Tag(name = "内容互动 Interaction", description = "C 端互动：点赞 / 收藏 / 互动统计（内容类型 type：ethnic / festival / art / topic）")
@RestController
@RequiredArgsConstructor
@RequestMapping("contents")
public class InteractionController {
    private final InteractionService interactionService;

    /**
     * 点赞内容
     * <p>对指定类型、ID 的内容点赞，需登录（content:like 权限），返回最新点赞状态与点赞数。</p>
     */
    @Operation(summary = "点赞内容", description = "对指定类型、ID 的内容点赞（需登录，content:like 权限）")
    @SaCheckLogin
    @SaCheckPermission("content:like")
    @PostMapping("{type}/{id}/like")
    Result<LikeResource> like(@PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String type, @PathVariable @Parameter(description = "内容 ID") String id) {
        return Result.success(interactionService.like(StpUtil.getLoginIdAsString(), type, id));
    }

    /**
     * 取消点赞
     * <p>取消对指定内容的点赞，需登录（content:like 权限）。</p>
     */
    @Operation(summary = "取消点赞", description = "取消对指定内容类型的点赞（需登录，content:like 权限）")
    @SaCheckLogin
    @SaCheckPermission("content:like")
    @DeleteMapping("{type}/{id}/like")
    Result<LikeResource> unlike(@PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String type, @PathVariable @Parameter(description = "内容 ID") String id) {
        return Result.success(interactionService.unlike(StpUtil.getLoginIdAsString(), type, id));
    }

    /**
     * 收藏内容
     * <p>收藏指定内容，需登录（content:favorite 权限）。</p>
     */
    @Operation(summary = "收藏内容", description = "收藏指定内容（需登录，content:favorite 权限）")
    @SaCheckLogin
    @SaCheckPermission("content:favorite")
    @PostMapping("{type}/{id}/favorite")
    Result<Void> favorite(@PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String type, @PathVariable @Parameter(description = "内容 ID") String id) {
        interactionService.favorite(StpUtil.getLoginIdAsString(), type, id);
        return Result.success(null);
    }

    @Operation(summary = "取消收藏", description = "取消收藏指定内容（需登录，content:favorite 权限）")
    @SaCheckLogin
    @SaCheckPermission("content:favorite")
    @DeleteMapping("{type}/{id}/favorite")
    Result<Void> unfavorite(@PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String type, @PathVariable @Parameter(description = "内容 ID") String id) {
        interactionService.unfavorite(StpUtil.getLoginIdAsString(), type, id);
        return Result.success(null);
    }

    /**
     * 记录浏览量
     * <p>浏览内容详情页时调用，公开接口无需登录，返回最新浏览量。</p>
     */
    @Operation(summary = "记录浏览量", description = "浏览内容详情页时调用一次，浏览量 +1（公开接口），返回最新浏览量")
    @PostMapping("{type}/{id}/view")
    Result<Long> view(@PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String type, @PathVariable @Parameter(description = "内容 ID") String id) {
        return Result.success(interactionService.view(type, id));
    }

    /**
     * 互动统计
     * <p>查询指定内容的点赞数、收藏数，公开接口无需登录。</p>
     */
    @Operation(summary = "互动统计", description = "查询指定内容的点赞数、收藏数、浏览量（公开接口）")
    @GetMapping("{type}/{id}/stats")
    Result<StatsResource> stats(@PathVariable @Parameter(description = "内容类型：ethnic / festival / art / topic") String type, @PathVariable @Parameter(description = "内容 ID") String id) {
        return Result.success(interactionService.stats(type, id));
    }
}
