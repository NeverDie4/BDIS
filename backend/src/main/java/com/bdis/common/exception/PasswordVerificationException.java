package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class PasswordVerificationException extends BusinessException {

    public PasswordVerificationException() {
        super(ResultCodeEnum.PASSWORD_VERIFICATION_FAILED, "密码验证失败");
    }
}
