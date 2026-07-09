package com.bdis.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

    private String realName;

    private String phoneNumber;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Long organizationId;

    private Long departmentId;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;

    private List<Long> roleIds;
}
