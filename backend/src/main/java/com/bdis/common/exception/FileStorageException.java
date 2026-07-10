package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class FileStorageException extends BusinessException {

    public FileStorageException(String message) {
        super(ResultCodeEnum.SYSTEM_ERROR, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(ResultCodeEnum.SYSTEM_ERROR, message);
        initCause(cause);
    }
}
