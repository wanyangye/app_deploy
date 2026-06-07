package com.bc.app_deploy.exception;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class BaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Integer errcode;
    private String errmsg;


    public BaseException(Integer errcode, String errmsg) {
        super(errmsg);
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

    public BaseException(Integer errcode, String errmsg, Throwable cause) {
        super(errmsg, cause);
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

    public BaseException(String message) {
        super(message);
        this.errcode = 5000;
        this.errmsg = Arrays.toString(new Object[]{message});
    }

}