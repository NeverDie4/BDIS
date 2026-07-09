package com.bdis.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataScopeDTO {

    @NotNull(message = "角色不能为空")
    private Long roleId;

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    private Long organizationId;

    private Long departmentId;

    private String customRule;

    private Integer status;
}
