package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 翻译请求：按需翻译一条内容（标题或正文）。
 *
 * @author cz
 */
@Schema(description = "机器翻译请求")
public record TranslateRequest(
        @Schema(description = "内容类型：topic 帖子 / post 楼层 / message 私信 / board 板块") String targetType,
        @Schema(description = "内容 ID") String targetId,
        @Schema(description = "目标语言（读者语言，如 en / zh）") String targetLocale,
        @Schema(description = "翻译范围：title 仅标题（列表页用）/ body 仅正文 / all 标题+正文（默认）") String scope
) {
}
