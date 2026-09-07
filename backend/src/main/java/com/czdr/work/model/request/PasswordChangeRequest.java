package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修改密码请求（三种验证方式任选其一：旧密码 / 验证码 / 密保答案）
 *
 * @author cz
 */
@Schema(description = "修改密码请求")
public record PasswordChangeRequest(
        @Schema(description = "验证方式一：旧密码")
        String oldPassword,
        @Schema(description = "验证方式二：接收验证码的账号（邮箱 / 手机号）")
        String account,
        @Schema(description = "验证方式二：验证码")
        String code,
        @Schema(description = "验证方式三：密保答案")
        String securityAnswer,
        @Schema(description = "新密码（强密码：8 位以上且含大小写字母、数字、特殊字符）")
        String newPassword
) {
}
