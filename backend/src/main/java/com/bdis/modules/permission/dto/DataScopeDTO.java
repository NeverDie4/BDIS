package com.bdis.modules.permission.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "all|organization|department|self|custom",
            message = "范围类型只能是 all、organization、department、self、custom")
    private String scopeType;

    private Long organizationId;

    private Long departmentId;

    private String customRule;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
