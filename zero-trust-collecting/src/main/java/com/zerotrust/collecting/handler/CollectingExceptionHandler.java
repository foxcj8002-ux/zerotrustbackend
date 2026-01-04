package com.zerotrust.collecting.handler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import zerotrust.common.Exceptions.CommonException;
import zerotrust.common.Response.ErrorCode;
import zerotrust.common.Response.Result;

@RestControllerAdvice
public class CollectingExceptionHandler {
    @ExceptionHandler(CommonException.class)
    public Result<Void> handleCommon(CommonException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        return Result.fail(ErrorCode.SERVER_ERROR.getCode(), "Server internal error");
    }

}
