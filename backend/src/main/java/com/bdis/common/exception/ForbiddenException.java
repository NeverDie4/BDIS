package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ResultCodeEnum.FORBIDDEN, message);
    }
}
