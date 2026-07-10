package com.bdis.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BootstrapAdminDTO {

    @NotBlank(message = "初始化令牌不能为空")
    private String bootstrapToken;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少 8 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String phoneNumber;

    private String email;
}
