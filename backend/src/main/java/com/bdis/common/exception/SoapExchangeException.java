package com.bdis.common.exception;

public class SoapExchangeException extends BusinessException {

    public SoapExchangeException(String message) {
        super(500, message);
    }

    public SoapExchangeException(String message, Throwable cause) {
        super(500, message);
        initCause(cause);
    }
}
