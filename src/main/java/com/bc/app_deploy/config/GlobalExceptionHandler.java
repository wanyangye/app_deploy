package com.bc.app_deploy.config;

import com.bc.app_deploy.exception.BaseException;
import com.bc.app_deploy.utils.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e) {
        return Result.error(e.getErrcode(), e.getErrmsg());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error("系统异常: " + e.getMessage());
    }
}
