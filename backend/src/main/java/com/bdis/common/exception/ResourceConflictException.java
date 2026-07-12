package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class ResourceConflictException extends BusinessException {

    public ResourceConflictException(String message) {
        super(ResultCodeEnum.RESOURCE_CONFLICT, message);
    }
}
