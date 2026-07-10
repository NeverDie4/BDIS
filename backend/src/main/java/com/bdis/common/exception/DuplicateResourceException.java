package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(ResultCodeEnum.CONFLICT, message);
    }
}
