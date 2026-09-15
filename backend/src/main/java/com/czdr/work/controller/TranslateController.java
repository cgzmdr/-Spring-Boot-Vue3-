package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.TranslateRequest;
import com.czdr.work.model.resource.TranslateStatusResource;
import com.czdr.work.model.resource.TranslationResource;
import com.czdr.work.service.RateLimitService;
import com.czdr.work.service.TranslateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 机器翻译接口（自建服务，公开可读；失败降级为原文，不报错）。
 *
 * @author cz
 */
@Tag(name = "机器翻译 Translate", description = "按需翻译社区内容（自建 provider + 译文缓存）")
@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class TranslateController {

    private final TranslateService translateService;
    private final RateLimitService rateLimitService;

    @Operation(summary = "翻译能力状态", description = "enabled=false 时前端隐藏「译」按钮")
    @GetMapping("translate/status")
    Result<TranslateStatusResource> status() {
        return Result.success(translateService.status());
    }

    @Operation(summary = "按需翻译", description = "targetType：topic/post/message/board；scope：title 仅标题（列表页）/ body / all（默认）。命中缓存直接返回；服务不可用时 translated=false 并回退原文")
    @PostMapping("translate")
    Result<TranslationResource> translate(@RequestBody TranslateRequest request, HttpServletRequest http) {
        String actor = actor(http);
        // 翻译会调用自建服务，按人限流，避免被当成免费翻译网关
        rateLimitService.consume("translate", actor, 60, 60, "翻译请求过于频繁，请稍后再试");
        UUID viewer = viewerId();
        return Result.success(translateService.translate(
                request == null ? null : request.targetType(),
                request == null || request.targetId() == null ? null : parse(request.targetId()),
                request == null ? null : request.targetLocale(),
                request == null ? null : request.scope(),
                viewer));
    }

    private UUID parse(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new com.czdr.work.comment.exception.BusinessException(
                    com.czdr.work.comment.exception.ErrorCode.PARAM_ERROR, "内容 ID 不合法");
        }
    }

    private UUID viewerId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        try {
            return UUID.fromString(StpUtil.getLoginIdAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private String actor(HttpServletRequest http) {
        UUID viewer = viewerId();
        if (viewer != null) {
            return viewer.toString();
        }
        return "ip:" + (http == null ? "unknown" : http.getRemoteAddr());
    }
}
