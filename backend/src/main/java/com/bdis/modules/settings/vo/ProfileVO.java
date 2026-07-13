package com.bdis.modules.settings.vo;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileVO {

    private Long userId;

    private String username;

    private String realName;

    private String phoneNumber;

    private String email;

    private String avatarUrl;

    private Long organizationId;

    private String organizationName;

    private Long departmentId;

    private String departmentName;

    private Set<String> roleCodes;

    private LocalDateTime lastLoginAt;

    private LocalDateTime passwordChangedAt;

    private Boolean mustChangePassword;
}
