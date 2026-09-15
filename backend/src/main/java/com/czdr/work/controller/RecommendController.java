package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.InterestTag;
import com.czdr.work.model.resource.RecommendationResource;
import com.czdr.work.service.InterestService;
import com.czdr.work.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 兴趣标签与个性化推荐接口（方向 D）。
 *
 * <p>鉴权策略：标签字典与推荐**游客可读**（未登录时退化为热度推荐，并如实说明），
 * 维护兴趣与行为上报需登录。</p>
 *
 * @author cz
 */
@Tag(name = "推荐 Recommend", description = "兴趣标签、个性化推荐与行为上报")
@RestController
@RequiredArgsConstructor
public class RecommendController {

    private final InterestService interestService;
    private final RecommendationService recommendationService;

    // ---------------------------------------------------------------- 兴趣标签

    @Operation(summary = "兴趣标签字典",
            description = "按维度分组返回全部启用标签：ethnic 民族 / region 地域 / type 内容类型 / topic 主题")
    @GetMapping("interests/tags")
    Result<Map<String, List<InterestTag>>> tags() {
        return Result.success(interestService.grouped());
    }

    @Operation(summary = "我的兴趣标签", description = "返回当前登录用户已选的兴趣标签明细；未登录返回空列表")
    @GetMapping("interests/mine")
    Result<List<InterestTag>> mine() {
        return Result.success(interestService.userTags(currentUserId()));
    }

    @Operation(summary = "保存我的兴趣标签",
            description = "覆盖式保存（多选语义）。tagIds 为空表示清空兴趣。需登录。")
    @PostMapping("interests/mine")
    Result<Integer> saveMine(@RequestBody Map<String, List<String>> body) {
        UUID userId = requireUserId();
        List<String> tagIds = body == null ? List.of() : body.getOrDefault("tagIds", List.of());
        return Result.success(interestService.setUserInterests(userId, tagIds));
    }

    // ---------------------------------------------------------------- 推荐

    @Operation(summary = "个性化推荐",
            description = "混合推荐：兴趣标签（显式）+ 浏览行为（隐式）+ 内容相似 + 热度兜底。"
                    + "游客亦可用（退化为热度推荐）。返回 basis / dataNote 如实说明本次推荐的实际依据与样本量。")
    @GetMapping("recommend")
    Result<RecommendationResource> recommend(
            @RequestParam(value = "size", required = false, defaultValue = "8") int size,
            @RequestParam(value = "excludeType", required = false)
            @Parameter(description = "排除的内容类型（详情页推荐时排除自身类型）") String excludeType) {
        return Result.success(recommendationService.recommend(
                currentUserId(), Math.min(Math.max(size, 1), 30), excludeType));
    }

    // ---------------------------------------------------------------- 行为上报

    @Operation(summary = "上报浏览行为",
            description = "记录一次内容浏览，作为个性化推荐的隐式信号。未登录时静默忽略（不构成个人画像）。")
    @PostMapping("behaviors/view")
    Result<Void> reportView(@RequestBody Map<String, String> body) {
        UUID userId = currentUserId();
        if (userId == null || body == null) {
            return Result.success(null);
        }
        UUID targetId = parseUuid(body.get("targetId"));
        recommendationService.record(userId, "view", body.get("targetType"), targetId, null);
        return Result.success(null);
    }

    // ---------------------------------------------------------------- 内部工具

    /** 当前登录用户 ID；未登录返回 null（不抛错，便于游客使用推荐） */
    private UUID currentUserId() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return UUID.fromString(StpUtil.getLoginIdAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private UUID requireUserId() {
        UUID id = currentUserId();
        if (id == null) {
            throw new com.czdr.work.comment.exception.BusinessException(
                    com.czdr.work.comment.exception.ErrorCode.NOT_LOGIN, "请先登录");
        }
        return id;
    }

    private UUID parseUuid(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
