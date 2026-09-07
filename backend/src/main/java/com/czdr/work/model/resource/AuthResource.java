package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录请求参数
 */
@Schema(description = "登录请求参数")
public record AuthResource(
        @Schema(description = "登录账号") String account,
        @Schema(description = "登录密码") String password
) {
}
