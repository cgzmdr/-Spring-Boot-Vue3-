package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 注册结果
 */
@Schema(description = "注册结果")
public record RegisterResource(
        @Schema(description = "昵称") String nickname,
        @Schema(description = "创建时间") LocalDate createdAt
) {

}
