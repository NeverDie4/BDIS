package com.bdis.modules.user.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVO {

    private Long id;

    private String userNo;

    private String username;

    private String realName;

    private String phoneNumber;

    private String email;

    private Long organizationId;

    private Long departmentId;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private List<RoleVO> roles;
}
