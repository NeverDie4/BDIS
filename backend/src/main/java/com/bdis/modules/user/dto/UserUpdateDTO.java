package com.bdis.modules.user.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

    private String realName;

    private String phoneNumber;

    private String email;

    private Long organizationId;

    private Long departmentId;

    private String userType;

    private Integer status;

    private List<Long> roleIds;
}
