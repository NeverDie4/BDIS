package com.bdis.modules.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateDTO {

    private String roleName;

    private String roleType;

    private String dataScope;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
