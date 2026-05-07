package com.cutting.cuttingsystem.exception;

/** 权限不足异常，GlobalExceptionHandler 统一处理 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
