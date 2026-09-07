package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 后台新建 / 编辑用户请求（超级管理员创建测试账号）。
 * <p>新建时 account / nickname / password 必填（account 留空时回退为 nickname）；
 * 编辑时仅提交需要变更的字段，password 留空表示不重置密码，
 * roleIds 为 null 表示不调整角色。</p>
 *
 * @author cz
 */
@Schema(description = "后台新建/编辑用户请求")
public record AdminUserSaveRequest(
        @Schema(description = "账号（留空时新建默认取昵称）")
        String account,
        @Schema(description = "昵称（可用于登录）")
        String nickname,
        @Schema(description = "密码（新建必填；编辑留空表示不重置）")
        String password,
        @Schema(description = "手机号（可用于登录）")
        String mobile,
        @Schema(description = "邮箱（可用于登录）")
        String email,
        @Schema(description = "状态：active / disabled，默认 active")
        String status,
        @Schema(description = "角色 ID 列表（标准 UUID 字符串），null 表示不调整角色")
        List<String> roleIds
) {
}
