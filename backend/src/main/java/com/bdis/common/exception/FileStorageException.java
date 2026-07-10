package com.bdis.common.exception;

public class FileStorageException extends BusinessException {

    public FileStorageException(String message) {
        super(500, message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(500, message);
        initCause(cause);
    }
}
