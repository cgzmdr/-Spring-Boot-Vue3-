package com.czdr.work.comment.exception;

import lombok.Getter;

/**
 * 业务异常，携带统一错误码
 *
 * @author cz
 */
public class BusinessException extends RuntimeException {
    @Getter
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }
}
