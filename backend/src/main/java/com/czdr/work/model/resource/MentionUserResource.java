package com.czdr.work.model.resource;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @提及联想用户（回复框输入「@」时的候选列表项）
 * <p>只暴露昵称/头像/简介，避免把账号、邮箱等隐私字段带给 C 端联想接口。</p>
 */
@Schema(description = "@提及联想用户")
public record MentionUserResource(
        @Schema(description = "用户 ID") String id,
        @Schema(description = "昵称（@ 后的文本，必须与昵称完全一致才能命中通知）") String nickname,
        @Schema(description = "头像") String avatar,
        @Schema(description = "个人简介") String bio
) {
}
