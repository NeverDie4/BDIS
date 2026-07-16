package com.bdis.common.exception;

import com.bdis.common.enums.ResultCodeEnum;

public class PasswordChangeRequiredException extends BusinessException {

    public PasswordChangeRequiredException() {
        super(ResultCodeEnum.PASSWORD_CHANGE_REQUIRED, "首次登录需要修改密码");
    }
}
