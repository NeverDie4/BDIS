package com.bdis.common.security;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import lombok.Getter;

@Getter
public class CurrentUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;

    private final String username;

    private final String realName;

    private final Long organizationId;

    private final Long departmentId;

    private final Set<String> roleCodes;

    private final Set<Long> roleIds;

    private final Set<String> permissions;

    public CurrentUser(
            Long userId,
            String username,
            String realName,
            Long organizationId,
            Long departmentId,
            Set<String> roleCodes,
            Set<Long> roleIds,
            Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.organizationId = organizationId;
        this.departmentId = departmentId;
        this.roleCodes = Collections.unmodifiableSet(roleCodes);
        this.roleIds = Collections.unmodifiableSet(roleIds);
        this.permissions = Collections.unmodifiableSet(permissions);
    }
}
