package com.czdr.work.model.resource;

import com.czdr.work.model.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 用户信息（后台管理列表返回）
 */
@Schema(description = "用户信息（后台管理列表返回）")
public record UserQueryInfoResource(
        @Schema(description = "用户 ID") UUID id,
        @Schema(description = "登录账号") String account,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "头像") String avatar,
        @Schema(description = "手机号") String mobile,
        @Schema(description = "邮箱") String email,
        @Schema(description = "用户状态（active/disabled）") String status,
        @Schema(description = "创建时间") LocalDate createdAt,
        @Schema(description = "更新时间") LocalDate updatedAt,
        @Schema(description = "角色列表") Role[] roles
) {
}
