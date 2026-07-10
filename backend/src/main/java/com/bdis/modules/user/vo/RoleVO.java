package com.bdis.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String roleType;

    private String dataScope;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
