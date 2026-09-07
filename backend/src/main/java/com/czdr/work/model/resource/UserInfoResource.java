package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 当前登录用户信息
 */
@Schema(description = "当前登录用户信息")
public record UserInfoResource(
        @Schema(description = "用户 ID") UUID id,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "头像") String avatar,
        @Schema(description = "界面语言偏好（zh / en）") String lang,
        @Schema(description = "角色标识列表") String[] roles,
        @Schema(description = "手机号（空串表示未绑定）") String mobile,
        @Schema(description = "邮箱（空串表示未绑定）") String email,
        @Schema(description = "注册日期") LocalDate createdAt,
        @Schema(description = "密保问题（未设置时为 null）") String securityQuestion
) {
}
