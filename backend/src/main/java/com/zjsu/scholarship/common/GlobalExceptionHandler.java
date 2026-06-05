package com.zjsu.scholarship.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException ex) {
        return R.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return R.fail(500, ex.getMessage() == null ? "服务器异常" : ex.getMessage());
    }
}
