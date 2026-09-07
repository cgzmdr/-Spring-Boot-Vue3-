package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.ShareRequest;
import com.czdr.work.model.resource.ShareResource;
import com.czdr.work.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 分享接口
 *
 * @author cz
 */
@Tag(name = "分享 Share", description = "C 端登录用户：生成分享链接 / 海报")
@RestController
@RequiredArgsConstructor
@RequestMapping("share")
public class ShareController {
    private final InteractionService interactionService;

    /**
     * 生成分享链接 / 海报
     * <p>根据内容类型 type 与内容 ID 生成分享信息，需登录（share:create 权限）。</p>
     */
    @Operation(summary = "生成分享", description = "根据内容类型与 ID 生成分享链接 / 海报（需登录，share:create 权限）")
    @SaCheckLogin
    @SaCheckPermission("share:create")
    @PostMapping
    Result<ShareResource> share(@RequestBody ShareRequest request) {
        return Result.success(interactionService.share(request.type(), request.id()));
    }
}
