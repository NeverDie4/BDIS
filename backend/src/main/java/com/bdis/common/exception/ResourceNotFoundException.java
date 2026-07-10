package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ResultCodeEnum.NOT_FOUND, message);
    }
}
