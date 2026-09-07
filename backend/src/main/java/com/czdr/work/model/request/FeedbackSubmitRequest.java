package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户反馈提交请求（C 端首页浮动按钮表单）
 *
 * @author cz
 */
@Schema(description = "用户反馈提交请求")
public record FeedbackSubmitRequest(
        @Schema(description = "称呼")
        String name,
        @Schema(description = "联系方式（邮箱 / 手机号）")
        String contact,
        @Schema(description = "反馈类型：correction / suggestion / bug")
        String topic,
        @Schema(description = "满意度：good / ok / bad")
        String rating,
        @Schema(description = "反馈内容")
        String content,
        @Schema(description = "访问日期（YYYY-MM-DD）")
        String visitDate
) {
}
