package com.bdis.modules.permission.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataScopeVO {

    private Long id;

    private Long roleId;

    private String resourceType;

    private String scopeType;

    private Long organizationId;

    private Long departmentId;

    private String customRule;

    private Integer status;
}
