package com.bdis.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
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

    private String email;

    private Long organizationId;

    private Long departmentId;

    private String userType;

    private List<Long> roleIds;
}
