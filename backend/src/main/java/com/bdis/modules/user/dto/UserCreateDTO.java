package com.bdis.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateDTO {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少 8 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private String phoneNumber;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Long organizationId;

    private Long departmentId;

    private Boolean mustChangePassword = true;

    @NotEmpty(message = "角色列表不能为空")
    private List<Long> roleIds;
}
