package com.bdis.modules.permission.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionVO {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private Long menuId;

    private String apiPath;

    private String requestMethod;

    private String description;

    private Integer status;
}
