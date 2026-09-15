package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 后台 OA 站内公告群发请求。
 *
 * @author cz
 */
@Schema(description = "站内公告群发请求")
public record BroadcastRequest(
        @Schema(description = "公告标题") String title,
        @Schema(description = "公告正文（纯文本，换行会保留）") String content,
        @Schema(description = "是否同时发送邮件（默认 false）") Boolean email,
        @Schema(description = "受众：all 全部注册用户 / active 近 30 天活跃用户") String audience,
        @Schema(description = "可选跳转链接（如 /discussion）") String link
) {
}
