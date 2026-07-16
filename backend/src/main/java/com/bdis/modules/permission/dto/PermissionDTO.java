package com.bdis.modules.permission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "menu|button|api", message = "权限类型只能是 menu、button、api")
    private String permissionType;

    private Long menuId;

    private String apiPath;

    @Pattern(
            regexp = "GET|POST|PUT|PATCH|DELETE|OPTIONS",
            message = "请求方法只能是 GET、POST、PUT、PATCH、DELETE、OPTIONS")
    private String requestMethod;

    private String description;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
