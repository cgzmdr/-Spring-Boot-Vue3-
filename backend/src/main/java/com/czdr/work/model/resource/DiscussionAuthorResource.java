package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 讨论区作者信息（避免下发完整用户对象）
 */
@Schema(description = "讨论区作者信息")
public record DiscussionAuthorResource(
        @Schema(description = "用户 ID") String id,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "头像") String avatar,
        @Schema(description = "信任等级") Integer trustLevel,
        @Schema(description = "是否楼主") Boolean owner
) {
}
