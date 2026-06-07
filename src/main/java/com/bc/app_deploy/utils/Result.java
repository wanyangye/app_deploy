package com.bc.app_deploy.utils;

import lombok.Data;

@Data
public class Result<T> {
    private Integer errcode;
    private String errmsg;
    private T data;
    private String traceId;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setErrcode(0);
        result.setErrmsg("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setErrcode(5000);
        result.setErrmsg(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setErrcode(code);
        result.setErrmsg(message);
        return result;
    }
}