package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class SoapExchangeException extends BusinessException {

    public SoapExchangeException(String message) {
        super(ResultCodeEnum.SYSTEM_ERROR, message);
    }

    public SoapExchangeException(String message, Throwable cause) {
        super(ResultCodeEnum.SYSTEM_ERROR, message);
        initCause(cause);
    }
}
