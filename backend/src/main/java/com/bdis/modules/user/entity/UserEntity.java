package com.bdis.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user")
public class UserEntity extends BaseEntity {

    private String userNo;

    private String username;

    private String passwordHash;

    private String realName;

    private String phoneNumber;

    private String email;

    private Long organizationId;

    private Long departmentId;

    private String userType;

    private String avatarUrl;

    private LocalDateTime lastLoginAt;
}
