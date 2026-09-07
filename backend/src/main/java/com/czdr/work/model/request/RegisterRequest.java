package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户注册请求")
public record RegisterRequest(
        @Schema(description = "昵称（用户名，可用于登录）")
        String nickname,
        @Schema(description = "密码")
        String password
) {
}
