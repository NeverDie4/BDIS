package com.bdis.modules.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordUpdateRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 72, message = "新密码长度必须为8到72位")
    private String newPassword;

    private Boolean revokeOtherSessions = true;
}
