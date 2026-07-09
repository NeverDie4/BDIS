package com.bdis.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionDTO {

    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @NotBlank(message = "权限类型不能为空")
    private String permissionType;

    private Long menuId;

    private String apiPath;

    private String requestMethod;

    private String description;

    private Integer status;
}
