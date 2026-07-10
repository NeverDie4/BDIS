package com.bdis.common.core;

import com.bdis.common.enums.ResultCodeEnum;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class Result<T> {

    private final String code;

    private final String message;

    private final T data;

    private final OffsetDateTime timestamp;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = OffsetDateTime.now();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> error(ResultCodeEnum resultCode, String message, T data) {
        return new Result<>(resultCode.getCode(), message, data);
    }
}
