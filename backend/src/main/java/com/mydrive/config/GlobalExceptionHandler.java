package com.mydrive.config;

import com.mydrive.common.Result;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException me) {
            if (me.getBindingResult().getFieldError() != null) {
                message = me.getBindingResult().getFieldError().getDefaultMessage();
            }
        } else if (e instanceof BindException be) {
            if (be.getBindingResult().getFieldError() != null) {
                message = be.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return Result.error(400, message);
    }
}
