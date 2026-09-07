package com.czdr.work.comment.exception;

import lombok.Getter;

/**
 * 统一业务错误码（对应 API.md 第 11 章错误码表）
 *
 * @author cz
 */
@Getter
public enum ErrorCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(1001, "参数错误"),
    NOT_FOUND(1002, "资源不存在"),
    NOT_LOGIN(1003, "未登录或登录已失效"),
    FORBIDDEN(1004, "无权限访问"),
    TOKEN_EXPIRED(1005, "token 已过期"),
    LOGIN_FAILED(1006, "账号或密码错误"),
    ACCOUNT_DISABLED(1007, "账号已被禁用"),
    RATE_LIMITED(1008, "请求过于频繁"),
    CODE_INVALID(1009, "验证码错误或已过期"),
    DUPLICATE(2001, "内容已存在"),
    REPEATED_OPERATION(2002, "重复操作"),
    SERVER_ERROR(5000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
