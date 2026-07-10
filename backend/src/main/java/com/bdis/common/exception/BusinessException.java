package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCodeEnum resultCode;

    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
