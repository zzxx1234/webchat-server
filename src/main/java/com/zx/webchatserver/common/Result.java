package com.zx.webchatserver.common;

import com.zx.webchatserver.common.constant.HttpStatus;
import lombok.Data;

@Data
public class Result {

    private Integer code;

    private Boolean success;

    private String message;

    private Object data;

    public Result() {}

    public Result(Integer code, Boolean success, String message, Object data) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static Result ok() {
        return new Result(HttpStatus.SUCCESS, true, "操作成功", null);
    }

    public static Result ok(Object data) {
        return new Result(HttpStatus.SUCCESS, true, "操作成功", data);
    }

    public static Result ok(String message, Object data) {
        return new Result(HttpStatus.SUCCESS, true, message, data);
    }

    public static Result fail() {
        return new Result(HttpStatus.ERROR, false, "操作失败", null);
    }

    public static Result fail(Object data) {
        return new Result(HttpStatus.ERROR, false, "操作失败", data);
    }

    public static Result fail(String message, Object data) {
        return new Result(HttpStatus.ERROR, false, message, data);
    }
}
