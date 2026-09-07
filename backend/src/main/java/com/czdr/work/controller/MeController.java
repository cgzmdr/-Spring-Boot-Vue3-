package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.FavoriteQueryInfoResource;
import com.czdr.work.service.InteractionService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心接口
 *
 * @author cz
 */
@Tag(name = "个人中心 Me", description = "C 端登录用户：我的收藏列表")
@RestController
@RequiredArgsConstructor
@RequestMapping("me")
public class MeController {
    private final InteractionService interactionService;

    /**
     * 我的收藏列表（分页）
     * <p>返回当前登录用户收藏的内容，可按内容类型 type 筛选，按收藏时间倒序。</p>
     */
    @Operation(summary = "我的收藏列表", description = "分页查询当前登录用户的收藏内容，可按内容类型筛选（需登录）")
    @SaCheckLogin
    @GetMapping("favorites")
    Result<EasyPageResult<FavoriteQueryInfoResource>> favorites(
            @RequestParam(value = "type", required = false) @Parameter(description = "内容类型：ethnic / festival / art / topic，为空返回全部") String type,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return Result.success(interactionService.favorites(StpUtil.getLoginIdAsString(), type, pageable));
    }
}
