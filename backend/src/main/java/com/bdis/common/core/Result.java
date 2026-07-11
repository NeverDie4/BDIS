package com.bdis.common.core;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Result<T> {

    private String code;

    private String message;

    private T data;

    private OffsetDateTime timestamp;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode("SUCCESS");
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(OffsetDateTime.now());
        return result;
    }

    public static <T> Result<T> failure(String code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setTimestamp(OffsetDateTime.now());
        return result;
    }
}
