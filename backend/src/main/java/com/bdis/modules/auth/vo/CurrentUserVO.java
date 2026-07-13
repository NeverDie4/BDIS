package com.bdis.modules.auth.vo;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrentUserVO {

    private Long userId;

    private String username;

    private String realName;

    private String avatarUrl;

    private Long organizationId;

    private Long departmentId;

    private Set<String> roleCodes;

    private Set<Long> roleIds;

    private Set<String> permissions;

    private Boolean mustChangePassword;
}
