package com.bdis.common.enums;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {
    SUCCESS("SUCCESS", "操作成功"),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数错误"),
    UNAUTHORIZED("UNAUTHORIZED", "未登录或登录已失效"),
    FORBIDDEN("FORBIDDEN", "无权限访问"),
    PASSWORD_CHANGE_REQUIRED("PASSWORD_CHANGE_REQUIRED", "首次登录需要修改密码"),
    PASSWORD_VERIFICATION_FAILED("PASSWORD_VERIFICATION_FAILED", "密码验证失败"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    CONFLICT("CONFLICT", "资源冲突"),
    RESOURCE_CONFLICT("RESOURCE_CONFLICT", "资源已被其他请求修改"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常");

    private final String code;

    private final String message;

    ResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
