package com.bdis.modules.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {

    @Size(max = 50, message = "姓名不能超过50个字符")
    private String realName;

    @Pattern(regexp = "^$|^[0-9+() -]{6,30}$", message = "手机号格式不正确")
    private String phoneNumber;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;
}
