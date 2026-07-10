package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ResultCodeEnum.UNAUTHORIZED, message);
    }
}
